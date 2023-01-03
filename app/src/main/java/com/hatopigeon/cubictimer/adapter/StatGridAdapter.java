package com.hatopigeon.cubictimer.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.hatopigeon.cubicify.R;
import com.hatopigeon.cubictimer.items.Stat;
import com.hatopigeon.cubictimer.utils.ThemeUtils;
import com.github.mikephil.charting.utils.Utils;

import java.util.ArrayList;

import androidx.core.content.res.ResourcesCompat;

/**
 * Created by Ari Neto on 19-Aug-17.
 *
 * An adapter that's used to fill the Statistics card in {@link com.hatopigeon.cubictimer.fragment.TimerGraphFragment}
 */

public class StatGridAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<Stat> mStats;

    public StatGridAdapter(Context mContext, ArrayList<Stat> mStats) {
        this.mContext = mContext;
        this.mStats = mStats;
    }

    @Override
    public int getCount() {
        return mStats.size();
    }

    @Override
    public Object getItem(int index) {
        return mStats.get(index);
    }

    @Override
    public long getItemId(int i) {
        return mStats.get(i).getRow();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView textView = new TextView(mContext, null, R.attr.statTextStyle);

        // alternate colors between rows to make viewing easier
        if (getItemId(position) % 2 == 0)
            textView.setBackgroundColor(ThemeUtils.fetchAttrColor(mContext, R.attr.graph_stats_card_background_alt));
        else
            textView.setBackgroundColor(ThemeUtils.fetchAttrColor(mContext, R.attr.graph_stats_card_background));

        //textView.setTypeface(ResourcesCompat.getFont(mContext, R.font.quicksand));
        textView.setText(mStats.get(position).getTime());

        return textView;
    }

}
