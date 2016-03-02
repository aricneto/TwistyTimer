package com.aricneto.twistytimer.activity;


import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.aricneto.twistify.R;
import com.jenzz.materialpreference.Preference;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

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
                        createTextSizeDialog("timerTextSize");
                        break;
                    case "timerTextOffset":
                        createOffsetDialog();
                        break;
                    case "scrambleTextSize":
                        createTextSizeDialog("scrambleTextSize");
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
            final Preference scrambleTextSize = (Preference) getPreferenceScreen().findPreference("scrambleTextSize");
            final Preference timerTextOffset = (Preference) getPreferenceScreen().findPreference("timerTextOffset");

            timerTextSize.setOnPreferenceClickListener(clickListener);
            scrambleTextSize.setOnPreferenceClickListener(clickListener);
            timerTextOffset.setOnPreferenceClickListener(clickListener);

        }

        private void createTextSizeDialog(final String pref) {
            final DiscreteSeekBar seekBar
                    = (DiscreteSeekBar) LayoutInflater.from(getActivity()).inflate(R.layout.dialog_progress, null);
            seekBar.setMin(1);
            seekBar.setMax(30);
            seekBar.setProgress(preferences.getInt(pref, 10));
            seekBar.setNumericTransformer(new DiscreteSeekBar.NumericTransformer() {
                @Override
                public int transform(int value) {
                    return value * 10;
                }
            });

            new MaterialDialog.Builder(getActivity())
                    .title(R.string.timer_text_size)
                    .customView(seekBar, false)
                    .positiveText(R.string.action_done)
                    .negativeText(R.string.action_cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            int seekProgress = seekBar.getProgress();
                            editor.putInt(pref, seekProgress);
                            editor.apply();
                        }
                    })
                    .show();
        }

        private void createOffsetDialog() {
            final DiscreteSeekBar seekBar
                    = (DiscreteSeekBar) LayoutInflater.from(getActivity()).inflate(R.layout.dialog_progress, null);
            seekBar.setMin(-250);
            seekBar.setMax(250);
            seekBar.setProgress(preferences.getInt("timerTextOffset", 0));
            seekBar.setIndicatorFormatter("%dpx");

            new MaterialDialog.Builder(getActivity())
                    .title(R.string.timer_text_offset)
                    .customView(seekBar, false)
                    .positiveText(R.string.action_done)
                    .negativeText(R.string.action_cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            int seekProgress = seekBar.getProgress();
                            editor.putInt("timerTextOffset", seekProgress);
                            editor.apply();
                        }
                    })
                    .show();
        }
    }

}