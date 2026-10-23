package algorithms;

import graph.Graph;
import metrics.Metrics;

import java.util.List;

public final class CriticalPath {

    private CriticalPath() {
    }

    public record Result(long length, List<Integer> path) {
    }

    public static Result longestPath(Graph dag, Metrics metrics) {
        Kahn.Result topo = Kahn.run(dag, new Metrics());
        if (topo.hasCycle()) {
            throw new IllegalArgumentException("the graph has a cycle - there is no critical path");
        }
        if (dag.n() == 0) {
            return new Result(0, List.of());
        }

        Graph negated = new Graph(dag.n(), true, Math.max(dag.m(), 16));
        for (int i = 0; i < dag.m(); i++) {
            negated.addEdge(dag.edgeFrom(i), dag.edgeTo(i), -dag.edgeWeight(i));
        }
        DagShortestPath.Result r = DagShortestPath.run(negated, topo.order(), metrics);

        int best = 0;
        for (int v = 1; v < dag.n(); v++) {
            if (r.dist()[v] < r.dist()[best]) {
                best = v;
            }
        }
        return new Result(-r.dist()[best], r.pathTo(best));
    }
}
