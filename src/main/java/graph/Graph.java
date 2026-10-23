package graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Graph {

    private final int n;
    private final boolean directed;
    private final List<Edge>[] adj;
    private final int[] inDegree;

    private int[] edgeFrom;
    private int[] edgeTo;
    private int[] edgeWeight;
    private int m;                    // number of edges added so far

    public Graph(int n, boolean directed) {
        this(n, directed, 16);
    }

    @SuppressWarnings("unchecked")
    public Graph(int n, boolean directed, int expectedEdges) {
        if (n < 0) {
            throw new IllegalArgumentException("n must be non-negative, got " + n);
        }
        this.n = n;
        this.directed = directed;
        this.adj = (List<Edge>[]) new List[n];
        for (int v = 0; v < n; v++) {
            adj[v] = new ArrayList<>();
        }
        this.inDegree = new int[n];
        int capacity = Math.max(expectedEdges, 16);
        this.edgeFrom = new int[capacity];
        this.edgeTo = new int[capacity];
        this.edgeWeight = new int[capacity];
    }

    public void addEdge(int u, int v, int w) {
        checkVertex(u);
        checkVertex(v);

        adj[u].add(new Edge(v, w));
        inDegree[v]++;
        if (!directed && u != v) {          // a self-loop needs only one arc
            adj[v].add(new Edge(u, w));
            inDegree[u]++;
        }

        if (m == edgeFrom.length) {         // grow the edge list
            int capacity = m * 2;
            edgeFrom = Arrays.copyOf(edgeFrom, capacity);
            edgeTo = Arrays.copyOf(edgeTo, capacity);
            edgeWeight = Arrays.copyOf(edgeWeight, capacity);
        }
        edgeFrom[m] = u;
        edgeTo[m] = v;
        edgeWeight[m] = w;
        m++;
    }

    public int n() {
        return n;
    }

    public int m() {
        return m;
    }

    public boolean isDirected() {
        return directed;
    }

    public List<Edge> adj(int u) {
        checkVertex(u);
        return adj[u];
    }

    public int inDegree(int v) {
        checkVertex(v);
        return inDegree[v];
    }

    public int edgeFrom(int i) {
        return edgeFrom[i];
    }

    public int edgeTo(int i) {
        return edgeTo[i];
    }

    public int edgeWeight(int i) {
        return edgeWeight[i];
    }

    public Graph reversed() {
        if (!directed) {
            throw new IllegalStateException("reversed() only makes sense for a directed graph");
        }
        Graph r = new Graph(n, true, m);
        for (int i = 0; i < m; i++) {
            r.addEdge(edgeTo[i], edgeFrom[i], edgeWeight[i]);
        }
        return r;
    }

    private void checkVertex(int v) {
        if (v < 0 || v >= n) {
            throw new IllegalArgumentException("vertex " + v + " is out of range 0.." + (n - 1));
        }
    }

    @Override
    public String toString() {
        return "Graph{n=" + n + ", m=" + m + ", " + (directed ? "directed" : "undirected") + "}";
    }
}
