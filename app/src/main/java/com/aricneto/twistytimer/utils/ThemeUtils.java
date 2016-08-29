package com.aricneto.twistytimer.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.preference.PreferenceManager;
import android.support.annotation.AttrRes;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.TypedValue;

import com.aricneto.twistify.R;

/**
 * Util to make themeing easier
 */
public class ThemeUtils {

    public static int getCurrentTheme(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean backgroundEnabled = sharedPreferences.getBoolean("backgroundEnabled", true);
        switch (sharedPreferences.getString("theme", "indigo")) {
            case "indigo":
                if (backgroundEnabled)
                    return R.style.DefaultTheme;
                else
                    return R.style.DefaultTheme_NoBackground;
            case "purple":
                if (backgroundEnabled)
                    return R.style.PurpleTheme;
                else
                    return R.style.PurpleTheme_NoBackground;
            case "teal":
                if (backgroundEnabled)
                    return R.style.TealTheme;
                else
                    return R.style.TealTheme_NoBackground;
            case "pink":
                if (backgroundEnabled)
                    return R.style.PinkTheme;
                else
                    return R.style.PinkTheme_NoBackground;
            case "red":
                if (backgroundEnabled)
                    return R.style.RedTheme;
                else
                    return R.style.RedTheme_NoBackground;
            case "brown":
                if (backgroundEnabled)
                    return R.style.BrownTheme;
                else
                    return R.style.BrownTheme_NoBackground;
            case "blue":
                if (backgroundEnabled)
                    return R.style.BlueTheme;
                else
                    return R.style.BlueTheme_NoBackground;
            case "black":
                if (backgroundEnabled)
                    return R.style.BlackTheme;
                else
                    return R.style.BlackTheme_NoBackground;
            case "orange":
                if (backgroundEnabled)
                    return R.style.OrangeTheme;
                else
                    return R.style.OrangeTheme_NoBackground;
            case "green":
                if (backgroundEnabled)
                    return R.style.GreenTheme;
                else
                    return R.style.GreenTheme_NoBackground;
            case "deepPurple":
                if (backgroundEnabled)
                    return R.style.DeepPurpleTheme;
                else
                    return R.style.DeepPurpleTheme_NoBackground;
            case "blueGray":
                if (backgroundEnabled)
                    return R.style.BlueGrayTheme;
                else
                    return R.style.BlueGrayTheme_NoBackground;
        }
        return R.style.DefaultTheme;
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
