package com.aricneto.twistytimer.fragment;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.FragmentManager;
import androidx.loader.app.LoaderManager;
import androidx.core.content.ContextCompat;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.aricneto.twistify.BuildConfig;
import com.aricneto.twistify.R;
import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.activity.MainActivity;
import com.aricneto.twistytimer.adapter.TimeCursorAdapter;
import com.aricneto.twistytimer.database.TimeTaskLoader;
import com.aricneto.twistytimer.fragment.dialog.AddTimeDialog;
import com.aricneto.twistytimer.listener.OnBackPressedInFragmentListener;
import com.aricneto.twistytimer.stats.Statistics;
import com.aricneto.twistytimer.stats.StatisticsCache;
import com.aricneto.twistytimer.utils.Prefs;
import com.aricneto.twistytimer.utils.PuzzleUtils;
import com.aricneto.twistytimer.utils.TTIntent;
import com.aricneto.twistytimer.utils.ThemeUtils;

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

    private Context mContext;

    /**
     * A "tag" to identify this class in log messages.
     */
    private static final String TAG = TimerListFragment.class.getSimpleName();

    private static final String PUZZLE         = "puzzle";
    private static final String PUZZLE_SUBTYPE = "puzzle_type";
    private static final String HISTORY        = "history";

    private static final String SHOWCASE_FAB_ID = "SHOWCASE_FAB_ID";

    // True if you want to search history, false if you only want to search session
    boolean         history;

    private                        Unbinder          mUnbinder;
    @BindView(R.id.list)           RecyclerView      recyclerView;
    @BindView(R.id.warn_empty_list)ImageView         nothingHere;
    @BindView(R.id.nothing_text)   TextView          nothingText;
    @BindView(R.id.buttons_layout) View              buttonsLayout;
    @BindView(R.id.clear_button)   View              clearButton;
    @BindView(R.id.archive_button) View              archiveButton;
    @BindView(R.id.add_time_button)View              addTimeButton;
    @BindView(R.id.search_box)     AppCompatEditText searchEditText;
    @BindView(R.id.more_button)    View              moreButton;

    private String currentPuzzle;
    private String currentPuzzleCategory;
    private String currentScramble;

    // Stores the current comment search query
    private String searchComment = "";

    private TimeCursorAdapter timeCursorAdapter;

    /**
     * The most recently notified solve time statistics. These may be used when sharing averages.
     */
    private Statistics mRecentStatistics;

    @SuppressLint("RestrictedApi")
    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // TODO: Should use "mRecentStatistics" when sharing averages.
            switch (view.getId()) {
                case R.id.add_time_button:
                    AddTimeDialog addTimeDialog = AddTimeDialog.newInstance(currentPuzzle, currentPuzzleCategory, currentScramble);
                    FragmentManager manager = getFragmentManager();
                    if (manager != null)
                        addTimeDialog.show(manager, "dialog_add_time");
                    break;
                case R.id.archive_button:
                    final Spannable text = new SpannableString(getString(R.string.move_solves_to_history_content) + "  ");
                    text.setSpan(ThemeUtils.getIconSpan(mContext, 0.6f), text.length() - 1, text.length(), 0);

                    ThemeUtils.roundAndShowDialog(mContext, new MaterialDialog.Builder(mContext)
                            .title(R.string.move_solves_to_history)
                            .content(text)
                            .positiveText(R.string.action_move)
                            .negativeText(R.string.action_cancel)
                            .neutralColor(ContextCompat.getColor(mContext, R.color.black_secondary))
                            .negativeColor(ContextCompat.getColor(mContext, R.color.black_secondary))
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(MaterialDialog dialog, DialogAction which) {
                                    TwistyTimer.getDBHandler().moveAllSolvesToHistory(
                                            currentPuzzle, currentPuzzleCategory);
                                    broadcast(CATEGORY_TIME_DATA_CHANGES, ACTION_TIMES_MOVED_TO_HISTORY);
                                }
                            })
                            .build());
                    break;
                case R.id.clear_button:
                    ThemeUtils.roundAndShowDialog(mContext, new MaterialDialog.Builder(mContext)
                            .title(R.string.remove_session_title)
                            .content(R.string.remove_session_confirmation_content)
                            .positiveText(R.string.action_remove)
                            .negativeText(R.string.action_cancel)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(MaterialDialog dialog, DialogAction which) {
                                    TwistyTimer.getDBHandler().deleteAllFromSession(
                                            currentPuzzle, currentPuzzleCategory);
                                    broadcast(CATEGORY_TIME_DATA_CHANGES, ACTION_TIMES_MODIFIED);
                                }
                            })
                            .build());
                    break;
                case R.id.more_button:
                    PopupMenu popupMenu = new PopupMenu(getContext(), moreButton);
                    popupMenu.getMenuInflater().inflate(R.menu.menu_list_more, popupMenu.getMenu());

                    MenuPopupHelper popupHelper = new MenuPopupHelper(mContext, (MenuBuilder) popupMenu.getMenu(), moreButton);
                    popupHelper.setForceShowIcon(true);

                    PopupMenu shareMenu = new PopupMenu(getContext(), moreButton);
                    shareMenu.getMenuInflater().inflate(R.menu.menu_list_share, shareMenu.getMenu());

                    MenuPopupHelper sharePopupHelper = new MenuPopupHelper(mContext, (MenuBuilder) shareMenu.getMenu(), moreButton);
                    sharePopupHelper.setForceShowIcon(true);


                    shareMenu.setOnMenuItemClickListener(item -> {
                        switch (item.getItemId()) {
                            case R.id.share_ao5:
                                PuzzleUtils.shareAverageOf(5, currentPuzzle, mRecentStatistics, getActivity());
                                break;
                            case R.id.share_ao12:
                                PuzzleUtils.shareAverageOf(12, currentPuzzle, mRecentStatistics, getActivity());
                                break;
                            case R.id.share_histogram:
                                PuzzleUtils.shareHistogramOf(currentPuzzle, mRecentStatistics, getActivity());
                                break;
                        }
                        return true;
                    });

                    popupMenu.setOnMenuItemClickListener(item -> {
                        switch (item.getItemId()) {
                            case R.id.unarchive:
                                ThemeUtils.roundAndShowDialog(mContext, new MaterialDialog.Builder(mContext)
                                        .title(R.string.list_options_item_from_history)
                                        .content(getString(R.string.unarchive_dialog_summary,
                                                           TwistyTimer.getDBHandler()
                                                                   .getNumArchivedSolves(currentPuzzle, currentPuzzleCategory)))
                                        .inputType(InputType.TYPE_CLASS_NUMBER)
                                        .input(null, null, false, (dialog, input) -> {
                                            TwistyTimer.getDBHandler().unarchiveSolves(
                                                    currentPuzzle, currentPuzzleCategory, Integer.parseInt(input.toString()));
                                            reloadList();
                                        })
                                        .positiveText(R.string.list_options_item_from_history)
                                        .negativeText(R.string.action_cancel)
                                        .build());
                                break;
                            case R.id.share:
                                sharePopupHelper.show();
                                break;
                        }
                        return true;
                    });

                    popupHelper.show();
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
                case ACTION_COMMENT_ADDED:
                    if (!history)
                        reloadList();
                    break;
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

                case ACTION_TIMES_MOVED_TO_HISTORY:
                case ACTION_TIMES_MODIFIED:
                    reloadList();
                    break;

                case ACTION_HISTORY_TIMES_SHOWN:
                    history = true;
                    reloadList();
                    break;

                case ACTION_SESSION_TIMES_SHOWN:
                    history = false;
                    reloadList();
                    break;
            }
        }
    };

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
                case ACTION_SCRAMBLE_MODIFIED:
                    // A new scramble was generated
                    currentScramble = TTIntent.getScramble(intent);
                    break;
                    default:
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
        if (DEBUG_ME) Log.d(TAG, "updateLocale(savedInstanceState=" + savedInstanceState + ")");
        super.onCreate(savedInstanceState);
        mContext = getContext();
        if (getArguments() != null) {
            currentPuzzle = getArguments().getString(PUZZLE);
            currentPuzzleCategory = getArguments().getString(PUZZLE_SUBTYPE);
            history = getArguments().getBoolean(HISTORY);
        }
        if (savedInstanceState != null) {
            currentScramble = savedInstanceState.getString("scramble");
        }
    }

    @SuppressLint("DefaultLocale")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (DEBUG_ME) Log.d(TAG, "onCreateView(savedInstanceState=" + savedInstanceState + ")");
        View rootView = inflater.inflate(R.layout.fragment_time_list, container, false);
        mUnbinder = ButterKnife.bind(this, rootView);

        if (Prefs.getBoolean(R.string.pk_show_clear_button, false)) {
            clearButton.setVisibility(View.VISIBLE);
        }

        addTimeButton.setOnClickListener(clickListener);

        archiveButton.setOnClickListener(clickListener);

        clearButton.setOnClickListener(clickListener);

        moreButton.setOnClickListener(clickListener);

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                searchComment = s.toString();
                reloadList();
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
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("scramble", currentScramble);
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
    }

    /**
     * Note: FAB is no longer used. This method does nothing.
     *
     * @return
     *     {@code true} if the "Back" button press was consumed to close the sheet; or
     *     {@code false} if the sheet is not showing and the "Back" button press was ignored.
     */
    @Override
    public boolean onBackPressedInFragment() {
        if (DEBUG_ME) Log.d(TAG, "onBackPressedInFragment()");
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
        // Sometimes reloadList is scheduled9 to be called after a set amount of time
        // (like when a new time is added) so the app UI doesn't stutter. However, the fragment
        // may not exist after that while, so we have to check if it still exists before
        // restarting the loader. Otherwise, we'd get an IllegalStateException
        // You can reproduce a crash by disabling this safety check, and then rotating the
        // phone immediately after finishing a solve
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null && !fragmentManager.isDestroyed())
            getLoaderManager().restartLoader(MainActivity.TIME_LIST_LOADER_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        if (DEBUG_ME) Log.d(TAG, "onCreateLoader()");
        return new TimeTaskLoader(currentPuzzle, currentPuzzleCategory, history, searchComment);
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
            if (history) {
                nothingText.setText(R.string.list_empty_state_message_history);
            } else {
                nothingText.setText(R.string.list_empty_state_message);
            }
        } else {
            nothingHere.setVisibility(View.INVISIBLE);
            nothingText.setVisibility(View.INVISIBLE);
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
