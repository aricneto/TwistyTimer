package com.aricneto.twistytimer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.fragment.dialog.AlgDialog;
import com.aricneto.twistytimer.items.Algorithm;
import com.aricneto.twistytimer.layout.Cube2D;
import com.aricneto.twistytimer.layout.CubeIsometric;
import com.aricneto.twistytimer.listener.DialogListener;
import com.aricneto.twistytimer.utils.AlgUtils;
import com.aricneto.twistytimer.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

import static com.aricneto.twistytimer.items.AlgorithmModel.Case;

/**
 * Created by Ari on 05/06/2015.
 */

public class AlgRecylerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements DialogListener {

    private Context                     mContext;

    private String          mSubset;
    private String          mPuzzle;
    private ArrayList<Case> cases;
    private int             mCubePuzzleSize;

    private boolean mIsIsometricView;

    private FragmentManager fragmentManager;

    // Locks opening new windows until the last one is dismissed
    private boolean isLocked;

    public AlgRecylerAdapter(Context context, FragmentManager manager, String puzzle, String subset) {
        this.mContext = context;
        this.mSubset = subset;
        this.mPuzzle = puzzle;
        this.fragmentManager = manager;

        this.cases = AlgUtils.getAlgJsonSubsetModel(puzzle, subset).getCases();

        mIsIsometricView = AlgUtils.isIsometricView(puzzle, subset);
        mCubePuzzleSize = AlgUtils.getPuzzleSize(puzzle);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        View v = inflater.inflate(R.layout.item_alg_list, parent, false);

        return new AlgHolder(v);
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
            if (!isLocked()) {
                setIsLocked(true);
                AlgDialog algDialog = AlgDialog.newInstance(mPuzzle, mSubset, pCase);
                algDialog.show(fragmentManager, "alg_dialog");
                algDialog.setDialogListener(AlgRecylerAdapter.this);
            }
        });

        holder.name.setText(pName);

        Algorithm algorithm = AlgUtils.getAlgFromDB(mPuzzle, mSubset, pName);
        holder.progressBar.setProgress(algorithm.getProgress());

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ThemeUtils.dpToPix(108), ThemeUtils.dpToPix(108));
        params.addRule(RelativeLayout.BELOW, R.id.progressBar);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        params.topMargin = ThemeUtils.dpToPix(4);

        // Remove old view if there is one (Remember that in a RecyclerView the view is recycled)
        View cube = holder.root.findViewById(R.id.cube);
        if (cube != null)
            holder.root.removeView(cube);

        if (mIsIsometricView)
            cube = new CubeIsometric(mContext).init(70, mCubePuzzleSize, pState);
        else
            cube = new Cube2D(mContext).init(mCubePuzzleSize, pState);

        cube.setId(R.id.cube);
        holder.root.addView(cube, params);

//         If mSubset is PLL, it'll need to show the pll arrows.
        if (mSubset.equals("PLL")) {
            ImageView pllArrows = new ImageView(mContext);
            pllArrows.setImageDrawable(AlgUtils.getPllArrow(mContext, pName));
            pllArrows.setScaleX(0.65f);
            pllArrows.setScaleY(0.65f);
            holder.root.addView(pllArrows, params);
        }

    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setIsLocked(boolean isLocked) {
        this.isLocked = isLocked;
    }

    @Override
    public int getItemCount() {
        return cases.size();
    }

    static class AlgHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.name)
        TextView name;
        @BindView(R.id.progressBar)
        MaterialProgressBar progressBar;
        @BindView(R.id.root)
        RelativeLayout root;
        @BindView(R.id.card)
        CardView       card;

        AlgHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
