package com.aricneto.twistytimer.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Parcelable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

import com.aricneto.twistify.BuildConfig;
import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.items.Solve;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * The actions for the broadcast intents that notify listeners of changes to the data or to the
 * state of the application.
 *
 * @author damo
 */
public final class TTIntent {
    /**
     * Flag to enable debug logging for this class.
     */
    private static final boolean DEBUG_ME = false;

    /**
     * A "tag" to identify this class in log messages.
     */
    private static final String TAG = TTIntent.class.getSimpleName();

    /**
     * The name prefix for all categories and actions to ensure that their names do not clash with
     * any system names.
     */
    private static final String BASE_PREFIX = "com.aricneto.twistytimer.";

    /**
     * The name prefix for all categories.
     */
    private static final String CATEGORY_PREFIX = BASE_PREFIX + "category.";

    /**
     * The name prefix for all actions.
     */
    private static final String ACTION_PREFIX = BASE_PREFIX + "action.";

    /**
     * The name prefix for all extras.
     */
    private static final String EXTRA_PREFIX = BASE_PREFIX + "extra.";

    /**
     * The category for intents that communicate interactions with, or changes to the state of, the
     * timer and other user-interface elements.
     */
    public static final String CATEGORY_UI_INTERACTIONS = CATEGORY_PREFIX + "UI_INTERACTIONS";

    /**
     * The category for intents that communicate changes to the solve time data, or to the
     * selection of the set of solve time data to be presented.
     */
    public static final String CATEGORY_TIME_DATA_CHANGES = CATEGORY_PREFIX + "TIME_DATA_CHANGES";

    /**
     * The category for intents that communicate changes to the algorithm data, or to the selection
     * of the set of data to be presented.
     */
    public static final String CATEGORY_ALG_DATA_CHANGES = CATEGORY_PREFIX + "ALG_DATA_CHANGES";

    /**
     * One new solve time has been added.
     */
    public static final String ACTION_TIME_ADDED = ACTION_PREFIX + "TIME_ADDED";

    /**
     * One new solve time has been added manually via the TimerList FAB.
     */
    public static final String ACTION_TIME_ADDED_MANUALLY = ACTION_PREFIX + "TIME_ADDED_MANUALLY";

    /**
     * One or more solve times have been modified in unspecified ways. Modifications include adding
     * times (bulk import), deleting selected times, or changing the penalties, comments, history
     * status or other properties of one or more times. A full refresh of any displayed time data
     * may be required.
     */
    public static final String ACTION_TIMES_MODIFIED = ACTION_PREFIX + "TIMES_MODIFIED";

    /**
     * One or more solves have been moved from the current session to the history of all sessions.
     */
    public static final String ACTION_TIMES_MOVED_TO_HISTORY
            = ACTION_PREFIX + "TIMES_MOVED_TO_HISTORY";

    /**
     * The user has selected the option to show only times for the current session. Any solve times
     * being presented should be reloaded to match the new state, if necessary.
     */
    public static final String ACTION_SESSION_TIMES_SHOWN = ACTION_PREFIX + "SESSION_TIMES_SHOWN";

    /**
     * The user has added a comment to their last solve
     */
    public static final String ACTION_COMMENT_ADDED = ACTION_PREFIX + "COMMENT_ADDED";

    /**
     * The user has selected the option to show the fill history of all times for all past and
     * current sessions. Any solve times being presented should be reloaded to match the new state,
     * if necessary.
     */
    public static final String ACTION_HISTORY_TIMES_SHOWN = ACTION_PREFIX + "HISTORY_TIMES_SHOWN";

    /**
     * The user has scrolled a page, causing a different tab to be displayed. At the start of a
     * scroll action, the user touches the screen, which may have been interpreted as an action to
     * start the timer. However, if that touch then developed into a swipe that scrolled the page
     * and switched tabs, the action to start the timer should be cancelled.
     */
    public static final String ACTION_SCROLLED_PAGE = ACTION_PREFIX + "SCROLLED_PAGE";

    /**
     * The display of the tool bar has been restored. This action corresponds to the end of the
     * animation that restores the tool bar.
     */
    public static final String ACTION_TOOLBAR_RESTORED = ACTION_PREFIX + "TOOLBAR_RESTORED";

    /**
     * The tool bar button to generate a new scramble has been pressed and the receiver should
     * perform that action.
     */
    public static final String ACTION_GENERATE_SCRAMBLE = ACTION_PREFIX + "GENERATE_SCRAMBLE";

    /**
     * The current scramble has been modified, either by the user or by the timer itself.
     */
    public static final String ACTION_SCRAMBLE_MODIFIED = ACTION_PREFIX + "SCRAMBLE_MODIFIED";

    /**
     * Selection mode has been turned on for the list of times.
     */
    public static final String ACTION_SELECTION_MODE_ON = ACTION_PREFIX + "SELECTION_MODE_ON";

    /**
     * Selection mode has been turned off for the list of times.
     */
    public static final String ACTION_SELECTION_MODE_OFF = ACTION_PREFIX + "SELECTION_MODE_OFF";

    /**
     * An item in the list of times has been selected.
     */
    public static final String ACTION_TIME_SELECTED = ACTION_PREFIX + "TIME_SELECTED";

    /**
     * An item in the list of times has been unselected.
     */
    public static final String ACTION_TIME_UNSELECTED = ACTION_PREFIX + "TIME_UNSELECTED";

    /**
     * The user has chosen the action to delete all of the selected times. The receiver should
     * perform that operation and broadcast {@link #ACTION_TIMES_MODIFIED}.
     */
    public static final String ACTION_DELETE_SELECTED_TIMES
            = ACTION_PREFIX + "DELETE_SELECTED_TIMES";

    /**
     * The timer has been started.
     */
    public static final String ACTION_TIMER_STARTED = ACTION_PREFIX + "TIMER_STARTED";

    /**
     * The user has selected a new puzzle category (subtype)
     */
    public static final String ACTION_CHANGED_CATEGORY = ACTION_PREFIX + "CHANGED_CATEGORY";

    /**
     * The user has selected a new theme
     */
    public static final String ACTION_CHANGED_THEME = ACTION_PREFIX + "CHANGED_THEME";

    /**
     * The timer has been stopped.
     */
    public static final String ACTION_TIMER_STOPPED = ACTION_PREFIX + "TIMER_STOPPED";

    /**
     * One or more algorithms has been added, deleted or otherwise modified.
     */
    public static final String ACTION_ALGS_MODIFIED = ACTION_PREFIX + "ALGS_MODIFIED";

    /**
     * The name of an intent extra that can hold the name of the puzzle type.
     */
    public static final String EXTRA_PUZZLE_TYPE = EXTRA_PREFIX + "PUZZLE_TYPE";

    /**
     * The name of an intent extra that can hold the name of the puzzle subtype.
     */
    public static final String EXTRA_PUZZLE_SUBTYPE = EXTRA_PREFIX + "PUZZLE_SUBTYPE";

    /**
     * The name of an intent extra that can be used to record a {@link Solve}.
     */
    public static final String EXTRA_SOLVE = EXTRA_PREFIX + "SOLVE";

    /**
     * The name of an intent extra that can be used to record a scramble
     */
    public static final String EXTRA_SCRAMBLE = EXTRA_PREFIX + "SCRAMBLE";

    /**
     * The actions that are allowed under each category. The category name is the key and the
     * corresponding entry is a collection of action names that are supported for that category.
     * An action may be supported by more than one category.
     */
    // NOTE: To match an "Intent", it is not sufficient for an "IntentFilter" to simply match all
    // categories defined on the intent, it must also match the action on the "Intent" (unless the
    // intent action is null, in which case it is always matched). For the purposes of receiving
    // local broadcast intents in this app, it is no harm to ensure that intents are not broadcast
    // with the wrong category, so requiring each category to have a defined list of supported
    // actions (for use when creating the "IntentFilter") makes things clearer. It also allows some
    // defensive checks in the "broadcast" methods that might highlight bugs in the code.
    private static final Map<String, String[]> ACTIONS_SUPPORTED_BY_CATEGORY
            = new HashMap<String, String[]>() {{
        put(CATEGORY_TIME_DATA_CHANGES, new String[] {
                ACTION_TIME_ADDED,
                ACTION_TIME_ADDED_MANUALLY,
                ACTION_TIMES_MODIFIED,
                ACTION_TIMES_MOVED_TO_HISTORY,
                ACTION_HISTORY_TIMES_SHOWN,
                ACTION_SESSION_TIMES_SHOWN,
                ACTION_COMMENT_ADDED
        });

        put(CATEGORY_ALG_DATA_CHANGES, new String[] {
                ACTION_ALGS_MODIFIED,
        });

        put(CATEGORY_UI_INTERACTIONS, new String[] {
                ACTION_TIME_SELECTED,
                ACTION_TIME_UNSELECTED,
                ACTION_DELETE_SELECTED_TIMES,
                ACTION_SELECTION_MODE_ON,
                ACTION_SELECTION_MODE_OFF,
                ACTION_TIMER_STARTED,
                ACTION_TIMER_STOPPED,
                ACTION_TOOLBAR_RESTORED,
                ACTION_GENERATE_SCRAMBLE,
                ACTION_SCRAMBLE_MODIFIED,
                ACTION_SCROLLED_PAGE,
                ACTION_CHANGED_CATEGORY,
                ACTION_CHANGED_THEME
        });
    }};

    /**
     * Private constructor to prevent instantiation of this class containing only constants and
     * utility methods.
     */
    private TTIntent() {
    }

    /**
     * A convenient wrapper for fragments that use a broadcast receiver that will only notify the
     * fragment of an intent when the fragment is currently added to its activity.
     */
    // NOTE: The goal of this class is to make a more obvious connection between the categories and
    // the fragments, as the category will be given in the code of the fragment class at the point
    // where it instantiates an instance of this class. It also simplifies
    public abstract static class TTFragmentBroadcastReceiver extends BroadcastReceiver {
        /**
         * The fragment that is receiving the broadcasts.
         */
        private final Fragment mFragment;

        /**
         * The intent category.
         */
        private final String mCategory;

        /**
         * Creates a new broadcast receiver to be used by a fragment. Matching broadcast intents
         * will only be notified to the fragment via {@link #onReceiveWhileAdded} if the fragment is
         * added to its activity at the time of the broadcast.
         *
         * @param fragment
         *     The fragment that will be receiving the broadcast intents.
         * @param category
         *     The category of
         */
        public TTFragmentBroadcastReceiver(Fragment fragment, String category) {
            mFragment = fragment;
            mCategory = category;
        }

        /**
         * Notifies the receiver of a matching broadcast intent that is received while the fragment
         * is added to its activity. The receiver will only be notified of intents that require the
         * category configured, or intents that require no category. (The latter is not a use-case
         * that is expected in this application.)
         *
         * @param context The context for the intent.
         * @param intent  The matching intent that was received.
         */
        public abstract void onReceiveWhileAdded(Context context, Intent intent);

        /**
         * Notifies the receiver of a matching broadcast intent. This implementation will call
         * {@link #onReceiveWhileAdded(Context, Intent)} only while the fragment is currently added
         * to its activity, otherwise the intent will be ignored.
         *
         * @param context The context for the intent.
         * @param intent  The matching intent that was received.
         */
        // Make this final to make sure extensions only override "onReceiveWhileAdded".
        @Override
        public final void onReceive(Context context, Intent intent) {
            if (mFragment.isAdded()) {
                if (DEBUG_ME) Log.d(TAG, mFragment.getClass().getSimpleName()
                        + ": onReceiveWhileAdded: " + intent);
                onReceiveWhileAdded(context, intent);
            }
        }

        /**
         * Gets the category of the intent actions that will be matched by this broadcast receiver.
         *
         * @return The category.
         */
        public String getCategory() {
            return mCategory;
        }
    }

    /**
     * Broadcasts an intent for the given category and action. To add more details to the intent
     * (via intent extras), use a {@link BroadcastBuilder}.
     *
     * @param category The category of the action.
     * @param action   The action.
     */
    public static void broadcast(String category, String action) {
        new BroadcastBuilder(category, action).broadcast();
    }

    /**
     * Registers a broadcast receiver. The receiver will only be notified of intents that require
     * the category given and only for the actions that are supported for that category. If the
     * receiver is used by a fragment, create an instance of {@link TTFragmentBroadcastReceiver}
     * and register it with the {@link #registerReceiver(TTFragmentBroadcastReceiver)} method
     * instead, as it will be easier to maintain.
     *
     * @param receiver
     *     The broadcast receiver to be registered.
     * @param category
     *     The category for the actions to be received. Must not be {@code null} and must be a
     *     supported category.
     *
     * @throws IllegalArgumentException
     *     If the category is {@code null}, or is not one of the supported categories.
     */
    public static void registerReceiver(BroadcastReceiver receiver, String category) {
        final String[] actions = ACTIONS_SUPPORTED_BY_CATEGORY.get(category);

        if (category == null || actions.length == 0) {
            throw new IllegalArgumentException("Category is not supported: " + category);
        }

        final IntentFilter filter = new IntentFilter();

        filter.addCategory(category);

        for (String action : actions) {
            // IntentFilter will only match Intents with one of these actions.
            filter.addAction(action);
        }

        LocalBroadcastManager.getInstance(TwistyTimer.getAppContext())
                .registerReceiver(receiver, filter);
    }

    /**
     * Registers a fragment broadcast receiver. The receiver will only be notified of intents that
     * require the category defined for the {@code TTFragmentBroadcastReceiver} and only for the
     * actions supported by that category.
     *
     * @param receiver The fragment broadcast receiver to be registered.
     *
     * @throws IllegalArgumentException
     *     If the receiver does not define the name of a supported category.
     */
    public static void registerReceiver(TTFragmentBroadcastReceiver receiver) {
        registerReceiver(receiver, receiver.getCategory());
    }

    /**
     * Unregisters a broadcast receiver. Any further broadcast intent will be ignored.
     *
     * @param receiver The receiver to be unregistered.
     */
    public static void unregisterReceiver(BroadcastReceiver receiver) {
        LocalBroadcastManager.getInstance(TwistyTimer.getAppContext()).unregisterReceiver(receiver);
    }

    /**
     * Gets the name of the puzzle type from an intent extra.
     *
     * @param intent The intent from which to get the puzzle type.
     * @return The puzzle type, or {@code null} if the intent does not specify a puzzle type.
     */
    public static String getPuzzleType(Intent intent) {
        return intent.getStringExtra(EXTRA_PUZZLE_TYPE);
    }

    /**
     * Gets the name of the puzzle subtype from an intent extra.
     *
     * @param intent The intent from which to get the puzzle subtype.
     * @return The puzzle subtype, or {@code null} if the intent does not specify a puzzle subtype.
     */
    public static String getPuzzleSubtype(Intent intent) {
        return intent.getStringExtra(EXTRA_PUZZLE_SUBTYPE);
    }

    /**
     * Gets the solve specified in an intent extra.
     *
     * @param intent The intent from which to get the solve.
     * @return The solve, or {@code null} if the intent does not specify a solve.
     */
    public static Solve getSolve(Intent intent) {
        final Parcelable solve = intent.getParcelableExtra(EXTRA_SOLVE);

        return solve == null ? null : (Solve) solve;
    }

    /**
     * Gets the scramble specified in an intent extra.
     *
     * @param intent The intent from which to get the scramble.
     * @return The scramble, or {@code null} if the intent does not specify a scramble.
     */
    public static String getScramble(Intent intent) {
        final String scramble = intent.getStringExtra(EXTRA_SCRAMBLE);

        return scramble;
    }


    /**
     * A builder for local broadcasts.
     *
     * @author damo
     */
    public static class BroadcastBuilder {
        /**
         * The intent that will be broadcast when building is complete.
         */
        private Intent mIntent;

        /**
         * Creates a new broadcast builder for the given intent category and action.
         *
         * @param category The category of the action. Must not be {@code null}.
         * @param action   The action. Must not be {@code null}.
         */
        public BroadcastBuilder(String category, String action) {
            mIntent = new Intent(action);
            mIntent.addCategory(category); // Will throw NPE if category is null, but that is OK.
        }

        /**
         * Broadcasts the intent configured by this builder.
         *
         * @throws IllegalStateException
         *     If the category specified on the intent does not support the defined action.
         */
        public void broadcast() {
            // For sanity, check that the category and action on the intent are supported. This
            // will unearth bugs in the code where actions have the wrong category and will not
            // end up where they are expected. Only do this if this is a debug build, as it would
            // be a waste of time in a release build.
            if (BuildConfig.DEBUG) {
                final String action = mIntent.getAction();

                if (action == null) {
                    throw new IllegalStateException("An intent action is expected.");
                }
                if (mIntent.getCategories().size() != 1) {
                    throw new IllegalStateException("Exactly one intent category is expected.");
                }

                final String category = mIntent.getCategories().iterator().next(); // First entry.
                final String[] actions = ACTIONS_SUPPORTED_BY_CATEGORY.get(category);

                if (!Arrays.asList(actions).contains(mIntent.getAction())) {
                    throw new IllegalStateException(
                            "Action '" + action + "' not allowed for category '" + category + "'.");
                }
            }

            LocalBroadcastManager.getInstance(TwistyTimer.getAppContext()).sendBroadcast(mIntent);
        }

        /**
         * Sets extras that identify the puzzle type and subtype related to the action of the
         * intent that will be broadcast. The receiver can retrieve the type and subtype by calling
         * {@link TTIntent#getPuzzleType(Intent)} and {@link TTIntent#getPuzzleSubtype(Intent)}.
         *
         * @param puzzleType    The name of the type of puzzle.
         * @param puzzleSubtype The name of the subtype of puzzle.
         *
         * @return {@code this} broadcast builder, allowing method calls to be chained.
         */
        public BroadcastBuilder puzzle(String puzzleType, String puzzleSubtype) {
            if (puzzleType != null) {
                mIntent.putExtra(EXTRA_PUZZLE_TYPE, puzzleType);
            }
            if (puzzleSubtype != null) {
                mIntent.putExtra(EXTRA_PUZZLE_SUBTYPE, puzzleSubtype);
            }

            return this;
        }

        /**
         * Sets an optional extra that identifies a solve time related to the action of the intent
         * that will be broadcast. The receiver can call {@link TTIntent#getSolve(Intent)} to
         * retrieve the solve from the intent.
         *
         * @param solve The solve to be added to the broadcast intent.
         *
         * @return {@code this} broadcast builder, allowing method calls to be chained.
         */
        public BroadcastBuilder solve(Solve solve) {
            if (solve != null) {
                // "Solve" implements "Parcelable" to allow it to be passed in an intent extra.
                mIntent.putExtra(EXTRA_SOLVE, solve);
            }

            return this;
        }

        /**
         * Sets an optional extra that identifies a scramble string related to the action of the intent
         * that will be broadcast. The receiver can call {@link TTIntent#getScramble(Intent)} to
         * retrieve the solve from the intent.
         *
         * @param scramble The scramble to be added to the broadcast intent.
         *
         * @return {@code this} broadcast builder, allowing method calls to be chained.
         */
        public BroadcastBuilder scramble(String scramble) {
            if (scramble != null) {
                // "Solve" implements "Parcelable" to allow it to be passed in an intent extra.
                mIntent.putExtra(EXTRA_SCRAMBLE, scramble);
            }

            return this;
        }
    }
}
