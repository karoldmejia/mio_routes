package org.example.service;

import org.example.model.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class GraphImageExporterService {

    public void exportGlobalGraph(GlobalGraph graph, String filePath) {

        int width = 2000;
        int height = 1600;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        // Fondo blanco
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Obtener bounds de lat/lon para escalar

        final double[] minLat = { Double.MAX_VALUE };
        final double[] maxLat = { Double.MIN_VALUE };
        final double[] minLon = { Double.MAX_VALUE };
        final double[] maxLon = { Double.MIN_VALUE };

        // Calcular min/max
        for (Stop s : graph.getStops()) {
            minLat[0] = Math.min(minLat[0], s.getLat());
            maxLat[0] = Math.max(maxLat[0], s.getLat());
            minLon[0] = Math.min(minLon[0], s.getLon());
            maxLon[0] = Math.max(maxLon[0], s.getLon());
        }

        double latRange = maxLat[0] - minLat[0];
        double lonRange = maxLon[0] - minLon[0];

        double padding = 80;

        // 2. Función local para convertir lat/lon a pixel
        class P {
            int x(double lon) {
                return (int) ((lon - minLon[0]) / (maxLon[0] - minLon[0]) * (width - 2 * padding) + padding);
            }
            int y(double lat) {
                return (int) ((maxLat[0] - lat) / (maxLat[0] - minLat[0]) * (height - 2 * padding) + padding);
            }
        }

        P proj = new P();

        // 3. Colores por ruta
        Random r = new Random(42);
        Map<Integer, Color> routeColors = new HashMap<>();

        for (Arc a : graph.getArcs()) {
            int id = a.getRouteId();
            routeColors.putIfAbsent(
                    id,
                    new Color(r.nextInt(200), r.nextInt(200), r.nextInt(200))
            );
        }

        // 4. Dibujar arcos
        for (Arc a : graph.getArcs()) {

            Stop from = a.getFrom();
            Stop to = a.getTo();

            int x1 = proj.x(from.getLon());
            int y1 = proj.y(from.getLat());
            int x2 = proj.x(to.getLon());
            int y2 = proj.y(to.getLat());

            g.setColor(routeColors.get(a.getRouteId()));
            g.setStroke(new BasicStroke(2.0f));

            g.draw(new Line2D.Double(x1, y1, x2, y2));
        }

        // 5. Dibujar nodos
        g.setColor(Color.BLACK);

        for (Stop s : graph.getStops()) {
            int x = proj.x(s.getLon());
            int y = proj.y(s.getLat());

            g.fillOval(x - 5, y - 5, 10, 10);

            g.drawString(s.getShortName(), x + 6, y - 6);
        }

        g.dispose();

        try {
            ImageIO.write(img, "jpg", new File(filePath));
            System.out.println("Imagen guardada en: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
