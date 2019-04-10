package com.aricneto.twistytimer.adapter;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.fragment.BaseFragment;
import com.aricneto.twistytimer.fragment.dialog.AlgDialog;
import com.aricneto.twistytimer.items.AlgorithmModel;
import com.aricneto.twistytimer.layout.Cube;
import com.aricneto.twistytimer.layout.isometric.Color;
import com.aricneto.twistytimer.layout.isometric.IsometricView;
import com.aricneto.twistytimer.layout.isometric.Path;
import com.aricneto.twistytimer.layout.isometric.Point;
import com.aricneto.twistytimer.layout.isometric.shapes.Prism;
import com.aricneto.twistytimer.listener.DialogListener;
import com.aricneto.twistytimer.utils.AlgUtils;
import com.aricneto.twistytimer.utils.StoreUtils;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

import static com.aricneto.twistytimer.items.AlgorithmModel.*;

/**
 * Created by Ari on 05/06/2015.
 */

public class AlgRecylerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements DialogListener {

    private Context          mContext;
    private HashMap<Character, Integer> colorHash;

    private String          mSubset;
    private ArrayList<Case> cases;

    private final int mCubePuzzleSize = 3; // 3x3
    private final double mCubeSize = 2;
    private double mPadding = mCubeSize * 0.06;
    private double mStickerSize = (mCubeSize - (mPadding * (mCubePuzzleSize + 1))) / mCubePuzzleSize;

    private FragmentManager fragmentManager;

    // Locks opening new windows until the last one is dismissed
    private boolean isLocked;

    public AlgRecylerAdapter(Context context, FragmentManager manager, String mSubset) {
        this.mContext = context;
        this.colorHash = AlgUtils.getColorLetterHashMap();
        this.mSubset = mSubset;
        this.fragmentManager = manager;

        String myJson = StoreUtils.inputStreamToString(context.getResources().openRawResource(R.raw.algorithms));
        AlgorithmModel model = new Gson().fromJson(myJson, AlgorithmModel.class);
        this.cases = model.subsets.get(0).getCases();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View v;
        RecyclerView.ViewHolder viewHolder;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        v = inflater.inflate(R.layout.item_alg_list, parent, false);
        viewHolder = new AlgHolder(v);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        handleTime((AlgHolder) holder, position);
    }

    @Override
    public void onUpdateDialog() {
        // Do nothing.
    }

    @Override
    public void onDismissDialog() {
        setIsLocked(false);
    }

    public void handleTime(AlgHolder holder, int position) {
        Case pCase = cases.get(position);
        final String pName = pCase.getName();
        final String[] pState = pCase.getState();

        holder.root.setOnClickListener(view -> {
            if (! isLocked()) {
                setIsLocked(true);
                AlgDialog algDialog = AlgDialog.newInstance(mSubset, pCase);
                algDialog.show(fragmentManager, "alg_dialog");
                algDialog.setDialogListener(AlgRecylerAdapter.this);
            }
        });

        holder.name.setText(pName);
        //holder.progressBar.setProgress(pProgress);
        //holder.cube.setCubeState(pState);

        holder.cube.add(
                new Prism(Point.ORIGIN, 2, 2, 2),
                new Color(40, 40, 40)
        );

        // Every start sticker starts at the top-left of the face
        Point[] rightStartPoint = new Point[] {
                new Point(mPadding, 0, mCubeSize - mPadding),
                new Point(mPadding + mStickerSize, 0, mCubeSize - mPadding),
                new Point(mPadding + mStickerSize, 0, mCubeSize - mPadding - mStickerSize),
                new Point(mPadding, 0, mCubeSize - mPadding - mStickerSize),
        };

        Point[] leftStartPoint = new Point[] {
                new Point(0, mCubeSize - mPadding, mCubeSize - mPadding),
                new Point(0, mCubeSize - mPadding - mStickerSize, mCubeSize - mPadding),
                new Point(0, mCubeSize - mPadding - mStickerSize, mCubeSize - mPadding - mStickerSize),
                new Point(0, mCubeSize - mPadding, mCubeSize - mPadding - mStickerSize),
        };

        Point[] topStartPoint = new Point[] {
                new Point(mPadding, mCubeSize - mPadding, mCubeSize),
                new Point(mPadding + mStickerSize, mCubeSize - mPadding, mCubeSize),
                new Point(mPadding + mStickerSize, mCubeSize - mPadding - mStickerSize, mCubeSize),
                new Point(mPadding, mCubeSize - mPadding - mStickerSize, mCubeSize),
        };


        char sFace;
        int[] sColor;

        for (int i = 0; i < mCubePuzzleSize; i++) {
            for (int j = 0; j < mCubePuzzleSize; j++) {

                sFace = pState[Case.FACE_F].charAt((mCubePuzzleSize * i) + j);
                sColor = AlgUtils.hexToRGBColor(AlgUtils.getColorLetterHashMapHex().get(sFace));
                holder.cube.add(new Path(
                                        translatePoints(rightStartPoint, (mPadding + mStickerSize) * j, 0, -(mPadding + mStickerSize) * i)),
                                new Color(sColor[0], sColor[1], sColor[2]));

                sFace = pState[Case.FACE_L].charAt((mCubePuzzleSize * i) + j);
                sColor = AlgUtils.hexToRGBColor(AlgUtils.getColorLetterHashMapHex().get(sFace));
                holder.cube.add(new Path(
                                        translatePoints(leftStartPoint, 0, -(mPadding + mStickerSize) * j, -(mPadding + mStickerSize) * i)),
                                new Color(sColor[0], sColor[1], sColor[2]));

                sFace = pState[Case.FACE_U].charAt((mCubePuzzleSize * i) + j);
                sColor = AlgUtils.hexToRGBColor(AlgUtils.getColorLetterHashMapHex().get(sFace));
                holder.cube.add(new Path(
                                        translatePoints(topStartPoint, (mPadding + mStickerSize) * j, -(mPadding + mStickerSize) * i, 0)),
                                new Color(sColor[0], sColor[1], sColor[2]));
            }
        }




        // If the mSubset is PLL, it'll need to show the pll arrows.
        if (mSubset.equals("PLL")) {
            holder.pllArrows.setImageDrawable(AlgUtils.getPllArrow(mContext, pName));
            holder.pllArrows.setVisibility(View.VISIBLE);
        }

    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setIsLocked(boolean isLocked) {
        this.isLocked = isLocked;
    }

    private Point[] translatePoints(Point[] startPoints, double dx, double dy, double dz) {
        Point[] newPoint = new Point[4];
        for (int i = 0; i < 4; i++)
            newPoint[i] = startPoints[i].translate(dx, dy, dz);
        return newPoint;
    }

    private Point[] scalePoints(Point[] startPoints, double dd) {
        Point[] newPoint = new Point[4];
        Point origin = getPointOrigin(startPoints);
        for (int i = 0; i < startPoints.length; i++)
            newPoint[i] = startPoints[i].scale(origin, dd, dd, dd);
        return newPoint;
    }

    private Point getPointOrigin(Point[] startPoints) {
        return new Point(
                (startPoints[0].getX() + startPoints[2].getX()) / 2,
                (startPoints[0].getY() + startPoints[2].getY()) / 2,
                (startPoints[0].getZ() + startPoints[2].getZ()) / 2);
    }

    @Override
    public int getItemCount() {
        return cases.size();
    }

    static class AlgHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.name)       TextView            name;
        @BindView(R.id.pll_arrows) ImageView           pllArrows;
        @BindView(R.id.progressBar)MaterialProgressBar progressBar;
        @BindView(R.id.root) RelativeLayout root;
        @BindView(R.id.card) CardView       card;
        @BindView(R.id.cube) IsometricView  cube;

        public AlgHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
