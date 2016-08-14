package com.aricneto.twistytimer.adapter;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
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
import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.database.DatabaseHandler;
import com.aricneto.twistytimer.fragment.dialog.TimeDialog;
import com.aricneto.twistytimer.fragment.TimerListFragment;
import com.aricneto.twistytimer.listener.DialogListener;
import com.aricneto.twistytimer.utils.PuzzleUtils;
import com.aricneto.twistytimer.utils.ThemeUtils;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Ari on 05/06/2015.
 */

public class TimeCursorAdapter extends CursorRecyclerAdapter<RecyclerView.ViewHolder> implements DialogListener {
    private final Context           mContext;  // Current context
    private final FragmentManager   mFragmentManager;
    private final TimeCursorAdapter thisThing; // HOLY MOTHER OF WORKAROUNDS

    int cardColor;
    int selectedCardColor;

    private boolean isInSelectionMode;

    private List<Long> selectedItems = new ArrayList<>();

    // Locks opening new windows until the last one is dismissed
    private boolean isLocked;

    public TimeCursorAdapter(Context context, Cursor cursor, TimerListFragment listFragment) {
        super(cursor);
        this.mContext = context;
        this.mFragmentManager = listFragment.getFragmentManager();
        cardColor = ThemeUtils.fetchAttrColor(mContext, R.attr.colorItemListBackground);
        selectedCardColor = ThemeUtils.fetchAttrColor(mContext, R.attr.colorItemListBackgroundSelected);
        thisThing = this;
    }

    @Override
    public Cursor swapCursor(Cursor cursor) {
        super.swapCursor(cursor);
        unselectAll();
        return cursor;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View v;
        RecyclerView.ViewHolder viewHolder;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        v = inflater.inflate(R.layout.item_time_list, parent, false);
        viewHolder = new TimeHolder(v);

        return viewHolder;
    }

    @Override
    public void onBindViewHolderCursor(final RecyclerView.ViewHolder viewHolder, final Cursor cursor) {
        TimeHolder holder = (TimeHolder) viewHolder;
        handleTime(holder, cursor);

    }

    @Override
    public void onUpdateDialog() {
        Intent sendIntent = new Intent("TIMELIST");
        sendIntent.putExtra("action", "TIME UPDATED");
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(sendIntent);
    }

    @Override
    public void onDismissDialog() {
        setIsLocked(false);
    }

    private boolean isSelected(long id) {
        return selectedItems.contains(id);
    }

    public void unselectAll() {
        selectedItems.clear();
        isInSelectionMode = false;
        broadcastToMain("SELECTIONMODE FALSE");
    }

    public void deleteAllSelected() {
        TwistyTimer.getDBHandler().deleteAllFromList(selectedItems);
        resetList();
    }

    private void resetList() {
        Intent sendIntent = new Intent("TIMELIST");
        sendIntent.putExtra("action", "TIME ADDED");
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(sendIntent);
    }

    private void toggleSelection(long id, CardView card) {
        if (! isSelected(id)) {
            broadcastToMain("LISTITEM SELECTED");
            selectedItems.add(id);
            card.setCardBackgroundColor(selectedCardColor);
        } else {
            broadcastToMain("LISTITEM UNSELECTED");
            selectedItems.remove(id);
            card.setCardBackgroundColor(cardColor);
        }

        if (selectedItems.size() == 0) {
            broadcastToMain("SELECTIONMODE FALSE");
            isInSelectionMode = false;
        }
    }

    private void broadcastToMain(String message) {
        Intent sendIntent = new Intent("TIMER");
        sendIntent.putExtra("action", message);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(sendIntent);
    }

    private void handleTime(final TimeHolder holder, final Cursor cursor) {
        final long mId = cursor.getLong(0); // id
        final int pTime = cursor.getInt(3); // time
        final int pPenalty = cursor.getInt(6); // penalty
        final long pDate = cursor.getLong(4); // date
        final String pComment = cursor.getString(7); // comment

        holder.dateText.setText(new DateTime(pDate).toString("dd'/'MM"));

        if (isSelected(mId))
            holder.card.setCardBackgroundColor(selectedCardColor);
        else
            holder.card.setCardBackgroundColor(cardColor);

        holder.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isInSelectionMode)
                    toggleSelection(mId, holder.card);
                else if (! isLocked()) {
                    setIsLocked(true);
                    TimeDialog timeDialog = TimeDialog.newInstance(mId);
                    timeDialog.show(mFragmentManager, "time_dialog");
                    timeDialog.setDialogListener(thisThing);
                }

            }
        });

        holder.root.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (! isInSelectionMode) {
                    isInSelectionMode = true;
                    broadcastToMain("SELECTIONMODE TRUE");
                    toggleSelection(mId, holder.card);
                }
                return true;
            }
        });

        holder.timeText.setText(PuzzleUtils.convertTimeToString(pTime));
        holder.penaltyText.setTextColor(ContextCompat.getColor(mContext, R.color.red_material));

        switch (pPenalty) {
            case PuzzleUtils.PENALTY_DNF:
                holder.timeText.setText("DNF");
                holder.penaltyText.setVisibility(View.GONE);
                break;
            case PuzzleUtils.PENALTY_PLUSTWO:
                holder.penaltyText.setText("+2");
                holder.penaltyText.setVisibility(View.VISIBLE);
                break;
            default:
                holder.penaltyText.setVisibility(View.GONE);
                break;
        }

        if (! pComment.equals("")) {
            holder.commentIcon.setVisibility(View.VISIBLE);
        } else {
            // This else is needed because the view recycles.
            holder.commentIcon.setVisibility(View.GONE);
        }

    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setIsLocked(boolean isLocked) {
        this.isLocked = isLocked;
    }

    static class TimeHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.card)        CardView       card;
        @Bind(R.id.root)        RelativeLayout root;
        @Bind(R.id.timeText)    TextView       timeText;
        @Bind(R.id.penaltyText) TextView       penaltyText;
        @Bind(R.id.date)        TextView       dateText;
        @Bind(R.id.commentIcon) ImageView      commentIcon;

        public TimeHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
