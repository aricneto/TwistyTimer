package com.hatopigeon.cubictimer.items;

import com.hatopigeon.cubictimer.utils.PuzzleUtils;
import com.hatopigeon.cubictimer.utils.ThemeUtils;

import androidx.annotation.StyleRes;

public class Theme {
    // The name that is saved in preferences
    private String prefName;
    // The name that is shown to the user
    private String name;
    // The style resource ID
    private @StyleRes int resId;

    public Theme(String prefName, String name) {
        this.prefName = prefName;
        this.name = name;
        this.resId = ThemeUtils.getThemeStyleRes(prefName);
    }

    public String getPrefName() {
        return prefName;
    }

    public void setPrefName(String prefName) {
        this.prefName = prefName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getResId() {
        return resId;
    }

    public void setResId(int resId) {
        this.resId = resId;
    }
}
