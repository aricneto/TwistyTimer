package com.aricneto.twistytimer.stats;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import androidx.loader.content.AsyncTaskLoader;
import android.util.Log;
import android.widget.Toast;

import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.utils.PuzzleUtils;
import com.aricneto.twistytimer.utils.TTIntent;
import com.aricneto.twistytimer.utils.Wrapper;

import static com.aricneto.twistytimer.utils.TTIntent.ACTION_STATISTICS_LOADED;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_TIMES_MODIFIED;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_TIMES_MOVED_TO_HISTORY;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_TIME_ADDED;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_TIME_ADDED_MANUALLY;
import static com.aricneto.twistytimer.utils.TTIntent.CATEGORY_TIME_DATA_CHANGES;
import static com.aricneto.twistytimer.utils.TTIntent.broadcast;

/**
 * <p>
 * A loader used to populate a {@link Statistics} object from the database for use in the timer
 * and timer graph (statistics table) fragments. The main timer fragment will manage the listener
 * and notify its subordinate fragments of any updates.
 * </p>
 * <p>
 * It is expected that the fragment will be destroyed and recreated if the puzzle type or subtype
 * are changed. However, the fragment may also be destroyed and recreated for other reasons, such
 * as a configuration change when a user changes the device orientation. If a fragment is using
 * this loader and is destroyed and re-created in the context of the same activity, the loader
 * instance created by the previous fragment may be returned to the new fragment. If the puzzle
 * type and subtype passed to the fragment are the same, {@code LoaderManager.initLoader} can be
 * called to reuse the old loader and its loaded statistics (i.e., {@code onCreateLoader} may not
 * be called on the fragment). However, if the puzzle type or subtype have changed,
 * {@code LoaderManager.restartLoader} must be called to create a new loader with the correct
 * puzzle type and subtype, which forces a call of {@code onCreateLoader} on the fragment. It is
 * the responsibility of the fragment to call {@code initLoader} or {@code restartLoader}. If in
 * doubt, however, just call the latter method always; or the former method, but call
 * {@code LoaderManager.destroyLoader} from {@code Fragment.onDetach}.
 * </p>
 * <p>
 * A {@link Wrapper} is required around the loaded {@code Statistics} to ensure that the loader
 * manager delivers updates via {@code onLoadFinished}. The same {@code Statistics} instance is
 * delivered each time, but the loader manager will only deliver the object if it is different
 * from the previous one delivered. Creating a new {@code Wrapper} instance around the same
 * {@code Statistics} instance for each delivery works around that behaviour.
 * </p>
 *
 * @author damo
 */
public class StatisticsLoader extends AsyncTaskLoader<Wrapper<Statistics>> {
    /**
     * Flag to enable debug logging from this class.
     */
    private static final boolean DEBUG_ME = false;

    /**
     * A "tag" used to identify this class as the source of log messages.
     */
    private static final String TAG = StatisticsLoader.class.getSimpleName();

    /**
     * The cached statistics that have been loaded previously. This reference will be reset to
     * {@code null} if the data changes and the data will need to be loaded again when requested.
     */
    private final Statistics mStatistics;

    /**
     * A wrapper around the loaded statistics. This is required to allow the same {@code Statistics}
     * instance to be used for every load while ensuring that the {@code LoaderManager} sees a
     * different object delivered, otherwise it would not invoke {@code onLoadFinished} on the
     * activity or fragment waiting for the updated data.
     */
    private Wrapper<Statistics> mLoadedData;

    /**
     * The puzzle type. This is fixed for the lifetime of the loader instance.
     */
    private final String mPuzzleType;

    /**
     * The puzzle subtype. This is fixed for the lifetime of the loader instance.
     */
    private final String mPuzzleSubtype;

    /**
     * The broadcast receiver that is notified of changes to the solve time data.
     */
    private BroadcastReceiver mTimeDataChangedReceiver;

    /**
     * A broadcast receiver that is notified of changes to the solve time data.
     */
    private static class TimeDataChangedReceiver extends BroadcastReceiver {
        /**
         * The loader to be notified of changes to the solve time data.
         */
        private final StatisticsLoader mLoader;

        /**
         * Creates a new broadcast receiver that will notify a loader of changes to the solve
         * time data.
         *
         * @param loader The loader to be notified of changes to the solve times.
         */
        TimeDataChangedReceiver(StatisticsLoader loader) {
            mLoader = loader;
        }

        /**
         * Receives notification of a change to the solve time data and notifies the loader if the
         * change is pertinent and a new load is required.
         *
         * @param context The context of the receiver.
         * @param intent  The intent detailing the action that has occurred.
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG_ME) Log.d(TAG, "onReceive: " + intent);
            switch (intent.getAction()) {
                case ACTION_TIME_ADDED:
                    if (!mLoader.deliverQuickResult(intent)) {
                        if (DEBUG_ME) Log.d(TAG, "  Quick update not possible. Will reload!");
                        mLoader.onContentChanged();
                    } // else updated statistics have been delivered without a full re-load.
                    break;

                case ACTION_TIMES_MODIFIED:
                case ACTION_TIMES_MOVED_TO_HISTORY:
                    // If times were moved to the history or other unspecified modifications were
                    // made (e.g., deletions, changes to penalties, etc.), then "mStatistics"
                    // cannot be simply updated. A full re-load will be needed.
                    //
                    // NOTE: The default implementation of "onContentChanged" will only force a
                    // re-load if the loader is currently started (i.e., in use by a live fragment).
                    // If the loader is not started, a reload will not occur until the next time it
                    // is restarted (which might never happen). The test of "takeContentChanged()"
                    // in "onStartLoading" picks up on any such deferred reloading task.
                    if (DEBUG_ME) Log.d(TAG, "  Unknown changes or history toggle. Will reload!");
                    mLoader.onContentChanged();
                    break;

                // Switching between the history of all times and the session times does
                // not affect the statistics table, which always shows statistics for both.
                // case ACTION_HISTORY_TIMES_SHOWN:
                // case ACTION_SESSION_TIMES_SHOWN:
            }
        }
    }

    /**
     * Creates a new loader for the solve time statistics. The given statistics define the set of
     * "average-of-N" calculations to be made.
     *
     * @param context
     *     The context for this loader. This may be an application context.
     * @param statistics
     *     The {@code Statistics} instance to be populated from the solve times loaded from the
     *     database. Any existing statistics will be reset before loading new statistics. Must not
     *     be {@code null}.
     * @param puzzleType
     *     The name of the puzzle type for which statistics are required. See the {@code TYPE_*}
     *     constants in {@link PuzzleUtils}.
     * @param puzzleSubtype
     *     The name of the puzzle subtype.
     */
    public StatisticsLoader(Context context, Statistics statistics,
                            String puzzleType, String puzzleSubtype) {
        super(context);

        if (DEBUG_ME) Log.d(TAG, "Created new Loader for Statistics!");

        mStatistics = statistics;
        mPuzzleType = puzzleType;
        mPuzzleSubtype = puzzleSubtype;

        mStatistics.reset(); // NPE if null is OK, as it is documented above.
        // Wrapper remains empty until the first load is attempted.
        mLoadedData = Wrapper.wrap(null);
    }

    /**
     * Attempts a quick update of the statistics without resorting to a full read of the database.
     * If the statistics were previously read from the database, then a single new time, added for
     * the current session, can be added directly to the statistics and the update can be delivered
     * to the activity or fragment.
     *
     * @param intent
     *     The intent that may contain details of a new solve time.
     *
     * @return
     *     {@code true} if the statistics were up-to-date with respect to the database and the
     *     intent contained a new solve time that was added to the statistics directly, avoiding
     *     the need for a full database re-load; or {@code false} if a full database reload will
     *     still be required to update the statistics.
     */
    private boolean deliverQuickResult(Intent intent) {
        if (!mLoadedData.isEmpty()) {
            // All statistics were loaded previously from the database (because the wrapper is not
            // empty), so try a quick update.
            final Solve solve = TTIntent.getSolve(intent);

            if (solve != null) {
                if (solve.getPenalty() == PuzzleUtils.PENALTY_DNF) {
                    mStatistics.addDNF(true);
                } else {
                    mStatistics.addTime(solve.getTime(), true);
                }

                mLoadedData = mLoadedData.rewrap(); // See explanation in "loadInBackground".

                if (DEBUG_ME) Log.d(TAG, "  Delivering quick update to statistics!");
                deliverResult(mLoadedData); // Will trigger "onLoadFinished" in Fragment/Activity.

                return true;
            }
        }

        return false;
    }

    /**
     * Starts loading the statistics from the database. If statistics were previously loaded, they
     * will be re-delivered. If the statistics that were so delivered are out of date, or if no
     * statistics were available for immediate delivery, a new full re-load of the statistics from
     * the database will be triggered and deliver will occur once that background task completes.
     */
    @Override
    protected void onStartLoading() {
        if (DEBUG_ME) Log.d(TAG, "onStartLoading");

        if (!mLoadedData.isEmpty()) {
            // If statistics are available, deliver them now (even if they are not up-to-date).
            if (DEBUG_ME) Log.d(TAG, "  Delivering available Statistics...");
            deliverResult(mLoadedData);
        }

        // If not already listening for changes to the time data, start listening now. If any
        // pertinent change is detected (i.e., one that would impact on the validity of the
        // statistics), the statistics will need to be updated or reloaded.
        if (mTimeDataChangedReceiver == null) {
            if (DEBUG_ME) Log.d(TAG, "  Starting monitoring of changes affecting Statistics.");
            mTimeDataChangedReceiver = new TimeDataChangedReceiver(this);
            // Register here and unregister in "onReset()".
            TTIntent.registerReceiver(
                    mTimeDataChangedReceiver, TTIntent.CATEGORY_TIME_DATA_CHANGES);
        }

        // If any pertinent change was detected by the receiver, or if no statistics have been
        // loaded, then perform a full load from the database now.
        if (takeContentChanged() || mLoadedData.isEmpty()) {
            if (DEBUG_ME) Log.d(TAG, "  forceLoad() called...");
            forceLoad();
            commitContentChanged();
        }
    }

    @Override
    protected void onReset() {
        if (DEBUG_ME) Log.d(TAG, "onReset");

        super.onReset();

        // "Unload" any previously loaded statistics, so a full re-load will happen the next time.
        mLoadedData = Wrapper.wrap(null); // "null" flags "not loaded" state.
        mStatistics.reset();

        if (mTimeDataChangedReceiver != null) {
            if (DEBUG_ME) Log.d(TAG, "  Stopping monitoring of changes affecting Statistics.");
            // Receiver will be re-registered in "onStartLoading", if that is called again.
            TTIntent.unregisterReceiver(mTimeDataChangedReceiver);
            mTimeDataChangedReceiver = null;
        }
    }

    /**
     * Loads the statistics from the database on a background thread.
     *
     * @return The loaded statistics.
     */
    @Override
    public Wrapper<Statistics> loadInBackground() {
        @SuppressWarnings("UnusedAssignment") // For when "DEBUG_ME" is false.
        long startTime = 0L;
        if (DEBUG_ME) { Log.d(TAG, "loadInBackground"); startTime = SystemClock.elapsedRealtime(); }

        // This is a full, clean load, so clear out the results from the previous load.
        mStatistics.reset();

        // TODO: Add support for cancellation: add a call-back to "populateStatistics", so it can
        // poll the cancellation status as it iterates over the solves it reads from the database.
        TwistyTimer.getDBHandler().populateStatistics(mPuzzleType, mPuzzleSubtype, mStatistics);

        if (DEBUG_ME) {
            Log.d(TAG, String.format("  Loaded Statistics in %,d ms.",
                                     SystemClock.elapsedRealtime() - startTime));
            new TTIntent.BroadcastBuilder(CATEGORY_TIME_DATA_CHANGES, ACTION_STATISTICS_LOADED)
                    .longValue(SystemClock.elapsedRealtime() - startTime)
                    .broadcast();
        }

        // If this is not the first time loading the data, a different object must be returned if
        // the "LoaderManager" is to trigger "onLoadFinished" (go figure). As "mStatistics" is
        // still the same object, a new wrapper around that object is created instead to trick
        // "LoaderManager" into doing what is expected.
        return mLoadedData = Wrapper.wrap(mStatistics); // Old content may have been null.
    }
}
