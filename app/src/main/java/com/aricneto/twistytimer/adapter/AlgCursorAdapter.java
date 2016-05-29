package com.aricneto.twistytimer.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.fragment.dialog.AlgDialog;
import com.aricneto.twistytimer.fragment.AlgListFragment;
import com.aricneto.twistytimer.listener.DialogListener;
import com.aricneto.twistytimer.utils.AlgUtils;

import java.util.HashMap;

import butterknife.Bind;
import butterknife.ButterKnife;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

/**
 * Created by Ari on 05/06/2015.
 */

public class AlgCursorAdapter extends CursorRecyclerAdapter<RecyclerView.ViewHolder> implements DialogListener {
    private final Context          mContext;  // Current context
    private final FragmentManager  mFragmentManager;
    private final AlgCursorAdapter thisThing; // HOLY MOTHER OF WORKAROUNDS
    HashMap<Character, Integer> colorHash;

    // Locks opening new windows until the last one is dismissed
    private boolean isLocked;

    public AlgCursorAdapter(Context context, Cursor cursor, AlgListFragment listFragment) {
        super(cursor);
        this.mContext = context;
        this.mFragmentManager = listFragment.getFragmentManager();
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        colorHash = AlgUtils.getColorLetterHashMap(sp);
        thisThing = this;
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
        Intent sendIntent = new Intent("ALGLIST");
        sendIntent.putExtra("action", "ALG ADDED");
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(sendIntent);
    }

    @Override
    public void onDismissDialog() {
        setIsLocked(false);
    }

    private void handleTime(final AlgHolder holder, final Cursor cursor) {
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
                    algDialog.setDialogListener(thisThing);
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

    private void colorCube(AlgHolder holder, String state) {

        /*See the reference image to understand how this works
        * Yeah, I know. It's shitty as hell */

        char[] charState = state.toCharArray();
        holder.sticker1.setBackgroundColor(colorHash.get(charState[0]));
        holder.sticker2.setBackgroundColor(colorHash.get(charState[1]));
        holder.sticker3.setBackgroundColor(colorHash.get(charState[2]));
        holder.sticker4.setBackgroundColor(colorHash.get(charState[3]));
        holder.sticker5.setBackgroundColor(colorHash.get(charState[4]));
        holder.sticker6.setBackgroundColor(colorHash.get(charState[5]));
        holder.sticker7.setBackgroundColor(colorHash.get(charState[6]));
        holder.sticker8.setBackgroundColor(colorHash.get(charState[7]));
        holder.sticker9.setBackgroundColor(colorHash.get(charState[8]));
        holder.sticker10.setBackgroundColor(colorHash.get(charState[9]));
        holder.sticker11.setBackgroundColor(colorHash.get(charState[10]));
        holder.sticker12.setBackgroundColor(colorHash.get(charState[11]));
        holder.sticker13.setBackgroundColor(colorHash.get(charState[12]));
        holder.sticker14.setBackgroundColor(colorHash.get(charState[13]));
        holder.sticker15.setBackgroundColor(colorHash.get(charState[14]));
        holder.sticker16.setBackgroundColor(colorHash.get(charState[15]));
        holder.sticker17.setBackgroundColor(colorHash.get(charState[16]));
        holder.sticker18.setBackgroundColor(colorHash.get(charState[17]));
        holder.sticker19.setBackgroundColor(colorHash.get(charState[18]));
        holder.sticker20.setBackgroundColor(colorHash.get(charState[19]));
        holder.sticker21.setBackgroundColor(colorHash.get(charState[20]));

    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setIsLocked(boolean isLocked) {
        this.isLocked = isLocked;
    }

    static class AlgHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.name)       TextView  name;
        @Bind(R.id.pll_arrows) ImageView pllArrows;
        @Bind(R.id.sticker1)   View      sticker1;
        @Bind(R.id.sticker2)   View      sticker2;
        @Bind(R.id.sticker3)   View      sticker3;
        @Bind(R.id.sticker4)   View      sticker4;
        @Bind(R.id.sticker5)   View      sticker5;
        @Bind(R.id.sticker6)   View      sticker6;
        @Bind(R.id.sticker7)   View      sticker7;
        @Bind(R.id.sticker8)   View      sticker8;
        @Bind(R.id.sticker9)   View      sticker9;
        @Bind(R.id.sticker10)  View      sticker10;
        @Bind(R.id.sticker11)  View      sticker11;
        @Bind(R.id.sticker12)  View      sticker12;
        @Bind(R.id.sticker13)  View      sticker13;
        @Bind(R.id.sticker14)  View      sticker14;
        @Bind(R.id.sticker15)  View      sticker15;
        @Bind(R.id.sticker16)  View      sticker16;
        @Bind(R.id.sticker17)  View      sticker17;
        @Bind(R.id.sticker18)  View      sticker18;
        @Bind(R.id.sticker19)  View      sticker19;
        @Bind(R.id.sticker20)  View      sticker20;
        @Bind(R.id.sticker21)  View      sticker21;

        @Bind(R.id.progressBar) MaterialProgressBar progressBar;
        @Bind(R.id.root)        RelativeLayout      root;
        @Bind(R.id.card)        CardView            card;

        public AlgHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

}
