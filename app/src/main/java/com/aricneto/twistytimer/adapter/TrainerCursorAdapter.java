package com.aricneto.twistytimer.adapter;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.fragment.dialog.AlgDialog;
import com.aricneto.twistytimer.puzzle.TrainerScrambler;
import com.aricneto.twistytimer.utils.Prefs;
import com.aricneto.twistytimer.utils.ThemeUtils;
import com.github.mikephil.charting.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class TrainerCursorAdapter extends AlgCursorAdapter {

    private List<Long> selectedItems;
    private FragmentManager fragmentManager;
    private Context mContext;

    TrainerScrambler.TrainerSubset currentSubset;
    String currentPuzzleCategory;

    int cardColor;
    int selectedCardColor;

    public TrainerCursorAdapter(Context context, Cursor cursor, Fragment listFragment, TrainerScrambler.TrainerSubset subset, String category) {
        super(context, cursor, listFragment);
        Log.d("TrainerCursor", "Created trainerCursor " + subset + category);
        this.mContext = context;
        this.fragmentManager = listFragment.getFragmentManager();

        cardColor = ThemeUtils.fetchAttrColor(context, R.attr.colorItemListBackground);
        selectedCardColor = ThemeUtils.fetchAttrColor(context, R.attr.colorItemListBackgroundSelected);

        selectedItems = new ArrayList<>();
        selectedItems.addAll(TrainerScrambler.fetchSelectedItemsLong(subset, category));

        this.currentSubset = subset;
        this.currentPuzzleCategory = category;

    }

    private boolean isSelected(long id) {
        return selectedItems.contains(id);
    }

    public void unselectAll() {
        selectedItems.clear();
    }

    private void toggleSelection(long id, CardView card) {
        if (!isSelected(id)) {
            selectedItems.add(id);
            card.setCardBackgroundColor(selectedCardColor);
        } else {
            selectedItems.remove(id);
            card.setCardBackgroundColor(cardColor);
        }
        TrainerScrambler.saveSelectedItems(currentSubset, currentPuzzleCategory, selectedItems);
    }

    @Override
    public void handleTime(AlgHolder holder, Cursor cursor) {
        super.handleTime(holder, cursor);

        long id = cursor.getLong(0);

        if (isSelected(id)) {
            holder.card.setCardBackgroundColor(selectedCardColor);
        } else {
            holder.card.setCardBackgroundColor(cardColor);
        }

        holder.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSelection(id, holder.card);
            }
        });

        holder.root.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!isLocked()) {
                    setIsLocked(true);
                    AlgDialog algDialog = AlgDialog.newInstance(id);
                    algDialog.show(fragmentManager, "alg_dialog");
                    algDialog.setDialogListener(TrainerCursorAdapter.this);
                }
                return true;
            }
        });
    }

    @Override
    public void colorCube(AlgHolder holder, String state) {
        int i = 0;
        for (View sticker : holder.stickers) {
            sticker.setBackgroundColor(colorHash.get(state.charAt(i)));
            i++;
        }
    }

    @Override
    public Cursor swapCursor(Cursor cursor) {
        super.swapCursor(cursor);
        //unselectAll();
        return cursor;
    }
}
