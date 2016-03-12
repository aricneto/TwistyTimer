package com.aricneto.twistytimer.fragment.dialog;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.aricneto.twistify.R;
import com.aricneto.twistytimer.activity.MainActivity;
import com.pes.androidmaterialcolorpickerdialog.ColorPicker;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Ari on 09/02/2016.
 */
public class SchemeSelectDialogMain extends DialogFragment {


    @Bind(R.id.top)   View top;
    @Bind(R.id.left)  View left;
    @Bind(R.id.front) View front;
    @Bind(R.id.right) View right;
    @Bind(R.id.back)  View back;
    @Bind(R.id.down)  View down;
    @Bind(R.id.reset) TextView reset;
    @Bind(R.id.done)  TextView done;

    public static SchemeSelectDialogMain newInstance() {
        SchemeSelectDialogMain schemeSelectDialogMain = new SchemeSelectDialogMain();
        return schemeSelectDialogMain;
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
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


            final ColorPicker picker = new ColorPicker(getActivity(),
                    Integer.parseInt(currentHex.substring(0, 2), 16),
                    Integer.parseInt(currentHex.substring(2, 4), 16),
                    Integer.parseInt(currentHex.substring(4), 16));

            /* Show color picker dialog */
            picker.show();

    /* On Click listener for the dialog, when the user select the color */
            Button okColor = (Button) picker.findViewById(R.id.okColorButton);
            okColor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    String hexColor = Integer.toHexString(picker.getColor()).substring(2);
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
                    picker.dismiss();
                }
            });

        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View dialogView = inflater.inflate(R.layout.dialog_scheme_select_main, container);
        ButterKnife.bind(this, dialogView);

        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());

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
                Activity activity = getActivity();
                if (activity != null) {
                    activity.finish();
                    activity.startActivity(new Intent(activity, MainActivity.class));
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
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }
}
