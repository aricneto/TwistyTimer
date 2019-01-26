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
import com.aricneto.twistytimer.fragment.AlgListFragment;
import com.aricneto.twistytimer.fragment.dialog.AlgDialog;
import com.aricneto.twistytimer.listener.DialogListener;
import com.aricneto.twistytimer.utils.AlgUtils;

import java.util.HashMap;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

/**
 * Created by Ari on 05/06/2015.
 */

public class AlgCursorAdapter extends CursorRecyclerAdapter<RecyclerView.ViewHolder> implements DialogListener {
    private final Context          mContext;  // Current context
    private final FragmentManager  mFragmentManager;
    HashMap<Character, Integer> colorHash;

    // Locks opening new windows until the last one is dismissed
    private boolean isLocked;

    public AlgCursorAdapter(Context context, Cursor cursor, Fragment listFragment) {
        super(cursor);
        this.mContext = context;
        this.mFragmentManager = listFragment.getFragmentManager();
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        colorHash = AlgUtils.getColorLetterHashMap(sp);
    }

    @Override
    public Cursor swapCursor(Cursor cursor) {
        super.swapCursor(cursor);
        return cursor;
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
    public void onBindViewHolderCursor(final RecyclerView.ViewHolder viewHolder, final Cursor cursor) {
        AlgHolder holder = (AlgHolder) viewHolder;
        handleTime(holder, cursor);

    }

    @Override
    public void onUpdateDialog() {
        // Do nothing.
    }

    @Override
    public void onDismissDialog() {
        setIsLocked(false);
    }

    public void handleTime(final AlgHolder holder, final Cursor cursor) {
        final long mId = cursor.getLong(0); // id
        final String pName = cursor.getString(2);
        final String pState = cursor.getString(3);
        final int pProgress = cursor.getInt(5);

        holder.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (! isLocked()) {
                    setIsLocked(true);
                    AlgDialog algDialog = AlgDialog.newInstance(mId);
                    algDialog.show(mFragmentManager, "alg_dialog");
                    algDialog.setDialogListener(AlgCursorAdapter.this);
                }

            }
        });

        holder.name.setText(pName);
        holder.progressBar.setProgress(pProgress);
        colorCube(holder, pState);

        // If the subset is PLL, it'll need to show the pll arrows.
        if (cursor.getString(1).equals("PLL")) {
            holder.pllArrows.setImageDrawable(AlgUtils.getPllArrow(mContext, pName));
            holder.pllArrows.setVisibility(View.VISIBLE);
        }

    }

    public void colorCube(AlgHolder holder, String state) {
        int i = 0;
        for (View sticker : holder.stickers) {
            sticker.setBackgroundColor(colorHash.get(state.charAt(i)));
            i++;
        }
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setIsLocked(boolean isLocked) {
        this.isLocked = isLocked;
    }

    static class AlgHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.name)        TextView            name;
        @BindView(R.id.pll_arrows)  ImageView           pllArrows;
        @BindView(R.id.progressBar) MaterialProgressBar progressBar;
        @BindView(R.id.root)        RelativeLayout      root;
        @BindView(R.id.card)        CardView            card;

        @BindViews({
                R.id.sticker1,  R.id.sticker2,  R.id.sticker3,  R.id.sticker4,
                R.id.sticker5,  R.id.sticker6,  R.id.sticker7,  R.id.sticker8,
                R.id.sticker9,  R.id.sticker10, R.id.sticker11, R.id.sticker12,
                R.id.sticker13, R.id.sticker14, R.id.sticker15, R.id.sticker16,
                R.id.sticker17, R.id.sticker18, R.id.sticker19, R.id.sticker20,
                R.id.sticker21,
        }) View[] stickers;

        public AlgHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
