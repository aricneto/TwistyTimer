package com.hatopigeon.cubictimer.utils;
import android.content.res.Resources;
import androidx.annotation.BoolRes;

import com.hatopigeon.cubictimer.CubicTimer;

/**
 * Utility class to facilitate accessing the default arguments for preferences
 */

public final class DefaultPrefs {

    private static Resources mRes;

    private DefaultPrefs() {
    }

    /**
     * Gets the default shared preferences for this application.
     *
     * @return The default shared preferences.
     */
    public static Resources getRes() {
        if (mRes == null) {
            mRes = CubicTimer.getAppContext().getResources();
        }

        return mRes;
    }

    /**
     * Returns the boolean value assigned to the resource key
     *
     * @param defaultResID
     *      The resource key ID
     *
     * @return
     *      The resource value
     */
    public static boolean getBoolean(@BoolRes int defaultResID) {
        return getRes().getBoolean(defaultResID);
    }

}
