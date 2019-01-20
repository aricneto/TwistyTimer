package com.aricneto.twistytimer.fragment.dialog;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.ColorRes;
import androidx.fragment.app.DialogFragment;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.activity.MainActivity;
import com.aricneto.twistytimer.utils.Prefs;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by Ari on 09/02/2016.
 */
public class ThemeSelectDialog extends DialogFragment {

    private Unbinder mUnbinder;

    @BindView(R.id.indigo)       TextView indigo;
    @BindView(R.id.purple)       TextView purple;
    @BindView(R.id.teal)         TextView teal;
    @BindView(R.id.pink)         TextView pink;
    @BindView(R.id.red)          TextView red;
    @BindView(R.id.brown)        TextView brown;
    @BindView(R.id.blue)         TextView blue;
    @BindView(R.id.cyan)         TextView cyan;
    @BindView(R.id.light_blue)   TextView light_blue;
    @BindView(R.id.black)        TextView black;
    @BindView(R.id.green)        TextView green;
    @BindView(R.id.light_green)  TextView light_green;
    @BindView(R.id.orange)       TextView orange;
    @BindView(R.id.deepPurple)   TextView deepPurple;
    @BindView(R.id.blueGray)     TextView blueGray;

    public static ThemeSelectDialog newInstance() {
        return new ThemeSelectDialog();
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            final String oldTheme = Prefs.getString(R.string.pk_theme, "indigo");
            final String newTheme;

            switch (view.getId()) {
                case R.id.indigo:      newTheme = "indigo"; break;
                case R.id.purple:      newTheme = "purple"; break;
                case R.id.teal:        newTheme = "teal"; break;
                case R.id.pink:        newTheme = "pink"; break;
                case R.id.red:         newTheme = "red"; break;
                case R.id.brown:       newTheme = "brown"; break;
                case R.id.blue:        newTheme = "blue"; break;
                case R.id.cyan:        newTheme = "cyan"; break;
                case R.id.light_blue:  newTheme = "light_blue"; break;
                case R.id.black:       newTheme = "black"; break;
                case R.id.orange:      newTheme = "orange"; break;
                case R.id.green:       newTheme = "green"; break;
                case R.id.light_green: newTheme = "light_green"; break;
                case R.id.deepPurple:  newTheme = "deepPurple"; break;
                case R.id.blueGray:    newTheme = "blueGray"; break;
                default:               newTheme = oldTheme;
            }

            // If the theme has been changed, then the activity will need to be recreated. The
            // theme can only be applied properly during the inflation of the layouts, so it has
            // to go back to "Activity.onCreate()" to do that.
            if (!newTheme.equals(oldTheme)) {
                Prefs.edit().putString(R.string.pk_theme, newTheme).apply();

                ((MainActivity) getActivity()).onRecreateRequired();
            }

            dismiss();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View dialogView = inflater.inflate(R.layout.dialog_theme_select, container);
        mUnbinder = ButterKnife.bind(this, dialogView);

        setBlob(indigo,      R.color.md_indigo_500).setOnClickListener(clickListener);
        setBlob(purple,      R.color.md_purple_500).setOnClickListener(clickListener);
        setBlob(teal,        R.color.md_teal_500).setOnClickListener(clickListener);
        setBlob(pink,        R.color.md_pink_500).setOnClickListener(clickListener);
        setBlob(red,         R.color.md_red_500).setOnClickListener(clickListener);
        setBlob(brown,       R.color.md_brown_500).setOnClickListener(clickListener);
        setBlob(blue,        R.color.md_blue_500).setOnClickListener(clickListener);
        setBlob(cyan,        R.color.md_cyan_500).setOnClickListener(clickListener);
        setBlob(light_blue,  R.color.md_light_blue_500).setOnClickListener(clickListener);
        setBlob(black,       R.color.md_black_1000).setOnClickListener(clickListener);
        setBlob(orange,      R.color.md_deep_orange_500).setOnClickListener(clickListener);
        setBlob(green,       R.color.md_green_500).setOnClickListener(clickListener);
        setBlob(light_green, R.color.md_light_green_500).setOnClickListener(clickListener);
        setBlob(deepPurple,  R.color.md_deep_purple_500).setOnClickListener(clickListener);
        setBlob(blueGray,    R.color.md_blue_grey_500).setOnClickListener(clickListener);

        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return dialogView;
    }

    private View setBlob(TextView view, @ColorRes int colorRes) {
        Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.thumb_circle);
        Drawable wrap = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(wrap, ContextCompat.getColor(getContext(), colorRes));
        DrawableCompat.setTintMode(wrap, PorterDuff.Mode.SRC_IN);
        wrap = wrap.mutate();
        view.setCompoundDrawablesWithIntrinsicBounds(wrap, null, null, null);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }
}
