package org.example.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GpsEvent {
    private int eventType;
    private String date;
    private int stopId;
    private int odometer;
    private double latitude;
    private double longitude;
    private int lineId;
    private int tripId;
    private String datagramDate;
    private int busId;

    // Timestamp parseado
    private LocalDateTime timestamp;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public GpsEvent(int eventType, String date, int stopId, int odometer,
                    double latitude, double longitude, int lineId, int tripId,
                    String datagramDate, int busId) {
        this.eventType = eventType;
        this.date = date;
        this.stopId = stopId;
        this.odometer = odometer;
        this.latitude = latitude;  // Convertir a grados decimales
        this.longitude = longitude;
        this.lineId = lineId;
        this.tripId = tripId;
        this.datagramDate = datagramDate;
        this.busId = busId;

        // Parsear timestamp
        try {
            this.timestamp = LocalDateTime.parse(datagramDate, FORMATTER);
        } catch (Exception e) {
            this.timestamp = null;
        }
    }

    // Getters
    public int getEventType() { return eventType; }
    public String getDate() { return date; }
    public int getStopId() { return stopId; }
    public int getOdometer() { return odometer; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public int getLineId() { return lineId; }
    public int getTripId() { return tripId; }
    public String getDatagramDate() { return datagramDate; }
    public int getBusId() { return busId; }
    public LocalDateTime getTimestamp() { return timestamp; }

    /**
     * Calcula la distancia en metros entre este punto y otro usando la fórmula de Haversine
     */
    public double distanceTo(GpsEvent other) {
        double R = 6371000; // Radio de la Tierra en metros

        // Diferencias angulares en grados
        double dLatDeg = other.latitude - this.latitude;
        double dLonDeg = other.longitude - this.longitude;

        // Conversión a radianes
        double lat1Rad = Math.toRadians(this.latitude);
        double lat2Rad = Math.toRadians(other.latitude);
        double deltaLat = Math.toRadians(dLatDeg);
        double deltaLon = Math.toRadians(dLonDeg);

        // Cálculo Haversine paso a paso
        double sinLat = Math.sin(deltaLat / 2);
        double sinLon = Math.sin(deltaLon / 2);

        double a = sinLat * sinLat +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        sinLon * sinLon;

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distance = R * c;
        return distance;
    }

    /**
     * Calcula el tiempo en segundos entre este evento y otro
     */
    public long secondsTo(GpsEvent other) {
        if (this.timestamp == null || other.timestamp == null) {
            return 0;
        }
        return java.time.Duration.between(this.timestamp, other.timestamp).getSeconds();
    }

    @Override
    public String toString() {
        return String.format("GpsEvent[bus=%d, line=%d, trip=%d, lat=%.6f, lon=%.6f, time=%s]",
                busId, lineId, tripId, latitude, longitude, datagramDate);
    }
}