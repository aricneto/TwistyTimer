package com.aricneto.twistytimer.solver;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.solver.RubiksCubeSolver.State;
import com.aricneto.twistytimer.utils.DefaultPrefs;
import com.aricneto.twistytimer.utils.Prefs;

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
        int count = 0;
        State state = State.id.applySequence(scramble.split(" "));

        StringBuilder tip = new StringBuilder();

        // cross on U
        if (Prefs.getBoolean(R.string.pk_cross_hint_top_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintTopEnabled))) {
            count = 0; // limit number of algs
            State stateU =
                    x.multiply(x).multiply(state).multiply(x).multiply(x);
            tip.append(String.format(description, "U:")).append("\n");
            for (String[] solution : RubiksCubeCrossSolver.solve(stateU)) {
                tip.append("  x2 ").append(StringUtils.join(" ", solution)).append('\n');
                count++;
                if (count == maxCount)
                    break;
            }
            tip.append("\n");
        }

        // cross on D
        if (Prefs.getBoolean(R.string.pk_cross_hint_down_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintDownEnabled))) {
            count = 0;
            tip.append(String.format(description, "D")).append("\n");
            for (String[] solution : RubiksCubeCrossSolver.solve(state)) {
                tip.append("  ").append(StringUtils.join(" ", solution)).append('\n');
                count++;
                if (count == maxCount)
                    break;
            }
            tip.append("\n");
        }

        // cross on L
        if (Prefs.getBoolean(R.string.pk_cross_hint_left_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintLeftEnabled))) {
            count = 0;
            State stateL =
                    z.multiply(state).multiply(z).multiply(z).multiply(z);
            tip.append(String.format(description, "L")).append("\n");
            for (String[] solution : RubiksCubeCrossSolver.solve(stateL)) {
                tip.append("  z' ").append(StringUtils.join(" ", solution)).append('\n');
                count++;
                if (count == maxCount)
                    break;
            }
            tip.append("\n");
        }

        // cross on R
        if (Prefs.getBoolean(R.string.pk_cross_hint_right_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintRightEnabled))) {
            count = 0;
            State stateR =
                    z.multiply(z).multiply(z).multiply(state).multiply(z);
            tip.append(String.format(description, "R")).append("\n");
            for (String[] solution : RubiksCubeCrossSolver.solve(stateR)) {
                tip.append("  z ").append(StringUtils.join(" ", solution)).append('\n');
                count++;
                if (count == maxCount)
                    break;
            }
            tip.append("\n");
        }

        // cross on F
        if (Prefs.getBoolean(R.string.pk_cross_hint_front_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintFrontEnabled))) {
            count = 0;
            State stateF =
                    x.multiply(state).multiply(x).multiply(x).multiply(x);
            tip.append(String.format(description, "F")).append("\n");
            for (String[] solution : RubiksCubeCrossSolver.solve(stateF)) {
                tip.append("  x' ").append(StringUtils.join(" ", solution)).append('\n');
                count++;
                if (count == maxCount)
                    break;
            }
            tip.append("\n");
        }

        // cross on B
        if (Prefs.getBoolean(R.string.pk_cross_hint_back_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintBackEnabled))) {
            count = 0;
            State stateB =
                    x.multiply(x).multiply(x).multiply(state).multiply(x);
            tip.append(String.format(description, "B")).append("\n");
            for (String[] solution : RubiksCubeCrossSolver.solve(stateB)) {
                tip.append("  x ").append(StringUtils.join(" ", solution)).append('\n');
                count++;
                if (count == maxCount)
                    break;
            }
            tip.append("\n");
        }

        return tip.toString().trim();
    }

    @Override
    public String toString() {
        return getTipDescription();
    }
}

