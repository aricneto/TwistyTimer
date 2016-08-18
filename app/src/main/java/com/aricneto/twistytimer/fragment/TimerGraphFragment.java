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
import com.aricneto.twistytimer.utils.AverageCalculator;
import com.aricneto.twistytimer.utils.OffsetValuesLineChartRenderer;
import com.aricneto.twistytimer.utils.ChartStatistics;
import com.aricneto.twistytimer.utils.Statistics;
import com.aricneto.twistytimer.utils.ThemeUtils;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
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
    private Context mContext;

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

    // Receives broadcasts from the timer
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isAdded()) { // The fragment has to check if it is attached to an activity. Removing this will bug the app
                switch (intent.getStringExtra("action")) {
                    case "MOVED TO HISTORY":
                    case "TIME UPDATED":
                    case "REFRESH TIME":
                        generateChart();
                        calculateStats();
                        break;
                    case "TIME ADDED":
                        if (! history)
                            generateChart();
                        calculateStats();
                        break;
                    case "HISTORY":
                        history = ! history;
                        generateChart();
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

        mContext = getContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_timer_graph, container, false);
        ButterKnife.bind(this, root);

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
        // The maximum number of values that can be visible above which the time values are not
        // drawn on the chart beside their data points. However, values are only drawn for the few
        // "best" times, and these are likely to be much fewer (i.e., spread out along the X-axis),
        // so the maximum can be increased from the default of 100. Otherwise, if there are more
        // than 100 times visible, the "best" times will not be shown until the user zooms into the
        // chart quite a lot.
        //
        // One confusing aspect is that the visible count that the chart renderer compares to this
        // maximum count includes all points from all data sets, even those that have not been set
        // to show values (i.e., even when "setDrawValues(false)" is applied). For example, if
        // there are 1,000 solve times in one data set and then 951 Ao50 times and 901 Ao100 times,
        // and 8 "best" times, then the total number of visible data points is 2,860, even though
        // the chart is only 1,000 points wide and even though only the 8 "best" times will show
        // their values. Therefore, the maximum count needs to be about 3 times higher than the
        // number of solve times that would give rise to the number of "best" times that could have
        // their values shown without much visual overlap.
        lineChartView.setMaxVisibleValueCount(2_000);
        // Use the custom renderer to draw the values of the "best" times below their data points.
        lineChartView.setRenderer(new OffsetValuesLineChartRenderer(
                lineChartView, ChartStatistics.BEST_TIME_VALUES_Y_OFFSET_DP));

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

        // Launch the background tasks to load the data for the chart and statistics.
        // TODO: This should probably only be done when the fragment becomes visible, not when it
        // is created. If the app is started, it shows the timer tab, but this graph fragment is
        // created for another tab and triggers this loading, even though the graph and statistics
        // are not yet visible. Triggering this only when the fragment is visible would speed the
        // start-up of the app and the generation of the first scramble (if scrambles are enabled).
        generateChart();
        calculateStats();

        return root;
    }

    private void calculateStats() {
        new CalculateStats().execute();
    }

    private void generateChart() {
        new GenerateSolveList().execute(currentPuzzle, currentPuzzleSubtype);
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
    private class GenerateSolveList extends AsyncTask<String, Void, ChartStatistics> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected ChartStatistics doInBackground(String... params) {
            final ChartStatistics chartStats
                    = history ? ChartStatistics.newAllTimeChartStatistics(mContext)
                              : ChartStatistics.newCurrentSessionChartStatistics(mContext);

            TwistyTimer.getDBHandler().populateChartStatistics(
                    currentPuzzle, currentPuzzleSubtype, chartStats);

            return chartStats;
        }

        @Override
        protected void onPostExecute(ChartStatistics chartStats) {
            // Horizontal mean time line.
            final long mean = chartStats.getMeanTime();

            lineChartView.getAxisLeft().removeAllLimitLines();
            if (mean != AverageCalculator.UNKNOWN) {
                final LimitLine ll
                        = new LimitLine(mean / 1_000f, mContext.getString(R.string.graph_mean));
                final int meanColor
                        = ThemeUtils.fetchAttrColor(mContext, R.attr.colorChartMeanTime);

                ll.setLineColor(meanColor);
                ll.setLineWidth(1f);
                ll.enableDashedLine(20f, 10f, 0f);
                ll.setLabelPosition(LimitLine.LimitLabelPosition.LEFT_TOP);
                ll.setTextColor(meanColor);
                ll.setTextSize(12f);

                lineChartView.getAxisLeft().addLimitLine(ll);
            }

            // Add all times line, best times line and average-of-N times lines (with highlighted
            // best AoN times) and identify them using the custom-configured legend.
            lineChartView.setData(chartStats.getChartData());
            chartStats.configureLegend(lineChartView.getLegend());

            // Animate and refresh the chart.
            lineChartView.animateY(1_000);
        }
    }

    private class CalculateStats extends AsyncTask<Void, Void, Statistics> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            toggleCardStats(View.GONE);
        }

        @Override
        protected Statistics doInBackground(Void... voids) {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

            final Statistics stats = Statistics.newAllTimeStatistics();

            TwistyTimer.getDBHandler().populateStatistics(
                    currentPuzzle, currentPuzzleSubtype, stats);

            return stats;
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(Statistics stats) {
            super.onPostExecute(stats);

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

    @Override
    public void onDetach() {
        super.onDetach();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mReceiver);
    }
}
