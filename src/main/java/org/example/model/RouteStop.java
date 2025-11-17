package org.example.model;

public class RouteStop {
    private int lineId;
    private int stopId;        // ID de stop
    private int sequence;      // STOPSEQUENCE
    private int orientation;   // 0 ida, 1 vuelta
    private int variant;


    public RouteStop(int routeId, int stopId, int sequence, int orientation) {
        this.lineId=routeId;
        this.stopId=stopId;
        this.sequence=sequence;
        this.orientation=orientation;
    }

    public int getVariant() {
        return variant;
    }

    public int getLineId() {
        return lineId;
    }

    public int getOrientation() {
        return orientation;
    }

    public int getSequence() {
        return sequence;
    }

    public int getStopId() {
        return stopId;
    }
}
