package com.networkfinder.dsa;

import java.util.Collections;
import java.util.List;

public class PathResult {
    private final boolean reachable;
    private final List<String> path;
    private final int degreesOfSeparation;
    private final int nodesExplored;

    public PathResult(boolean reachable, List<String> path, int degreesOfSeparation, int nodesExplored) {
        this.reachable = reachable;
        this.path = path != null ? Collections.unmodifiableList(path) : Collections.emptyList();
        this.degreesOfSeparation = degreesOfSeparation;
        this.nodesExplored = nodesExplored;
    }

    public static PathResult unreachable(int nodesExplored) {
        return new PathResult(false, Collections.emptyList(), -1, nodesExplored);
    }

    public boolean isReachable() {
        return reachable;
    }

    public List<String> getPath() {
        return path;
    }

    public int getDegreesOfSeparation() {
        return degreesOfSeparation;
    }

    public int getNodesExplored() {
        return nodesExplored;
    }

    @Override
    public String toString() {
        if (!reachable) {
            return "No connection path found (Explored " + nodesExplored + " nodes).";
        }
        return "Path: " + String.join(" -> ", path) + " (" + degreesOfSeparation + " degree(s) of separation, " 
                + nodesExplored + " nodes explored)";
    }
}
