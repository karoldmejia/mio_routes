package org.example.service;

import org.example.model.*;

import java.util.*;

public class GraphBuilderService {

    private Map<Integer, Stop> stops;
    private Map<Integer, Route> routes;
    private List<RouteStop> routeStops;

    public GraphBuilderService(
            Map<Integer, Stop> stops,
            Map<Integer, Route> routes,
            List<RouteStop> routeStops
    ) {
        this.stops = stops;
        this.routes = routes;
        this.routeStops = routeStops;
    }

    // construir ambos grafos
    public GraphBuildResult build() {

        Map<Integer, RouteGraph> graphs = buildAllGraphs();
        GlobalGraph globalGraph = new GlobalGraph();

        for (Stop s : stops.values()) {
            globalGraph.addStop(s);
        }
        for (RouteGraph g : graphs.values()) {

            for (Arc a : g.getOutboundArcs()) {
                globalGraph.addArc(a);
            }

            for (Arc a : g.getInboundArcs()) {
                globalGraph.addArc(a);
            }
        }

        return new GraphBuildResult(graphs, globalGraph);
    }

    // Helpers

    public Map<Integer, RouteGraph> buildAllGraphs() {
        Map<Integer, RouteGraph> graphs = new HashMap<>();

        Map<Integer, Map<Integer, List<RouteStop>>> grouped = groupStops();

        for (Integer routeId : grouped.keySet()) {
            RouteGraph graph = buildGraphForRoute(routeId, grouped.get(routeId));
            graphs.put(routeId, graph);
        }

        return graphs;
    }

    private Map<Integer, Map<Integer, List<RouteStop>>> groupStops() {

        Map<Integer, Map<Integer, List<RouteStop>>> grouped = new HashMap<>();

        for (RouteStop rs : routeStops) {
            int routeId = rs.getLineId();
            int orientation = rs.getOrientation();

            grouped
                    .computeIfAbsent(routeId, r -> new HashMap<>())
                    .computeIfAbsent(orientation, o -> new ArrayList<>())
                    .add(rs);
        }

        for (Map<Integer, List<RouteStop>> orientations : grouped.values()) {
            for (List<RouteStop> list : orientations.values()) {
                list.sort(Comparator.comparing(RouteStop::getSequence));
            }
        }

        return grouped;
    }

    private RouteGraph buildGraphForRoute(
            Integer routeId,
            Map<Integer, List<RouteStop>> orientationMap
    ) {
        RouteGraph graph = new RouteGraph(routes.get(routeId));

        for (Integer orientation : orientationMap.keySet()) {
            List<RouteStop> stopsSeq = orientationMap.get(orientation);

            for (int i = 0; i < stopsSeq.size() - 1; i++) {

                Stop from = stops.get(stopsSeq.get(i).getStopId());
                Stop to = stops.get(stopsSeq.get(i + 1).getStopId());

                Arc arc = new Arc(from, to, routeId);

                if (orientation == 0) {
                    graph.addOutboundArc(arc);
                } else {
                    graph.addInboundArc(arc);
                }
            }
        }

        return graph;
    }


}
