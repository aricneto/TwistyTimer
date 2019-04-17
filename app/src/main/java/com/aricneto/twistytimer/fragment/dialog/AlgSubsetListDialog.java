package com.aricneto.twistytimer.fragment.dialog;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.items.AlgorithmModel;
import com.aricneto.twistytimer.layout.Cube2D;
import com.aricneto.twistytimer.layout.CubeIsometric;
import com.aricneto.twistytimer.listener.AlgorithmDialogListener;
import com.aricneto.twistytimer.utils.AlgUtils;

import java.util.ArrayList;
import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class AlgSubsetListDialog extends DialogFragment {

    private AlgorithmDialogListener mDialogListener;
    private Unbinder mUnbinder;
    private Context mContext;

    @BindView(R.id.list) RecyclerView recyclerView;

    public static AlgSubsetListDialog newInstance() {
        return new AlgSubsetListDialog();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View dialogView = inflater.inflate(R.layout.dialog_puzzle_select, container);
        mUnbinder = ButterKnife.bind(this, dialogView);

        mContext = getContext();

        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        recyclerView.setHasFixedSize(true);

        GridLayoutManager layoutManager = new GridLayoutManager(mContext, 3, RecyclerView.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);

        AlgSubsetListAdapter listAdapter = new AlgSubsetListAdapter(mContext, mDialogListener);

        recyclerView.setAdapter(listAdapter);

        return dialogView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    public void setDialogListener(AlgorithmDialogListener listener) {
        this.mDialogListener = listener;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }
}

class AlgSubsetListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context mContext;
    private final AlgorithmDialogListener mDialogListener;
    private final ArrayList<AlgorithmModel.Subset> subsets;

    public AlgSubsetListAdapter(Context context, AlgorithmDialogListener listener) {
        this.mContext = context;
        this.mDialogListener = listener;

        AlgorithmModel model = AlgUtils.getAlgJsonModel();
        this.subsets = new ArrayList<>();
        subsets.addAll(model.subsets);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        View v = inflater.inflate(R.layout.item_alg_case_list, parent, false);

        return new CaseHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        handleCase((CaseHolder) holder, position);
    }

    @Override
    public int getItemCount() {
        return subsets.size();
    }

    private void handleCase(CaseHolder holder, int position) {
        AlgorithmModel.Subset subset = subsets.get(position);
        String name = subset.getSubset();
        String cases = String.format("%d %s", subset.getCases().size(), "cases");

        holder.name.setText(name);
        holder.numCases.setText(cases);

        int puzzleSize = AlgUtils.getPuzzleSize(subset.getPuzzle());
        AlgorithmModel.Case firstCase = subset.getCases().get(0);

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.cube.getLayoutParams();

        // Remove old view if there is one (Remember that in a RecyclerView the view is recycled)
        View cube = holder.root.findViewById(R.id.cube);
        if (cube != null)
            holder.root.removeView(cube);

        if (!AlgUtils.isIsometricView(subset.getSubset()))
            cube = CubeIsometric.init(mContext, puzzleSize, firstCase.getState());
        else
            cube = new Cube2D(mContext).setCubeState(puzzleSize, firstCase.getState());

        cube.setId(R.id.cube);
        holder.root.addView(cube, params);

        holder.root.setOnClickListener(v -> mDialogListener.onSubsetSelected(subset.getPuzzle(), subset.getSubset()));
    }

    static class CaseHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.case_name)
        TextView name;
        @BindView(R.id.num_cases)
        TextView numCases;
        @BindView(R.id.root)
        RelativeLayout root;
        @BindView(R.id.cube)
        View cube;

        public CaseHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

}

