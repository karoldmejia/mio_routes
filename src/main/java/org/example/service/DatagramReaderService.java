package org.example.service;

import org.example.model.GpsEvent;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DatagramReaderService {

    /**
     * Lee todos los datagramas de un archivo CSV
     */
    public List<GpsEvent> readDatagrams(String filePath) {
        List<GpsEvent> events = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            // Skip header
            String line = br.readLine();

            while ((line = br.readLine()) != null) {
                try {
                    GpsEvent event = parseLine(line);
                    if (event != null) {
                        events.add(event);
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing line: " + line);
                }
            }

        } catch (IOException e) {
            System.err.println("Error reading file: " + filePath);
            e.printStackTrace();
        }

        return events;
    }

    /**
     * Lee datagramas en lotes para procesamiento distribuido
     */
    public List<List<GpsEvent>> readDatagramsInBatches(String filePath, int batchSize) {
        List<List<GpsEvent>> batches = new ArrayList<>();
        List<GpsEvent> currentBatch = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            // Skip header
            String line = br.readLine();

            while ((line = br.readLine()) != null) {
                try {
                    GpsEvent event = parseLine(line);
                    if (event != null) {
                        currentBatch.add(event);

                        if (currentBatch.size() >= batchSize) {
                            batches.add(new ArrayList<>(currentBatch));
                            currentBatch.clear();
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing line: " + line);
                }
            }

            // Agregar último batch si tiene datos
            if (!currentBatch.isEmpty()) {
                batches.add(currentBatch);
            }

        } catch (IOException e) {
            System.err.println("Error reading file: " + filePath);
            e.printStackTrace();
        }

        return batches;
    }

    private GpsEvent parseLine(String line) {
        // Remover comillas y split por coma
        String[] parts = line.replace("\"", "").split(",");

        if (parts.length < 11) {
            return null;
        }

        try {
            int eventType = Integer.parseInt(parts[0].trim());
            String date = parts[1].trim();
            int stopId = Integer.parseInt(parts[2].trim());
            int odometer = Integer.parseInt(parts[3].trim());
            double latitude = Double.parseDouble(parts[4].trim());
            double longitude = Double.parseDouble(parts[5].trim());
            int lineId = Integer.parseInt(parts[6].trim());
            int tripId = Integer.parseInt(parts[7].trim());
            String datagramDate = parts[9].trim();
            int busId = Integer.parseInt(parts[10].trim());

            return new GpsEvent(eventType, date, stopId, odometer,
                    latitude, longitude, lineId, tripId, datagramDate, busId);

        } catch (NumberFormatException e) {
            return null;
        }
    }
}