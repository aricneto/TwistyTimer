package com.aricneto.twistytimer.items;

/**
 * Stores a statistic, for use in {@link com.aricneto.twistytimer.adapter.StatGridAdapter}
 */

public class Stat {

    public static final int SCOPE_GLOBAL = 0;
    public static final int SCOPE_SESSION = 1;
    public static final int SCOPE_CURRENT  = 2;

    // The time string ("12.43")
    private String time;
    // The scope (Best all time, session, current)
    private int scope;
    // The row it's supposed to be displayed in
    // For Average scopes, row 0 = Ao3, row 1 = Ao12, row 2 = Ao50...
    private int row;
    // The stat label for custom stat lists
    // "ao3", "best", "totaltime", etc
    private String label;

    public Stat(String time, int scope, int row) {
        this.time = time;
        this.scope = scope;
        this.row = row;
    }

    public Stat(String time, int row) {
        this.time = time;
        this.row = row;
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

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

}
