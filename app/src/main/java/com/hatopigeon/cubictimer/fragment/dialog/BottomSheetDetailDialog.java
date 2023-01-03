package com.hatopigeon.cubictimer.fragment.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.hatopigeon.cubicify.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class BottomSheetDetailDialog extends BottomSheetDialogFragment {

    @BindView(R.id.detail_text) TextView detailTextView;
    @BindView(R.id.hint_text) TextView hintTextView;
    @BindView(R.id.hint_divider) View hintDividerView;
    @BindView(R.id.hint_title) View hintTitleView;
    @BindView(R.id.hint_progress) View hintProgress;

    private boolean hasHints = false;

    private String detailText;
    private String hintText;
    private Unbinder mUnbinder;
    private float detailTextSize;

    public static BottomSheetDetailDialog newInstance() {
        return new BottomSheetDetailDialog();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        // This makes the bottomSheet dialog start in the expanded state
        dialog.setOnShowListener(dia -> {
            BottomSheetDialog bottomDialog = (BottomSheetDialog) dia;
            FrameLayout bottomSheet =  bottomDialog .findViewById(com.google.android.material.R.id.design_bottom_sheet);
            BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
            BottomSheetBehavior.from(bottomSheet).setSkipCollapsed(true);
            BottomSheetBehavior.from(bottomSheet).setHideable(true);
        });

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_bottomsheet_detail, container, false);
        mUnbinder = ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        detailTextView.setText(detailText);
        detailTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, detailTextView.getTextSize() * detailTextSize);

        if (!hasHints) {
            setHintVisibility(999);
        } else {
            setHintVisibility(View.GONE);
        }
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

    public void setDetailTextSize (float size) {
        this.detailTextSize = size;
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }

}
