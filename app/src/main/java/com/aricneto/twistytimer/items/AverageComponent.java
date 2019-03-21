package com.aricneto.twistytimer.items;

import com.aricneto.twistytimer.structures.RedBlackTree;

import androidx.annotation.Nullable;

import static com.aricneto.twistytimer.stats.AverageCalculator.DNF;
import static com.aricneto.twistytimer.stats.AverageCalculator.UNKNOWN;

/**
 * Stores a sum, least and greatest times
 */
public class AverageComponent {
    private long               sum;
    private @Nullable Long     least;
    private @Nullable Long     greatest;
    private RedBlackTree<Long> tree;

    public AverageComponent() {
        this.sum = UNKNOWN;
        this.least = null;
        this.greatest = null;
        this.tree = new RedBlackTree<>();
    }

    public void put(long val) {
        tree.add(val);
        addSum(val);

        // Update least/greatest caches if necessary
        if (least != null && val < least)
            least = val;
        if (greatest != null && val > greatest)
            greatest = val;
    }

    public void remove(long val) {
        tree.remove(val);
        subSum(val);

        // Update least/greatest caches if necessary
        if (least != null && val == least)
            least = null;
        if (greatest != null && val == greatest)
            greatest = null;
    }

    public long getLeast() {
        // Cache request
        if (least == null)
            least = tree.getLeast();
        return least;
    }

    public long getGreatest() {
        // Cache request
        if (greatest == null)
            greatest = tree.getGreatest();
        return greatest;
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
