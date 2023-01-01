package com.hatopigeon.cubictimer.adapter;

import android.content.Context;
import androidx.core.content.ContextCompat;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.hatopigeon.cubicify.R;

import java.util.ArrayList;
import java.util.List;

public class SpinnerAdapter extends BaseAdapter {
    private final Context mContext;
    private List<Pair<String, String>> mItems = new ArrayList<>();

    public SpinnerAdapter(Context context) {
        this.mContext = context;
    }

    public void clear() {
        mItems.clear();
    }

    public void addItem(String title, String subtitle) {
        mItems.add(Pair.create(title, subtitle));
    }

    public void updateItemSubtitle(int position, String subtitle) {
        mItems.set(position, new Pair<>(mItems.get(position).first, subtitle));
    }

    public void addItems(List<Pair<String, String>> items) {
        mItems.addAll(items);
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getDropDownView(int position, View view, ViewGroup parent) {
        if (view == null || ! view.getTag().toString().equals("DROPDOWN")) {
            view = LayoutInflater.from(mContext).inflate(R.layout.toolbar_spinner_item_dropdown, parent, false);
            view.setTag("DROPDOWN");
        }

        TextView titleView = (TextView) view.findViewById(android.R.id.text1);

        titleView.setText(getTitle(position));


        return view;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null || ! view.getTag().toString().equals("NON_DROPDOWN")) {
            view = LayoutInflater.from(mContext).inflate(R.layout.
                    toolbar_spinner_item_actionbar, parent, false);
            view.setTag("NON_DROPDOWN");
        }
        TextView titleView = (TextView) view.findViewById(android.R.id.text1);

        titleView.setText(getTitle(position));
        titleView.setCompoundDrawablesWithIntrinsicBounds(null, null,
                ContextCompat.getDrawable(mContext, R.drawable.ic_action_arrow_drop_down_white_24), null);


        return view;
    }

    private String getTitle(int position) {
        return position >= 0 && position < mItems.size() ? mItems.get(position).first : "";
    }

    private String getSubtitle(int position) {
        return position >= 0 && position < mItems.size() ? mItems.get(position).second : "";
    }
}
