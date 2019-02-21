package com.aricneto.twistytimer.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.aricneto.twistify.R;

import androidx.core.content.ContextCompat;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

/**
 * Custom adapter for {@link com.aricneto.twistytimer.fragment.dialog.BottomSheetSpinnerDialog}
 * Populates ListView with simple text-icon pairs.
 */

public class BottomSheetSpinnerAdapter extends BaseAdapter {

    private Context  mContext;
    private String[] mTitles;
    private int[]    mIconRes;
    private int      iconResLenght;

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
        Drawable icon;

        titleView.setText(mTitles[position]);

        if (iconResLenght > 0) {
            if (mIconRes[position] != 0) {
                try {
                    icon = VectorDrawableCompat.create(mContext.getResources(), mIconRes[position], null);
                    titleView.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
                } catch (Exception e) {
                    Log.e("BottomSheetSpinner", "Error populating list!: " + e);
                }
            }
        } else {
            icon = VectorDrawableCompat.create(mContext.getResources(), R.drawable.ic_label, null);
            if (icon != null)
                icon.setAlpha(90);
            titleView.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        }

        return view;
    }
}
