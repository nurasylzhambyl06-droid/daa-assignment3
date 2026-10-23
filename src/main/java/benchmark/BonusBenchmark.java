package benchmark;

import algorithms.Condensation;
import algorithms.CriticalPath;
import algorithms.Kosaraju;
import algorithms.SccResult;
import algorithms.Tarjan;
import graph.Graph;
import graph.GraphGenerator;
import metrics.Metrics;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class BonusBenchmark {

    private static final int[] SIZES = {1_000, 3_162, 10_000, 31_623, 100_000};
    private static final int[] DENSITY_FACTORS = {4, 20};

    private BonusBenchmark() {
    }

    public static void main(String[] args) throws IOException {
        Path out = Path.of("results", "bonus.csv");
        Files.createDirectories(out.getParent());
        try (PrintWriter csv = new PrintWriter(Files.newBufferedWriter(out))) {
            csv.println("n,m,components,kosaraju_edge_scans,tarjan_edge_scans,kosaraju_max_depth,tarjan_max_depth,"
                    + "condensation_vertices,condensation_edges,critical_path_length,critical_path_vertices");
            for (int n : SIZES) {
                for (int factor : DENSITY_FACTORS) {
                    int m = n * factor;
                    Graph g = GraphGenerator.directed(n, m);

                    Metrics km = new Metrics();
                    SccResult kosaraju = Kosaraju.run(g, km);
                    Metrics tm = new Metrics();
                    SccResult tarjan = Tarjan.run(g, tm);
                    check(kosaraju.samePartitionAs(tarjan), "Tarjan and Kosaraju found different components for n=" + n + ", m=" + m);
                    check(km.edgeScans == 2L * m, "Kosaraju must scan 2m edges");
                    check(tm.edgeScans == m, "Tarjan must scan m edges");

                    Graph dag = Condensation.build(g, tarjan);
                    CriticalPath.Result critical = CriticalPath.longestPath(dag, new Metrics());

                    csv.printf(Locale.US, "%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d%n", n, m, tarjan.componentCount(),
                            km.edgeScans, tm.edgeScans, km.maxDepth, tm.maxDepth,
                            dag.n(), dag.m(), critical.length(), critical.path().size());
                    csv.flush();
                    System.out.printf(Locale.US, "n=%d m=%d: %d SCCs | scans Kosaraju=%d Tarjan=%d | condensation %d vertices, %d edges | critical path length %d over %d vertices%n",
                            n, m, tarjan.componentCount(), km.edgeScans, tm.edgeScans,
                            dag.n(), dag.m(), critical.length(), critical.path().size());
                }
            }
        }
        System.out.println("Bonus self-checks passed. Results written to " + out);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("SELF-CHECK FAILED: " + message);
        }
    }
}
