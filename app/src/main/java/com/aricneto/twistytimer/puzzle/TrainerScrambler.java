package com.aricneto.twistytimer.puzzle;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.Log;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.items.Algorithm;
import com.aricneto.twistytimer.utils.Prefs;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides scramble algorithms to be used in the trainer
 */
public abstract class TrainerScrambler {
    // The algorithms were taken from
    // github.com/Roman-/oll_trainer TODO: credit them in the app
    private TrainerScrambler() {}

    public static final String KEY_TRAINER = "TRAINER";

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
    public static void saveSelectedItems(TrainerSubset subset, String category, List<Long> selectedItems) {
        Set<String> set = new HashSet<>();

        for (Long item : selectedItems) {
            set.add(item.toString());
        }

        Prefs.getPrefs().edit()
                .putStringSet(KEY_TRAINER + subset.name() + category, set)
                .apply();
    }

    /**
     * Fetches previously selected cases depending on current subset and category
     * @param subset
     * @param category
     * @return
     */
    public static List<String> fetchSelectedItems(TrainerSubset subset, String category) {
        Set<String> prefs = Prefs.getPrefs()
                .getStringSet(KEY_TRAINER + subset.name() + category, new HashSet<>());
        return new ArrayList<>(prefs);
    }

    /**
     * Fetches previously selected cases depending on current subset and category
     * @param subset
     * @param category
     * @return
     */
    public static List<Long> fetchSelectedItemsLong(TrainerSubset subset, String category) {
        List<String> prefs = fetchSelectedItems(subset, category);
        List<Long> list = new ArrayList<>();
        for(String s : prefs) list.add(Long.valueOf(s));
        return list;
    }

    /**
     * Fetches a random scramble from XML file
     */
    public static String fetchRandomScramble(Context context, TrainerSubset subset, String category) {
        Resources resources = context.getResources();
        List<String> selectedItems = fetchSelectedItems(subset, category);

        // Finds a resource with a name matching the subset and a random number from the selected.
        // Ex. oll_5
        Log.d("TrainerScramble", String.valueOf(selectedItems));

        try {
            int resId = resources.getIdentifier(
                    subset.name() + "_" + selectedItems.get(new Random().nextInt(selectedItems.size())),
                    "array",
                    context.getPackageName()
            );

            String[] res = resources.getStringArray(resId);
            return res[new Random().nextInt(res.length)];
        } catch (Exception e) {
            Log.e("TRAINER_SCRAMBLE", "Error retrieving scramble: " + e);
        }

        return "Select your cases first";
    }

}


