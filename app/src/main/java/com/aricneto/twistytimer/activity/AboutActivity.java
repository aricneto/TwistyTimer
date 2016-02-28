package com.aricneto.twistytimer.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.aricneto.twistify.R;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.psdev.licensesdialog.LicensesDialog;

public class AboutActivity extends AppCompatActivity {

    private final static String APP_TITLE = "Twisty Timer";
    private final static String APP_PNAME = "com.aricneto.twistytimer";

    @Bind(R.id.toolbar)        Toolbar  toolbar;
    @Bind(R.id.rateButton)     TextView rateButton;
    @Bind(R.id.feedbackButton) TextView feedbackButton;
    @Bind(R.id.licenseButton)  TextView licenseButton;
    @Bind(R.id.testersButton)  TextView testersButton;
    @Bind(R.id.appVersion)     TextView appVersion;

    Activity activity;


    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.feedbackButton:
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + "aricnetodev@gmail.com"));
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback");
                    startActivity(Intent.createChooser(emailIntent, getString(R.string.send_email_title)));
                    break;
                case R.id.licenseButton:
                    new LicensesDialog.Builder(activity)
                            .setNotices(R.raw.notices_app)
                            .build()
                            .show();
                    break;
                case R.id.rateButton:
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + APP_PNAME)));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case R.id.testersButton:
                    new MaterialDialog.Builder(activity)
                            .title(R.string.testers)
                            .content(R.string.testers_content)
                            .positiveText(R.string.action_ok)
                            .show();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        ButterKnife.bind(this);
        toolbar.setTitle(R.string.title_activity_about);
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setNavigationIcon(R.drawable.ic_action_arrow_back_white_24);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        activity = this;

        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            appVersion.setText(versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        feedbackButton.setOnClickListener(clickListener);
        licenseButton.setOnClickListener(clickListener);
        rateButton.setOnClickListener(clickListener);
        testersButton.setOnClickListener(clickListener);

    }
}
