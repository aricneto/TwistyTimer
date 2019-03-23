package com.aricneto.twistytimer.stats;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.utils.PuzzleUtils;
import com.aricneto.twistytimer.utils.TTIntent;
import com.aricneto.twistytimer.utils.Wrapper;

import java.util.Objects;

import androidx.loader.content.AsyncTaskLoader;

import static com.aricneto.twistytimer.utils.TTIntent.ACTION_HISTORY_TIMES_SHOWN;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_SESSION_TIMES_SHOWN;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_TIMES_MODIFIED;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_TIMES_MOVED_TO_HISTORY;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_TIME_ADDED;

/**
 * <p>
 * A loader used to populate a {@link ChartStatistics} object from the database for use in the main
 * chart in the {@link com.aricneto.twistytimer.fragment.TimerGraphFragment}.
 * </p>
 * <p>
 * See the class description for {@link StatisticsLoader} for more details, as that loader behaves
 * in the same way as this loader.
 * </p>
 * <p>
 * A different between this loader and the {@code StatisticsLoader}, is that this loader may return
 * a different object inside the {@code Wrapper}. When the selection changes to or from the history
 * of all solve times and the current session solve times, a full re-load is required and a new
 * {@code ChartStatistics} object will be created.
 * </p>
 *
 * @author damo
 */
public class ChartStatisticsLoader extends AsyncTaskLoader<Wrapper<ChartStatistics>> {
    /**
     * Flag to enable debug logging from this class.
     */
    private static final boolean DEBUG_ME = false;

    /**
     * A "tag" used to identify this class as the source of log messages.
     */
    private static final String TAG = ChartStatisticsLoader.class.getSimpleName();

    /**
     * The chart style information that will be used to set the labels, colors and other styles on
     * the data sets created to present the chart statistics.
     */
    private final ChartStyle mChartStyle;

    /**
     * The cached chart statistics that have been loaded previously. This reference will be reset to
     * {@code null} if the data changes and the data will need to be loaded again when requested.
     */
    private ChartStatistics mChartStats;

    /**
     * A wrapper around the loaded chart statistics. This is required to allow the same
     * {@code ChartStatistics} instance to be used for successive loads while ensuring that the
     * {@code LoaderManager} sees a different object delivered, otherwise it would not invoke
     * {@code onLoadFinished} on the activity or fragment waiting for the updated data.
     */
    private Wrapper<ChartStatistics> mLoadedData;

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
        private final ChartStatisticsLoader mLoader;

        /**
         * Creates a new broadcast receiver that will notify a loader of changes to the solve
         * time data.
         *
         * @param loader The loader to be notified of changes to the solve times.
         */
        TimeDataChangedReceiver(ChartStatisticsLoader loader) {
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
            if (DEBUG_ME) Log.d(TAG, "onReceive: " + intent); // Later log messages are indented.

            // Calls to "mLoader.onContentChanged" will trigger a new call to "onStartLoading",
            // which will result in a call to "deliverResult" (or to "forceLoad", then
            // "loadInBackground", then "deliverResult") which will cause the "LoaderManager" to
            // call "onLoadFinished" on the owning Fragment or Activity ... if the delivered result
            // is not the same object as in the previous delivery (hence the Wrapper).
            //
            // NOTE: The default implementation of "onContentChanged" will only force a re-load if
            // the loader is currently started (i.e., in use by a live fragment). If the loader is
            // not started, a reload will not occur until the next time it is restarted (which
            // might never happen). The test of "takeContentChanged()" in "onStartLoading" picks
            // up on any such deferred reloading task.

            final Intent finalIntent = intent;

            switch (Objects.requireNonNull(intent.getAction())) {
                case ACTION_TIME_ADDED:
                    if (!mLoader.deliverQuickResult(intent)) {
                        // Could not deliver a quick update, so trigger a full re-load instead.
                        if (DEBUG_ME) Log.d(TAG, "  Quick update not possible. Will reload!");
                        mLoader.onContentChanged();
                    }
                    break;

                case ACTION_TIMES_MOVED_TO_HISTORY:
                    // If session times were moved to the history, AND THE FULL HISTORY IS BEING
                    // SHOWN, then the chart is already up-to-date, as the charted full history of
                    // times already includes the times from the current session. If the chart is
                    // showing only the current session, then a full re-load is required. (There
                    // is no check if the current session is already empty, as that would just be
                    // an over-complication and the database re-load will be very quick, anyway.)
                    if (mLoader.isForCurrentSessionOnly()) {
                        // The chart is showing only session times and these have moved, so re-load.
                        if (DEBUG_ME) Log.d(TAG, "  Session moved to history. Will reload!");
                        mLoader.onContentChanged();
                    } // else the chart is already up to date.
                    break;

                case ACTION_TIMES_MODIFIED:
                    // If other unspecified modifications were made (e.g., deletions, changes to
                    // penalties, etc.), a full re-load will be needed.
                    if (DEBUG_ME) Log.d(TAG, "  Unknown changes. Will reload!");
                    mLoader.onContentChanged();
                    break;

                case ACTION_SESSION_TIMES_SHOWN:
                    if (mLoader.resetForSelection(true)) {
                        if (DEBUG_ME) Log.d(TAG, "  Changed to session times. Will reload!");
                        mLoader.onContentChanged();
                    } // else was already showing "current session" chart, so do nothing.
                    break;

                case ACTION_HISTORY_TIMES_SHOWN:
                    if (mLoader.resetForSelection(false)) {
                        if (DEBUG_ME) Log.d(TAG, "  Changed to history times. Will reload!");
                        mLoader.onContentChanged();
                    } // else was already showing "full history" chart, so do nothing.
                    break;
            }
        }
    }

    /**
     * Creates a new loader for the solve time chart statistics. The given chart statistics define
     * the set of "average-of-N" calculations to be made. The statistics may relate to the current
     * session only, or to all past and current sessions.
     *
     * @param context
     *     The context for this loader. This may be an application context.
     * @param chartStyle
     *     The {@link ChartStyle} defining the styles to be applied to the data sets in the loaded
     *     chart statistics. Must not be {@code null}.
     * @param puzzleType
     *     The name of the puzzle type for which statistics are required. See the {@code TYPE_*}
     *     constants in {@link PuzzleUtils}.
     * @param puzzleSubtype
     *     The name of the puzzle subtype.
     * @param isForCurrentSessionOnly
     *     {@code true} if the chart statistics should be compiled initially only for the solve
     *     times of the current session; or {@code false} if all times from all past and current
     *     sessions should be used. The loader will listen to messages broadcasting changes to this
     *     selection and will deliver updated statistics if any change is detected.
     */
    public ChartStatisticsLoader(Context context, ChartStyle chartStyle,
                                 String puzzleType, String puzzleSubtype,
                                 boolean isForCurrentSessionOnly) {
        super(context);

        if (DEBUG_ME) Log.d(TAG, "Created new Loader for ChartStatistics!");

        // NOTE: "ChartStyle" must be initialised from an "Activity" context, as it needs to access
        // theme attributes. Therefore, it cannot be instantiated here, as the given context may be
        // an "Application" context, which cannot access theme attributes. It is the responsibility
        // of the Activity or Fragment that creates this loader to instantiate the ChartStyle with
        // the necessary context before creating the loader. ChartStyle will not hold a reference
        // to the context, so no memory leaks should occur. However, holding a reference to an
        // Activity context from this Loader would be a really bad idea.
        mChartStyle = chartStyle;
        mPuzzleType = puzzleType;
        mPuzzleSubtype = puzzleSubtype;

        resetForSelection(isForCurrentSessionOnly);
    }

    /**
     * Resets the chart statistics if the "current session only" flag has changed. If the chart
     * statistics have not been created before, they will be created now. If new chart statistics
     * are created, the "wrapper" content will be set to {@code null} to indicate that the new
     * chart statistics have not yet been loaded.
     *
     * @param isForCurrentSessionOnly
     *     {@code true} if the chart statistics should be compiled initially only for the solve
     *     times of the current session; or {@code false} if all times from all past and current
     *     sessions should be used. The loader will listen to messages broadcasting changes to this
     *     selection and will deliver updated statistics if any change is detected.
     *
     * @return
     *     {@code true} if the selection for the solve times was changed and new chart statistics
     *     were created (and will need to be loaded from the database); or {@code false} if the
     *     selection is the same as that already loaded (and the old chart statistics are still
     *     valid).
     */
    private boolean resetForSelection(boolean isForCurrentSessionOnly) {
        if (mChartStats == null
                || mChartStats.isForCurrentSessionOnly() != isForCurrentSessionOnly) {
            if (DEBUG_ME) Log.d(TAG, "resetForSelection(): Creating new ChartStatistics");
            // Unlike the "StatisticsLoader", the chart statistics cannot load statistics for all
            // times and for the current session in the same instance.
            mChartStats = isForCurrentSessionOnly
                    ? ChartStatistics.newCurrentSessionChartStatistics(mChartStyle)
                    : ChartStatistics.newAllTimeChartStatistics(mChartStyle);

            // Wrapper remains empty until the first load is attempted. This allows a distinction
            // to be made between "was not loaded" and "was loaded, but the database had no times".
            mLoadedData = Wrapper.wrap(null);

            return true;
        }

        // Selection did not change.
        return false;
    }

    /**
     * Indicates if the currently-loaded chart statistics are for the current session only, or if
     * they cover the full history of all times from all sessions.
     *
     * @return
     *     {@code true} if the loaded chart statistics cover only the solve times for the current
     *     session; or {@code false} if the full history of times is loaded. If no statistics have
     *     been loaded, the result will be {@code true}.
     */
    private boolean isForCurrentSessionOnly() {
        // NOTE: "true" if nothing is loaded, as it suits the case for ACTION_TIMES_MOVED_TO_HISTORY
        // in the broadcast receiver. If "true", it will trigger a re-load, which makes sense.
        return mLoadedData.isEmpty() || mChartStats.isForCurrentSessionOnly();
    }

    /**
     * Attempts a quick update of the chart statistics without resorting to a full read of the
     * database. If the statistics were previously read from the database, then a single new time,
     * added for the current session, can be added directly to the statistics and the update can be
     * delivered to the activity or fragment.
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
                    mChartStats.addDNF(solve.getDate());
                } else {
                    mChartStats.addTime(solve.getTime(), solve.getDate());
                }

                mLoadedData = mLoadedData.rewrap(); // See explanation in "loadInBackground".

                if (DEBUG_ME) Log.d(TAG, "  Delivering quick update to chart statistics!");
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
            if (DEBUG_ME) Log.d(TAG, "  Delivering available ChartStatistics...");
            deliverResult(mLoadedData);
        }

        // If not already listening for changes to the time data, start listening now. If any
        // pertinent change is detected (i.e., one that would impact on the validity of the
        // statistics), the statistics will need to be updated or reloaded.
        if (mTimeDataChangedReceiver == null) {
            if (DEBUG_ME) Log.d(TAG, "  Starting monitoring of changes affecting ChartStatistics.");
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
        mChartStats.reset();

        if (mTimeDataChangedReceiver != null) {
            if (DEBUG_ME) Log.d(TAG, "  Stopping monitoring of changes affecting ChartStatistics.");
            // Receiver will be re-registered in "onStartLoading", if that is called again.
            TTIntent.unregisterReceiver(mTimeDataChangedReceiver);
            mTimeDataChangedReceiver = null;
        }
    }

    /**
     * Loads the chart statistics from the database on a background thread.
     *
     * @return The loaded chart statistics.
     */
    @Override
    public Wrapper<ChartStatistics> loadInBackground() {
        @SuppressWarnings("UnusedAssignment") // For when "DEBUG_ME" is false.
        long startTime = 0L;
        if (DEBUG_ME) { Log.d(TAG, "loadInBackground"); startTime = SystemClock.elapsedRealtime(); }

        // This is a full, clean load, so clear out the results from the previous load.
        mChartStats.reset();

        // TODO: Add support for cancellation: add a call-back to "populateChartStatistics", so
        // it can poll the cancellation status as it iterates over the solves it reads from the
        // database.
        TwistyTimer.getDBHandler().populateChartStatistics(
                mPuzzleType, mPuzzleSubtype, mChartStats);

        if (DEBUG_ME)
            Log.d(TAG, String.format("  Loaded ChartStatistics in %,d ms.",
                    SystemClock.elapsedRealtime() - startTime));

        // If this is not the first time loading the data, a different object must be returned if
        // the "LoaderManager" is to trigger "onLoadFinished" (go figure). As "mChartStats" is
        // still the same object, a new wrapper around that object is created instead to trick
        // "LoaderManager" into doing what is expected.
        return mLoadedData = Wrapper.wrap(mChartStats); // Old content may have been null.
    }
}
