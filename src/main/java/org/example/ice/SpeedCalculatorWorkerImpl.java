package org.example.ice;

import MioGraph.*;
import MioGraph.GpsEvent;
import com.zeroc.Ice.Current;
import org.example.model.*;
import org.example.service.SpeedCalculatorService;

import java.util.*;

public class SpeedCalculatorWorkerImpl implements SpeedCalculatorWorker {

    private int nodeId;
    private String nodeName;
    private Map<Integer, Stop> stops;
    private Map<Integer, RouteGraph> routeGraphs;

    // Estadísticas
    private int eventsProcessed = 0;
    private int arcsCalculated = 0;
    private long totalTimeMs = 0;

    public SpeedCalculatorWorkerImpl(int nodeId, String nodeName,
                                     Map<Integer, Stop> stops,
                                     Map<Integer, RouteGraph> routeGraphs) {
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.stops = stops;
        this.routeGraphs = routeGraphs;
    }

    @Override
    public ArcSpeed[] processEventBatch(GpsEvent[] events, int batchId, Current current) {
        long startTime = System.currentTimeMillis();

        System.out.printf("[Worker %d] Procesando batch %d con %d eventos%n",
                nodeId, batchId, events.length);

        // Convertir eventos Ice a modelo Java
        List<org.example.model.GpsEvent> javaEvents = convertIceEvents(events);

        // Calcular velocidades
        SpeedCalculatorService calculator = new SpeedCalculatorService(stops, routeGraphs);
        Map<String, SpeedCalculatorService.ArcSpeedResult> results =
                calculator.calculateArcSpeeds(javaEvents);

        // Convertir resultados a Ice
        ArcSpeed[] arcSpeeds = convertToIceResults(results);

        long endTime = System.currentTimeMillis();
        long elapsed = endTime - startTime;

        // Actualizar estadísticas
        eventsProcessed += events.length;
        arcsCalculated += arcSpeeds.length;
        totalTimeMs += elapsed;

        System.out.printf("[Worker %d] Batch %d completado: %d arcos calculados en %d ms%n",
                nodeId, batchId, arcSpeeds.length, elapsed);

        return arcSpeeds;
    }

    @Override
    public ProcessingStats getStats(Current current) {
        ProcessingStats stats = new ProcessingStats();
        stats.nodeId = nodeId;
        stats.nodeName = nodeName;
        stats.eventsProcessed = eventsProcessed;
        stats.arcsCalculated = arcsCalculated;
        stats.totalTimeMs = totalTimeMs;
        return stats;
    }

    @Override
    public void resetStats(Current current) {
        eventsProcessed = 0;
        arcsCalculated = 0;
        totalTimeMs = 0;
        System.out.printf("[Worker %d] Estadísticas reiniciadas%n", nodeId);
    }

    @Override
    public boolean ping(Current current) {
        return true;
    }

    // Métodos de conversión

    private List<org.example.model.GpsEvent> convertIceEvents(GpsEvent[] iceEvents) {
        List<org.example.model.GpsEvent> javaEvents = new ArrayList<>();

        for (GpsEvent ice : iceEvents) {
            org.example.model.GpsEvent java = new org.example.model.GpsEvent(
                    ice.eventType,
                    ice.date,
                    ice.stopId,
                    ice.odometer,
                    ice.latitude,  // Convertir de vuelta a formato original
                    ice.longitude,
                    ice.lineId,
                    ice.tripId,
                    ice.datagramDate,
                    ice.busId
            );
            javaEvents.add(java);
        }

        return javaEvents;
    }

    private ArcSpeed[] convertToIceResults(
            Map<String, SpeedCalculatorService.ArcSpeedResult> results) {

        List<ArcSpeed> arcSpeeds = new ArrayList<>();

        for (SpeedCalculatorService.ArcSpeedResult result : results.values()) {
            ArcSpeed arcSpeed = new ArcSpeed();
            arcSpeed.fromStopId = result.getFromStopId();
            arcSpeed.toStopId = result.getToStopId();
            arcSpeed.routeId = result.getRouteId();
            arcSpeed.orientation = result.getOrientation();
            arcSpeed.averageSpeed = result.getAverageSpeed();
            arcSpeed.sampleCount = result.getSampleCount();
            arcSpeed.processingTimeMs = 0; // Se calcula en el worker

            arcSpeeds.add(arcSpeed);
        }

        return arcSpeeds.toArray(new ArcSpeed[0]);
    }
}