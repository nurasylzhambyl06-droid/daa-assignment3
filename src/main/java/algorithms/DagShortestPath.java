package algorithms;

import graph.Edge;
import graph.Graph;
import metrics.Metrics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class DagShortestPath {

    public static final long INF = Long.MAX_VALUE;

    private DagShortestPath() {
    }

    public record Result(long[] dist, int[] parent, long sum) {

        public List<Integer> pathTo(int v) {
            List<Integer> path = new ArrayList<>();
            for (int x = v; x != -1; x = parent[x]) {
                path.add(x);
            }
            Collections.reverse(path);
            return path;
        }
    }

    public static Result run(Graph g, int[] order, Metrics metrics) {
        if (!g.isDirected()) {
            throw new IllegalArgumentException("DAG shortest paths need a directed graph");
        }
        int n = g.n();
        if (order == null || order.length != n) {
            throw new IllegalArgumentException("a complete topological order is required (is the graph really acyclic?)");
        }

        long[] dist = new long[n];
        int[] parent = new int[n];
        Arrays.fill(dist, INF);
        Arrays.fill(parent, -1);
        for (int v = 0; v < n; v++) {
            if (g.inDegree(v) == 0) {
                dist[v] = 0;
            }
        }

        for (int u : order) {
            if (dist[u] == INF) {
                continue;
            }
            for (Edge e : g.adj(u)) {
                metrics.edgeScans++;
                metrics.comparisons++;
                long candidate = dist[u] + e.weight();
                if (candidate < dist[e.to()]) {
                    dist[e.to()] = candidate;
                    parent[e.to()] = u;
                }
            }
        }

        long sum = 0;
        for (long d : dist) {
            if (d != INF) {
                sum += d;
            }
        }
        return new Result(dist, parent, sum);
    }
}
