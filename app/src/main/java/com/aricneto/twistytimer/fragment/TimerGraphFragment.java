package com.aricneto.twistytimer.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.Toast;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.activity.MainActivity;
import com.aricneto.twistytimer.adapter.StatGridAdapter;
import com.aricneto.twistytimer.items.Stat;
import com.aricneto.twistytimer.spans.TimeFormatter;
import com.aricneto.twistytimer.stats.ChartStatistics;
import com.aricneto.twistytimer.stats.ChartStatisticsLoader;
import com.aricneto.twistytimer.stats.ChartStyle;
import com.aricneto.twistytimer.stats.Statistics;
import com.aricneto.twistytimer.stats.StatisticsCache;
import com.aricneto.twistytimer.utils.PuzzleUtils;
import com.aricneto.twistytimer.utils.Wrapper;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;

import java.util.ArrayList;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

import static com.aricneto.twistytimer.stats.AverageCalculator.tr;
import static com.aricneto.twistytimer.utils.PuzzleUtils.convertTimeToString;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link TimerGraphFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TimerGraphFragment extends Fragment implements StatisticsCache.StatisticsObserver {
    /**
     * Flag to enable debug logging for this class.
     */
    private static final boolean DEBUG_ME = false;

    /**
     * A "tag" to identify this class in log messages.
     */
    private static final String TAG = TimerGraphFragment.class.getSimpleName();

    private static final String PUZZLE         = "puzzle";
    private static final String PUZZLE_SUBTYPE = "puzzle_type";
    private static final String HISTORY        = "history";

    private static final int COLUMN_LABEL   = 0;
    private static final int COLUMN_GLOBAL  = 1;
    private static final int COLUMN_SESSION = 2;
    private static final int COLUMN_CURRENT = 3;

    private String  currentPuzzle;
    private String  currentPuzzleSubtype;
    private boolean history;

    private Unbinder mUnbinder;

    @BindView(R.id.linechart)           LineChart lineChartView;
    @BindView(R.id.stats_gridView)
        GridView statsGridView;


    @OnClick( {R.id.stats_label})//, R.id.stats_global, R.id.stats_session, R.id.stats_current} )
    public void onClickStats(View view) {
        String label = "";
        switch (view.getId()) {
            case R.id.stats_label:
                label = getString(R.string.graph_stats_average_label);
                break;
                /*
            case R.id.stats_global:
                label = getString(R.string.graph_stats_average_global);
                break;
            case R.id.stats_session:
                label = getString(R.string.graph_stats_average_session);
                break;
            case R.id.stats_current:
                label = getString(R.string.graph_stats_average_current);
                break;
                */

        }
        Toast.makeText(mContext, label, Toast.LENGTH_LONG).show();
    }

    //@BindView(R.id.progressSpinner)     MaterialProgressBar progressBar;

    private Context mContext;

    /*/ Things that must be hidden/shown when refreshing the card.
    @BindViews({
            R.id.personalBestTitle, R.id.sessionBestTitle, R.id.sessionCurrentTitle,
            R.id.horizontalDivider02, R.id.verticalDivider02, R.id.verticalDivider03,
    }) View[] statisticsTableViews;*/

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
        if (DEBUG_ME) Log.d(TAG, "newInstance() -> " + fragment);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (DEBUG_ME) Log.d(TAG, "onCreate(savedInstanceState=" + savedInstanceState + ")");
        super.onCreate(savedInstanceState);

        mContext = getContext();

        if (getArguments() != null) {
            currentPuzzle = getArguments().getString(PUZZLE);
            currentPuzzleSubtype = getArguments().getString(PUZZLE_SUBTYPE);
            history = getArguments().getBoolean(HISTORY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        if (DEBUG_ME) Log.d(TAG, "onCreateView(savedInstanceState=" + savedInstanceState + ")");
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


        // Setting for landscape mode. The chart and statistics table need to be scrolled, as the
        // statistics table will likely almost fill the screen. The automatic layout causes the
        // chart to use only the remaining space after the statistics table takes its space.
        // However, this may lead to the whole chart being squeezed into a few vertical pixels.
        // Therefore, set a fixed height for the chart that will force the statistics table to be
        // scrolled down to allow the chart to fit.
        if (getActivity().getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE) {
            root.post(new Runnable() {
                @Override
                public void run() {
                    if (container != null && lineChartView != null) {
                        final LineChart.LayoutParams params = lineChartView.getLayoutParams();

                        if (params != null) {
                            params.height = container.getHeight();
                            lineChartView.setLayoutParams(params);
                            lineChartView.requestLayout();
                        }
                    }
                }
            });
        }



        // If the statistics are already loaded, the update notification will have been missed,
        // so fire that notification now. If the statistics are non-null, they will be displayed.
        // If they are null (i.e., not yet loaded), the progress bar will be displayed until this
        // fragment, as a registered observer, is notified when loading is complete. Post the
        // firing of the event, so that it is received after "onCreateView" returns.
        root.post(new Runnable() {
            @Override
            public void run() {
                onStatisticsUpdated(StatisticsCache.getInstance().getStatistics());
            }
        });
        StatisticsCache.getInstance().registerObserver(this); // Unregistered in "onDestroyView".

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
        // An anonymous inner class is neater than implementing "LoaderCallbacks".
        if (DEBUG_ME) Log.d(TAG, "onActivityCreated -> restartLoader: CHART_DATA_LOADER_ID");
        getLoaderManager().restartLoader(MainActivity.CHART_DATA_LOADER_ID, null,
                new LoaderManager.LoaderCallbacks<Wrapper<ChartStatistics>>() {
                    @Override
                    public Loader<Wrapper<ChartStatistics>> onCreateLoader(int id, Bundle args) {
                        if (DEBUG_ME) Log.d(TAG, "onCreateLoader: CHART_DATA_LOADER_ID");
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
                        if (DEBUG_ME) Log.d(TAG, "onLoadFinished: CHART_DATA_LOADER_ID");
                        updateChart(data.content());
                    }

                    @Override
                    public void onLoaderReset(Loader<Wrapper<ChartStatistics>> loader) {
                        if (DEBUG_ME) Log.d(TAG, "onLoaderReset: CHART_DATA_LOADER_ID");
                        // Nothing to do here, as the "ChartStatistics" object was never retained.
                        // The view is most likely destroyed at this time, so no need to update it.
                    }
                });
    }

    @Override
    public void onDestroyView() {
        if (DEBUG_ME) Log.d(TAG, "onDestroyView()");
        super.onDestroyView();
        mUnbinder.unbind();
        StatisticsCache.getInstance().unregisterObserver(this);
    }

    /**
     * Sets the visibility of the statistics table values columns. When loading starts, the columns
     * should be hidden and a progress bar shown. When loading finishes, the columns should be
     * populated with values and be shown and the progress bar hidden. No action will be taken if
     * this fragment does not yet have a view, or if its view has been destroyed.
     *
     * @param visibility
     *     The visibility to set on the statistics table columns. Use {@code View.GONE} or
     *     {@code View.VISIBLE}. The opposite visibility will be applied to the progress bar.
     */
    private void setStatsTableVisibility(final int visibility) {
        if (getView() == null) {
            // Called before "onCreateView" or after "onDestroyView", so do nothing.
            return;
        }
        /*
        ButterKnife.apply(statisticsTableViews, new ButterKnife.Action<View>() {
            @Override
            public void apply(@NonNull View view, int index) {
                view.setVisibility(visibility);
            }
        });*/

        // TODO: reimplement this


        // NOTE: Use "INVISIBLE", not "GONE", or there will be problems with vertical centering.
        //progressBar.setVisibility(visibility == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
    }

    /**
     * Called when the chart statistics loader has completed loading the chart data. The loader
     * listens for changes to the data and this method will be called each time the display of the
     * chart needs to be refreshed. If this fragment has no view, no update will be attempted.
     *
     * @param chartStats The chart statistics populated by the loader.
     */
    private void updateChart(ChartStatistics chartStats) {
        if (DEBUG_ME) Log.d(TAG, "updateChart()");

        if (getView() == null) {
            // Must have arrived after "onDestroyView" was called, so do nothing.
            return;
        }

        // Add all times line, best times line, average-of-N times lines (with highlighted
        // best AoN times) and mean limit line and identify them all using a custom legend.
        chartStats.applyTo(lineChartView);

        // Animate and refresh the chart.
        lineChartView.animateY(1_000);
    }

    /**
     * Refreshes the display of the statistics. If this fragment has no view, no update will be
     * attempted.
     *
     * @param stats
     *     The updated statistics. These will not be modified. If {@code null}, a progress bar will
     *     be displayed until non-{@code null} statistics are passed to this method in a later call.
     */
    @SuppressLint("SetTextI18n")
    @Override
    public void onStatisticsUpdated(Statistics stats) {
        if (DEBUG_ME) Log.d(TAG, "onStatisticsUpdated(" + stats + ")");

        if (getView() == null) {
            // Must have arrived after "onDestroyView" was called, so do nothing.
            return;
        }

        if (stats == null) {
            // Hide the statistics and show the progress bar until the statistics become ready.
            setStatsTableVisibility(View.GONE);
            return;
        }

        // "tr()" converts from "AverageCalculator.UNKNOWN" and "AverageCalculator.DNF" to the
        // values needed by "convertTimeToString".

        ArrayList<Stat> averagelist = buildAverageList(stats);

        statsGridView.setAdapter(new StatGridAdapter(mContext, averagelist));

        String allTimeMeanTime = convertTimeToString(tr(stats.getAllTimeMeanTime()), PuzzleUtils.FORMAT_DEFAULT);
        String allTimeBestTime = convertTimeToString(tr(stats.getAllTimeBestTime()), PuzzleUtils.FORMAT_DEFAULT);
        String allTimeWorstTime = convertTimeToString(tr(stats.getAllTimeWorstTime()), PuzzleUtils.FORMAT_DEFAULT);
        // Format count using appropriate grouping separators, e.g., "1,234", not "1234".
        String allTimeCount = String.format(Locale.getDefault(), "%,d", stats.getAllTimeNumSolves());

        String sessionMeanTime = convertTimeToString(tr(stats.getSessionMeanTime()), PuzzleUtils.FORMAT_DEFAULT);
        String sessionBestTime = convertTimeToString(tr(stats.getSessionBestTime()), PuzzleUtils.FORMAT_DEFAULT);
        String sessionWorstTime = convertTimeToString(tr(stats.getSessionWorstTime()), PuzzleUtils.FORMAT_DEFAULT);
        String sessionCount = String.format(Locale.getDefault(), "%,d", stats.getSessionNumSolves());

        // Display the statistics and hide the progress bar.
        setStatsTableVisibility(View.VISIBLE);
    }

    // TODO: COMMENT
    private ArrayList<Stat> buildAverageList(Statistics stats) {
        int averageNumbers[] = {3, 5, 12, 50, 100, 1000};
        int capacity = 3 * 6; // 3 columns, 6 averages
        ArrayList<Stat> statsList = new ArrayList<>(3 * 6);
        for (int row = 0; row < 6; row++) {
            // best all time
            statsList.add(new Stat(convertTimeToString(
                    tr(stats.getAverageOf(averageNumbers[row], false)
                            .getBestAverage()), PuzzleUtils.FORMAT_DEFAULT),
                    Stat.SCOPE_GLOBAL_BEST, row));
            // session best
            statsList.add(new Stat(convertTimeToString(
                    tr(stats.getAverageOf(averageNumbers[row], true)
                            .getBestAverage()), PuzzleUtils.FORMAT_DEFAULT),
                    Stat.SCOPE_GLOBAL_BEST, row));
            // current
            statsList.add(new Stat(convertTimeToString(
                    tr(stats.getAverageOf(averageNumbers[row], true)
                            .getCurrentAverage()), PuzzleUtils.FORMAT_DEFAULT),
                    Stat.SCOPE_CURRENT, row));
        }
        return statsList;
    }

}
