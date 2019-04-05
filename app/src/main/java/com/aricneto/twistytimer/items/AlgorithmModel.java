package com.aricneto.twistytimer.items;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Stores algorithms
 */
public class AlgorithmModel {

    @SerializedName("subsets")
    public ArrayList<Subset> subsets;

    public static class Subset {
        @SerializedName("subset")
        String subset;

        @SerializedName("cases")
        ArrayList<Case> cases;

        public String getSubset() {
            return subset;
        }

        public ArrayList<Case> getCases() {
            return cases;
        }
    }

    public static class Case {
        @SerializedName("state")
        String state; // sticker colors

        @SerializedName("name")
        String name; // case name

        @SerializedName("algorithms")
        String[] algorithms;

        public String getState() {
            return state;
        }

        public String getName() {
            return name;
        }

        public String[] getAlgorithms() {
            return algorithms;
        }
    }
}
