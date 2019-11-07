package com.aricneto.twistytimer.items;

import android.graphics.drawable.GradientDrawable;

import androidx.annotation.StyleRes;

public class TextStyle {
    // The name that is shown to the user
    private String name;
    // Res id
    private @StyleRes int styleRes;
    // Pref name
    private String prefName;

    public TextStyle(String prefName, int styleRes, String name) {
        this.name = name;
        this.styleRes = styleRes;
        this.prefName = prefName;
    }

    public String getName() {
        return name;
    }

    public int getStyleRes() {
        return styleRes;
    }

    public String getPrefName() {
        return prefName;
    }
}
