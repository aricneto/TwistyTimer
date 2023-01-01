package com.hatopigeon.cubictimer.layout;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;

/**
 * A gridview that has a fixed height, when it first runs, it adjusts its height to the number of
 * items within it and does not change. Scroll is not enabled either.
 * This allows it to be placed inside a ScrollView without the problems that usually comes from
 * nesting ScrollViews.
 *
 * Such an approach can be considered an "unoptimized hack", since this disables what makes
 * GridView viable in the first place: View Recycling. If used incorrectly, this can lead to memory
 * leaks and crashes.
 *
 * However, in the particular case which prompted the use of this hack (the stats gridview in
 * {@link com.hatopigeon.cubictimer.fragment.TimerGraphFragment}, we do not intend to use
 * GridView to display a scrollable list of components; all items are supposed to be seen on the
 * screen at the same time, so View Recycling is not going to be an issue.
 */
public class StaticGridView extends GridView {

    public StaticGridView(Context context) {
        super(context);
    }

    public StaticGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StaticGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(MEASURED_SIZE_MASK, MeasureSpec.AT_MOST));
        getLayoutParams().height = getMeasuredHeight();
    }
}