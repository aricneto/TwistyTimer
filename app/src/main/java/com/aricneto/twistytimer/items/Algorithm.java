package com.aricneto.twistytimer.items;

/**
 * Stores an algorithm for use in the alg list
 */
public class Algorithm {
    long id;
    String subset;
    String name;
    String state;
    String algs;
    int progress;

    public Algorithm(long id, String subset, String name, String state, String algs, int progress) {
        this.id = id;
        this.subset = subset;
        this.name = name;
        this.state = state;
        this.algs = algs;
        this.progress = progress;
    }

    public Algorithm(String subset, String name, String state, String algs, int progress) {
        this.subset = subset;
        this.name = name;
        this.state = state;
        this.algs = algs;
        this.progress = progress;
    }

    public long getId() {
        return id;
    }

    public String getSubset() {
        return subset;
    }

    public void setSubset(String subset) {
        this.subset = subset;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getAlgs() {
        return algs;
    }

    public void setAlgs(String algs) {
        this.algs = algs;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }
}
