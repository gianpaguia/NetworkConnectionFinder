package com.networkfinder.ui;

import com.networkfinder.dsa.Graph;
import com.networkfinder.dsa.StudentNode;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.*;

public class GraphVisualizerCanvas extends Pane {
    private final Graph graph;
    private List<String> highlightedPath = Collections.emptyList();
    private java.util.function.Consumer<StudentNode> onNodeClicked;

    public GraphVisualizerCanvas(Graph graph) {
        this.graph = graph;
        setStyle("-fx-background-color: rgba(26, 26, 34, 0.80); -fx-border-color: rgba(255, 255, 255, 0.08); -fx-border-radius: 8; -fx-background-radius: 8;");
        setPrefSize(700, 480);
        setMinHeight(420);

        widthProperty().addListener((obs, old, val) -> redraw());
        heightProperty().addListener((obs, old, val) -> redraw());
    }

    public void setOnNodeClicked(java.util.function.Consumer<StudentNode> onNodeClicked) {
        this.onNodeClicked = onNodeClicked;
    }

    public void setHighlightedPath(List<String> path) {
        this.highlightedPath = path != null ? new ArrayList<>(path) : Collections.emptyList();
        redraw();
    }

    public void redraw() {
        getChildren().clear();

        Collection<StudentNode> nodes = graph.getAllNodes();
        if (nodes.isEmpty()) {
            Label empty = new Label("Network graph is currently empty.");
            empty.setStyle("-fx-text-fill: -fx-ink-soft; -fx-font-size: 13px;");
            empty.relocate(20, 20);
            getChildren().add(empty);
            return;
        }

        double width = getWidth() > 0 ? getWidth() : 700;
        double height = getHeight() > 0 ? getHeight() : 480;
        double centerX = width / 2.0;
        double centerY = height / 2.0;
        double radiusX = centerX - 90;
        if (radiusX < 80) radiusX = 80;
        double radiusY = centerY - 70;
        if (radiusY < 80) radiusY = 80;

        List<StudentNode> nodeList = new ArrayList<>(nodes);
        Map<String, double[]> nodePositions = new HashMap<>();

        int total = nodeList.size();
        for (int i = 0; i < total; i++) {
            double angle = (2 * Math.PI * i) / total - (Math.PI / 2);
            double x = centerX + radiusX * Math.cos(angle);
            double y = centerY + radiusY * Math.sin(angle);
            nodePositions.put(nodeList.get(i).getStudentId(), new double[]{x, y});
        }

        Set<String> drawnEdges = new HashSet<>();
        for (StudentNode uNode : nodeList) {
            String u = uNode.getStudentId();
            double[] posU = nodePositions.get(u);

            for (String v : graph.getNeighbors(u)) {
                String edgeKey = u.compareTo(v) < 0 ? u + "-" + v : v + "-" + u;
                if (!drawnEdges.contains(edgeKey) && nodePositions.containsKey(v)) {
                    drawnEdges.add(edgeKey);
                    double[] posV = nodePositions.get(v);

                    Line line = new Line(posU[0], posU[1], posV[0], posV[1]);
                    line.setStroke(Color.color(1.0, 1.0, 1.0, 0.12));
                    line.setStrokeWidth(1.8);
                    getChildren().add(line);
                }
            }
        }

        if (highlightedPath.size() > 1) {
            for (int i = 0; i < highlightedPath.size() - 1; i++) {
                String u = highlightedPath.get(i);
                String v = highlightedPath.get(i + 1);

                double[] posU = nodePositions.get(u);
                double[] posV = nodePositions.get(v);

                if (posU != null && posV != null) {
                    Line pathLine = new Line(posU[0], posU[1], posV[0], posV[1]);
                    pathLine.setStroke(Color.web("#60CDFF"));
                    pathLine.setStrokeWidth(4.5);
                    getChildren().add(pathLine);
                }
            }
        }

        for (StudentNode node : nodeList) {
            String id = node.getStudentId();
            double[] pos = nodePositions.get(id);

            boolean inPath = highlightedPath.contains(id);
            boolean isSource = !highlightedPath.isEmpty() && id.equals(highlightedPath.get(0));
            boolean isTarget = !highlightedPath.isEmpty() && id.equals(highlightedPath.get(highlightedPath.size() - 1));

            StackPane nodeCirclePane = new StackPane();
            nodeCirclePane.setPrefSize(48, 48);
            nodeCirclePane.setLayoutX(pos[0] - 24);
            nodeCirclePane.setLayoutY(pos[1] - 24);

            Circle circle = new Circle(22);
            circle.setStrokeWidth(2.0);

            Text label = new Text();
            label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));

            String[] parts = node.getFullName().split("\\s+");
            String initials = parts.length > 1 ? ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)) : parts[0].substring(0, Math.min(2, parts[0].length()));
            label.setText(initials.toUpperCase());

            if (isSource) {
                circle.setFill(Color.web("#60CDFF"));
                circle.setStroke(Color.web("#78D5FF"));
                label.setFill(Color.web("#0F172A"));
            } else if (isTarget) {
                circle.setFill(Color.web("#A855F7"));
                circle.setStroke(Color.web("#C084FC"));
                label.setFill(Color.WHITE);
            } else if (inPath) {
                circle.setFill(Color.color(0.38, 0.80, 1.0, 0.25));
                circle.setStroke(Color.web("#60CDFF"));
                label.setFill(Color.web("#60CDFF"));
            } else {
                circle.setFill(Color.web("#262632"));
                circle.setStroke(Color.color(1.0, 1.0, 1.0, 0.22));
                label.setFill(Color.WHITE);
            }

            nodeCirclePane.getChildren().addAll(circle, label);
            nodeCirclePane.setCursor(javafx.scene.Cursor.HAND);

            Label nameLabel = new Label(node.getFullName() + "\n" + id);
            nameLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 10));
            nameLabel.setAlignment(Pos.CENTER);
            nameLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            nameLabel.setStyle("-fx-text-fill: -fx-ink-soft;");
            nameLabel.setLayoutX(pos[0] - 50);
            nameLabel.setLayoutY(pos[1] + 24);
            nameLabel.setPrefWidth(100);
            nameLabel.setCursor(javafx.scene.Cursor.HAND);

            nodeCirclePane.setOnMouseClicked(e -> {
                if (onNodeClicked != null) onNodeClicked.accept(node);
            });
            nameLabel.setOnMouseClicked(e -> {
                if (onNodeClicked != null) onNodeClicked.accept(node);
            });

            String tooltipText = node.getFullName() + " (" + id + ")\n" +
                    "Course: " + (node.getProfile() != null ? node.getProfile().getCourse() : "N/A") + "\n" +
                    "Connections: " + graph.getDegree(id);
            Tooltip.install(nodeCirclePane, new Tooltip(tooltipText));

            getChildren().addAll(nodeCirclePane, nameLabel);
        }
    }
}
