package com.aricneto.twistytimer.fragment.dialog;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.ColorRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.activity.MainActivity;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Ari on 09/02/2016.
 */
public class ThemeSelectDialog extends DialogFragment {


    @Bind(R.id.indigo)     TextView indigo;
    @Bind(R.id.purple)     TextView purple;
    @Bind(R.id.teal)       TextView teal;
    @Bind(R.id.pink)       TextView pink;
    @Bind(R.id.red)        TextView red;
    @Bind(R.id.brown)      TextView brown;
    @Bind(R.id.blue)       TextView blue;
    @Bind(R.id.black)      TextView black;
    @Bind(R.id.green)      TextView green;
    @Bind(R.id.orange)     TextView orange;
    @Bind(R.id.deepPurple) TextView deepPurple;
    @Bind(R.id.blueGray)   TextView blueGray;

    public static ThemeSelectDialog newInstance() {
        ThemeSelectDialog themeSelectDialog = new ThemeSelectDialog();
        return themeSelectDialog;
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            SharedPreferences.Editor editor = sharedPreferences.edit();

            switch (view.getId()) {
                case R.id.indigo:
                    editor.putString("theme", "indigo");
                    break;
                case R.id.purple:
                    editor.putString("theme", "purple");
                    break;
                case R.id.teal:
                    editor.putString("theme", "teal");
                    break;
                case R.id.pink:
                    editor.putString("theme", "pink");
                    break;
                case R.id.red:
                    editor.putString("theme", "red");
                    break;
                case R.id.brown:
                    editor.putString("theme", "brown");
                    break;
                case R.id.blue:
                    editor.putString("theme", "blue");
                    break;
                case R.id.black:
                    editor.putString("theme", "black");
                    break;
                case R.id.orange:
                    editor.putString("theme", "orange");
                    break;
                case R.id.green:
                    editor.putString("theme", "green");
                    break;
                case R.id.deepPurple:
                    editor.putString("theme", "deepPurple");
                    break;
                case R.id.blueGray:
                    editor.putString("theme", "blueGray");
                    break;
            }

            editor.apply();
            dismiss();

            Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
                activity.startActivity(new Intent(activity, MainActivity.class));
            }

        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View dialogView = inflater.inflate(R.layout.dialog_theme_select, container);
        ButterKnife.bind(this, dialogView);

        setBlob(indigo, R.color.md_indigo_500);
        setBlob(purple, R.color.md_purple_500);
        setBlob(teal, R.color.md_teal_500);
        setBlob(pink, R.color.md_pink_500);
        setBlob(red, R.color.md_red_500);
        setBlob(brown, R.color.md_brown_500);
        setBlob(blue, R.color.md_blue_500);
        setBlob(black, R.color.md_black_1000);
        setBlob(orange, R.color.md_deep_orange_500);
        setBlob(green, R.color.md_green_500);
        setBlob(deepPurple, R.color.md_deep_purple_500);
        setBlob(blueGray, R.color.md_blue_grey_500);

        indigo.setOnClickListener(clickListener);
        purple.setOnClickListener(clickListener);
        teal.setOnClickListener(clickListener);
        pink.setOnClickListener(clickListener);
        red.setOnClickListener(clickListener);
        brown.setOnClickListener(clickListener);
        blue.setOnClickListener(clickListener);
        black.setOnClickListener(clickListener);
        orange.setOnClickListener(clickListener);
        green.setOnClickListener(clickListener);
        deepPurple.setOnClickListener(clickListener);
        blueGray.setOnClickListener(clickListener);

        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialogView;
    }

    private void setBlob(TextView view, @ColorRes int colorRes) {
        Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.thumb_circle);
        Drawable wrap = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(wrap, ContextCompat.getColor(getContext(), colorRes));
        DrawableCompat.setTintMode(wrap, PorterDuff.Mode.SRC_IN);
        wrap = wrap.mutate();
        view.setCompoundDrawablesWithIntrinsicBounds(wrap, null, null, null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }
}
