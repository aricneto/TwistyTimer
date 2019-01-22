package com.aricneto.twistytimer.stats;

import com.aricneto.twistytimer.utils.OffsetValuesLineChartRenderer;
import com.aricneto.twistytimer.utils.PuzzleUtils;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import static com.aricneto.twistytimer.stats.AverageCalculator.DNF;
import static com.aricneto.twistytimer.stats.AverageCalculator.UNKNOWN;

/**
 * A collector for solve times and related statistics (average times) to be presented in a chart.
 *
 * @author damo
 */
public class ChartStatistics {
    // NOTE: This "ChartStatistics" class does not extend "Statistics", it contains an instance of
    // that class. The API of "Statistics" is not compatible, as it is one-dimensional (requiring
    // only a solve times to be added), but "ChartStatistics" is two-dimensional (requiring both
    // solve times and the date of each of the solve events). Re-use by containment avoids the mess
    // of trying to hide "Statistics.addTime", "Statistics.addDNF" and various other methods.

    // NOTE: "ChartStatistics" is expected to be used from a Loader or AsyncTask, so it is
    // preferable not to have this class depend on a Context, as that could lead to memory leaks.
    // Instead, "ChartStyle" captures the necessary values from resources and theme attributes via
    // a Context and then it can be passed when creating an instance of this class. Neither class
    // then needs to hold a Context. "ChartStyle" can be created before the Loader or AsyncTask is
    // invoked and passed in before execution.

    /**
     * The line width to use in the chart when a thicker line is appropriate. The value is in DIP
     * units.
     */
    private static final float LINE_WIDTH_THICK_DP = 2.0f;

    /**
     * The line width to use in the chart when a thinner line is appropriate. The value is in DIP
     * units.
     */
    private static final float LINE_WIDTH_THIN_DP = 1.0f;

    /**
     * The text size to use for limit line marking the mean time. The value is in DIP units.
     */
    private static final float MEAN_LIMIT_LINE_TEXT_SIZE_DP = 12f;

    /**
     * The text size to use for the value text shown near the data points for the best times. The
     * value is in DIP units.
     */
    private static final float BEST_TIME_VALUES_TEXT_SIZE_DP = 10f;

    /**
     * The circle radius to use for the circles drawn at the data points for the "best" times. The
     * value is in DIP units.
     */
    private static final float BEST_TIME_CIRCLE_RADIUS_DP = 3.5f;

    /**
     * The Y-coordinate offset to apply to the value text of the "best" times to cause the text to
     * be drawn below the corresponding data point instead of above it. The value is in DIP units.
     */
    // NOTE: This calculation approximately flips the text position to the opposite side of the
    // data point (i.e., from above to below) based on the way "LineChartRenderer.drawValues" does
    // the calculation (baseline is offset by -1.75 * circle-radius). Here, we reverse that offset
    // twice to set the reflected position of the top of the text *below* the point and then offset
    // by the text size to set the position on the new text baseline.
    private static final float BEST_TIME_VALUES_Y_OFFSET_DP
            = BEST_TIME_CIRCLE_RADIUS_DP * 1.75f * 2f + BEST_TIME_VALUES_TEXT_SIZE_DP;

    /**
     * The data set index for the graph of all solve times.
     */
    private static final int DS_ALL = 0;

    /**
     * The data set index for the graph of changes to the the best solve time.
     */
    private static final int DS_BEST = 1;

    /**
     * The data set index for the first of a series of graphs of "average-of-N" (AoN) solve times.
     * The data set at this index corresponds to the AoN for the value of "N" at index zero in
     * {@link #mNsOfAverages}. Like the {@link #DS_ALL} and {@link #DS_BEST} indices, these AoN
     * indices come in pairs, with the first index for the data set of AoN times and the second for
     * the best AoN time for that "N". The data set at {@code DS_AVG_0 + 2} corresponds to the
     * average for the value of "N" at index one in {@code mNsOfAverages}, and so on.
     */
    private static final int DS_AVG_0 = 2;

    /**
     * The collection of statistics that are required to support the calculation of any number of
     * average-of-N lines in the graph.
     */
    private final Statistics mStatistics;

    /**
     * The styles that will be applied to the data sets of the chart.
     */
    private final ChartStyle mChartStyle;

    /**
     * The values of "N" for all "average-of-N" data sets to be charted.
     */
    private final int[] mNsOfAverages;

    /**
     * Indicates if the chart data is for the current session only or for all past and current
     * sessions.
     */
    private final boolean mIsForCurrentSessionOnly;

    /**
     * The chart data for all solves and for each "average-of-N". The first data set (index zero)
     * is the data set for all solves. The other data sets correspond to the data sets for each
     * average-of-N, starting at index one and in the order of the entries in the
     * {@link #mNsOfAverages} array (i.e., the entry at index zero of that array is the value of
     * "N" for the average values in the data set at index one in the chart data).
     */
    // At present, a line chart is shown, but it could be changed to show a mix of different types
    // of charts in the future, so the field is "mChartData", not "mLineData".
    private LineData mChartData;

    /**
     * The current X-index for the solve time added to the chart.
     */
    private int mXIndex;

    /**
     * The current best solve time recorded so far (in milliseconds).
     */
    private long mBestTime;

    /**
     * The "pre-compiled" date formatter for the X-axis labels.
     */
    private DateTimeFormatter mXValueFormatter;

    /**
     * The day for which the previous data set entry was recorded. If the day has not changed, the
     * value of {@link #mPrevEntryXValue} can be re-used instead of re-formatting the date object
     * to a new string. If {@code null}, there was no previous entry.
     */
    private LocalDate mPrevEntryDay;

    /**
     * The formatted X-value with which the previous data set entry was recorded. If the day has
     * not changed (tested against {@link #mPrevEntryDay}), this X-value can be re-used instead of
     * re-formatting the date object to a new string. If {@code null}, there was no previous entry.
     */
    private String mPrevEntryXValue;

    /**
     * The label to apply to the "limit line".
     */
    //private final String mLimitLineLabel;

    /**
     * The color to apply to the "limit line".
     */
    //private final int mLimitLineColor;

    /**
     * Creates a new collector for chart statistics that will chart all collected values and all
     * averages-of-N values collected by the given {@code Statistics}. Each instance of
     * {@code ChartStatistics} can collect statistics for the set of solve times for the current
     * session, or the set of solve times for all sessions, but not a combination of the two.
     *
     * @param statistics
     *     The statistics that will be updated as each solve time is recorded and that will provide
     *     the average values to be charted. Must not be {@code null}. Regardless of whether or not
     *     the chart data is for the current session or for all sessions, the statistics must be
     *     configured to collect only solve times for the current session (i.e.,
     *     {@link Statistics#isForCurrentSessionOnly()} must return {@code true}).
     * @param isForCurrentSessionOnly
     *     {@code true} if the solve times to be charted are only those solves added in the current
     *     sessions; or {@code false} if the solve times are only those solves added across all
     *     sessions.
     * @param chartStyle
     *     The styling information for the chart. This defines the labels and colors for the data
     *     sets, among other information.
     *
     * @throws IllegalArgumentException
     *     If {@code statistics} is not configured for the current session only.
     * @throws IllegalStateException
     *     If there are more than three average-of-N lines to be graphed.
     */
    private ChartStatistics(Statistics statistics, boolean isForCurrentSessionOnly,
                            ChartStyle chartStyle)
                throws IllegalArgumentException, IllegalStateException {
        if (!statistics.isForCurrentSessionOnly()) {
            // Enforcing this requirement means that there will be no excess clutter caused by
            // conditions in this class that need to decide between the two sets of times and
            // there will be no ambiguity when calling "Statistics.getRow(int, boolean)".
            // Also, "Statistics.getNsOfAverages" (see below) has a clear meaning, as there will
            // be no case where one "N" can refer to averages for both the session and for all time.
            throw new IllegalArgumentException("Statistics must be for current session only.");
        }

        mStatistics = statistics;
        mChartStyle = chartStyle;
        mNsOfAverages = statistics.getNsOfAverages();
        mIsForCurrentSessionOnly = isForCurrentSessionOnly;

        /*/ Unfortunately, the mean value can only be set in the "LimitLine" constructor, so save
        // the label and color of the line now (while a "Context" is available) and create the line
        // later in "applyTo".
        mLimitLineLabel = chartStyle.getLimitLineLabel();
        mLimitLineColor = chartStyle.getLimitLineColor();
        */

        // Set the formatter for the date label on the chart X-axis. Localise the format, mostly to
        // support the "MM/DD" order used in the USA. It will fall back to the most common "DD/MM"
        // format (from "values/formats.xml") if no more specific localised format is found (such
        // as in "values-en-rUS/formats.xml". This also "pre-compiles" the pattern, making the
        // formatting operation faster later.
        mXValueFormatter = DateTimeFormat.forPattern(chartStyle.getDateFormatSpec());

        // Initialise and reset everything to a sane, empty state.
        reset();
    }

    /**
     * Resets all chart data and statistics to their initial, empty state.
     */
    public void reset() {
        mStatistics.reset();
        mXIndex = 0;
        mBestTime = Long.MAX_VALUE;
        mPrevEntryDay = null;
        mPrevEntryXValue = null;

        // There does not seem to be an easy way to clear existing Y-values *and* X-values from
        // each data set in the chart data. Just create a new one instead.
        mChartData = new LineData();

        // The order in which the data sets are added is important to ensure that "DS_ALL", etc.
        // remain meaningful.
        addMainDataSets(mChartData, mChartStyle.getAllTimesLabel(), mChartStyle.getAllTimesColor(),
                mChartStyle.getBestTimesLabel(), mChartStyle.getBestTimesColor());

        for (int nIndex = 0; nIndex < mNsOfAverages.length; nIndex++) {
            addAoNDataSets(mChartData,
                    mChartStyle.getAverageOfNLabelPrefix() + mNsOfAverages[nIndex],
                    mChartStyle.getExtraColor(nIndex));
        }
    }

    /**
     * Adds the main data set for all times and the data set for the progression of record best
     * times among all times. The progression of best times are marked in a different color to the
     * main line of all time using circles lined with a dashed line. This will appear to connect
     * the lowest troughs along the main line of all times.
     *
     * @param chartData The chart data to which to add the new data sets.
     * @param allLabel  The label of the all-times line.
     * @param allColor  The color of the all-times line.
     * @param bestLabel The label of the best-times line.
     * @param bestColor The color of the best-times line.
     */
    private void addMainDataSets(LineData chartData, String allLabel, int allColor,
                                 String bestLabel, int bestColor) {
        // Main data set for all solve times.
        chartData.addDataSet(createDataSet(allLabel, allColor));

        // Data set to show the progression of best times along the main line of all times.
        final LineDataSet bestDataSet = createDataSet(bestLabel, bestColor);

        bestDataSet.enableDashedLine(3f, 6f, 0f);

        bestDataSet.setDrawCircles(true);
        bestDataSet.setCircleRadius(BEST_TIME_CIRCLE_RADIUS_DP);
        bestDataSet.setCircleColor(bestColor);

        bestDataSet.setDrawValues(false);
        bestDataSet.setValueTextColor(bestColor);
        bestDataSet.setValueTextSize(BEST_TIME_VALUES_TEXT_SIZE_DP);
        bestDataSet.setValueFormatter(new TimeChartValueFormatter());

        chartData.addDataSet(bestDataSet);
    }

    /**
     * Adds the data set for the average-of-N (AoN) times and the corresponding data set for the
     * single best average time for that value of "N". The best AoN times are not shown as a
     * progression; only one time is shown and it superimposed on its main AoN line, rendered in
     * the same color as a circle and with the value drawn on the chart.
     *
     * @param chartData The chart data to which to add the new data sets.
     * @param label     The label of the AoN line and best AoN time marker.
     * @param color     The color of the AoN line and best AoN time marker.
     */
    private void addAoNDataSets(LineData chartData, String label, int color) {
        // Main AoN data set for all AoN times for one value of "N".
        chartData.addDataSet(createDataSet(label, color));

        // Data set for the single best AoN time for this "N".
        final LineDataSet bestAoNDataSet = createDataSet(label, color);

        bestAoNDataSet.setDrawCircles(true);
        bestAoNDataSet.setCircleRadius(BEST_TIME_CIRCLE_RADIUS_DP);
        bestAoNDataSet.setCircleColor(color);

        // Drawing the value of the best AoN time for each "N" seems like it would be a good idea,
        // but the values are really hard because they appear over other chart lines and sometimes
        // over the values drawn for the best time progression. Disabling them is no great loss,
        // as the statistics table shows the same values, anyway. Just showing a circle to mark
        // the best AoN time looks well enough on its own.
        bestAoNDataSet.setDrawValues(false);
//        bestAoNDataSet.setValueTextColor(color);
//        bestAoNDataSet.setValueTextSize(BEST_TIME_VALUES_TEXT_SIZE_DP);
//        bestAoNDataSet.setValueFormatter(new TimeChartValueFormatter());

        chartData.addDataSet(bestAoNDataSet);
    }

    /**
     * Creates a data set with the given label and color. Highlights and drawing of values and
     * circles are disabled, as that is common for many cases.
     *
     * @param label The label to assign to the new data set.
     * @param color The line color to set for the new data set.
     */
    private LineDataSet createDataSet(String label, int color) {
        // A legend is enabled on the chart view in the graph fragment. The legend is created
        // automatically, but requires a unique labels and colors on each data set.
        final LineDataSet dataSet = new LineDataSet(null, label);

        // A dashed line can make peaks inaccurate. It also makes the graph look too "busy". It
        // is OK for some uses, such as progressions of best times, but that is left to the caller
        // to change once this new data set is returned.
        //
        // If graphing only times for a session, there will be fewer, and a thicker line will look
        // well. However, if all times are graphed, a thinner line will probably look better, as
        // the finer details will be more visible.
        dataSet.setLineWidth(getLineWidth());
        dataSet.setColor(color);
        dataSet.setDrawCubic(true);
        dataSet.setCubicIntensity(0.2f);
        dataSet.setHighlightEnabled(false);

        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);

        return dataSet;
    }

    /**
     * Creates a new collector for chart data and statistics for the all-time chart. This includes
     * data for all solve times across all past and current sessions and the running averages of 50
     * and 100 consecutive times. These averages permit all but one solve to be a DNF solve.
     *
     * @param chartStyle
     *     The chart style information required for the data sets that will be populated with
     *     statistics.
     *
     * @return
     *     The collector for chart statistics.
     */
    public static ChartStatistics newAllTimeChartStatistics(ChartStyle chartStyle) {
        return new ChartStatistics(
                Statistics.newAllTimeAveragesChartStatistics(), false, chartStyle);
    }

    /**
     * Creates a new collector for chart data and statistics for the current session chart. This
     * includes data for all solve times across only the current session and the running averages
     * of 5 and 12 consecutive times. These averages permit no more than one solve to be a DNF
     * solve.
     *
     * @param chartStyle
     *     The chart style information required for the data sets that will be populated with
     *     statistics.
     *
     * @return
     *     The collector for chart statistics.
     */
    public static ChartStatistics newCurrentSessionChartStatistics(ChartStyle chartStyle) {
        return new ChartStatistics(
                Statistics.newCurrentSessionAveragesChartStatistics(), true, chartStyle);
    }

    /**
     * Indicates if all of the charted times required are across the current session only. If
     * only times for the current session are required, a more efficient approach may be taken to
     * load the saved solve times.
     *
     * @return
     *     {@code true} if all required chart data applies only to solve times for the current
     *     session; or {@code false} if the data includes times across all past and current
     *     sessions.
     */
    public boolean isForCurrentSessionOnly() {
        return mIsForCurrentSessionOnly;
    }

    /**
     * Applies the data sets for the collected chart statistics to the given chart and sets the
     * appropriate legend.
     *
     * @param chart The chart to which to apply the collected statistics.
     */
    public void applyTo(LineChart chart) throws IllegalStateException {
        // It seems that it is important to set the custom legend before setting the chart data.
        // If it is done the other way around, some cached values related to the layout of the
        // legend for the previous statistics are not updated to match the new data sets and
        // crashes occur during rendering of the legend.
        configureLegend(chart.getLegend());

        /*
        chart.getAxisLeft().removeAllLimitLines();

        if (getMeanTime() != AverageCalculator.UNKNOWN) { // At least one non-DNF solve time?
            final LimitLine ll = new LimitLine(getMeanTime() / 1_000f, mLimitLineLabel);

            ll.setLineColor(mLimitLineColor);
            ll.setLineWidth(getLineWidth());
            ll.enableDashedLine(20f, 10f, 0f);

            ll.setTextColor(mLimitLineColor);
            ll.setTextSize(MEAN_LIMIT_LINE_TEXT_SIZE_DP);
            ll.setLabelPosition(LimitLine.LimitLabelPosition.LEFT_TOP);

            chart.getAxisLeft().addLimitLine(ll);
        }*/

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
        chart.setMaxVisibleValueCount(2_000);
        // Use a custom renderer to draw the values of the best times *below* their data points.
        chart.setRenderer(new OffsetValuesLineChartRenderer(chart, BEST_TIME_VALUES_Y_OFFSET_DP));

        chart.setData(mChartData);
    }

    /**
     * Configures the given {@code Legend} for the data sets that will be displayed by the chart.
     *
     * @param legend The legend to be configured.
     */
    private void configureLegend(Legend legend) {
        // NOTE: If "Legend" is allowed to configure itself automatically, it will add two entries
        // for each AoN/best-AoN pair of data sets, but only one should be shown. Go custom....
        final int numNs = mNsOfAverages.length;
        final String[] labels = new String[DS_AVG_0 + numNs];
        final int[] colors = new int[DS_AVG_0 + numNs];

        LineDataSet ds;

        ds = (LineDataSet) mChartData.getDataSetByIndex(DS_ALL);
        labels[DS_ALL] = ds.getLabel();
        colors[DS_ALL] = ds.getColor();

        ds = (LineDataSet) mChartData.getDataSetByIndex(DS_BEST);
        labels[DS_BEST] = ds.getLabel();
        colors[DS_BEST] = ds.getColor();

        for (int nIndex = 0; nIndex < numNs; nIndex++) {
            // A main AoN data set. The "best AoN" data sets are not represented in the legend.
            ds = (LineDataSet) mChartData.getDataSetByIndex(DS_AVG_0 + 2 * nIndex);
            labels[DS_AVG_0 + nIndex] = ds.getLabel();
            colors[DS_AVG_0 + nIndex] = ds.getColor();
        }

        legend.setCustom(colors, labels);
    }

    /**
     * Records a solve time. The time value should be in milliseconds. If the solve is a DNF,
     * call {@link #addDNF} instead.
     *
     * @param time
     *     The solve time in milliseconds. Must be positive (though {@link AverageCalculator#DNF}
     *     is also accepted).
     * @param date
     *     The date on which the solve time was recorded. The values should be in milliseconds
     *     since the Unix epoch time.
     *
     * @throws IllegalArgumentException
     *     If the time is not greater than zero and is not {@code DNF}.
     */
    public void addTime(long time, long date) {
        boolean isEntryAdded = false;

        // The value of "time" is validated by "Statistics.addTime".
        mStatistics.addTime(time, true); // May throw IAE.

        if (time != DNF) {
            mChartData.addEntry(new Entry(time / 1_000f, mXIndex), DS_ALL);
            isEntryAdded = true;

            // Only update the recorded best time if it changes. The result should be a line that
            // traces (if lucky) a staircase descending from left to right (never rising).
            if (time < mBestTime) {
                mBestTime = time;
                mChartData.addEntry(new Entry(mBestTime / 1_000f, mXIndex), DS_BEST);
            }
        }

        for (int nIndex = 0; nIndex < mNsOfAverages.length; nIndex++) {
            final AverageCalculator ac = mStatistics.getAverageOf(mNsOfAverages[nIndex], true);
            final long averageTime = ac.getCurrentAverage();

            if (averageTime != AverageCalculator.DNF && averageTime != UNKNOWN) {
                // AoN data sets start at "DS_AVG_0" and come in pairs. In each pair, the first is
                // the data set for all AoN times for that "N" and the second is the data set for
                // the single best AoN time for that "N".
                final int aonDSIndex = DS_AVG_0 + 2 * nIndex;
                final float aonYValue = averageTime / 1_000f;

                mChartData.addEntry(new Entry(aonYValue, mXIndex), aonDSIndex);
                isEntryAdded = true;

                // Just keep a single entry in each data set for each best AoN; it will be rendered
                // as a single circle that is coincident with the main AoN line and its value will
                // be drawn. There is no line charting the *progression* of best AoN times.
                final LineDataSet bestAoNDS
                        = (LineDataSet) mChartData.getDataSetByIndex(aonDSIndex + 1);

                if (bestAoNDS.getEntryCount() > 0) { // Should be 0 or 1, nothing more.
                    final Entry oldEntry = bestAoNDS.getEntryForIndex(0); // Not an X-index.

                    if (aonYValue < oldEntry.getVal()) {
                        // A new best AoN time! Replace the old one with this new one.
                        bestAoNDS.removeEntry(oldEntry);
                        bestAoNDS.addEntry(new Entry(aonYValue, mXIndex));
                    }
                } else {
                    // This is the first AoN time, so just add it as the best (and only) AoN time.
                    bestAoNDS.addEntry(new Entry(aonYValue, mXIndex));
                }
            }
        }

        if (isEntryAdded) {
            // Add the X-axis value (a formatted solve date with just the day and month). The
            // "date" value is interpreted as an instant in time relative to the Unix epoch in the
            // UTC time zone. When "LocalDate" trims off the time to represent only a day, that
            // day corresponds to a day in the system default (local) time zone that corresponds
            // to that instant in time.
            final LocalDate day = new LocalDate(date);

            // The nature of the data means that sequential times will often be from the same
            // session performed on the same day. Therefore, it is easy to optimise this a bit by
            // not formatting the same day over-and-over. This may also save memory, as only a
            // single "String" instance is created for each day.
            final String xValue;

            if (day.equals(mPrevEntryDay)) { // Also implies "mPrevEntryDay != null"
                // Day has not changed, so re-use the previous X-value.
                xValue = mPrevEntryXValue;
            } else {
                // A new day (or the very first day), so format the day to a string and cache it.
                xValue = mPrevEntryXValue = mXValueFormatter.print(day);
                mPrevEntryDay = day;
            }

            mChartData.addXValue(xValue);
            mXIndex++;
        }
        // If the new solve and all current averages were DNF or UNKNOWN, then no entry was added
        // to the chart, so do not add any X-axis value and do not increment the X-index.
    }

    /**
     * Records a did-not-finish (DNF) solve, one where no time was recorded.
     *
     * @param date
     *     The date on which the solve time was recorded. The values should be in milliseconds
     *     since the Unix epoch time.
     */
    // This methods takes away any confusion about what time value represents a DNF.
    public void addDNF(long date) {
        addTime(DNF, date);
    }

    /**
     * Gets the simple arithmetic mean time of all non-DNF solves that were added to these chart
     * statistics. The returned millisecond value is truncated to a whole milliseconds value, not
     * rounded.
     *
     * @return
     *     The mean time of all non-DNF solves that were added for the chart statistics. The result
     *     will be {@link AverageCalculator#UNKNOWN} if no times have been added, or if all added
     *     times were DNFs.
     */
    public long getMeanTime() {
        return mStatistics.getSessionMeanTime();
    }

    /**
     * Gets the width to use for all lines on the chart. The lines are shown slightly wider when
     * only the session times are displayed, as there will be less data points on the chart.
     *
     * @return The line width (in DIP units).
     */
    private float getLineWidth() {
        // Perhaps adjust this for the number of data points in the chart data.
        return isForCurrentSessionOnly() ? LINE_WIDTH_THICK_DP : LINE_WIDTH_THIN_DP;
    }

    /**
     * A formatter for time values displayed beside points in the chart. This converts the stored
     * values (in seconds) to the normal representation.
     */
    private static class TimeChartValueFormatter implements ValueFormatter {
        @Override
        public String getFormattedValue(float value, Entry entry, int dataSetIndex,
                                        ViewPortHandler viewPortHandler) {
            // "value" is in fractional seconds. Convert to whole milliseconds and format it.
            return PuzzleUtils.convertTimeToString(Math.round(value * 1_000), PuzzleUtils.FORMAT_DEFAULT);
        }
    }
}
