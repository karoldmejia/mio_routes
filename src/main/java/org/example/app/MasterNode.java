package org.example.app;

import MioGraph.ArcSpeed;
import MioGraph.ProcessingStats;
import MioGraph.SpeedCalculatorMasterPrx;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;
import org.example.ice.SpeedCalculatorMasterImpl;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

/**
 * Nodo Master - Coordina el procesamiento distribuido
 *
 * Uso:
 *   java MasterNode <port>
 *
 * Ejemplo:
 *   java MasterNode 10000
 */
public class MasterNode {

    private static SpeedCalculatorMasterPrx masterProxy;
    private static Communicator communicator;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Uso: java MasterNode <port>");
            System.err.println("Ejemplo: java MasterNode 10000");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);

        System.out.println("========================================");
        System.out.println("  MASTER NODE - SITM-MIO");
        System.out.println("  Cálculo Distribuido de Velocidades");
        System.out.println("========================================");
        System.out.printf("Port: %d%n", port);
        System.out.println("========================================\n");

        try {
            // Crear comunicador Ice
            communicator = Util.initialize(args);

            // Crear adaptador
            ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints(
                    "MasterAdapter",
                    "default -p " + port
            );

            // Crear servant
            SpeedCalculatorMasterImpl master = new SpeedCalculatorMasterImpl();

            // Agregar servant al adaptador
            adapter.add(master, Util.stringToIdentity("SpeedMaster"));

            // Activar adaptador
            adapter.activate();

            // Crear proxy para uso local
            masterProxy = SpeedCalculatorMasterPrx.checkedCast(
                    adapter.createProxy(Util.stringToIdentity("SpeedMaster"))
            );

            System.out.println("Master iniciado correctamente");
            System.out.printf("Escuchando en puerto %d%n", port);
            System.out.println("Proxy: SpeedMaster:default -p " + port);
            System.out.println("\nIniciando menú interactivo...\n");

            // Iniciar menú en thread separado
            Thread menuThread = new Thread(() -> runInteractiveMenu());
            menuThread.start();

            // Esperar shutdown
            communicator.waitForShutdown();

        } catch (Exception e) {
            System.err.println("Error en Master: " + e.getMessage());
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

    private static void runInteractiveMenu() {
        Scanner scanner = new Scanner(System.in);
        boolean continuar = true;

        while (continuar) {
            mostrarMenu();
            String opcion = scanner.nextLine().trim();

            switch (opcion) {
                case "1":
                    registrarWorker(scanner);
                    break;
                case "2":
                    procesarArchivo(scanner);
                    break;
                case "3":
                    mostrarEstadisticas();
                    break;
                case "4":
                    exportarResultados(scanner);
                    break;
                case "5":
                    mostrarResumenResultados();
                    break;
                case "6":
                    continuar = false;
                    shutdown();
                    break;
                default:
                    System.out.println("\nOpción no válida\n");
            }
        }

        scanner.close();
    }

    private static void mostrarMenu() {
        System.out.println("========================================");
        System.out.println("  MENÚ PRINCIPAL");
        System.out.println("========================================");
        System.out.println("1. Registrar Worker");
        System.out.println("2. Procesar archivo de datagramas");
        System.out.println("3. Ver estadísticas de workers");
        System.out.println("4. Exportar resultados a CSV");
        System.out.println("5. Ver resumen de resultados");
        System.out.println("6. Salir");
        System.out.println("========================================");
        System.out.print("Selecciona una opción: ");
    }

    private static void registrarWorker(Scanner scanner) {
        System.out.println("\n--- Registrar Worker ---");
        System.out.print("Node ID: ");
        int nodeId = Integer.parseInt(scanner.nextLine().trim());

        System.out.print("Host (default: localhost): ");
        String host = scanner.nextLine().trim();
        if (host.isEmpty()) {
            host = "localhost";
        }

        System.out.print("Puerto: ");
        int port = Integer.parseInt(scanner.nextLine().trim());

        String proxy = String.format("SpeedWorker:default -h %s -p %d", host, port);

        try {
            masterProxy.registerWorker(proxy, nodeId);
            System.out.println("✓ Worker registrado exitosamente\n");
        } catch (Exception e) {
            System.err.println("✗ Error registrando worker: " + e.getMessage() + "\n");
        }
    }

    private static void procesarArchivo(Scanner scanner) {
        System.out.println("\n--- Procesar Archivo ---");
        System.out.println("Archivos disponibles:");
        System.out.println("  1. data/datagrams_1M.csv");
        System.out.println("  2. data/datagrams_10M.csv");
        System.out.println("  3. data/datagrams_100M.csv");
        System.out.print("Selecciona (1-3) o ingresa ruta personalizada: ");

        String input = scanner.nextLine().trim();
        String filePath;

        switch (input) {
            case "1":
                filePath = "data/datagrams_1M.csv";
                break;
            case "2":
                filePath = "data/datagrams_10M.csv";
                break;
            case "3":
                filePath = "data/datagrams_100M.csv";
                break;
            default:
                filePath = input;
        }

        System.out.println("\nIniciando procesamiento de: " + filePath);
        System.out.println("Esto puede tomar varios minutos...\n");

        long start = 0;
        long end = 0;

        try {
            start = System.currentTimeMillis();
            masterProxy.processFile(filePath);
            end = System.currentTimeMillis();

            System.out.println("\n✓ Procesamiento completado");
            System.out.printf("✓ Tiempo total: %.2f segundos%n", (end - start) / 1000.0);
            System.out.println();

        } catch (Exception e) {
            System.err.println("✗ Error procesando archivo: " + e.getMessage() + "\n");
            e.printStackTrace();
            return;
        }

        // 1. Determinar tamaño del dataset
        String sizeLabel = "UNKNOWN";
        if (filePath.contains("1M")) sizeLabel = "1M";
        else if (filePath.contains("10M")) sizeLabel = "10M";
        else if (filePath.contains("100M")) sizeLabel = "100M";

        // 2. Obtener número de workers activos
        int workerCount = masterProxy.getAllStats().length;

        // 3. Crear carpeta results/<size>_<workers>
        String resultDir = "results/" + sizeLabel + "_" + workerCount + "workers";
        new java.io.File(resultDir).mkdirs();

        // 4. Guardar resultados de arcos
        try (PrintWriter writer = new PrintWriter(new FileWriter(resultDir + "/arc_speeds.csv"))) {
            writer.println("fromStopId,toStopId,routeId,orientation,averageSpeed,sampleCount");

            ArcSpeed[] results = masterProxy.getAggregatedResults();
            for (ArcSpeed arc : results) {
                writer.printf("%d,%d,%d,%d,%.4f,%d%n",
                        arc.fromStopId, arc.toStopId, arc.routeId, arc.orientation,
                        arc.averageSpeed, arc.sampleCount);
            }
        } catch (IOException e) {
            System.err.println("✗ Error guardando arc_speeds.csv: " + e.getMessage());
        }

        // 5. Guardar estadísticas de workers
        try (PrintWriter w = new PrintWriter(new FileWriter(resultDir + "/stats.txt"))) {
            ProcessingStats[] stats = masterProxy.getAllStats();

            for (ProcessingStats s : stats) {
                w.printf("Worker %d (%s)%n", s.nodeId, s.nodeName);
                w.printf("  Eventos procesados: %d%n", s.eventsProcessed);
                w.printf("  Arcos calculados: %d%n", s.arcsCalculated);
                w.printf("  Tiempo total: %d ms%n%n", s.totalTimeMs);
            }
        } catch (IOException e) {
            System.err.println("✗ Error guardando stats.txt: " + e.getMessage());
        }

        // 6. Guardar resumen general
        try (PrintWriter w = new PrintWriter(new FileWriter(resultDir + "/summary.txt"))) {
            w.printf("Total de arcos: %d%n", masterProxy.getAggregatedResults().length);
            w.printf("Workers activos: %d%n", workerCount);
            w.printf("Archivo procesado: %s%n", filePath);
            w.printf("Duración total: %.2f segundos%n", (end - start) / 1000.0);
        } catch (IOException e) {
            System.err.println("✗ Error guardando summary.txt: " + e.getMessage());
        }

        System.out.println("✓ Resultados guardados automáticamente en: " + resultDir + "\n");
    }

    private static void mostrarEstadisticas() {
        System.out.println("\n--- Estadísticas de Workers ---");

        try {
            ProcessingStats[] stats = masterProxy.getAllStats();

            if (stats.length == 0) {
                System.out.println("No hay workers registrados\n");
                return;
            }

            System.out.println();
            int totalEvents = 0;
            int totalArcs = 0;
            long totalTime = 0;

            for (ProcessingStats stat : stats) {
                System.out.printf("Worker %d (%s):%n", stat.nodeId, stat.nodeName);
                System.out.printf("  Eventos procesados: %,d%n", stat.eventsProcessed);
                System.out.printf("  Arcos calculados: %,d%n", stat.arcsCalculated);
                System.out.printf("  Tiempo total: %,d ms (%.2f seg)%n",
                        stat.totalTimeMs, stat.totalTimeMs / 1000.0);
                System.out.println();

                totalEvents += stat.eventsProcessed;
                totalArcs += stat.arcsCalculated;
                totalTime += stat.totalTimeMs;
            }

            System.out.println("--- TOTALES ---");
            System.out.printf("Workers activos: %d%n", stats.length);
            System.out.printf("Eventos totales: %,d%n", totalEvents);
            System.out.printf("Arcos totales: %,d%n", totalArcs);
            System.out.printf("Tiempo acumulado: %,d ms (%.2f seg)%n",
                    totalTime, totalTime / 1000.0);
            System.out.println();

        } catch (Exception e) {
            System.err.println("Error obteniendo estadísticas: " + e.getMessage() + "\n");
        }
    }

    private static void mostrarResumenResultados() {
        System.out.println("\n--- Resumen de Resultados ---");

        try {
            ArcSpeed[] results = masterProxy.getAggregatedResults();

            System.out.printf("\nArcos únicos calculados: %,d%n", results.length);

            if (results.length > 0) {
                // Calcular estadísticas
                double sumSpeed = 0;
                int maxSamples = 0;
                double maxSpeed = 0;
                double minSpeed = Double.MAX_VALUE;

                for (ArcSpeed arc : results) {
                    sumSpeed += arc.averageSpeed;
                    maxSamples = Math.max(maxSamples, arc.sampleCount);
                    maxSpeed = Math.max(maxSpeed, arc.averageSpeed);
                    minSpeed = Math.min(minSpeed, arc.averageSpeed);
                }

                System.out.printf("Velocidad promedio global: %.2f km/h%n", sumSpeed / results.length);
                System.out.printf("Velocidad máxima: %.2f km/h%n", maxSpeed);
                System.out.printf("Velocidad mínima: %.2f km/h%n", minSpeed);
                System.out.printf("Máximo de muestras en un arco: %d%n", maxSamples);

                // Mostrar algunos ejemplos
                System.out.println("\nPrimeros 5 arcos:");
                for (int i = 0; i < Math.min(5, results.length); i++) {
                    ArcSpeed arc = results[i];
                    System.out.printf("  Arco %d→%d (Ruta %d, Ori %d): %.2f km/h (%d muestras)%n",
                            arc.fromStopId, arc.toStopId, arc.routeId, arc.orientation,
                            arc.averageSpeed, arc.sampleCount);
                }
            }

            System.out.println();

        } catch (Exception e) {
            System.err.println("Error obteniendo resultados: " + e.getMessage() + "\n");
        }
    }

    private static void exportarResultados(Scanner scanner) {
        System.out.println("\n--- Exportar Resultados ---");
        System.out.print("Nombre del archivo (default: arc_speeds.csv): ");

        String fileName = scanner.nextLine().trim();
        if (fileName.isEmpty()) {
            fileName = "arc_speeds.csv";
        }

        try {
            ArcSpeed[] results = masterProxy.getAggregatedResults();

            try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
                writer.println("fromStopId,toStopId,routeId,orientation,averageSpeed,sampleCount");

                for (ArcSpeed arc : results) {
                    writer.printf("%d,%d,%d,%d,%.4f,%d%n",
                            arc.fromStopId, arc.toStopId, arc.routeId, arc.orientation,
                            arc.averageSpeed, arc.sampleCount);
                }
            }

            System.out.printf("✓ Resultados exportados a: %s%n", fileName);
            System.out.printf("✓ Total de arcos: %,d%n%n", results.length);

        } catch (Exception e) {
            System.err.println("Error exportando resultados: " + e.getMessage() + "\n");
        }
    }

    private static void shutdown() {
        System.out.println("\nCerrando Master Node...");
        if (communicator != null) {
            communicator.shutdown();
        }
        System.out.println("¡Hasta luego!");
    }
}