package com.aricneto.twistytimer.activity;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.preference.PreferenceScreen;
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
import com.aricneto.twistytimer.fragment.dialog.CrossHintFaceSelectDialog;
import com.aricneto.twistytimer.fragment.dialog.LocaleSelectDialog;
import com.aricneto.twistytimer.listener.OnBackPressedInFragmentListener;
import com.aricneto.twistytimer.utils.LocaleUtils;
import com.aricneto.twistytimer.utils.Prefs;
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat;

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


    @BindView(R.id.actionbar)
    Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (DEBUG_ME) Log.d(TAG, "onCreate(savedInstanceState=" + savedInstanceState + ")");

        setTheme(R.style.SettingsTheme);

        LocaleUtils.onCreate();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);
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
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_activity_container, new SettingsFragment(), "fragment_settings")
                    .commit();
        }

    }

    public void onRecreateRequired() {
        if (DEBUG_ME) Log.d(TAG, "onRecreationRequired(): " + this);
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if (DEBUG_ME) Log.d(TAG, "  Activity.recreate() NOW!: " + this);
                recreate();
            }
        });
    }

    @Override
    public void onBackPressed() {
        final Fragment settingsFragment = getSupportFragmentManager().findFragmentByTag("fragment_settings");

        if (settingsFragment instanceof OnBackPressedInFragmentListener) { // => not null
            // If the main fragment is open, let it and its "child" fragments consume the "Back"
            // button press if necessary.
            if (((OnBackPressedInFragmentListener) settingsFragment).onBackPressedInFragment()) {
                // Button press was consumed. Stop here.
                return;
            }
        }
        super.onBackPressed();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements OnBackPressedInFragmentListener {

        // Variables used to handle back button behavior
        // Stores last PreferenceScreen opened
        private PreferenceScreen lastPreferenceScreen;
        // Stores the main PreferenceScreen
        private PreferenceScreen mainScreen;

        private int inspectionDuration;

        private final android.support.v7.preference.Preference.OnPreferenceClickListener clickListener
                = new android.support.v7.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(android.support.v7.preference.Preference preference) {
                switch (Prefs.keyToResourceID(preference.getKey(),
                        R.string.pk_inspection_time,
                        R.string.pk_show_scramble_x_cross_hints,
                        R.string.pref_screen_title_timer_appearance_settings,
                        R.string.pk_locale,
                        R.string.pk_options_show_scramble_hints,
                        R.string.pk_timer_text_size,
                        R.string.pk_timer_text_offset,
                        R.string.pk_scramble_image_size,
                        R.string.pk_scramble_text_size,
                        R.string.pk_advanced_timer_settings_enabled)) {

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

                    case R.string.pk_options_show_scramble_hints:
                        CrossHintFaceSelectDialog.newInstance()
                                .show(((AppCompatActivity) getActivity()).getSupportFragmentManager(), "cross_hint_face_dialog");
                        break;

                    case R.string.pk_locale:
                        if (getActivity() instanceof AppCompatActivity) {
                            LocaleSelectDialog.newInstance()
                                    .show(((AppCompatActivity) getActivity()).getSupportFragmentManager(), "locale_dialog");
                        } else {
                            Log.e(TAG, "Could not find correct activity to launch dialog!");
                        }
                        break;

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


        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
            super.onCreate(savedInstanceState);
        }

        @Override
        public void onCreatePreferencesFix(Bundle bundle, String rootKey) {
            setPreferencesFromResource(R.xml.prefs, rootKey);

            int listenerPrefIds[] = {R.string.pk_inspection_time,
                    R.string.pref_screen_title_timer_appearance_settings,
                    R.string.pk_show_scramble_x_cross_hints,
                    R.string.pk_locale,
                    R.string.pk_options_show_scramble_hints,
                    R.string.pk_timer_text_size,
                    R.string.pk_timer_text_offset,
                    R.string.pk_scramble_text_size,
                    R.string.pk_scramble_image_size,
                    R.string.pk_advanced_timer_settings_enabled};

            for (int prefId : listenerPrefIds) {
                findPreference(getString(prefId))
                        .setOnPreferenceClickListener(clickListener);
            }

            mainScreen = getPreferenceScreen();

            // Set the Inspection Alert preference summary to display the correct information
            // about time elapsed depending on user's current inspection duration
            updateInspectionAlertText();
        }


        private void updateInspectionAlertText() {
            inspectionDuration = Prefs.getInt(R.string.pk_inspection_time, 15);
            findPreference(getString(R.string.pk_inspection_time))
                    .setSummary(getResources().getQuantityString(R.plurals.inspectionTimeSummary, inspectionDuration, inspectionDuration));
            findPreference(getString(R.string.pk_inspection_alert_enabled))
                    .setSummary(getString(R.string.pref_inspection_alert_summary,
                            inspectionDuration == 15 ? 8 : (int) (inspectionDuration * 0.5f),
                            inspectionDuration == 15 ? 12 : (int) (inspectionDuration * 0.8f)));
        }

        @Override
        public void onNavigateToScreen(PreferenceScreen preferenceScreen) {
            lastPreferenceScreen = getPreferenceScreen();
            setPreferenceScreen(preferenceScreen);
        }

        /**
         * PreferenceFragmentCompat does not handle back button behavior by default.
         * To implement the correct behavior, we store the last opened {@link PreferenceScreen} and
         * the main {@link PreferenceScreen}. When back button is pressed, we check if the current
         * screen is the same as mainScreen, if it is, we return false so the Activity can handle
         * it by closing {@link SettingsActivity}. If not, we set our screen to the last one we opened
         *
         * @return true if back button was consumed
         */
        @Override
        public boolean onBackPressedInFragment() {
            if (lastPreferenceScreen != null && getPreferenceScreen() != mainScreen) {
                setPreferenceScreen(lastPreferenceScreen);
                return true;
            }
            return false;
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
                    .onAny(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            updateInspectionAlertText();
                        }
                    })
                    .show();
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
