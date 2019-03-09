package com.aricneto.twistytimer.fragment.dialog;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.SkuDetails;
import com.aricneto.twistify.BuildConfig;
import com.aricneto.twistify.R;
import com.aricneto.twistytimer.activity.MainActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class DonateDialog extends DialogFragment {

    @BindView(R.id.tier5) TextView tier5;
    @BindView(R.id.tier4) TextView tier4;
    @BindView(R.id.tier3) TextView tier3;
    @BindView(R.id.tier2) TextView tier2;
    @BindView(R.id.tier1) TextView tier1;

    private MainActivity activity;
    private Unbinder mUnbinder;
    private BillingProcessor bp;

    public static DonateDialog newInstance() {
        return new DonateDialog();
    }

    View.OnClickListener clickListener = v -> {
        switch (v.getId()) {
            case R.id.tier5:
                activity.purchase("donation_tier5");
                break;
            case R.id.tier4:
                activity.purchase("donation_tier4");
                break;
            case R.id.tier3:
                activity.purchase("donation_tier3");
                break;
            case R.id.tier2:
                activity.purchase("donation_tier2");
                break;
            case R.id.tier1:
                activity.purchase("donation_tier1");
                break;
        }
    };

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View dialogView = inflater.inflate(R.layout.content_donate, container);
        mUnbinder = ButterKnife.bind(this, dialogView);

        activity = ((MainActivity) getActivity());
        bp = activity.getBp();

        tier5.setOnClickListener(clickListener);
        tier4.setOnClickListener(clickListener);
        tier3.setOnClickListener(clickListener);
        tier2.setOnClickListener(clickListener);
        tier1.setOnClickListener(clickListener);

        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return dialogView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // App billing is only able to fetch these details on a signed release version,
        // so ignore it on debug versions
        if (!BuildConfig.DEBUG) {
            ArrayList<String> donationTiers = new ArrayList<>(
                    Arrays.asList("donation_tier1", "donation_tier2", "donation_tier3", "donation_tier4", "donation_tier5"));
            List<SkuDetails> tiers = bp.getPurchaseListingDetails(donationTiers);

            tier5.setText(tiers.get(4).currency + " " + String.valueOf(tiers.get(4).priceValue));
            tier4.setText(tiers.get(3).currency + " " + String.valueOf(tiers.get(3).priceValue));
            tier3.setText(tiers.get(2).currency + " " + String.valueOf(tiers.get(2).priceValue));
            tier2.setText(tiers.get(1).currency + " " + String.valueOf(tiers.get(1).priceValue));
            tier1.setText(tiers.get(0).currency + " " + String.valueOf(tiers.get(0).priceValue));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }

}
