package org.example.service;

import org.example.model.GpsEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DatagramReaderService {

    /**
     * Procesa el archivo por streaming - NO acumula en memoria
     * @param filePath Ruta del archivo
     * @param batchSize Tamaño del lote
     * @param batchConsumer Consumidor que procesa cada batch (envía a workers, etc.)
     */
    public void processFileInBatches(String filePath, int batchSize, Consumer<List<GpsEvent>> batchConsumer) {

        int totalLines = 0;
        int parsedOK = 0;
        int parseErrors = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            br.readLine(); // Skip header
            List<GpsEvent> currentBatch = new ArrayList<>(batchSize);

            String line;
            while ((line = br.readLine()) != null) {
                totalLines++;

                try {
                    GpsEvent event = parseLine(line);

                    if (event != null) {
                        currentBatch.add(event);
                        parsedOK++;

                        // Cuando el batch está lleno, procesar y liberar memoria
                        if (currentBatch.size() >= batchSize) {
                            batchConsumer.accept(currentBatch);
                            currentBatch = new ArrayList<>(batchSize); // Nuevo batch
                        }
                    } else {
                        parseErrors++;
                    }

                } catch (Exception ex) {
                    parseErrors++;
                }
            }

            // Procesar el último batch si queda algo
            if (!currentBatch.isEmpty()) {
                batchConsumer.accept(currentBatch);
            }

            System.out.printf("\n===== RESUMEN PROCESAMIENTO =====\n");
            System.out.printf("Total líneas leídas: %d\n", totalLines);
            System.out.printf("Líneas parseadas OK: %d\n", parsedOK);
            System.out.printf("Errores de parseo: %d\n", parseErrors);

        } catch (IOException e) {
            throw new RuntimeException("Error leyendo archivo: " + filePath, e);
        }
    }

    /**
     * Versión que retorna batches pero con límite máximo (para testing/debug)
     */
    public List<List<GpsEvent>> readFirstNBatches(String filePath, int batchSize, int maxBatches) {
        List<List<GpsEvent>> batches = new ArrayList<>();
        int batchCount = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            br.readLine(); // Skip header
            List<GpsEvent> currentBatch = new ArrayList<>(batchSize);

            String line;
            while ((line = br.readLine()) != null && batchCount < maxBatches) {

                GpsEvent event = parseLine(line);
                if (event != null) {
                    currentBatch.add(event);

                    if (currentBatch.size() >= batchSize) {
                        batches.add(currentBatch);
                        currentBatch = new ArrayList<>(batchSize);
                        batchCount++;
                    }
                }
            }

            // Agregar el último batch incompleto
            if (!currentBatch.isEmpty() && batchCount < maxBatches) {
                batches.add(currentBatch);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return batches;
    }

    /**
     * Optimización del parseLine para usar menos memoria
     */
    private GpsEvent parseLine(String line) {
        // Split con límite para evitar arrays enormes
        String[] p = line.split(",", 13); // Máximo 13 campos esperados

        if (p.length < 12) {
            return null;
        }

        try {
            // Procesar sin crear objetos String intermedios innecesarios
            int eventType = parseInt(p[0]);
            String date = p[1].trim().replace("\"", "");
            int stopId = parseInt(p[2]);
            int odometer = parseInt(p[3]);
            double lat = parseDouble(p[4]) / 10_000_000.0;
            double lon = parseDouble(p[5]) / 10_000_000.0;
            int lineId = parseInt(p[6]);
            int tripId = parseInt(p[7]);
            String datagramDate = p[10].trim().replace("\"", "");
            int busId = parseInt(p[11]);

            return new GpsEvent(eventType, date, stopId, odometer, lat, lon,
                    lineId, tripId, datagramDate, busId);

        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int parseInt(String str) {
        return Integer.parseInt(str.trim().replace("\"", ""));
    }

    private double parseDouble(String str) {
        return Double.parseDouble(str.trim().replace("\"", ""));
    }
}