package com.hatopigeon.cubictimer.stats;

import android.content.Context;

import com.hatopigeon.cubicify.R;

import static com.hatopigeon.cubictimer.utils.ThemeUtils.fetchAttrColor;

/**
 * A collection of styling information for a chart. This is initialised from string resources and
 * theme attributes and requires an activity context to access the theme attributes. However, if
 * {@link ChartStatistics} are loaded in the background, an activity context may not be available
 * (and it may not be safe to attempt to make one available outside of the main UI thread).
 * Therefore, create this styling information with an activity context and then use it to
 * instantiate any required {@code ChartStatistics} instances.
 *
 * @author damo
 */
public class ChartStyle {
    // NOTE: Separating this from "ChartStatistics" avoid the need for that class to be given an
    // activity context (for theme attribute values), which allows it to be used from Loaders or
    // other tasks run on background threads.

    /**
     * The maximum number of "extra" colors supported. These correspond to the theme attributes
     * with the naming pattern {@code colorChartExtra*}. If more colors are required, more
     * attributes will need to be declared and then defined for each theme.
     */
    private static final int MAX_EXTRA_COLORS = 3;

    /**
     * The color to use for the main data set of the chart that displays all times for all sessions,
     * or all times for the current session.
     */
    private final int mAllTimesColor;

    /**
     * The color to use for the data set of the chart that display the progression of best times.
     */
    private final int mBestTimesColor;

    /**
     * The color to use for a limit line shown on the chart.
     */
    private final int mLimitLineColor;

    /**
     * The colors to use for the "extra" data sets of the chart. These are likely to be average-of-N
     * data sets.
     */
    // NOTE: An array makes it easier to support new "extra" color attributes to support charts
    // with more average-of-N, or other, data sets.
    private final int[] mExtraColors = new int[MAX_EXTRA_COLORS];

    /**
     * The label to use for the main data set of the chart that displays all times for all sessions,
     * or all times for the current session.
     */
    private final String mAllTimesLabel;

    /**
     * The label to use for the data set of the chart that display the progression of best times.
     */
    private final String mBestTimesLabel;

    /**
     * The label prefix to use for the average-of-N data sets of the chart. The value of "N" is
     * appended to the label prefix. For example, the prefix may be "Ao" and the value 12 may be
     * appended to form the label "Ao12" for the average-of-12 data set.
     */
    private final String mAvgLabelPrefix;

    /**
     * The label to use for a limit line shown on the chart.
     */
    private final String mLimitLineLabel;

    /**
     * The data format specification for the X-axis labels on the chart.
     */
    private final String mDateFormatSpec;

    /**
     * Creates a new chart style with label and color values. The labels are loaded from string
     * resources and the colors from theme attributes.
     *
     * @param context
     *     The context required to access the string resources and the line colors defined for the
     *     current theme. An application context is not sufficient to access the theme colors, so
     *     an activity context is required. A reference to this context is <i>not</i> retained by
     *     the new instance.
     */
    public ChartStyle(Context context) {
        mAllTimesColor  = fetchAttrColor(context, R.attr.colorChartAllTimes);
        mBestTimesColor = fetchAttrColor(context, R.attr.colorChartBestTimes);
        mLimitLineColor = fetchAttrColor(context, R.attr.colorChartMeanTime);

        mExtraColors[0] = fetchAttrColor(context, R.attr.colorChartExtra1);
        mExtraColors[1] = fetchAttrColor(context, R.attr.colorChartExtra2);
        mExtraColors[2] = fetchAttrColor(context, R.attr.colorChartExtra3);

        mAllTimesLabel  = context.getString(R.string.graph_legend_all_times);
        mBestTimesLabel = context.getString(R.string.graph_legend_best_times);
        mAvgLabelPrefix = context.getString(R.string.graph_legend_avg_prefix);
        mLimitLineLabel = context.getString(R.string.graph_mean);
        mDateFormatSpec = context.getString(R.string.shortDateFormat);
    }

    /**
     * Gets the color to use when representing the data set for all times for all sessions, or all
     * times for the current session.
     *
     * @return The color for all times.
     */
    public int getAllTimesColor() {
        return mAllTimesColor;
    }

    /**
     * Gets the color to use when representing the data set for the progression of best times.
     *
     * @return The color for the progression of best times.
     */
    public int getBestTimesColor() {
        return mBestTimesColor;
    }

    /**
     * Gets the color to use for a limit line displayed on the chart.
     *
     * @return The color for a limit line.
     */
    public int getLimitLineColor() {
        return mLimitLineColor;
    }

    /**
     * Gets a color to use when representing a data set for extra information. For example, a data
     * set for the average-of-N times.
     *
     * @param index The (zero-based) index of the extra color to be retrieved.
     * @return The extra color value.
     *
     * @throws IllegalArgumentException
     *     If the index is outside of the range for the number of extra colors supported by the
     *     application themes.
     */
    public int getExtraColor(int index) {
        if (index < 0 || index >= MAX_EXTRA_COLORS) {
            // NOTE: If more colors are needed, declare new "colorChartExtraX" attributes
            // in "values/attrs.xml" and then define the color values for each theme in
            // "values/styles.xml".
            throw new IllegalArgumentException(
                    "Index '" + index + "' out of range. Only " + MAX_EXTRA_COLORS + " supported.");
        }

        return mExtraColors[index];
    }

    /**
     * Gets the label to use when representing the data set for all times for all sessions, or all
     * times for the current session.
     *
     * @return The label for all times.
     */
    public String getAllTimesLabel() {
        return mAllTimesLabel;
    }

    /**
     * Gets the label to use when representing the data set for the progression of best times.
     *
     * @return The label for the progression of best times.
     */
    public String getBestTimesLabel() {
        return mBestTimesLabel;
    }

    /**
     * Gets the label prefix to use when representing a data set for average-of-N times. The value
     * of "N" should be appended to the prefix to form the full label.
     *
     * @return The label prefix for average-of-N data sets.
     */
    public String getAverageOfNLabelPrefix() {
        return mAvgLabelPrefix;
    }

    /**
     * Gets the label to use for a limit line displayed on the chart.
     *
     * @return The label for a limit line.
     */
    public String getLimitLineLabel() {
        return mLimitLineLabel;
    }

    /**
     * Gets the date format specification to use when formatting the date value shown along the
     * X-axis of the chart.
     *
     * @return The date format specification.
     */
    public String getDateFormatSpec() {
        return mDateFormatSpec;
    }
}
