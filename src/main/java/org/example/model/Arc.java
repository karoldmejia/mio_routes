package org.example.model;

public class Arc {
    private Stop from;
    private Stop to;
    private int routeId;


    public Arc(Stop from, Stop to, int routeId) {
        this.from=from;
        this.to=to;
        this.routeId=routeId;
    }

    public Stop getFrom() {
        return from;
    }

    public Stop getTo() {
        return to;
    }

    public int getRouteId() {
        return routeId;
    }
}

