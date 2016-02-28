package com.aricneto.twistytimer.items;

/**
 * Stores a solve
 */
public class Solve {
    long   id;
    int    time;
    String puzzle;
    String subtype;
    long   date;
    String scramble;
    int    penalty;
    String comment;
    boolean history;

    public Solve(int time, String puzzle, String subtype, long date, String scramble, int penalty, String comment, boolean history) {
        this.time = time;
        this.puzzle = puzzle;
        this.subtype = subtype;
        this.date = date;
        this.scramble = scramble;
        this.penalty = penalty;
        this.comment = comment;
        this.history = history;
    }

    public Solve(long id, int time, String puzzle, String subtype, long date, String scramble, int penalty, String comment, boolean history) {
        this.id = id;
        this.time = time;
        this.puzzle = puzzle;
        this.subtype = subtype;
        this.date = date;
        this.scramble = scramble;
        this.penalty = penalty;
        this.comment = comment;
        this.history = history;
    }

    public boolean isHistory() {
        return history;
    }

    public void setHistory(boolean history) {
        this.history = history;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public long getId() {
        return id;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public String getPuzzle() {
        return puzzle;
    }

    public void setPuzzle(String puzzle) {
        this.puzzle = puzzle;
    }

    public String getSubtype() {
        return subtype;
    }

    public void setSubtype(String subtype) {
        this.subtype = subtype;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getScramble() {
        return scramble;
    }

    public void setScramble(String scramble) {
        this.scramble = scramble;
    }

    public int getPenalty() {
        return penalty;
    }

    public void setPenalty(int penalty) {
        this.penalty = penalty;
    }
}
