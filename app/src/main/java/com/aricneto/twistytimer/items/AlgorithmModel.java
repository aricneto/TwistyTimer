package com.aricneto.twistytimer.items;

import android.os.Parcel;
import android.os.Parcelable;

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

    public static class Case implements Parcelable {
        public static final int FACE_U = 0;
        public static final int FACE_F = 1;
        public static final int FACE_R = 2;
        public static final int FACE_B = 3;
        public static final int FACE_L = 4;
        public static final int FACE_D = 5;
        /**
         * Sticker colors. The array is arranged with the faces described as such:
         * [U, F, R, B, L, D]
         * Stickers are read from the top-left, left to right
         */
        @SerializedName("state")
        String[] state; // sticker colors
        @SerializedName("name")
        String name; // case name
        @SerializedName("algorithms")
        String[] algorithms;

        public static final Creator<Case> CREATOR = new Creator<Case>() {
            @Override
            public Case createFromParcel(Parcel in) {
                return new Case(in);
            }

            @Override
            public Case[] newArray(int size) {
                return new Case[size];
            }
        };

        protected Case(Parcel in) {
            name = in.readString();
            state = in.createStringArray();
            algorithms = in.createStringArray();
        }

        public String[] getState() {
            return state;
        }

        public String getName() {
            return name;
        }

        public String[] getAlgorithms() {
            return algorithms;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(name);
            dest.writeStringArray(state);
            dest.writeStringArray(algorithms);
        }
    }
}
