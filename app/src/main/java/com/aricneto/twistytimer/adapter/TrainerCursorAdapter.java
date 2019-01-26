package com.aricneto.twistytimer.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.View;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.fragment.dialog.AlgDialog;
import com.aricneto.twistytimer.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.List;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class TrainerCursorAdapter extends AlgCursorAdapter{

    private List<Long> selectedItems = new ArrayList<>();
    private FragmentManager fragmentManager;

    int cardColor;
    int selectedCardColor;

    public TrainerCursorAdapter(Context context, Cursor cursor, Fragment listFragment) {
        super(context, cursor, listFragment);
        this.fragmentManager = listFragment.getFragmentManager();
        cardColor = ThemeUtils.fetchAttrColor(context, R.attr.colorItemListBackground);
        selectedCardColor = ThemeUtils.fetchAttrColor(context, R.attr.colorItemListBackgroundSelected);
    }

    private boolean isSelected(long id) {
        return selectedItems.contains(id);
    }

    public void unselectAll() {
        selectedItems.clear();
    }

    private void toggleSelection(long id, CardView card) {
        if (! isSelected(id)) {
            selectedItems.add(id);
            card.setCardBackgroundColor(selectedCardColor);
        } else {
            selectedItems.remove(id);
            card.setCardBackgroundColor(cardColor);
        }
    }

    @Override
    public void handleTime(AlgHolder holder, Cursor cursor) {
        super.handleTime(holder, cursor);

        long id = cursor.getLong(0);

        holder.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSelection(id, holder.card);
            }
        });

        holder.root.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (! isLocked()) {
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
    public Cursor swapCursor(Cursor cursor) {
        super.swapCursor(cursor);
        unselectAll();
        return cursor;
    }
}
