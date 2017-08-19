package com.aricneto.twistytimer.items;

/**
 * Stores a statistic, for use in {@link com.aricneto.twistytimer.adapter.StatGridAdapter}
 */

public class Stat {

    public static final int SCOPE_GLOBAL_BEST  = 0;
    public static final int SCOPE_SESSION_BEST = 1;
    public static final int SCOPE_CURRENT      = 2;

    // The time string ("12.43")
    private String time;
    // The scope (Best all time, session, current)
    private int scope;
    // The average scope (3, 12, 50...)
    private int averageScope;

    public Stat(String time, int scope, int averageScope) {
        this.time = time;
        this.scope = scope;
        this.averageScope = averageScope;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public int getScope() {
        return scope;
    }

    public void setScope(int scope) {
        this.scope = scope;
    }

    public int getAverageScope() {
        return averageScope;
    }

    public void setAverageScope(int averageScope) {
        this.averageScope = averageScope;
    }

}
