package com.aricneto.twistytimer.activity;


import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.aricneto.twistify.R;
import com.jenzz.materialpreference.Preference;

import butterknife.Bind;
import butterknife.ButterKnife;

public class TimerAppearanceSettingsActivity extends AppCompatActivity {

    @Bind(R.id.actionbar) Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);
        mToolbar.setTitle(R.string.title_activity_settings);
        mToolbar.setTitleTextColor(Color.WHITE);
        mToolbar.setNavigationIcon(R.drawable.ic_action_arrow_back_white_24);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.main_activity_container, new TimerAppearanceSettingsFragment())
                    .addToBackStack("main")
                    .commit();
        }
    }

    public static class TimerAppearanceSettingsFragment extends PreferenceFragment {

        private SharedPreferences.Editor editor;
        private SharedPreferences        preferences;

        android.preference.Preference.OnPreferenceClickListener clickListener = new android.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(android.preference.Preference preference) {
                switch (preference.getKey()) {
                    case "timerTextSize":
                        createSeekTextSizeDialog("timerTextSize", 60, "12.34", true);
                        break;
                    case "scrambleImageSize":
                        createImageSeekDialog("scrambleImageSize", R.string.scrambleImageSize_text);
                        break;
                    case "scrambleTextSize":
                        createSeekTextSizeDialog("scrambleTextSize", 14, "R U R' U' R' F R2 U' R' U' R U R' F'", false);
                        break;
                    case "enableAdvanced":
                        if (preferences.getBoolean("enableAdvanced", false)) {
                            new MaterialDialog.Builder(getActivity())
                                    .title(R.string.warning)
                                    .content(R.string.advanced_pref_summary)
                                    .positiveText(R.string.action_ok)
                                    .show();
                        }
                        break;
                    case "timerTextOffset":
                        createSeekTextOffsetDialog();
                        break;
                }
                return false;
            }
        };

        @SuppressLint("CommitPrefEdits")
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.prefs_timer_appearance);

            preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            editor = preferences.edit();

            final Preference timerTextSize = (Preference) getPreferenceScreen().findPreference("timerTextSize");
            final Preference scrambleImageSize = (Preference) getPreferenceScreen().findPreference("scrambleImageSize");
            final Preference scrambleTextSize = (Preference) getPreferenceScreen().findPreference("scrambleTextSize");
            final Preference enableAdvanced = (Preference) getPreferenceScreen().findPreference("enableAdvanced");
            final Preference timerTextOffset = (Preference) getPreferenceScreen().findPreference("timerTextOffset");

            timerTextSize.setOnPreferenceClickListener(clickListener);
            scrambleImageSize.setOnPreferenceClickListener(clickListener);
            scrambleTextSize.setOnPreferenceClickListener(clickListener);
            enableAdvanced.setOnPreferenceClickListener(clickListener);
            timerTextOffset.setOnPreferenceClickListener(clickListener);

        }

        private void createSeekTextSizeDialog(final String setting, int defaultTextSize, String showText, boolean bold) {
            final View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_settings_progress, null);
            final AppCompatSeekBar seekBar = (AppCompatSeekBar) dialogView.findViewById(R.id.seekbar);
            final TextView text = (TextView) dialogView.findViewById(R.id.text);
            seekBar.setMax(300);
            seekBar.setProgress(preferences.getInt(setting, 100));

            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, defaultTextSize);
            final float defaultTextSizePx = text.getTextSize();
            text.setTextSize(TypedValue.COMPLEX_UNIT_PX, defaultTextSizePx * (seekBar.getProgress() / 100f));
            if (bold)
                text.setTypeface(Typeface.DEFAULT_BOLD);
            text.setText(showText);

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    text.setTextSize(TypedValue.COMPLEX_UNIT_PX, defaultTextSizePx * (i / 100f));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            new MaterialDialog.Builder(getActivity())
                    .customView(dialogView, true)
                    .positiveText(R.string.action_done)
                    .negativeText(R.string.action_cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            int seekProgress = seekBar.getProgress();
                            editor.putInt(setting, seekProgress > 10 ? seekProgress : 10);
                            editor.apply();
                        }
                    })
                    .neutralText(R.string.action_default)
                    .onNeutral(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            editor.putInt(setting, 100);
                            editor.apply();
                        }
                    })
                    .show();
        }

        private void createSeekTextOffsetDialog() {
            final View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_settings_progress, null);
            final AppCompatSeekBar seekBar = (AppCompatSeekBar) dialogView.findViewById(R.id.seekbar);
            final TextView text = (TextView) dialogView.findViewById(R.id.text);
            seekBar.setMax(500);
            seekBar.setProgress(preferences.getInt("timerTextOffset", 0) + 250);

            final float defaultY = text.getY();

            text.setY(defaultY - (seekBar.getProgress() - 250));
            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 60);
            text.setTypeface(Typeface.DEFAULT_BOLD);
            text.setText("12.34");

            text.getLayoutParams().height += 650;

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    text.setY(defaultY - (i - 250));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            new MaterialDialog.Builder(getActivity())
                    .customView(dialogView, true)
                    .positiveText(R.string.action_done)
                    .negativeText(R.string.action_cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            int seekProgress = seekBar.getProgress();
                            editor.putInt("timerTextOffset", seekProgress - 250);
                            editor.apply();
                        }
                    })
                    .neutralText(R.string.action_default)
                    .onNeutral(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            editor.putInt("timerTextOffset", 0);
                            editor.apply();
                        }
                    })
                    .show();
        }

        private void createImageSeekDialog(final String setting, @StringRes int title) {
            final View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_settings_progress_image, null);
            final AppCompatSeekBar seekBar = (AppCompatSeekBar) dialogView.findViewById(R.id.seekbar);
            final View image = dialogView.findViewById(R.id.image);
            seekBar.setMax(300);
            seekBar.setProgress(preferences.getInt(setting, 100));

            final int defaultWidth = image.getLayoutParams().width;
            final int defaultHeight = image.getLayoutParams().height;

            image.getLayoutParams().width *= (seekBar.getProgress() / 100f);
            image.getLayoutParams().height *= (seekBar.getProgress() / 100f);


            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) image.getLayoutParams();
                    params.width = (int) (defaultWidth * (i / 100f));
                    params.height = (int) (defaultHeight * (i / 100f));
                    image.setLayoutParams(params);

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            new MaterialDialog.Builder(getActivity())
                    .customView(dialogView, true)
                    .positiveText(R.string.action_done)
                    .negativeText(R.string.action_cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            int seekProgress = seekBar.getProgress();
                            editor.putInt(setting, seekProgress > 10 ? seekProgress : 10);
                            editor.apply();
                        }
                    })
                    .neutralText(R.string.action_default)
                    .onNeutral(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            editor.putInt(setting, 100);
                            editor.apply();
                        }
                    })
                    .show();
        }
    }

}