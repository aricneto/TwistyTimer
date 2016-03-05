package com.aricneto.twistytimer.solver;


public interface Tip {
    String getTipId();
    String getPuzzleId();
    String getTipDescription();
    String getTip(String scramble);
}