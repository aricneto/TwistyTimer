package com.aricneto.twistytimer.layout;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.util.AttributeSet;

import com.pavelsikun.seekbarpreference.SeekBarPreference;

/**
 * Created by Ari on 03/03/2016.
 */
public class CustomSeekBarPreference extends SeekBarPreference {

    public CustomSeekBarPreference(Context context) {
        super(context);
    }

    public CustomSeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomSeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public CustomSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected Object onGetDefaultValue(@NonNull TypedArray ta, int index) {
        return ta.getInt(index, 50);
    }
}
