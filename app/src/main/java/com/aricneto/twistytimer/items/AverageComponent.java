package com.aricneto.twistytimer.items;

import com.google.common.collect.Multiset;
import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;

/**
 * Stores a sum, best and worst times
 */
public class AverageComponent {
    public int sum;
    public int best;
    public int worst;
    public SortedMultiset<Long> tree;

    public AverageComponent(int sum, int best, int worst, SortedMultiset<Long> tree) {
        this.sum = sum;
        this.best = best;
        this.worst = worst;
        this.tree = tree;
    }
}
