package org.example.service;

import org.example.model.GpsEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DatagramReaderService {

    /**
     * Lee todos los datagramas y muestra debug detallado
     */
    public List<GpsEvent> readDatagrams(String filePath) {

        System.out.println("\n===== DEBUG: Iniciando lectura completa =====");
        System.out.println("Archivo: " + filePath);

        List<GpsEvent> events = new ArrayList<>();
        int totalLines = 0;
        int parsedOK = 0;
        int parseErrors = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            String header = br.readLine(); // salta encabezado

            String line;
            while ((line = br.readLine()) != null) {
                totalLines++;

                try {
                    GpsEvent event = parseLine(line);

                    if (event != null) {
                        events.add(event);
                        parsedOK++;

                    } else {
                        parseErrors++;
                    }

                } catch (Exception ex) {
                    parseErrors++;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return events;
    }

    /**
     * Mismo debug, pero para lectura en BATCHES
     */
    public List<List<GpsEvent>> readDatagramsInBatches(String filePath, int batchSize) {

        List<List<GpsEvent>> batches = new ArrayList<>();
        List<GpsEvent> current = new ArrayList<>();

        int totalLines = 0;
        int parsed = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            br.readLine(); // Skip header

            String line;
            while ((line = br.readLine()) != null) {

                totalLines++;
                GpsEvent e = parseLine(line);

                if (e != null) {
                    current.add(e);
                    parsed++;

                    if (current.size() == batchSize) {
                        batches.add(new ArrayList<>(current));
                        current.clear();
                    }
                } else {
                    System.out.println("X Línea con error en parseo: " + line);
                }
            }

            if (!current.isEmpty()) {
                batches.add(current);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return batches;
    }

    private GpsEvent parseLine(String line) {

        String[] p = line.replace("\"", "").split(",");

        // Ahora deben ser al menos 12 columnas
        if (p.length < 12) return null;

        try {
            return new GpsEvent(
                    Integer.parseInt(p[0].trim()),   // eventType
                    p[1].trim(),                     // date
                    Integer.parseInt(p[2].trim()),   // stopId
                    Integer.parseInt(p[3].trim()),   // odometer
                    Double.parseDouble(p[4].trim()) / 10_000_000.0, // lat
                    Double.parseDouble(p[5].trim()) / 10_000_000.0,
                    Integer.parseInt(p[6].trim()),   // lineId
                    Integer.parseInt(p[7].trim()),   // tripId
                    p[10].trim(),                    // datagramDate
                    Integer.parseInt(p[11].trim())   // busId
            );

        } catch (NumberFormatException e) {
            return null;
        }
    }
}
