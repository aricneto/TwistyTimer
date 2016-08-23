package com.aricneto.twistytimer.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;

import com.aricneto.twistytimer.TwistyTimer;

/**
 * The actions for the broadcast intents that notify listeners of changes to the data or to the
 * state of the application.
 *
 * @author damo
 */
public final class TTIntent {
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
     * The category for intents that communicate application-level events.
     */
    public static final String CATEGORY_APP_LEVEL_EVENTS = CATEGORY_PREFIX + "APP_LEVEL_EVENTS";

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
     * The user has activate the "Back" button, or other UI control, to "go back" to the previous
     * activity or tab or to exit the application. The interpretation depends on the UI state at
     * the time of the action and different receivers will be configured to react appropriately in
     * each state.
     */
    public static final String ACTION_GO_BACK = ACTION_PREFIX + "GO_BACK";

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
     * The timer has been stopped.
     */
    public static final String ACTION_TIMER_STOPPED = ACTION_PREFIX + "TIMER_STOPPED";

    /**
     * One or more algorithms has been added, deleted or otherwise modified.
     */
    public static final String ACTION_ALGS_MODIFIED = ACTION_PREFIX + "ALGS_MODIFIED";

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
     * Broadcasts an intent for the given category and action.
     *
     * @param category The category of the action.
     * @param action   The action.
     */
    public static void broadcast(String category, String action) {
        final Intent intent = new Intent(action);

        intent.addCategory(category);
        LocalBroadcastManager.getInstance(TwistyTimer.getAppContext()).sendBroadcast(intent);
    }

    /**
     * Registers a broadcast receiver. The receiver will only be notified of intents that require
     * the category given, or intents that require no category. (The latter is not a use-case that
     * is expected in this application.) If the receiver is used by a fragment, create an instance
     * of {@link TTFragmentBroadcastReceiver} and register it with the
     * {@link #registerReceiver(TTFragmentBroadcastReceiver)} method instead, as it will be easier
     * to maintain.
     *
     * @param receiver The fragment broadcast receiver to be registered.
     */
    public static void registerReceiver(BroadcastReceiver receiver, String category) {
        final IntentFilter filter = new IntentFilter();

        filter.addCategory(category);
        LocalBroadcastManager.getInstance(TwistyTimer.getAppContext())
                .registerReceiver(receiver, filter);
    }

    /**
     * Registers a fragment broadcast receiver. The receiver will only be notified of intents that
     * require the category defined for the {@code TTFragmentBroadcastReceiver}, or intents that
     * require no category. (The latter is not a use-case that is expected in this application.)
     *
     * @param receiver The fragment broadcast receiver to be registered.
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
}
