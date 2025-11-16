package org.example.model;

import java.util.ArrayList;
import java.util.List;

public class GlobalGraph {

    private List<Arc> arcs = new ArrayList<>();
    private List<Stop> stops = new ArrayList<>();

    public void addArc(Arc arc) {
        arcs.add(arc);
    }

    public void addStop(Stop stop) {
        stops.add(stop);
    }

    public List<Arc> getArcs() { return arcs; }
    public List<Stop> getStops() { return stops; }
}
