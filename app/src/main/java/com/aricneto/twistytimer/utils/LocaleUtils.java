package com.aricneto.twistytimer.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.TwistyTimer;
import java.util.Locale;

/**
 * Utility class to facilitate locale customization by the user
 * Modified from http://gunhansancar.com/change-language-programmatically-in-android/
 */
public class LocaleUtils {

    // The codes used are alpha-2 ISO 639-1, followed by underline
    // and alpha-2 ISO 3166 country/subdivision code if necessary.

    // English is separated into "normal" and "USA" since America has its own date format
    public static final String CZECH             = "cs";
    public static final String CHINESE           = "zh_CN";
    public static final String CATALAN           = "ca_ES";
    public static final String HUNGARIAN         = "hu_HU";
    public static final String INDONESIAN        = "in_ID";
    public static final String HEBREW            = "iw_IL";
    public static final String DUTCH             = "nl_NL";
    public static final String SWEDISH           = "sv_SE";
    public static final String VALENCIAN         = "val_ES";
    public static final String ITALIAN           = "it_IT";
    public static final String ESPERANTO         = "eo_UY";
    public static final String ENGLISH           = "en_UK";
    public static final String ENGLISH_USA       = "en_US";
    public static final String SPANISH           = "es_ES";
    public static final String GERMAN            = "de";
    public static final String FRENCH            = "fr";
    public static final String RUSSIAN           = "ru";
    public static final String PORTUGUESE_BRAZIL = "pt_BR";
    public static final String LITHUANIAN        = "lt";
    public static final String POLISH            = "pl";
    public static final String SERBIAN_LATIN     = "sr_CS";
    public static final String CROATIAN          = "hr";



    public static Context updateLocale(Context context) {
        // toString returns language + "_" + country + "_" + (variant + "_#" | "#") + script + "-" + extensions
        String language = Prefs.getString(R.string.pk_locale, Locale.getDefault().toString());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return updateResources(context, language);
        } else {
            return updateResourcesLegacy(context, language);
        }
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
        updateLocale(TwistyTimer.getAppContext());
    }

    @TargetApi(Build.VERSION_CODES.N)
    private static Context updateResources(Context context, String language) {
        Locale locale = fetchLocaleFromString(language);
        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration);
    }

    /**
     * Updates the app to use the new locale
     * This method was deprecated in newer versions of Android. See {@code updateResources}
     * for a method that'll work from API version N and above
     *
     * @param language the language code (use one of the constants)
     */
    private static Context updateResourcesLegacy(Context context, String language) {
        Locale locale = fetchLocaleFromString(language);

        Locale.setDefault(locale);

        Resources resources = TwistyTimer.getAppContext().getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());

        // NOTE: The following methods are deprecated, but the correct way to do it (with setLocale and then
        // createConfigurationContext) does not work on Lollipop for some reason. I'm leaving it this
        // way since, as far as I've read, these functions don't pose any serious issues.
        configuration.locale = locale;
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        return context;
    }

    /**
     * Converts a string like pt_BR into its appropriate locale object
     * @param language
     * @return
     */
    private static Locale fetchLocaleFromString(String language) {
        /**
         * If a language has a country code, we have to create a {@link Locale} with a country parameter.
         */
        if (language.length() > 2) {
            // e.g.: pt_BR becomes "pt" (index 0) and "BR" (index 1)
            String[] localeString = language.split("_|-");
            return new Locale(localeString[0], localeString[1]);
        } else {
            // e.g.: en, pt, fr
            return new Locale(language);
        }
    }
}