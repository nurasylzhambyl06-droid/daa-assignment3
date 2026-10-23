package algorithms;

import graph.Graph;
import graph.GraphIO;
import metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CondensationTest {

    @Test
    void cyclicFileCondensesToOneEdgeBetweenTwoComponents() throws IOException {
        Graph g = GraphIO.read(Path.of("data", "cyclic.txt"));
        SccResult scc = Kosaraju.run(g, new Metrics());
        Graph dag = Condensation.build(g, scc);
        assertEquals(2, dag.n());
        assertEquals(1, dag.m());
        assertEquals(2, dag.edgeWeight(0));
        assertFalse(Kahn.run(dag, new Metrics()).hasCycle());
        assertEquals(2, CriticalPath.longestPath(dag, new Metrics()).length());
    }

    @Test
    void condensationOfAnyGraphIsAlwaysAcyclic() {
        Random rnd = new Random(8);
        for (int round = 0; round < 60; round++) {
            int n = 2 + rnd.nextInt(80);
            Graph g = new Graph(n, true);
            int m = rnd.nextInt(4 * n);
            for (int i = 0; i < m; i++) {
                g.addEdge(rnd.nextInt(n), rnd.nextInt(n), 1 + rnd.nextInt(9));
            }
            Graph dag = Condensation.build(g, Tarjan.run(g, new Metrics()));
            assertFalse(Kahn.run(dag, new Metrics()).hasCycle(), "round " + round);
        }
    }

    @Test
    void criticalPathOfTheDagFile() throws IOException {
        Graph g = GraphIO.read(Path.of("data", "dag.txt"));
        CriticalPath.Result r = CriticalPath.longestPath(g, new Metrics());
        assertEquals(10, r.length());
        assertEquals(List.of(0, 2, 4), r.path());
    }

    @Test
    void criticalPathOfAChainWithABranch() {
        Graph g = new Graph(4, true);
        g.addEdge(0, 1, 4);
        g.addEdge(1, 3, 5);
        g.addEdge(0, 2, 1);
        g.addEdge(2, 3, 1);
        CriticalPath.Result r = CriticalPath.longestPath(g, new Metrics());
        assertEquals(9, r.length());
        assertEquals(List.of(0, 1, 3), r.path());
    }

    @Test
    void aGraphWithACycleHasNoCriticalPath() throws IOException {
        Graph g = GraphIO.read(Path.of("data", "cyclic.txt"));
        assertThrows(IllegalArgumentException.class, () -> CriticalPath.longestPath(g, new Metrics()));
    }
}
