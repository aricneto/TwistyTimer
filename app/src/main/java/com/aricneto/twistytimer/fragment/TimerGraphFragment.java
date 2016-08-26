package com.aricneto.twistytimer.fragment;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.activity.MainActivity;
import com.aricneto.twistytimer.spans.TimeFormatter;
import com.aricneto.twistytimer.stats.ChartStatistics;
import com.aricneto.twistytimer.stats.ChartStatisticsLoader;
import com.aricneto.twistytimer.stats.ChartStyle;
import com.aricneto.twistytimer.stats.Statistics;
import com.aricneto.twistytimer.stats.StatisticsLoader;
import com.aricneto.twistytimer.utils.Wrapper;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;

import java.util.Locale;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

import static com.aricneto.twistytimer.stats.AverageCalculator.tr;
import static com.aricneto.twistytimer.utils.PuzzleUtils.convertTimeToString;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link TimerGraphFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TimerGraphFragment extends Fragment {
    /**
     * A "tag" to identify log messages originating from this class.
     */
    private static final String TAG = "CHART/STATS FRAG";

    private static final String PUZZLE         = "puzzle";
    private static final String PUZZLE_SUBTYPE = "puzzle_type";
    private static final String HISTORY        = "history";

    private String  currentPuzzle;
    private String  currentPuzzleSubtype;

    private Unbinder mUnbinder;
    @BindView(R.id.linechart)           LineChart lineChartView;
    @BindView(R.id.personalBestTimes)   TextView  personalBestTimes;
    @BindView(R.id.sessionBestTimes)    TextView  sessionBestTimes;
    @BindView(R.id.sessionCurrentTimes) TextView  sessionCurrentTimes;
    @BindView(R.id.progressSpinner)     MaterialProgressBar progressBar;

    // Things that must be hidden/shown when refreshing the card.
    @BindViews({
            R.id.personalBestTitle, R.id.sessionBestTitle, R.id.sessionCurrentTitle,
            R.id.horizontalDivider02, R.id.verticalDivider02, R.id.verticalDivider03,
    }) View[] statisticsTableViews;

    private boolean history;

    public TimerGraphFragment() {
        // Required empty public constructor
    }

    // We have to put a boolean history here because it resets when we change puzzles.
    public static TimerGraphFragment newInstance(String puzzle, String puzzleType, boolean history) {
        Log.d(TAG, "newInstance");
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
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            currentPuzzle = getArguments().getString(PUZZLE);
            currentPuzzleSubtype = getArguments().getString(PUZZLE_SUBTYPE);
            history = getArguments().getBoolean(HISTORY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        final View root = inflater.inflate(R.layout.fragment_timer_graph, container, false);
        mUnbinder = ButterKnife.bind(this, root);

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
        final int axisColor = ContextCompat.getColor(getActivity(), R.color.white_secondary_icon);

        xAxis.setDrawGridLines(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setAxisLineColor(axisColor);
        xAxis.setDrawAxisLine(true);
        xAxis.setTextColor(chartTextColor);
        xAxis.setAvoidFirstLastClipping(true);

        axisLeft.setDrawGridLines(true);
        axisLeft.setDrawAxisLine(true);
        axisLeft.setTextColor(chartTextColor);
        axisLeft.enableGridDashedLine(10f, 8f, 0f);
        axisLeft.setAxisLineColor(axisColor);
        axisLeft.setGridColor(axisColor);
        axisLeft.setValueFormatter(new TimeFormatter());
        axisLeft.setDrawLimitLinesBehindData(true);

        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // "restartLoader" ensures that any old loader with the wrong puzzle type/subtype will not
        // be reused. For now, those arguments are just passed via their respective fields to
        // "onCreateLoader".
        //
        // Starting loaders here in "onActivityCreated" ensures that "onCreateView" is complete.
        //
        // The loaders deliver different types of data, so two "LoaderCallbacks" are required.
        // However, this turns out to be simpler, as there will be no need to check loader IDs.

        Log.d(TAG, "onActivityCreated -> restartLoader: CHART_DATA_LOADER_ID");
        getLoaderManager().restartLoader(MainActivity.CHART_DATA_LOADER_ID, null,
                new LoaderManager.LoaderCallbacks<Wrapper<ChartStatistics>>() {
                    @Override
                    public Loader<Wrapper<ChartStatistics>> onCreateLoader(int id, Bundle args) {
                        Log.d(TAG, "onCreateLoader: CHART_DATA_LOADER_ID");
                        // "ChartStyle" allows the Loader to be executed without the need to hold a
                        // reference to an Activity context (required to access theme attributes),
                        // which would be likely to cause memory leaks and crashes.
                        return new ChartStatisticsLoader(
                                getContext(), new ChartStyle(getActivity()), currentPuzzle,
                                currentPuzzleSubtype, !history);
                    }

                    @Override
                    public void onLoadFinished(Loader<Wrapper<ChartStatistics>> loader,
                                               Wrapper<ChartStatistics> data) {
                        Log.d(TAG, "onLoadFinished: CHART_DATA_LOADER_ID");
                        if (isResumed()) {
                            updateChart(data.content());
                        }
                    }

                    @Override
                    public void onLoaderReset(Loader<Wrapper<ChartStatistics>> loader) {
                        Log.d(TAG, "onLoaderReset: CHART_DATA_LOADER_ID");
                        // Nothing to do here, as the "ChartStatistics" object was never retained.
                        // The view is most likely destroyed at this time, so no need to update it.
                    }
                });

        Log.d(TAG, "onActivityCreated -> restartLoader: STATS_TABLE_LOADER_ID");
        getLoaderManager().restartLoader(MainActivity.STATS_TABLE_LOADER_ID, null,
                new LoaderManager.LoaderCallbacks<Wrapper<Statistics>>() {
                    @Override
                    public Loader<Wrapper<Statistics>> onCreateLoader(int id, Bundle args) {
                        Log.d(TAG, "onCreateLoader: STATS_TABLE_LOADER_ID");
                        if (isResumed()) {
                            progressBar.setVisibility(View.VISIBLE);
                            toggleCardStats(View.GONE);
                        }
                        return new StatisticsLoader(getContext(), Statistics.newAllTimeStatistics(),
                                currentPuzzle, currentPuzzleSubtype);
                    }

                    @Override
                    public void onLoadFinished(Loader<Wrapper<Statistics>> loader,
                                               Wrapper<Statistics> data) {
                        Log.d(TAG, "onLoadFinished: STATS_TABLE_LOADER_ID");
                        if (isResumed()) {
                            updateStatistics(data.content());
                        }
                    }

                    @Override
                    public void onLoaderReset(Loader<Wrapper<Statistics>> loader) {
                        Log.d(TAG, "onLoaderReset: STATS_TABLE_LOADER_ID");
                        // Nothing to do here, as the "ChartStatistics" object was never retained.
                        // The view is most likely destroyed at this time, so no need to update it.
                    }
                });
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView");
        super.onDestroyView();
        mUnbinder.unbind();
    }

    private void toggleCardStats(final int visibility) {
        ButterKnife.apply(statisticsTableViews, new ButterKnife.Action<View>() {
            @Override
            public void apply(@NonNull View view, int index) {
                view.setVisibility(visibility);
            }
        });

        personalBestTimes.setVisibility(visibility);
        sessionBestTimes.setVisibility(visibility);
        sessionCurrentTimes.setVisibility(visibility);
    }

    /**
     * Called when the chart statistics loader has completed loading the chart data. The loader
     * listens for changes to the data and this method will be called each time the display of the
     * chart needs to be refreshed.
     *
     * @param chartStats The chart statistics populated by the loader.
     */
    private void updateChart(ChartStatistics chartStats) {
        Log.d(TAG, "updateChart");

        // Add all times line, best times line, average-of-N times lines (with highlighted
        // best AoN times) and mean limit line and identify them all using a custom legend.
        chartStats.applyTo(lineChartView);

        // Animate and refresh the chart.
        lineChartView.animateY(1_000);
    }

    /**
     * Called when the statistics loader has completed loading the statistics data. The loader
     * listens for changes to the data and this method will be called each time the display of the
     * statistics needs to be refreshed.
     *
     * @param stats The statistics populated by the loader.
     */
    @SuppressLint("SetTextI18n")
    private void updateStatistics(Statistics stats) {
        Log.d(TAG, "updateStatistics");

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
        String allTimeCount = String.format(Locale.getDefault(), "%,d", stats.getAllTimeNumSolves());

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
        String sessionCount = String.format(Locale.getDefault(), "%,d", stats.getSessionNumSolves());

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
