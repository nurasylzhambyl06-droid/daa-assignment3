package algorithms;

import graph.Graph;
import graph.GraphGenerator;
import graph.GraphIO;
import metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MstTest {

    private static Graph randomConnectedGraph(Random rnd) {
        int n = 2 + rnd.nextInt(59);
        Graph g = new Graph(n, false);
        for (int v = 1; v < n; v++) {
            g.addEdge(rnd.nextInt(v), v, 1 + rnd.nextInt(50));          // random spanning tree keeps it connected
        }
        int extra = rnd.nextInt(3 * n);
        for (int i = 0; i < extra; i++) {
            g.addEdge(rnd.nextInt(n), rnd.nextInt(n), 1 + rnd.nextInt(50));   // may be a self-loop or a parallel edge
        }
        return g;
    }

    private static long matrixPrim(Graph g) {
        int n = g.n();
        int none = Integer.MAX_VALUE;
        int[][] w = new int[n][n];
        for (int[] row : w) {
            java.util.Arrays.fill(row, none);
        }
        for (int i = 0; i < g.m(); i++) {
            int u = g.edgeFrom(i);
            int v = g.edgeTo(i);
            if (u != v) {
                w[u][v] = Math.min(w[u][v], g.edgeWeight(i));
                w[v][u] = w[u][v];
            }
        }
        boolean[] in = new boolean[n];
        int[] best = new int[n];
        java.util.Arrays.fill(best, none);
        best[0] = 0;
        long total = 0;
        for (int step = 0; step < n; step++) {
            int u = -1;
            for (int v = 0; v < n; v++) {
                if (!in[v] && (u == -1 || best[v] < best[u])) {
                    u = v;
                }
            }
            in[u] = true;
            total += best[u];
            for (int v = 0; v < n; v++) {
                if (!in[v] && w[u][v] < best[v]) {
                    best[v] = w[u][v];
                }
            }
        }
        return total;
    }

    @Test
    void kruskalAndPrimAgreeOnHundredRandomConnectedGraphs() {
        Random rnd = new Random(2024);
        for (int round = 0; round < 100; round++) {
            Graph g = randomConnectedGraph(rnd);
            MstResult k = Kruskal.run(g, new Metrics());
            MstResult p = Prim.run(g, new Metrics());

            assertEquals(k.totalWeight(), p.totalWeight(), "round " + round);
            assertEquals(matrixPrim(g), k.totalWeight(), "round " + round);
            assertEquals(g.n() - 1, k.edgeCount());              // a spanning tree has exactly n-1 edges
            assertEquals(g.n() - 1, p.edgeCount());
            assertEquals(1, k.components());
            assertEquals(1, p.components());
        }
    }

    @Test
    void theChosenEdgesReallyFormASpanningTreeWhoseWeightIsTheReportedTotal() {
        Random rnd = new Random(7);
        for (int round = 0; round < 30; round++) {
            Graph g = randomConnectedGraph(rnd);
            for (MstResult r : new MstResult[]{Kruskal.run(g, new Metrics()), Prim.run(g, new Metrics())}) {
                DSU dsu = new DSU(g.n());
                long sum = 0;
                for (int[] e : r.edges()) {
                    assertTrue(dsu.union(e[0], e[1]), "an edge closed a cycle");   // no cycle
                    sum += e[2];
                }
                assertEquals(1, dsu.components());                                   // connects everything
                assertEquals(sum, r.totalWeight());
            }
        }
    }

    @Test
    void smallConnectedFileHasWeightFourteen() throws IOException {
        Graph g = GraphIO.read(Path.of("data", "small_connected.txt"));
        assertEquals(14, Kruskal.run(g, new Metrics()).totalWeight());
        assertEquals(14, Prim.run(g, new Metrics()).totalWeight());
    }

    /** MST must return a forest, not throw, and report the number of components. */
    @Test
    void disconnectedGraphGivesAForestAndTheNumberOfComponents() throws IOException {
        Graph g = GraphIO.read(Path.of("data", "disconnected.txt"));
        for (MstResult r : new MstResult[]{Kruskal.run(g, new Metrics()), Prim.run(g, new Metrics())}) {
            assertEquals(9, r.totalWeight());
            assertEquals(5, r.components());
            assertEquals(3, r.edgeCount());          // n - components = 8 - 5
        }
    }

    @Test
    void graphWithoutEdgesIsAForestOfSingletons() {
        Graph g = new Graph(5, false);
        for (MstResult r : new MstResult[]{Kruskal.run(g, new Metrics()), Prim.run(g, new Metrics())}) {
            assertEquals(0, r.totalWeight());
            assertEquals(5, r.components());
            assertEquals(0, r.edgeCount());
        }
    }

    @Test
    void emptyGraphAndSingleVertexDoNotCrash() {
        for (int n = 0; n <= 1; n++) {
            Graph g = new Graph(n, false);
            assertEquals(n, Kruskal.run(g, new Metrics()).components());
            assertEquals(n, Prim.run(g, new Metrics()).components());
        }
    }

    @Test
    void selfLoopsAndParallelEdgesAreIgnoredCorrectly() {
        Graph g = new Graph(2, false);
        g.addEdge(0, 0, 1);       // the cheapest edge of all is a self-loop - it must NOT be taken
        g.addEdge(0, 1, 9);
        g.addEdge(0, 1, 5);       // cheaper parallel edge wins
        assertEquals(5, Kruskal.run(g, new Metrics()).totalWeight());
        assertEquals(5, Prim.run(g, new Metrics()).totalWeight());
    }

    @Test
    void negativeWeightsAreFineForMst() {
        Graph g = new Graph(3, false);
        g.addEdge(0, 1, -4);
        g.addEdge(1, 2, -2);
        g.addEdge(0, 2, 10);
        assertEquals(-6, Kruskal.run(g, new Metrics()).totalWeight());
        assertEquals(-6, Prim.run(g, new Metrics()).totalWeight());
    }

    @Test
    void directedGraphsAreRejected() {
        Graph g = new Graph(3, true);
        assertThrows(IllegalArgumentException.class, () -> Kruskal.run(g, new Metrics()));
        assertThrows(IllegalArgumentException.class, () -> Prim.run(g, new Metrics()));
    }

    @Test
    void sortingByWeightIsOrderedStableAndCounted() {
        Graph g = new Graph(6, false);
        int[] weights = {5, 3, 5, 1, 3, 5, 2};
        for (int i = 0; i < weights.length; i++) {
            g.addEdge(i % 6, (i + 1) % 6, weights[i]);
        }
        Metrics metrics = new Metrics();
        int[] order = Kruskal.sortEdgesByWeight(g, metrics);
        for (int i = 1; i < order.length; i++) {
            int prev = g.edgeWeight(order[i - 1]);
            int cur = g.edgeWeight(order[i]);
            assertTrue(prev <= cur);
            if (prev == cur) {
                assertTrue(order[i - 1] < order[i]);
            }
        }
        assertTrue(metrics.comparisons > 0);
    }

    @Test
    void countersAreCountedInsideTheAlgorithms() {
        Graph g = GraphGenerator.undirected(500, 5000);
        Metrics km = new Metrics();
        Kruskal.run(g, km);
        assertTrue(km.edgeScans > 0 && km.edgeScans <= g.m());
        assertTrue(km.dsuOps > 0);
        assertTrue(km.comparisons > 0);

        Metrics pm = new Metrics();
        Prim.run(g, pm);
        assertEquals(2L * g.m(), pm.edgeScans);
        assertEquals(0, pm.dsuOps);
        assertTrue(pm.comparisons > 0);
    }
}
