package com.aricneto.twistytimer.items;

import com.aricneto.twistytimer.structures.RedBlackTree;

import java.util.TreeMap;

import androidx.annotation.Nullable;

import static com.aricneto.twistytimer.stats.AverageCalculator.DNF;
import static com.aricneto.twistytimer.stats.AverageCalculator.UNKNOWN;

/**
 * Stores a sum, best and worst times
 */
public class AverageComponent {
    private long                sum;
    private @Nullable Long      best;
    private @Nullable Long      worst;
    private RedBlackTree<Long>  tree;

    public AverageComponent() {
        this.sum = UNKNOWN;
        this.best = null;
        this.worst = null;
        this.tree = new RedBlackTree<>();
    }

    public void put(long val) {
        tree.add(val);
        addSum(val);

        // Update best/worst caches if necessary
        if (best != null && val < best)
            best = val;
        if (worst != null && val > worst)
            worst = val;
    }

    public void remove(long val) {
        tree.remove(val);
        subSum(val);

        // Update best/worst caches if necessary
        if (best != null && val == best)
            best = null;
        if (worst != null && val == worst)
            worst = null;
    }

    public long getLeast() {
        // Cache request
        if (best == null)
            best = tree.getLeast();
        return best;
    }

    public long getGreatest() {
        // Cache request
        if (worst == null)
            worst = tree.getGreatest();
        return worst;
    }

    public long getSum() {
        return sum;
    }

    public RedBlackTree<Long> getTree() {
        return tree;
    }

    public void addSum(long val) {
        if (val != DNF)
            sum = (sum == UNKNOWN ? 0L : sum) + val;
    }

    public void subSum(long val) {
        if (val != DNF && sum != 0)
            sum = (sum == UNKNOWN ? 0L : sum) - val;
    }
}
