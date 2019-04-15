package com.aricneto.twistytimer.layout;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import com.aricneto.twistytimer.utils.AlgUtils;

import java.util.HashMap;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class Cube2D extends View {

    private HashMap<Character, Integer> mStickerColors;
    private String[]                      mCubeState;

    private Paint mCubePaint;
    private Paint mStickerPaint;

    private RectF mStickerRect;
    private RectF mStickerStartRect;
    private RectF mStickerHalfHorizontalStartRect;
    private RectF mStickerHalfVerticalStartRect;
    private RectF mCubeRect;

    private int   mPadding;
    private float mStickerSize;
    private int mPuzzleSize;
    private float mCubeCornerRadius;
    private float mStickerCornerRadius;

    public Cube2D(Context context) {
        super(context, null);
        init(null);
    }

    public Cube2D(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, 0);
        init(attrs);
    }

    public Cube2D(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public Cube2D(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(@Nullable AttributeSet attrs) {
        mPadding = 0;
        mCubeCornerRadius = 0f;
        mStickerCornerRadius = 0f;
        initPaints();
        initRects();
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
        mStickerPaint.setColor(Color.parseColor("#FF0000"));
    }

    private void initRects() {
        mCubeRect = new RectF();
        mStickerRect = new RectF();
        mStickerStartRect = new RectF();
        mStickerHalfHorizontalStartRect = new RectF();
        mStickerHalfVerticalStartRect = new RectF();
    }

    @SuppressWarnings("ConstantConditions")
    private @ColorInt
    int getStickerColor(Character colorLetter) {
        return mStickerColors.get(colorLetter);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);


    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final int width = getWidth();

        // padding is 3% of the cube's size
        mPadding = (int) (width * 0.04f);

        // sticker size.
        // in a 3x3, there'll be 5 stickers in each line (3 for the face, 2 for the sides)
        mStickerSize = (width - (mPadding * (mPuzzleSize + 3))) / (float) (mPuzzleSize + 2);

        mCubeRect.set((mStickerSize / 2f),
                      (mStickerSize / 2f),
                      (mStickerSize / 2f) + (mPadding * (mPuzzleSize + 3)) + (mStickerSize * (mPuzzleSize + 1)),
                      (mStickerSize / 2f) + (mPadding * (mPuzzleSize + 3)) + (mStickerSize * (mPuzzleSize + 1)));

        mStickerHalfHorizontalStartRect.set(
                mCubeRect.left + (mPadding * 2) + (mStickerSize / 2f),
                mCubeRect.top + mPadding,
                mCubeRect.left + (mPadding * 2) + (mStickerSize * 1.5f),
                mCubeRect.top + mPadding + (mStickerSize / 2f)
        );

        mStickerHalfVerticalStartRect.set(
                mCubeRect.left + mPadding,
                mCubeRect.top + (mPadding * 2) + (mStickerSize / 2f),
                mCubeRect.left + mPadding + (mStickerSize / 2f),
                mCubeRect.top + (mPadding * 2) + (mStickerSize / 2f) + mStickerSize
        );

        // draw cube background
        canvas.drawRoundRect(mCubeRect, mCubeCornerRadius, mCubeCornerRadius, mCubePaint);

        char sFace;

        for (int i = 0; i < mPuzzleSize; i++) {
            mStickerRect = translateRect(mStickerHalfHorizontalStartRect, i * (mPadding + mStickerSize), 0);
            canvas.drawRoundRect(mStickerRect, mStickerCornerRadius, mStickerCornerRadius, mStickerPaint);
            mStickerRect = translateRect(mStickerHalfHorizontalStartRect, i * (mPadding + mStickerSize), mCubeRect.bottom - (2 * mPadding) - (mStickerSize));
            canvas.drawRoundRect(mStickerRect, mStickerCornerRadius, mStickerCornerRadius, mStickerPaint);

            mStickerRect = translateRect(mStickerHalfVerticalStartRect, 0, i * (mPadding + mStickerSize));
            canvas.drawRoundRect(mStickerRect, mStickerCornerRadius, mStickerCornerRadius, mStickerPaint);
            mStickerRect = translateRect(mStickerHalfVerticalStartRect, mCubeRect.right - (2 * mPadding) - (mStickerSize), i * (mPadding + mStickerSize));
            canvas.drawRoundRect(mStickerRect, mStickerCornerRadius, mStickerCornerRadius, mStickerPaint);
        }

        mStickerStartRect.set(
                mStickerHalfHorizontalStartRect.left,
                mStickerHalfHorizontalStartRect.bottom + mPadding,
                mStickerHalfHorizontalStartRect.left + mStickerSize,
                mStickerHalfHorizontalStartRect.bottom + mPadding + mStickerSize
        );

        for (int i = 0; i < mPuzzleSize; i++) {
            for (int j = 0; j < mPuzzleSize; j++) {
                mStickerRect = translateRect(mStickerStartRect, j * (mPadding + mStickerSize), i * (mPadding + mStickerSize));
                canvas.drawRoundRect(mStickerRect, mStickerCornerRadius, mStickerCornerRadius, mStickerPaint);
            }
        }
    }

    private static RectF translateRect(RectF startRect, float dx, float dy) {
        RectF newRect = new RectF();
        newRect.set(
                startRect.left + dx,
                startRect.top + dy,
                startRect.right + dx,
                startRect.bottom + dy
        );
        return newRect;
    }

    public String[] getCubeState() {
        return mCubeState;
    }

    public Cube2D setCubeState(String[] cubeState, int puzzleSize) {
        this.mCubeState = cubeState;
        this.mPuzzleSize = 3;
        invalidate();
        return this;
    }
}
