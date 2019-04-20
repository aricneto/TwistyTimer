package com.aricneto.twistytimer.layout;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;

import com.aricneto.twistytimer.items.AlgorithmModel;
import com.aricneto.twistytimer.layout.isometric.Color;
import com.aricneto.twistytimer.layout.isometric.IsometricView;
import com.aricneto.twistytimer.layout.isometric.Path;
import com.aricneto.twistytimer.layout.isometric.Point;
import com.aricneto.twistytimer.layout.isometric.shapes.Prism;
import com.aricneto.twistytimer.utils.AlgUtils;

public class CubeIsometric extends IsometricView {

    public CubeIsometric(Context context) {
        super(context);
    }

    public CubeIsometric(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CubeIsometric(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CubeIsometric(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private static Path[][][] stickerLocations = null;
    private static int mPuzzleSize;

    private static Point[] translatePoints(Point[] startPoints, double dx, double dy, double dz) {
        Point[] newPoint = new Point[4];
        for (int i = 0; i < 4; i++)
            newPoint[i] = startPoints[i].translate(dx, dy, dz);
        return newPoint;
    }

    private static void cacheStickerLocations(int puzzleSize, double mCubeSize, double mPadding, double mStickerSize) {
        if (stickerLocations == null || mPuzzleSize != puzzleSize) {
            stickerLocations = new Path[3][puzzleSize][puzzleSize];
            mPuzzleSize = puzzleSize;

            // Every start sticker starts at the top-left of the face
            Point[] rightStartPoint = new Point[]{
                    new Point(mPadding, 0, mCubeSize - mPadding),
                    new Point(mPadding + mStickerSize, 0, mCubeSize - mPadding),
                    new Point(mPadding + mStickerSize, 0, mCubeSize - mPadding - mStickerSize),
                    new Point(mPadding, 0, mCubeSize - mPadding - mStickerSize),
                    };

            Point[] leftStartPoint = new Point[]{
                    new Point(0, mCubeSize - mPadding, mCubeSize - mPadding),
                    new Point(0, mCubeSize - mPadding - mStickerSize, mCubeSize - mPadding),
                    new Point(0, mCubeSize - mPadding - mStickerSize, mCubeSize - mPadding - mStickerSize),
                    new Point(0, mCubeSize - mPadding, mCubeSize - mPadding - mStickerSize),
                    };

            Point[] topStartPoint = new Point[]{
                    new Point(mPadding, mCubeSize - mPadding, mCubeSize),
                    new Point(mPadding + mStickerSize, mCubeSize - mPadding, mCubeSize),
                    new Point(mPadding + mStickerSize, mCubeSize - mPadding - mStickerSize, mCubeSize),
                    new Point(mPadding, mCubeSize - mPadding - mStickerSize, mCubeSize),
                    };

            for (int i = 0; i < puzzleSize; i++) {
                for (int j = 0; j < puzzleSize; j++) {
                    stickerLocations[0][i][j] = new Path(translatePoints(rightStartPoint,
                                                                         (mPadding + mStickerSize) * j,
                                                                         0,
                                                                         -(mPadding + mStickerSize) * i));
                    stickerLocations[1][i][j] = new Path(translatePoints(leftStartPoint,
                                                                         0,
                                                                         -(mPadding + mStickerSize) * j,
                                                                         -(mPadding + mStickerSize) * i));
                    stickerLocations[2][i][j] = new Path(translatePoints(topStartPoint,
                                                                         (mPadding + mStickerSize) * j,
                                                                         -(mPadding + mStickerSize) * i,
                                                                         0));
                }
            }
        }
    }

    public IsometricView init(double scale, int puzzleSize, String[] state) {
        this.setIsometricScale(scale);
        final double mCubeSize = 2;
        final double mPadding = mCubeSize * 0.06;
        final double mStickerSize = (mCubeSize - (mPadding * (puzzleSize + 1))) / puzzleSize;

        cacheStickerLocations(puzzleSize, mCubeSize, mPadding, mStickerSize);

        this.add(
                new Prism(Point.ORIGIN, 2, 2, 2),
                new Color(40, 40, 40)
        );

        char sFace;
        int[] sColor;
        Color color;

        for (int i = 0; i < puzzleSize; i++) {
            for (int j = 0; j < puzzleSize; j++) {

                sFace = state[AlgorithmModel.Case.FACE_F].charAt((puzzleSize * i) + j);
                sColor = AlgUtils.hexToRGBColor(AlgUtils.getColorLetterHashMapHex().get(sFace));
                color = new Color(sColor[0], sColor[1], sColor[2]);
                this.add(stickerLocations[0][i][j], color);

                sFace = state[AlgorithmModel.Case.FACE_L].charAt((puzzleSize * i) + j);
                sColor = AlgUtils.hexToRGBColor(AlgUtils.getColorLetterHashMapHex().get(sFace));
                color = new Color(sColor[0], sColor[1], sColor[2]);
                this.add(stickerLocations[1][i][j], color);

                sFace = state[AlgorithmModel.Case.FACE_U].charAt((puzzleSize * i) + j);
                sColor = AlgUtils.hexToRGBColor(AlgUtils.getColorLetterHashMapHex().get(sFace));
                color = new Color(sColor[0], sColor[1], sColor[2]);
                this.add(stickerLocations[2][i][j], color);
            }
        }
        return this;
    }
}
