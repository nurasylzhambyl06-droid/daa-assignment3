package algorithms;

import graph.Edge;
import graph.Graph;
import metrics.Metrics;

import java.util.List;

public final class Kosaraju {

    private Kosaraju() {
    }

    public static SccResult run(Graph g, Metrics metrics) {
        if (!g.isDirected()) {
            throw new IllegalArgumentException("Kosaraju needs a directed graph");
        }
        int[] finishOrder = firstPass(g, metrics);
        Graph reversed = g.reversed();
        return secondPass(reversed, finishOrder, metrics);
    }

    private static int[] firstPass(Graph g, Metrics metrics) {
        int n = g.n();
        boolean[] visited = new boolean[n];
        int[] nextArc = new int[n];
        int[] stack = new int[n];
        int[] order = new int[n];
        int finished = 0;

        for (int s = 0; s < n; s++) {
            if (visited[s]) {
                continue;
            }
            int top = 0;
            stack[top++] = s;
            visited[s] = true;
            metrics.trackDepth(top);

            while (top > 0) {
                int v = stack[top - 1];
                List<Edge> out = g.adj(v);
                if (nextArc[v] < out.size()) {
                    Edge e = out.get(nextArc[v]++);
                    metrics.edgeScans++;
                    if (!visited[e.to()]) {
                        visited[e.to()] = true;
                        stack[top++] = e.to();
                        metrics.trackDepth(top);
                    }
                } else {
                    top--;
                    order[finished++] = v;
                }
            }
        }
        return order;
    }

    private static SccResult secondPass(Graph reversed, int[] finishOrder, Metrics metrics) {
        int n = reversed.n();
        int[] componentOf = new int[n];
        for (int i = 0; i < n; i++) {
            componentOf[i] = -1;
        }
        int[] stack = new int[n];
        int count = 0;

        for (int i = n - 1; i >= 0; i--) {
            int s = finishOrder[i];
            if (componentOf[s] != -1) {
                continue;
            }
            int top = 0;
            stack[top++] = s;
            componentOf[s] = count;
            metrics.trackDepth(top);
            while (top > 0) {
                int v = stack[--top];
                for (Edge e : reversed.adj(v)) {
                    metrics.edgeScans++;
                    if (componentOf[e.to()] == -1) {
                        componentOf[e.to()] = count;
                        stack[top++] = e.to();
                        metrics.trackDepth(top);
                    }
                }
            }
            count++;
        }
        return new SccResult(count, componentOf);
    }
}
