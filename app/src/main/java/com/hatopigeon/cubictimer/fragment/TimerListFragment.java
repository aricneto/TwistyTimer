package com.hatopigeon.cubictimer.fragment;


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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.hatopigeon.cubicify.BuildConfig;
import com.hatopigeon.cubicify.R;
import com.hatopigeon.cubictimer.CubicTimer;
import com.hatopigeon.cubictimer.activity.MainActivity;
import com.hatopigeon.cubictimer.adapter.TimeCursorAdapter;
import com.hatopigeon.cubictimer.database.DatabaseHandler;
import com.hatopigeon.cubictimer.database.TimeTaskLoader;
import com.hatopigeon.cubictimer.fragment.dialog.AddTimeDialog;
import com.hatopigeon.cubictimer.items.Theme;
import com.hatopigeon.cubictimer.listener.OnBackPressedInFragmentListener;
import com.hatopigeon.cubictimer.stats.Statistics;
import com.hatopigeon.cubictimer.stats.StatisticsCache;
import com.hatopigeon.cubictimer.utils.Prefs;
import com.hatopigeon.cubictimer.utils.PuzzleUtils;
import com.hatopigeon.cubictimer.utils.TTIntent;
import com.hatopigeon.cubictimer.utils.ThemeUtils;

import org.joda.time.DateTime;

import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static com.hatopigeon.cubictimer.utils.TTIntent.*;

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

    private String orderByKey = DatabaseHandler.KEY_DATE;
    private String orderByDir = TimeTaskLoader.DIR_DESC;

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
                                    CubicTimer.getDBHandler().moveAllSolvesToHistory(
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
                                    CubicTimer.getDBHandler().deleteAllFromSession(
                                            currentPuzzle, currentPuzzleCategory);
                                    broadcast(CATEGORY_TIME_DATA_CHANGES, ACTION_TIMES_MODIFIED);
                                }
                            })
                            .build());
                    break;
                case R.id.more_button:

                    // Main popup
                    PopupMenu popupMenu = new PopupMenu(getActivity(), moreButton);
                    popupMenu.getMenuInflater().inflate(R.menu.menu_list_more, popupMenu.getMenu());

                    popupMenu.setOnMenuItemClickListener(item -> {
                        switch (item.getItemId()) {
                            case R.id.unarchive:
                                ThemeUtils.roundAndShowDialog(mContext, new MaterialDialog.Builder(mContext)
                                        .title(R.string.list_options_item_from_history)
                                        .content(getString(R.string.unarchive_dialog_summary,
                                                           CubicTimer.getDBHandler()
                                                                   .getNumArchivedSolves(currentPuzzle, currentPuzzleCategory)))
                                        .inputType(InputType.TYPE_CLASS_NUMBER)
                                        .input(null, null, false, (dialog, input) -> {
                                            CubicTimer.getDBHandler().unarchiveSolves(
                                                    currentPuzzle, currentPuzzleCategory, Integer.parseInt(input.toString()));
                                            broadcast(CATEGORY_TIME_DATA_CHANGES, ACTION_TIME_ADDED);
                                        })
                                        .positiveText(R.string.list_options_item_from_history)
                                        .negativeText(R.string.action_cancel)
                                        .build());
                                break;
                            case R.id.share_ao5:
                                PuzzleUtils.shareAverageOf(5, currentPuzzle, mRecentStatistics, getActivity());
                                break;
                            case R.id.share_ao12:
                                PuzzleUtils.shareAverageOf(12, currentPuzzle, mRecentStatistics, getActivity());
                                break;
                            case R.id.share_histogram:
                                PuzzleUtils.shareHistogramOf(currentPuzzle, mRecentStatistics, getActivity());
                                break;
                            case R.id.sort_time:
                                orderByKey = DatabaseHandler.KEY_TIME;
                                break;
                            case R.id.sort_date:
                                orderByKey = DatabaseHandler.KEY_DATE;
                                break;
                            case R.id.sort_ascd:
                            case R.id.sort_asc:
                                orderByDir = TimeTaskLoader.DIR_ASC;
                                reloadList();
                                break;
                            case R.id.sort_descd:
                            case R.id.sort_desc:
                                orderByDir = TimeTaskLoader.DIR_DESC;
                                reloadList();
                                break;
                                default:
                                    break;
                        }
                        return true;
                    });

                    MenuPopupHelper popupHelper = new MenuPopupHelper(getActivity(), (MenuBuilder) popupMenu.getMenu(), moreButton);
                    popupHelper.setForceShowIcon(true);

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

        updateEasterEggs((RelativeLayout) rootView);

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
        return new TimeTaskLoader(currentPuzzle, currentPuzzleCategory, history, searchComment, orderByKey, orderByDir);
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

    private void updateEasterEggs(RelativeLayout root) {
        DateTime date = new DateTime(DateTime.now());

        // April 1st (April Fools)
        if (date.getMonthOfYear() == 4 && date.getDayOfMonth() == 1) {
            View clippyView = LayoutInflater.from(mContext).inflate(R.layout.item_easteregg_clippy, null);

            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );

            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);

            layoutParams.rightMargin = ThemeUtils.dpToPix(mContext, 8);
            layoutParams.bottomMargin = ThemeUtils.dpToPix(mContext, 8);

            String[] clippyLines = {
                    "It looks like you're having some trouble on that last layer.\n\nWould you like me to throw your cube away?",
                    "Wow, is that a 10 by 10??",
                    "It looks like you're relying too much on magnets\n\nWould you like to swap your cube for a Rubiks™ brand?",
                    "Hey there, I'm Clippy, your new cubing assistant!",
                    "I have a friend that can solve it in like, 3 seconds",
                    "I mean, it's not that hard. Just some algorithms, right?",
                    "It looks like you're trying to become color neutral.\n\nDid I mention I'm colorblind?",
                    "To be honest, I just peel the stickers off",
                    "I was walking through a cemetery once and saw a ghost cube.\n\nScary stuff.",
                    "It looks like you're abusing that table too much\n\nWould you like to learn a real one-handed method?",
                    "It looks like you're having some trouble with that 4x4 parity\n\nWould you like me to just peel off the stickers?",
                    "I am Clippy. I am ethereal, ETERNAL-Oh, hey!\n\nWould you like some help?",
                    "I once got six N perms in a row.\n\nI still have nightmares about that.",
                    "I once solved like five sides, couldn't quite figure out the last one",
                    "I CAN'T BELIEVE YOU GOT THAT PB, JUST AS MY SD CARD RUNS OUT",
                    "I once used my Pyraminx as a fork. True story.",
                    "Do you stop the timer with your feet too? Yuck.",
                    "In the abscence of a rock, a Megaminx makes a great substitute.\n\nOr so I've heard.",
                    "It is the year 2049. You have been cubing non-stop for over 30 years now.\n\nDon't you think it's time to take a break?"
            };

            TextView clippyText = clippyView.findViewById(R.id.clippy_text);
            clippyText.setText(clippyLines[new Random().nextInt(clippyLines.length)]);

            clippyView.setOnClickListener(v -> clippyView.animate()
                    .alpha(0)
                    .setDuration(300)
                    .withEndAction(() -> clippyView.setVisibility(View.GONE)));

            root.addView(clippyView, layoutParams);
        }
    }
}
