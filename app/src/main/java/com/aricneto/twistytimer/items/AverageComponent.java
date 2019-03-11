package com.aricneto.twistytimer.items;

import com.google.common.collect.Multiset;
import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;

/**
 * Stores a sum, best and worst times
 */
public class AverageComponent {
    public long sum;
    public long best;
    public long worst;
    public SortedMultiset<Long> tree;

    public AverageComponent(long sum, long best, long worst, SortedMultiset<Long> tree) {
        this.sum = sum;
        this.best = best;
        this.worst = worst;
        this.tree = tree;
    }
}
