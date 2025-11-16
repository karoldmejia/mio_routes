package org.example.model;

public class Stop {
    private int id;
    private String shortName;
    private String longName;
    private double lat;
    private double lon;

    public Stop(int stopId, String shortName, String longName, double lat, double lon) {
        this.id = stopId;
        this.shortName = shortName;
        this.longName = longName;
        this.lat = lat;
        this.lon = lon;
    }

    public String getShortName() {
        return shortName;
    }

    public int getId() {
        return id;
    }

    public double getLat() { return lat; }
    public double getLon() { return lon; }
    public String getLongName() { return longName; }

}


