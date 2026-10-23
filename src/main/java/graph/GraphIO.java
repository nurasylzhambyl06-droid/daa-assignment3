package graph;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.StringTokenizer;

public final class GraphIO {

    private GraphIO() {
    }

    public static Graph read(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            return read(reader);
        }
    }

    public static Graph parse(String text) throws IOException {
        return read(new BufferedReader(new StringReader(text)));
    }

    public static Graph read(BufferedReader reader) throws IOException {
        String header = nextDataLine(reader);
        if (header == null) {
            throw new IllegalArgumentException("empty input: expected 'n m directed|undirected'");
        }
        StringTokenizer st = new StringTokenizer(header);
        if (st.countTokens() != 3) {
            throw new IllegalArgumentException("bad header (expected 'n m directed|undirected'): " + header);
        }
        int n = parseInt(st.nextToken(), header);
        int m = parseInt(st.nextToken(), header);
        String kind = st.nextToken().toLowerCase();
        boolean directed;
        if (kind.equals("directed")) {
            directed = true;
        } else if (kind.equals("undirected")) {
            directed = false;
        } else {
            throw new IllegalArgumentException("third header field must be 'directed' or 'undirected', got: " + kind);
        }

        Graph g = new Graph(n, directed, m);
        for (int i = 0; i < m; i++) {
            String line = nextDataLine(reader);
            if (line == null) {
                throw new IllegalArgumentException("expected " + m + " edges but the file has only " + i);
            }
            StringTokenizer t = new StringTokenizer(line);
            if (t.countTokens() < 2 || t.countTokens() > 3) {
                throw new IllegalArgumentException("bad edge line (expected 'u v w'): " + line);
            }
            int u = parseInt(t.nextToken(), line);
            int v = parseInt(t.nextToken(), line);
            int w = t.hasMoreTokens() ? parseInt(t.nextToken(), line) : 1;
            g.addEdge(u, v, w);
        }
        return g;
    }

    public static void write(Graph g, Path path) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(path)) {
            w.write(g.n() + " " + g.m() + " " + (g.isDirected() ? "directed" : "undirected"));
            w.newLine();
            for (int i = 0; i < g.m(); i++) {
                w.write(g.edgeFrom(i) + " " + g.edgeTo(i) + " " + g.edgeWeight(i));
                w.newLine();
            }
        }
    }

    private static String nextDataLine(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("#")) {
                return line;
            }
        }
        return null;
    }

    private static int parseInt(String token, String line) {
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("not an integer '" + token + "' in line: " + line);
        }
    }
}
