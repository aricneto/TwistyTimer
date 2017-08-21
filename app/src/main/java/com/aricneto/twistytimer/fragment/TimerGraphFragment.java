package com.aricneto.twistytimer.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

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
import com.aricneto.twistytimer.utils.ThemeUtils;
import com.aricneto.twistytimer.utils.Wrapper;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.utils.Utils;

import java.util.ArrayList;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
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

    private static final int TAB_FAVORITE   = 0;
    private static final int TAB_AVERAGE  = 1;
    private static final int TAB_OTHER = 2;

    private String  currentPuzzle;
    private String  currentPuzzleSubtype;
    private boolean history;

    private Unbinder mUnbinder;

    @BindView(R.id.linechart)
        LineChart lineChartView;


    @BindView(R.id.stats_tab_favorite)
        TextView statsTabFavorite;
    @BindView(R.id.stats_tab_average)
        TextView statsTabAverage;
    @BindView(R.id.stats_tab_other)
        TextView statsTabOther;


    @BindView(R.id.stats_table_favorite)
        View statsFavoriteLayout;
    @BindView(R.id.stats_table_average)
        View statsAverageLayout;
    @BindView(R.id.stats_table_other)
        View statsOtherlayout;

    GridView statsFavoriteGridView;
    GridView statsAverageGridView;
    GridView statsOtherGridView;


    @BindView(R.id.stats_table_viewflipper)
        ViewFlipper statsTableViewFlipper;


    /*
    @OnClick( {R.id.stats_label, R.id.stats_global, R.id.stats_session, R.id.stats_current} )
    public void onClickStats(View view) {
        String label = "";
        switch (view.getId()) {
            case R.id.stats_label:
                label = getString(R.string.graph_stats_average_label);
                break;
            case R.id.stats_global:
                label = getString(R.string.graph_stats_average_global);
                break;
            case R.id.stats_session:
                label = getString(R.string.graph_stats_average_session);
                break;
            case R.id.stats_current:
                label = getString(R.string.graph_stats_average_current);
                break;

        }
        Toast.makeText(mContext, label, Toast.LENGTH_LONG).show();
    }*/

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
        lineChartView.getAxisLeft().setEnabled(false);
        lineChartView.getLegend().setTextColor(chartTextColor);
        lineChartView.setViewPortOffsets(8f, 8f, 8f, 70f);
        lineChartView.setDescription("");

        final YAxis axisLeft = lineChartView.getAxisRight();
        final XAxis xAxis = lineChartView.getXAxis();
        final int axisColor = ContextCompat.getColor(getActivity(), R.color.md_amber_400);
        final int gridColor = ContextCompat.getColor(getActivity(), R.color.gridColor);

        xAxis.setDrawGridLines(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setAxisLineColor(axisColor);
        xAxis.setDrawAxisLine(false);
        xAxis.setTextColor(chartTextColor);
        xAxis.setAvoidFirstLastClipping(true);

        axisLeft.setDrawGridLines(true);
        axisLeft.setTextColor(axisColor);
        axisLeft.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
        axisLeft.setAxisLineColor(axisColor);
        axisLeft.setDrawAxisLine(false);
        axisLeft.setGridColor(gridColor);
        axisLeft.enableGridDashedLine(10, 8f, 0);
        axisLeft.setValueFormatter(new TimeFormatter());
        axisLeft.setDrawLimitLinesBehindData(true);


        // Find the gridView inside each included layout
        statsFavoriteGridView = statsFavoriteLayout.findViewById(R.id.stats_gridView);
        statsAverageGridView = statsAverageLayout.findViewById(R.id.stats_gridView);
        statsOtherGridView = statsOtherlayout.findViewById(R.id.stats_gridView);

        // In order for the user to be able to drag the stats panel up, we have to disable
        // interaction with GridView, so it can be handled by the panel
        statsFavoriteGridView.setOnTouchListener(doNothingTouchListener);
        statsAverageGridView.setOnTouchListener(doNothingTouchListener);
        statsOtherGridView.setOnTouchListener(doNothingTouchListener);

        // The "Favorites" and "Other" grids should only have two columns. (Best and Session)
        // Since the base layout is 3-columns wide, we have to hide the last column title and set
        // num of columns to 2 for these two views
        statsFavoriteLayout.findViewById(R.id.stats_current).setVisibility(View.GONE);
        statsOtherlayout.findViewById(R.id.stats_current).setVisibility(View.GONE);
        statsFavoriteGridView.setNumColumns(2);
        statsOtherGridView.setNumColumns(2);

        // Also, change stats_label_column weight for these columns so it can take advantage of the
        // extra space and display more text
        LinearLayout.LayoutParams layoutParams
                = (LinearLayout.LayoutParams) statsFavoriteLayout
                .findViewById(R.id.stats_label_column).getLayoutParams();
        layoutParams.weight = 2f;
        statsFavoriteLayout.findViewById(R.id.stats_label_column).setLayoutParams(layoutParams);
        statsOtherlayout.findViewById(R.id.stats_label_column).setLayoutParams(layoutParams);



        statsTableViewFlipper.setInAnimation(mContext, R.anim.stats_grid_in);
        statsTableViewFlipper.setOutAnimation(mContext, R.anim.stats_grid_out);
        statsTabFavorite.setOnClickListener(statTabClickListener);
        statsTabAverage.setOnClickListener(statTabClickListener);
        statsTabOther.setOnClickListener(statTabClickListener);


        // Setting for landscape mode. The chart and statistics table need to be scrolled, as the
        // statistics table will likely almost fill the screen. The automatic layout causes the
        // chart to use only the remaining space after the statistics table takes its space.
        // However, this may lead to the whole chart being squeezed into a few vertical pixels.
        // Therefore, set a fixed height for the chart that will force the statistics table to be
        // scrolled down to allow the chart to fit.
        /*
        root.post(new Runnable() {
            @Override
            public void run() {
                if (container != null && lineChartView != null) {
                    final LineChart.LayoutParams params = lineChartView.getLayoutParams();

                    if (params != null) {
                        params.height = container.getHeight() - statsTable.getHeight();
                        lineChartView.setLayoutParams(params);
                        lineChartView.requestLayout();
                        statsTable.requestLayout();
                    }
                }
            }
        });*/



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


    private void highlightStatTab(TextView tab) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) tab.getLayoutParams();
        params.setMargins(0, 0, 0, 0);
        tab.setLayoutParams(params);
        tab.setTextColor(ThemeUtils.fetchAttrColor(mContext, R.attr.graph_stats_card_text_color));
        tab.setBackgroundColor(ThemeUtils.fetchAttrColor(mContext, R.attr.graph_stats_card_background));
    }

    private void fadeStatTab(TextView tab) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) tab.getLayoutParams();
        params.setMargins(0, (int) Utils.convertDpToPixel(4), 0, 0);
        tab.setLayoutParams(params);
        tab.setTextColor(ThemeUtils.fetchAttrColor(mContext, R.attr
                .graph_stats_card_text_color_faded));
        tab.setBackgroundColor(ThemeUtils.fetchAttrColor(mContext, R.attr
                .graph_stats_card_background_faded));
    }

    private View.OnClickListener statTabClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View tab) {
            switch (tab.getId()) {
                case R.id.stats_tab_favorite:
                    statsTableViewFlipper.setDisplayedChild(TAB_FAVORITE);
                    highlightStatTab(statsTabFavorite);
                    fadeStatTab(statsTabOther);
                    fadeStatTab(statsTabAverage);
                    break;
                case R.id.stats_tab_average:
                    statsTableViewFlipper.setDisplayedChild(TAB_AVERAGE);
                    highlightStatTab(statsTabAverage);
                    fadeStatTab(statsTabOther);
                    fadeStatTab(statsTabFavorite);
                    break;
                case R.id.stats_tab_other:
                    statsTableViewFlipper.setDisplayedChild(TAB_OTHER);
                    highlightStatTab(statsTabOther);
                    fadeStatTab(statsTabFavorite);
                    fadeStatTab(statsTabAverage);
                    break;
            }
        }
    };


    private View.OnTouchListener doNothingTouchListener = new View.OnTouchListener() {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            return true;
        }
    };

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
        lineChartView.animateY(700, Easing.EasingOption.EaseInOutSine);
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

        ArrayList<Stat> averageList = buildAverageList(stats);
        ArrayList<Stat> otherList = buildOtherStatList(stats);
        statsAverageGridView.setAdapter(new StatGridAdapter(mContext, averageList));
        statsOtherGridView.setAdapter(new StatGridAdapter(mContext, otherList));
        statsFavoriteGridView.setAdapter(new StatGridAdapter(mContext, otherList));


        // Display the statistics and hide the progress bar.
        setStatsTableVisibility(View.VISIBLE);
    }

    private ArrayList<Stat> buildOtherStatList(Statistics stats) {
        // There are 2 columns (best, session), and 5 rows (best, worst, deviation, total time
        // and count).
        ArrayList<Stat> statsList = new ArrayList<>(5 * 2);


        // DO NOT CHANGE THE ORDER!
        // The adapter adds views in the list order, left -> right, top -> bottom, so if the order
        // is changed, times will be placed in the wrong spots on the grid!
        statsList.add(new Stat(convertTimeToString(tr(stats.getAllTimeBestTime()), PuzzleUtils
                .FORMAT_DEFAULT), Stat.SCOPE_GLOBAL, 0));
        statsList.add(new Stat(convertTimeToString(tr(stats.getSessionBestTime()), PuzzleUtils
                .FORMAT_DEFAULT), Stat.SCOPE_SESSION, 0));

        statsList.add(new Stat(convertTimeToString(tr(stats.getAllTimeWorstTime()), PuzzleUtils
                .FORMAT_DEFAULT), Stat.SCOPE_GLOBAL, 1));
        statsList.add(new Stat(convertTimeToString(tr(stats.getSessionWorstTime()), PuzzleUtils
                .FORMAT_DEFAULT), Stat.SCOPE_SESSION, 1));

        statsList.add(new Stat(convertTimeToString(tr(stats.getAllTimeTotalTime()), PuzzleUtils
                .FORMAT_NO_MILLI), Stat.SCOPE_GLOBAL, 3));
        statsList.add(new Stat(convertTimeToString(tr(stats.getSessionTotalTime()), PuzzleUtils
                .FORMAT_NO_MILLI), Stat.SCOPE_SESSION, 3));

        statsList.add(new Stat(String.format(Locale.getDefault(), "%,d", stats.getAllTimeNumSolves()),
                Stat.SCOPE_GLOBAL, 4));
        statsList.add(new Stat(String.format(Locale.getDefault(), "%,d", stats.getSessionNumSolves()),
                Stat.SCOPE_SESSION, 4));

        return statsList;
    }
    /**
     * Builds a list of averages for use int {@link StatGridAdapter}
     * The order of the times is alterned (best, session, current, best, session...), so
     * it can work in an Adapter without much tinkering
     * @param stats
     *      The {@link Statistics} instance
     * @return
     *      An {@link ArrayList<Stat>} containing the averages for the adapter
     */
    private ArrayList<Stat> buildAverageList(Statistics stats) {
        int averageNumbers[] = {3, 5, 12, 50, 100, 1000};
        // There are 3 columns (best, session, current), and 6 rows (the averages)
        // So the capacity of the list needs to be 3*6
        ArrayList<Stat> statsList = new ArrayList<>(3 * 6);
        for (int row = 0; row < 6; row++) {
            // best all time
            statsList.add(new Stat(convertTimeToString(
                    tr(stats.getAverageOf(averageNumbers[row], false)
                            .getBestAverage()), PuzzleUtils.FORMAT_DEFAULT),
                    Stat.SCOPE_GLOBAL, row));
            // session best
            statsList.add(new Stat(convertTimeToString(
                    tr(stats.getAverageOf(averageNumbers[row], true)
                            .getBestAverage()), PuzzleUtils.FORMAT_DEFAULT),
                    Stat.SCOPE_GLOBAL, row));
            // current
            statsList.add(new Stat(convertTimeToString(
                    tr(stats.getAverageOf(averageNumbers[row], true)
                            .getCurrentAverage()), PuzzleUtils.FORMAT_DEFAULT),
                    Stat.SCOPE_CURRENT, row));
        }
        return statsList;
    }

}
