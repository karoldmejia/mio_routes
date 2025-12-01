package org.example.service;

import org.example.model.*;

import java.util.*;

public class SpeedCalculatorService {

    private Map<Integer, Stop> stops;
    private Map<Integer, RouteGraph> routeGraphs;

    // Almacena velocidades calculadas: key = "fromStopId-toStopId-routeId-orientation"
    private Map<String, ArcSpeedResult> arcSpeedResults;

    public SpeedCalculatorService(Map<Integer, Stop> stops,
                                  Map<Integer, RouteGraph> routeGraphs) {
        this.stops = stops;
        this.routeGraphs = routeGraphs;
        this.arcSpeedResults = new HashMap<>();
    }

    /**
     * Calcula velocidades promedio de arcos basado en eventos GPS
     */
    public Map<String, ArcSpeedResult> calculateArcSpeeds(List<GpsEvent> events) {
        // Agrupar eventos por (busId, tripId) y ordenar por timestamp
        Map<String, List<GpsEvent>> trips = groupEventsByTrip(events);

        // Para cada viaje, calcular velocidades entre paradas consecutivas
        for (Map.Entry<String, List<GpsEvent>> entry : trips.entrySet()) {
            List<GpsEvent> tripEvents = entry.getValue();

            // Ordenar por timestamp
            tripEvents.sort((e1, e2) -> {
                if (e1.getTimestamp() == null || e2.getTimestamp() == null) {
                    return 0;
                }
                return e1.getTimestamp().compareTo(e2.getTimestamp());
            });

            // Calcular velocidades entre eventos consecutivos
            calculateSpeedsForTrip(tripEvents);
        }

        return arcSpeedResults;
    }

    private Map<String, List<GpsEvent>> groupEventsByTrip(List<GpsEvent> events) {
        Map<String, List<GpsEvent>> trips = new HashMap<>();

        for (GpsEvent event : events) {
            // Filtrar eventos inválidos
            if (event.getLineId() <= 0 || event.getBusId() <= 0) {
                continue;
            }

            String key = event.getBusId() + "-" + event.getTripId();
            trips.computeIfAbsent(key, k -> new ArrayList<>()).add(event);
        }

        return trips;
    }

    private void calculateSpeedsForTrip(List<GpsEvent> events) {
        for (int i = 0; i < events.size() - 1; i++) {
            GpsEvent current = events.get(i);
            GpsEvent next = events.get(i + 1);

            // Calcular solo para eventos del mismo lineId
            if (current.getLineId() != next.getLineId()) {
                continue;
            }

            // Calcular distancia y tiempo
            double distance = current.distanceTo(next); // metros
            long seconds = current.secondsTo(next);

            // Filtrar valores inválidos
            if (distance < 10 || seconds <= 0 || seconds > 3600) {
                continue;
            }

            // Calcular velocidad en km/h
            double speedKmh = (distance / 1000.0) / (seconds / 3600.0);

            // Filtrar velocidades anómalas (más de 100 km/h o menos de 1 km/h)
            if (speedKmh < 1 || speedKmh > 100) {
                continue;
            }

            // Intentar identificar el arco en el grafo de rutas
            identifyAndRecordArc(current, next, speedKmh);
        }
    }

    private void identifyAndRecordArc(GpsEvent from, GpsEvent to, double speed) {
        int routeId = from.getLineId();

        RouteGraph graph = routeGraphs.get(routeId);
        if (graph == null) {
            return;
        }

        // Buscar el arco más cercano a estos eventos GPS
        Arc closestArc = findClosestArc(graph, from, to);

        if (closestArc != null) {
            String key = closestArc.getFrom().getId() + "-" +
                    closestArc.getTo().getId() + "-" +
                    routeId + "-" +
                    closestArc.getOrientation();

            ArcSpeedResult result = arcSpeedResults.computeIfAbsent(key,
                    k -> new ArcSpeedResult(
                            closestArc.getFrom().getId(),
                            closestArc.getTo().getId(),
                            routeId,
                            closestArc.getOrientation()
                    ));

            result.addSample(speed);
        }
    }

    private Arc findClosestArc(RouteGraph graph, GpsEvent from, GpsEvent to) {
        Arc best = null;
        double minDistance = Double.MAX_VALUE;

        // Buscar en arcos outbound
        for (Arc arc : graph.getOutboundArcs()) {
            double dist = calculateArcDistance(arc, from, to);
            if (dist < minDistance) {
                minDistance = dist;
                best = arc;
            }
        }

        // Buscar en arcos inbound
        for (Arc arc : graph.getInboundArcs()) {
            double dist = calculateArcDistance(arc, from, to);
            if (dist < minDistance) {
                minDistance = dist;
                best = arc;
            }
        }

        // Solo retornar si la distancia es razonable (< 500m)
        return (minDistance < 500) ? best : null;
    }

    private double calculateArcDistance(Arc arc, GpsEvent from, GpsEvent to) {
        Stop fromStop = arc.getFrom();
        Stop toStop = arc.getTo();

        // Distancia euclidiana simple (aproximada)
        double d1 = Math.sqrt(
                Math.pow(fromStop.getLat() - from.getLatitude(), 2) +
                        Math.pow(fromStop.getLon() - from.getLongitude(), 2)
        );

        double d2 = Math.sqrt(
                Math.pow(toStop.getLat() - to.getLatitude(), 2) +
                        Math.pow(toStop.getLon() - to.getLongitude(), 2)
        );

        return (d1 + d2) * 111000; // Conversión aproximada a metros
    }

    public Map<String, ArcSpeedResult> getResults() {
        return arcSpeedResults;
    }

    /**
     * Clase interna para almacenar resultados de velocidad de un arco
     */
    public static class ArcSpeedResult {
        private int fromStopId;
        private int toStopId;
        private int routeId;
        private int orientation;
        private List<Double> speeds = new ArrayList<>();

        public ArcSpeedResult(int fromStopId, int toStopId, int routeId, int orientation) {
            this.fromStopId = fromStopId;
            this.toStopId = toStopId;
            this.routeId = routeId;
            this.orientation = orientation;
        }

        public void addSample(double speed) {
            speeds.add(speed);
        }

        public double getAverageSpeed() {
            if (speeds.isEmpty()) return 0;
            return speeds.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        }

        public int getSampleCount() {
            return speeds.size();
        }

        public int getFromStopId() { return fromStopId; }
        public int getToStopId() { return toStopId; }
        public int getRouteId() { return routeId; }
        public int getOrientation() { return orientation; }
    }
}