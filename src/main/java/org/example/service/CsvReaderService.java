package org.example.service;

import org.example.model.Route;
import org.example.model.RouteStop;
import org.example.model.Stop;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class CsvReaderService {

    public Map<Integer, Stop> readStops(String filePath) {
        Map<Integer, Stop> stops = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            String line = br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String[] parts = parseCSVLine(line);

                int stopId = Integer.parseInt(parts[0]);
                String shortName = parts[2];
                String longName = parts[3];
                double lon = Double.parseDouble(parts[6]);
                double lat = Double.parseDouble(parts[7]);

                Stop stop = new Stop(stopId, shortName, longName, lat, lon);
                stops.put(stopId, stop);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return stops;
    }

    public Map<Integer, Route> readRoutes(String filePath) {
        Map<Integer, Route> routes = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            String line = br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String[] parts = parseCSVLine(line);

                int routeId = Integer.parseInt(parts[0]);
                String shortName = parts[2];
                String description = parts[3];

                Route route = new Route(routeId, shortName, description);
                routes.put(routeId, route);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return routes;
    }

    public List<RouteStop> readRouteStops(String filePath) {
        List<RouteStop> routeStops = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            String line = br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String[] parts = parseCSVLine(line);

                int sequence = Integer.parseInt(parts[1]);
                int orientation = Integer.parseInt(parts[2]);
                int routeId = Integer.parseInt(parts[3]);
                int stopId = Integer.parseInt(parts[4]);

                RouteStop rs = new RouteStop(routeId, stopId, sequence, orientation);
                routeStops.add(rs);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return routeStops;
    }

    // Helpers
    private String[] parseCSVLine(String line) {
        // Quitar comillas y separar por comas
        return line.replace("\"", "").split(",");
    }
}
