package org.example.app;

import MioGraph.ArcSpeed;
import MioGraph.ProcessingStats;
import MioGraph.SpeedCalculatorMasterPrx;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Util;

import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * Cliente automatizado para ejecutar experimentos
 *
 * Uso:
 *   java AutomatedClient <master_host> <master_port> <num_workers> <data_file> <output_file>
 *
 * Ejemplo:
 *   java AutomatedClient localhost 10000 4 data/datagrams_100000.csv results/exp_4workers.csv
 */
public class AutomatedClient {

    public static void main(String[] args) {
        if (args.length < 5) {
            System.err.println("Uso: AutomatedClient <master_host> <master_port> <num_workers> <data_file> <output_file>");
            System.exit(1);
        }

        String masterHost = args[0];
        int masterPort = Integer.parseInt(args[1]);
        int numWorkers = Integer.parseInt(args[2]);
        String dataFile = args[3];
        String outputFile = args[4];

        System.out.println("========================================");
        System.out.println("  CLIENTE AUTOMATIZADO - EXPERIMENTO");
        System.out.println("========================================");
        System.out.printf("Master: %s:%d%n", masterHost, masterPort);
        System.out.printf("Workers esperados: %d%n", numWorkers);
        System.out.printf("Archivo: %s%n", dataFile);
        System.out.printf("Salida: %s%n", outputFile);
        System.out.println("========================================\n");

        Communicator communicator = null;

        try {
            // Inicializar Ice
            communicator = Util.initialize(args);

            // Conectar al Master
            String proxyStr = String.format("SpeedMaster:default -h %s -p %d",
                    masterHost, masterPort);
            SpeedCalculatorMasterPrx master = SpeedCalculatorMasterPrx.checkedCast(
                    communicator.stringToProxy(proxyStr)
            );

            if (master == null) {
                throw new Exception("No se pudo conectar al Master");
            }

            System.out.println("✓ Conectado al Master\n");

            // Registrar workers
            System.out.println("Registrando workers...");
            int workerBasePort = 10001;

            for (int i = 1; i <= numWorkers; i++) {
                int workerPort = workerBasePort + i - 1;
                String workerProxy = String.format("SpeedWorker:default -h %s -p %d",
                        masterHost, workerPort);

                System.out.printf("  Registrando Worker %d (puerto %d)...%n", i, workerPort);
                master.registerWorker(workerProxy, i);
            }

            System.out.println(" Workers registrados\n");
            Thread.sleep(2000);

            // Iniciar procesamiento
            System.out.println("Iniciando procesamiento...");
            System.out.println("Esto puede tomar varios minutos...\n");

            long startTime = System.currentTimeMillis();
            master.processFile(dataFile);
            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;

            System.out.println("\n✓ Procesamiento completado");
            System.out.printf("✓ Tiempo: %,d ms (%.2f seg)%n",
                    processingTime, processingTime / 1000.0);

            // Obtener estadísticas
            System.out.println("\nObteniendo estadísticas...");
            ProcessingStats[] stats = master.getAllStats();

            int totalEvents = 0;
            int totalArcs = 0;

            System.out.println("\nEstadísticas por Worker:");
            for (ProcessingStats stat : stats) {
                System.out.printf("  Worker %d: %,d eventos, %,d arcos, %,d ms%n",
                        stat.nodeId, stat.eventsProcessed, stat.arcsCalculated,
                        stat.totalTimeMs);
                totalEvents += stat.eventsProcessed;
                totalArcs += stat.arcsCalculated;
            }

            // Obtener resultados
            System.out.println("\nObteniendo resultados...");
            ArcSpeed[] results = master.getAggregatedResults();

            System.out.printf("✓ Arcos únicos: %,d%n", results.length);

            // Calcular métricas
            double avgSpeed = 0;
            int totalSamples = 0;

            for (ArcSpeed arc : results) {
                avgSpeed += arc.averageSpeed;
                totalSamples += arc.sampleCount;
            }

            avgSpeed = results.length > 0 ? avgSpeed / results.length : 0;

            System.out.printf("✓ Velocidad promedio: %.2f km/h%n", avgSpeed);
            System.out.printf("✓ Total de muestras: %,d%n", totalSamples);

            // Guardar resultados
            System.out.println("\nGuardando resultados...");
            saveResults(outputFile, numWorkers, dataFile, processingTime,
                    totalEvents, totalArcs, results.length, avgSpeed, totalSamples, stats);

            System.out.printf("✓ Resultados guardados en: %s%n", outputFile);

            // Guardar detalle de arcos
            String detailFile = outputFile.replace(".csv", "_detail.csv");
            saveArcDetails(detailFile, results);
            System.out.printf("✓ Detalle de arcos guardado en: %s%n", detailFile);

            System.out.println("\n========================================");
            System.out.println("  EXPERIMENTO COMPLETADO");
            System.out.println("========================================\n");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            if (communicator != null) {
                communicator.destroy();
            }
        }
    }

    private static void saveResults(String outputFile, int numWorkers, String dataFile,
                                    long processingTime, int totalEvents, int totalArcs,
                                    int uniqueArcs, double avgSpeed, int totalSamples,
                                    ProcessingStats[] stats) throws Exception {

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            // Resumen general
            writer.println("=== RESUMEN DEL EXPERIMENTO ===");
            writer.println("Numero de Workers," + numWorkers);
            writer.println("Archivo de Datos," + dataFile);
            writer.println("Tiempo de Procesamiento (ms)," + processingTime);
            writer.println("Tiempo de Procesamiento (seg)," + (processingTime / 1000.0));
            writer.println("Eventos Procesados," + totalEvents);
            writer.println("Arcos Calculados (total)," + totalArcs);
            writer.println("Arcos Únicos," + uniqueArcs);
            writer.println("Velocidad Promedio (km/h)," + String.format("%.2f", avgSpeed));
            writer.println("Total de Muestras," + totalSamples);
            writer.println();

            // Estadísticas por worker
            writer.println("=== ESTADÍSTICAS POR WORKER ===");
            writer.println("Worker ID,Nombre,Eventos,Arcos,Tiempo (ms)");

            for (ProcessingStats stat : stats) {
                writer.printf("%d,%s,%d,%d,%d%n",
                        stat.nodeId, stat.nodeName, stat.eventsProcessed,
                        stat.arcsCalculated, stat.totalTimeMs);
            }

            writer.println();

            // Métricas de rendimiento
            writer.println("=== MÉTRICAS DE RENDIMIENTO ===");
            double eventsPerSec = totalEvents / (processingTime / 1000.0);
            double arcsPerSec = uniqueArcs / (processingTime / 1000.0);
            double speedup = stats.length > 0 ?
                    (double) totalEvents / stats[0].eventsProcessed : 1.0;

            writer.println("Eventos por Segundo," + String.format("%.2f", eventsPerSec));
            writer.println("Arcos por Segundo," + String.format("%.2f", arcsPerSec));
            writer.println("Speedup Estimado," + String.format("%.2f", speedup));
            writer.println("Eficiencia," + String.format("%.2f%%", (speedup / numWorkers) * 100));
        }
    }

    private static void saveArcDetails(String detailFile, ArcSpeed[] results) throws Exception {
        try (PrintWriter writer = new PrintWriter(new FileWriter(detailFile))) {
            writer.println("fromStopId,toStopId,routeId,orientation,averageSpeed,sampleCount");

            for (ArcSpeed arc : results) {
                writer.printf("%d,%d,%d,%d,%.4f,%d%n",
                        arc.fromStopId, arc.toStopId, arc.routeId,
                        arc.orientation, arc.averageSpeed, arc.sampleCount);
            }
        }
    }
}