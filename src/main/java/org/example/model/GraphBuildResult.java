package org.example.model;

import java.util.Map;

public class GraphBuildResult {

    private Map<Integer, RouteGraph> graphsByRoute;
    private GlobalGraph globalGraph;

    public GraphBuildResult(Map<Integer, RouteGraph> graphsByRoute, GlobalGraph globalGraph) {
        this.graphsByRoute = graphsByRoute;
        this.globalGraph = globalGraph;
    }

    public Map<Integer, RouteGraph> getGraphsByRoute() { return graphsByRoute; }
    public GlobalGraph getGlobalGraph() { return globalGraph; }
}
