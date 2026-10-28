package com.networkfinder.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GraphConstellationCanvas extends Pane {

    private static final Color BG_DARK = Color.web("#141318");
    private static final Color ACCENT_CYAN = Color.web("#60CDFF");
    private static final Color ACCENT_VIOLET = Color.web("#A855F7");
    private static final Color ACCENT_MAGENTA = Color.web("#D946EF");
    private static final Color TEXT_WHITE = Color.web("#FFFFFF");
    private static final Color TEXT_MUTED = Color.web("#A2A2AE");

    private final Canvas canvas = new Canvas();
    private final List<double[]> nodes = new ArrayList<>();
    private final List<int[]> edges = new ArrayList<>();
    private final int[] pathIndices = new int[3];

    @SuppressWarnings("this-escape")

    public GraphConstellationCanvas() {
        getChildren().add(canvas);
        widthProperty().addListener((obs, old, val) -> redraw());
        heightProperty().addListener((obs, old, val) -> redraw());
        generateLayout();
    }

    private void generateLayout() {
        Random rand = new Random(20261007L);

        double[][] clusterCenters = {
                {0.26, 0.30}, {0.60, 0.52}, {0.42, 0.76}, {0.78, 0.26}
        };

        for (double[] center : clusterCenters) {
            int count = 4 + rand.nextInt(3);
            for (int i = 0; i < count; i++) {
                double angle = rand.nextDouble() * Math.PI * 2;
                double dist = 0.05 + rand.nextDouble() * 0.10;
                double rx = clamp(center[0] + Math.cos(angle) * dist);
                double ry = clamp(center[1] + Math.sin(angle) * dist * 0.9);
                nodes.add(new double[]{rx, ry, 0.5 + rand.nextDouble() * 0.8});
            }
        }

        for (int i = 0; i < 4; i++) {
            nodes.add(new double[]{
                    clamp(0.10 + rand.nextDouble() * 0.80),
                    clamp(0.10 + rand.nextDouble() * 0.80),
                    0.4 + rand.nextDouble() * 0.5
            });
        }

        pathIndices[0] = nodes.size();
        nodes.add(new double[]{0.30, 0.24, 1.35});
        pathIndices[1] = nodes.size();
        nodes.add(new double[]{0.50, 0.42, 1.35});
        pathIndices[2] = nodes.size();
        nodes.add(new double[]{0.70, 0.60, 1.35});

        double threshold = 0.14;
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                boolean isPathEdge = (i == pathIndices[0] && j == pathIndices[1])
                        || (i == pathIndices[1] && j == pathIndices[2]);
                if (isPathEdge) continue;
                double d = dist(nodes.get(i), nodes.get(j));
                if (d < threshold && edges.size() < 26) {
                    edges.add(new int[]{i, j});
                }
            }
        }

        for (int p : pathIndices) {
            int nearest = -1;
            double best = Double.MAX_VALUE;
            for (int i = 0; i < nodes.size(); i++) {
                if (i == p || isPathNode(i)) continue;
                double d = dist(nodes.get(p), nodes.get(i));
                if (d < best) {
                    best = d;
                    nearest = i;
                }
            }
            if (nearest >= 0 && best < threshold * 1.6) {
                edges.add(new int[]{p, nearest});
            }
        }
    }

    private boolean isPathNode(int i) {
        return i == pathIndices[0] || i == pathIndices[1] || i == pathIndices[2];
    }

    private double clamp(double v) {
        return Math.max(0.07, Math.min(0.93, v));
    }

    private double dist(double[] a, double[] b) {
        double dx = a[0] - b[0];
        double dy = a[1] - b[1];
        return Math.sqrt(dx * dx + dy * dy);
    }

    private void redraw() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;

        canvas.setWidth(w);
        canvas.setHeight(h);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        gc.setFill(BG_DARK);
        gc.fillRect(0, 0, w, h);

        double minDim = Math.min(w, h);

        // Ambient plum/magenta and cyan glows matching Windows wallpaper bloom
        gc.setFill(new RadialGradient(0, 0, 0.25, 0.22, 0.65, true, CycleMethod.NO_CYCLE,
                new Stop(0, ACCENT_MAGENTA.deriveColor(0, 1, 1, 0.22)),
                new Stop(1, ACCENT_MAGENTA.deriveColor(0, 1, 1, 0))));
        gc.fillRect(0, 0, w, h);

        gc.setFill(new RadialGradient(0, 0, 0.75, 0.75, 0.60, true, CycleMethod.NO_CYCLE,
                new Stop(0, ACCENT_CYAN.deriveColor(0, 1, 1, 0.18)),
                new Stop(1, ACCENT_CYAN.deriveColor(0, 1, 1, 0))));
        gc.fillRect(0, 0, w, h);

        // Network Edges
        gc.setStroke(Color.color(1.0, 1.0, 1.0, 0.14));
        gc.setLineWidth(1.0);
        for (int[] edge : edges) {
            double[] a = nodes.get(edge[0]);
            double[] b = nodes.get(edge[1]);
            gc.strokeLine(a[0] * w, a[1] * h, b[0] * w, b[1] * h);
        }

        // Regular Nodes
        for (int i = 0; i < nodes.size(); i++) {
            double[] n = nodes.get(i);
            double r = minDim * 0.012 * n[2];
            double x = n[0] * w;
            double y = n[1] * h;
            gc.setFill(Color.color(1.0, 1.0, 1.0, 0.35 + n[2] * 0.35));
            gc.fillOval(x - r, y - r, r * 2, r * 2);
        }

        // Highlighted BFS Path
        double[] p0 = nodes.get(pathIndices[0]);
        double[] p1 = nodes.get(pathIndices[1]);
        double[] p2 = nodes.get(pathIndices[2]);

        gc.setStroke(ACCENT_CYAN);
        gc.setLineWidth(minDim * 0.007);
        gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        gc.strokeLine(p0[0] * w, p0[1] * h, p1[0] * w, p1[1] * h);
        gc.strokeLine(p1[0] * w, p1[1] * h, p2[0] * w, p2[1] * h);

        for (int i = 0; i < 3; i++) {
            double[] n = nodes.get(pathIndices[i]);
            double r = minDim * 0.022;
            double x = n[0] * w;
            double y = n[1] * h;
            gc.setFill(i == 1 ? ACCENT_VIOLET : ACCENT_CYAN);
            gc.fillOval(x - r, y - r, r * 2, r * 2);
            gc.setStroke(BG_DARK);
            gc.setLineWidth(2.5);
            gc.strokeOval(x - r, y - r, r * 2, r * 2);
        }

        gc.setFill(TEXT_WHITE);
        gc.setFont(javafx.scene.text.Font.font("Segoe UI", javafx.scene.text.FontWeight.SEMI_BOLD, minDim * 0.028));
        gc.fillText("1st degree", p0[0] * w + minDim * 0.030, p0[1] * h - minDim * 0.030);
        gc.fillText("2nd degree", p2[0] * w - minDim * 0.115, p2[1] * h + minDim * 0.055);

    }
}
