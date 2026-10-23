package benchmark;

import algorithms.DagShortestPath;
import algorithms.Kahn;
import algorithms.Kosaraju;
import algorithms.Kruskal;
import algorithms.Prim;
import algorithms.Tarjan;
import graph.Graph;
import graph.GraphGenerator;
import metrics.Metrics;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferenceValuesTest {

    @Test
    void generatedGraphsReproduceTheReferenceTable() {
        for (long[] row : Reference.TABLE) {
            int n = (int) row[0];
            int m = (int) row[1];
            if (n > 3_162) {
                continue;
            }
            Graph und = GraphGenerator.undirected(n, m);
            assertEquals(row[2], Kruskal.run(und, new Metrics()).totalWeight(), "Kruskal " + n + "/" + m);
            assertEquals(row[2], Prim.run(und, new Metrics()).totalWeight(), "Prim " + n + "/" + m);

            Graph dir = GraphGenerator.directed(n, m);
            Metrics km = new Metrics();
            assertEquals(row[3], Kosaraju.run(dir, km).componentCount(), "SCC " + n + "/" + m);
            assertEquals(2L * m, km.edgeScans);
            Metrics tm = new Metrics();
            assertEquals(row[3], Tarjan.run(dir, tm).componentCount(), "Tarjan " + n + "/" + m);
            assertEquals((long) m, tm.edgeScans);

            Graph dag = GraphGenerator.dag(n, m);
            Metrics hm = new Metrics();
            Kahn.Result topo = Kahn.run(dag, hm);
            assertEquals((long) m, hm.edgeScans);
            assertTrue(Kahn.isValidOrder(dag, topo.order()));
            Metrics dm = new Metrics();
            assertEquals(row[4], DagShortestPath.run(dag, topo.order(), dm).sum(), "DAG sum " + n + "/" + m);
            assertEquals((long) m, dm.edgeScans);
        }
    }

    @Test
    void theGeneratedUndirectedGraphIsConnected() {
        Graph und = GraphGenerator.undirected(1_000, 4_000);
        assertEquals(1, Kruskal.run(und, new Metrics()).components());
        assertEquals(4_000, und.m());
    }
}
