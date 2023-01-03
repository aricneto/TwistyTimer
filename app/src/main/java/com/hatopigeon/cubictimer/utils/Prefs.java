package com.hatopigeon.cubictimer.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.BoolRes;
import androidx.annotation.IntegerRes;
import androidx.annotation.StringRes;

import com.hatopigeon.cubictimer.CubicTimer;

import java.util.Set;

/**
 * Utility class to access the default shared preferences.
 *
 * @author damo
 */
public final class Prefs {
    /**
     * The preferences instance. There is only one shared preferences instance per preferences
     * file for each process, so this can be cached safely and will reflect any changes made by
     * any other code that makes changes to the preferences.
     */
    private static SharedPreferences sPrefs;

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private Prefs() {
    }

    /**
     * Gets the default shared preferences for this application.
     *
     * @return The default shared preferences.
     */
    public static SharedPreferences getPrefs() {
        if (sPrefs == null) {
            sPrefs = PreferenceManager.getDefaultSharedPreferences(CubicTimer.getAppContext());
        }

        return sPrefs;
    }

    /**
     * Returns an {@link Integer} given an {@link IntegerRes}
     * @param res an {@link IntegerRes}
     * @return an {@link Integer} associated with the given {@link IntegerRes}
     */
    public static int getDefaultIntValue(@IntegerRes int res) {
        return CubicTimer.getAppContext().getResources().getInteger(res);
    }

    /**
     * Returns a {@link Boolean} given a {@link BoolRes}
     * @param res an {@link BoolRes}
     * @return an {@link Boolean} associated with the given {@link BoolRes}
     */
    public static boolean getDefaultBoolValue(@BoolRes int res) {
        return CubicTimer.getAppContext().getResources().getBoolean(res);
    }

    /**
     * Gets the string value of a shared preference.
     *
     * @param prefKeyResID
     *     The string resource ID for the name of the preference. See {@code values/pref_keys.xml}.
     * @param defaultValue
     *     The default preference value to return if the preference is not defined.
     *
     * @return
     *     The value of the preference, or the given default value if the preference is not defined.
     */
    public static String getString(@StringRes int prefKeyResID, String defaultValue) {
        return getPrefs().getString(
                CubicTimer.getAppContext().getString(prefKeyResID), defaultValue);
    }

    /**
     * Gets the integer value of a shared preference.
     *
     * @param prefKeyResID
     *     The string resource ID for the name of the preference. See {@code values/pref_keys.xml}.
     * @param defaultValue
     *     The default preference value to return if the preference is not defined.
     *
     * @return
     *     The value of the preference, or the given default value if the preference is not defined.
     */
    public static int getInt(@StringRes int prefKeyResID, int defaultValue) {
        return getPrefs().getInt(
                CubicTimer.getAppContext().getString(prefKeyResID), defaultValue);
    }

    /**
     * Gets the Boolean value of a shared preference.
     *
     * @param prefKeyResID
     *     The string resource ID for the name of the preference. See {@code values/pref_keys.xml}.
     * @param defaultValue
     *     The default preference value to return if the preference is not defined.
     *
     * @return
     *     The value of the preference, or the given default value if the preference is not defined.
     */
    public static boolean getBoolean(@StringRes int prefKeyResID, boolean defaultValue) {
        return getPrefs().getBoolean(
                CubicTimer.getAppContext().getString(prefKeyResID), defaultValue);
    }

    /**
     * <p>
     * Identifies the resource ID from those given whose string value matches the given shared
     * preferences key. This is useful when constructing {@code switch} statements. For example:
     * </p>
     * <pre>
     * String key = preference.getKey();
     *
     * switch (keyToResourceID(key, R.string.pk_1, R.string.pk_2, R.string.pk_3)) {
     *     case R.string.pk_1:
     *         // Do something.
     *         break;
     *     case R.string.pk_2:
     *         // Do something else.
     *         break;
     *     case R.string.pk_3:
     *         // Do something entirely different.
     *         break;
     *     default:
     *         // "key" did not match any of the string value of any of the given resource IDs.
     *         break;
     * }
     * </pre>
     *
     * @param key
     *     The string value of the shared preferences key to be matched to a resource ID.
     * @param prefKeyResIDs
     *     Any number of string resource IDs. See {@code values/pref_keys.xml}.
     *
     * @return
     *     The first resource ID whose string value matches the given key; or zero if the key is
     *     {@code null}, there are no resource IDs given, or the key does not match the string
     *     value of any of the given string resources.
     */
    public static int keyToResourceID(String key, @StringRes int... prefKeyResIDs) {
        if (key != null && prefKeyResIDs != null && prefKeyResIDs.length > 0) {
            final Context context = CubicTimer.getAppContext();

            for (int resID : prefKeyResIDs) {
                if (key.equals(context.getString(resID))) {
                    return resID;
                }
            }
        }
        return 0;
    }

    /**
     * Gets an editor for the default shared preferences. When editing is complete, call
     * {@link Editor#apply()} on the editor to save the changes.
     */
    @SuppressLint("CommitPrefEdits")
    public static Editor edit() {
        return new Editor(getPrefs().edit());
    }

    /**
     * A simple wrapper for the shared preference editor that provides a easy way to use string
     * resource IDs when setting preference values.
     */
    public static class Editor {
        /**
         * The shared preferences editor wrapped by this helper class.
         */
        private final SharedPreferences.Editor mSPEditor;

        /**
         * Creates a new editor helper that wraps the given shared preferences editor.
         *
         * @param spEditor The shared preferences editor to be wrapped.
         */
        private Editor(SharedPreferences.Editor spEditor) {
            mSPEditor = spEditor;
        }

        /**
         * Commits any changes made using this editor.
         */
        public void apply() {
            mSPEditor.apply();
        }

        /**
         * Sets the value of a shared preference to the given string.
         *
         * @param prefKeyResID
         *     The string resource ID for the name of the preference key.
         *     See {@code values/pref_keys.xml}.
         * @param value
         *     The new value of the preference.
         *
         * @return
         *     This editor, to allow method calls to be chained.
         */
        public Editor putString(@StringRes int prefKeyResID, String value) {
            mSPEditor.putString(CubicTimer.getAppContext().getString(prefKeyResID), value);
            return this;
        }

        /**
         * Sets the value of a shared preference to the given string set.
         *
         * @param prefKeyResID
         *     The string resource ID for the name of the preference key.
         *     See {@code values/pref_keys.xml}.
         * @param value
         *     The new value of the preference.
         *
         * @return
         *     This editor, to allow method calls to be chained.
         */
        public Editor putStringSet(@StringRes int prefKeyResID, Set<String> value) {
            mSPEditor.putStringSet(CubicTimer.getAppContext().getString(prefKeyResID), value);
            return this;
        }

        /**
         * Sets the value of a shared preference to the given integer.
         *
         * @param prefKeyResID
         *     The string resource ID for the name of the preference key.
         *     See {@code values/pref_keys.xml}.
         * @param value
         *     The new value of the preference.
         *
         * @return
         *     This editor, to allow method calls to be chained.
         */
        public Editor putInt(@StringRes int prefKeyResID, int value) {
            mSPEditor.putInt(CubicTimer.getAppContext().getString(prefKeyResID), value);
            return this;
        }

        /**
         * Sets the value of a shared preference to the given Boolean.
         *
         * @param prefKeyResID
         *     The string resource ID for the name of the preference key.
         *     See {@code values/pref_keys.xml}.
         * @param value
         *     The new value of the preference.
         *
         * @return
         *     This editor, to allow method calls to be chained.
         */
        public Editor putBoolean(@StringRes int prefKeyResID, boolean value) {
            mSPEditor.putBoolean(CubicTimer.getAppContext().getString(prefKeyResID), value);
            return this;
        }
    }
}
