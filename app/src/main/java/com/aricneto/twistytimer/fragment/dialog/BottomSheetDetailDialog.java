package com.aricneto.twistytimer.fragment.dialog;

import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.aricneto.twistify.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public class BottomSheetDetailDialog extends BottomSheetDialogFragment {

    private TextView detailTextView;

    private TextView hintTextView;
    private View hintDividerView;
    private View hintTitleView;
    private View hintProgress;

    private boolean hasHints = false;

    private String detailText;
    private String hintText;

    public static BottomSheetDetailDialog newInstance() {
        return new BottomSheetDetailDialog();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_bottomsheet_detail, container, false);

        detailTextView = view.findViewById(R.id.detail_text);
        hintTextView = view.findViewById(R.id.hint_text);
        hintDividerView = view.findViewById(R.id.hint_divider);
        hintTitleView = view.findViewById(R.id.hint_title);
        hintProgress = view.findViewById(R.id.hint_progress);

        detailTextView.setText(detailText);

        if (!hasHints) {
            setHintVisibility(999);
        } else {
            setHintVisibility(View.GONE);
        }

        return view;
    }


    public void setDetailText(String text) {
        detailText = text;
    }

    public void setHintText(String text) {
        hintText = text;
        if (hintTextView != null) {
            hintTextView.setText(hintText);
        }
    }

    public void hasHints(boolean hasHints) {
        this.hasHints = hasHints;
    }

    public void setHintVisibility(int visibility) {
        if (hintTextView != null) {
            if (visibility == View.VISIBLE) {
                hintTextView.setVisibility(View.VISIBLE);
                hintProgress.setVisibility(View.GONE);
                hintTitleView.setVisibility(View.VISIBLE);
                hintDividerView.setVisibility(View.VISIBLE);
            } else if (visibility == View.GONE) {
                hintTextView.setVisibility(View.GONE);
                hintProgress.setVisibility(View.VISIBLE);
                hintTitleView.setVisibility(View.VISIBLE);
                hintDividerView.setVisibility(View.VISIBLE);
            } else {
                hintProgress.setVisibility(View.GONE);
                hintTextView.setVisibility(View.GONE);
                hintTitleView.setVisibility(View.GONE);
                hintDividerView.setVisibility(View.GONE);
            }
        }
    }

}
