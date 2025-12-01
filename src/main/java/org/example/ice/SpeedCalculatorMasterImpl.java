package org.example.ice;

import MioGraph.*;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.ObjectPrx;
import org.example.model.GpsEvent;
import org.example.service.DatagramReaderService;

import java.util.*;
import java.util.concurrent.*;

public class SpeedCalculatorMasterImpl implements SpeedCalculatorMaster {

    // Workers registrados: nodeId -> proxy
    private Map<Integer, SpeedCalculatorWorkerPrx> workers = new ConcurrentHashMap<>();

    // Resultados agregados
    private Map<String, ArcSpeedAggregator> aggregatedResults = new ConcurrentHashMap<>();

    // Control de procesamiento
    private long totalProcessingTime = 0;
    private boolean processing = false;

    @Override
    public synchronized void registerWorker(String workerProxy, int nodeId, Current current) {
        try {
            ObjectPrx base = current.adapter.getCommunicator().stringToProxy(workerProxy);
            SpeedCalculatorWorkerPrx worker = SpeedCalculatorWorkerPrx.checkedCast(base);

            if (worker != null) {
                workers.put(nodeId, worker);
                System.out.printf("[Master] Worker %d registrado: %s%n", nodeId, workerProxy);

                // Verificar que el worker está vivo
                if (worker.ping()) {
                    System.out.printf("[Master] Worker %d respondió correctamente%n", nodeId);
                }
            } else {
                System.err.printf("[Master] Error: No se pudo crear proxy para worker %d%n", nodeId);
            }
        } catch (Exception e) {
            System.err.printf("[Master] Error registrando worker %d: %s%n", nodeId, e.getMessage());
        }
    }

    @Override
    public synchronized void unregisterWorker(int nodeId, Current current) {
        workers.remove(nodeId);
        System.out.printf("[Master] Worker %d desregistrado%n", nodeId);
    }

    @Override
    public void processFile(String filePath, Current current) {
        if (processing) {
            System.err.println("[Master] Ya hay un procesamiento en curso");
            return;
        }

        if (workers.isEmpty()) {
            System.err.println("[Master] No hay workers disponibles");
            return;
        }

        processing = true;
        aggregatedResults.clear();

        long startTime = System.currentTimeMillis();

        try {
            System.out.printf("[Master] Iniciando procesamiento de: %s%n", filePath);
            System.out.printf("[Master] Workers disponibles: %d%n", workers.size());

            // Leer eventos en batches
            DatagramReaderService reader = new DatagramReaderService();
            int batchSize = 10000; // Ajustable según necesidades
            List<List<org.example.model.GpsEvent>> batches =
                    reader.readDatagramsInBatches(filePath, batchSize);

            System.out.printf("[Master] Total de batches: %d (tamaño: %d eventos)%n",
                    batches.size(), batchSize);

            // Distribuir batches entre workers usando ThreadPool
            ExecutorService executor = Executors.newFixedThreadPool(workers.size());
            List<Future<BatchResult>> futures = new ArrayList<>();

            int batchId = 0;
            Iterator<SpeedCalculatorWorkerPrx> workerIterator =
                    workers.values().iterator();

            for (List<org.example.model.GpsEvent> batch : batches) {
                // Round-robin entre workers
                if (!workerIterator.hasNext()) {
                    workerIterator = workers.values().iterator();
                }

                SpeedCalculatorWorkerPrx worker = workerIterator.next();
                final int currentBatchId = batchId++;

                Future<BatchResult> future = executor.submit(() ->
                        processBatchWithWorker(worker, batch, currentBatchId)
                );

                futures.add(future);
            }

            // Esperar resultados y agregar
            int completedBatches = 0;
            for (Future<BatchResult> future : futures) {
                try {
                    BatchResult result = future.get();
                    aggregateResults(result.arcSpeeds);
                    completedBatches++;

                    if (completedBatches % 10 == 0) {
                        System.out.printf("[Master] Progreso: %d/%d batches completados%n",
                                completedBatches, batches.size());
                    }
                } catch (Exception e) {
                    System.err.println("[Master] Error procesando batch: " + e.getMessage());
                }
            }

            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.HOURS);

            long endTime = System.currentTimeMillis();
            totalProcessingTime = endTime - startTime;

            System.out.printf("[Master] Procesamiento completado en %d ms%n", totalProcessingTime);
            System.out.printf("[Master] Arcos únicos calculados: %d%n", aggregatedResults.size());

        } catch (Exception e) {
            System.err.println("[Master] Error en procesamiento: " + e.getMessage());
            e.printStackTrace();
        } finally {
            processing = false;
        }
    }

    @Override
    public ArcSpeed[] getAggregatedResults(Current current) {
        List<ArcSpeed> results = new ArrayList<>();

        for (ArcSpeedAggregator agg : aggregatedResults.values()) {
            ArcSpeed arcSpeed = new ArcSpeed();
            arcSpeed.fromStopId = agg.fromStopId;
            arcSpeed.toStopId = agg.toStopId;
            arcSpeed.routeId = agg.routeId;
            arcSpeed.orientation = agg.orientation;
            arcSpeed.averageSpeed = agg.getAverageSpeed();
            arcSpeed.sampleCount = agg.getTotalSamples();
            arcSpeed.processingTimeMs = 0;

            results.add(arcSpeed);
        }

        return results.toArray(new ArcSpeed[0]);
    }

    @Override
    public ProcessingStats[] getAllStats(Current current) {
        List<ProcessingStats> allStats = new ArrayList<>();

        for (Map.Entry<Integer, SpeedCalculatorWorkerPrx> entry : workers.entrySet()) {
            try {
                ProcessingStats stats = entry.getValue().getStats();
                allStats.add(stats);
            } catch (Exception e) {
                System.err.printf("[Master] Error obteniendo stats de worker %d: %s%n",
                        entry.getKey(), e.getMessage());
            }
        }

        return allStats.toArray(new ProcessingStats[0]);
    }

    @Override
    public long getTotalProcessingTime(Current current) {
        return totalProcessingTime;
    }

    // Métodos auxiliares

    private BatchResult processBatchWithWorker(SpeedCalculatorWorkerPrx worker,
                                               List<org.example.model.GpsEvent> batch,
                                               int batchId) {
        try {
            // Convertir eventos Java a Ice
            MioGraph.GpsEvent[] iceEvents = convertToIceEvents(batch);

            // Procesar en el worker
            ArcSpeed[] arcSpeeds = worker.processEventBatch(iceEvents, batchId);

            return new BatchResult(batchId, arcSpeeds);

        } catch (Exception e) {
            System.err.printf("[Master] Error en worker para batch %d: %s%n",
                    batchId, e.getMessage());
            return new BatchResult(batchId, new ArcSpeed[0]);
        }
    }

    private MioGraph.GpsEvent[] convertToIceEvents(List<org.example.model.GpsEvent> javaEvents) {
        MioGraph.GpsEvent[] iceEvents = new MioGraph.GpsEvent[javaEvents.size()];

        for (int i = 0; i < javaEvents.size(); i++) {
            org.example.model.GpsEvent java = javaEvents.get(i);
            MioGraph.GpsEvent ice = new MioGraph.GpsEvent();

            ice.eventType = java.getEventType();
            ice.date = java.getDate();
            ice.stopId = java.getStopId();
            ice.odometer = java.getOdometer();
            ice.latitude = java.getLatitude();
            ice.longitude = java.getLongitude();
            ice.lineId = java.getLineId();
            ice.tripId = java.getTripId();
            ice.datagramDate = java.getDatagramDate();
            ice.busId = java.getBusId();

            iceEvents[i] = ice;
        }

        return iceEvents;
    }

    private synchronized void aggregateResults(ArcSpeed[] arcSpeeds) {
        for (ArcSpeed arcSpeed : arcSpeeds) {
            String key = arcSpeed.fromStopId + "-" + arcSpeed.toStopId + "-" +
                    arcSpeed.routeId + "-" + arcSpeed.orientation;

            ArcSpeedAggregator agg = aggregatedResults.computeIfAbsent(key,
                    k -> new ArcSpeedAggregator(
                            arcSpeed.fromStopId,
                            arcSpeed.toStopId,
                            arcSpeed.routeId,
                            arcSpeed.orientation
                    ));

            agg.addSamples(arcSpeed.averageSpeed, arcSpeed.sampleCount);
        }
    }

    // Clases auxiliares

    private static class BatchResult {
        int batchId;
        ArcSpeed[] arcSpeeds;

        BatchResult(int batchId, ArcSpeed[] arcSpeeds) {
            this.batchId = batchId;
            this.arcSpeeds = arcSpeeds;
        }
    }

    private static class ArcSpeedAggregator {
        int fromStopId;
        int toStopId;
        int routeId;
        int orientation;
        double sumSpeed = 0;
        int totalSamples = 0;

        ArcSpeedAggregator(int fromStopId, int toStopId, int routeId, int orientation) {
            this.fromStopId = fromStopId;
            this.toStopId = toStopId;
            this.routeId = routeId;
            this.orientation = orientation;
        }

        void addSamples(double avgSpeed, int samples) {
            sumSpeed += avgSpeed * samples;
            totalSamples += samples;
        }

        double getAverageSpeed() {
            return totalSamples > 0 ? sumSpeed / totalSamples : 0;
        }

        int getTotalSamples() {
            return totalSamples;
        }
    }
}