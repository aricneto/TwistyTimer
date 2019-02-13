package com.aricneto.twistytimer.puzzle;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.Log;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.items.Algorithm;
import com.aricneto.twistytimer.utils.AlgUtils;
import com.aricneto.twistytimer.utils.Prefs;
import com.aricneto.twistytimer.utils.PuzzleUtils;

import net.gnehzr.tnoodle.scrambles.InvalidScrambleException;
import net.gnehzr.tnoodle.scrambles.Puzzle;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import puzzle.CubePuzzle;
import puzzle.ThreeByThreeCubePuzzle;

/**
 * Provides scramble algorithms to be used in the trainer
 */
public abstract class TrainerScrambler {
    // The algorithms were taken from
    // github.com/Roman-/oll_trainer TODO: credit them in the app
    private static Random random = new Random();

    private static String[] Y_ROTATIONS = {"", "y", "y2", "y'"};

    private static CubePuzzle puzzle = new ThreeByThreeCubePuzzle();
    private static CubePuzzle.CubeState solved = puzzle.getSolvedState();

    private TrainerScrambler() {}

    // The key preceding every trainer entry.
    // The version MUST NOT be decreased, only increased, as previous users may have
    // configurations saved in a different TRAINER version that is incompatible with the current
    // implementation, causing crashes.
    public static final String KEY_TRAINER = "TRAINER_V2";

    public static enum TrainerSubset {
        OLL, PLL
    };

    /**
     *  Amount of different variations for each registered subset
     */
    private static int getSubsetVariations(TrainerSubset subset) {
        switch (subset) {
            case OLL:
                return 57;
            case PLL:
                return 0;
        }
        return 0;
    }

    /**
     * Saves selected cases to preferences, to be fetched later
     * Preference will be saved in the format:
     *      key: TRAINER[SUBSET][CATEGORY]
     *      set: ITEMS
     */
    public static void saveSelectedItems(TrainerSubset subset, String category, Set<String> selectedItems) {
        Prefs.getPrefs().edit()
                .putStringSet(KEY_TRAINER + subset.name() + category, selectedItems)
                .apply();
    }

    /**
     * Saves selected cases to preferences, to be fetched later. Accepts a List<String> input.
     * Preference will be saved in the format:
     *      key: TRAINER[SUBSET][CATEGORY]
     *      set: ITEMS
     */
    public static void saveSelectedItems(TrainerSubset subset, String category, List<String> selectedItems) {
        saveSelectedItems(subset, category, new HashSet<>(selectedItems));
    }

    /**
     * Utility function to rename a Trainer category, maintaining trainer subsets
     * @param subset
     * @param oldCategoryName
     * @param newCategoryName
     */
    public static void renameCategory(TrainerSubset subset, String oldCategoryName, String newCategoryName) {
        Set<String> items = fetchSelectedItems(subset, oldCategoryName);
        Prefs.getPrefs().edit().remove(KEY_TRAINER + subset.name() + oldCategoryName).apply();
        saveSelectedItems(subset, newCategoryName, items);
    }

    /**
     * Fetches previously selected cases depending on current subset and category
     * @param subset
     * @param category
     * @return
     */
    public static Set<String> fetchSelectedItems(TrainerSubset subset, String category) {
        return Prefs.getPrefs()
                .getStringSet(KEY_TRAINER + subset.name() + category, new HashSet<>());
    }

    /**
     * Generates a random trainer case from the selected cases
     */
    public static String generateTrainerCase(Context context, TrainerSubset subset, String category) {
        List<String> selectedItems = new ArrayList<>(fetchSelectedItems(subset, category));
        String caseAlg = "";
        String scramble = "";

        CubePuzzle.CubeState state = null;

        if (selectedItems.size() != 0) {
            try {
                // Fetch a random setup algorithm and set it as the cube state
                caseAlg = fetchCaseAlgorithm(context, subset.name(), selectedItems.get(random.nextInt(selectedItems.size())));
                state = (CubePuzzle.CubeState) solved.applyAlgorithm(caseAlg);

                // Solve the state
                scramble = ((ThreeByThreeCubePuzzle) puzzle).solveIn(state, 20, null, null);
            } catch (InvalidScrambleException e) {
                e.printStackTrace();
            }
        } else {
            scramble = context.getString(R.string.trainer_help_message);
        }

        return PuzzleUtils.applyRotationForAlgorithm(scramble, Y_ROTATIONS[random.nextInt(4)]);
    }

    private static String fetchCaseAlgorithm(Context context, String subset, String name) {
        Resources resources = context.getResources();

        // Finds an algorithm resource with a matching name on the file trainer_scrambles.xml

        try {
            // Find the resource
            int resId = resources.getIdentifier(
                    "TRAINER_" + subset + "_" + name.replace(" ", "_").toUpperCase(),
                    "array",
                    context.getPackageName()
            );

            // Split the resouce entries
            String[] res = resources.getStringArray(resId);

            // Return one of the entries
            return res[random.nextInt(res.length)];
        } catch (Exception e) {
            Log.e("TRAINER_SCRAMBLE", "Error retrieving scramble: " + e);
        }

        return "U";
    }

}


