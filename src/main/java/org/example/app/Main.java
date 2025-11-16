package org.example.app;

import org.example.model.*;
import org.example.service.*;

import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        // 1. Leer CSVs
        CsvReaderService reader = new CsvReaderService();
        Map<Integer, Stop> stops = reader.readStops("data/stops.csv");
        Map<Integer, Route> routes = reader.readRoutes("data/lines.csv");
        List<RouteStop> routeStops = reader.readRouteStops("data/linestops.csv");

        // 2. Construir grafos
        GraphBuilderService builder = new GraphBuilderService(stops, routes, routeStops);
        GraphBuildResult result = builder.build();

        // 3. Imprimir cada grafo de ruta
        for (RouteGraph g : result.getGraphsByRoute().values()) {
            System.out.println(g);
        }

        // 4. Exportar grafo global
        GraphImageExporterService exporter = new GraphImageExporterService();
        String globalFile = "graphs/global_graph.jpg";
        exporter.exportGlobalGraph(result.getGlobalGraph(), globalFile);
    }

}
