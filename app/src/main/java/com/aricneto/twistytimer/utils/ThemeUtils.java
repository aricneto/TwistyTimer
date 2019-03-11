package com.aricneto.twistytimer.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.annotation.StyleableRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import android.text.style.ImageSpan;
import android.util.TypedValue;

import com.afollestad.materialdialogs.MaterialDialog;
import com.aricneto.twistify.R;
import com.aricneto.twistytimer.items.Theme;

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


    public static final String TEXT_DEFAULT   = "default";
    public static final String TEXT_TARKOVSKY = "tarkovsky";
    public static final String TEXT_MATSSON   = "matsson";
    public static final String TEXT_TOLKIEN   = "tolkien";
    public static final String TEXT_PESSOA    = "pessoa";

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
    public static int getPreferredTheme() {
        return getThemeStyleRes(Prefs.getString(R.string.pk_theme, "indigo"));
    }

    public static int getPreferredTextStyle() {
        return getThemeStyleRes(Prefs.getString(R.string.pk_text_style, "default"));
    }

    public static int getThemeStyleRes(String theme) {
        switch (theme) {
            default:
            case THEME_INDIGO:
                return R.style.DefaultTheme;
            case THEME_PURPLE:
                return R.style.PurpleTheme;
            case THEME_TEAL:
                return R.style.TealTheme;
            case THEME_PINK:
                return R.style.PinkTheme;
            case THEME_RED:
                return R.style.RedTheme;
            case THEME_BROWN:
                return R.style.BrownTheme;
            case THEME_BLUE:
                return R.style.BlueTheme;
            case THEME_CYAN:
                return R.style.CyanTheme;
            case THEME_LIGHT_BLUE:
                return R.style.LightBlueTheme;
            case THEME_BLACK:
                return R.style.BlackTheme;
            case THEME_ORANGE:
                return R.style.OrangeTheme;
            case THEME_GREEN:
                return R.style.GreenTheme;
            case THEME_LIGHT_GREEN:
                return R.style.LightGreenTheme;
            case THEME_DEEPPURPLE:
                return R.style.DeepPurpleTheme;
            case THEME_BLUEGRAY:
                return R.style.BlueGrayTheme;
            case THEME_WHITE:
                return R.style.WhiteTheme;
            case THEME_YELLOW:
                return R.style.YellowTheme;
            case THEME_WHITE_GREEN:
                return R.style.WhiteGreenTheme;
            case THEME_DAWN:
                return R.style.DawnTheme;
            case THEME_BLUY_GRAY:
                return R.style.BluyGray;
            case THEME_TURTLY_SEA:
                return R.style.TurtlySea;
            case THEME_PIXIE_FALLS:
                return R.style.PixieFalls;
            case THEME_WANDERING_DUSK:
                return R.style.WanderingDusk;
            case THEME_SPOTTY_GUY:
                return R.style.SpottyGuy;
            case TEXT_DEFAULT:
                return getPreferredTheme();
            case TEXT_TARKOVSKY:
                return R.style.TextStyleTarkovsky;
            case TEXT_TOLKIEN:
                return R.style.TextStyleTolkien;
            case TEXT_MATSSON:
                return R.style.TextStyleMatsson;
            case TEXT_PESSOA:
                return R.style.TextStylePessoa;
        }
    }

    /**
     * Used to populate theme select dialogs.
     *
     * @return an array containing all available themes
     */
    public static Theme[] getAllThemes() {
        Theme[] themes = {
                new Theme(THEME_INDIGO, "Hazy Blues"),
                new Theme(THEME_GREEN, "What... Green?"),
                new Theme(THEME_SPOTTY_GUY, "Spotty Guy"),
                new Theme(THEME_BLACK, "Simply Black"),
                new Theme(THEME_WHITE, "Simply White"),
                new Theme(THEME_YELLOW, "Notably Yellow"),
                new Theme(THEME_BLUY_GRAY, "Bluy Gray"),
                new Theme(THEME_TURTLY_SEA, "Turtly Sea"),
                new Theme(THEME_PIXIE_FALLS, "Pixie Falls"),
                new Theme(THEME_RED, "Oof Hot"),
                new Theme(THEME_WANDERING_DUSK, "Wandy Dusk"),
                new Theme(THEME_DAWN, "Relaxing Dawn"),
                new Theme(THEME_DEEPPURPLE, "Quite Purply"),
                new Theme(THEME_BLUE, "Even Purplier"),
                new Theme(THEME_PURPLE, "Definitely Purple"),
                new Theme(THEME_ORANGE, "Tantalizing Torange"),
                new Theme(THEME_PINK, "Pinky Promises"),
                new Theme(THEME_BROWN, "Delicious Brownie"),
                new Theme(THEME_TEAL, "Earthy Teal"),
                new Theme(THEME_LIGHT_GREEN, "Greeny Gorilla"),
                new Theme(THEME_LIGHT_BLUE, "Lightly Skyish"),
                new Theme(THEME_CYAN, "Cyanic Teal"),
                new Theme(THEME_BLUEGRAY, "Icy Hills"),
                new Theme(THEME_WHITE_GREEN, "Greeny Everest")
        };
        return themes;
    }

    public static Theme[] getAllTextStyles(Context context) {
        Theme[] styles = {
                new Theme(TEXT_DEFAULT, context.getString(R.string.action_default)),
                new Theme(TEXT_TARKOVSKY, "Tarkovsky"),
                new Theme(TEXT_TOLKIEN, "Tolkien"),
                new Theme(TEXT_MATSSON, "Matsson"),
                new Theme(TEXT_PESSOA, "Pessoa")
        };
        return styles;
    }

    /**
     * Returns a {@link GradientDrawable} containing a linear gradient with the given style's colors
     *
     * @param context Context
     * @param style The style resource contaning the background color definition (colorMainGradient[Start|End])
     * @return {@link GradientDrawable} containing a linear gradient with the given style's colors
     */
    public static GradientDrawable fetchBackgroundGradient(Context context, @StyleRes int style) {
        TypedArray gradientColors = context.obtainStyledAttributes(style, R.styleable.BackgroundGradientStyle);

        GradientDrawable gradientDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{gradientColors.getColor(R.styleable.BackgroundGradientStyle_colorMainGradientStart, Color.BLUE),
                          gradientColors.getColor(R.styleable.BackgroundGradientStyle_colorMainGradientEnd, Color.BLUE)});

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
        TypedArray bool = context.obtainStyledAttributes(style, R.styleable.BooleanStyles);

        boolean finalBool = bool.getBoolean(boolRes, false);
        bool.recycle();

        return finalBool;
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
     * @param drawable drawable to be tinted
     * @param colorAttrRes attr res id for the tint
     * @return a tinted drawable
     */
    public static Drawable tintDrawable(Context context, Drawable drawable, @AttrRes int colorAttrRes) {
        Drawable wrap = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(wrap, ThemeUtils.fetchAttrColor(context, colorAttrRes));
        DrawableCompat.setTintMode(wrap, PorterDuff.Mode.SRC_IN);
        wrap = wrap.mutate();

        return wrap;
    }

    /**
     * Tints a drawable with {@param colorRes} and returns it.
     * @param context {@link Context}
     * @param drawableRes drawableres to be tinted
     * @param colorRes color res id for the tint
     * @return a tinted drawable
     */
    public static Drawable tintDrawable(Context context, @DrawableRes int drawableRes, @ColorInt int colorRes) {
        Drawable drawable = AppCompatResources.getDrawable(context, drawableRes).mutate();
        Drawable wrap = DrawableCompat.wrap(drawable).mutate();
        DrawableCompat.setTint(wrap, colorRes);
        DrawableCompat.setTintMode(wrap, PorterDuff.Mode.MULTIPLY);

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
