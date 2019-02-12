package com.aricneto.twistytimer.layout;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.utils.AlgUtils;
import com.aricneto.twistytimer.utils.ThemeUtils;

import java.util.HashMap;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class Cube extends View {

    private HashMap<Character, Integer> mStickerColors;
    private String                      mCubeState;

    private Paint mCubePaint;
    private Paint mStickerPaint;

    private RectF mStickerRect;
    private RectF mCubeRect;

    private int   mPadding;
    private float mStickerSize;
    private float mCubeCornerRadius;
    private float mStickerCornerRadius;

    public Cube(Context context) {
        super(context, null);
        init(null);
    }

    public Cube(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, 0);
        init(attrs);
    }

    public Cube(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public Cube(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(@Nullable AttributeSet attrs) {
        mPadding = 0;
        mCubeCornerRadius = 0f;
        mStickerCornerRadius = 0f;
        initPaints();
        initRects();

        if (attrs == null)
            return;

        TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.Cube);
        mCubeCornerRadius = ta.getDimensionPixelSize(R.styleable.Cube_cube_corner_radius, 8);
        mStickerCornerRadius = ta.getDimensionPixelSize(R.styleable.Cube_cube_sticker_corner_radius, 8);
        ta.recycle();
    }

    private void initPaints() {
        mStickerColors = AlgUtils.getColorLetterHashMap();

        mCubePaint = new Paint();
        mCubePaint.setStyle(Paint.Style.FILL);
        mCubePaint.setColor(Color.parseColor("#2E2E2E"));
        mCubePaint.setAntiAlias(true);

        mStickerPaint = new Paint();
        mStickerPaint.setStyle(Paint.Style.FILL);
        mStickerPaint.setAntiAlias(true);
    }

    private void initRects() {
        mCubeRect = new RectF();
        mStickerRect = new RectF();
    }

    @SuppressWarnings("ConstantConditions")
    private @ColorInt
    int getStickerColor(Character colorLetter) {
        return mStickerColors.get(colorLetter);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final int width = getWidth();

        // padding is 3% of the cube's size
        mPadding = (int) (width * 0.04f);

        // sticker size. in a 3x3, there'll be 5 stickers (3 for the face, 2 for the sides)
        // in each line.
        mStickerSize = (width - (mPadding * 6)) / (3 + 0.75f);
        // size to subtract from outermost stickers
        // you MUST change mStickerSize to divide by the correct amount
        // i.e. if sizeSubtract = mStickerSize / 2, there'll be 3 whole stickers and 2 half-stickers
        // which equals 4 stickers. Likewise, if you were to divide by 1.6, there would be 3.75 stickers
        float sizeSubtract = (mStickerSize / 1.6f);

        mCubeRect.set(0, 0,
                      (mPadding * 6) + (mStickerSize * 5) - (sizeSubtract * 2),
                      (mPadding * 6) + (mStickerSize * 5) - (sizeSubtract * 2));

        // draw cube background
        canvas.drawRoundRect(mCubeRect, mCubeCornerRadius, mCubeCornerRadius, mCubePaint);

        // Draw the cube
        // The edge conditions are used to draw half-stickers in the borders only
        // This code is rather complicated to explain
        // Basically, for every line, we create a rect at the beginning. We then draw that rect
        // and use its properties to calculate where the next sticker should be.
        for (int i = 0; i < 5; i++) {
            mStickerRect.set(
                    mPadding,
                    mPadding + ((mStickerSize + mPadding) * i) - sizeSubtract,
                    mPadding + mStickerSize,
                    mPadding + mStickerSize + ((mStickerSize + mPadding) * i) - sizeSubtract
            );

            // top outer border
            if (i == 0) {
                mStickerRect.set(
                        mStickerRect.left,
                        mPadding,
                        mStickerRect.right,
                        mStickerRect.bottom
                );
            }

            // bottom outer border
            if (i == 4) {
                mStickerRect.set(
                        mStickerRect.left,
                        mStickerRect.top,
                        mStickerRect.right,
                        mStickerRect.bottom - sizeSubtract
                );
            }

            for (int j = 0; j < 5; j++) {

                // left outer border
                if (j == 0) {
                    mStickerRect.set(
                            mPadding, //mStickerRect.left + sizeSubtract,
                            mStickerRect.top,
                            mStickerRect.right - sizeSubtract,
                            mStickerRect.bottom
                    );
                }

                // ignore the four corners
                // TODO: Error check if string is of correct size/format!
                if (!((i == 0 || i == 4) && (j == 0 || j == 4))) {
                    mStickerPaint.setColor(AlgUtils.getColorFromStateIndex(mCubeState, (5 * i) + j));
                    canvas.drawRoundRect(
                            mStickerRect,
                            mStickerCornerRadius,
                            mStickerCornerRadius,
                            mStickerPaint
                    );
                }
                mStickerRect.set(
                        mStickerRect.right + mPadding,
                        mStickerRect.top,
                        j != 3 ? mStickerRect.right + mPadding + mStickerSize : mStickerRect.right + mPadding + mStickerSize - sizeSubtract, // right outer border
                        mStickerRect.bottom
                );
            }
        }
    }

    public String getCubeState() {
        return mCubeState;
    }

    public void setCubeState(String cubeState) {
        this.mCubeState = cubeState;
        invalidate();
    }
}
