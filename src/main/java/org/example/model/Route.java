package org.example.model;

import java.util.HashMap;
import java.util.Map;

public class Route {
    private int id;
    private String shortName;
    private String description;

    private Map<Integer, Stop> stops = new HashMap<>();

    public Route(int routeId, String shortName, String description) {
        this.id=routeId;
        this.shortName=shortName;
        this.description=description;
    }

    public String getShortName() {
        return shortName;
    }

    public int getId() {
        return id;
    }
}
