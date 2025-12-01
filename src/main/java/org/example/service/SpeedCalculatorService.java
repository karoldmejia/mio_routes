package org.example.service;

import org.example.model.*;

import java.util.*;

public class SpeedCalculatorService {

    private Map<Integer, Stop> stops;
    private Map<Integer, RouteGraph> routeGraphs;
    private Map<String, ArcSpeedResult> arcSpeedResults;

    public SpeedCalculatorService(Map<Integer, Stop> stops,
                                  Map<Integer, RouteGraph> routeGraphs) {
        this.stops = stops;
        this.routeGraphs = routeGraphs;
        this.arcSpeedResults = new HashMap<>();
        System.out.println("SpeedCalculatorService inicializado con " +
                stops.size() + " paradas y " + routeGraphs.size() + " grafos de rutas.");
    }

    // =====================================================================
    // 1) FUNCIÓN PRINCIPAL DE CÁLCULO
    // =====================================================================
    public Map<String, ArcSpeedResult> calculateArcSpeeds(List<GpsEvent> events) {

        System.out.println("\n================ CALCULO DE VELOCIDAD INICIADO ================");
        System.out.println("Eventos recibidos: " + events.size());

        Map<String, List<GpsEvent>> trips = groupEventsByTrip(events);


        for (String key : trips.keySet()) {
            List<GpsEvent> tripEvents = trips.get(key);

            tripEvents.sort((e1, e2) -> {
                if (e1.getTimestamp()==null || e2.getTimestamp()==null) return 0;
                return e1.getTimestamp().compareTo(e2.getTimestamp());
            });

            calculateSpeedsForTrip(tripEvents);
        }

        System.out.println("\n================= RESULTADOS =================");
        System.out.println("Arcos finales con velocidad calculada: " + arcSpeedResults.size());

        for (String k : arcSpeedResults.keySet()) {
            ArcSpeedResult r = arcSpeedResults.get(k);
            System.out.println("✔ " + k + "  avg=" + r.getAverageSpeed() +
                    " km/h   samples=" + r.getSampleCount());
        }
        System.out.println("=====================================================\n");

        return arcSpeedResults;
    }


    // =====================================================================
    // 2) AGRUPAR EVENTOS POR VIAJE
    // =====================================================================
    private Map<String, List<GpsEvent>> groupEventsByTrip(List<GpsEvent> events) {
        Map<String, List<GpsEvent>> trips = new HashMap<>();

        for (GpsEvent e : events) {

            if (e.getLineId() == 0 || e.getBusId() <= 0)  {
                continue;
            }

            String key = e.getBusId() + "-" + e.getTripId();
            trips.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
        }

        return trips;
    }


    // =====================================================================
    // 3) CÁLCULO DE VELOCIDADES ENTRE EVENTOS
    // =====================================================================
    private void calculateSpeedsForTrip(List<GpsEvent> events) {
        System.out.println("Eventos en viaje: " + events.size());

        for (int i=0; i<events.size()-1; i++) {

            GpsEvent a = events.get(i);
            GpsEvent b = events.get(i+1);

            if (a.getLineId() != b.getLineId()) {
                continue;
            }

            double dist = a.distanceTo(b);
            long secs = a.secondsTo(b);

            System.out.println("Par evento " + i+" → dist="+dist+"m  time="+secs+"s");

            if (dist < 10 || secs <= 0 || secs > 3600) {
                continue;
            }

            double speed = (dist/1000.0) / (secs/3600.0);

            if (speed < 1 || speed > 100) {
                continue;
            }

            identifyAndRecordArc(a, b, speed);
        }
    }


    // =====================================================================
    // 4) IDENTIFICAR ARCO Y GUARDAR RESULTADO
    // =====================================================================
    private void identifyAndRecordArc(GpsEvent from, GpsEvent to, double speed) {

        int routeId = from.getLineId();
        System.out.println("Route id: "+routeId);

        RouteGraph graph = routeGraphs.get(routeId);
        System.out.println("Grafo: "+graph);

        if (graph == null) {
            return;
        }

        Arc arc = findClosestArc(graph, from, to);

        if (arc == null) {
            return;
        }

        String key = arc.getFrom().getId()+"-"+arc.getTo().getId()+"-"+
                routeId+"-"+arc.getOrientation();

        ArcSpeedResult r = arcSpeedResults.computeIfAbsent(key,
                k -> new ArcSpeedResult(arc.getFrom().getId(), arc.getTo().getId(),
                        routeId, arc.getOrientation()));

        r.addSample(speed);
        System.out.println("  Registro → " + key + "   speed=" + speed);
    }


    // =====================================================================
    // 5) BÚSQUEDA DEL ARCO MÁS CERCANO
    // =====================================================================
    private Arc findClosestArc(RouteGraph graph, GpsEvent from, GpsEvent to) {
        Arc best=null;
        double min=Double.MAX_VALUE;

        for (Arc arc : graph.getOutboundArcs()) {
            double d=calculateArcDistance(arc, from, to);
            if(d<min){min=d; best=arc;}
        }

        for (Arc arc : graph.getInboundArcs()) {
            double d=calculateArcDistance(arc, from, to);
            if(d<min){min=d; best=arc;}
        }

        return (min<1000) ? best : null;
    }


    private double calculateArcDistance(Arc arc, GpsEvent from, GpsEvent to) {

        Stop s1 = arc.getFrom();
        Stop s2 = arc.getTo();
        double fromLatDeg = from.getLatitude();
        double fromLonDeg = from.getLongitude();
        double toLatDeg = to.getLatitude();
        double toLonDeg = to.getLongitude();

        // Usar Haversine para calcular distancias reales
        double d1 = haversineDistance(s1.getLat(), s1.getLon(), fromLatDeg, fromLonDeg);
        double d2 = haversineDistance(s2.getLat(), s2.getLon(), toLatDeg, toLonDeg);

        double totalDistanceKm = d1 + d2;
        double totalDistanceMeters = totalDistanceKm;

        return totalDistanceMeters;
    }

    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371; // Radio de la Tierra en km

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // Distancia en km
    }


    public Map<String, ArcSpeedResult> getResults(){
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