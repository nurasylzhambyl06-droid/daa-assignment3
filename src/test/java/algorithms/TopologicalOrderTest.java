package algorithms;

import graph.Graph;
import graph.GraphGenerator;
import graph.GraphIO;
import metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopologicalOrderTest {

    @Test
    void returnsAValidOrderOnAGeneratedDag() {
        Graph dag = GraphGenerator.dag(500, 3000);
        Metrics metrics = new Metrics();
        Kahn.Result r = Kahn.run(dag, metrics);
        assertFalse(r.hasCycle());
        assertEquals(500, r.processed());
        assertNotNull(r.order());
        assertTrue(Kahn.isValidOrder(dag, r.order()));
        assertEquals(3000L, metrics.edgeScans);
    }

    @Test
    void returnsAValidOrderOnTheDagFile() throws IOException {
        Graph dag = GraphIO.read(Path.of("data", "dag.txt"));
        Kahn.Result r = Kahn.run(dag, new Metrics());
        assertFalse(r.hasCycle());
        assertTrue(Kahn.isValidOrder(dag, r.order()));
        assertEquals(0, r.order()[0]);
    }

    @Test
    void detectsTheCycleInTheCyclicFileInsteadOfReturningAWrongOrder() throws IOException {
        Graph g = GraphIO.read(Path.of("data", "cyclic.txt"));
        Kahn.Result r = Kahn.run(g, new Metrics());
        assertTrue(r.hasCycle());
        assertNull(r.order());
        assertTrue(r.processed() < g.n());
    }

    @Test
    void aCycleHiddenInsideAnOtherwiseAcyclicGraphIsStillFound() {
        Graph g = new Graph(4, true);
        g.addEdge(0, 1, 1);
        g.addEdge(1, 2, 1);
        g.addEdge(2, 1, 1);     // cycle 1 <-> 2
        g.addEdge(2, 3, 1);
        Kahn.Result r = Kahn.run(g, new Metrics());
        assertTrue(r.hasCycle());
        assertEquals(1, r.processed());
    }

    @Test
    void aSelfLoopIsACycle() {
        Graph g = new Graph(2, true);
        g.addEdge(0, 1, 1);
        g.addEdge(1, 1, 1);
        assertTrue(Kahn.run(g, new Metrics()).hasCycle());
    }

    @Test
    void isolatedVerticesAndParallelEdgesAreFine() {
        Graph g = new Graph(5, true);
        g.addEdge(0, 1, 1);
        g.addEdge(0, 1, 2);      // parallel edges
        Kahn.Result r = Kahn.run(g, new Metrics());
        assertFalse(r.hasCycle());
        assertEquals(5, r.processed());
        assertTrue(Kahn.isValidOrder(g, r.order()));
    }

    @Test
    void emptyGraphHasAnEmptyOrder() {
        Kahn.Result r = Kahn.run(new Graph(0, true), new Metrics());
        assertFalse(r.hasCycle());
        assertEquals(0, r.order().length);
    }

    @Test
    void theValidatorRejectsBadOrders() {
        Graph g = new Graph(3, true);
        g.addEdge(0, 1, 1);
        g.addEdge(1, 2, 1);
        assertTrue(Kahn.isValidOrder(g, new int[]{0, 1, 2}));
        assertFalse(Kahn.isValidOrder(g, new int[]{2, 1, 0}));
        assertFalse(Kahn.isValidOrder(g, new int[]{0, 1}));
        assertFalse(Kahn.isValidOrder(g, new int[]{0, 1, 1}));
        assertFalse(Kahn.isValidOrder(g, new int[]{0, 1, 5}));
        assertFalse(Kahn.isValidOrder(g, null));
    }

    @Test
    void undirectedGraphsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> Kahn.run(new Graph(3, false), new Metrics()));
    }
}
