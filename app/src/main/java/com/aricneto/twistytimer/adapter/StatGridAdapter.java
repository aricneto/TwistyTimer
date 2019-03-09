package com.aricneto.twistytimer.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.items.Stat;
import com.aricneto.twistytimer.utils.ThemeUtils;
import com.github.mikephil.charting.utils.Utils;

import java.util.ArrayList;

/**
 * Created by Ari Neto on 19-Aug-17.
 *
 * An adapter that's used to fill the Statistics card in {@link com.aricneto.twistytimer.fragment.TimerGraphFragment}
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
        TextView textView = new TextView(mContext, null, R.style.StatTextStyle);

        // alternate colors between rows to make viewing easier
        if (getItemId(position) % 2 == 0)
            textView.setBackgroundColor(ThemeUtils.fetchAttrColor(mContext, R.attr.graph_stats_card_background_alt));
        else
            textView.setBackgroundColor(ThemeUtils.fetchAttrColor(mContext, R.attr.graph_stats_card_background));


        textView.setTextColor(ThemeUtils.fetchAttrColor(mContext, R.attr.graph_stats_card_text_color));
        textView.setGravity(Gravity.RIGHT);
        textView.setSingleLine(true);
        textView.setMaxLines(1);

        // IMPORTANT: keep padding in sync with StatTextStyle so layout doesn't get out of order
        textView.setPadding(0, (int) Utils.convertDpToPixel(3f), (int) Utils.convertDpToPixel(8f), (int) Utils.convertDpToPixel(3f));
        textView.setText(mStats.get(position).getTime());

        return textView;
    }

}
