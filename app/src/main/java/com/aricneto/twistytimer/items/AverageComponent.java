package com.aricneto.twistytimer.items;

import static com.aricneto.twistytimer.stats.AverageCalculator.DNF;
import static com.aricneto.twistytimer.stats.AverageCalculator.UNKNOWN;

/**
 * Stores a sum, best and worst times
 */
public class AverageComponent {
    public long sum;
    public long best;
    public long worst;

    public AverageComponent() {
    }

    public AverageComponent(long sum, long best, long worst) {
        this.sum = sum;
        this.best = best;
        this.worst = worst;
    }

    public void add(long val) {
        if (val != DNF)
            sum = (this.sum == UNKNOWN ? 0L : this.sum) + val;
    }

    public void sub(long val) {
        // If the lower trim is filled with DNFs, its value may be 0
        // so do not allow any subtraction then
        if (val != DNF && sum != 0)
            sum = (this.sum == UNKNOWN ? 0L : this.sum) - val;
    }
}
