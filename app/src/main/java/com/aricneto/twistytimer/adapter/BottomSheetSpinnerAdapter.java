package com.aricneto.twistytimer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.aricneto.twistify.R;

import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

/**
 * Custom adapter for {@link com.aricneto.twistytimer.fragment.dialog.BottomSheetSpinnerDialog}
 * Populates ListView with simple text-icon pairs.
 */

public class BottomSheetSpinnerAdapter extends BaseAdapter {

    private Context mContext;
    private String[] mTitles;
    private int[] mIconRes;
    private int iconResLenght;

    public BottomSheetSpinnerAdapter(Context context, String[] titles, int[] iconRes) {
        this.mContext = context;
        this.mTitles = titles;
        this.mIconRes = iconRes;
        this.iconResLenght = mIconRes.length;
    }

    @Override
    public int getCount() {
        return mTitles.length;
    }

    @Override
    public Object getItem(int position) {
        return mTitles[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_bottom_spinner, parent, false);

        TextView titleView = view.findViewById(R.id.item);

        titleView.setText(mTitles[position]);
        if (iconResLenght > 0)
            titleView.setCompoundDrawablesWithIntrinsicBounds(mIconRes[position] != 0 ? ContextCompat.getDrawable(mContext, mIconRes[position]) : null, null, null, null);
        else
            titleView.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(mContext, R.drawable.ic_label), null, null, null);

        return view;
    }
}
