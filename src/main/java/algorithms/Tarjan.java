package algorithms;

import graph.Edge;
import graph.Graph;
import metrics.Metrics;

import java.util.List;

public final class Tarjan {

    private Tarjan() {
    }

    public static SccResult run(Graph g, Metrics metrics) {
        if (!g.isDirected()) {
            throw new IllegalArgumentException("Tarjan needs a directed graph");
        }
        int n = g.n();
        int[] index = new int[n];
        int[] lowlink = new int[n];
        int[] componentOf = new int[n];
        boolean[] onStack = new boolean[n];
        int[] nextArc = new int[n];
        for (int i = 0; i < n; i++) {
            index[i] = -1;
            componentOf[i] = -1;
        }
        int[] tarjanStack = new int[n];
        int tsTop = 0;
        int[] callStack = new int[n];
        int counter = 0;
        int componentCount = 0;

        for (int s = 0; s < n; s++) {
            if (index[s] != -1) {
                continue;
            }
            int top = 0;
            callStack[top++] = s;
            index[s] = lowlink[s] = counter++;
            tarjanStack[tsTop++] = s;
            onStack[s] = true;
            metrics.trackDepth(top);

            while (top > 0) {
                int v = callStack[top - 1];
                List<Edge> out = g.adj(v);
                if (nextArc[v] < out.size()) {
                    int w = out.get(nextArc[v]++).to();
                    metrics.edgeScans++;
                    if (index[w] == -1) {
                        index[w] = lowlink[w] = counter++;
                        tarjanStack[tsTop++] = w;
                        onStack[w] = true;
                        callStack[top++] = w;
                        metrics.trackDepth(top);
                    } else if (onStack[w]) {
                        metrics.comparisons++;
                        lowlink[v] = Math.min(lowlink[v], index[w]);
                    }
                } else {
                    if (lowlink[v] == index[v]) {
                        int x;
                        do {
                            x = tarjanStack[--tsTop];
                            onStack[x] = false;
                            componentOf[x] = componentCount;
                        } while (x != v);
                        componentCount++;
                    }
                    top--;
                    if (top > 0) {
                        int parent = callStack[top - 1];
                        metrics.comparisons++;
                        lowlink[parent] = Math.min(lowlink[parent], lowlink[v]);
                    }
                }
            }
        }
        return new SccResult(componentCount, componentOf);
    }
}
