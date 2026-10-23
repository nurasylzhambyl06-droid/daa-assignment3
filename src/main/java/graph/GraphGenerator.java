package graph;

import java.util.Random;

public final class GraphGenerator {

    public static final long SEED = 42;

    private GraphGenerator() {
    }

    public static Graph undirected(int n, int m) {
        if (m < n - 1) {
            throw new IllegalArgumentException("m must be at least n-1 to hold the spanning tree");
        }
        Random rnd = new Random(SEED);
        int[] perm = shuffledIdentity(n, rnd);
        Graph g = new Graph(n, false, m);

        for (int i = 1; i < n; i++) {                       // random spanning tree
            int u = perm[rnd.nextInt(i)];
            int v = perm[i];
            g.addEdge(u, v, randomWeight(rnd));
        }
        for (int k = Math.max(n - 1, 0); k < m; k++) {      // the remaining edges
            addRandomEdge(g, rnd, null);
        }
        return g;
    }

    public static Graph directed(int n, int m) {
        Random rnd = new Random(SEED);
        Graph g = new Graph(n, true, m);
        for (int k = 0; k < m; k++) {
            addRandomEdge(g, rnd, null);
        }
        return g;
    }

    public static Graph dag(int n, int m) {
        Random rnd = new Random(SEED);
        int[] rank = shuffledIdentity(n, rnd);
        Graph g = new Graph(n, true, m);
        for (int k = 0; k < m; k++) {
            addRandomEdge(g, rnd, rank);
        }
        return g;
    }

    private static int[] shuffledIdentity(int n, Random rnd) {
        int[] p = new int[n];
        for (int i = 0; i < n; i++) {
            p[i] = i;
        }
        for (int i = n - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int tmp = p[i];
            p[i] = p[j];
            p[j] = tmp;
        }
        return p;
    }

    private static void addRandomEdge(Graph g, Random rnd, int[] rank) {
        int n = g.n();
        if (n < 2) {
            throw new IllegalArgumentException("random edges without self-loops need n >= 2");
        }
        int a = rnd.nextInt(n);
        int b = rnd.nextInt(n);
        while (b == a) {
            b = rnd.nextInt(n);
        }
        int w = randomWeight(rnd);
        if (rank != null && rank[a] > rank[b]) {
            int tmp = a;
            a = b;
            b = tmp;
        }
        g.addEdge(a, b, w);
    }

    private static int randomWeight(Random rnd) {
        return 1 + rnd.nextInt(1000);
    }
}
