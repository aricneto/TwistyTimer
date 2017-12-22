package com.aricneto.twistytimer.fragment;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.aricneto.twistify.R;
import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.activity.MainActivity;
import com.aricneto.twistytimer.adapter.TimeCursorAdapter;
import com.aricneto.twistytimer.database.DatabaseHandler;
import com.aricneto.twistytimer.database.TimeTaskLoader;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.layout.Fab;
import com.aricneto.twistytimer.listener.OnBackPressedInFragmentListener;
import com.aricneto.twistytimer.stats.Statistics;
import com.aricneto.twistytimer.stats.StatisticsCache;
import com.aricneto.twistytimer.utils.Prefs;
import com.aricneto.twistytimer.utils.PuzzleUtils;
import com.aricneto.twistytimer.utils.TTIntent;
import com.aricneto.twistytimer.utils.ThemeUtils;
import com.gordonwong.materialsheetfab.DimOverlayFrameLayout;
import com.gordonwong.materialsheetfab.MaterialSheetFab;
import com.gordonwong.materialsheetfab.MaterialSheetFabEventListener;

import org.joda.time.DateTime;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static com.aricneto.twistytimer.utils.TTIntent.*;

public class TimerListFragment extends BaseFragment
        implements LoaderManager.LoaderCallbacks<Cursor>, OnBackPressedInFragmentListener,
        StatisticsCache.StatisticsObserver {
    /**
     * Flag to enable debug logging for this class.
     */
    private static final boolean DEBUG_ME = false;

    /**
     * A "tag" to identify this class in log messages.
     */
    private static final String TAG = TimerListFragment.class.getSimpleName();

    private static final String PUZZLE         = "puzzle";
    private static final String PUZZLE_SUBTYPE = "puzzle_type";
    private static final String HISTORY        = "history";

    private static final String SHOWCASE_FAB_ID = "SHOWCASE_FAB_ID";

    private MaterialSheetFab<Fab> materialSheetFab;
    // True if you want to search history, false if you only want to search session
    boolean         history;

    private Unbinder mUnbinder;
    @BindView(R.id.list)                 RecyclerView          recyclerView;
    @BindView(R.id.nothing_here)         ImageView             nothingHere;
    @BindView(R.id.nothing_text)         TextView              nothingText;
    @BindView(R.id.send_to_history_card) CardView              moveToHistory;
    @BindView(R.id.clear_button)         TextView              clearButton;
    @BindView(R.id.divider01)            View                  dividerView;
    @BindView(R.id.archive_button)       TextView              archiveButton;
    @BindView(R.id.fab_button)           Fab                   fabButton;
    @BindView(R.id.overlay)              DimOverlayFrameLayout overlay;
    @BindView(R.id.fab_sheet)            CardView              fabSheet;
    @BindView(R.id.fab_share_ao12)       TextView              fabShareAo12;
    @BindView(R.id.fab_share_ao5)        TextView              fabShareAo5;
    @BindView(R.id.fab_share_histogram)  TextView              fabShareHistogram;
    @BindView(R.id.fab_add_time)         TextView              fabAddTime;
    @BindView(R.id.fab_scroll)           ScrollView            fabScroll;

    private String            currentPuzzle;
    private String            currentPuzzleSubtype;
    private TimeCursorAdapter timeCursorAdapter;

    /**
     * The most recently notified solve time statistics. These may be used when sharing averages.
     */
    private Statistics mRecentStatistics;

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            final DatabaseHandler dbHandler = TwistyTimer.getDBHandler();
            // TODO: Should use "mRecentStatistics" when sharing averages.
            switch (view.getId()) {
                case R.id.fab_share_ao12:
                    if (!PuzzleUtils.shareAverageOf(
                            12, currentPuzzle, mRecentStatistics, getActivity())) {
                        Toast.makeText(getContext(), R.string.fab_share_error, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case R.id.fab_share_ao5:
                    if (!PuzzleUtils.shareAverageOf(
                            5, currentPuzzle, mRecentStatistics, getActivity())) {
                        Toast.makeText(getContext(), R.string.fab_share_error, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case R.id.fab_share_histogram:
                    if (!PuzzleUtils.shareHistogramOf(
                            currentPuzzle, mRecentStatistics, getActivity())) {
                        Toast.makeText(getContext(), R.string.fab_share_error, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case R.id.fab_add_time:
                    new MaterialDialog.Builder(getContext())
                        .title(R.string.add_time)
                        .input(getString(R.string.add_time_hint), "", false, new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(MaterialDialog dialog, CharSequence input) {
                                int time = PuzzleUtils.parseTime(input.toString());
                                if (time != 0) {
                                    final Solve solve = new Solve(time, currentPuzzle,
                                            currentPuzzleSubtype, new DateTime().getMillis(), "",
                                            PuzzleUtils.NO_PENALTY, "", false);

                                    dbHandler.addSolve(solve);
                                    // The receiver might be able to use the new solve and avoid
                                    // accessing the database.
                                    new TTIntent.BroadcastBuilder(
                                            CATEGORY_TIME_DATA_CHANGES, ACTION_TIME_ADDED_MANUALLY)
                                            .solve(solve)
                                            .broadcast();
                                }
                            }
                        })
                        .positiveText(R.string.action_add)
                        .negativeText(R.string.action_cancel)
                        .show();
                    break;
            }
        }
    };

    // Receives broadcasts after changes have been made to time data or the selection of that data.
    private TTFragmentBroadcastReceiver mTimeDataChangedReceiver
            = new TTFragmentBroadcastReceiver(this, CATEGORY_TIME_DATA_CHANGES) {
        @Override
        public void onReceiveWhileAdded(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_TIME_ADDED_MANUALLY:
                    if (!history)
                        reloadList();
                case ACTION_TIME_ADDED:
                    // When "history" is enabled, the list of times does not include times from
                    // the current session. Times are only added to the current session, so
                    // there is no need to refresh the "history" list on adding a session time.
                    if (! history) {
                        /*
                            If a time has been added by the timer, wait a few seconds to let the
                            (expensive) timer animations run before doing anything with the new data.
                            Since the user will, in most cases (unless they quickly change tabs
                            immediatelly after stopping the timer), will be at the Timer screen, this
                            delay will not be noticeable, and will improve the feeling of responsiveness
                            at the Timer page.
                         */
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                reloadList();
                            }
                        }, 600);
                    }
                    break;

                case ACTION_TIMES_MODIFIED:
                    reloadList();
                    break;

                case ACTION_HISTORY_TIMES_SHOWN:
                    setHistory(true);
                    reloadList();
                    break;

                case ACTION_SESSION_TIMES_SHOWN:
                    setHistory(false);
                    reloadList();
                    break;
            }
        }
    };

    private void setHistory(boolean value) {
        history = value;

        // Need to persist history to fragment arguments so that it is correctly persisted on
        // re-create (such as when device is rotated).
        Bundle arguments = getArguments();

        // Cargo-culted this null check from onCreate. Unsure how this could actually be null.
        if (arguments != null) {
            arguments.putBoolean(HISTORY, history);
            setArguments(arguments);
        }
    }

    // Receives broadcasts about UI interactions that require actions to be taken.
    private TTFragmentBroadcastReceiver mUIInteractionReceiver
            = new TTFragmentBroadcastReceiver(this, CATEGORY_UI_INTERACTIONS) {
        @Override
        public void onReceiveWhileAdded(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_DELETE_SELECTED_TIMES:
                    // Operation will delete times and then broadcast "ACTION_TIMES_MODIFIED".
                    timeCursorAdapter.deleteAllSelected();
                    break;
            }
        }
    };

    public TimerListFragment() {
        // Required empty public constructor
    }

    // We have to put a boolean history here because it resets when we change puzzles.
    public static TimerListFragment newInstance(String puzzle, String puzzleType, boolean history) {
        TimerListFragment fragment = new TimerListFragment();
        Bundle args = new Bundle();
        args.putString(PUZZLE, puzzle);
        args.putBoolean(HISTORY, history);
        args.putString(PUZZLE_SUBTYPE, puzzleType);
        fragment.setArguments(args);
        if (DEBUG_ME) Log.d(TAG, "newInstance() -> " + fragment);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (DEBUG_ME) Log.d(TAG, "onCreate(savedInstanceState=" + savedInstanceState + ")");
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentPuzzle = getArguments().getString(PUZZLE);
            currentPuzzleSubtype = getArguments().getString(PUZZLE_SUBTYPE);
            history = getArguments().getBoolean(HISTORY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (DEBUG_ME) Log.d(TAG, "onCreateView(savedInstanceState=" + savedInstanceState + ")");
        View rootView = inflater.inflate(R.layout.fragment_time_list, container, false);
        mUnbinder = ButterKnife.bind(this, rootView);

        if (Prefs.getBoolean(R.string.pk_show_clear_button, false)) {
            dividerView.setVisibility(View.VISIBLE);
            clearButton.setVisibility(View.VISIBLE);
            archiveButton.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_timer_sand_black_18dp, 0, 0, 0);
        }

        materialSheetFab = new MaterialSheetFab<>(
            fabButton, fabSheet, overlay, Color.WHITE, ThemeUtils.fetchAttrColor(getActivity(), R.attr.colorPrimary));

        materialSheetFab.setEventListener(new MaterialSheetFabEventListener() {
            @Override
            public void onSheetShown() {
                super.onSheetShown();
                fabScroll.post(new Runnable() {
                    @Override
                    public void run() {
                        fabScroll.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });

        fabShareAo12.setOnClickListener(clickListener);
        fabShareAo5.setOnClickListener(clickListener);
        fabShareHistogram.setOnClickListener(clickListener);
        fabAddTime.setOnClickListener(clickListener);

        archiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Spannable text = new SpannableString(getString(R.string.move_solves_to_history_content) + "  ");
                ImageSpan icon = new ImageSpan(getContext(), R.drawable.ic_icon_history_demonstration);
                text.setSpan(icon, text.length() - 1, text.length(), 0);

                new MaterialDialog.Builder(getContext())
                    .title(R.string.move_solves_to_history)
                    .content(text)
                    .positiveText(R.string.action_move)
                    .negativeText(R.string.action_cancel)
                    .neutralColor(ContextCompat.getColor(getContext(), R.color.black_secondary))
                    .negativeColor(ContextCompat.getColor(getContext(), R.color.black_secondary))
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(MaterialDialog dialog, DialogAction which) {
                            TwistyTimer.getDBHandler().moveAllSolvesToHistory(
                                    currentPuzzle, currentPuzzleSubtype);
                            broadcast(CATEGORY_TIME_DATA_CHANGES, ACTION_TIMES_MOVED_TO_HISTORY);
                            reloadList();
                        }
                    })
                    .show();
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new MaterialDialog.Builder(getContext())
                    .title(R.string.remove_session_title)
                    .content(R.string.remove_session_confirmation_content)
                    .positiveText(R.string.action_remove)
                    .negativeText(R.string.action_cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(MaterialDialog dialog, DialogAction which) {
                            TwistyTimer.getDBHandler().deleteAllFromSession(
                                    currentPuzzle, currentPuzzleSubtype);
                            broadcast(CATEGORY_TIME_DATA_CHANGES, ACTION_TIMES_MODIFIED);
                            reloadList();
                        }
                    })
                    .show();
            }
        });

        setupRecyclerView();
        getLoaderManager().initLoader(MainActivity.TIME_LIST_LOADER_ID, null, this);

        registerReceiver(mTimeDataChangedReceiver);
        registerReceiver(mUIInteractionReceiver);

        // If the statistics are already loaded, the update notification will have been missed,
        // so fire that notification now and start observing further updates.
        onStatisticsUpdated(StatisticsCache.getInstance().getStatistics());
        StatisticsCache.getInstance().registerObserver(this); // Unregistered in "onDestroyView".

        return rootView;
    }

    @Override
    public void onDestroyView() {
        if (DEBUG_ME) Log.d(TAG, "onDestroyView()");
        super.onDestroyView();
        mUnbinder.unbind();
        StatisticsCache.getInstance().unregisterObserver(this);
        mRecentStatistics = null;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        if (DEBUG_ME) Log.d(TAG, "setUserVisibleHint(isVisibleToUser=" + isVisibleToUser + ")");
        super.setUserVisibleHint(isVisibleToUser);

        if (isResumed()) {
            if (isVisibleToUser) {
                if (fabButton != null) {
                    // Show FAB and intro (if intro was not already dismissed by the user in a
                    // previous session) if the fragment has become visible.
                    fabButton.show();
                }
            } else if (materialSheetFab != null) {
                // Hide sheet and FAB if the fragment is no longer visible.
                materialSheetFab.hideSheetThenFab();
            }
        }
    }

    /**
     * Hides the sheet for the floating action button, if the sheet is currently open.
     *
     * @return
     *     {@code true} if the "Back" button press was consumed to close the sheet; or
     *     {@code false} if the sheet is not showing and the "Back" button press was ignored.
     */
    @Override
    public boolean onBackPressedInFragment() {
        if (DEBUG_ME) Log.d(TAG, "onBackPressedInFragment()");
        if (isResumed() && materialSheetFab != null && materialSheetFab.isSheetVisible()) {
            materialSheetFab.hideSheet();
            return true;
        }
        return false;
    }

    @Override
    public void onDetach() {
        if (DEBUG_ME) Log.d(TAG, "onDetach()");
        super.onDetach();
        // To fix memory leaks
        unregisterReceiver(mTimeDataChangedReceiver);
        unregisterReceiver(mUIInteractionReceiver);
        getLoaderManager().destroyLoader(MainActivity.TIME_LIST_LOADER_ID);
    }

    /**
     * Records the latest statistics for use when sharing such information.
     *
     * @param stats The updated statistics. These will not be modified. May be {@code null}.
     */
    @Override
    public void onStatisticsUpdated(Statistics stats) {
        if (DEBUG_ME) Log.d(TAG, "onStatisticsUpdated(" + stats + ")");
        mRecentStatistics = stats;
    }

    public void reloadList() {
        getLoaderManager().restartLoader(MainActivity.TIME_LIST_LOADER_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        if (DEBUG_ME) Log.d(TAG, "onCreateLoader()");
        return new TimeTaskLoader(currentPuzzle, currentPuzzleSubtype, history);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (DEBUG_ME) Log.d(TAG, "onLoadFinished()");
        timeCursorAdapter.swapCursor(cursor);
        recyclerView.getAdapter().notifyDataSetChanged();
        setEmptyState(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        if (DEBUG_ME) Log.d(TAG, "onLoaderReset()");
        timeCursorAdapter.swapCursor(null);
    }

    public void setEmptyState(Cursor cursor) {
        if (cursor.getCount() == 0) {
            nothingHere.setVisibility(View.VISIBLE);
            nothingText.setVisibility(View.VISIBLE);
            moveToHistory.setVisibility(View.GONE);
            if (history) {
                nothingHere.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.notherehistory));
                nothingText.setText(R.string.list_empty_state_message_history);
            } else {
                nothingHere.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.nothere2));
                nothingText.setText(R.string.list_empty_state_message);
            }
        } else {
            nothingHere.setVisibility(View.INVISIBLE);
            nothingText.setVisibility(View.INVISIBLE);
            if (history)
                moveToHistory.setVisibility(View.GONE);
            else
                moveToHistory.setVisibility(View.VISIBLE);
        }
    }

    private void setupRecyclerView() {
        Activity parentActivity = getActivity();

        timeCursorAdapter = new TimeCursorAdapter(getActivity(), null, this);

        // Set different managers to support different orientations
        StaggeredGridLayoutManager gridLayoutManagerHorizontal =
            new StaggeredGridLayoutManager(6, StaggeredGridLayoutManager.VERTICAL);
        StaggeredGridLayoutManager gridLayoutManagerVertical =
            new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL);

        // Adapt to orientation
        if (parentActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
            recyclerView.setLayoutManager(gridLayoutManagerVertical);
        else
            recyclerView.setLayoutManager(gridLayoutManagerHorizontal);

        recyclerView.setAdapter(timeCursorAdapter);
    }
}
