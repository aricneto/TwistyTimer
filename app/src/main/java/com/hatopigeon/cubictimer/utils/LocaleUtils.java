package com.hatopigeon.cubictimer.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.Pair;

import com.hatopigeon.cubicify.R;
import com.hatopigeon.cubictimer.CubicTimer;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;

/**
 * Utility class to facilitate locale customization by the user
 * Modified from http://gunhansancar.com/change-language-programmatically-in-android/
 */
public class LocaleUtils {

    // The codes used are alpha-2 ISO 639-1, followed by underline
    // and alpha-2 ISO 3166 country/subdivision code if necessary.

    // English is separated into "normal" and "USA" since America has its own date format
    public static final String CZECH             = "cs_CZ";
    public static final String CHINESE           = "zh_CN";
    public static final String CHINESE_HK        = "zh_HK";
    public static final String CHINESE_TW        = "zh_TW";
    public static final String CATALAN           = "ca_ES";
    public static final String HUNGARIAN         = "hu_HU";
    public static final String VIETNAMESE        = "vi_VN";
    public static final String ARABIC            = "ar_SA";
    public static final String TAMIL             = "ta_IN";
    public static final String INDONESIAN        = "in_ID";
    public static final String HEBREW            = "iw_IL";
    public static final String DUTCH             = "nl_NL";
    public static final String SWEDISH           = "sv_SE";
    public static final String VALENCIAN         = "val_ES";
    public static final String ITALIAN           = "it_IT";
    public static final String ESPERANTO         = "eo_UY";
    public static final String ENGLISH           = "en_GB";
    public static final String ENGLISH_USA       = "en_US";
    public static final String SPANISH           = "es_ES";
    public static final String GERMAN            = "de_DE";
    public static final String FRENCH            = "fr_FR";
    public static final String RUSSIAN           = "ru_RU";
    public static final String PORTUGUESE_BRAZIL = "pt_BR";
    public static final String LITHUANIAN        = "lt_LT";
    public static final String POLISH            = "pl_PL";
    public static final String SERBIAN_LATIN     = "sr_CS";
    public static final String CROATIAN          = "hr_HR";
    public static final String TURKISH           = "tr_TR";
    public static final String SLOVAK            = "sk_SK";
    public static final String JAPANESE          = "ja_JP";
    public static final String HINDI             = "hi_IN";
    public static final String UKRANIAN          = "uk_UA";



    public static Context updateLocale(Context context) {
        // toString returns language + "_" + country + "_" + (variant + "_#" | "#") + script + "-" + extensions
        String language = Prefs.getString(R.string.pk_locale, Locale.getDefault().toString());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
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

    private static LinkedHashMap<String, Pair<Integer, Integer>> localeHash = null;

    /**
     * Returns a HashMap containing {@link Pair} with each language name and flag resource ID.
     * The languages are keyed by the locale code
     */
    public static LinkedHashMap getLocaleHashMap() {
        if (localeHash == null) {
            localeHash = new LinkedHashMap<String, Pair<Integer, Integer>>() {
                {
                    put(ENGLISH, new Pair<>(R.string.language_english, R.drawable.flag_united_kingdom));
                    put(ENGLISH_USA, new Pair<>(R.string.language_english, R.drawable.flag_united_states));
                    put(SPANISH, new Pair<>(R.string.language_spanish, R.drawable.flag_spain));
                    put(GERMAN, new Pair<>(R.string.language_german, R.drawable.flag_germany));
                    put(FRENCH, new Pair<>(R.string.language_french, R.drawable.flag_france));
                    put(RUSSIAN, new Pair<>(R.string.language_russian, R.drawable.flag_russia));
                    put(UKRANIAN, new Pair<>(R.string.language_ukranian, R.drawable.flag_ukraine));
                    put(PORTUGUESE_BRAZIL, new Pair<>(R.string.language_portuguese_br, R.drawable.flag_brazil));
                    put(CZECH, new Pair<>(R.string.language_czech, R.drawable.flag_czech_republic));
                    put(LITHUANIAN, new Pair<>(R.string.language_lithuanian, R.drawable.flag_lithuania));
                    put(POLISH, new Pair<>(R.string.language_polish, R.drawable.flag_poland));
                    put(CHINESE, new Pair<>(R.string.language_chinese, R.drawable.flag_china));
                    //put(CHINESE_HK, new Pair<>(R.string.language_chinese_hk, R.drawable.flag_hongkong));
                    //put(CHINESE_TW, new Pair<>(R.string.language_chinese_tw, R.drawable.flag_taiwan));
                    //put(CATALAN, new Pair<>(R.string.language_catalan, R.drawable.flag_spain));
                    put(INDONESIAN, new Pair<>(R.string.language_indonesian, R.drawable.flag_indonesia));
                    put(HEBREW, new Pair<>(R.string.language_hebrew, R.drawable.flag_israel));
                    put(DUTCH, new Pair<>(R.string.language_dutch, R.drawable.flag_netherlands));
                    put(SWEDISH, new Pair<>(R.string.language_swedish, R.drawable.flag_sweden));
                    put(VALENCIAN, new Pair<>(R.string.language_valencian, R.drawable.flag_spain));
                    put(ESPERANTO, new Pair<>(R.string.language_esperanto, R.drawable.flag_esperanto));
                    put(ITALIAN, new Pair<>(R.string.language_italian, R.drawable.flag_italy));
                    put(CROATIAN, new Pair<>(R.string.language_croatian, R.drawable.flag_croatia));
                    put(SERBIAN_LATIN, new Pair<>(R.string.language_serbian, R.drawable.flag_serbia));
                    put(TURKISH, new Pair<>(R.string.language_turkish, R.drawable.flag_turkey));
                    put(SLOVAK, new Pair<>(R.string.language_slovak, R.drawable.flag_slovakia));
                    put(JAPANESE, new Pair<>(R.string.language_japanese, R.drawable.flag_japan));
                    //put(HUNGARIAN, new Pair<>(R.string.language_hungarian, R.drawable.flag_hungary));
                    put(VIETNAMESE, new Pair<>(R.string.language_vietnamese, R.drawable.flag_vietnam));
                    put(ARABIC, new Pair<>(R.string.language_arabic, R.drawable.flag_sudan));
                    //put(TAMIL, new Pair<>(R.string.language_tamil, R.drawable.flag_india));
                    put(HINDI, new Pair<>(R.string.language_hindi, R.drawable.flag_india));
                }
            };
        }

        return localeHash;
    }

    /**
     * Returns an array with all available locale codes
     * @return
     */
    public static String[] getLocaleArray() {
        return (String[]) getLocaleHashMap().keySet().toArray(new String[0]);
    }

    /**
     * Sets current locale
     *
     * @param language the language code (use one of the constants)
     */
    public static void setLocale(String language) {
        Prefs.edit().putString(R.string.pk_locale, language).apply();
        updateLocale(CubicTimer.getAppContext());
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
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

        Resources resources = CubicTimer.getAppContext().getResources();
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