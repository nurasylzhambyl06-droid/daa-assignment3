package algorithms;

import graph.Graph;
import graph.GraphIO;
import metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SccTest {

    private static Graph randomDirectedGraph(Random rnd, int n) {
        Graph g = new Graph(n, true);
        int m = rnd.nextInt(3 * n + 1);
        for (int i = 0; i < m; i++) {
            g.addEdge(rnd.nextInt(n), rnd.nextInt(n), 1 + rnd.nextInt(9));   // self-loops and parallel edges possible
        }
        return g;
    }

    private static boolean[][] reachability(Graph g) {
        int n = g.n();
        boolean[][] reach = new boolean[n][n];
        for (int s = 0; s < n; s++) {
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            reach[s][s] = true;
            queue.add(s);
            while (!queue.isEmpty()) {
                int u = queue.poll();
                for (graph.Edge e : g.adj(u)) {
                    if (!reach[s][e.to()]) {
                        reach[s][e.to()] = true;
                        queue.add(e.to());
                    }
                }
            }
        }
        return reach;
    }

    private static boolean[][] bruteForceSameScc(Graph g) {
        int n = g.n();
        boolean[][] forward = reachability(g);
        boolean[][] backward = reachability(g.reversed());
        boolean[][] same = new boolean[n][n];
        for (int u = 0; u < n; u++) {
            for (int v = 0; v < n; v++) {
                same[u][v] = forward[u][v] && backward[u][v];
            }
        }
        return same;
    }

    private static int bruteForceComponentCount(boolean[][] same) {
        int n = same.length;
        int count = 0;
        for (int v = 0; v < n; v++) {
            boolean firstOfItsClass = true;
            for (int u = 0; u < v; u++) {
                if (same[u][v]) {
                    firstOfItsClass = false;
                    break;
                }
            }
            if (firstOfItsClass) {
                count++;
            }
        }
        return count;
    }

    private static void assertMatches(boolean[][] same, SccResult r, String message) {
        int n = same.length;
        for (int u = 0; u < n; u++) {
            for (int v = 0; v < n; v++) {
                assertEquals(same[u][v], r.componentOf()[u] == r.componentOf()[v], message + " pair " + u + "," + v);
            }
        }
        assertEquals(bruteForceComponentCount(same), r.componentCount(), message);
    }

    @Test
    void kosarajuMatchesBruteForceOnRandomGraphsUpTo200Vertices() {
        Random rnd = new Random(11);
        for (int round = 0; round < 100; round++) {
            Graph g = randomDirectedGraph(rnd, 1 + rnd.nextInt(200));
            assertMatches(bruteForceSameScc(g), Kosaraju.run(g, new Metrics()), "round " + round);
        }
    }

    @Test
    void tarjanMatchesBruteForceAndKosaraju() {
        Random rnd = new Random(12);
        for (int round = 0; round < 100; round++) {
            Graph g = randomDirectedGraph(rnd, 1 + rnd.nextInt(200));
            SccResult t = Tarjan.run(g, new Metrics());
            assertMatches(bruteForceSameScc(g), t, "round " + round);
            assertTrue(t.samePartitionAs(Kosaraju.run(g, new Metrics())), "round " + round);
        }
    }

    @Test
    void partitionComparisonIgnoresTheNumberingOfComponents() {
        SccResult a = new SccResult(2, new int[]{0, 0, 1, 1});
        SccResult renamed = new SccResult(2, new int[]{1, 1, 0, 0});     // same groups, ids swapped
        SccResult different = new SccResult(2, new int[]{0, 1, 1, 1});   // other groups
        assertTrue(a.samePartitionAs(renamed));
        assertFalse(a.samePartitionAs(different));
        assertFalse(a.samePartitionAs(new SccResult(1, new int[]{0, 0, 0, 0})));
    }

    @Test
    void cyclicFileHasTwoComponentsOfThree() throws IOException {
        Graph g = GraphIO.read(Path.of("data", "cyclic.txt"));
        for (SccResult r : new SccResult[]{Kosaraju.run(g, new Metrics()), Tarjan.run(g, new Metrics())}) {
            assertEquals(2, r.componentCount());
            int[] c = r.componentOf();
            assertTrue(c[0] == c[1] && c[1] == c[2]);
            assertTrue(c[3] == c[4] && c[4] == c[5]);
            assertTrue(c[0] != c[3]);
        }
    }

    @Test
    void dagHasOneComponentPerVertex() throws IOException {
        Graph g = GraphIO.read(Path.of("data", "dag.txt"));
        assertEquals(6, Kosaraju.run(g, new Metrics()).componentCount());
        assertEquals(6, Tarjan.run(g, new Metrics()).componentCount());
    }

    @Test
    void selfLoopsAndParallelEdgesDoNotChangeComponents() {
        Graph g = new Graph(3, true);
        g.addEdge(0, 0, 1);      // self-loop
        g.addEdge(0, 1, 1);
        g.addEdge(0, 1, 2);      // parallel
        g.addEdge(1, 0, 1);      // 0 and 1 now reach each other
        assertEquals(2, Kosaraju.run(g, new Metrics()).componentCount());   // {0,1} and {2}
        assertEquals(2, Tarjan.run(g, new Metrics()).componentCount());
    }

    @Test
    void emptyAndSingleVertexGraphs() {
        assertEquals(0, Kosaraju.run(new Graph(0, true), new Metrics()).componentCount());
        assertEquals(1, Kosaraju.run(new Graph(1, true), new Metrics()).componentCount());
        assertEquals(0, Tarjan.run(new Graph(0, true), new Metrics()).componentCount());
        assertEquals(1, Tarjan.run(new Graph(1, true), new Metrics()).componentCount());
    }

    @Test
    void undirectedGraphsAreRejected() {
        Graph g = new Graph(3, false);
        assertThrows(IllegalArgumentException.class, () -> Kosaraju.run(g, new Metrics()));
        assertThrows(IllegalArgumentException.class, () -> Tarjan.run(g, new Metrics()));
    }

    @Test
    void scanCountsFollowFromTheAlgorithms() {
        Graph g = randomDirectedGraph(new Random(5), 150);
        Metrics km = new Metrics();
        Kosaraju.run(g, km);
        assertEquals(2L * g.m(), km.edgeScans);        // two passes over every arc
        Metrics tm = new Metrics();
        Tarjan.run(g, tm);
        assertEquals((long) g.m(), tm.edgeScans);      // one pass over every arc
    }

    @Test
    void pathOfOneHundredThousandVerticesDoesNotOverflowTheStack() {
        int n = 100_000;
        Graph path = new Graph(n, true, n);
        for (int i = 0; i + 1 < n; i++) {
            path.addEdge(i, i + 1, 1);
        }
        Metrics km = new Metrics();
        SccResult k = Kosaraju.run(path, km);
        assertEquals(n, k.componentCount());
        assertEquals(n, km.maxDepth);

        Metrics tm = new Metrics();
        SccResult t = Tarjan.run(path, tm);
        assertEquals(n, t.componentCount());
        assertEquals(n, tm.maxDepth);
    }

    @Test
    void oneHugeCycleIsASingleComponent() {
        int n = 100_000;
        Graph cycle = new Graph(n, true, n);
        for (int i = 0; i < n; i++) {
            cycle.addEdge(i, (i + 1) % n, 1);
        }
        assertEquals(1, Kosaraju.run(cycle, new Metrics()).componentCount());
        assertEquals(1, Tarjan.run(cycle, new Metrics()).componentCount());
    }
}
