package algorithms;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DsuTest {

    @Test
    void everyElementStartsInItsOwnSet() {
        DSU dsu = new DSU(5);
        assertEquals(5, dsu.components());
        for (int i = 0; i < 5; i++) {
            assertEquals(i, dsu.find(i));
        }
    }

    @Test
    void unionMergesAndReportsWhetherItDid() {
        DSU dsu = new DSU(4);
        assertTrue(dsu.union(0, 1));
        assertFalse(dsu.union(1, 0));          // already together
        assertTrue(dsu.connected(0, 1));
        assertFalse(dsu.connected(0, 2));
        assertEquals(3, dsu.components());
    }

    @Test
    void agreesWithASimpleReferenceAfterRandomUnions() {
        for (long seed = 1; seed <= 20; seed++) {
            Random rnd = new Random(seed);
            int n = 1 + rnd.nextInt(200);
            DSU dsu = new DSU(n);
            int[] label = new int[n];
            for (int i = 0; i < n; i++) {
                label[i] = i;
            }
            for (int op = 0; op < 600; op++) {
                int x = rnd.nextInt(n);
                int y = rnd.nextInt(n);
                boolean shouldMerge = label[x] != label[y];
                assertEquals(shouldMerge, dsu.union(x, y));
                if (shouldMerge) {
                    int from = label[y];
                    int to = label[x];
                    for (int i = 0; i < n; i++) {
                        if (label[i] == from) {
                            label[i] = to;
                        }
                    }
                }
            }
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j += 7) {
                    assertEquals(label[i] == label[j], dsu.connected(i, j));
                }
            }
            Set<Integer> distinct = new HashSet<>();
            for (int l : label) {
                distinct.add(l);
            }
            assertEquals(distinct.size(), dsu.components());
        }
    }

    @Test
    void countsFindCallsAndOnlySuccessfulUnions() {
        DSU dsu = new DSU(6);
        dsu.union(0, 1);            // success: 2 finds inside + 1 successful union
        dsu.union(0, 1);            // failure: 2 finds, no successful union
        dsu.union(2, 3);            // success
        assertEquals(2, dsu.getSuccessfulUnions());
        assertEquals(6, dsu.getFindCalls());
        assertEquals(8, dsu.getOps());
        dsu.find(5);
        assertEquals(7, dsu.getFindCalls());
    }

    @Test
    void makeSetResetsEverything() {
        DSU dsu = new DSU(3);
        dsu.union(0, 1);
        dsu.makeSet(7);
        assertEquals(7, dsu.components());
        assertEquals(0, dsu.getOps());
        assertFalse(dsu.connected(0, 1));
    }

    @Test
    void emptyAndNegativeSizes() {
        assertEquals(0, new DSU(0).components());
        assertThrows(IllegalArgumentException.class, () -> new DSU(-1));
    }

    @Test
    void longChainOfUnionsIsHandledWithoutRecursion() {
        int n = 200_000;
        DSU dsu = new DSU(n);
        for (int i = 0; i + 1 < n; i++) {
            dsu.union(i, i + 1);
        }
        assertEquals(1, dsu.components());
        assertTrue(dsu.connected(0, n - 1));
    }
}
