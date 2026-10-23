package algorithms;

import graph.Edge;
import graph.Graph;
import graph.GraphIO;
import metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DagShortestPathTest {

    private static DagShortestPath.Result solve(Graph g, Metrics metrics) {
        return DagShortestPath.run(g, Kahn.run(g, new Metrics()).order(), metrics);
    }

    private static Graph randomDag(Random rnd) {
        int n = 1 + rnd.nextInt(40);
        int[] rank = new int[n];
        for (int i = 0; i < n; i++) {
            rank[i] = i;
        }
        for (int i = n - 1; i > 0; i--) {                    // shuffle the ranks
            int j = rnd.nextInt(i + 1);
            int t = rank[i];
            rank[i] = rank[j];
            rank[j] = t;
        }
        Graph g = new Graph(n, true);
        int m = n < 2 ? 0 : rnd.nextInt(4 * n);
        for (int i = 0; i < m; i++) {
            int a = rnd.nextInt(n);
            int b = rnd.nextInt(n);
            if (a == b) {
                continue;
            }
            if (rank[a] > rank[b]) {                         //acyclic
                int t = a;
                a = b;
                b = t;
            }
            g.addEdge(a, b, rnd.nextInt(71) - 20);          // weights from -20 to 50
        }
        return g;
    }

    private static long[] bellmanFord(Graph g) {
        int n = g.n();
        long[] dist = new long[n];
        Arrays.fill(dist, DagShortestPath.INF);
        for (int v = 0; v < n; v++) {
            if (g.inDegree(v) == 0) {
                dist[v] = 0;
            }
        }
        for (int round = 0; round < n; round++) {
            boolean changed = false;
            for (int i = 0; i < g.m(); i++) {
                int u = g.edgeFrom(i);
                int v = g.edgeTo(i);
                if (dist[u] != DagShortestPath.INF && dist[u] + g.edgeWeight(i) < dist[v]) {
                    dist[v] = dist[u] + g.edgeWeight(i);
                    changed = true;
                }
            }
            if (!changed) {
                break;
            }
        }
        return dist;
    }

    private static int cheapestEdge(Graph g, int from, int to) {
        int best = Integer.MAX_VALUE;
        for (Edge e : g.adj(from)) {
            if (e.to() == to) {
                best = Math.min(best, e.weight());
            }
        }
        return best;
    }

    @Test
    void matchesBellmanFordOnTwoHundredRandomDagsIncludingNegativeWeights() {
        Random rnd = new Random(99);
        for (int round = 0; round < 200; round++) {
            Graph g = randomDag(rnd);
            DagShortestPath.Result r = solve(g, new Metrics());
            assertArrayEquals(bellmanFord(g), r.dist(), "round " + round);
        }
    }

    @Test
    void restoredPathsReallyHaveTheReportedLength() {
        Random rnd = new Random(100);
        for (int round = 0; round < 100; round++) {
            Graph g = randomDag(rnd);
            DagShortestPath.Result r = solve(g, new Metrics());
            for (int v = 0; v < g.n(); v++) {
                List<Integer> path = r.pathTo(v);
                assertEquals(v, path.get(path.size() - 1).intValue());
                assertEquals(0, g.inDegree(path.get(0)));               // a path starts at a source
                long length = 0;
                for (int i = 0; i + 1 < path.size(); i++) {
                    length += cheapestEdge(g, path.get(i), path.get(i + 1));
                }
                assertEquals(r.dist()[v], length, "round " + round + " vertex " + v);
            }
        }
    }

    @Test
    void dagFileGivesTheDocumentedDistancesAndPath() throws IOException {
        Graph g = GraphIO.read(Path.of("data", "dag.txt"));
        DagShortestPath.Result r = solve(g, new Metrics());
        assertArrayEquals(new long[]{0, 2, -1, 0, 5, -2}, r.dist());
        assertEquals(4, r.sum());
        assertEquals(List.of(0, 1, 2, 3, 5), r.pathTo(5));
    }

    @Test
    void negativeWeightsAreFineInADag() {
        Graph g = new Graph(3, true);
        g.addEdge(0, 1, -5);
        g.addEdge(1, 2, -5);
        g.addEdge(0, 2, 1);
        assertEquals(-10, solve(g, new Metrics()).dist()[2]);
    }

    @Test
    void severalSourcesAllStartAtZero() {
        Graph g = new Graph(4, true);
        g.addEdge(0, 2, 5);
        g.addEdge(1, 2, 3);       // second source gives the better way into 2
        g.addEdge(2, 3, 1);
        DagShortestPath.Result r = solve(g, new Metrics());
        assertArrayEquals(new long[]{0, 0, 3, 4}, r.dist());
        assertEquals(List.of(1, 2, 3), r.pathTo(3));
    }

    @Test
    void isolatedVerticesHaveDistanceZero() {
        Graph g = new Graph(3, true);
        assertArrayEquals(new long[]{0, 0, 0}, solve(g, new Metrics()).dist());
    }

    @Test
    void onePassScansEveryEdgeExactlyOnce() {
        Graph g = randomDag(new Random(3));
        Metrics metrics = new Metrics();
        solve(g, metrics);
        assertEquals((long) g.m(), metrics.edgeScans);
    }

    @Test
    void refusesAGraphWithoutATopologicalOrder() {
        Graph g = new Graph(2, true);
        g.addEdge(0, 1, 1);
        g.addEdge(1, 0, 1);
        assertThrows(IllegalArgumentException.class, () -> DagShortestPath.run(g, null, new Metrics()));
        assertThrows(IllegalArgumentException.class, () -> DagShortestPath.run(g, new int[]{0}, new Metrics()));
    }
}
