package org.example.service;

import org.example.model.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class GraphImageExporterService {

    public void exportGlobalGraph(GlobalGraph graph, String filePath) {

        int width = 15000;
        int height = 15000;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        // Fondo blanco
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Encontrar bounds
        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLon = -Double.MAX_VALUE;

        for (Stop s : graph.getStops()) {
            minLat = Math.min(minLat, s.getLat());
            maxLat = Math.max(maxLat, s.getLat());
            minLon = Math.min(minLon, s.getLon());
            maxLon = Math.max(maxLon, s.getLon());
        }

        double padding = 80.0;

        double latRange = Math.max(1e-9, maxLat - minLat);
        double lonRange = Math.max(1e-9, maxLon - minLon);

        double scaleXfit = (width  - 2.0 * padding) / lonRange;
        double scaleYfit = (height - 2.0 * padding) / latRange;
        double scaleFit  = Math.min(scaleXfit, scaleYfit);

        double maxZoomFactor = 2.5;
        double maxScale = scaleFit * maxZoomFactor;

        double scale = Math.min(scaleFit, maxScale);

        double projW = lonRange * scale;
        double projH = latRange * scale;

        double offsetX = (width  - projW) / 2.0;
        double offsetY = (height - projH) / 2.0;

        final double fMinLat = minLat;
        final double fMaxLat = maxLat;
        final double fMinLon = minLon;
        final double fMaxLon = maxLon;

        // Proyector
        class P {
            int x(double lon) { return (int) Math.round((lon - fMinLon) * scale + offsetX); }
            int y(double lat) { return (int) Math.round((fMaxLat - lat) * scale + offsetY); }
        }
        P proj = new P();

        Map<Integer, Map<Integer, Map<Integer, List<Arc>>>> grouped = new HashMap<>();

        for (Arc a : graph.getArcs()) {
            grouped
                    .computeIfAbsent(a.getRouteId(), rid -> new HashMap<>())
                    .computeIfAbsent(a.getVariant(), vid -> new HashMap<>())
                    .computeIfAbsent(a.getOrientation(), o -> new ArrayList<>())
                    .add(a);
        }

        // 2. Colores por ruta
        Random r = new Random(42);
        Map<Integer, Color> routeColors = new HashMap<>();

        for (Integer routeId : grouped.keySet()) {
            routeColors.putIfAbsent(
                    routeId,
                    new Color(r.nextInt(200), r.nextInt(200), r.nextInt(200))
            );
        }

        for (Integer routeId : grouped.keySet()) {
            Color c = routeColors.get(routeId);
            g.setColor(c);

            Map<Integer, Map<Integer, List<Arc>>> variants = grouped.get(routeId);

            for (Integer variantId : variants.keySet()) {
                Map<Integer, List<Arc>> orientations = variants.get(variantId);

                for (Integer orientation : orientations.keySet()) {
                    List<Arc> arcs = orientations.get(orientation);

                    g.setStroke(new BasicStroke(2.0f));

                    for (Arc a : arcs) {
                        int x1 = proj.x(a.getFrom().getLon());
                        int y1 = proj.y(a.getFrom().getLat());
                        int x2 = proj.x(a.getTo().getLon());
                        int y2 = proj.y(a.getTo().getLat());

                        g.draw(new Line2D.Double(x1, y1, x2, y2));
                    }
                }
            }
        }

        // 3. Dibujar nodos
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

    public void exportRouteGraph(RouteGraph graph, String filePath) {

        int width = 5000;
        int height = 5000;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // -----------------------------------------
        // 1. Calcular bounds SOLO con esta ruta
        // -----------------------------------------
        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLon = -Double.MAX_VALUE;

        for (Stop s : graph.getNodes()) {
            minLat = Math.min(minLat, s.getLat());
            maxLat = Math.max(maxLat, s.getLat());
            minLon = Math.min(minLon, s.getLon());
            maxLon = Math.max(maxLon, s.getLon());
        }

        double padding = 80.0;

        double latRange = Math.max(1e-9, maxLat - minLat);
        double lonRange = Math.max(1e-9, maxLon - minLon);

        double scaleXfit = (width  - 2.0 * padding) / lonRange;
        double scaleYfit = (height - 2.0 * padding) / latRange;

        double scale = Math.min(scaleXfit, scaleYfit);

        double projW = lonRange * scale;
        double projH = latRange * scale;

        double offsetX = (width  - projW) / 2.0;
        double offsetY = (height - projH) / 2.0;

        final double fMinLat = minLat;
        final double fMaxLat = maxLat;
        final double fMinLon = minLon;

        class P {
            int x(double lon) { return (int) ((lon - fMinLon) * scale + offsetX); }
            int y(double lat) { return (int) ((fMaxLat - lat) * scale + offsetY); }
        }
        P proj = new P();

        g.setStroke(new BasicStroke(3.0f));
        g.setColor(Color.BLUE);

        for (Arc a : graph.getOutboundArcs()) {
            g.draw(new Line2D.Double(
                    proj.x(a.getFrom().getLon()),
                    proj.y(a.getFrom().getLat()),
                    proj.x(a.getTo().getLon()),
                    proj.y(a.getTo().getLat())
            ));
        }

        g.setColor(Color.RED);
        for (Arc a : graph.getInboundArcs()) {
            g.draw(new Line2D.Double(
                    proj.x(a.getFrom().getLon()),
                    proj.y(a.getFrom().getLat()),
                    proj.x(a.getTo().getLon()),
                    proj.y(a.getTo().getLat())
            ));
        }

        g.setColor(Color.BLACK);
        for (Stop s : graph.getNodes()) {
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
