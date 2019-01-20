package com.aricneto.twistytimer.fragment.dialog;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.ColorInt;
import androidx.fragment.app.DialogFragment;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.aricneto.twistify.R;
import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.activity.MainActivity;
import com.aricneto.twistytimer.spans.ChromaDialogFixed;
import com.pavelsikun.vintagechroma.IndicatorMode;
import com.pavelsikun.vintagechroma.OnColorSelectedListener;
import com.pavelsikun.vintagechroma.colormode.ColorMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by Ari on 09/02/2016.
 */
public class SchemeSelectDialogMain extends DialogFragment {

    private Unbinder mUnbinder;

    @BindView(R.id.top)   View top;
    @BindView(R.id.left)  View left;
    @BindView(R.id.front) View front;
    @BindView(R.id.right) View right;
    @BindView(R.id.back)  View back;
    @BindView(R.id.down)  View down;
    @BindView(R.id.reset) TextView reset;
    @BindView(R.id.done)  TextView done;

    public static SchemeSelectDialogMain newInstance() {
        return new SchemeSelectDialogMain();
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(TwistyTimer.getAppContext());
            final SharedPreferences.Editor editor = sp.edit();
            String currentHex = "FFFFFF";
            switch (view.getId()) {
                case R.id.top:
                    currentHex = sp.getString("cubeTop", "FFFFFF");
                    break;
                case R.id.left:
                    currentHex = sp.getString("cubeLeft", "FF8B24");
                    break;
                case R.id.front:
                    currentHex = sp.getString("cubeFront", "02D040");
                    break;
                case R.id.right:
                    currentHex = sp.getString("cubeRight", "EC0000");
                    break;
                case R.id.back:
                    currentHex = sp.getString("cubeBack", "304FFE");
                    break;
                case R.id.down:
                    currentHex = sp.getString("cubeDown", "FDD835");
                    break;
            }

            new ChromaDialogFixed.Builder()
                    .initialColor(Color.parseColor("#" + currentHex))
                    .colorMode(ColorMode.RGB)
                    .indicatorMode(IndicatorMode.HEX)
                    .onColorSelected(new OnColorSelectedListener() {
                        @Override
                        public void onColorSelected(@ColorInt int color) {
                            String hexColor = Integer.toHexString(color).toUpperCase().substring(2);
                            switch (view.getId()) {
                                case R.id.top:
                                    setColor(top, Color.parseColor("#" + hexColor));
                                    editor.putString("cubeTop", hexColor);
                                    break;
                                case R.id.left:
                                    setColor(left, Color.parseColor("#" + hexColor));
                                    editor.putString("cubeLeft", hexColor);
                                    break;
                                case R.id.front:
                                    setColor(front, Color.parseColor("#" + hexColor));
                                    editor.putString("cubeFront", hexColor);
                                    break;
                                case R.id.right:
                                    setColor(right, Color.parseColor("#" + hexColor));
                                    editor.putString("cubeRight", hexColor);
                                    break;
                                case R.id.back:
                                    setColor(back, Color.parseColor("#" + hexColor));
                                    editor.putString("cubeBack", hexColor);
                                    break;
                                case R.id.down:
                                    setColor(down, Color.parseColor("#" + hexColor));
                                    editor.putString("cubeDown", hexColor);
                                    break;
                            }
                            editor.apply();
                        }
                    })
                    .create()
                    .show(getFragmentManager(), "ChromaDialog");

        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View dialogView = inflater.inflate(R.layout.dialog_scheme_select_main, container);
        mUnbinder = ButterKnife.bind(this, dialogView);

        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(TwistyTimer.getAppContext());

        setColor(top, Color.parseColor("#" + sp.getString("cubeTop", "FFFFFF")));
        setColor(left, Color.parseColor("#" + sp.getString("cubeLeft", "FF8B24")));
        setColor(front, Color.parseColor("#" + sp.getString("cubeFront", "02D040")));
        setColor(right, Color.parseColor("#" + sp.getString("cubeRight", "EC0000")));
        setColor(back, Color.parseColor("#" + sp.getString("cubeBack", "304FFE")));
        setColor(down, Color.parseColor("#" + sp.getString("cubeDown", "FDD835")));

        top.setOnClickListener(clickListener);
        left.setOnClickListener(clickListener);
        front.setOnClickListener(clickListener);
        right.setOnClickListener(clickListener);
        back.setOnClickListener(clickListener);
        down.setOnClickListener(clickListener);

        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new MaterialDialog.Builder(getContext())
                        .title(R.string.reset_colorscheme)
                        .positiveText(R.string.action_reset_colorscheme)
                        .negativeText(R.string.action_cancel)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(MaterialDialog dialog, DialogAction which) {
                                SharedPreferences.Editor editor = sp.edit();
                                editor.putString("cubeTop", "FFFFFF");
                                editor.putString("cubeLeft", "EF6C00");
                                editor.putString("cubeFront", "02D040");
                                editor.putString("cubeRight", "EC0000");
                                editor.putString("cubeBack", "304FFE");
                                editor.putString("cubeDown", "FDD835");
                                editor.apply();
                                setColor(top, Color.parseColor("#FFFFFF"));
                                setColor(left, Color.parseColor("#EF6C00"));
                                setColor(front, Color.parseColor("#02D040"));
                                setColor(right, Color.parseColor("#EC0000"));
                                setColor(back, Color.parseColor("#304FFE"));
                                setColor(down, Color.parseColor("#FDD835"));
                            }
                        })
                        .show();
            }
        });

        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).onRecreateRequired();
                }
                dismiss();
            }
        });

        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialogView;
    }

    private void setColor(View view, int color) {
        Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.square);
        Drawable wrap = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(wrap, color);
        DrawableCompat.setTintMode(wrap, PorterDuff.Mode.MULTIPLY);
        wrap = wrap.mutate();
        view.setBackground(wrap);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }
}
