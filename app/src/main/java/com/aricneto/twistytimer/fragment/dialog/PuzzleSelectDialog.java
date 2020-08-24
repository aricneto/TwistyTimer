package com.aricneto.twistytimer.fragment.dialog;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.listener.DialogListenerMessage;
import com.aricneto.twistytimer.utils.Prefs;
import com.aricneto.twistytimer.utils.PuzzleUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class PuzzleSelectDialog extends DialogFragment {

    private DialogListenerMessage dialogListener;
    private Unbinder mUnbinder;
    private Context mContext;

    @BindView(R.id.list) RecyclerView puzzleRecycler;

    public static PuzzleSelectDialog newInstance() {
        return new PuzzleSelectDialog();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View dialogView = inflater.inflate(R.layout.dialog_puzzle_select, container);
        mUnbinder = ButterKnife.bind(this, dialogView);

        mContext = getContext();

        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        puzzleRecycler.setHasFixedSize(true);

        GridLayoutManager layoutManager = new GridLayoutManager(mContext, 3, RecyclerView.VERTICAL, false);
        puzzleRecycler.setLayoutManager(layoutManager);

        PuzzleSelectAdapter puzzleAdapter = new PuzzleSelectAdapter(dialogListener,
                Pair.create(getString(R.string.cube_222), R.drawable.ic_2x2),
                Pair.create(getString(R.string.cube_333), R.drawable.ic_3x3),
                Pair.create(getString(R.string.cube_444), R.drawable.ic_4x4),
                Pair.create(getString(R.string.cube_555), R.drawable.ic_5x5),
                Pair.create(getString(R.string.cube_666), R.drawable.ic_6x6),
                Pair.create(getString(R.string.cube_777), R.drawable.ic_7x7),
                Pair.create(getString(R.string.cube_skewb), R.drawable.ic_skewb),
                Pair.create(getString(R.string.cube_mega), R.drawable.ic_mega),
                Pair.create(getString(R.string.cube_pyra), R.drawable.ic_pyra),
                Pair.create(getString(R.string.cube_sq1), R.drawable.ic_sq1),
                Pair.create(getString(R.string.cube_clock), R.drawable.ic_clock)
                );

        puzzleRecycler.setAdapter(puzzleAdapter);

        return dialogView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    public void setDialogListener(DialogListenerMessage listener) {
        this.dialogListener = listener;
    }
}

class PuzzleSelectAdapter extends RecyclerView.Adapter<PuzzleSelectAdapter.CardViewHolder> {

    DialogListenerMessage dialogListener;
    Pair<String, Integer>[] puzzles;

    static class CardViewHolder extends RecyclerView.ViewHolder {
        View view;
        TextView title;
        ImageView icon;

        public CardViewHolder(View view) {
            super(view);
            this.view = view;
            this.title = view.findViewById(R.id.title);
            this.icon = view.findViewById(R.id.icon);
        }
    }

    public PuzzleSelectAdapter(DialogListenerMessage dialogListener, Pair<String, Integer>... puzzles) {
        this.dialogListener = dialogListener;
        this.puzzles = puzzles;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_puzzle_select, parent, false);
        CardViewHolder viewHolder = new CardViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        holder.title.setText(puzzles[position].first);
        holder.icon.setImageResource(puzzles[position].second);

        holder.view.setOnClickListener(v -> {
            if (dialogListener != null)
                dialogListener.onUpdateDialog(PuzzleUtils.getPuzzleInPosition(position));
        });
    }

    @Override
    public int getItemCount() {
        return puzzles.length;
    }
}
