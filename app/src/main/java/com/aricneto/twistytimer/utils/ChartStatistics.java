package com.aricneto.twistytimer.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;

import com.aricneto.twistify.R;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.joda.time.DateTime;

import static com.aricneto.twistytimer.utils.AverageCalculator.DNF;
import static com.aricneto.twistytimer.utils.AverageCalculator.UNKNOWN;

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

    /**
     * The colors to use for the chart lines. The first colour is used for the line showing all
     * solve times, the next for the best times, and the rest are used for the averages.
     */
    // TODO: Probably want colors that do not get lost against the choice of background color.
    // They may need to be specific to each color scheme/theme. Different colours for each line
    // are preferred, as it makes the chart legend
    private final static int[] LINE_COLORS = {
            Color.WHITE, Color.YELLOW, Color.MAGENTA, Color.CYAN, Color.RED, Color.LTGRAY
    };

    /**
     * The data set index for the graph of all solve times.
     */
    private static final int DS_ALL = 0;

    /**
     * The data set index for the graph of changes to the the best solve time.
     */
    private static final int DS_BEST = 1;

    /**
     * The data set index for the first of a series of graphs of "average-of-N" solve times. The
     * data set at this index corresponds to the average for the value of "N" at index zero in
     * {@link #mNsOfAverages}, the data set at the next index after this index corresponds to the
     * average for the value of "N" at index one in that array, and so on.
     */
    private static final int DS_AVG_0 = 2;

    /**
     * The collection of statistics that are required to support the calculation of any number of
     * average-of-N lines in the graph.
     */
    private final Statistics mStatistics;

    /**
     * The chart data for all solves and for each "average-of-N". The first data set (index zero)
     * is the data set for all solves. The other data sets correspond to the data sets for each
     * average-of-N, starting at index one and in the order of the entries in the
     * {@link #mNsOfAverages} array (i.e., the entry at index zero of that array is the value of
     * "N" for the average values in the data set at index one in the chart data).
     */
    // At present, a line chart is shown, but it could be changed to show a mix of different types
    // of charts in the future, so the field is "mChartData", not "mLineData".
    private final LineData mChartData = new LineData();

    /**
     * The values of "N" for all "average-of-N" data sets to be charted. In the
     */
    private final int[] mNsOfAverages;

    /**
     * Indicates if the chart data is for the current session only or for all past and current
     * sessions.
     */
    private final boolean mIsForCurrentSessionOnly;

    /**
     * The number of solve times recorded for the chart.
     */
    private int mCount;

    /**
     * The current best solve time recorded so far (in milliseconds).
     */
    private long mBestTime = Long.MAX_VALUE;

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
     *
     * @throws IllegalArgumentException
     *     If {@code statistics} is not configured for the current session only.
     */
    private ChartStatistics(Statistics statistics, boolean isForCurrentSessionOnly)
                throws IllegalArgumentException {
        if (!statistics.isForCurrentSessionOnly()) {
            // Enforcing this requirement means that there will be no excess clutter caused by
            // conditions in this class that need to decide between the two sets of times and
            // there will be no ambiguity when calling "Statistics.getAverageOf(int, boolean)".
            // Also, "Statistics.getNsOfAverages" (see below) has a clear meaning, as there will
            // be no case where one "N" can refer to averages for both the session and for all time.
            throw new IllegalArgumentException("Statistics must be for current session only.");
        }

        mStatistics = statistics;
        mNsOfAverages = statistics.getNsOfAverages();
        mIsForCurrentSessionOnly = isForCurrentSessionOnly;

        // Data set at index DS_ALL (0) is the set for all solve times, not an average of solve
        // times. The data sets are not configured here, only later in "getChartData", which makes
        // it easy to pass in a "Context" to access configuration resources for colours, etc.,
        // while keeping the constructor API cleaner for easier testing and avoiding exceptions.
        mChartData.addDataSet(new LineDataSet(null, null));

        // Data set at index DS_BEST (1) is the set for the changes to the best puzzle time.
        mChartData.addDataSet(new LineDataSet(null, null));

        // Add data sets--starting at index DS_AVG_0 (2)--for each "average-of-N" to be charted.
        for (final int ignored : mNsOfAverages) {
            mChartData.addDataSet(new LineDataSet(null, null));
        }
    }

    /**
     * Creates a new collector for chart data and statistics for the all-time chart. This includes
     * data for all solve times across all past and current sessions and the running averages of 50
     * and 100 consecutive times. These averages permit all but one solve to be a DNF solve.
     *
     * @return The collector for chart statistics.
     */
    public static ChartStatistics newAllTimeChartStatistics() {
        return new ChartStatistics(Statistics.newAllTimeAveragesChartStatistics(), false);
    }

    /**
     * Creates a new collector for chart data and statistics for the current session chart. This
     * includes data for all solve times across only the current session and the running averages
     * of 5 and 12 consecutive times. These averages permit no more than one solve to be a DNF
     * solve.
     *
     * @return The collector for chart statistics.
     */
    public static ChartStatistics newCurrentSessionChartStatistics() {
        return new ChartStatistics(Statistics.newCurrentSessionAveragesChartStatistics(), true);
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
     * Gets the chart data for all of the recorded solve times. The data includes line data sets
     * for all solve times and for running averages of solve times.
     *
     * @param context
     *     The context that may be used to access resources when configuring the elements of the
     *     chart data.
     *
     * @return
     *     The chart data set.
     */
    public LineData getChartData(Context context) {
        // Not concerned that if this is called more than once that the data sets will be configured
        // more than once. Only one call is expected and two calls should not break anything.

        final Resources res = context.getResources();
        final LineDataSet allDataSet = (LineDataSet) mChartData.getDataSetByIndex(DS_ALL);

        // A legend is enabled on the chart view in the graph fragment. The legend is created
        // automatically, but requires a unique labels and colors on each data set.
        allDataSet.setLabel(res.getString(R.string.graph_legend_all_times));

        // If all times are graphed, a thinner line will probably look better.
        allDataSet.setLineWidth(isForCurrentSessionOnly() ? 2f : 1f);
        // Dashed line can make peaks inaccurate. Also makes the graph look too "busy".
        //allDataSet.enableDashedLine(10f, 10f, 0);
        allDataSet.setDrawCircles(false);
        //allDataSet.setCircleRadius(3f);
        allDataSet.setColor(LINE_COLORS[DS_ALL]);
        allDataSet.setHighlightEnabled(false);
        //allDataSet.setCircleColor(Color.WHITE);
        allDataSet.setDrawValues(false);

        final LineDataSet bestDataSet = (LineDataSet) mChartData.getDataSetByIndex(DS_BEST);

        bestDataSet.setLabel(res.getString(R.string.graph_legend_best_times));
        bestDataSet.setLineWidth(1f);
        bestDataSet.enableDashedLine(3f, 6f, 0);
        bestDataSet.setColor(LINE_COLORS[DS_BEST]);
        bestDataSet.setDrawCircles(true);
        bestDataSet.setCircleRadius(3.5f);
        bestDataSet.setCircleColor(LINE_COLORS[DS_BEST]);
        bestDataSet.setHighlightEnabled(false);
        bestDataSet.setDrawValues(false);

        final String avgPrefix = res.getString(R.string.graph_legend_avg_prefix); // e.g., "Ao".

        for (int i = 0; i < mNsOfAverages.length; i++) {
            final LineDataSet avgDataSet = (LineDataSet) mChartData.getDataSetByIndex(i + DS_AVG_0);

            avgDataSet.setLabel(avgPrefix + mNsOfAverages[i]); // e.g., "Ao12".
            avgDataSet.setLineWidth(1f);
            avgDataSet.setDrawCircles(false);
            // Wrap around the range of 1 to LINE_COLORS.length-1 (color 0 is used for all data).
            avgDataSet.setColor(LINE_COLORS[i % (LINE_COLORS.length - DS_AVG_0) + DS_AVG_0]);
            avgDataSet.setHighlightEnabled(false);
            avgDataSet.setDrawValues(false);
        }

        return mChartData;
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
        boolean isSolveCharted = false;

        // The value of "time" is validated by "Statistics.addTime".
        mStatistics.addTime(time, true); // May throw IAE.

        if (time != DNF) {
            isSolveCharted = true;
            mChartData.addEntry(new Entry(time / 1_000f, mCount), DS_ALL);

            // Only update the recorded best time if it changes. The result should be a line that
            // traces (if lucky) a staircase descending from left to right (never rising).
            if (time < mBestTime) {
                mBestTime = time;
                mChartData.addEntry(new Entry(mBestTime / 1_000f, mCount), DS_BEST);
            }
        }

        for (int i = 0; i < mNsOfAverages.length; i++) {
            final AverageCalculator ac = mStatistics.getAverageOf(mNsOfAverages[i], true);
            final long averageTime = ac.getCurrentAverage();

            if (averageTime != AverageCalculator.DNF && averageTime != UNKNOWN) {
                isSolveCharted = true;
                // Add the average value to the appropriate data set, using a one-based data set
                // index, as index zero is used for all times (not averages).
                mChartData.addEntry(new Entry(averageTime / 1_000f, mCount), i + DS_AVG_0);
            }
        }

        // If the new solve and all current averages were DNF or UNKNOWN, then no data was added
        // to the chart, so do not add any X-axis value and do not increment the counter.
        if (isSolveCharted) {
            // TODO: Localize the order of the day and month fields in this label. For now, it is
            // unchanged from the way it was done in the past. However, it should, for example,
            // be ordered "MM/dd" for the USA, etc.
            mChartData.addXValue(new DateTime(date).toString("dd'/'MM"));
            mCount++;
        }
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
}
