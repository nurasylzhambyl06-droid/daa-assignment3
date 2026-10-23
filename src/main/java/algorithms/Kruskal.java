package algorithms;

import graph.Graph;
import metrics.Metrics;

import java.util.ArrayList;
import java.util.List;

public final class Kruskal {

    private Kruskal() {
    }

    public static MstResult run(Graph g, Metrics metrics) {
        if (g.isDirected()) {
            throw new IllegalArgumentException("Kruskal needs an undirected graph");
        }
        int n = g.n();
        int[] order = sortEdgesByWeight(g, metrics);

        DSU dsu = new DSU(n);
        List<int[]> tree = new ArrayList<>();
        long total = 0;

        for (int idx = 0; idx < order.length; idx++) {
            if (n > 0 && tree.size() == n - 1) {
                break;
            }
            int e = order[idx];
            metrics.edgeScans++;
            int u = g.edgeFrom(e);
            int v = g.edgeTo(e);
            if (dsu.find(u) != dsu.find(v)) {
                dsu.union(u, v);
                int w = g.edgeWeight(e);
                tree.add(new int[]{u, v, w});
                total += w;
            }
        }
        metrics.dsuOps += dsu.getOps();
        return new MstResult(total, dsu.components(), tree);
    }

    static int[] sortEdgesByWeight(Graph g, Metrics metrics) {
        int m = g.m();
        int[] a = new int[m];
        int[] b = new int[m];
        for (int i = 0; i < m; i++) {
            a[i] = i;
        }
        for (int width = 1; width < m; width *= 2) {
            for (int lo = 0; lo < m; lo += 2 * width) {
                int mid = Math.min(lo + width, m);
                int hi = Math.min(lo + 2 * width, m);
                int i = lo;
                int j = mid;
                int k = lo;
                while (i < mid && j < hi) {
                    metrics.comparisons++;
                    if (g.edgeWeight(a[i]) <= g.edgeWeight(a[j])) {
                        b[k++] = a[i++];
                    } else {
                        b[k++] = a[j++];
                    }
                }
                while (i < mid) {
                    b[k++] = a[i++];
                }
                while (j < hi) {
                    b[k++] = a[j++];
                }
            }
            int[] tmp = a;
            a = b;
            b = tmp;
        }
        return a;
    }
}
