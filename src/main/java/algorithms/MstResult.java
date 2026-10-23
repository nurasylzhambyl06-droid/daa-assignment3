package algorithms;

import java.util.List;

public record MstResult(long totalWeight, int components, List<int[]> edges) {

    public int edgeCount() {
        return edges.size();
    }
}
