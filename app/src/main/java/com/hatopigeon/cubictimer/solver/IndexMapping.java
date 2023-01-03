package com.hatopigeon.cubictimer.solver;

public class IndexMapping {
    // permutation
    public static int permutationToIndex(byte[] permutation) {
        int index = 0;
        for (int i = 0; i < permutation.length - 1; i++) {
            index *= permutation.length - i;
            for (int j = i + 1; j < permutation.length; j++) {
                if (permutation[i] > permutation[j]) {
                    index++;
                }
            }
        }

        return index;
    }

    public static byte[] indexToPermutation(int index, int length) {
        byte[] permutation = new byte[length];
        permutation[length - 1] = 0;
        for (int i = length - 2; i >= 0; i--) {
            permutation[i] = (byte) (index % (length - i));
            index /= length - i;
            for (int j = i + 1; j < length; j++) {
                if (permutation[j] >= permutation[i]) {
                    permutation[j]++;
                }
            }
        }

        return permutation;
    }

    // even permutation
    public static int evenPermutationToIndex(byte[] permutation) {
        int index = 0;
        for (int i = 0; i < permutation.length - 2; i++) {
            index *= permutation.length - i;
            for (int j = i + 1; j < permutation.length; j++) {
                if (permutation[i] > permutation[j]) {
                    index++;
                }
            }
        }

        return index;
    }

    public static byte[] indexToEvenPermutation(int index, int length) {
        int sum = 0;
        byte[] permutation = new byte[length];

        permutation[length - 1] = 1;
        permutation[length - 2] = 0;
        for (int i = length - 3; i >= 0; i--) {
            permutation[i] = (byte) (index % (length - i));
            sum += permutation[i];
            index /= length - i;
            for (int j = i + 1; j < length; j++) {
                if (permutation[j] >= permutation[i]) {
                    permutation[j]++;
                }
            }
        }

        if (sum % 2 != 0) {
            byte temp = permutation[permutation.length - 1];
            permutation[permutation.length - 1] = permutation[permutation.length - 2];
            permutation[permutation.length - 2] = temp;
        }

        return permutation;
    }

    // orientation
    public static int orientationToIndex(byte[] orientation, int nValues) {
        int index = 0;
        for (int i = 0; i < orientation.length; i++) {
            index = nValues * index + orientation[i];
        }

        return index;
    }

    public static byte[] indexToOrientation(int index, int nValues, int length) {
        byte[] orientation = new byte[length];
        for (int i = length - 1; i >= 0; i--) {
            orientation[i] = (byte) (index % nValues);
            index /= nValues;
        }

        return orientation;
    }

    // zero sum orientation
    public static int zeroSumOrientationToIndex(byte[] orientation, int nValues) {
        int index = 0;
        for (int i = 0; i < orientation.length - 1; i++) {
            index = nValues * index + orientation[i];
        }

        return index;
    }

    public static byte[] indexToZeroSumOrientation(int index, int nValues, int length) {
        byte[] orientation = new byte[length];
        orientation[length - 1] = 0;
        for (int i = length - 2; i >= 0; i--) {
            orientation[i] = (byte) (index % nValues);
            index /= nValues;

            orientation[length - 1] += orientation[i];
        }
        orientation[length - 1] = (byte) ((nValues - orientation[length - 1] % nValues) % nValues);

        return orientation;
    }

    // combinations
    private static int nChooseK(int n, int k) {
        int value = 1;

        for (int i = 0; i < k; i++) {
            value *= n - i;
        }

        for (int i = 0; i < k; i++) {
            value /= k - i;
        }

        return value;
    }

    public static int combinationToIndex(boolean[] combination, int k) {
        int index = 0;
        for (int i = combination.length - 1; i >= 0 && k > 0; i--) {
            if (combination[i]) {
                index += nChooseK(i, k--);
            }
        }

        return index;
    }

    public static boolean[] indexToCombination(int index, int k, int length) {
        boolean[] combination = new boolean[length];
        for (int i = length - 1; i >= 0 && k >= 0; i--) {
            if (index >= nChooseK(i, k)) {
                combination[i] = true;
                index -= nChooseK(i, k--);
            }
        }

        return combination;
    }
}