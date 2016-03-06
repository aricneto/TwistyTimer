package com.aricneto.twistytimer.solver;

import com.aricneto.twistytimer.solver.RubiksCubeSolver.State;

import java.util.ArrayList;

public class RubiksCubeXCrossSolver {
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
            moves[i] = RubiksCubeSolver.State.moves.get(moveNames[i]);
        }
    }

    // constants
    private static int N_CORNERS_COMBINATIONS = 8;
    private static int N_CORNERS_PERMUTATIONS = 1;
    private static int N_CORNERS_ORIENTATIONS = 3;
    private static int N_EDGES_COMBINATIONS = 792;
    private static int N_EDGES_PERMUTATIONS = 120;
    private static int N_EDGES_ORIENTATIONS = 32;

    private static int goalCornersPermutation;
    private static int goalCornersOrientation;
    private static int goalEdgesPermutation;
    private static int goalEdgesOrientation;

    static {
        int[] goalIndices =
                stateToIndices(RubiksCubeSolver.State.id);

        goalCornersPermutation =
                goalIndices[0] * N_CORNERS_PERMUTATIONS + goalIndices[1];
        goalCornersOrientation =
                goalIndices[0] * N_CORNERS_ORIENTATIONS + goalIndices[2];
        goalEdgesPermutation =
                goalIndices[3] * N_EDGES_PERMUTATIONS + goalIndices[4];
        goalEdgesOrientation =
                goalIndices[3] * N_EDGES_ORIENTATIONS + goalIndices[5];
    }

    // move tables
    private static int[][] cornersPermutationMove;
    private static int[][] cornersOrientationMove;
    private static int[][] edgesPermutationMove;
    private static int[][] edgesOrientationMove;

    static {
        // corners permutation
        cornersPermutationMove = new int[N_CORNERS_COMBINATIONS * N_CORNERS_PERMUTATIONS][moves.length];
        for (int i = 0; i < N_CORNERS_COMBINATIONS; i++) {
            for (int j = 0; j < N_CORNERS_PERMUTATIONS; j++) {
                State state = indicesToState(new int[] { i, j, 0, 0, 0, 0 });
                for (int k = 0; k < moves.length; k++) {
                    int[] indices = stateToIndices(state.multiply(moves[k]));
                    cornersPermutationMove[i * N_CORNERS_PERMUTATIONS + j][k] =
                            indices[0] * N_CORNERS_PERMUTATIONS + indices[1];
                }
            }
        }

        // corners orientation
        cornersOrientationMove = new int[N_CORNERS_COMBINATIONS * N_CORNERS_ORIENTATIONS][moves.length];
        for (int i = 0; i < N_CORNERS_COMBINATIONS; i++) {
            for (int j = 0; j < N_CORNERS_ORIENTATIONS; j++) {
                State state = indicesToState(new int[] { i, 0, j, 0, 0, 0 });
                for (int k = 0; k < moves.length; k++) {
                    int[] indices = stateToIndices(state.multiply(moves[k]));
                    cornersOrientationMove[i * N_CORNERS_ORIENTATIONS + j][k] =
                            indices[0] * N_CORNERS_ORIENTATIONS + indices[2];
                }
            }
        }

        // edges permutation
        edgesPermutationMove = new int[N_EDGES_COMBINATIONS * N_EDGES_PERMUTATIONS][moves.length];
        for (int i = 0; i < N_EDGES_COMBINATIONS; i++) {
            for (int j = 0; j < N_EDGES_PERMUTATIONS; j++) {
                State state = indicesToState(new int[] { 0, 0, 0, i, j, 0 });
                for (int k = 0; k < moves.length; k++) {
                    int[] indices = stateToIndices(state.multiply(moves[k]));
                    edgesPermutationMove[i * N_EDGES_PERMUTATIONS + j][k] =
                            indices[3] * N_EDGES_PERMUTATIONS + indices[4];
                }
            }
        }

        // edges orientation
        edgesOrientationMove = new int[N_EDGES_COMBINATIONS * N_EDGES_ORIENTATIONS][moves.length];
        for (int i = 0; i < N_EDGES_COMBINATIONS; i++) {
            for (int j = 0; j < N_EDGES_ORIENTATIONS; j++) {
                State state = indicesToState(new int[] { 0, 0, 0, i, 0, j });
                for (int k = 0; k < moves.length; k++) {
                    int[] indices = stateToIndices(state.multiply(moves[k]));
                    edgesOrientationMove[i * N_EDGES_ORIENTATIONS + j][k] =
                            indices[3] * N_EDGES_ORIENTATIONS + indices[5];
                }
            }
        }
    }

    private static int[] stateToIndices(State state) {
        // corners
        boolean[] selectedCorners = {
                false, false, false, false,
                true,  false, false, false,
                };

        byte[] cornersMapping = {
                -1, -1, -1, -1,
                0, -1, -1, -1,
                };

        boolean[] cornersCombination = new boolean[state.cornersPermutation.length];
        for (int i = 0; i < cornersCombination.length; i++) {
            cornersCombination[i] = selectedCorners[state.cornersPermutation[i]];
        }
        int cornersCombinationIndex =
                IndexMapping.combinationToIndex(cornersCombination, 1);

        byte[] cornersPermutation = new byte[1];
        byte[] cornersOrientation = new byte[1];
        int next = 0;
        for (int i = 0; i < state.cornersPermutation.length; i++) {
            if (cornersCombination[i]) {
                cornersPermutation[next] = cornersMapping[state.cornersPermutation[i]];
                cornersOrientation[next] = state.cornersOrientation[i];
                next++;
            }
        }
        int cornersPermutationIndex =
                IndexMapping.permutationToIndex(cornersPermutation);
        int cornersOrientationIndex =
                IndexMapping.orientationToIndex(cornersOrientation, 3);

        // edges
        boolean[] selectedEdges = {
                true,  false, false, false,
                false, false, false, false,
                true,  true,  true,  true,
                };

        byte[] edgesMapping = {
                0, -1, -1, -1,
                -1, -1, -1, -1,
                1,  2,  3,  4,
                };

        boolean[] edgesCombination = new boolean[state.edgesPermutation.length];
        for (int i = 0; i < edgesCombination.length; i++) {
            edgesCombination[i] = selectedEdges[state.edgesPermutation[i]];
        }
        int edgesCombinationIndex =
                IndexMapping.combinationToIndex(edgesCombination, 5);

        byte[] edgesPermutation = new byte[5];
        byte[] edgesOrientation = new byte[5];
        next = 0;
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
                cornersCombinationIndex,
                cornersPermutationIndex,
                cornersOrientationIndex,
                edgesCombinationIndex,
                edgesPermutationIndex,
                edgesOrientationIndex,
                };
    }

    private static State indicesToState(int[] indices) {
        // corners
        boolean[] combination =
                IndexMapping.indexToCombination(indices[0], 1, 8);
        byte[] permutation =
                IndexMapping.indexToPermutation(indices[1], 1);
        byte[] orientation =
                IndexMapping.indexToOrientation(indices[2], 3, 1);

        byte[] selectedCorners = { 4 };
        int nextSelectedCornerIndex = 0;
        byte[] otherCorners = { 0, 1, 2, 3, 5, 6, 7 };
        int nextOtherCornerIndex = 0;

        byte[] cornersPermutation = new byte[8];
        byte[] cornersOrientation = new byte[8];
        for (int i = 0; i < cornersPermutation.length; i++) {
            if (combination[i]) {
                cornersPermutation[i] = selectedCorners[permutation[nextSelectedCornerIndex]];
                cornersOrientation[i] = orientation[nextSelectedCornerIndex];
                nextSelectedCornerIndex++;
            } else {
                cornersPermutation[i] = otherCorners[nextOtherCornerIndex];
                cornersOrientation[i] = 0;
                nextOtherCornerIndex++;
            }
        }

        // edges
        combination =
                IndexMapping.indexToCombination(indices[3], 5, 12);
        permutation =
                IndexMapping.indexToPermutation(indices[4], 5);
        orientation =
                IndexMapping.indexToOrientation(indices[5], 2, 5);

        byte[] selectedEdges = { 0, 8, 9, 10, 11 };
        int nextSelectedEdgeIndex = 0;
        byte[] otherEdges = { 1, 2, 3, 4, 5, 6, 7 };
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
                cornersPermutation,
                cornersOrientation,
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

        int cornersPermutationIndex =
                indices[0] * N_CORNERS_PERMUTATIONS + indices[1];
        int cornersOrientationIndex =
                indices[0] * N_CORNERS_ORIENTATIONS + indices[2];
        int edgesPermutationIndex =
                indices[3] * N_EDGES_PERMUTATIONS + indices[4];
        int edgesOrientationIndex =
                indices[3] * N_EDGES_ORIENTATIONS + indices[5];

        ArrayList<String[]> solutions = new ArrayList<String[]>();

        for (int depth = 0; ; depth++) {
            int[] path = new int[depth];

            search(cornersPermutationIndex,
                    cornersOrientationIndex,
                    edgesPermutationIndex,
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
            int cornersPermutation,
            int cornersOrientation,
            int edgesPermutation,
            int edgesOrientation,
            int depth,
            int[] path,
            ArrayList<String[]> solutions) {
        if (depth == 0) {
            if (cornersPermutation == goalCornersPermutation &&
                    cornersOrientation == goalCornersOrientation &&
                    edgesPermutation == goalEdgesPermutation &&
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
                    cornersPermutationMove[cornersPermutation][i],
                    cornersOrientationMove[cornersOrientation][i],
                    edgesPermutationMove[edgesPermutation][i],
                    edgesOrientationMove[edgesOrientation][i],
                    depth - 1,
                    path,
                    solutions);
        }
    }
}
