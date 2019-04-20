package com.aricneto.twistytimer.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.fragment.AlgListFragment;
import com.aricneto.twistytimer.fragment.dialog.AlgSubsetListDialog;
import com.aricneto.twistytimer.fragment.dialog.PuzzleSelectDialog;
import com.aricneto.twistytimer.items.Algorithm;
import com.aricneto.twistytimer.items.AlgorithmModel;
import com.aricneto.twistytimer.items.AlgorithmModel.Case;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Used by the alg list
 */
public final class AlgUtils {

    private static AlgorithmModel mAlgorithmModel;

    /**
     * Returns the size of a puzzle (how many cubies per side, e.g. 333 = 3, 666 = 6)
     */
    public static int getPuzzleSize(String puzzle) {
        switch (puzzle) {
            case "333":
                return 3;
            case "222":
                return 2;
            case "444":
                return 4;
        }
        throw new IllegalArgumentException("Invalid puzzle");
    }

    /**
     * Loads the Algorithms JSON file and assigns it to the appropriate GSON model object
     * @return The loaded GSON model
     */
    public static AlgorithmModel getAlgJsonModel() {
        if (mAlgorithmModel == null) {
            String myJson = StoreUtils.inputStreamToString(TwistyTimer.getAppContext().getResources().openRawResource(R.raw.algorithms));
            mAlgorithmModel = new Gson().fromJson(myJson, AlgorithmModel.class);
        }
        return mAlgorithmModel;
    }

    /**
     * Gets a list of algs from a given puzzle/subset, sourced from the Algorithms JSON file.
     * @param puzzle The puzzle (333, 222, pyra...)
     * @param subset The subset (OLL, CLL, ...)
     * @return A {@link com.aricneto.twistytimer.items.AlgorithmModel.Subset} object containing the
     * appropriate info
     */
    public static AlgorithmModel.Subset getAlgJsonSubsetModel(String puzzle, String subset) {
        for (AlgorithmModel.Subset algSubset : getAlgJsonModel().subsets) {
            if (algSubset.getSubset().equals(subset) && algSubset.getPuzzle().equals(puzzle))
                return algSubset;
        }

        throw new IllegalArgumentException("Subset or puzzle not found");
    }

    /**
     * Whether the subset should be represented in a 2D or isometric view.
     * @param subset
     * @return
     */
    public static boolean isIsometricView(String puzzle, String subset) {
        switch (puzzle + subset) {
            case "333CMLL":
            case "333COLL":
            case "333ELL":
            case "333OLL":
            case "333PLL":
            case "222CLL":
            case "222EG-1":
            case "222EG-2":
            case "222Ortega OLL":
                return false;
        }
        return true;
    }

    private AlgUtils() {

    }

    /**
     * Shows a dialog for the user to choose a puzzle and alg case.
     * @param fragmentManager
     * @return
     */
    public static void showAlgSelectDialog(FragmentManager fragmentManager) {
        PuzzleSelectDialog puzzleSubsetSelectDialog = PuzzleSelectDialog.newInstance();
        puzzleSubsetSelectDialog.setCustomPuzzleList(
                Pair.create(TwistyTimer.getAppContext().getString(R.string.cube_222), R.drawable.ic_2x2),
                Pair.create(TwistyTimer.getAppContext().getString(R.string.cube_333), R.drawable.ic_3x3),
                Pair.create(TwistyTimer.getAppContext().getString(R.string.cube_444), R.drawable.ic_4x4)
        );

        puzzleSubsetSelectDialog.setDialogListener(text -> {
            puzzleSubsetSelectDialog.dismiss();

            AlgSubsetListDialog algSubsetListDialog = AlgSubsetListDialog.newInstance(text);
            algSubsetListDialog.setDialogListener((puzzle, subset) -> {
                fragmentManager
                        .beginTransaction()
                        .replace(R.id.main_activity_container,
                                 AlgListFragment.newInstance(puzzle, subset), "fragment_alg_list")
                        .commit();
                if (algSubsetListDialog != null)
                    algSubsetListDialog.dismiss();
            });

            algSubsetListDialog.show(fragmentManager, "reference_subset_case_select");
        });
        puzzleSubsetSelectDialog.show(fragmentManager, "reference_subset_puzzle_select");
    }

    /**
     * Fetches and returns details from an algorithm from the database. Creates an entry for the
     * algorithm if it doesn't already exist
     * @param puzzle The puzzle name
     * @param subset The algorithm subset
     * @param caseName The algorithm case name
     * @return An {@link Algorithm} object containing data relevant to the algorithm
     */
    public static Algorithm getAlgFromDB (String puzzle, String subset, String caseName) {
        Algorithm algorithm = TwistyTimer.getDBHandler().getAlg(puzzle, subset, caseName);
        if (algorithm == null)
            algorithm = new Algorithm(TwistyTimer.getDBHandler().createAlg(puzzle, subset, caseName),
                                      puzzle, subset, caseName);
        return algorithm;
    }

    private static HashMap<Character, Integer> colorLetterMap;
    private static HashMap<Character, String> colorLetterMapHex;

    /**
     * Returns a hashmap which contains the colors for each face of the cube
     */
    public static HashMap<Character, Integer> getColorLetterHashMap() {
        if (colorLetterMap == null) {
            colorLetterMap = new HashMap<>(7);
            colorLetterMap.put('Y', Color.parseColor("#" + Prefs.getString(R.string.pk_cube_down_color, "FDD835")));
            colorLetterMap.put('R', Color.parseColor("#" + Prefs.getString(R.string.pk_cube_right_color, "EC0000")));
            colorLetterMap.put('G', Color.parseColor("#" + Prefs.getString(R.string.pk_cube_front_color, "02D040")));
            colorLetterMap.put('B', Color.parseColor("#" + Prefs.getString(R.string.pk_cube_back_color, "304FFE")));
            colorLetterMap.put('O', Color.parseColor("#" + Prefs.getString(R.string.pk_cube_left_color, "FF8B24")));
            colorLetterMap.put('W', Color.parseColor("#" + Prefs.getString(R.string.pk_cube_top_color, "FFFFFF")));
            colorLetterMap.put('N', Color.parseColor("#4c4c4c"));
            colorLetterMap.put('X', 0);
        }

        return colorLetterMap;
    }

    /**
     * Returns a hashmap which contains the colors (in hex) for each face of the cube
     */
    public static HashMap<Character, String> getColorLetterHashMapHex() {
        if (colorLetterMapHex == null) {
            colorLetterMapHex = new HashMap<>(7);
            colorLetterMapHex.put('Y', "#" + Prefs.getString(R.string.pk_cube_down_color, "FDD835"));
            colorLetterMapHex.put('R', "#" + Prefs.getString(R.string.pk_cube_right_color, "EC0000"));
            colorLetterMapHex.put('G', "#" + Prefs.getString(R.string.pk_cube_front_color, "02D040"));
            colorLetterMapHex.put('B', "#" + Prefs.getString(R.string.pk_cube_back_color, "304FFE"));
            colorLetterMapHex.put('O', "#" + Prefs.getString(R.string.pk_cube_left_color, "FF8B24"));
            colorLetterMapHex.put('W', "#" + Prefs.getString(R.string.pk_cube_top_color, "FFFFFF"));
            colorLetterMapHex.put('N', "#4c4c4c");
            colorLetterMapHex.put('X', "#000000");
        }

        return colorLetterMapHex;
    }

    public static int[] hexToRGBColor(String hex){
        int red = Integer.valueOf(hex.substring(1, 3), 16);
        int green = Integer.valueOf(hex.substring(3, 5), 16);
        int blue = Integer.valueOf(hex.substring(5, 7), 16);
        return new int[] {red, green, blue};
    }

    public static Drawable getPllArrow(Context context, String name) {
        switch (name) {
            case "H":
                return ContextCompat.getDrawable(context, R.drawable.pll_h_perm);
            case "Ua":
                return ContextCompat.getDrawable(context, R.drawable.pll_ua_perm);
            case "Ub":
                return ContextCompat.getDrawable(context, R.drawable.pll_ub_perm);
            case "Z":
                return ContextCompat.getDrawable(context, R.drawable.pll_z_perm);
            case "Aa":
                return ContextCompat.getDrawable(context, R.drawable.pll_aa_perm);
            case "Ab":
                return ContextCompat.getDrawable(context, R.drawable.pll_ab_perm);
            case "E":
                return ContextCompat.getDrawable(context, R.drawable.pll_e_perm);
            case "F":
                return ContextCompat.getDrawable(context, R.drawable.pll_f_perm);
            case "Ga":
                return ContextCompat.getDrawable(context, R.drawable.pll_ga_perm);
            case "Gb":
                return ContextCompat.getDrawable(context, R.drawable.pll_gb_perm);
            case "Gc":
                return ContextCompat.getDrawable(context, R.drawable.pll_gc_perm);
            case "Gd":
                return ContextCompat.getDrawable(context, R.drawable.pll_gd_perm);
            case "Ja":
                return ContextCompat.getDrawable(context, R.drawable.pll_ja_perm);
            case "Jb":
                return ContextCompat.getDrawable(context, R.drawable.pll_jb_perm);
            case "Na":
                return ContextCompat.getDrawable(context, R.drawable.pll_na_perm);
            case "Nb":
                return ContextCompat.getDrawable(context, R.drawable.pll_nb_perm);
            case "Ra":
                return ContextCompat.getDrawable(context, R.drawable.pll_ra_perm);
            case "Rb":
                return ContextCompat.getDrawable(context, R.drawable.pll_rb_perm);
            case "T":
                return ContextCompat.getDrawable(context, R.drawable.pll_t_perm);
            case "V":
                return ContextCompat.getDrawable(context, R.drawable.pll_v_perm);
            case "Y":
                return ContextCompat.getDrawable(context, R.drawable.pll_y_perm);
        }
        return null;
    }


}
