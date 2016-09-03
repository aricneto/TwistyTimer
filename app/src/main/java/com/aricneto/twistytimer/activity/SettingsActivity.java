package com.aricneto.twistytimer.activity;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.aricneto.twistify.R;
import com.aricneto.twistytimer.utils.Prefs;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SettingsActivity extends AppCompatActivity {
    /**
     * Flag to enable debug logging for this class.
     */
    private static final boolean DEBUG_ME = false;

    /**
     * A "tag" to identify this class in log messages.
     */
    private static final String TAG = SettingsActivity.class.getSimpleName();

    @BindView(R.id.actionbar) Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (DEBUG_ME) Log.d(TAG, "onCreate(savedInstanceState=" + savedInstanceState + ")");
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
            // Add the main "parent" settings fragment. It is not added to be back stack, so that
            // when "Back" is pressed, the "SettingsActivity" will exit, which is appropriate.
            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_activity_container, new SettingsFragment())
                    .commit();
        }
    }

    /**
     * Finds the preference whose key matches the string value of the given preference key
     * string resource ID.
     *
     * @param fragment
     *     The preference fragment in which to search for the preference with the given key.
     * @param prefKeyResID
     *     The string resource ID of the preference key.
     * @return
     *     The preference that matches that key; or {@code null} if no such preference is found.
     */
    private static Preference find(PreferenceFragment fragment, int prefKeyResID) {
        return fragment.getPreferenceScreen().findPreference(fragment.getString(prefKeyResID));
    }

    // TODO: Should this be using "android.support.v7.preference.PreferenceFragmentCompat" or
    // "android.support.v14.preference.PreferenceFragment" instead? Those implementations have
    // more support for new API features and Material Design themes.
    public static class SettingsFragment extends PreferenceFragment {
        private final android.preference.Preference.OnPreferenceClickListener clickListener
                = new android.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(android.preference.Preference preference) {
                switch (Prefs.keyToResourceID(preference.getKey(),
                        R.string.pk_inspection_time,
                        R.string.pk_show_scramble_x_cross_hints,
                        R.string.pk_open_timer_appearance_settings)) {

                    case R.string.pk_inspection_time:
                        createNumberDialog(R.string.inspection_time, R.string.pk_inspection_time);
                        break;

                    case R.string.pk_show_scramble_x_cross_hints:
                        if (Prefs.getBoolean(R.string.pk_show_scramble_x_cross_hints, false)) {
                            new MaterialDialog.Builder(getActivity())
                                    .title(R.string.warning)
                                    .content(R.string.showHintsXCrossSummary)
                                    .positiveText(R.string.action_ok)
                                    .show();
                        }
                        break;

                    case R.string.pk_open_timer_appearance_settings:
                        // Open the new "child" settings fragment and add it to be back stack, so
                        // that if "Back" is pressed, this "parent" fragment will be restored.
                        getFragmentManager()
                                .beginTransaction()
                                .replace(R.id.main_activity_container,
                                        new TimerAppearanceSettingsFragment())
                                .addToBackStack(null)
                                .commit();
                        break;
                }
                return false;
            }
        };

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.prefs);

            find(this, R.string.pk_inspection_time)
                    .setOnPreferenceClickListener(clickListener);
            find(this, R.string.pk_open_timer_appearance_settings)
                    .setOnPreferenceClickListener(clickListener);
            find(this, R.string.pk_show_scramble_x_cross_hints)
                    .setOnPreferenceClickListener(clickListener);
        }

        private void createNumberDialog(@StringRes int title, final int prefKeyResID) {
            new MaterialDialog.Builder(getActivity())
                    .title(title)
                    .input("", String.valueOf(Prefs.getInt(prefKeyResID, 15)),
                            new MaterialDialog.InputCallback() {
                        @Override
                        public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                            try {
                                final int time = Integer.parseInt(input.toString());

                                Prefs.edit().putInt(prefKeyResID, time).apply();
                            } catch (NumberFormatException e) {
                                Toast.makeText(getActivity(),
                                        R.string.invalid_time, Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .inputType(InputType.TYPE_CLASS_NUMBER)
                    .positiveText(R.string.action_done)
                    .negativeText(R.string.action_cancel)
                    .neutralText(R.string.action_default)
                    .onNeutral(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(
                                @NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            Prefs.edit().putInt(prefKeyResID, 15).apply();
                        }
                    })
                    .show();
        }
    }

    public static class TimerAppearanceSettingsFragment extends PreferenceFragment {
        private final android.preference.Preference.OnPreferenceClickListener clickListener
                = new android.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(android.preference.Preference preference) {
                switch (Prefs.keyToResourceID(preference.getKey(),
                        R.string.pk_timer_text_size,
                        R.string.pk_timer_text_offset,
                        R.string.pk_scramble_image_size,
                        R.string.pk_scramble_text_size,
                        R.string.pk_advanced_timer_settings_enabled)) {

                    case R.string.pk_timer_text_size:
                        createSeekTextSizeDialog(R.string.pk_timer_text_size, 60, "12.34", true);
                        break;

                    case R.string.pk_timer_text_offset:
                        createSeekTextOffsetDialog();
                        break;

                    case R.string.pk_scramble_image_size:
                        createImageSeekDialog(
                                R.string.pk_scramble_image_size, R.string.scrambleImageSize_text);
                        break;

                    case R.string.pk_scramble_text_size:
                        createSeekTextSizeDialog(R.string.pk_scramble_text_size,
                                14, "R U R' U' R' F R2 U' R' U' R U R' F'", false);
                        break;

                    case R.string.pk_advanced_timer_settings_enabled:
                        if (Prefs.getBoolean(R.string.pk_advanced_timer_settings_enabled, false)) {
                            new MaterialDialog.Builder(getActivity())
                                    .title(R.string.warning)
                                    .content(R.string.advanced_pref_summary)
                                    .positiveText(R.string.action_ok)
                                    .show();
                        }
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

            find(this, R.string.pk_timer_text_size).setOnPreferenceClickListener(clickListener);
            find(this, R.string.pk_timer_text_offset).setOnPreferenceClickListener(clickListener);
            find(this, R.string.pk_scramble_text_size).setOnPreferenceClickListener(clickListener);
            find(this, R.string.pk_scramble_image_size).setOnPreferenceClickListener(clickListener);
            find(this, R.string.pk_advanced_timer_settings_enabled)
                    .setOnPreferenceClickListener(clickListener);
        }

        private void createSeekTextSizeDialog(
                final int prefKeyResID, int defaultTextSize, String showText, boolean bold) {
            final View dialogView = LayoutInflater.from(
                    getActivity()).inflate(R.layout.dialog_settings_progress, null);
            final AppCompatSeekBar seekBar
                    = (AppCompatSeekBar) dialogView.findViewById(R.id.seekbar);
            final TextView text = (TextView) dialogView.findViewById(R.id.text);
            seekBar.setMax(300);
            seekBar.setProgress(Prefs.getInt(prefKeyResID, 100));

            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, defaultTextSize);
            final float defaultTextSizePx = text.getTextSize();
            text.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    defaultTextSizePx * (seekBar.getProgress() / 100f));
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
                        public void onClick(
                                @NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            final int seekProgress = seekBar.getProgress();

                            Prefs.edit()
                                    .putInt(prefKeyResID, seekProgress > 10 ? seekProgress : 10)
                                    .apply();
                        }
                    })
                    .neutralText(R.string.action_default)
                    .onNeutral(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(
                                @NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            Prefs.edit().putInt(prefKeyResID, 100).apply();
                        }
                    })
                    .show();
        }

        private void createSeekTextOffsetDialog() {
            final View dialogView = LayoutInflater.from(
                    getActivity()).inflate(R.layout.dialog_settings_progress, null);
            final AppCompatSeekBar seekBar
                    = (AppCompatSeekBar) dialogView.findViewById(R.id.seekbar);
            final TextView text = (TextView) dialogView.findViewById(R.id.text);
            seekBar.setMax(500);
            seekBar.setProgress(Prefs.getInt(R.string.pk_timer_text_offset, 0) + 250);

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
                        public void onClick(
                                @NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            Prefs.edit()
                                    .putInt(R.string.pk_timer_text_offset,
                                            seekBar.getProgress() - 250)
                                    .apply();
                        }
                    })
                    .neutralText(R.string.action_default)
                    .onNeutral(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(
                                @NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            Prefs.edit().putInt(R.string.pk_timer_text_offset, 0).apply();
                        }
                    })
                    .show();
        }

        private void createImageSeekDialog(final int prefKeyResID, @StringRes int title) {
            final View dialogView = LayoutInflater.from(
                    getActivity()).inflate(R.layout.dialog_settings_progress_image, null);
            final AppCompatSeekBar seekBar
                    = (AppCompatSeekBar) dialogView.findViewById(R.id.seekbar);
            final View image = dialogView.findViewById(R.id.image);
            seekBar.setMax(300);
            seekBar.setProgress(Prefs.getInt(prefKeyResID, 100));

            final int defaultWidth = image.getLayoutParams().width;
            final int defaultHeight = image.getLayoutParams().height;

            image.getLayoutParams().width *= (seekBar.getProgress() / 100f);
            image.getLayoutParams().height *= (seekBar.getProgress() / 100f);


            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    LinearLayout.LayoutParams params
                            = (LinearLayout.LayoutParams) image.getLayoutParams();
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
                        public void onClick(
                                @NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            final int seekProgress = seekBar.getProgress();

                            Prefs.edit()
                                    .putInt(prefKeyResID, seekProgress > 10 ? seekProgress : 10)
                                    .apply();
                        }
                    })
                    .neutralText(R.string.action_default)
                    .onNeutral(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(
                                @NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            Prefs.edit().putInt(prefKeyResID, 100).apply();
                        }
                    })
                    .show();
        }
    }
}
