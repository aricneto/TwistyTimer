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
import com.aricneto.twistytimer.listener.DialogListener;
import com.aricneto.twistytimer.utils.AlgUtils;
import com.aricneto.twistytimer.utils.StoreUtils;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

/**
 * Created by Ari on 05/06/2015.
 */

public class AlgRecylerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements DialogListener {

    private Context          mContext;
    private HashMap<Character, Integer> colorHash;

    private String                         mSubset;
    private ArrayList<AlgorithmModel.Case> cases;

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
        AlgorithmModel.Case pCase = cases.get(position);
        final String pName = pCase.getName();
        final String pState = AlgUtils.getCaseState(mContext, mSubset, pName);

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
        holder.cube.setCubeState(pState);

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

    @Override
    public int getItemCount() {
        return cases.size();
    }

    static class AlgHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.name)        TextView            name;
        @BindView(R.id.pll_arrows)  ImageView           pllArrows;
        @BindView(R.id.progressBar) MaterialProgressBar progressBar;
        @BindView(R.id.root)        RelativeLayout      root;
        @BindView(R.id.card)        CardView            card;
        @BindView(R.id.cube)        Cube                cube;

        public AlgHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
