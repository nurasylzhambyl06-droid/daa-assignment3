package graph;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphTest {

    @Test
    void parsesTheFileFormat() throws IOException {
        Graph g = GraphIO.parse("""
                # a comment
                4 3 directed

                0 1 5
                1 2 -7
                2 3
                """);
        assertEquals(4, g.n());
        assertEquals(3, g.m());
        assertTrue(g.isDirected());
        assertEquals(5, g.adj(0).get(0).weight());
        assertEquals(-7, g.adj(1).get(0).weight());
        assertEquals(1, g.adj(2).get(0).weight());      // omitted weight defaults to 1
    }

    @Test
    void undirectedEdgeIsStoredAsTwoArcsButCountsOnce() {
        Graph g = new Graph(3, false);
        g.addEdge(0, 1, 9);
        assertEquals(1, g.m());
        assertEquals(1, g.adj(0).size());
        assertEquals(1, g.adj(1).size());
        assertEquals(1, g.adj(0).get(0).to());
        assertEquals(0, g.adj(1).get(0).to());
        assertEquals(1, g.inDegree(0));
        assertEquals(1, g.inDegree(1));
    }

    @Test
    void directedEdgeIsOneArcAndUpdatesInDegree() {
        Graph g = new Graph(3, true);
        g.addEdge(0, 1, 9);
        assertEquals(1, g.adj(0).size());
        assertEquals(0, g.adj(1).size());
        assertEquals(0, g.inDegree(0));
        assertEquals(1, g.inDegree(1));
    }

    @Test
    void selfLoopsParallelEdgesAndIsolatedVerticesAreAllowed() {
        Graph g = new Graph(5, false);
        g.addEdge(2, 2, 1);        // self-loop
        g.addEdge(0, 1, 4);
        g.addEdge(0, 1, 6);        // parallel edge
        assertEquals(3, g.m());
        assertEquals(1, g.adj(2).size());     // a self-loop is one arc
        assertEquals(2, g.adj(0).size());
        assertEquals(0, g.adj(4).size());     // isolated vertex 4
    }

    @Test
    void edgeListGrowsBeyondItsInitialCapacity() {
        Graph g = new Graph(10, true, 1);
        for (int i = 0; i < 1000; i++) {
            g.addEdge(i % 10, (i + 1) % 10, i);
        }
        assertEquals(1000, g.m());
        assertEquals(999, g.edgeWeight(999));
    }

    @Test
    void badInputIsRejectedWithAnException() {
        assertThrows(IllegalArgumentException.class, () -> GraphIO.parse(""));
        assertThrows(IllegalArgumentException.class, () -> GraphIO.parse("3 1 sideways\n0 1 1\n"));
        assertThrows(IllegalArgumentException.class, () -> GraphIO.parse("3 2 directed\n0 1 1\n"));      // missing edge
        assertThrows(IllegalArgumentException.class, () -> GraphIO.parse("3 1 directed\n0 7 1\n"));      // vertex out of range
        assertThrows(IllegalArgumentException.class, () -> GraphIO.parse("3 1 directed\n0 x 1\n"));      // not a number
        assertThrows(IllegalArgumentException.class, () -> new Graph(-1, true));
    }

    @Test
    void reversedGraphTurnsEveryArrowAround() {
        Graph g = new Graph(3, true);
        g.addEdge(0, 1, 5);
        g.addEdge(1, 2, 6);
        Graph r = g.reversed();
        assertEquals(2, r.m());
        assertEquals(0, r.adj(0).size());
        assertEquals(0, r.adj(1).get(0).to());
        assertEquals(1, r.adj(2).get(0).to());
        assertEquals(6, r.adj(2).get(0).weight());
        assertThrows(IllegalStateException.class, () -> new Graph(2, false).reversed());
    }

    @Test
    void writeThenReadGivesTheSameGraph() throws IOException {
        Graph g = GraphGenerator.dag(50, 200);
        Path tmp = Files.createTempFile("graph", ".txt");
        try {
            GraphIO.write(g, tmp);
            Graph h = GraphIO.read(tmp);
            assertEquals(g.n(), h.n());
            assertEquals(g.m(), h.m());
            assertEquals(g.isDirected(), h.isDirected());
            for (int i = 0; i < g.m(); i++) {
                assertEquals(g.edgeFrom(i), h.edgeFrom(i));
                assertEquals(g.edgeTo(i), h.edgeTo(i));
                assertEquals(g.edgeWeight(i), h.edgeWeight(i));
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void theFourDataFilesCanBeRead() throws IOException {
        Graph connected = GraphIO.read(Path.of("data", "small_connected.txt"));
        assertEquals(6, connected.n());
        assertEquals(8, connected.m());
        assertFalse(connected.isDirected());

        Graph disconnected = GraphIO.read(Path.of("data", "disconnected.txt"));
        assertEquals(8, disconnected.n());
        assertEquals(6, disconnected.m());
        assertFalse(disconnected.isDirected());

        Graph cyclic = GraphIO.read(Path.of("data", "cyclic.txt"));
        assertEquals(6, cyclic.n());
        assertEquals(7, cyclic.m());
        assertTrue(cyclic.isDirected());

        Graph dag = GraphIO.read(Path.of("data", "dag.txt"));
        assertEquals(6, dag.n());
        assertEquals(7, dag.m());
        assertTrue(dag.isDirected());
    }

    @Test
    void generatorIsDeterministicAndHasTheRequestedSize() {
        Graph a = GraphGenerator.undirected(300, 1200);
        Graph b = GraphGenerator.undirected(300, 1200);
        assertEquals(1200, a.m());
        for (int i = 0; i < a.m(); i++) {
            assertEquals(a.edgeFrom(i), b.edgeFrom(i));
            assertEquals(a.edgeTo(i), b.edgeTo(i));
            assertEquals(a.edgeWeight(i), b.edgeWeight(i));
        }
        assertEquals(1000, GraphGenerator.directed(100, 1000).m());
        assertEquals(1000, GraphGenerator.dag(100, 1000).m());
        for (int i = 0; i < a.m(); i++) {
            assertTrue(a.edgeWeight(i) >= 1 && a.edgeWeight(i) <= 1000);
        }
    }
}
