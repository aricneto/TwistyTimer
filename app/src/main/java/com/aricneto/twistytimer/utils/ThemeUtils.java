package com.aricneto.twistytimer.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;

import androidx.annotation.AttrRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StyleRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import android.text.style.ImageSpan;
import android.util.TypedValue;

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
        }
    }

    /**
     * Used to populate theme select dialogs.
     *
     * @return an array containing all available themes
     */
    public static Theme[] getAllThemes() {
        Theme[] themes = {
                new Theme(THEME_INDIGO, "Hazy\nBlues"),
                new Theme(THEME_GREEN, "Surprisingly\nGreen"),
                new Theme(THEME_YELLOW, "Notably\nYellow"),
                new Theme(THEME_WHITE, "Simply\nWhite"),
                new Theme(THEME_BLACK, "Simply\nBlack"),
                new Theme(THEME_BLUY_GRAY, "Bluy\nGray"),
                new Theme(THEME_DEEPPURPLE, "Quite\nPurply"),
                new Theme(THEME_BLUE, "Even\nPurplier"),
                new Theme(THEME_PURPLE, "Definitely\nPurple"),
                new Theme(THEME_ORANGE, "Tantalizing\nTorange"),
                new Theme(THEME_DAWN, "Relaxing\nDawn"),
                new Theme(THEME_RED, "Oof\nHot"),
                new Theme(THEME_PINK, "Pinky\nPromises"),
                new Theme(THEME_BROWN, "Delicious\nBrownie"),
                new Theme(THEME_TEAL, "Earthy\nTeal"),
                new Theme(THEME_LIGHT_GREEN, "Greeny\nGorilla"),
                new Theme(THEME_LIGHT_BLUE, "Lightly\nSkyish"),
                new Theme(THEME_CYAN, "Cyanic\nTeal"),
                new Theme(THEME_BLUEGRAY, "Icy\nHills"),
                new Theme(THEME_WHITE_GREEN, "Greeny\nEverest")
        };
        return themes;
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

    public static int convertDoToPixels(Context context, int dp) {
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
        Drawable drawable = AppCompatResources.getDrawable(context, drawableRes);
        Drawable wrap = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(wrap, ThemeUtils.fetchAttrColor(context, colorAttrRes));
        DrawableCompat.setTintMode(wrap, PorterDuff.Mode.MULTIPLY);
        wrap = wrap.mutate();

        return wrap;
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
     * @return A ImageSpan with the given size multiplier. Supports vector drawables
     */
    public static ImageSpan getIconSpan(Context context, float size) {
        Drawable drawable = AppCompatResources.getDrawable(context, R.drawable.ic_history_off);
        drawable.setBounds(0, 0, (int) (drawable.getIntrinsicWidth() * size), (int) (drawable.getIntrinsicHeight() * size));

        return new ImageSpan(drawable);
    }
}
