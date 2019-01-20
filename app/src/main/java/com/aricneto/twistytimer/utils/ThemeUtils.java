package com.aricneto.twistytimer.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import androidx.annotation.AttrRes;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import android.util.TypedValue;

import com.aricneto.twistify.R;

/**
 * Utility class to make themeing easier.
 */
public final class ThemeUtils {
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

        switch (Prefs.getString(R.string.pk_theme, "indigo")) {
            default:
            case "indigo":
                return R.style.DefaultTheme;
            case "purple":
                return R.style.PurpleTheme;
            case "teal":
                return R.style.TealTheme;
            case "pink":
                return R.style.PinkTheme;
            case "red":
                return R.style.RedTheme;
            case "brown":
                return R.style.BrownTheme;
            case "cyan":
                return R.style.CyanTheme;
            case "blue":
                return R.style.BlueTheme;
            case "light_blue":
                return R.style.LightBlueTheme;
            case "black":
                return R.style.BlackTheme;
            case "orange":
                return R.style.OrangeTheme;
            case "green":
                return R.style.GreenTheme;
            case "light_green":
                return R.style.LightGreenTheme;
            case "deepPurple":
                return R.style.DeepPurpleTheme;
            case "blueGray":
                return R.style.BlueGrayTheme;
        }
    }

    /**
     * Gets a color from an attr resource value
     * @param context Context
     * @param attrRes The attribute resource (ex. R.attr.colorPrimary)
     * @return @ColorRes
     */
    public static int fetchAttrColor(Context context, @AttrRes int attrRes) {
        final TypedValue value = new TypedValue ();
        context.getTheme().resolveAttribute(attrRes, value, true);
        return value.data;
    }

    public static Drawable tintDrawable(Context context, @DrawableRes int drawableRes, @AttrRes int colorAttrRes) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableRes);
        Drawable wrap = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(wrap, ThemeUtils.fetchAttrColor(context, colorAttrRes));
        DrawableCompat.setTintMode(wrap, PorterDuff.Mode.MULTIPLY);
        wrap = wrap.mutate();

        return wrap;
    }

    // The following two functions are used to tint the history switch
    // TODO: simplify these functions

    public static Drawable tintPositiveThumb(Context context, @DrawableRes int drawableRes, @AttrRes int colorAttrRes) {
        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.thumb_circle);
        Drawable wrap = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(wrap, ThemeUtils.fetchAttrColor(context, colorAttrRes));
        DrawableCompat.setTintMode(wrap, PorterDuff.Mode.MULTIPLY);
        wrap = wrap.mutate();

        Drawable[] layers = new Drawable[2];
        layers[0] = wrap;
        layers[1] = ContextCompat.getDrawable(context, drawableRes);

        return new LayerDrawable(layers);
    }

    public static Drawable tintNegativeThumb(Context context, @DrawableRes int drawableRes, @AttrRes int colorAttrRes) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableRes);
        Drawable wrap = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(wrap, ThemeUtils.fetchAttrColor(context, colorAttrRes));
        DrawableCompat.setTintMode(wrap, PorterDuff.Mode.MULTIPLY);
        wrap = wrap.mutate();

        Drawable circle = ContextCompat.getDrawable(context, R.drawable.thumb_circle);
        Drawable circleWrap = DrawableCompat.wrap(circle);
        DrawableCompat.setTint(circleWrap, Color.WHITE);
        DrawableCompat.setTintMode(circleWrap, PorterDuff.Mode.MULTIPLY);
        circleWrap = circleWrap.mutate();

        Drawable[] layers = new Drawable[2];
        layers[0] = circleWrap;
        layers[1] = wrap;

        return new LayerDrawable(layers);
    }
}
