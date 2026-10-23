package algorithms;

import graph.Edge;
import graph.Graph;
import metrics.Metrics;

import java.util.Arrays;

public final class Kahn {

    private Kahn() {
    }

    public record Result(int[] order, int processed, boolean hasCycle) {
    }

    public static Result run(Graph g, Metrics metrics) {
        if (!g.isDirected()) {
            throw new IllegalArgumentException("a topological order needs a directed graph");
        }
        int n = g.n();
        int[] inDegree = new int[n];
        int[] queue = new int[n];
        int head = 0;
        int tail = 0;

        for (int v = 0; v < n; v++) {
            inDegree[v] = g.inDegree(v);
            if (inDegree[v] == 0) {
                queue[tail++] = v;
            }
        }
        while (head < tail) {
            int u = queue[head++];
            for (Edge e : g.adj(u)) {
                metrics.edgeScans++;
                if (--inDegree[e.to()] == 0) {
                    queue[tail++] = e.to();
                }
            }
        }

        boolean hasCycle = tail < n;
        int[] order = hasCycle ? null : Arrays.copyOf(queue, n);
        return new Result(order, tail, hasCycle);
    }

    public static boolean isValidOrder(Graph g, int[] order) {
        int n = g.n();
        if (order == null || order.length != n) {
            return false;
        }
        int[] pos = new int[n];
        Arrays.fill(pos, -1);
        for (int i = 0; i < n; i++) {
            int v = order[i];
            if (v < 0 || v >= n || pos[v] != -1) {
                return false;
            }
            pos[v] = i;
        }
        for (int i = 0; i < g.m(); i++) {
            if (pos[g.edgeFrom(i)] >= pos[g.edgeTo(i)]) {
                return false;
            }
        }
        return true;
    }
}
