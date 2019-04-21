package com.aricneto.twistytimer.fragment;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.activity.MainActivity;
import com.aricneto.twistytimer.adapter.AlgRecylerAdapter;
import com.aricneto.twistytimer.utils.AlgUtils;
import com.aricneto.twistytimer.utils.TTIntent.TTFragmentBroadcastReceiver;
import com.aricneto.twistytimer.utils.ThemeUtils;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static com.aricneto.twistytimer.utils.TTIntent.ACTION_ALGS_MODIFIED;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_CHANGED_THEME;
import static com.aricneto.twistytimer.utils.TTIntent.CATEGORY_ALG_DATA_CHANGES;
import static com.aricneto.twistytimer.utils.TTIntent.CATEGORY_UI_INTERACTIONS;
import static com.aricneto.twistytimer.utils.TTIntent.registerReceiver;
import static com.aricneto.twistytimer.utils.TTIntent.unregisterReceiver;

public class AlgListFragment extends BaseFragment {

    private static final String KEY_SUBSET = "subset";
    private static final String KEY_PUZZLE = "puzzle";

    @BindView(R.id.root)
    LinearLayout rootLayout;

    @BindView(R.id.puzzleSpinner)
    View puzzleSpinner;

    @BindView(R.id.spinnerIcon)
    View spinnerIcon;

    @BindView(R.id.puzzleName)
    TextView titleView;

    @BindView(R.id.puzzleCategory)
    TextView subtitleView;

    @BindView(R.id.nav_button_history)
    View button1;

    @BindView(R.id.nav_button_category)
    View button2;

    @BindView(R.id.nav_button_settings)
    View buttonSettings;

    @BindView(R.id.list)
    RecyclerView recyclerView;

    private Unbinder                    mUnbinder;
    private String                      currentSubset;
    private String                      currentPuzzle;
    private AlgRecylerAdapter           algCursorAdapter;


    // Receives broadcasts about changes to the algorithm data.
    private TTFragmentBroadcastReceiver mAlgDataChangedReceiver
            = new TTFragmentBroadcastReceiver(this, CATEGORY_ALG_DATA_CHANGES) {
        @Override
        public void onReceiveWhileAdded(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_ALGS_MODIFIED:
                    setupRecyclerView();
                    break;
            }
        }
    };

    // Receives broadcasts about changes to the time user interface.
    private TTFragmentBroadcastReceiver mUIInteractionReceiver
            = new TTFragmentBroadcastReceiver(this, CATEGORY_UI_INTERACTIONS) {
        @Override
        public void onReceiveWhileAdded(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_CHANGED_THEME:
                    try {
                        // If the theme has been changed, then the activity will need to be recreated. The
                        // theme can only be applied properly during the inflation of the layouts, so it has
                        // to go back to "Activity.updateLocale()" to do that.
                        ((MainActivity) getActivity()).onRecreateRequired();
                    } catch (Exception e) {}
                    break;
            }
        }
    };


    public AlgListFragment() {
        // Required empty public constructor
    }

    public static AlgListFragment newInstance(String puzzle, String subset) {
        AlgListFragment fragment = new AlgListFragment();
        Bundle args = new Bundle();
        args.putString(KEY_SUBSET, subset);
        args.putString(KEY_PUZZLE, puzzle);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentSubset = getArguments().getString(KEY_SUBSET);
            currentPuzzle = getArguments().getString(KEY_PUZZLE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_alg_list, container, false);
        mUnbinder = ButterKnife.bind(this, rootView);

        rootLayout.setBackground(ThemeUtils.fetchBackgroundGradient(getContext(), ThemeUtils.getPreferredTheme()));

        titleView.setText(R.string.title_algorithms);
        subtitleView.setText(currentSubset);

        spinnerIcon.setVisibility(View.GONE);
        button1.setVisibility(View.GONE);
        button2.setVisibility(View.GONE);
        buttonSettings.setOnClickListener(v -> getMainActivity().openDrawer());

        puzzleSpinner.setOnClickListener(v -> AlgUtils.showAlgSelectDialog(getFragmentManager(), currentPuzzle));

        setupRecyclerView();

        // Register a receiver to update if something has changed
        registerReceiver(mAlgDataChangedReceiver);
        registerReceiver(mUIInteractionReceiver);

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mAlgDataChangedReceiver);
        unregisterReceiver(mUIInteractionReceiver);
    }

    private void setupRecyclerView() {
        Activity parentActivity = getActivity();

        algCursorAdapter = new AlgRecylerAdapter(getActivity(), getFragmentManager(), currentPuzzle, currentSubset);

        // Set different managers to support different orientations
        StaggeredGridLayoutManager gridLayoutManagerHorizontal =
                new StaggeredGridLayoutManager(4, StaggeredGridLayoutManager.VERTICAL);
        StaggeredGridLayoutManager gridLayoutManagerVertical =
                new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL);

        // Adapt to orientation
        if (parentActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
            recyclerView.setLayoutManager(gridLayoutManagerVertical);
        else
            recyclerView.setLayoutManager(gridLayoutManagerHorizontal);

        recyclerView.setAdapter(algCursorAdapter);
    }
}
