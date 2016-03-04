package com.aricneto.twistytimer.activity;


import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.aricneto.twistify.R;

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

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.prefs_timer_appearance);

        }

    }

}