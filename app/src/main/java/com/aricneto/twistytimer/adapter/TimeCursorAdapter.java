package com.aricneto.twistytimer.adapter;

import android.content.Context;
import android.database.Cursor;
import androidx.fragment.app.FragmentManager;
import androidx.core.content.ContextCompat;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.fragment.TimerListFragment;
import com.aricneto.twistytimer.fragment.dialog.SolveDialog;
import com.aricneto.twistytimer.listener.DialogListener;
import com.aricneto.twistytimer.utils.PuzzleUtils;
import com.aricneto.twistytimer.utils.ThemeUtils;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.aricneto.twistytimer.utils.TTIntent.*;

/**
 * Created by Ari on 05/06/2015.
 */

public class TimeCursorAdapter extends CursorRecyclerAdapter<RecyclerView.ViewHolder> implements DialogListener {
    private final Context           mContext;  // Current context
    private final FragmentManager   mFragmentManager;

    Drawable cardBackground;
    Drawable selectedCardBackground;

    String mDateFormatSpec;

    private boolean isInSelectionMode;

    private List<Long> selectedItems = new ArrayList<>();

    // Locks opening new windows until the last one is dismissed
    private boolean isLocked;

    public TimeCursorAdapter(Context context, Cursor cursor, TimerListFragment listFragment) {
        super(cursor);
        this.mContext = context;
        this.mFragmentManager = listFragment.getFragmentManager();

        // Drawables for the cards
        cardBackground = ThemeUtils.createSquareDrawable(
                mContext,
                ThemeUtils.fetchAttrColor(mContext, R.attr.colorItemListBackground),
                0, 10, 0);
        selectedCardBackground = ThemeUtils.createSquareDrawable(
                mContext,
                ThemeUtils.fetchAttrColor(mContext, R.attr.colorItemListBackgroundSelected),
                Color.BLACK, 10, 2);

        mDateFormatSpec = context.getString(R.string.shortDateFormat);
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
        broadcast(CATEGORY_TIME_DATA_CHANGES, ACTION_TIMES_MODIFIED);
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
        broadcast(CATEGORY_UI_INTERACTIONS, ACTION_SELECTION_MODE_OFF);
    }

    public void deleteAllSelected() {
        TwistyTimer.getDBHandler().deleteSolvesByID(selectedItems, null); // Ignore progress.
        broadcast(CATEGORY_TIME_DATA_CHANGES, ACTION_TIMES_MODIFIED);
    }

    private void toggleSelection(long id, CardView card) {
        if (! isSelected(id)) {
            broadcast(CATEGORY_UI_INTERACTIONS, ACTION_TIME_SELECTED);
            selectedItems.add(id);
            card.setBackground(selectedCardBackground);
        } else {
            broadcast(CATEGORY_UI_INTERACTIONS, ACTION_TIME_UNSELECTED);
            selectedItems.remove(id);
            card.setBackground(cardBackground);
        }

        if (selectedItems.size() == 0) {
            broadcast(CATEGORY_UI_INTERACTIONS, ACTION_SELECTION_MODE_OFF);
            isInSelectionMode = false;
        }
    }

    private void handleTime(final TimeHolder holder, final Cursor cursor) {
        final long mId = cursor.getLong(0); // id
        final int pTime = cursor.getInt(3); // time
        final int pPenalty = cursor.getInt(6); // penalty
        final long pDate = cursor.getLong(4); // date
        final String pComment = cursor.getString(7); // comment

        holder.dateText.setText(new DateTime(pDate).toString(mDateFormatSpec));

        if (isSelected(mId))
            holder.card.setBackground(selectedCardBackground);
        else
            holder.card.setBackground(cardBackground);

        holder.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isInSelectionMode)
                    toggleSelection(mId, holder.card);
                else if (! isLocked()) {
                    setIsLocked(true);
                    SolveDialog solveDialog = SolveDialog.newInstance(mId);
                    solveDialog.show(mFragmentManager, "time_dialog");
                    solveDialog.setDialogListener(TimeCursorAdapter.this);
                }
            }
        });

        holder.root.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (! isInSelectionMode) {
                    isInSelectionMode = true;
                    broadcast(CATEGORY_UI_INTERACTIONS, ACTION_SELECTION_MODE_ON);
                    toggleSelection(mId, holder.card);
                }
                return true;
            }
        });

        holder.timeText.setText(Html.fromHtml(PuzzleUtils.convertTimeToString(pTime, PuzzleUtils.FORMAT_SMALL_MILLI)));
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
        @BindView(R.id.card)        CardView       card;
        @BindView(R.id.root)        RelativeLayout root;
        @BindView(R.id.timeText)    TextView       timeText;
        @BindView(R.id.penaltyText) TextView       penaltyText;
        @BindView(R.id.date)        TextView       dateText;
        @BindView(R.id.commentIcon) ImageView      commentIcon;

        public TimeHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
