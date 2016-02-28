package com.aricneto.twistytimer.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.aricneto.twistify.R;
import com.jenzz.materialpreference.Preference;

import butterknife.Bind;
import butterknife.ButterKnife;

public class SettingsActivity extends AppCompatActivity {

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
                    .add(R.id.main_activity_container, new SettingsFragment())
                    .addToBackStack("main")
                    .commit();
        }
    }

    public static class SettingsFragment extends PreferenceFragment {

        private SharedPreferences.Editor editor;
        private SharedPreferences        preferences;

        android.preference.Preference.OnPreferenceClickListener clickListener = new android.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(android.preference.Preference preference) {
                switch (preference.getKey()) {
                    case "inspectionTime":
                        createNumberDialog(R.string.inspection_time, "inspectionTime");
                        break;
                    case "timerAppearance":
                        Intent appearanceIntent = new Intent(getActivity(), TimerAppearanceSettingsActivity.class);
                        startActivity(appearanceIntent);
                        break;
                }
                return false;
            }
        };

        @SuppressLint("CommitPrefEdits")
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.prefs);

            preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            editor = preferences.edit();

            final Preference inspectionTime = (Preference) getPreferenceScreen().findPreference("inspectionTime");
            final Preference timerAppearance = (Preference) getPreferenceScreen().findPreference("timerAppearance");

            timerAppearance.setOnPreferenceClickListener(clickListener);
            inspectionTime.setOnPreferenceClickListener(clickListener);

        }

        private void createNumberDialog(@StringRes int title, final String key) {
            new MaterialDialog.Builder(getActivity())
                    .title(title)
                    .input("", String.valueOf(preferences.getInt(key, 15)), new MaterialDialog.InputCallback() {
                        @Override
                        public void onInput(MaterialDialog dialog, CharSequence input) {
                            int time;
                            try {
                                time = Integer.parseInt(input.toString());
                                editor.putInt(key, time);
                                editor.apply();
                            } catch (NumberFormatException e) {
                                Toast.makeText(getActivity(), R.string.invalid_time, Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .inputType(InputType.TYPE_CLASS_NUMBER)
                    .positiveText(R.string.action_done)
                    .negativeText(R.string.action_cancel)
                    .show();
        }
    }
}