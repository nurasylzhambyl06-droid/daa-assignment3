package algorithms;

import graph.Edge;
import graph.Graph;
import metrics.Metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public final class Prim {

    private Prim() {
    }

    /** A queue entry: "the edge from -> to with this weight". */
    private record Candidate(int weight, int from, int to) {
    }

    public static MstResult run(Graph g, Metrics metrics) {
        if (g.isDirected()) {
            throw new IllegalArgumentException("Prim needs an undirected graph");
        }
        int n = g.n();
        boolean[] inTree = new boolean[n];
        PriorityQueue<Candidate> pq = new PriorityQueue<>((a, b) -> {
            metrics.comparisons++;
            return Integer.compare(a.weight(), b.weight());
        });

        List<int[]> tree = new ArrayList<>();
        long total = 0;
        int components = 0;

        for (int start = 0; start < n; start++) {
            if (inTree[start]) {
                continue;
            }
            components++;
            addVertex(g, start, inTree, pq, metrics);
            while (!pq.isEmpty()) {
                Candidate c = pq.poll();
                if (inTree[c.to()]) {
                    continue;
                }
                tree.add(new int[]{c.from(), c.to(), c.weight()});
                total += c.weight();
                addVertex(g, c.to(), inTree, pq, metrics);
            }
        }
        return new MstResult(total, components, tree);
    }

    private static void addVertex(Graph g, int v, boolean[] inTree, PriorityQueue<Candidate> pq, Metrics metrics) {
        inTree[v] = true;
        for (Edge e : g.adj(v)) {
            metrics.edgeScans++;
            if (!inTree[e.to()]) {
                pq.add(new Candidate(e.weight(), v, e.to()));
            }
        }
    }
}
