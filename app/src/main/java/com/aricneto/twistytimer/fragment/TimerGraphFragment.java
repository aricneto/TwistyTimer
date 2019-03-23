package com.aricneto.twistytimer.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.activity.MainActivity;
import com.aricneto.twistytimer.adapter.StatGridAdapter;
import com.aricneto.twistytimer.items.Stat;
import com.aricneto.twistytimer.spans.RoundedAxisValueFormatter;
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

import java.util.ArrayList;
import java.util.Locale;

import androidx.annotation.ArrayRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.TooltipCompat;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
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


    @BindView(R.id.stats_tab_improvement)
        TextView statsTabFavorite;
    @BindView(R.id.stats_tab_average)
        TextView statsTabAverage;
    @BindView(R.id.stats_tab_other)
        TextView statsTabOther;


    @BindView(R.id.stats_table_improvement)
        View statsImprovementLayout;
    @BindView(R.id.stats_table_average)
        View statsAverageLayout;
    @BindView(R.id.stats_table_other)
        View statsOtherlayout;

    private GridView statsImprovementGridView;
    private GridView statsAverageGridView;
    private GridView statsOtherGridView;
    private GridView statsImprovementLabelGridView;
    private GridView statsAverageLabelGridView;
    private GridView statsOtherLabelGridView;

    private Drawable buttonDrawable;
    private Drawable buttonDrawableFaded;


    @BindView(R.id.stats_table_viewflipper)
        ViewFlipper statsTableViewFlipper;

    @BindView(R.id.stats_container_pager)
        View statsContainerPager;

    @BindView(R.id.stats_card)
        CardView statsCard;



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
        if (DEBUG_ME) Log.d(TAG, "updateLocale(savedInstanceState=" + savedInstanceState + ")");
        super.onCreate(savedInstanceState);

        mContext = getContext();

        if (getArguments() != null) {
            currentPuzzle = getArguments().getString(PUZZLE);
            currentPuzzleSubtype = getArguments().getString(PUZZLE_SUBTYPE);
            history = getArguments().getBoolean(HISTORY);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        if (DEBUG_ME) Log.d(TAG, "onCreateView(savedInstanceState=" + savedInstanceState + ")");
        final View root = inflater.inflate(R.layout.fragment_timer_graph, container, false);
        mUnbinder = ButterKnife.bind(this, root);

        // Drawables for the stats cards buttons
        buttonDrawable = ThemeUtils.createSquareDrawable(
                mContext,
                ThemeUtils.fetchAttrColor(mContext, R.attr.graph_stats_card_background),
                0, 20, 0);
        buttonDrawableFaded = ThemeUtils.createSquareDrawable(
                mContext,
                ThemeUtils.fetchAttrColor(mContext, R.attr.graph_stats_card_background_faded),
                0, 20, 0);

        // Setting for landscape mode. The chart and statistics table need to be scrolled, as the
        // statistics table will likely almost fill the screen. The automatic layout causes the
        // chart to use only the remaining space after the statistics table takes its space.
        // However, this may lead to the whole chart being squeezed into a few vertical pixels.
        // Therefore, set a fixed height for the chart that will force the statistics table to be
        // scrolled down to allow the chart to fit.
        root.post(() -> statsCard.post(() -> {
            if (lineChartView != null) {
                final ViewGroup.LayoutParams chartParams = lineChartView.getLayoutParams();
                final int cardHeight = statsCard.getHeight();
                // ATTENTION: 134dp is the sum of the actionBarPadding and tabBarPadding attributes,
                // plus 16 dp for the view padding! Keep these in sync.
                final int viewHeight = container.getHeight()
                                       - ThemeUtils.dpToPix(mContext, 134);
                if (chartParams != null) {
                    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        chartParams.height = viewHeight;
                        lineChartView.setLayoutParams(chartParams);
                        lineChartView.requestLayout();
                        statsContainerPager.requestLayout();
                    } else {
                        // On portrait mode, if the stats card occupies less than 40% of the view,
                        // the graph should fill all remaining space.
                        // If the stats card is bigger than 40%, the graph should occupy 70% of the view,
                        // and the user will have to scroll down to see the card
                        Log.d(TAG, "card: " + cardHeight + " | view: " + viewHeight + " | div: " + ((float) cardHeight / (float) viewHeight));
                        if (((float) cardHeight / (float) viewHeight) <= 0.4f)
                            chartParams.height = viewHeight - cardHeight;
                        else
                            chartParams.height = (int) (viewHeight * 0.7f);

                        lineChartView.setLayoutParams(chartParams);
                        lineChartView.requestLayout();
                        statsContainerPager.requestLayout();
                    }
                }
            }
        }));

        return root;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // The color for the text in the legend and for the values along the chart's axes.
        final int chartTextColor = ThemeUtils.fetchAttrColor(mContext, R.attr.colorChartText);

        // The color for the grid and the axes
        final int axisColor = ThemeUtils.fetchAttrColor(mContext, R.attr.colorChartAxis);
        final int gridColor = ThemeUtils.fetchAttrColor(mContext, R.attr.colorChartGrid);

        // Most of the following settings should be self explanatory.
        // Those that aren't will be commented

        // General chart settings
        //lineChartView.setPinchZoom(true);
        lineChartView.setBackgroundColor(Color.TRANSPARENT);
        lineChartView.setDrawGridBackground(false);
        lineChartView.getAxisLeft().setEnabled(false);
        lineChartView.getLegend().setTextColor(chartTextColor);
        lineChartView.setExtraBottomOffset(ThemeUtils.dpToPix(mContext, 4));
        lineChartView.setDescription(null);

        // Set axis colors
        final YAxis axisLeft = lineChartView.getAxisRight();
        final XAxis xAxis = lineChartView.getXAxis();

        // X-axis settings
        xAxis.setDrawGridLines(false);
        // Draw X line markings on the bottom
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setAxisLineColor(axisColor);
        xAxis.setDrawAxisLine(false);
        xAxis.setTextColor(chartTextColor);
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setValueFormatter(new RoundedAxisValueFormatter(xAxis.mDecimals));

        axisLeft.setDrawGridLines(true);
        //axisLeft.setSpaceTop(30f);
        axisLeft.setTextColor(axisColor);
        axisLeft.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        axisLeft.setAxisLineColor(axisColor);
        axisLeft.setDrawAxisLine(false);
        axisLeft.setGridColor(gridColor);
        axisLeft.enableGridDashedLine(10, 8f, 0);
        axisLeft.setValueFormatter(new TimeFormatter());
        axisLeft.setDrawLimitLinesBehindData(true);


        // Find the gridView inside each included layout
        statsImprovementGridView = statsImprovementLayout.findViewById(R.id.stats_gridView);
        statsAverageGridView = statsAverageLayout.findViewById(R.id.stats_gridView);
        statsOtherGridView = statsOtherlayout.findViewById(R.id.stats_gridView);

        // And the label gridview...
        // We need a separate gridview for the label because gridView doesn't support staggered
        // layouts, and we need the label gridview to be slightly smaller for aesthetic reasons
        statsImprovementLabelGridView = statsImprovementLayout.findViewById(R.id.stats_label_gridView);
        statsAverageLabelGridView = statsAverageLayout.findViewById(R.id.stats_label_gridView);
        statsOtherLabelGridView = statsOtherlayout.findViewById(R.id.stats_label_gridView);

        // The "Improvement" and "Other" grids should only have two columns. (Best and Session)
        // Since the base layout is 3-columns wide, we have to hide the last column title and set
        // num of columns to 2 for these two views
        statsImprovementLayout.findViewById(R.id.stats_current).setVisibility(View.GONE);
        statsOtherlayout.findViewById(R.id.stats_current).setVisibility(View.GONE);
        statsImprovementGridView.setNumColumns(2);
        statsOtherGridView.setNumColumns(2);

        // Set stats name tooltips (label shown on long press)
        setTooltipText(R.id.stats_global, R.string.graph_stats_title_best_all_time);
        setTooltipText(R.id.stats_session, R.string.graph_stats_title_session_best);
        setTooltipText(R.id.stats_current, R.string.graph_stats_title_current);

        // Finally, name the label column.
        statsImprovementLabelGridView.setAdapter(new StatGridAdapter(mContext, buildLabelList(R.array.stats_column_improvement)));
        statsAverageLabelGridView.setAdapter(new StatGridAdapter(mContext, buildLabelList(R.array.stats_column_average)));
        statsOtherLabelGridView.setAdapter(new StatGridAdapter(mContext, buildLabelList(R.array.stats_column_other)));


        statsTableViewFlipper.setInAnimation(mContext, R.anim.stats_grid_in);
        statsTableViewFlipper.setOutAnimation(mContext, R.anim.stats_grid_out);
        statsTabFavorite.setOnClickListener(statTabClickListener);
        statsTabAverage.setOnClickListener(statTabClickListener);
        statsTabOther.setOnClickListener(statTabClickListener);

        view.post(() -> {
            highlightStatTab(statsTabFavorite);
            fadeStatTab(statsTabOther);
            fadeStatTab(statsTabAverage);
        });

        // If the statistics are already loaded, the update notification will have been missed,
        // so fire that notification now. If the statistics are non-null, they will be displayed.
        // If they are null (i.e., not yet loaded), the progress bar will be displayed until this
        // fragment, as a registered observer, is notified when loading is complete. Post the
        // firing of the event, so that it is received after "onCreateView" returns.
        view.post(() -> onStatisticsUpdated(StatisticsCache.getInstance().getStatistics()));
        StatisticsCache.getInstance().registerObserver(this); // Unregistered in "onDestroyView".
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
     * Since all stats views have the same base layout, they share the same views
     * with the same ids. This function uses the TooltipCompat function added in support library
     * v26 to set a tooltip for an icon in each of the stats tabs.
     *
     * @param viewId
     *      The ID of the view that should receive the tooltip
     * @param tooltipTextRes
     *      The string resource to be displayed
     */
    private void setTooltipText(@IdRes int viewId, @StringRes int tooltipTextRes) {
        TooltipCompat.setTooltipText(statsImprovementLayout.findViewById(viewId),
                getString(tooltipTextRes));
        TooltipCompat.setTooltipText(statsAverageLayout.findViewById(viewId),
                getString(tooltipTextRes));
        TooltipCompat.setTooltipText(statsOtherlayout.findViewById(viewId),
                getString(tooltipTextRes));
    }

    private ArrayList<Stat> buildLabelList(@ArrayRes int stringArrayRes) {
        ArrayList<Stat> statList = new ArrayList<>();
        // Used to alternate background colors in foreach
        int row = 0;

        for (String label : getResources().getStringArray(stringArrayRes)) {
            statList.add(new Stat(label, row));
            row++;
        }
        return statList;
    }

    private void highlightStatTab(TextView tab) {
        tab.setTextColor(ThemeUtils.fetchAttrColor(mContext, R.attr.graph_stats_card_text_color));
        tab.setBackground(buttonDrawable);
    }

    private void fadeStatTab(TextView tab) {
        tab.setTextColor(ThemeUtils.fetchAttrColor(mContext, R.attr
                .graph_stats_card_text_color_faded));
        tab.setBackground(buttonDrawableFaded);
    }

    private View.OnClickListener statTabClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View tab) {
            switch (tab.getId()) {
                case R.id.stats_tab_improvement:
                    if (!(statsTableViewFlipper.getDisplayedChild() == TAB_FAVORITE)) {
                        statsTableViewFlipper.setDisplayedChild(TAB_FAVORITE);
                        highlightStatTab(statsTabFavorite);
                        fadeStatTab(statsTabOther);
                        fadeStatTab(statsTabAverage);
                    }
                    break;
                case R.id.stats_tab_average:
                    if (!(statsTableViewFlipper.getDisplayedChild() == TAB_AVERAGE)) {
                        statsTableViewFlipper.setDisplayedChild(TAB_AVERAGE);
                        highlightStatTab(statsTabAverage);
                        fadeStatTab(statsTabOther);
                        fadeStatTab(statsTabFavorite);
                    }
                    break;
                case R.id.stats_tab_other:
                    if (!(statsTableViewFlipper.getDisplayedChild() == TAB_OTHER)) {
                        statsTableViewFlipper.setDisplayedChild(TAB_OTHER);
                        highlightStatTab(statsTabOther);
                        fadeStatTab(statsTabFavorite);
                        fadeStatTab(statsTabAverage);
                    }
                    break;
            }
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
        ArrayList<Stat> improvementList = buildImprovementStatList(stats);
        statsAverageGridView.setAdapter(new StatGridAdapter(mContext, averageList));
        statsOtherGridView.setAdapter(new StatGridAdapter(mContext, otherList));
        statsImprovementGridView.setAdapter(new StatGridAdapter(mContext, improvementList));


        // Display the statistics and hide the progress bar.
        setStatsTableVisibility(View.VISIBLE);
    }

    private ArrayList<Stat> buildImprovementStatList(Statistics stats) {
        // There are 2 columns (best, session), and 6 rows
        ArrayList<Stat> statsList = new ArrayList<>(5 * 2);


        // DO NOT CHANGE THE ORDER!
        // The adapter adds views in the list order, left -> right, top -> bottom, so if the order
        // is changed, times will be placed in the wrong spots on the grid!

        statsList.add(new Stat(convertTimeToString(tr(stats.getAllTimeStdDeviation()), PuzzleUtils
                .FORMAT_DEFAULT), Stat.SCOPE_GLOBAL, 0));
        statsList.add(new Stat(convertTimeToString(tr(stats.getSessionStdDeviation()), PuzzleUtils
                .FORMAT_DEFAULT), Stat.SCOPE_SESSION, 0));

        // Ao12
        // all time best
        statsList.add(new Stat(convertTimeToString(
                tr(stats.getAverageOf(12, false)
                        .getBestAverage()), PuzzleUtils.FORMAT_DEFAULT),
                Stat.SCOPE_GLOBAL, 1));
        // session best
        statsList.add(new Stat(convertTimeToString(
                tr(stats.getAverageOf(12, true)
                        .getBestAverage()), PuzzleUtils.FORMAT_DEFAULT),
                Stat.SCOPE_GLOBAL, 1));

        // Ao50
        // all time best
        statsList.add(new Stat(convertTimeToString(
                tr(stats.getAverageOf(50, false)
                        .getBestAverage()), PuzzleUtils.FORMAT_DEFAULT),
                Stat.SCOPE_GLOBAL, 2));
        // session best
        statsList.add(new Stat(convertTimeToString(
                tr(stats.getAverageOf(50, true)
                        .getBestAverage()), PuzzleUtils.FORMAT_DEFAULT),
                Stat.SCOPE_GLOBAL, 2));

        // Ao100
        // all time best
        statsList.add(new Stat(convertTimeToString(
                tr(stats.getAverageOf(100, false)
                        .getBestAverage()), PuzzleUtils.FORMAT_DEFAULT),
                Stat.SCOPE_GLOBAL, 3));
        // session best
        statsList.add(new Stat(convertTimeToString(
                tr(stats.getAverageOf(100, true)
                        .getBestAverage()), PuzzleUtils.FORMAT_DEFAULT),
                Stat.SCOPE_GLOBAL, 3));

        // Best time
        statsList.add(new Stat(convertTimeToString(tr(stats.getAllTimeBestTime()), PuzzleUtils
                .FORMAT_DEFAULT), Stat.SCOPE_GLOBAL, 4));
        statsList.add(new Stat(convertTimeToString(tr(stats.getSessionBestTime()), PuzzleUtils
                .FORMAT_DEFAULT), Stat.SCOPE_SESSION, 4));

        // Num solves
        statsList.add(new Stat(String.format(Locale.getDefault(), "%,d", stats.getAllTimeNumSolves()),
                Stat.SCOPE_GLOBAL, 5));
        statsList.add(new Stat(String.format(Locale.getDefault(), "%,d", stats.getSessionNumSolves()),
                Stat.SCOPE_SESSION, 5));

        return statsList;
    }

    private ArrayList<Stat> buildOtherStatList(Statistics stats) {
        // There are 2 columns (best, session), and 6 rows (best, worst, deviation, total time, mean
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

        statsList.add(new Stat(convertTimeToString(tr(stats.getAllTimeStdDeviation()), PuzzleUtils
                .FORMAT_DEFAULT), Stat.SCOPE_GLOBAL, 2));
        statsList.add(new Stat(convertTimeToString(tr(stats.getSessionStdDeviation()), PuzzleUtils
                .FORMAT_DEFAULT), Stat.SCOPE_SESSION, 2));

        statsList.add(new Stat(convertTimeToString(tr(stats.getAllTimeMeanTime()), PuzzleUtils
                .FORMAT_DEFAULT), Stat.SCOPE_GLOBAL, 3));
        statsList.add(new Stat(convertTimeToString(tr(stats.getSessionMeanTime()), PuzzleUtils
                .FORMAT_DEFAULT), Stat.SCOPE_SESSION, 3));

        statsList.add(new Stat(convertTimeToString(tr(stats.getAllTimeTotalTime()), PuzzleUtils
                .FORMAT_LARGE), Stat.SCOPE_GLOBAL, 4));
        statsList.add(new Stat(convertTimeToString(tr(stats.getSessionTotalTime()), PuzzleUtils
                .FORMAT_LARGE), Stat.SCOPE_SESSION, 4));

        statsList.add(new Stat(String.format(Locale.getDefault(), "%,d", stats.getAllTimeNumSolves()),
                Stat.SCOPE_GLOBAL, 5));
        statsList.add(new Stat(String.format(Locale.getDefault(), "%,d", stats.getSessionNumSolves()),
                Stat.SCOPE_SESSION, 5));

        return statsList;
    }
    /**
     * Builds a list of averages for use int {@link StatGridAdapter}
     * The order of the times is alternated (best, session, current, best, session...), so
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
