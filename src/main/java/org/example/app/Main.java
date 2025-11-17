package org.example.app;

import org.example.model.*;
import org.example.service.*;

import java.io.File;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        // 1. Leer CSVs
        System.out.println("Cargando datos del SITM-MIO...");
        CsvReaderService reader = new CsvReaderService();
        Map<Integer, Stop> stops = reader.readStops("data/stops.csv");
        Map<Integer, Route> routes = reader.readRoutes("data/lines.csv");
        List<RouteStop> routeStops = reader.readRouteStops("data/linestops.csv");

        // 2. Construir grafos
        System.out.println("Construyendo grafos...");
        GraphBuilderService builder = new GraphBuilderService(stops, routes, routeStops);
        GraphBuildResult result = builder.build();

        // 3. Imprimir resumen en consola
        System.out.println("\n========================================");
        System.out.println("Total de rutas: " + result.getGraphsByRoute().size());
        System.out.println("Total de paradas: " + stops.size());
        System.out.println("========================================\n");

        // 4. Menú interactivo
        Scanner scanner = new Scanner(System.in);
        boolean continuar = true;

        while (continuar) {
            mostrarMenu();
            String opcion = scanner.nextLine().trim();

            switch (opcion) {
                case "1":
                    exportarRutaEspecifica(scanner, result);
                    break;
                case "2":
                    exportarTodasLasRutas(result);
                    break;
                case "3":
                    exportarGrafoGlobal(result);
                    break;
                case "4":
                    listarRutas(result);
                    break;
                case "5":
                    imprimirRutaEnConsola(scanner, result);
                    break;
                case "6":
                    continuar = false;
                    System.out.println("\n¡Hasta luego!");
                    break;
                default:
                    System.out.println("\nOpción no válida. Intenta de nuevo.\n");
            }
        }

        scanner.close();
    }

    private static void mostrarMenu() {
        System.out.println("========================================");
        System.out.println("  EXPORTADOR DE GRAFOS SITM-MIO");
        System.out.println("========================================");
        System.out.println("1. Exportar ruta específica");
        System.out.println("2. Exportar todas las rutas");
        System.out.println("3. Exportar grafo global");
        System.out.println("4. Listar todas las rutas disponibles");
        System.out.println("5. Ver ruta en consola (texto)");
        System.out.println("6. Salir");
        System.out.println("========================================");
        System.out.print("Selecciona una opción: ");
    }

    private static void exportarRutaEspecifica(Scanner scanner, GraphBuildResult result) {
        System.out.println("\n--- Exportar Ruta Específica ---");
        System.out.print("Ingresa el ID de la ruta (ej: 131, 140, 2301): ");

        try {
            int routeId = Integer.parseInt(scanner.nextLine().trim());
            RouteGraph routeGraph = result.getGraphsByRoute().get(routeId);

            if (routeGraph == null) {
                System.out.println("\nRuta no encontrada. Usa la opción 4 para ver rutas disponibles.\n");
                return;
            }

            File routesDir = new File("graphs/routes");
            if (!routesDir.exists()) {
                routesDir.mkdirs();
            }

            String shortName = routeGraph.getRoute().getShortName()
                    .replaceAll("[^a-zA-Z0-9]", "_");

            String fileName = String.format("graphs/routes/route_%04d_%s.jpg",
                    routeId, shortName);

            GraphImageExporterService exporter = new GraphImageExporterService();
            exporter.exportRouteGraph(routeGraph, fileName);

            System.out.println("\nRuta " + routeGraph.getRoute().getShortName() +
                    " (ID: " + routeId + ") exportada exitosamente.");
            System.out.println("Ubicación: " + fileName + "\n");

        } catch (NumberFormatException e) {
            System.out.println("\nError: Debes ingresar un número válido.\n");
        } catch (Exception e) {
            System.out.println("\nError al exportar: " + e.getMessage() + "\n");
        }
    }

    private static void exportarTodasLasRutas(GraphBuildResult result) {
        System.out.println("\n--- Exportar Todas las Rutas ---");
        System.out.print("¿Estás seguro? Esto puede tomar varios minutos (s/n): ");

        Scanner scanner = new Scanner(System.in);
        String confirmacion = scanner.nextLine().trim().toLowerCase();

        if (!confirmacion.equals("s") && !confirmacion.equals("si")) {
            System.out.println("\nExportación cancelada.\n");
            return;
        }

        File routesDir = new File("graphs/routes");
        if (!routesDir.exists()) {
            routesDir.mkdirs();
        }

        GraphImageExporterService exporter = new GraphImageExporterService();

        System.out.println("\n📤 Exportando todas las rutas...\n");

        int count = 0;
        int skipped = 0;
        long startTime = System.currentTimeMillis();

        for (Map.Entry<Integer, RouteGraph> entry : result.getGraphsByRoute().entrySet()) {
            int routeId = entry.getKey();
            RouteGraph routeGraph = entry.getValue();

            int totalArcs = routeGraph.getOutboundArcs().size() +
                    routeGraph.getInboundArcs().size();

            if (totalArcs < 2) {
                skipped++;
                continue;
            }

            String shortName = routeGraph.getRoute().getShortName()
                    .replaceAll("[^a-zA-Z0-9]", "_");

            String fileName = String.format("graphs/routes/route_%04d_%s.jpg",
                    routeId, shortName);

            try {
                exporter.exportRouteGraph(routeGraph, fileName);
                count++;

                if (count % 10 == 0) {
                    System.out.println("  ✓ Exportadas " + count + " rutas...");
                }
            } catch (Exception e) {
                System.err.println("  ✗ Error exportando ruta " + routeId + ": " + e.getMessage());
            }
        }

        long endTime = System.currentTimeMillis();
        double seconds = (endTime - startTime) / 1000.0;

        System.out.println("\n========================================");
        System.out.println("   Exportación completada");
        System.out.println("   Rutas exportadas: " + count);
        System.out.println("   Rutas omitidas: " + skipped);
        System.out.println("   Tiempo: " + String.format("%.2f", seconds) + " segundos");
        System.out.println("   Ubicación: graphs/routes/");
        System.out.println("========================================\n");
    }

    private static void exportarGrafoGlobal(GraphBuildResult result) {
        System.out.println("\n--- Exportar Grafo Global ---");
        System.out.println("Advertencia: El grafo global puede ser muy grande y difícil de visualizar.");
        System.out.print("¿Continuar? (s/n): ");

        Scanner scanner = new Scanner(System.in);
        String confirmacion = scanner.nextLine().trim().toLowerCase();

        if (!confirmacion.equals("s") && !confirmacion.equals("si")) {
            System.out.println("\nExportación cancelada.\n");
            return;
        }

        File graphsDir = new File("graphs");
        if (!graphsDir.exists()) {
            graphsDir.mkdirs();
        }

        System.out.println("\n📤 Exportando grafo global (esto puede tomar tiempo)...");

        GraphImageExporterService exporter = new GraphImageExporterService();
        String globalFile = "graphs/global_graph.jpg";

        try {
            exporter.exportGlobalGraph(result.getGlobalGraph(), globalFile);
            System.out.println("\nGrafo global exportado exitosamente.");
            System.out.println("Ubicación: " + globalFile + "\n");
        } catch (Exception e) {
            System.out.println("\nError al exportar: " + e.getMessage() + "\n");
        }
    }

    private static void listarRutas(GraphBuildResult result) {
        System.out.println("\n========================================");
        System.out.println("  RUTAS DISPONIBLES");
        System.out.println("========================================");

        List<Map.Entry<Integer, RouteGraph>> sorted = new ArrayList<>(result.getGraphsByRoute().entrySet());
        sorted.sort(Map.Entry.comparingByKey());

        int count = 0;
        for (Map.Entry<Integer, RouteGraph> entry : sorted) {
            int routeId = entry.getKey();
            RouteGraph rg = entry.getValue();

            int arcs = rg.getOutboundArcs().size() + rg.getInboundArcs().size();

            System.out.printf("  ID: %4d | %-10s | Arcos: %3d%n",
                    routeId,
                    rg.getRoute().getShortName(),
                    arcs);

            count++;

            // Pausar cada 20 rutas
            if (count % 20 == 0) {
                System.out.print("\nPresiona Enter para continuar...");
                Scanner scanner = new Scanner(System.in);
                scanner.nextLine();
            }
        }

        System.out.println("========================================");
        System.out.println("Total: " + result.getGraphsByRoute().size() + " rutas\n");
    }

    private static void imprimirRutaEnConsola(Scanner scanner, GraphBuildResult result) {
        System.out.println("\n--- Ver Ruta en Consola ---");
        System.out.print("Ingresa el ID de la ruta: ");

        try {
            int routeId = Integer.parseInt(scanner.nextLine().trim());
            RouteGraph routeGraph = result.getGraphsByRoute().get(routeId);

            if (routeGraph == null) {
                System.out.println("\nRuta no encontrada.\n");
                return;
            }

            System.out.println();
            System.out.println(routeGraph);

        } catch (NumberFormatException e) {
            System.out.println("\nError: Debes ingresar un número válido.\n");
        }
    }
}
