package org.example.model;

import java.util.ArrayList;
import java.util.List;

public class RouteGraph {

    private Route route;

    private List<Arc> outboundArcs = new ArrayList<>();
    private List<Arc> inboundArcs = new ArrayList<>();

    public Route getRoute() {
        return route;
    }

    public RouteGraph(Route route) {
        this.route = route;
    }

    public void addOutboundArc(Arc arc) {
        outboundArcs.add(arc);
    }

    public void addInboundArc(Arc arc) {
        inboundArcs.add(arc);
    }

    public List<Arc> getOutboundArcs() { return outboundArcs; }
    public List<Arc> getInboundArcs() { return inboundArcs; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("======================================\n");
        sb.append("Ruta: ")
                .append(route.getShortName())
                .append(" (ID ").append(route.getId()).append(")\n");
        sb.append("======================================\n");

        sb.append("IDA (orientation = 0):\n");
        for (Arc a : outboundArcs) {
            sb.append("  ")
                    .append(a.getFrom().getShortName())
                    .append(" -> ")
                    .append(a.getTo().getShortName())
                    .append("\n");
        }

        sb.append("VUELTA (orientation = 1):\n");
        for (Arc a : inboundArcs) {
            sb.append("  ")
                    .append(a.getFrom().getShortName())
                    .append(" -> ")
                    .append(a.getTo().getShortName())
                    .append("\n");
        }

        sb.append("\n");
        return sb.toString();
    }
}
