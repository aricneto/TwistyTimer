package com.aricneto.twistytimer.fragment;


import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.spans.TimeFormatter;
import com.aricneto.twistytimer.utils.ChartStatistics;
import com.aricneto.twistytimer.utils.Statistics;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;

import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

import static com.aricneto.twistytimer.utils.AverageCalculator.tr;
import static com.aricneto.twistytimer.utils.PuzzleUtils.convertTimeToString;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link TimerGraphFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TimerGraphFragment extends Fragment {

    private static final String PUZZLE         = "puzzle";
    private static final String PUZZLE_SUBTYPE = "puzzle_type";
    private static final String HISTORY        = "history";

    private String  currentPuzzle;
    private String  currentPuzzleSubtype;

    // The context used by the AsyncTask instances. This is set to null when the Fragment is
    // detached, to notify any still-running tasks to skip any UI changes when done. It would
    // probably be much better to replace those tasks with "Loader" instances, which could be
    // better integrated into the life-cycle of the fragment.
    private volatile Context mContext;

    @Bind(R.id.linechart)           LineChart lineChartView;
    @Bind(R.id.personalBestTimes)   TextView  personalBestTimes;
    @Bind(R.id.sessionBestTimes)    TextView  sessionBestTimes;
    @Bind(R.id.sessionCurrentTimes) TextView  sessionCurrentTimes;
    @Bind(R.id.progressSpinner)     MaterialProgressBar progressBar;

    // Things that must be hidden/shown when refreshing the card.
    // The names don't matter since we're just going to show/hide them anyway
    @Bind(R.id.personalBestTitle)   View v1;
    @Bind(R.id.sessionBestTitle)    View v2;
    @Bind(R.id.sessionCurrentTitle) View v3;
    @Bind(R.id.horizontalDivider02) View v4;
    @Bind(R.id.verticalDivider02)   View v5;
    @Bind(R.id.verticalDivider03)   View v6;

    private boolean history;

    /**
     * Indicates if the generation of the chart has been deferred because the fragment is not
     * visible. Once the fragment becomes visible, the chart should be generated to ensure its is
     * up to date.
     */
    private boolean mIsGenerateChartDeferred;

    /**
     * Indicates if the calculation of the statistics for the statistics table has been deferred
     * because the fragment is not visible. Once the fragment becomes visible, the statistics
     * should be calculated to ensure they are up to date.
     */
    private boolean mIsCalculateStatsDeferred;

    // Receives broadcasts from the timer
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isAdded()) { // The fragment has to check if it is attached to an activity. Removing this will bug the app
                switch (intent.getStringExtra("action")) {
                    case "MOVED TO HISTORY":
                    case "TIME UPDATED":
                    case "REFRESH TIME":
                    case "TIME ADDED":
                        // The full history chart includes the times for the current session, so if
                        // a new time is added to the current session, the chart needs to be updated
                        // even it is showing the all-times "history".
                        generateChart();
                        calculateStats();
                        break;

                    case "HISTORY":
                        history = ! history;
                        generateChart();
                        // Switching between the "history" of all times and the session times does
                        // not affect the statistics table, which always shows statistics for both.
                        break;
                }
            }
        }
    };

    public TimerGraphFragment() {
        // Required empty public constructor
    }

    // We have to put a boolean history here because it resets when we change puzzles.
    public static TimerGraphFragment newInstance(String puzzle, String puzzleType, boolean history) {
        TimerGraphFragment fragment = new TimerGraphFragment();
        Bundle args = new Bundle();
        args.putString(PUZZLE, puzzle);
        args.putBoolean(HISTORY, history);
        args.putString(PUZZLE_SUBTYPE, puzzleType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentPuzzle = getArguments().getString(PUZZLE);
            currentPuzzleSubtype = getArguments().getString(PUZZLE_SUBTYPE);
            history = getArguments().getBoolean(HISTORY);
        }
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mReceiver, new IntentFilter("TIMELIST"));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_timer_graph, container, false);
        ButterKnife.bind(this, root);

        // The context used by the AsyncTasks must only be set when the fragment view exists. It
        // must be reset to null in "onDestroyView".
        mContext = getContext();

        // Setting for landscape mode
        if (getActivity().getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE) {
            root.post(new Runnable() {
                @Override
                public void run() {
                    if (container != null) {
                        LineChart.LayoutParams params = lineChartView.getLayoutParams();
                        params.height = container.getHeight();
                        lineChartView.setLayoutParams(params);
                        lineChartView.requestLayout();
                        root.findViewById(R.id.graphScroll).setScrollY(params.height / 2);
                    }
                }
            });
        }

        // The color for the text in the legend and for the values along the chart's axes.
        final int chartTextColor = Color.WHITE;

        lineChartView.setPinchZoom(true);
        lineChartView.setBackgroundColor(Color.TRANSPARENT);
        lineChartView.setDrawGridBackground(false);
        lineChartView.getAxisRight().setEnabled(false);
        lineChartView.getLegend().setTextColor(chartTextColor);
        lineChartView.setDescription("");

        final YAxis axisLeft = lineChartView.getAxisLeft();
        final XAxis xAxis = lineChartView.getXAxis();
        final int axisColor = ContextCompat.getColor(mContext, R.color.white_secondary_icon);

        xAxis.setDrawGridLines(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setAxisLineColor(axisColor);
        xAxis.setDrawAxisLine(true);
        xAxis.setTextColor(chartTextColor);
        xAxis.setAvoidFirstLastClipping(true);

        axisLeft.setDrawLimitLinesBehindData(true);
        axisLeft.setDrawGridLines(true);
        axisLeft.setDrawAxisLine(true);
        axisLeft.setTextColor(chartTextColor);
        axisLeft.enableGridDashedLine(10f, 8f, 0f);
        axisLeft.setAxisLineColor(axisColor);
        axisLeft.setGridColor(axisColor);
        axisLeft.setValueFormatter(new TimeFormatter());

        // Launch the background tasks to load the data for the chart and statistics. If this
        // fragment is not visible, these tasks will be deferred automatically.
        generateChart();
        calculateStats();

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Any background tasks can update the UI as long as the fragment is attached to an
        // activity context and has a fragment view. Once the view goes away, the context must be
        // reset to null to ensure "AsyncTask.onPostExecute" methods do not attempt to change the
        // UI after it is gone. This prevents crashes that happen if the activity is exited just
        // after a background task starts.
        //
        // Using a "Loader" would probably be better, but this small fix will do for now.
        mContext = null;
    }

    public void onDetach() {
        super.onDetach();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mReceiver);
    }

    /**
     * Notifies the fragment that it has become visible to the user. This is expected to be called
     * by the {@code ViewPager} as the user changes tabs. Take care, as this method may be called
     * outside of the fragment lifecycle.
     *
     * @param isVisibleToUser
     *     {@code true} if the fragment has become visible to the user; or {@code false} if it is
     *     not visible to the user.
     */
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser) {
            // If "generateChart" or "calculateStats" was called when the fragment was not visible
            // (i.e., when the tab was not selected), then the respective "mIs...Deferred" flag
            // will have been raised to avoid performing unnecessary actions. However, now that the
            // fragment has become visible, re-call those methods to execute the deferred actions.
            if (mIsGenerateChartDeferred) {
                generateChart();
            }

            if (mIsCalculateStatsDeferred) {
                calculateStats();
            }
        } // else switching away from this tab. (TODO: could cancel any still-running tasks.)
    }

    private void calculateStats() {
        // (Re)calculate the statistics and display them.
        if (getUserVisibleHint()) {
            // If the fragment is visible (i.e., if the tab is selected), perform the action.
            new CalculateStats().execute();
            mIsCalculateStatsDeferred = false; // Any deferred action has now been executed.
        } else {
            // If the fragment is not visible (i.e., if the tab is not selected), defer the action.
            // This avoids overloading the system at the same time as, say, the timer tab is trying
            // to generate a scramble. It should help to make the start-up of the application a bit
            // snappier. Once the fragment becomes visible, "setUserVisibleHint" will call this
            // method again to perform the deferred action.
            mIsCalculateStatsDeferred = true;
        }
    }

    private void generateChart() {
        // (Re)generate the chart and display it. Same approach as in "calculateStats".
        if (getUserVisibleHint()) {
            new GenerateSolveList().execute();
            mIsGenerateChartDeferred = false;
        } else {
            mIsGenerateChartDeferred = true;
        }
    }

    private void toggleCardStats(int visibility) {
        v1.setVisibility(visibility);
        v2.setVisibility(visibility);
        v3.setVisibility(visibility);
        v4.setVisibility(visibility);
        v5.setVisibility(visibility);
        v6.setVisibility(visibility);

        personalBestTimes.setVisibility(visibility);
        sessionBestTimes.setVisibility(visibility);
        sessionCurrentTimes.setVisibility(visibility);
    }

    /**
     * Generate a list of solves for the chart
     */
    private class GenerateSolveList extends AsyncTask<Void, Void, ChartStatistics> {
        @Override
        protected ChartStatistics doInBackground(Void... voids) {
            final Context context = mContext; // Copy the field in case it changes on the UI thread.

            if (context != null) {
                final ChartStatistics chartStats
                        = history ? ChartStatistics.newAllTimeChartStatistics(context)
                                  : ChartStatistics.newCurrentSessionChartStatistics(context);

                TwistyTimer.getDBHandler().populateChartStatistics(
                        currentPuzzle, currentPuzzleSubtype, chartStats);

                return chartStats;
            }

            return null;
        }

        @Override
        protected void onPostExecute(ChartStatistics chartStats) {
            if (mContext == null || chartStats == null) {
                // If there is no context, the UI has gone away since the task started. Do nothing.
                return;
            }

            // Add all times line, best times line, average-of-N times lines (with highlighted
            // best AoN times) and mean limit line and identify them all using a custom legend.
            chartStats.applyTo(lineChartView);

            // Animate and refresh the chart.
            lineChartView.animateY(1_000);
        }
    }

    private class CalculateStats extends AsyncTask<Void, Void, Statistics> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if (mContext != null) {
                progressBar.setVisibility(View.VISIBLE);
                toggleCardStats(View.GONE);
            }
        }

        @Override
        protected Statistics doInBackground(Void... voids) {
            final Context context = mContext; // Copy the field in case it changes on the UI thread.

            if (context != null) {
                final Statistics stats = Statistics.newAllTimeStatistics();

                TwistyTimer.getDBHandler().populateStatistics(
                        currentPuzzle, currentPuzzleSubtype, stats);

                return stats;
            }

            return null;
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(Statistics stats) {
            super.onPostExecute(stats);

            if (mContext == null || stats == null) {
                // If there is no context, the UI has gone away since the task started. Do nothing.
                return;
            }

            // "tr()" converts from "AverageCalculator.UNKNOWN" and "AverageCalculator.DNF" to the
            // values needed by "convertTimeToString".

            String allTimeBestAvg3 = convertTimeToString(
                    tr(stats.getAverageOf(3, false).getBestAverage()));
            String allTimeBestAvg5 = convertTimeToString(
                    tr(stats.getAverageOf(5, false).getBestAverage()));
            String allTimeBestAvg12 = convertTimeToString(
                    tr(stats.getAverageOf(12, false).getBestAverage()));
            String allTimeBestAvg50 = convertTimeToString(
                    tr(stats.getAverageOf(50, false).getBestAverage()));
            String allTimeBestAvg100 = convertTimeToString(
                    tr(stats.getAverageOf(100, false).getBestAverage()));
            String allTimeBestAvg1000 = convertTimeToString(
                    tr(stats.getAverageOf(1_000, false).getBestAverage()));

            String allTimeMeanTime = convertTimeToString(tr(stats.getAllTimeMeanTime()));
            String allTimeBestTime = convertTimeToString(tr(stats.getAllTimeBestTime()));
            String allTimeWorstTime = convertTimeToString(tr(stats.getAllTimeWorstTime()));
            // Format count using appropriate grouping separators, e.g., "1,234", not "1234".
            String allTimeCount
                    = String.format(Locale.getDefault(), "%,d", stats.getAllTimeNumSolves());

            String sessionBestAvg3 = convertTimeToString(
                    tr(stats.getAverageOf(3, true).getBestAverage()));
            String sessionBestAvg5 = convertTimeToString(
                    tr(stats.getAverageOf(5, true).getBestAverage()));
            String sessionBestAvg12 = convertTimeToString(
                    tr(stats.getAverageOf(12, true).getBestAverage()));
            String sessionBestAvg50 = convertTimeToString(
                    tr(stats.getAverageOf(50, true).getBestAverage()));
            String sessionBestAvg100 = convertTimeToString(
                    tr(stats.getAverageOf(100, true).getBestAverage()));
            String sessionBestAvg1000 = convertTimeToString(
                    tr(stats.getAverageOf(1_000, true).getBestAverage()));

            String sessionMeanTime = convertTimeToString(tr(stats.getSessionMeanTime()));
            String sessionBestTime = convertTimeToString(tr(stats.getSessionBestTime()));
            String sessionWorstTime = convertTimeToString(tr(stats.getSessionWorstTime()));
            String sessionCount
                    = String.format(Locale.getDefault(), "%,d", stats.getSessionNumSolves());

            String sessionCurrentAvg3 = convertTimeToString(
                    tr(stats.getAverageOf(3, true).getCurrentAverage()));
            String sessionCurrentAvg5 = convertTimeToString(
                    tr(stats.getAverageOf(5, true).getCurrentAverage()));
            String sessionCurrentAvg12 = convertTimeToString(
                    tr(stats.getAverageOf(12, true).getCurrentAverage()));
            String sessionCurrentAvg50 = convertTimeToString(
                    tr(stats.getAverageOf(50, true).getCurrentAverage()));
            String sessionCurrentAvg100 = convertTimeToString(
                    tr(stats.getAverageOf(100, true).getCurrentAverage()));
            String sessionCurrentAvg1000 = convertTimeToString(
                    tr(stats.getAverageOf(1_000, true).getCurrentAverage()));

            personalBestTimes.setText(
                    allTimeBestAvg3 + "\n" +
                            allTimeBestAvg5 + "\n" +
                            allTimeBestAvg12 + "\n" +
                            allTimeBestAvg50 + "\n" +
                            allTimeBestAvg100 + "\n" +
                            allTimeBestAvg1000 + "\n" +
                            allTimeMeanTime + "\n" +
                            allTimeBestTime + "\n" +
                            allTimeWorstTime + "\n" +
                            allTimeCount);
            sessionBestTimes.setText(
                    sessionBestAvg3 + "\n" +
                            sessionBestAvg5 + "\n" +
                            sessionBestAvg12 + "\n" +
                            sessionBestAvg50 + "\n" +
                            sessionBestAvg100 + "\n" +
                            sessionBestAvg1000 + "\n" +
                            sessionMeanTime + "\n" +
                            sessionBestTime + "\n" +
                            sessionWorstTime + "\n" +
                            sessionCount);
            sessionCurrentTimes.setText(
                    sessionCurrentAvg3 + "\n" +
                            sessionCurrentAvg5 + "\n" +
                            sessionCurrentAvg12 + "\n" +
                            sessionCurrentAvg50 + "\n" +
                            sessionCurrentAvg100 + "\n" +
                            sessionCurrentAvg1000 + "\n" +
                            // Last few are the same for "Session Best" and "Session Current".
                            sessionMeanTime + "\n" +
                            sessionBestTime + "\n" +
                            sessionWorstTime + "\n" +
                            sessionCount);

            toggleCardStats(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }
    }
}
