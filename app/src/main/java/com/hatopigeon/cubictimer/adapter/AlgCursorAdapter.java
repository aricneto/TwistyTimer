package com.hatopigeon.cubictimer.adapter;

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

import com.hatopigeon.cubicify.R;
import com.hatopigeon.cubictimer.database.DatabaseHandler;
import com.hatopigeon.cubictimer.fragment.AlgListFragment;
import com.hatopigeon.cubictimer.fragment.dialog.AlgDialog;
import com.hatopigeon.cubictimer.layout.Cube;
import com.hatopigeon.cubictimer.listener.DialogListener;
import com.hatopigeon.cubictimer.utils.AlgUtils;

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
        colorHash = AlgUtils.getColorLetterHashMap();
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
        final String pSubset = cursor.getString(1);
        final String pState = AlgUtils.getCaseState(mContext, pSubset, pName);
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
        holder.cube.setCubeState(pState);

        // If the subset is PLL, it'll need to show the pll arrows.
        if (cursor.getString(1).equals("PLL")) {
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
