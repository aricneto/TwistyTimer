package com.aricneto.twistytimer.items;

public class Algorithm {
    private long id;
    private String puzzle;
    private String subset;
    private String name;
    private String customAlgs;
    private int progress;

    public Algorithm(long id, String puzzle, String subset, String name) {
        this.id = id;
        this.puzzle = puzzle;
        this.subset = subset;
        this.name = name;
        this.progress = 0;
    }

    public Algorithm(long id, String puzzle, String subset, String name, String customAlgs, int progress) {
        this.id = id;
        this.puzzle = puzzle;
        this.subset = subset;
        this.name = name;
        this.customAlgs = customAlgs;
        this.progress = progress;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPuzzle() {
        return puzzle;
    }

    public void setPuzzle(String puzzle) {
        this.puzzle = puzzle;
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

    public String getCustomAlgs() {
        return customAlgs;
    }

    public void setCustomAlgs(String customAlgs) {
        this.customAlgs = customAlgs;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }
}
