package com.aricneto.twistytimer.utils;

import android.view.View;

/**
 * Created by Ari on 15/03/2016.
 */
public class AnimUtils {
    public static void toggleContentVisibility(View... views) {
        for (final View v : views) {
            if (v.getVisibility() == View.GONE) {
                v.setAlpha(0);
                v.setVisibility(View.VISIBLE);
                v.animate()
                        .alpha(1)
                        .start();
            } else {
                v.animate()
                        .alpha(0)
                        .setDuration(100)
                        .withEndAction(() -> v.setVisibility(View.GONE))
                        .start();
            }
        }
    }
}
