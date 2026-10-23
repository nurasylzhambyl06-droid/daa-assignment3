package metrics;

public final class Metrics {

    public long dsuOps;
    public long edgeScans;
    public long comparisons;
    public int maxDepth;

    public void trackDepth(int depth) {
        if (depth > maxDepth) {
            maxDepth = depth;
        }
    }

    public void reset() {
        dsuOps = 0;
        edgeScans = 0;
        comparisons = 0;
        maxDepth = 0;
    }

    @Override
    public String toString() {
        return "dsuOps=" + dsuOps + ", edgeScans=" + edgeScans + ", comparisons=" + comparisons + ", maxDepth=" + maxDepth;
    }
}
