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
    private int   mStickerSize;
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
        mPadding = (int) (width * 0.03f);
        mStickerSize = (width - (mPadding * 6)) / 5;

        mCubeRect.set(0, 0,
                      (mPadding * 6) + (mStickerSize * 5),
                      (mPadding * 6) + (mStickerSize * 5));

        // draw cube background
        canvas.drawRoundRect(mCubeRect, mCubeCornerRadius, mCubeCornerRadius, mCubePaint);

        for (int i = 0; i < 5; i++) {
            mStickerRect.set(
                    mPadding,
                    mPadding + ((mStickerSize + mPadding) * i),
                    mPadding + mStickerSize,
                    mPadding + mStickerSize + ((mStickerSize + mPadding) * i)
            );
            for (int j = 0; j < 5; j++) {
                // ignore the four corners
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
                        mStickerRect.right + mPadding + mStickerSize,
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
