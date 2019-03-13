package com.aricneto.twistytimer.solver;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.solver.RubiksCubeSolver.State;
import com.aricneto.twistytimer.utils.DefaultPrefs;
import com.aricneto.twistytimer.utils.Prefs;

import java.util.ArrayList;

public class RubiksCubeOptimalXCross implements Tip {
    private static State x;
    private static State y;
    private static State z;

    private String description;

    public RubiksCubeOptimalXCross(String description) {
        this.description = description;
    }

    static {
        x = new State(
                new byte[] { 3, 2, 6, 7, 0, 1, 5, 4 },
                new byte[] { 2, 1, 2, 1, 1, 2, 1, 2 },
                new byte[] { 7, 5, 9, 11, 6, 2, 10, 3, 4, 1, 8, 0 },
                new byte[] { 0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 1, 0 });

        y = new State(
                new byte[] { 3, 0, 1, 2, 7, 4, 5, 6 },
                new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 },
                new byte[] { 3, 0, 1, 2, 7, 4, 5, 6, 11, 8, 9, 10 },
                new byte[] { 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0 });

        z = new State(
                new byte[] { 4, 0, 3, 7, 5, 1, 2, 6 },
                new byte[] { 1, 2, 1, 2, 2, 1, 2, 1 },
                new byte[] { 8, 4, 6, 10, 0, 7, 3, 11, 1, 5, 2, 9 },
                new byte[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 });
    }

    @Override
    public String getTipId() {
        return "RUBIKS-CUBE-OPTIMAL-X-CROSS";
    }

    @Override
    public String getPuzzleId() {
        return "RUBIKS-CUBE";
    }

    @Override
    public String getTipDescription() {
        return "RUBIKS-CUBE-OPTIMAL-X-CROSS";
    }

    @Override
    public String getTip(String scramble) {
        State state = State.id.applySequence(scramble.split(" "));

        StringBuilder tip = new StringBuilder();


        // x-cross on U
        if (Prefs.getBoolean(R.string.pk_cross_hint_top_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintTopEnabled))) {
            State stateU = x.multiply(x).multiply(state).multiply(x).multiply(x);
            tip.append(String.format(description, "U")).append("\n");
            tip.append(getOptimalSolutions(stateU, "x2 "));
            tip.append("\n");
        }

        // x-cross on D
        if (Prefs.getBoolean(R.string.pk_cross_hint_down_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintDownEnabled))) {
            tip.append(String.format(description, "D")).append("\n");
            tip.append(getOptimalSolutions(state, "")); // state == "stateD"
            tip.append("\n");
        }

        // x-cross on L
        if (Prefs.getBoolean(R.string.pk_cross_hint_left_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintLeftEnabled))) {
            State stateL = z.multiply(state).multiply(z).multiply(z).multiply(z);
            tip.append(String.format(description, "L")).append("\n");
            tip.append(getOptimalSolutions(stateL, "z' "));
            tip.append("\n");
        }

        // x-cross on R
        if (Prefs.getBoolean(R.string.pk_cross_hint_right_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintRightEnabled))) {
            State stateR = z.multiply(z).multiply(z).multiply(state).multiply(z);
            tip.append(String.format(description, "R")).append("\n");
            tip.append(getOptimalSolutions(stateR, "z "));
            tip.append("\n");
        }

        // x-cross on F
        if (Prefs.getBoolean(R.string.pk_cross_hint_front_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintFrontEnabled))) {
            State stateF = x.multiply(state).multiply(x).multiply(x).multiply(x);
            tip.append(String.format(description, "F")).append("\n");
            tip.append(getOptimalSolutions(stateF, "x' "));
            tip.append("\n");
        }

        // x-cross on B
        if (Prefs.getBoolean(R.string.pk_cross_hint_back_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintBackEnabled))) {
            State stateB = x.multiply(x).multiply(x).multiply(state).multiply(x);
            tip.append(String.format(description, "B")).append("\n");
            tip.append(getOptimalSolutions(stateB, "x "));
            tip.append("\n");
        }

        return tip.toString().trim();
    }

    private String getOptimalSolutions(State state, String prefix) {
        int count = 0;

        ArrayList<String> prefixes = new ArrayList<>();
        ArrayList<String[]> solutions = new ArrayList<>();

        // id
        for (String[] solution : RubiksCubeXCrossSolver.solve(state)) {
            prefixes.add(prefix);
            solutions.add(solution);
            count++;
            if (count == 2) {
                break;
            }
        }

        // y
        count = 0;
        State stateY = y.multiply(y).multiply(y).multiply(state).multiply(y);
        for (String[] solution : RubiksCubeXCrossSolver.solve(stateY)) {
            prefixes.add(prefix + "y ");
            solutions.add(solution);
            count++;
            if (count == 2) {
                break;
            }
        }

        // y2
        count = 0;
        State stateY2 = y.multiply(y).multiply(state).multiply(y).multiply(y);
        for (String[] solution : RubiksCubeXCrossSolver.solve(stateY2)) {
            prefixes.add(prefix + "y2 ");
            solutions.add(solution);
            count++;
            if (count == 2) {
                break;
            }
        }

        // y'
        count = 0;
        State stateY3 = y.multiply(state).multiply(y).multiply(y).multiply(y);
        for (String[] solution : RubiksCubeXCrossSolver.solve(stateY3)) {
            prefixes.add(prefix + "y' ");
            solutions.add(solution);
            count++;
            if (count == 2) {
                break;
            }
        }

        int minLength = Integer.MAX_VALUE;
        for (String[] solution : solutions) {
            if (solution.length < minLength) {
                minLength = solution.length;
            }
        }

        StringBuilder output = new StringBuilder();
        for (int i = 0; i < solutions.size(); i++) {
            if (solutions.get(i).length == minLength) {
                output.append("  ")
                        .append(prefixes.get(i))
                        .append(StringUtils.join(" ", solutions.get(i)))
                        .append('\n');
            }
        }

        return output.toString();
    }

    @Override
    public String toString() {
        return getTipDescription();
    }
}
