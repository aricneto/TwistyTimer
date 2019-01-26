package com.aricneto.twistytimer.fragment.dialog;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.items.Theme;
import com.aricneto.twistytimer.utils.Prefs;
import com.aricneto.twistytimer.utils.TTIntent;
import com.aricneto.twistytimer.utils.ThemeUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static com.aricneto.twistytimer.utils.TTIntent.ACTION_CHANGED_THEME;
import static com.aricneto.twistytimer.utils.TTIntent.CATEGORY_UI_INTERACTIONS;

/**
 * Created by Ari on 09/02/2016.
 */
public class ThemeSelectDialog extends BottomSheetDialogFragment {

    private Unbinder mUnbinder;

    @BindView(R.id.list)
    RecyclerView recyclerView;

    public static ThemeSelectDialog newInstance() {
        return new ThemeSelectDialog();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View dialogView = inflater.inflate(R.layout.dialog_bottomsheet_recycler, container);
        mUnbinder = ButterKnife.bind(this, dialogView);

        recyclerView.setHasFixedSize(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        recyclerView.setLayoutManager(layoutManager);

        ThemeListAdapter recyclerAdapter = new ThemeListAdapter(ThemeUtils.getAllThemes(), getContext());
        recyclerView.setAdapter(recyclerAdapter);

        return dialogView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }
}

class ThemeListAdapter extends RecyclerView.Adapter<ThemeListAdapter.CardViewHolder> {

    private Theme[] themeSet;
    private Context mContext;

    String currentTheme = Prefs.getString(R.string.pk_theme, "indigo");

    static class CardViewHolder extends RecyclerView.ViewHolder {
        View view;
        View themeCard;
        TextView themeTitle;

        public CardViewHolder(View view) {
            super(view);
            this.view = view;
            this.themeCard = view.findViewById(R.id.card);
            this.themeTitle = view.findViewById(R.id.title);
        }
    }

    ThemeListAdapter(Theme[] themeSet, Context context) {
        this.themeSet = themeSet;
        this.mContext = context;
    }

    @Override
    public ThemeListAdapter.CardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_theme_select_card, parent, false);

        CardViewHolder viewHolder = new CardViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(CardViewHolder holder, int position) {
        // Create gradient drawable
        GradientDrawable gradientDrawable = ThemeUtils.fetchBackgroundGradient(mContext, themeSet[position].getResId());
        gradientDrawable.setCornerRadius(18f);
        gradientDrawable.setStroke(4, Color.BLACK);

        // Set card title and background
        holder.themeTitle.setText(themeSet[position].getName());
        holder.themeCard.setBackground(gradientDrawable);

        // Create onClickListener
        holder.themeCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newTheme;

                newTheme = themeSet[position].getPrefName();

                if (!newTheme.equals(currentTheme)) {
                    Prefs.edit().putString(R.string.pk_theme, newTheme).apply();

                    TTIntent.broadcast(CATEGORY_UI_INTERACTIONS, ACTION_CHANGED_THEME);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return themeSet.length;
    }
}

