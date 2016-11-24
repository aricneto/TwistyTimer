package com.aricneto.twistytimer.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.AttrRes;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
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
        final boolean bgEnabled = Prefs.getBoolean(R.string.pk_timer_bg_enabled, true);

        switch (Prefs.getString(R.string.pk_theme, "indigo")) {
            default:
            case "indigo":
                return bgEnabled ? R.style.DefaultTheme    : R.style.DefaultTheme_NoBackground;
            case "purple":
                return bgEnabled ? R.style.PurpleTheme     : R.style.PurpleTheme_NoBackground;
            case "teal":
                return bgEnabled ? R.style.TealTheme       : R.style.TealTheme_NoBackground;
            case "pink":
                return bgEnabled ? R.style.PinkTheme       : R.style.PinkTheme_NoBackground;
            case "red":
                return bgEnabled ? R.style.RedTheme        : R.style.RedTheme_NoBackground;
            case "brown":
                return bgEnabled ? R.style.BrownTheme      : R.style.BrownTheme_NoBackground;
            case "cyan":
                return bgEnabled ? R.style.CyanTheme       : R.style.CyanTheme_NoBackground;
            case "blue":
                return bgEnabled ? R.style.BlueTheme       : R.style.BlueTheme_NoBackground;
            case "light_blue":
                return bgEnabled ? R.style.LightBlueTheme  : R.style.LightBlueTheme_NoBackground;
            case "black":
                return bgEnabled ? R.style.BlackTheme      : R.style.BlackTheme_NoBackground;
            case "orange":
                return bgEnabled ? R.style.OrangeTheme     : R.style.OrangeTheme_NoBackground;
            case "green":
                return bgEnabled ? R.style.GreenTheme      : R.style.GreenTheme_NoBackground;
            case "light_green":
                return bgEnabled ? R.style.LightGreenTheme : R.style.LightGreenTheme_NoBackground;
            case "deepPurple":
                return bgEnabled ? R.style.DeepPurpleTheme : R.style.DeepPurpleTheme_NoBackground;
            case "blueGray":
                return bgEnabled ? R.style.BlueGrayTheme   : R.style.BlueGrayTheme_NoBackground;
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
