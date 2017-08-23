package com.aricneto.twistytimer.spans;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.style.LineBackgroundSpan;

/*
*  Span for drawing a rectangle with rounded corners
*
* */

public class RoundRectSpan implements LineBackgroundSpan {
    private final int color;

    public RoundRectSpan(int color) {
        this.color = color;
    }

    public void drawBackground(Canvas canvas, Paint paint,
                               int left, int right, int top, int baseline, int bottom,
                               CharSequence charSequence,
                               int start, int end, int lineNum) {

        int oldColor = paint.getColor();
        paint.setColor(color);
        RectF rect0 = new RectF(left, top, right, bottom);

        canvas.drawRoundRect(rect0, 6, 6, paint);

        paint.setColor(oldColor);
    }
}
