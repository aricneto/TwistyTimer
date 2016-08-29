package com.aricneto.twistytimer.solver;

// kociemba's two phase algorithm
// references: http://kociemba.org/cube.htm
//             http://www.jaapsch.net/puzzles/compcube.htm

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class RubiksCubeSolver {
    public static class State {
        public byte[] cornersPermutation;
        public byte[] cornersOrientation;
        public byte[] edgesPermutation;
        public byte[] edgesOrientation;

        public State(byte[] cornersPermutation, byte[] cornersOrientation, byte[] edgesPermutation, byte[] edgesOrientation) {
            this.cornersPermutation = cornersPermutation;
            this.cornersOrientation = cornersOrientation;
            this.edgesPermutation = edgesPermutation;
            this.edgesOrientation = edgesOrientation;
        }

        public State multiply(State move) {
            // corners
            byte[] cornersPermutation = new byte[8];
            byte[] cornersOrientation = new byte[8];

            for (int i = 0; i < 8; i++) {
                cornersPermutation[i] = this.cornersPermutation[move.cornersPermutation[i]];
                cornersOrientation[i] = (byte) ((this.cornersOrientation[move.cornersPermutation[i]] + move.cornersOrientation[i]) % 3);
            }

            // edges
            byte[] edgesPermutation = new byte[12];
            byte[] edgesOrientation = new byte[12];

            for (int i = 0; i < 12; i++) {
                edgesPermutation[i] = this.edgesPermutation[move.edgesPermutation[i]];
                edgesOrientation[i] = (byte) ((this.edgesOrientation[move.edgesPermutation[i]] + move.edgesOrientation[i]) % 2);
            }

            return new State(cornersPermutation, cornersOrientation, edgesPermutation, edgesOrientation);
        }

        public State applySequence(String[] sequence) {
            State state = this;
            for (String move : sequence) {
                state = state.multiply(moves.get(move));
            }

            return state;
        }

        public static final HashMap<String, State> moves;

        static {
            State moveU = new State(new byte[] { 3, 0, 1, 2, 4, 5, 6, 7 }, new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 }, new byte[] { 0, 1, 2, 3, 7, 4, 5, 6, 8, 9, 10, 11 }, new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 });
            State moveD = new State(new byte[] { 0, 1, 2, 3, 5, 6, 7, 4 }, new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 }, new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 9, 10, 11, 8 }, new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 });
            State moveL = new State(new byte[] { 4, 1, 2, 0, 7, 5, 6, 3 }, new byte[] { 2, 0, 0, 1, 1, 0, 0, 2 }, new byte[] { 11, 1, 2, 7, 4, 5, 6, 0, 8, 9, 10, 3 }, new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 });
            State moveR = new State(new byte[] { 0, 2, 6, 3, 4, 1, 5, 7 }, new byte[] { 0, 1, 2, 0, 0, 2, 1, 0 }, new byte[] { 0, 5, 9, 3, 4, 2, 6, 7, 8, 1, 10, 11 }, new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 });
            State moveF = new State(new byte[] { 0, 1, 3, 7, 4, 5, 2, 6 }, new byte[] { 0, 0, 1, 2, 0, 0, 2, 1 }, new byte[] { 0, 1, 6, 10, 4, 5, 3, 7, 8, 9, 2, 11 }, new byte[] { 0, 0, 1, 1, 0, 0, 1, 0, 0, 0, 1, 0 });
            State moveB = new State(new byte[] { 1, 5, 2, 3, 0, 4, 6, 7 }, new byte[] { 1, 2, 0, 0, 2, 1, 0, 0 }, new byte[] { 4, 8, 2, 3, 1, 5, 6, 7, 0, 9, 10, 11 }, new byte[] { 1, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0 });

            moves = new HashMap<>();
            moves.put("U", moveU);
            moves.put("U2", moveU.multiply(moveU));
            moves.put("U'", moveU.multiply(moveU).multiply(moveU));
            moves.put("D", moveD);
            moves.put("D2", moveD.multiply(moveD));
            moves.put("D'", moveD.multiply(moveD).multiply(moveD));
            moves.put("L", moveL);
            moves.put("L2", moveL.multiply(moveL));
            moves.put("L'", moveL.multiply(moveL).multiply(moveL));
            moves.put("R", moveR);
            moves.put("R2", moveR.multiply(moveR));
            moves.put("R'", moveR.multiply(moveR).multiply(moveR));
            moves.put("F", moveF);
            moves.put("F2", moveF.multiply(moveF));
            moves.put("F'", moveF.multiply(moveF).multiply(moveF));
            moves.put("B", moveB);
            moves.put("B2", moveB.multiply(moveB));
            moves.put("B'", moveB.multiply(moveB).multiply(moveB));
        }

        public static State id;

        static {
            id = new State(
                    IndexMapping.indexToPermutation(0, 8),
                    IndexMapping.indexToOrientation(0, 3, 8),
                    IndexMapping.indexToPermutation(0, 12),
                    IndexMapping.indexToOrientation(0, 2, 12));
        }
    }

    // constants
    public static final int N_CORNERS_ORIENTATIONS   = 2187;
    public static final int N_EDGES_ORIENTATIONS     = 2048;
    public static final int N_E_EDGES_COMBINATIONS   = 495;
    public static final int N_CORNERS_PERMUTATIONS   = 40320;
    public static final int N_U_D_EDGES_PERMUTATIONS = 40320;
    public static final int N_E_EDGES_PERMUTATIONS   = 24;
    public static final int N_EDGES_PERMUTATIONS     = 479001600;

    // moves
    private static String[] moveNames1;
    private static State[]  moves1;
    private static int[]    sides1;
    private static int[]    axes1;
    private static String[] moveNames2;
    private static State[]  moves2;
    private static int[]    sides2;
    private static int[]    axes2;

    static {
        // phase 1
        moveNames1 = new String[] {
                "U", "U2", "U'",
                "D", "D2", "D'",
                "L", "L2", "L'",
                "R", "R2", "R'",
                "F", "F2", "F'",
                "B", "B2", "B'",
                };

        moves1 = new State[moveNames1.length];
        for (int i = 0; i < moves1.length; i++) {
            moves1[i] = State.moves.get(moveNames1[i]);
        }

        sides1 = new int[] {
                0, 0, 0,
                1, 1, 1,
                2, 2, 2,
                3, 3, 3,
                4, 4, 4,
                5, 5, 5,
                };

        axes1 = new int[] {
                0, 0, 0,
                0, 0, 0,
                1, 1, 1,
                1, 1, 1,
                2, 2, 2,
                2, 2, 2,
                };

        // phase 2
        moveNames2 = new String[] {
                "U", "U2", "U'",
                "D", "D2", "D'",
                "L2",
                "R2",
                "F2",
                "B2",
                };

        moves2 = new State[moveNames2.length];
        for (int i = 0; i < moves2.length; i++) {
            moves2[i] = State.moves.get(moveNames2[i]);
        }

        sides2 = new int[] {
                0, 0, 0,
                1, 1, 1,
                2,
                3,
                4,
                5,
                };

        axes2 = new int[] {
                0, 0, 0,
                0, 0, 0,
                1,
                1,
                2,
                2,
                };
    }

    // move tables
    private static int[][] cornersOrientationMove;
    private static int[][] edgesOrientationMove;
    private static int[][] eEdgesCombinationMove;
    private static int[][] cornersPermutationMove;
    private static int[][] uDEdgesPermutationMove;
    private static int[][] eEdgesPermutationMove;

    static {
        // phase 1
        cornersOrientationMove = new int[N_CORNERS_ORIENTATIONS][moves1.length];
        for (int i = 0; i < N_CORNERS_ORIENTATIONS; i++) {
            State state = new State(new byte[8], IndexMapping.indexToZeroSumOrientation(i, 3, 8), new byte[12], new byte[12]);
            for (int j = 0; j < moves1.length; j++) {
                cornersOrientationMove[i][j] = IndexMapping.zeroSumOrientationToIndex(state.multiply(moves1[j]).cornersOrientation, 3);
            }
        }


        edgesOrientationMove = new int[N_EDGES_ORIENTATIONS][moves1.length];
        for (int i = 0; i < N_EDGES_ORIENTATIONS; i++) {
            State state = new State(new byte[8], new byte[8], new byte[12], IndexMapping.indexToZeroSumOrientation(i, 2, 12));
            for (int j = 0; j < moves1.length; j++) {
                edgesOrientationMove[i][j] = IndexMapping.zeroSumOrientationToIndex(state.multiply(moves1[j]).edgesOrientation, 2);
            }
        }


        eEdgesCombinationMove = new int[N_E_EDGES_COMBINATIONS][moves1.length];
        for (int i = 0; i < N_E_EDGES_COMBINATIONS; i++) {
            boolean[] combination = IndexMapping.indexToCombination(i, 4, 12);

            byte[] edges = new byte[12];
            byte nextE = 0;
            byte nextUD = 4;

            for (int j = 0; j < edges.length; j++) {
                if (combination[j]) {
                    edges[j] = nextE++;
                } else {
                    edges[j] = nextUD++;
                }
            }

            State state = new State(new byte[8], new byte[8], edges, new byte[12]);
            for (int j = 0; j < moves1.length; j++) {
                State result = state.multiply(moves1[j]);

                boolean[] isEEdge = new boolean[12];
                for (int k = 0; k < isEEdge.length; k++) {
                    isEEdge[k] = result.edgesPermutation[k] < 4;
                }

                eEdgesCombinationMove[i][j] = IndexMapping.combinationToIndex(isEEdge, 4);
            }
        }


        // phase 2
        cornersPermutationMove = new int[N_CORNERS_PERMUTATIONS][moves2.length];
        for (int i = 0; i < N_CORNERS_PERMUTATIONS; i++) {
            State state = new State(IndexMapping.indexToPermutation(i, 8), new byte[8], new byte[12], new byte[12]);
            for (int j = 0; j < moves2.length; j++) {
                cornersPermutationMove[i][j] = IndexMapping.permutationToIndex(state.multiply(moves2[j]).cornersPermutation);
            }
        }


        uDEdgesPermutationMove = new int[N_U_D_EDGES_PERMUTATIONS][moves2.length];
        for (int i = 0; i < N_U_D_EDGES_PERMUTATIONS; i++) {
            byte[] permutation = IndexMapping.indexToPermutation(i, 8);

            byte[] edges = new byte[12];
            for (int j = 0; j < edges.length; j++) {
                edges[j] = j >= 4 ? permutation[j - 4] : (byte) j;
            }

            State state = new State(new byte[8], new byte[8], edges, new byte[12]);
            for (int j = 0; j < moves2.length; j++) {
                State result = state.multiply(moves2[j]);

                byte[] uDEdges = new byte[8];
                for (int k = 0; k < uDEdges.length; k++) {
                    uDEdges[k] = (byte) (result.edgesPermutation[k + 4] - 4);
                }

                uDEdgesPermutationMove[i][j] = IndexMapping.permutationToIndex(uDEdges);
            }
        }


        eEdgesPermutationMove = new int[N_E_EDGES_PERMUTATIONS][moves2.length];
        for (int i = 0; i < N_E_EDGES_PERMUTATIONS; i++) {
            byte[] permutation = IndexMapping.indexToPermutation(i, 4);

            byte[] edges = new byte[12];
            for (int j = 0; j < edges.length; j++) {
                edges[j] = j >= 4 ? (byte) j : permutation[j];
            }

            State state = new State(new byte[8], new byte[8], edges, new byte[12]);
            for (int j = 0; j < moves2.length; j++) {
                State result = state.multiply(moves2[j]);

                byte[] eEdges = Arrays.copyOf(result.edgesPermutation, 4);

                eEdgesPermutationMove[i][j] = IndexMapping.permutationToIndex(eEdges);
            }
        }
    }

    // prune tables
    private static byte[][] cornersOrientationDistance;
    private static byte[][] edgesOrientationDistance;
    private static byte[][] cornersPermutationDistance;
    private static byte[][] uDEdgesPermutationDistance;

    static {
        // phase 1
        cornersOrientationDistance = new byte[N_CORNERS_ORIENTATIONS][N_E_EDGES_COMBINATIONS];
        for (int i = 0; i < cornersOrientationDistance.length; i++) {
            for (int j = 0; j < cornersOrientationDistance[i].length; j++) {
                cornersOrientationDistance[i][j] = - 1;
            }
        }
        cornersOrientationDistance[0][0] = 0;

        int distance = 0;
        int nVisited = 1;
        while (nVisited < N_CORNERS_ORIENTATIONS * N_E_EDGES_COMBINATIONS) {
            for (int i = 0; i < N_CORNERS_ORIENTATIONS; i++) {
                for (int j = 0; j < N_E_EDGES_COMBINATIONS; j++) {
                    if (cornersOrientationDistance[i][j] == distance) {
                        for (int k = 0; k < moves1.length; k++) {
                            int nextCornersOrientation = cornersOrientationMove[i][k];
                            int nextEEdgesCombination = eEdgesCombinationMove[j][k];
                            if (cornersOrientationDistance[nextCornersOrientation][nextEEdgesCombination] < 0) {
                                cornersOrientationDistance[nextCornersOrientation][nextEEdgesCombination] = (byte) (distance + 1);
                                nVisited++;
                            }
                        }
                    }
                }
            }
            distance++;
        }


        edgesOrientationDistance = new byte[N_EDGES_ORIENTATIONS][N_E_EDGES_COMBINATIONS];
        for (int i = 0; i < edgesOrientationDistance.length; i++) {
            for (int j = 0; j < edgesOrientationDistance[i].length; j++) {
                edgesOrientationDistance[i][j] = - 1;
            }
        }
        edgesOrientationDistance[0][0] = 0;

        distance = 0;
        nVisited = 1;
        while (nVisited < N_EDGES_ORIENTATIONS * N_E_EDGES_COMBINATIONS) {
            for (int i = 0; i < N_EDGES_ORIENTATIONS; i++) {
                for (int j = 0; j < N_E_EDGES_COMBINATIONS; j++) {
                    if (edgesOrientationDistance[i][j] == distance) {
                        for (int k = 0; k < moves1.length; k++) {
                            int nextEdgesOrientation = edgesOrientationMove[i][k];
                            int nextEEdgesCombination = eEdgesCombinationMove[j][k];
                            if (edgesOrientationDistance[nextEdgesOrientation][nextEEdgesCombination] < 0) {
                                edgesOrientationDistance[nextEdgesOrientation][nextEEdgesCombination] = (byte) (distance + 1);
                                nVisited++;
                            }
                        }
                    }
                }
            }
            distance++;
        }


        // phase 2
        cornersPermutationDistance = new byte[N_CORNERS_PERMUTATIONS][N_E_EDGES_PERMUTATIONS];
        for (int i = 0; i < cornersPermutationDistance.length; i++) {
            for (int j = 0; j < cornersPermutationDistance[i].length; j++) {
                cornersPermutationDistance[i][j] = - 1;
            }
        }
        cornersPermutationDistance[0][0] = 0;

        distance = 0;
        nVisited = 1;
        while (nVisited < N_CORNERS_PERMUTATIONS * N_E_EDGES_PERMUTATIONS) {
            for (int i = 0; i < N_CORNERS_PERMUTATIONS; i++) {
                for (int j = 0; j < N_E_EDGES_PERMUTATIONS; j++) {
                    if (cornersPermutationDistance[i][j] == distance) {
                        for (int k = 0; k < moves2.length; k++) {
                            int nextCornersPermutation = cornersPermutationMove[i][k];
                            int nextEEdgesPermutation = eEdgesPermutationMove[j][k];
                            if (cornersPermutationDistance[nextCornersPermutation][nextEEdgesPermutation] < 0) {
                                cornersPermutationDistance[nextCornersPermutation][nextEEdgesPermutation] = (byte) (distance + 1);
                                nVisited++;
                            }
                        }
                    }
                }
            }
            distance++;
        }


        uDEdgesPermutationDistance = new byte[N_U_D_EDGES_PERMUTATIONS][N_E_EDGES_PERMUTATIONS];
        for (int i = 0; i < uDEdgesPermutationDistance.length; i++) {
            for (int j = 0; j < uDEdgesPermutationDistance[i].length; j++) {
                uDEdgesPermutationDistance[i][j] = - 1;
            }
        }
        uDEdgesPermutationDistance[0][0] = 0;

        distance = 0;
        nVisited = 1;
        while (nVisited < N_U_D_EDGES_PERMUTATIONS * N_E_EDGES_PERMUTATIONS) {
            for (int i = 0; i < N_U_D_EDGES_PERMUTATIONS; i++) {
                for (int j = 0; j < N_E_EDGES_PERMUTATIONS; j++) {
                    if (uDEdgesPermutationDistance[i][j] == distance) {
                        for (int k = 0; k < moves2.length; k++) {
                            int nextUDEdgesPermutation = uDEdgesPermutationMove[i][k];
                            int nextEEdgesPermutation = eEdgesPermutationMove[j][k];
                            if (uDEdgesPermutationDistance[nextUDEdgesPermutation][nextEEdgesPermutation] < 0) {
                                uDEdgesPermutationDistance[nextUDEdgesPermutation][nextEEdgesPermutation] = (byte) (distance + 1);
                                nVisited++;
                            }
                        }
                    }
                }
            }
            distance++;
        }
    }

    // search
    private static final int MAX_SOLUTION_LENGTH         = 23;
    private static final int MAX_PHASE_2_SOLUTION_LENGTH = 12;

    private static State              initialState;
    private static ArrayList<Integer> solution1;
    private static ArrayList<Integer> solution2;

    private static String[] solution(State state) {
        initialState = state;

        // corners orientation index
        int cornersOrientation = IndexMapping.zeroSumOrientationToIndex(state.cornersOrientation, 3);

        // edges orientation index
        int edgesOrientation = IndexMapping.zeroSumOrientationToIndex(state.edgesOrientation, 2);

        // e edges combination index
        boolean[] isEEdge = new boolean[12];
        for (int i = 0; i < isEEdge.length; i++) {
            isEEdge[i] = state.edgesPermutation[i] < 4;
        }
        int eEdgesCombination = IndexMapping.combinationToIndex(isEEdge, 4);

        for (int depth = 0; ; depth++) {
            solution1 = new ArrayList<>(MAX_SOLUTION_LENGTH);
            if (search1(cornersOrientation, edgesOrientation, eEdgesCombination, depth)) {
                ArrayList<String> sequence = new ArrayList<>();
                for (int moveIndex : solution1) {
                    sequence.add(moveNames1[moveIndex]);
                }
                for (int moveIndex : solution2) {
                    sequence.add(moveNames2[moveIndex]);
                }

                String[] sequenceArray = new String[sequence.size()];
                sequence.toArray(sequenceArray);

                return sequenceArray;
            }
        }
    }

    private static boolean search1(int cornersOrientation, int edgesOrientation, int eEdgesCombinations, int depth) {
        if (depth == 0) {
            if (cornersOrientation == 0 && edgesOrientation == 0 && eEdgesCombinations == 0) {
                State state = initialState;
                for (int moveIndex : solution1) {
                    state = state.multiply(moves1[moveIndex]);
                }

                return solution2(state, MAX_SOLUTION_LENGTH - solution1.size());
            }

            return false;
        }

        if (cornersOrientationDistance[cornersOrientation][eEdgesCombinations] <= depth &&
                edgesOrientationDistance[edgesOrientation][eEdgesCombinations] <= depth) {
            int[] lastMoves = { - 1, - 1 };
            for (int i = 0; i < lastMoves.length && i < solution1.size(); i++) {
                lastMoves[i] = solution1.get(solution1.size() - 1 - i);
            }

            for (int i = 0; i < moves1.length; i++) {
                // same side
                if (lastMoves[0] >= 0 && sides1[i] == sides1[lastMoves[0]]) {
                    continue;
                }

                // same axis three times in a row
                if (lastMoves[0] >= 0 && axes1[i] == axes1[lastMoves[0]] &&
                        lastMoves[1] >= 0 && axes1[i] == axes1[lastMoves[1]]) {
                    continue;
                }

                solution1.add(i);
                if (search1(cornersOrientationMove[cornersOrientation][i],
                        edgesOrientationMove[edgesOrientation][i],
                        eEdgesCombinationMove[eEdgesCombinations][i],
                        depth - 1)) {
                    return true;
                }
                solution1.remove(solution1.size() - 1);
            }
        }

        return false;
    }

    private static boolean solution2(State state, int maxDepth) {
        if (solution1.size() > 0) {
            int lastMove = solution1.get(solution1.size() - 1);
            for (int i = 0; i < moveNames2.length; i++) {
                if (moveNames1[lastMove].equals(moveNames2[i])) {
                    return false;
                }
            }
        }

        // corners permutation index
        int cornersPermutation = IndexMapping.permutationToIndex(state.cornersPermutation);

        // u and d edges permutation index
        byte[] uDEdges = new byte[8];
        for (int i = 0; i < uDEdges.length; i++) {
            uDEdges[i] = (byte) (state.edgesPermutation[i + 4] - 4);
        }
        int uDEdgesPermutation = IndexMapping.permutationToIndex(uDEdges);

        // e edges permutation index
        byte[] eEdges = Arrays.copyOf(state.edgesPermutation, 4);
        int eEdgesPermutation = IndexMapping.permutationToIndex(eEdges);

        for (int depth = 0; depth < Math.min(MAX_PHASE_2_SOLUTION_LENGTH, maxDepth); depth++) {
            solution2 = new ArrayList<>(MAX_SOLUTION_LENGTH);
            if (search2(cornersPermutation, uDEdgesPermutation, eEdgesPermutation, depth)) {
                return true;
            }
        }

        return false;
    }

    private static boolean search2(int cornersPermutation, int uDEdgesPermutation, int eEdgesPermutation, int depth) {
        if (depth == 0) {
            return cornersPermutation == 0 && uDEdgesPermutation == 0 && eEdgesPermutation == 0;
        }

        if (cornersPermutationDistance[cornersPermutation][eEdgesPermutation] <= depth &&
                uDEdgesPermutationDistance[uDEdgesPermutation][eEdgesPermutation] <= depth) {
            int lastSide = Integer.MAX_VALUE;
            if (solution2.size() > 0) {
                lastSide = sides2[solution2.get(solution2.size() - 1)];
            }

            for (int i = 0; i < moves2.length; i++) {
                // avoid superfluous moves between phases
                if (solution2.size() == 0) {
                    int lastPhase1Axis = Integer.MAX_VALUE;
                    if (solution1.size() > 0) {
                        lastPhase1Axis = axes1[solution1.get(solution1.size() - 1)];
                    }

                    if (axes2[i] == lastPhase1Axis) {
                        continue;
                    }
                }

                // same side
                if (sides2[i] == lastSide) {
                    continue;
                }

                solution2.add(i);
                if (search2(cornersPermutationMove[cornersPermutation][i],
                        uDEdgesPermutationMove[uDEdgesPermutation][i],
                        eEdgesPermutationMove[eEdgesPermutation][i],
                        depth - 1)) {
                    return true;
                }
                solution2.remove(solution2.size() - 1);
            }
        }

        return false;
    }

    public static String[] generate(State state) {
        String[] solution = solution(state);

        HashMap<String, String> inverseMoveNames = new HashMap<>();
        inverseMoveNames.put("U", "U'");
        inverseMoveNames.put("U2", "U2");
        inverseMoveNames.put("U'", "U");
        inverseMoveNames.put("D", "D'");
        inverseMoveNames.put("D2", "D2");
        inverseMoveNames.put("D'", "D");
        inverseMoveNames.put("L", "L'");
        inverseMoveNames.put("L2", "L2");
        inverseMoveNames.put("L'", "L");
        inverseMoveNames.put("R", "R'");
        inverseMoveNames.put("R2", "R2");
        inverseMoveNames.put("R'", "R");
        inverseMoveNames.put("F", "F'");
        inverseMoveNames.put("F2", "F2");
        inverseMoveNames.put("F'", "F");
        inverseMoveNames.put("B", "B'");
        inverseMoveNames.put("B2", "B2");
        inverseMoveNames.put("B'", "B");

        String[] sequence = new String[solution.length];
        for (int i = 0; i < solution.length; i++) {
            sequence[i] = inverseMoveNames.get(solution[solution.length - i - 1]);
        }

        return sequence;
    }
}
