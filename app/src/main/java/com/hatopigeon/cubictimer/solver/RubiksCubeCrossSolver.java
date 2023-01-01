package com.hatopigeon.cubictimer.solver;

import com.hatopigeon.cubictimer.solver.RubiksCubeSolver.State;

import java.util.ArrayList;

public class RubiksCubeCrossSolver {
    // moves
    private static String[] moveNames;
    private static State[] moves;

    static {
        moveNames = new String[] {
                "U", "U2", "U'",
                "D", "D2", "D'",
                "L", "L2", "L'",
                "R", "R2", "R'",
                "F", "F2", "F'",
                "B", "B2", "B'",
                };

        moves = new State[moveNames.length];
        for (int i = 0; i < moves.length; i++) {
            moves[i] = State.moves.get(moveNames[i]);
        }
    }

    // constants
    private static int N_EDGES_COMBINATIONS = 495;
    private static int N_EDGES_PERMUTATIONS = 24;
    private static int N_EDGES_ORIENTATIONS = 16;

    private static int goalEdgesPermutation;
    private static int goalEdgesOrientation;

    static {
        int[] goalIndices =
                stateToIndices(State.id);

        goalEdgesPermutation =
                goalIndices[0] * N_EDGES_PERMUTATIONS + goalIndices[1];
        goalEdgesOrientation =
                goalIndices[0] * N_EDGES_ORIENTATIONS + goalIndices[2];
    }

    // move tables
    private static int[][] edgesPermutationMove;
    private static int[][] edgesOrientationMove;

    static {
        // edges permutation
        edgesPermutationMove = new int[N_EDGES_COMBINATIONS * N_EDGES_PERMUTATIONS][moves.length];
        for (int i = 0; i < N_EDGES_COMBINATIONS; i++) {
            for (int j = 0; j < N_EDGES_PERMUTATIONS; j++) {
                State state = indicesToState(new int[] { i, j, 0 });
                for (int k = 0; k < moves.length; k++) {
                    int[] indices = stateToIndices(state.multiply(moves[k]));
                    edgesPermutationMove[i * N_EDGES_PERMUTATIONS + j][k] =
                            indices[0] * N_EDGES_PERMUTATIONS + indices[1];
                }
            }
        }

        // edges orientation
        edgesOrientationMove = new int[N_EDGES_COMBINATIONS * N_EDGES_ORIENTATIONS][moves.length];
        for (int i = 0; i < N_EDGES_COMBINATIONS; i++) {
            for (int j = 0; j < N_EDGES_ORIENTATIONS; j++) {
                State state = indicesToState(new int[] { i, 0, j });
                for (int k = 0; k < moves.length; k++) {
                    int[] indices = stateToIndices(state.multiply(moves[k]));
                    edgesOrientationMove[i * N_EDGES_ORIENTATIONS + j][k] =
                            indices[0] * N_EDGES_ORIENTATIONS + indices[2];
                }
            }
        }
    }

    private static int[] stateToIndices(State state) {
        // edges
        boolean[] selectedEdges = {
                false, false, false, false,
                false, false, false, false,
                true,  true,  true,  true,
                };

        byte[] edgesMapping = {
                -1, -1, -1, -1,
                -1, -1, -1, -1,
                0,  1,  2,  3,
                };

        boolean[] edgesCombination = new boolean[state.edgesPermutation.length];
        for (int i = 0; i < edgesCombination.length; i++) {
            edgesCombination[i] = selectedEdges[state.edgesPermutation[i]];
        }
        int edgesCombinationIndex =
                IndexMapping.combinationToIndex(edgesCombination, 4);

        byte[] edgesPermutation = new byte[4];
        byte[] edgesOrientation = new byte[4];
        int next = 0;
        for (int i = 0; i < state.edgesPermutation.length; i++) {
            if (edgesCombination[i]) {
                edgesPermutation[next] = edgesMapping[state.edgesPermutation[i]];
                edgesOrientation[next] = state.edgesOrientation[i];
                next++;
            }
        }
        int edgesPermutationIndex =
                IndexMapping.permutationToIndex(edgesPermutation);
        int edgesOrientationIndex =
                IndexMapping.orientationToIndex(edgesOrientation, 2);

        return new int[] {
                edgesCombinationIndex,
                edgesPermutationIndex,
                edgesOrientationIndex,
                };
    }

    private static State indicesToState(int[] indices) {
        boolean[] combination =
                IndexMapping.indexToCombination(indices[0], 4, 12);
        byte[] permutation =
                IndexMapping.indexToPermutation(indices[1], 4);
        byte[] orientation =
                IndexMapping.indexToOrientation(indices[2], 2, 4);

        byte[] selectedEdges = { 8, 9, 10, 11 };
        int nextSelectedEdgeIndex = 0;
        byte[] otherEdges = { 0, 1, 2, 3, 4, 5, 6, 7 };
        int nextOtherEdgeIndex = 0;

        byte[] edgesPermutation = new byte[12];
        byte[] edgesOrientation = new byte[12];
        for (int i = 0; i < edgesPermutation.length; i++) {
            if (combination[i]) {
                edgesPermutation[i] = selectedEdges[permutation[nextSelectedEdgeIndex]];
                edgesOrientation[i] = orientation[nextSelectedEdgeIndex];
                nextSelectedEdgeIndex++;
            } else {
                edgesPermutation[i] = otherEdges[nextOtherEdgeIndex];
                edgesOrientation[i] = 0;
                nextOtherEdgeIndex++;
            }
        }

        return new State(
                State.id.cornersPermutation,
                State.id.cornersOrientation,
                edgesPermutation,
                edgesOrientation);
    }

    // distance tables
    private static byte[] edgesPermutationDistance;
    private static byte[] edgesOrientationDistance;

    static {
        // edges permutation
        edgesPermutationDistance = new byte[N_EDGES_COMBINATIONS * N_EDGES_PERMUTATIONS];
        for (int i = 0; i < edgesPermutationDistance.length; i++) {
            edgesPermutationDistance[i] = -1;
        }
        edgesPermutationDistance[goalEdgesPermutation] = 0;

        int distance = 0;
        int nVisited = 1;
        while (nVisited < N_EDGES_COMBINATIONS * N_EDGES_PERMUTATIONS) {
            for (int i = 0; i < edgesPermutationDistance.length; i++) {
                if (edgesPermutationDistance[i] != distance) {
                    continue;
                }

                for (int j = 0; j < edgesPermutationMove[i].length; j++) {
                    int next = edgesPermutationMove[i][j];
                    if (edgesPermutationDistance[next] < 0) {
                        edgesPermutationDistance[next] = (byte) (distance + 1);
                        nVisited++;
                    }
                }
            }

            distance++;
        }


        // edges orientation
        edgesOrientationDistance = new byte[N_EDGES_COMBINATIONS * N_EDGES_ORIENTATIONS];
        for (int i = 0; i < edgesOrientationDistance.length; i++) {
            edgesOrientationDistance[i] = -1;
        }
        edgesOrientationDistance[goalEdgesOrientation] = 0;

        distance = 0;
        nVisited = 1;
        while (nVisited < N_EDGES_COMBINATIONS * N_EDGES_ORIENTATIONS) {
            for (int i = 0; i < edgesOrientationDistance.length; i++) {
                if (edgesOrientationDistance[i] != distance) {
                    continue;
                }

                for (int j = 0; j < edgesOrientationMove[i].length; j++) {
                    int next = edgesOrientationMove[i][j];
                    if (edgesOrientationDistance[next] < 0) {
                        edgesOrientationDistance[next] = (byte) (distance + 1);
                        nVisited++;
                    }
                }
            }

            distance++;
        }
    }

    public static ArrayList<String[]> solve(State state) {
        int[] indices = stateToIndices(state);

        int edgesPermutationIndex =
                indices[0] * N_EDGES_PERMUTATIONS + indices[1];
        int edgesOrientationIndex =
                indices[0] * N_EDGES_ORIENTATIONS + indices[2];

        ArrayList<String[]> solutions = new ArrayList<>();

        for (int depth = 0; ; depth++) {
            int[] path = new int[depth];

            search(edgesPermutationIndex,
                    edgesOrientationIndex,
                    depth,
                    path,
                    solutions);

            if (solutions.size() > 0) {
                return solutions;
            }
        }
    }

    private static void search(
            int edgesPermutation,
            int edgesOrientation,
            int depth,
            int[] path,
            ArrayList<String[]> solutions) {

        // limit number of solutions
        if (solutions.size() >= 3) {
            return;
        }

        if (depth == 0) {
            if (edgesPermutation == goalEdgesPermutation &&
                    edgesOrientation == goalEdgesOrientation) {
                String[] sequence = new String[path.length];
                for (int i = 0; i < sequence.length; i++) {
                    sequence[i] = moveNames[path[i]];
                }

                solutions.add(sequence);
            }

            return;
        }

        if (edgesPermutationDistance[edgesPermutation] > depth ||
                edgesOrientationDistance[edgesOrientation] > depth) {
            return;
        }

        for (int i = 0; i < moves.length; i++) {
            path[path.length - depth] = i;
            search(
                    edgesPermutationMove[edgesPermutation][i],
                    edgesOrientationMove[edgesOrientation][i],
                    depth - 1,
                    path,
                    solutions);
        }
    }
}
