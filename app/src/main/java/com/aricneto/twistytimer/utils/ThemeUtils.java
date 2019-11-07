package com.aricneto.twistytimer.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

import androidx.annotation.AttrRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StyleRes;
import androidx.annotation.StyleableRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import android.text.style.ImageSpan;
import android.util.TypedValue;

import com.afollestad.materialdialogs.MaterialDialog;
import com.aricneto.twistify.R;
import com.aricneto.twistytimer.items.TextStyle;
import com.aricneto.twistytimer.items.Theme;

import java.util.LinkedHashMap;

/**
 * Utility class to make themeing easier.
 */
public final class ThemeUtils {

    public static final String THEME_INDIGO      = "indigo";
    public static final String THEME_PURPLE      = "purple";
    public static final String THEME_TEAL        = "teal";
    public static final String THEME_PINK        = "pink";
    public static final String THEME_RED         = "red";
    public static final String THEME_BROWN       = "brown";
    public static final String THEME_BLUE        = "blue";
    public static final String THEME_CYAN        = "cyan";
    public static final String THEME_LIGHT_BLUE  = "light_blue";
    public static final String THEME_BLACK       = "black";
    public static final String THEME_ORANGE      = "orange";
    public static final String THEME_GREEN       = "green";
    public static final String THEME_LIGHT_GREEN = "light_green";
    public static final String THEME_DEEPPURPLE  = "deeppurple";
    public static final String THEME_BLUEGRAY    = "bluegray";
    public static final String THEME_WHITE       = "white";
    public static final String THEME_YELLOW      = "yellow";
    public static final String THEME_WHITE_GREEN = "white_green";
    public static final String THEME_DAWN        = "dawn";
    public static final String THEME_BLUY_GRAY   = "bluy_gray";
    public static final String THEME_TURTLY_SEA  = "turtly_sea";
    public static final String THEME_PIXIE_FALLS = "pixie_falls";
    public static final String THEME_WANDERING_DUSK = "wandering_dusk";
    public static final String THEME_SPOTTY_GUY = "spotty_guy";
    public static final String THEME_FAIRY_FROG = "fairy_frog";


    public static final String TEXT_DEFAULT   = "default";
    public static final String TEXT_PESSOA    = "pessoa";
    public static final String TEXT_BURGESS   = "burgess";
    public static final String TEXT_LOU       = "lou";
    public static final String TEXT_BOWIE     = "bowie";
    public static final String TEXT_BRIE      = "brie";
    public static final String TEXT_MATSSON   = "matsson";
    public static final String TEXT_ISAKOV    = "isakov";
    public static final String TEXT_ADAMS     = "adams";
    public static final String TEXT_IRWIN     = "irwin";
    public static final String TEXT_TARKOVSKY = "tarkovsky";
    public static final String TEXT_EBERT     = "ebert";
    public static final String TEXT_TOLKIEN   = "tolkien";
    public static final String TEXT_ASIMOV    = "asimov";
    public static final String TEXT_KUBRICK   = "kubrick";

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private ThemeUtils() {
    }

    /**
     * Gets the user's preferred theme. This is the theme that has been selected and saved to the
     * settings (or the default theme); it is not necessarily the same as the theme that is
     * currently applied to the user interface.
     *
     * @return The user's chosen preferred theme.
     */
    public static Theme getPreferredTheme() {
        return getAllThemesHashmap().get(Prefs.getString(R.string.pk_theme, "indigo"));
    }

    public static TextStyle getPreferredTextStyle() {
        return getAllTextStylesHashMap().get(Prefs.getString(R.string.pk_text_style, "default"));
    }

    private static LinkedHashMap<String, Theme> themesHashMap = null;
    private static Theme[] themes = null;


    public static LinkedHashMap<String, Theme> getAllThemesHashmap() {
        if (themesHashMap == null) {
            themesHashMap = new LinkedHashMap<>();
            themesHashMap.put(THEME_INDIGO, new Theme("Hazy Blues",));
            themesHashMap.put(THEME_GREEN, new Theme("What... Green?",));
            themesHashMap.put(THEME_SPOTTY_GUY, new Theme("Spotty Guy",));
            themesHashMap.put(THEME_BLACK, new Theme("Simply Black",));
            themesHashMap.put(THEME_WHITE, new Theme("Simply White",));
            themesHashMap.put(THEME_YELLOW, new Theme("Notably Yellow",));
            themesHashMap.put(THEME_BLUY_GRAY, new Theme("Bluy Gray",));
            themesHashMap.put(THEME_TURTLY_SEA, new Theme("Turtly Sea",));
            themesHashMap.put(THEME_PIXIE_FALLS, new Theme("Pixie Falls",));
            themesHashMap.put(THEME_RED, new Theme("Oof Hot",));
            themesHashMap.put(THEME_WANDERING_DUSK, new Theme("Wandy Dusk",));
            themesHashMap.put(THEME_DAWN, new Theme("Relaxing Dawn",));
            themesHashMap.put(THEME_FAIRY_FROG, new Theme("Fairy Frog",));
            themesHashMap.put(THEME_DEEPPURPLE, new Theme("Quite Purply",));
            themesHashMap.put(THEME_BLUE, new Theme("Even Purplier",));
            themesHashMap.put(THEME_PURPLE, new Theme("Definitely Purple",));
            themesHashMap.put(THEME_ORANGE, new Theme("Tantalizing Torange",));
            themesHashMap.put(THEME_PINK, new Theme("Pinky Promises",));
            themesHashMap.put(THEME_BROWN, new Theme("Delicious Brownie",));
            themesHashMap.put(THEME_TEAL, new Theme("Earthy Teal",));
            themesHashMap.put(THEME_LIGHT_GREEN, new Theme("Greeny Gorilla",));
            themesHashMap.put(THEME_LIGHT_BLUE, new Theme("Lightly Skyish",));
            themesHashMap.put(THEME_CYAN, new Theme("Cyanic Teal",));
            themesHashMap.put(THEME_BLUEGRAY, new Theme("Icy Hills",));
            themesHashMap.put(THEME_WHITE_GREEN, new Theme("Greeny Everest",));
        }
        return themesHashMap;
    }

    public static Theme[] getAllThemes() {
        if (themes == null)
            themes = getAllThemesHashmap().values().toArray(new Theme[0]);
        return themes;
    }

    private static LinkedHashMap<String, TextStyle> textStyleHashMap = null;
    private static TextStyle[] textStyles = null;

    public static LinkedHashMap<String, TextStyle> getAllTextStylesHashMap() {
        if (textStyleHashMap == null) {
            textStyleHashMap = new LinkedHashMap<>();
            textStyleHashMap.put(TEXT_DEFAULT, new TextStyle(TEXT_DEFAULT, R.style.TextStyleDefault, "Default"));
            textStyleHashMap.put(TEXT_PESSOA, new TextStyle(TEXT_PESSOA, R.style.TextStylePessoa, "Pessoa"));
            textStyleHashMap.put(TEXT_LOU, new TextStyle(TEXT_LOU, R.style.TextStyleLou, "Lou"));
            textStyleHashMap.put(TEXT_BURGESS, new TextStyle(TEXT_BURGESS, R.style.TextStyleBurgess, "Burgess"));
            textStyleHashMap.put(TEXT_BOWIE, new TextStyle(TEXT_BOWIE, R.style.TextStyleBowie, "Bowie"));
            textStyleHashMap.put(TEXT_BRIE, new TextStyle(TEXT_BRIE, R.style.TextStyleBrie, "Brie"));
            textStyleHashMap.put(TEXT_MATSSON, new TextStyle(TEXT_MATSSON, R.style.TextStyleMatsson, "Matsson"));
            textStyleHashMap.put(TEXT_ISAKOV, new TextStyle(TEXT_ISAKOV, R.style.TextStyleIsakov, "Isakov"));
            textStyleHashMap.put(TEXT_ADAMS, new TextStyle(TEXT_ADAMS, R.style.TextStyleAdams, "Adams"));
            textStyleHashMap.put(TEXT_IRWIN, new TextStyle(TEXT_IRWIN, R.style.TextStyleIrwin, "Irwin"));
            textStyleHashMap.put(TEXT_TARKOVSKY, new TextStyle(TEXT_TARKOVSKY, R.style.TextStyleTarkovsky, "Tarkovsky"));
            textStyleHashMap.put(TEXT_EBERT, new TextStyle(TEXT_EBERT, R.style.TextStyleEbert, "Ebert"));
            textStyleHashMap.put(TEXT_TOLKIEN, new TextStyle(TEXT_TOLKIEN, R.style.TextStyleTolkien, "Tolkien"));
            textStyleHashMap.put(TEXT_ASIMOV, new TextStyle(TEXT_ASIMOV, R.style.TextStyleAsimov, "Asimov"));
            textStyleHashMap.put(TEXT_KUBRICK, new TextStyle(TEXT_KUBRICK, R.style.TextStyleKubrick, "Kubrick"));
        }
        return textStyleHashMap;
    }

    public static TextStyle[] getAllTextStyles() {
        if (textStyles == null)
            textStyles = getAllTextStylesHashMap().values().toArray(new TextStyle[0]);
        return textStyles;
    }

//    public static int getThemeGradientNumber(Theme theme) {
//        switch (theme.getPrefName()) {
//            case THEME_FAIRY_FROG:
//                return 3;
//            default:
//                return 2;
//        }
//    }

    /**
     * Returns a {@link GradientDrawable} containing a linear gradient with the given style's colors
     *
     * @param context Context
     * @param theme The style resource contaning the background color definition (colorMainGradient[Start|End])
     * @return {@link GradientDrawable} containing a linear gradient with the given style's colors
     */
    public static GradientDrawable fetchBackgroundGradient(Context context, Theme theme) {
        TypedArray gradientColors = context.obtainStyledAttributes(theme.getResId(), R.styleable.BaseTwistyTheme);
        GradientDrawable gradientDrawable;

//        switch (getThemeGradientNumber(theme)) {
//            default:
//            case 2:
                gradientDrawable = new GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{gradientColors.getColor(R.styleable.BaseTwistyTheme_colorMainGradientStart, Color.BLUE),
                                  gradientColors.getColor(R.styleable.BaseTwistyTheme_colorMainGradientEnd, Color.BLUE)});
//                break;
//            case 3:
//                gradientDrawable = new GradientDrawable(
//                    GradientDrawable.Orientation.TOP_BOTTOM,
//                    new int[]{gradientColors.getColor(R.styleable.BaseTwistyTheme_colorMainGradientStart, Color.BLUE),
//                              gradientColors.getColor(R.styleable.BaseTwistyTheme_colorMainGradientMiddle, Color.BLUE),
//                              gradientColors.getColor(R.styleable.BaseTwistyTheme_colorMainGradientEnd, Color.BLUE)});
//                break;
//        }

        gradientColors.recycle();

        return gradientDrawable;
    }

    /**
     * Fetches and returns a Styleable value
     * @param context
     * @param styleable The styleable to fetch
     * @param style Property of the styleable to fetch
     * @param defaultAttr default attr to return if styleable can't be fetched
     * @return
     */
    public static int fetchStyleableAttr(Context context, @StyleRes int style, @StyleableRes int[] styleable, @StyleableRes int styleableStyle, @AttrRes int defaultAttr) {
        TypedArray textColors = context.obtainStyledAttributes(style, styleable);

        int color = textColors.getColor(styleableStyle, fetchAttrColor(context, defaultAttr));
        textColors.recycle();

        return color;
    }

    /**
     * Gets a color from an attr resource value
     *
     * @param context Context
     * @param attrRes The attribute resource (ex. R.attr.colorPrimary)
     * @return @ColorRes
     */
    public static int fetchAttrColor(Context context, @AttrRes int attrRes) {
        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attrRes, value, true);
        return value.data;
    }

    /**
     * Gets a boolean from an attr resource value
     *
     * @param context Context
     * @return @ColorRes
     */
    public static boolean fetchAttrBool(Context context, @StyleRes int style, @StyleableRes int boolRes) {
        TypedArray bool = context.obtainStyledAttributes(style, R.styleable.BaseTwistyTheme);

        boolean finalBool = bool.getBoolean(boolRes, false);
        bool.recycle();

        return finalBool;
    }

    public static Drawable fetchAttrDrawable (Context context, @AttrRes int attrRes) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(attrRes, outValue, true);
        return ContextCompat.getDrawable(context, outValue.resourceId);
    }

    public static int dpToPix(Context context, float dp) {
        return (int) (TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics()));
    }

    /**
     * Fetches a drawable from a resource id (can be a vector drawable),
     * tints with {@param colorAttrRes} and returns it.
     * @param context {@link Context}
     * @param drawableRes resource id for the drawable
     * @param colorAttrRes attr res id for the tint
     * @return a tinted drawable
     */
    public static Drawable fetchTintedDrawable(Context context, @DrawableRes int drawableRes, @AttrRes int colorAttrRes) {
        Drawable drawable = DrawableCompat.wrap(ContextCompat.getDrawable(context, drawableRes)).mutate();
        DrawableCompat.setTint(drawable, ThemeUtils.fetchAttrColor(context, colorAttrRes));
        drawable.invalidateSelf();
        return drawable;
    }

    /**
     * Creates and returns a {@link GradientDrawable} shape with custom radius and colors.
     * @param context Current context
     * @param backgroundColors 2 or 3 int array with the shape background colors
     * @param strokeColor Color of the shape stroke
     * @param cornerRadius Radius of the shape in dp
     * @param strokeWidth Stroke width in dp
     * @return A {@link GradientDrawable} with the set arguments
     */
    public static GradientDrawable createSquareDrawable(Context context, int[] backgroundColors, int strokeColor, int cornerRadius, float strokeWidth) {
        GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, backgroundColors);
        gradientDrawable.setStroke(ThemeUtils.dpToPix(context, strokeWidth), strokeColor);
        gradientDrawable.setCornerRadius(ThemeUtils.dpToPix(context, cornerRadius));

        return gradientDrawable;
    }

    /**
     * Creates and returns a {@link GradientDrawable} shape with custom radius and colors.
     * @param context Current context
     * @param backgroundColor Shape background color
     * @param strokeColor Color of the shape stroke
     * @param cornerRadius Radius of the shape in dp
     * @param strokeWidth Stroke width in dp
     * @return A {@link GradientDrawable} with the set arguments
     */
    public static GradientDrawable createSquareDrawable(Context context, int backgroundColor, int strokeColor, int cornerRadius, float strokeWidth) {
        int[] colors = {backgroundColor, backgroundColor};
        return createSquareDrawable(context, colors, strokeColor, cornerRadius, strokeWidth);
    }

    /**
     * Creates and returns a {@link GradientDrawable} shape with custom radius and colors.
     * Pass 0 to either of the color arguments to make it transparent
     * @param context Current context
     * @param backgroundColor Shape background color
     * @param strokeColor Color of the shape stroke
     * @param cornerRadius Radius of the shape in dp
     * @param strokeWidth Stroke width in dp
     * @return A {@link GradientDrawable} with the set arguments
     */
    public static GradientDrawable createSquareDrawableAttr(Context context, int backgroundColor, int strokeColor, int cornerRadius, float strokeWidth) {
        int color = backgroundColor == 0 ? Color.TRANSPARENT : ThemeUtils.fetchAttrColor(context, backgroundColor);
        int[] colors = {color, color};
        return createSquareDrawable(context, colors,
                                    strokeColor == 0 ? Color.TRANSPARENT : ThemeUtils.fetchAttrColor(context, strokeColor),
                                    cornerRadius, strokeWidth);
    }

    /**
     * Tints a drawable with {@param colorAttrRes} and returns it.
     * @param context {@link Context}
     * @param drawableRes drawableRes to be tinted
     * @param colorAttrRes attr res id for the tint
     * @return a tinted drawable
     */
    public static Drawable tintDrawable(Context context, @DrawableRes int drawableRes, @AttrRes int colorAttrRes) {
        Drawable drawable = AppCompatResources.getDrawable(context, drawableRes).mutate();
        Drawable wrap = DrawableCompat.wrap(drawable).mutate();
        DrawableCompat.setTint(wrap, ThemeUtils.fetchAttrColor(context, colorAttrRes));
        DrawableCompat.setTintMode(wrap, PorterDuff.Mode.SRC_IN);

        return wrap;
    }

    /**
     * @return A ImageSpan with the given size multiplier. Supports vector drawables
     */
    public static ImageSpan getIconSpan(Context context, float size) {
        Drawable drawable = AppCompatResources.getDrawable(context, R.drawable.ic_history_off);
        drawable.setBounds(0, 0, (int) (drawable.getIntrinsicWidth() * size), (int) (drawable.getIntrinsicHeight() * size));

        return new ImageSpan(drawable);
    }

    public static int spToPx(float sp, Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics());
    }

    /**
     * afollestad's MaterialDialog library only allows corner round on versions >2.0.0
     * However, it's only available in Kotlin, and rewriting the app to use Kotlin would be a major
     * task, so this function does a workaround to get dialog corner rounding working.
     *
     * Rounds and returns the dialog.
     *
     * @param context The dialog context
     * @param dialog The dialog to be rounded
     */
    public static MaterialDialog roundDialog(Context context, MaterialDialog dialog) {
        dialog.getWindow().setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.outline_background_card_smoother));
        return dialog;
    }

    /**
     * afollestad's MaterialDialog library only allows corner round on versions >2.0.0
     * However, it's only available in Kotlin, and rewriting the app to use Kotlin would be a major
     * task, so this function does a workaround to get dialog corner rounding working.
     *
     * Rounds and shows the dialog.
     *
     * @param context The dialog context
     * @param dialog The dialog to be rounded
     */
    public static void roundAndShowDialog(Context context, MaterialDialog dialog) {
        roundDialog(context, dialog).show();
    }
}
