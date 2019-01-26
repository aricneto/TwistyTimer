package com.aricneto.twistytimer.puzzle;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.Log;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.items.Algorithm;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Provides scramble algorithms to be used in the trainer
 */
public abstract class TrainerScrambler {
    // The algorithms were taken from
    // github.com/Roman-/oll_trainer TODO: credit them in the app
    private TrainerScrambler() {}

    public static enum TrainerSubset {
        OLL, PLL
    };

    // Amount of different variations for each registered subset
    private static int getSubsetVariations(TrainerSubset subset) {
        switch (subset) {
            case OLL:
                return 57;
            case PLL:
                return 0;
        }
        return 0;
    }

    // Fetches a random scramble from XML file
    public static String fetchRandomScramble(Context context, List<Long> selectedItems, TrainerSubset subset) {
        Resources resources = context.getResources();
        TypedArray array;

        Class<R.array> res = R.array.class;
        try {
            Field field = res.getField(subset.name() + "_" + selectedItems.get(new Random().nextInt(selectedItems.size())));
            array = resources.obtainTypedArray(field.getInt(null));
            return  array.getString(new Random().nextInt(getSubsetVariations(subset)));
        } catch (Exception e) {
            Log.e("TRAINER_SCRAMBLE", "Error retrieving scramble: " + e);
        }

        return "Error retrieving scramble";
    }

}


