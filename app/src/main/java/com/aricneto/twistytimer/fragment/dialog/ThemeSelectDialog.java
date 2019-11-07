package com.aricneto.twistytimer.fragment.dialog;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.items.TextStyle;
import com.aricneto.twistytimer.items.Theme;
import com.aricneto.twistytimer.utils.Prefs;
import com.aricneto.twistytimer.utils.TTIntent;
import com.aricneto.twistytimer.utils.ThemeUtils;

import androidx.annotation.StyleRes;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.LinkedHashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static com.aricneto.twistytimer.utils.TTIntent.ACTION_CHANGED_THEME;
import static com.aricneto.twistytimer.utils.TTIntent.CATEGORY_UI_INTERACTIONS;

/**
 * Created by Ari on 09/02/2016.
 */
public class ThemeSelectDialog extends DialogFragment {

    private Unbinder mUnbinder;
    private Context mContext;

    @BindView(R.id.list)
    RecyclerView themeRecycler;

    @BindView(R.id.list2)
    RecyclerView textStyleRecycler;

    public static ThemeSelectDialog newInstance() {
        return new ThemeSelectDialog();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View dialogView = inflater.inflate(R.layout.dialog_theme_select, container);
        mUnbinder = ButterKnife.bind(this, dialogView);

        mContext = getContext();

        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        themeRecycler.setHasFixedSize(true);
        textStyleRecycler.setHasFixedSize(true);


        GridLayoutManager themeLayoutManager = new GridLayoutManager(mContext, 2, GridLayoutManager.HORIZONTAL, false);
        GridLayoutManager textLayoutManager = new GridLayoutManager(mContext, 2, GridLayoutManager.HORIZONTAL, false);

        themeRecycler.setLayoutManager(themeLayoutManager);
        textStyleRecycler.setLayoutManager(textLayoutManager);

        ThemeListAdapter themeListAdapter = new ThemeListAdapter(ThemeUtils.getAllThemesHashmap(), mContext);
        TextStyleListAdapter textStyleListAdapter = new TextStyleListAdapter(ThemeUtils.getAllTextStylesHashMap(), mContext);
        themeRecycler.setAdapter(themeListAdapter);
        textStyleRecycler.setAdapter(textStyleListAdapter);

        int cornerRadius = ThemeUtils.dpToPix(mContext, 20);

        // Set Text Style selector background
        GradientDrawable gradientDrawable = ThemeUtils.fetchBackgroundGradient(mContext, ThemeUtils.getPreferredTheme());
        gradientDrawable.setCornerRadii(new float[] {0, 0, 0, 0, cornerRadius, cornerRadius, cornerRadius, cornerRadius});

        textStyleRecycler.setBackground(gradientDrawable);

        return dialogView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }
}

class ThemeListAdapter extends RecyclerView.Adapter<ThemeListAdapter.CardViewHolder> {

    private LinkedHashMap<String, Theme> themeHash;
    private String[]                     themeNames;
    private Context                      mContext;
    private int                          cornerRadius;
    private int                          strokeWidth;

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

    ThemeListAdapter(LinkedHashMap<String, Theme> themeHash, Context context) {
        this.themeHash = themeHash;
        this.themeNames = themeHash.keySet().toArray(new String[0]);
        this.mContext = context;
        this.cornerRadius = ThemeUtils.dpToPix(context, 8);
        this.strokeWidth = ThemeUtils.dpToPix(context, 1);
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
        GradientDrawable gradientDrawable = ThemeUtils.fetchBackgroundGradient(mContext, themeHash.get(themeNames[position]));
        gradientDrawable.setCornerRadius(cornerRadius);
        gradientDrawable.setStroke(strokeWidth, Color.BLACK);

        // Set card title and background
        holder.themeTitle.setText(themeHash.get(themeNames[position]).getName());
        holder.themeCard.setBackground(gradientDrawable);

        if (themeNames[position].equals(currentTheme)) {
            holder.themeTitle.setBackgroundResource(R.drawable.outline_background_card_warn);
        } else {
            holder.themeTitle.setBackground(null);
        }

        // Create onClickListener
        holder.themeCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newTheme;

                newTheme = themeNames[position];

                if (!newTheme.equals(currentTheme)) {
                    Prefs.edit().putString(R.string.pk_theme, newTheme).apply();
                    // Reset text style
                    Prefs.edit().putString(R.string.pk_text_style, "default").apply();

                    TTIntent.broadcast(CATEGORY_UI_INTERACTIONS, ACTION_CHANGED_THEME);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return themeHash.size();
    }
}

class TextStyleListAdapter extends RecyclerView.Adapter<TextStyleListAdapter.CardViewHolder> {

    private final int cornerRadius;
    private final int strokeWidth;
    private LinkedHashMap<String, TextStyle> textStyleHashMap;
    private String[] textStyleNames;
    private Context mContext;

    private String currentTextStyle = Prefs.getString(R.string.pk_text_style, "default");
    private TextStyle currentTheme = ThemeUtils.getPreferredTextStyle();
    int colorTimerText;

    static class CardViewHolder extends RecyclerView.ViewHolder {
        View view;
        TextView themeCard;
        TextView themeTitle;

        public CardViewHolder(View view) {
            super(view);
            this.view = view;
            this.themeCard = view.findViewById(R.id.card);
            this.themeTitle = view.findViewById(R.id.title);
        }
    }

    TextStyleListAdapter(LinkedHashMap<String, TextStyle> textStyleHashMap, Context context) {
        this.textStyleHashMap = textStyleHashMap;
        this.textStyleNames = textStyleHashMap.keySet().toArray(new String[0]);
        this.mContext = context;
        colorTimerText = ThemeUtils.fetchAttrColor(mContext, R.attr.colorTimerText);
        this.cornerRadius = ThemeUtils.dpToPix(context, 8);
        this.strokeWidth = ThemeUtils.dpToPix(context, 1);
    }

    @Override
    public TextStyleListAdapter.CardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_text_style_card, parent, false);

        CardViewHolder viewHolder = new CardViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(CardViewHolder holder, int position) {
        // Create gradient drawable
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(Color.TRANSPARENT);
        gradientDrawable.setCornerRadius(cornerRadius);
        gradientDrawable.setStroke(strokeWidth, colorTimerText);

        // Set card title and background
        holder.themeTitle.setText(textStyleHashMap.get(textStyleNames[position]).getName());
        holder.themeCard.setBackground(gradientDrawable);
        holder.themeCard.setTextColor(ThemeUtils.fetchStyleableAttr(mContext, textStyleHashMap.get(textStyleNames[position]).getStyleRes(),
                                                                    R.styleable.BaseTwistyTheme,
                                                                    R.styleable.BaseTwistyTheme_colorTimerText,
                                                                    R.attr.colorTimerText));

        if (textStyleNames[position].equals(currentTextStyle)) {
            holder.themeTitle.setBackgroundResource(R.drawable.outline_background_card_warn);
            holder.themeTitle.setTextColor(Color.BLACK);
        } else {
            holder.themeTitle.setBackground(null);
            holder.themeTitle.setTextColor(colorTimerText);
        }

        // Create onClickListener
        holder.themeCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newTheme;

                newTheme = textStyleNames[position];

                if (!newTheme.equals(currentTextStyle)) {
                    Prefs.edit().putString(R.string.pk_text_style, newTheme).apply();

                    TTIntent.broadcast(CATEGORY_UI_INTERACTIONS, ACTION_CHANGED_THEME);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return textStyleHashMap.size();
    }
}

