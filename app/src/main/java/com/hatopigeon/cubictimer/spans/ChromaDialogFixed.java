package com.hatopigeon.cubictimer.spans;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.ColorInt;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;

import android.util.TypedValue;
import android.view.WindowManager;

import com.hatopigeon.cubicify.R;
import com.pavelsikun.vintagechroma.IndicatorMode;
import com.pavelsikun.vintagechroma.OnColorSelectedListener;
import com.pavelsikun.vintagechroma.colormode.ColorMode;
import com.pavelsikun.vintagechroma.view.ChromaView;

/**
 *  The library's default dialog is broken and doesn't work on landscape mode
 *  This class fixes this issue.
 *
 *  Remove this when the library is updated
 */
public class ChromaDialogFixed extends DialogFragment {

    private final static String ARG_INITIAL_COLOR = "arg_initial_color";
    private final static String ARG_COLOR_MODE_ID = "arg_color_mode_id";
    private final static String ARG_INDICATOR_MODE = "arg_indicator_mode";

    private OnColorSelectedListener listener;
    private ChromaView chromaView;

    private static ChromaDialogFixed newInstance(@ColorInt int initialColor, ColorMode colorMode, IndicatorMode indicatorMode) {
        ChromaDialogFixed fragment = new ChromaDialogFixed();
        fragment.setArguments(makeArgs(initialColor, colorMode, indicatorMode));
        return fragment;
    }

    private static Bundle makeArgs(@ColorInt int initialColor, ColorMode colorMode, IndicatorMode indicatorMode) {
        Bundle args = new Bundle();
        args.putInt(ARG_INITIAL_COLOR, initialColor);
        args.putInt(ARG_COLOR_MODE_ID, colorMode.ordinal());
        args.putInt(ARG_INDICATOR_MODE, indicatorMode.ordinal());
        return args;
    }

    public static class Builder {
        private
        @ColorInt
        int initialColor = ChromaView.DEFAULT_COLOR;
        private ColorMode colorMode = ChromaView.DEFAULT_MODE;
        private IndicatorMode indicatorMode = IndicatorMode.DECIMAL;
        private OnColorSelectedListener listener = null;

        public Builder initialColor(@ColorInt int initialColor) {
            this.initialColor = initialColor;
            return this;
        }

        public Builder colorMode(ColorMode colorMode) {
            this.colorMode = colorMode;
            return this;
        }

        public Builder indicatorMode(IndicatorMode indicatorMode) {
            this.indicatorMode = indicatorMode;
            return this;
        }

        public Builder onColorSelected(OnColorSelectedListener listener) {
            this.listener = listener;
            return this;
        }

        public ChromaDialogFixed create() {
            ChromaDialogFixed fragment = newInstance(initialColor, colorMode, indicatorMode);
            fragment.setListener(listener);
            return fragment;
        }
    }

    public void setListener(OnColorSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        if (savedInstanceState == null) {
            chromaView = new ChromaView(
                    getArguments().getInt(ARG_INITIAL_COLOR),

                    ColorMode.values()[
                            getArguments().getInt(ARG_COLOR_MODE_ID)],

                    IndicatorMode.values()[
                            getArguments().getInt(ARG_INDICATOR_MODE)],

                    getActivity());
        } else {
            chromaView = new ChromaView(

                    savedInstanceState.getInt(ARG_INITIAL_COLOR, ChromaView.DEFAULT_COLOR),

                    ColorMode.values()[
                            savedInstanceState.getInt(ARG_COLOR_MODE_ID)],

                    IndicatorMode.values()[
                            savedInstanceState.getInt(ARG_INDICATOR_MODE)],

                    getActivity());
        }

        chromaView.enableButtonBar(new ChromaView.ButtonBarListener() {
            @Override
            public void onPositiveButtonClick(int color) {
                if (listener != null) {
                    listener.onColorSelected(color);
                }
                dismiss();
            }

            @Override
            public void onNegativeButtonClick() {
                dismiss();
            }
        });

        final AlertDialog ad = new AlertDialog.Builder(getActivity(), getTheme()).setView(chromaView).create();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            ad.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    measureLayout(ad);
                }
            });
        }

        return ad;
    }

    private void measureLayout(AlertDialog ad) {
        int widthMultiplier = getResources().getConfiguration()
                .orientation == Configuration.ORIENTATION_LANDSCAPE
                ? 2
                : 1;

        TypedValue typedValue = new TypedValue();
        getResources().getValue(R.dimen.chroma_dialog_height_multiplier, typedValue, true);
        float heightMultiplier = typedValue.getFloat();

        int height = getResources().getConfiguration()
                .orientation == Configuration.ORIENTATION_LANDSCAPE
                ? (int) (ad.getContext().getResources().getDisplayMetrics().heightPixels * heightMultiplier)
                : WindowManager.LayoutParams.WRAP_CONTENT;

        int width = getResources().getDimensionPixelSize(R.dimen.chroma_dialog_width) * widthMultiplier;

        ad.getWindow().setLayout(width, height);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putAll(makeArgs(chromaView.getCurrentColor(), chromaView.getColorMode(), chromaView.getIndicatorMode()));
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listener = null;
    }
}