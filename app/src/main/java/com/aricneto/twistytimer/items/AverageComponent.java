package com.aricneto.twistytimer.items;

import android.util.Log;

import com.aricneto.twistytimer.structures.RedBlackTree;

import androidx.annotation.Nullable;

import static com.aricneto.twistytimer.stats.AverageCalculator.DNF;
import static com.aricneto.twistytimer.stats.AverageCalculator.UNKNOWN;

/**
 * Stores a balanced tree, its sum, and its least and greatest times
 */
public class AverageComponent {
    private Long               sum;
    private Long               least;
    private Long               greatest;
    private RedBlackTree<Long> tree;

    /**
     * This debug flag enables hard-checking every remove to verify that the element being
     * removed does indeed exist. If the check fails, the application will crash.
     */
    private static final boolean DEBUG = false;

    /**
     * Default constructor
     */
    public AverageComponent() {
        this.sum = UNKNOWN;
        this.least = UNKNOWN;
        this.greatest = UNKNOWN;
        this.tree = new RedBlackTree<>();
    }

    /**
     * Inserts an element into the tree and updates its sum and best/worst cache
     * @param val The value to be inserted
     */
    public void put(long val) {
        tree.add(val);
        addSum(val);

        // Update least/greatest caches if necessary
        if (least != UNKNOWN && val < least)
            least = val;
        if (greatest != UNKNOWN && val > greatest)
            greatest = val;
    }

    /**
     * Removes an element from tree and updates its sum and best/worst cache
     * @param val The value to be removed
     */
    public void remove(long val) {
        try {
            tree.remove(val);
        } catch (Exception e) {
            Log.d("AverageComponent", "Error while trying to remove value: " + val);
        }
        subSum(val);

        // Update least/greatest caches if necessary
        if (least != UNKNOWN && val == least)
            least = UNKNOWN;
        if (greatest != UNKNOWN && val == greatest)
            greatest = UNKNOWN;
    }

    /**
     * Gets the smallest element of the tree
     * @return The smallest element of the tree
     */
    public long getLeast() {
        // Cache request
        if (least == UNKNOWN && tree.size() > 0)
            least = tree.getLeast();
        return least;
    }

    /**
     * Gets the biggest element of the tree
     * @return The biggest element of the tree
     */
    public long getGreatest() {
        // Cache request
        if (greatest == UNKNOWN && tree.size() > 0)
            greatest = tree.getGreatest();
        return greatest;
    }

    /**
     * Gets the sum of all elements of the tree
     * @return The sum of all elements of the tree
     */
    public long getSum() {
        return sum;
    }

    public RedBlackTree<Long> getTree() {
        return tree;
    }

    /**
     * Adds a value to the total sum of the tree
     * @param val The value to be added
     */
    private void addSum(long val) {
        if (val != DNF)
            sum = (sum == UNKNOWN ? 0L : sum) + val;
    }

    /**
     * Removes a value from the total sum of the tree
     * @param val The value to be removed
     */
    private void subSum(long val) {
        if (val != DNF && sum != 0)
            sum = (sum == UNKNOWN ? 0L : sum) - val;
    }
}
