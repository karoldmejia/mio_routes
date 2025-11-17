package org.example.model;

import java.util.HashSet;
import java.util.Set;

public class GlobalGraph {

    private Set<Arc> arcs = new HashSet<>();
    private Set<Stop> stops = new HashSet<>();

    public void addArc(Arc arc) {
        arcs.add(arc);
    }

    public void addStop(Stop stop) {
        stops.add(stop);
    }

    public Set<Arc> getArcs() { return arcs; }
    public Set<Stop> getStops() { return stops; }
}
