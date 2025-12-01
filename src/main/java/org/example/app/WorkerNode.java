package org.example.app;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;
import org.example.ice.SpeedCalculatorWorkerImpl;
import org.example.model.Route;
import org.example.model.RouteGraph;
import org.example.model.RouteStop;
import org.example.model.Stop;
import org.example.service.CsvReaderService;
import org.example.service.GraphBuilderService;

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

        // Cargar datos del grafo
        System.out.println("Cargando datos del SITM-MIO...");
        CsvReaderService reader = new CsvReaderService();
        Map<Integer, Stop> stops = reader.readStops("data/stops.csv");
        Map<Integer, Route> routes = reader.readRoutes("data/lines.csv");
        List<RouteStop> routeStops = reader.readRouteStops("data/linestops.csv");

        System.out.println("Construyendo grafos...");
        GraphBuilderService builder = new GraphBuilderService(stops, routes, routeStops);
        Map<Integer, RouteGraph> routeGraphs = builder.buildAllGraphs();

        System.out.printf("Grafos cargados: %d rutas, %d paradas%n",
                routeGraphs.size(), stops.size());

        // Inicializar Ice
        Communicator communicator = null;

        try {
            // Crear comunicador Ice
            communicator = Util.initialize(args);

            // Crear adaptador
            ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints(
                    "WorkerAdapter",
                    "default -p " + port
            );

            // Crear servant
            SpeedCalculatorWorkerImpl worker = new SpeedCalculatorWorkerImpl(
                    nodeId, nodeName, stops, routeGraphs
            );

            // Agregar servant al adaptador
            adapter.add(worker, Util.stringToIdentity("SpeedWorker"));

            // Activar adaptador
            adapter.activate();

            System.out.println("\n✓ Worker iniciado correctamente");
            System.out.printf("✓ Escuchando en puerto %d%n", port);
            System.out.printf("✓ Proxy: SpeedWorker:default -p %d%n", port);
            System.out.println("\nEsperando conexiones del Master...");
            System.out.println("Presiona Ctrl+C para detener\n");

            // Esperar shutdown
            communicator.waitForShutdown();

        } catch (Exception e) {
            System.err.println("Error en Worker: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (communicator != null) {
                try {
                    communicator.destroy();
                } catch (Exception e) {
                    System.err.println("Error cerrando comunicador: " + e.getMessage());
                }
            }
        }
    }
}