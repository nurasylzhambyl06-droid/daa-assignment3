package algorithms;

import graph.Graph;

public final class Condensation {

    private Condensation() {
    }

    public static Graph build(Graph g, SccResult scc) {
        Graph dag = new Graph(scc.componentCount(), true, Math.max(g.m(), 16));
        int[] comp = scc.componentOf();
        for (int i = 0; i < g.m(); i++) {
            int cu = comp[g.edgeFrom(i)];
            int cv = comp[g.edgeTo(i)];
            if (cu != cv) {
                dag.addEdge(cu, cv, g.edgeWeight(i));
            }
        }
        return dag;
    }
}
