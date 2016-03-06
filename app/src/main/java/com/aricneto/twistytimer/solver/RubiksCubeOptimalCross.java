package com.aricneto.twistytimer.solver;

import com.aricneto.twistytimer.solver.RubiksCubeSolver.State;

public class RubiksCubeOptimalCross implements Tip {
    private static State x;
    private static State z;
    private String description;

    public RubiksCubeOptimalCross(String description) {
        this.description = description;
    }

    static {
        x = new State(
                new byte[] { 3, 2, 6, 7, 0, 1, 5, 4 },
                new byte[] { 2, 1, 2, 1, 1, 2, 1, 2 },
                new byte[] { 7, 5, 9, 11, 6, 2, 10, 3, 4, 1, 8, 0 },
                new byte[] { 0, 0, 0,  0, 1, 0,  1, 0, 1, 0, 1, 0 });

        z = new State(
                new byte[] { 4, 0, 3, 7, 5, 1, 2, 6 },
                new byte[] { 1, 2, 1, 2, 2, 1, 2, 1 },
                new byte[] { 8, 4, 6, 10, 0, 7, 3, 11, 1, 5, 2, 9 },
                new byte[] { 1, 1, 1,  1, 1, 1, 1, 1,  1, 1, 1, 1 });
    }

    @Override
    public String getTipId() {
        return "RUBIKS-CUBE-OPTIMAL-CROSS";
    }

    @Override
    public String getPuzzleId() {
        return "RUBIKS-CUBE";
    }

    @Override
    public String getTipDescription() {
        return ("RUBIKS-CUBE-OPTIMAL-CROSS");
    }

    @Override
    public String getTip(String scramble) {
        int maxCount = 3;
        State state = State.id.applySequence(scramble.split(" "));

        StringBuilder tip = new StringBuilder();

        // cross on U
        int count = 0; // limit number of algs
        State stateU =
                x.multiply(x).multiply(state).multiply(x).multiply(x);
        tip.append(description + " U" + ":\n");
        for (String[] solution : RubiksCubeCrossSolver.solve(stateU)) {
            tip.append("  x2 " + StringUtils.join(" ", solution) + "\n");
            count++;
            if (count == maxCount)
                break;
        }
        tip.append("\n");

        // cross on D
        count = 0;
        State stateD = state;
        tip.append(description + " D" + ":\n");
        for (String[] solution : RubiksCubeCrossSolver.solve(stateD)) {
            tip.append("  " + StringUtils.join(" ", solution) + "\n");
            count++;
            if (count == maxCount)
                break;
        }
        tip.append("\n");

        // cross on L
        count = 0;
        State stateL =
                z.multiply(state).multiply(z).multiply(z).multiply(z);
        tip.append(description + " L" + ":\n");
        for (String[] solution : RubiksCubeCrossSolver.solve(stateL)) {
            tip.append("  z' " + StringUtils.join(" ", solution) + "\n");
            count++;
            if (count == maxCount)
                break;
        }
        tip.append("\n");

        // cross on R
        count = 0;
        State stateR =
                z.multiply(z).multiply(z).multiply(state).multiply(z);
        tip.append(description + " R" + ":\n");
        for (String[] solution : RubiksCubeCrossSolver.solve(stateR)) {
            tip.append("  z " + StringUtils.join(" ", solution) + "\n");
            count++;
            if (count == maxCount)
                break;
        }
        tip.append("\n");

        // cross on F
        count = 0;
        State stateF =
                x.multiply(state).multiply(x).multiply(x).multiply(x);
        tip.append(description + " F" + ":\n");
        for (String[] solution : RubiksCubeCrossSolver.solve(stateF)) {
            tip.append("  x' " + StringUtils.join(" ", solution) + "\n");
            count++;
            if (count == maxCount)
                break;
        }
        tip.append("\n");

        // cross on B
        count = 0;
        State stateB =
                x.multiply(x).multiply(x).multiply(state).multiply(x);
        tip.append(description + " B" + ":\n");
        for (String[] solution : RubiksCubeCrossSolver.solve(stateB)) {
            tip.append("  x " + StringUtils.join(" ", solution) + "\n");
            count++;
            if (count == maxCount)
                break;
        }
        tip.append("\n");

        return tip.toString().trim();
    }

    @Override
    public String toString() {
        return getTipDescription();
    }
}

