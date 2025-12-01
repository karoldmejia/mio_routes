package org.example.app;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;
import org.example.ice.SpeedCalculatorWorkerImpl;
import org.example.model.*;
import org.example.service.CsvReaderService;
import org.example.service.GraphBuilderService;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * Nodo Worker - Procesa batches de eventos GPS
 *
 * Uso:
 *   java WorkerNode <nodeId> <port>
 *
 * Ejemplo:
 *   java WorkerNode 1 10001
 */
public class WorkerNode {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Uso: java WorkerNode <nodeId> <port>");
            System.err.println("Ejemplo: java WorkerNode 1 10001");
            System.exit(1);
        }

        int nodeId = Integer.parseInt(args[0]);
        int port = Integer.parseInt(args[1]);
        String nodeName = "Worker-" + nodeId;

        System.out.println("========================================");
        System.out.println("  WORKER NODE - SITM-MIO");
        System.out.println("========================================");
        System.out.printf("Node ID: %d%n", nodeId);
        System.out.printf("Port: %d%n", port);
        System.out.println("========================================\n");

        try {
            // Cargar datos del grafo
            System.out.println("Cargando datos del SITM-MIO...");
            CsvReaderService reader = new CsvReaderService();
            Map<Integer, Stop> stops = reader.readStops("data/stops.csv");
            Map<Integer, Route> routes = reader.readRoutes("data/lines.csv");
            List<RouteStop> routeStops = reader.readRouteStops("data/linestops.csv");

            System.out.println("Construyendo grafos...");
            GraphBuilderService builder = new GraphBuilderService(stops, routes, routeStops);

            // DEBUG: Verificar métodos disponibles
            System.out.println("\n[DEBUG] Verificando métodos de GraphBuilderService:");
            System.out.println("  - build(): " + hasMethod(builder, "build"));
            System.out.println("  - buildAllGraphs(): " + hasMethod(builder, "buildAllGraphs"));
            System.out.println("  - buildGraph(): " + hasMethod(builder, "buildGraph"));

            Map<Integer, RouteGraph> routeGraphs;

            // Intentar diferentes métodos de construcción
            if (hasMethod(builder, "build")) {
                System.out.println("Usando método build()...");
                try {
                    // Asumiendo que build() retorna GraphBuildResult
                    Method buildMethod = builder.getClass().getMethod("build");
                    Object result = buildMethod.invoke(builder);

                    if (result instanceof GraphBuildResult) {
                        routeGraphs = ((GraphBuildResult) result).getGraphsByRoute();
                    } else if (result instanceof Map) {
                        // Si build() retorna directamente el Map
                        routeGraphs = (Map<Integer, RouteGraph>) result;
                    } else {
                        throw new RuntimeException("Tipo de retorno inesperado: " + result.getClass());
                    }
                } catch (Exception e) {
                    System.err.println("Error usando build(): " + e.getMessage());
                    System.out.println("Intentando buildAllGraphs()...");
                    routeGraphs = builder.buildAllGraphs();
                }
            } else if (hasMethod(builder, "buildAllGraphs")) {
                System.out.println("Usando método buildAllGraphs()...");
                routeGraphs = builder.buildAllGraphs();
            } else {
                throw new RuntimeException("No se encontraron métodos para construir grafos");
            }

            System.out.printf("\n Grafos cargados exitosamente:%n");
            System.out.printf("  - Paradas: %d%n", stops.size());
            System.out.printf("  - Rutas: %d%n", routes.size());
            System.out.printf("  - Grafos de ruta: %d%n", routeGraphs.size());

            // DEBUG: Mostrar algunas rutas cargadas
            if (!routeGraphs.isEmpty()) {
                System.out.println("\n[DEBUG] Primeras 5 rutas cargadas:");
                int count = 0;
                for (Map.Entry<Integer, RouteGraph> entry : routeGraphs.entrySet()) {
                    if (count++ >= 5) break;
                    RouteGraph graph = entry.getValue();
                    System.out.printf("  - Ruta %d: %d outbound, %d inbound arcos%n",
                            entry.getKey(),
                            graph.getOutboundArcs().size(),
                            graph.getInboundArcs().size());
                }
            }

            // Inicializar Ice
            Communicator communicator = null;

            // Crear comunicador Ice
            communicator = Util.initialize(args);

            // Crear adaptador
            ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints(
                    "WorkerAdapter",
                    "default -p " + port
            );

            // Crear servant con DEBUG extendido
            System.out.println("\nCreando SpeedCalculatorWorkerImpl...");
            SpeedCalculatorWorkerImpl worker = new SpeedCalculatorWorkerImpl(
                    nodeId, nodeName, stops, routeGraphs
            );

            // Agregar servant al adaptador
            adapter.add(worker, Util.stringToIdentity("SpeedWorker"));

            // Activar adaptador
            adapter.activate();

            System.out.println("\n" + "=".repeat(40));
            System.out.println("✓ Worker iniciado correctamente");
            System.out.printf("✓ Escuchando en puerto %d%n", port);
            System.out.printf("✓ Proxy: SpeedWorker:default -p %d%n", port);
            System.out.println("=".repeat(40));
            System.out.println("\nEsperando conexiones del Master...");
            System.out.println("Presiona Ctrl+C para detener\n");

            // Esperar shutdown
            communicator.waitForShutdown();

        } catch (Exception e) {
            System.err.println("\n✗ Error en Worker: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    // Método auxiliar para verificar si un objeto tiene un método
    private static boolean hasMethod(Object obj, String methodName) {
        try {
            obj.getClass().getMethod(methodName);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}