package benchmark;

import algorithms.DagShortestPath;
import algorithms.Kahn;
import algorithms.Kosaraju;
import algorithms.Kruskal;
import algorithms.MstResult;
import algorithms.Prim;
import algorithms.SccResult;
import graph.Graph;
import graph.GraphGenerator;
import metrics.Metrics;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Supplier;

public final class Benchmark {

    private static final int[] SIZES = {1_000, 3_162, 10_000, 31_623, 100_000};
    private static final int[] DENSITY_FACTORS = {4, 20};            // sparse: m = 4n, dense: m = 20n
    private static final String[] DENSITY_NAMES = {"sparse", "dense"};
    private static final int TIMED_RUNS = 5;

    private Benchmark() {
    }

    private record Outcome(Metrics metrics, long result, double timeMs) {
    }

    public static void main(String[] args) throws IOException {
        int maxN = args.length > 0 ? Integer.parseInt(args[0]) : Integer.MAX_VALUE;
        Path out = Path.of("results", "results.csv");
        Files.createDirectories(out.getParent());

        try (PrintWriter csv = new PrintWriter(Files.newBufferedWriter(out))) {
            csv.println("algorithm,graph,n,m,time_ms,dsu_ops,edge_scans,comparisons,max_depth,result");

            for (int n : SIZES) {
                if (n > maxN) {
                    continue;
                }
                for (int d = 0; d < DENSITY_FACTORS.length; d++) {
                    int m = n * DENSITY_FACTORS[d];
                    runConfiguration(n, m, DENSITY_NAMES[d], csv);
                }
            }
        }
        System.out.println();
        System.out.println("All self-checks passed. Results written to " + out);
    }

    private static void runConfiguration(int n, int m, String density, PrintWriter csv) {
        System.out.printf("%n=== n=%d, m=%d (%s) ===%n", n, m, density);
        long[] ref = Reference.find(n, m);

        //undirected graph: Kruskal and Prim
        Graph und = GraphGenerator.undirected(n, m);
        Outcome kruskal = measure(() -> {
            Metrics mt = new Metrics();
            return new Outcome(mt, Kruskal.run(und, mt).totalWeight(), 0);
        });
        Outcome prim = measure(() -> {
            Metrics mt = new Metrics();
            return new Outcome(mt, Prim.run(und, mt).totalWeight(), 0);
        });
        check(kruskal.result() == prim.result(),
                "Kruskal and Prim disagree: " + kruskal.result() + " vs " + prim.result());
        if (ref != null) {
            check(kruskal.result() == ref[2], "MST weight " + kruskal.result() + " != reference " + ref[2]);
        }
        report(csv, "Kruskal", "undirected_" + density, n, m, kruskal);
        report(csv, "Prim", "undirected_" + density, n, m, prim);

        //directed graph: Kosaraju
        Graph dir = GraphGenerator.directed(n, m);
        Outcome kosaraju = measure(() -> {
            Metrics mt = new Metrics();
            SccResult r = Kosaraju.run(dir, mt);
            return new Outcome(mt, r.componentCount(), 0);
        });
        check(kosaraju.metrics().edgeScans == 2L * m,
                "Kosaraju must scan 2m edges, got " + kosaraju.metrics().edgeScans);
        if (ref != null) {
            check(kosaraju.result() == ref[3], "SCC count " + kosaraju.result() + " != reference " + ref[3]);
        }
        report(csv, "Kosaraju", "directed_" + density, n, m, kosaraju);

        // ---- DAG: Kahn and shortest paths ----
        Graph dag = GraphGenerator.dag(n, m);
        Outcome kahn = measure(() -> {
            Metrics mt = new Metrics();
            Kahn.Result r = Kahn.run(dag, mt);
            check(!r.hasCycle(), "Kahn found a cycle in a DAG");
            check(Kahn.isValidOrder(dag, r.order()), "Kahn returned an invalid topological order");
            return new Outcome(mt, r.processed(), 0);
        });
        check(kahn.metrics().edgeScans == m, "Kahn must scan m edges, got " + kahn.metrics().edgeScans);
        report(csv, "Kahn", "dag_" + density, n, m, kahn);

        int[] order = Kahn.run(dag, new Metrics()).order();
        Outcome paths = measure(() -> {
            Metrics mt = new Metrics();
            DagShortestPath.Result r = DagShortestPath.run(dag, order, mt);
            return new Outcome(mt, r.sum(), 0);
        });
        check(paths.metrics().edgeScans == m, "DAG relaxation must scan m edges, got " + paths.metrics().edgeScans);
        if (ref != null) {
            check(paths.result() == ref[4], "sum of DAG distances " + paths.result() + " != reference " + ref[4]);
        }
        report(csv, "DagShortestPath", "dag_" + density, n, m, paths);

        if (ref != null) {                               // reaching this line means every comparison above passed
            System.out.printf("Reference check OK  (n=%d, m=%d): MST weight %d, SCC count %d, sum of DAG distances %d%n",
                    n, m, ref[2], ref[3], ref[4]);
        }
    }


    private static Outcome measure(Supplier<Outcome> task) {
        task.get();                                                  // warm-up, result discarded
        double[] times = new double[TIMED_RUNS];
        Outcome last = null;
        for (int i = 0; i < TIMED_RUNS; i++) {
            long start = System.nanoTime();
            last = task.get();
            times[i] = (System.nanoTime() - start) / 1_000_000.0;
        }
        Arrays.sort(times);
        return new Outcome(last.metrics(), last.result(), times[TIMED_RUNS / 2]);
    }

    private static void report(PrintWriter csv, String algorithm, String graphName, int n, int m, Outcome o) {
        Metrics mt = o.metrics();
        csv.printf(Locale.US, "%s,%s,%d,%d,%.3f,%d,%d,%d,%d,%d%n",
                algorithm, graphName, n, m, o.timeMs(), mt.dsuOps, mt.edgeScans, mt.comparisons, mt.maxDepth, o.result());
        csv.flush();
        System.out.printf(Locale.US, "%-16s %-18s time=%9.3f ms  %s  result=%d%n",
                algorithm, graphName, o.timeMs(), mt, o.result());
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("SELF-CHECK FAILED: " + message);
        }
    }
}
