package algorithms;

public record SccResult(int componentCount, int[] componentOf) {

    public boolean samePartitionAs(SccResult other) {
        if (componentCount != other.componentCount || componentOf.length != other.componentOf.length) {
            return false;
        }
        int[] thisToOther = new int[componentCount];
        int[] otherToThis = new int[componentCount];
        java.util.Arrays.fill(thisToOther, -1);
        java.util.Arrays.fill(otherToThis, -1);
        for (int v = 0; v < componentOf.length; v++) {
            int a = componentOf[v];
            int b = other.componentOf[v];
            if (thisToOther[a] == -1 && otherToThis[b] == -1) {
                thisToOther[a] = b;
                otherToThis[b] = a;
            } else if (thisToOther[a] != b || otherToThis[b] != a) {
                return false;
            }
        }
        return true;
    }
}
