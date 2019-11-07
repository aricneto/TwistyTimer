package com.aricneto.twistytimer.items;

import android.graphics.drawable.GradientDrawable;

public class Theme {
    // The name that is shown to the user
    private String                       name;
    // The theme colors
    private String[]                     colors;
    // The theme style (radial, linear...)
    private int                          style;
    // The theme orientation (top-bottom, bottom-top...)
    private GradientDrawable.Orientation orientation;

    public Theme(String name, String... colors) {
        this.name = name;
        this.style = GradientDrawable.LINEAR_GRADIENT;
        this.colors = colors;
        this.orientation = GradientDrawable.Orientation.TOP_BOTTOM;
    }

    public Theme(String name, int style, GradientDrawable.Orientation orientation, String... colors) {
        this.name = name;
        this.style = style;
        this.colors = colors;
        this.orientation = orientation;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getColors() {
        return colors;
    }

    public int getStyle() {
        return style;
    }

    public GradientDrawable.Orientation getOrientation() {
        return orientation;
    }
}
