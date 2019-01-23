package com.aricneto.twistytimer.fragment.dialog;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.adapter.BottomSheetSpinnerAdapter;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/**
 * Implements a layout for easy creation of spinner-like bottom sheet dialogs (like the ones seen in
 * the puzzle selection screen).
 */

public class BottomSheetSpinnerDialog extends BottomSheetDialogFragment {

    private TextView mTitle;
    private ListView mList;
    private Context mContext;
    private BottomSheetSpinnerAdapter mAdapter;
    private AdapterView.OnItemClickListener mClickListener;

    public static BottomSheetSpinnerDialog newInstance() {
        return new BottomSheetSpinnerDialog();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_puzzle_spinner, container, false);

        mContext = getContext();
        mTitle = view.findViewById(R.id.title);
        mList = view.findViewById(R.id.list);

        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(mClickListener);

        return view;
    }

    public void setTitle(String title, @DrawableRes int iconRes) {
        mTitle.setText(title);
        mTitle.setCompoundDrawables(null, null, ContextCompat.getDrawable(mContext, iconRes), null);
    }

    public void setListAdapter(BottomSheetSpinnerAdapter adapter) {
        mAdapter = adapter;
    }

    public void setListClickListener(AdapterView.OnItemClickListener clickListener) {
        mClickListener = clickListener;
    }
}
