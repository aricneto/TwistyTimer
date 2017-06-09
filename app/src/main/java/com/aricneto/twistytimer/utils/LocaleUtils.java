package com.aricneto.twistytimer.utils;

import android.content.res.Configuration;
import android.content.res.Resources;
import com.aricneto.twistify.R;
import com.aricneto.twistytimer.TwistyTimer;
import java.util.Locale;

/**
 * Utility class to facilitate locale customization by the user
 * Modified from http://gunhansancar.com/change-language-programmatically-in-android/
 */
public class LocaleUtils {

    // The codes used are alpha-2 ISO 639-1, followed by underline
    // and alpha-2 ISO 3166 country code if necessary.

    // English is separated into "normal" and "USA" since America has its own date format
    public static final String ENGLISH           = "en_UK";
    public static final String ENGLISH_USA       = "en_US";
    public static final String SPANISH           = "es";
    public static final String GERMAN            = "de";
    public static final String FRENCH            = "fr";
    public static final String RUSSIAN           = "ru";
    public static final String PORTUGUESE_BRAZIL = "pt_BR";
    public static final String CZECH             = "cs";
    public static final String LITHUANIAN        = "lt";
    public static final String POLISH            = "pl";

    public static void onCreate() {
        // toString returns language + "_" + country + "_" + (variant + "_#" | "#") + script + "-" + extensions
        String language = Prefs.getString(R.string.pk_locale, Locale.getDefault().toString());
        updateResources(language);
    }

    /**
     * Gets current locale
     *
     * @return the locale
     */
    public static String getLocale() {
        return Prefs.getString(R.string.pk_locale, Locale.getDefault().getLanguage());
    }

    /**
     * Sets current locale
     *
     * @param language the language code (use one of the constants)
     */
    public static void setLocale(String language) {
        Prefs.edit().putString(R.string.pk_locale, language).apply();
        updateResources(language);
    }

    /**
     * Updates the app to use the new locale
     *
     * @param language the language code (use one of the constants)
     */
    private static void updateResources(String language) {

        Locale locale;
        /**
         * If a language has a country code, we have to create a {@link Locale} with a country parameter.
          */
        if (language.length() > 2) {
            // e.g.: pt_BR becomes "pt" (index 0) and "BR" (index 1)
            String[] localeString = language.split("_|-");
            locale = new Locale(localeString[0], localeString[1]);
        } else {
            // e.g.: en, pt, fr
            locale = new Locale(language);
        }

        Locale.setDefault(locale);

        Resources resources = TwistyTimer.getAppContext().getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());

        // NOTE: The following methods are deprecated, but the correct way to do it (with setLocale and then
        // createConfigurationContext) does not work on Lollipop for some reason. I'm leaving it this
        // way since, as far as I've read, these functions don't pose any serious issues.
        configuration.locale = locale;
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
    }
}