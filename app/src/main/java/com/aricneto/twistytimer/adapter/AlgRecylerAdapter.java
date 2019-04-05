package com.aricneto.twistytimer.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.database.DatabaseHandler;
import com.aricneto.twistytimer.fragment.AlgListFragment;
import com.aricneto.twistytimer.fragment.dialog.AlgDialog;
import com.aricneto.twistytimer.items.AlgorithmModel;
import com.aricneto.twistytimer.layout.Cube;
import com.aricneto.twistytimer.listener.DialogListener;
import com.aricneto.twistytimer.utils.AlgUtils;
import com.aricneto.twistytimer.utils.StoreUtils;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

/**
 * Created by Ari on 05/06/2015.
 */

public class AlgRecylerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements DialogListener {
    private Context          mContext;
    HashMap<Character, Integer> colorHash;

    private String subset;
    private ArrayList<AlgorithmModel.Case> cases;

    // Locks opening new windows until the last one is dismissed
    private boolean isLocked;

    public AlgRecylerAdapter(Context context, String subset) {
        this.mContext = context;
        this.colorHash = AlgUtils.getColorLetterHashMap();
        this.subset = subset;

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
        final String pName = cases.get(position).getName();
        final String pState = AlgUtils.getCaseState(mContext, subset, pName);

//        holder.root.setOnClickListener(view -> {
//            if (! isLocked()) {
//                setIsLocked(true);
//                AlgDialog algDialog = AlgDialog.newInstance(mId);
//                algDialog.show(mFragmentManager, "alg_dialog");
//                algDialog.setDialogListener(AlgRecylerAdapter.this);
//            }
//        });

        holder.name.setText(pName);
        //holder.progressBar.setProgress(pProgress);
        holder.cube.setCubeState(pState);

        // If the subset is PLL, it'll need to show the pll arrows.
        if (subset.equals("PLL")) {
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
