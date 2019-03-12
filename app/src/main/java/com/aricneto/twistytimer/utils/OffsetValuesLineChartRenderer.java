package com.aricneto.twistytimer.utils;

import android.graphics.Canvas;

import com.aricneto.twistytimer.stats.ChartStatistics;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.renderer.LineChartRenderer;
import com.github.mikephil.charting.utils.Utils;

/**
 * <p>
 * A line chart renderer that can adjust the vertical positions of the text values drawn near the
 * data points. The purpose of this <i>ad hoc</i> implementation is to move the drawn text of each
 * "best time" value below its data point, rather than at its default position above the data point.
 * The problem with the default positioning is that "best" times are lowest times, so if the text
 * is drawn above the point, the line graph for "all times" will always intersect the text and make
 * it difficult to read. Placing the text below the data point avoids this problem.
 * </p>
 * <p>
 * This renderer is not very smart. The value of the Y-offset must be chosen based on trial and
 * error and will depend on factors such as the chosen text size, the drawing (or not) of circles
 * at the data points and the radius of those circles. None of these factors are considered by this
 * renderer, which just applies the given Y-offset. Start with offset values in the 40 to 50 range
 * and adjust things from there. However, {@link ChartStatistics#BEST_TIME_VALUES_Y_OFFSET_DP} is a
 * reasonable value for cases where the chart data sets are configured by that class.
 * </p>
 *
 * @author damo
 */
public class OffsetValuesLineChartRenderer extends LineChartRenderer {
    /**
     * The offset to apply to the default Y-coordinate of the value text to be drawn. The value is
     * in pixels.
     */
    private final float mValueYOffsetPX;

    /**
     * Creates a new line chart renderer that can adjust the vertical positions of the text values
     * drawn near the data points.
     *
     * @param lineChart
     *     The line chart on which the renderer will be set. This constructor does not set the new
     *     instance it creates as the renderer on the given chart. After construction, call
     *     {@code LineChart.setRenderer} passing the new instance of this class to set the renderer.
     * @param valueYOffsetDP
     *     The offset to apply to the default Y-coordinate of the value text to be drawn. Positive
     *     offsets move the position of values down and negative offsets move it up. See the class
     *     description for more details on how to choose the value. The value is in DIP units.
     */
    public OffsetValuesLineChartRenderer(LineChart lineChart, float valueYOffsetDP) {
        super(lineChart, lineChart.getAnimator(), lineChart.getViewPortHandler());

        // When a "LineChart" is created "Utils" is initialised with the necessary display metrics
        // to convert DIPs to pixels based on the display. As a "LineChart" instance is passed to
        // this constructor, "Utils" should be ready to go.
        mValueYOffsetPX = Utils.convertDpToPixel(valueYOffsetDP);
    }

    /**
     * Overrides the default drawing position by offsetting the Y-coordinate of the default value
     * position. The value text is drawn if the data point is in bounds, so the value text may be
     * slightly out-of-bounds.
     */
    @Override
    public void drawValue(Canvas c, IValueFormatter formatter, float value, Entry entry, int dataSetIndex, float x, float y, int color) {
        // NOTE: The given value of "y" is calculated in "LineChartRenderer.drawValues". It is the
        // Y-coordinate for the *baseline* of the drawn text. By default, it is offset above the
        // data point based on the circle radius value (even if drawing of circles is disabled) and
        // is offset even more if drawing of circles is enabled. Setting a negative circle radius
        // actually works to reposition the text, but then the circles are not drawn.
        //
        // Therefore, choosing a value for "mValueYOffset" depends on the circle radius and whether
        // or not circles are enabled. However, because the text extends *up* from its baseline,
        // if the text is drawn below the data point, the size of the text must also be considered
        // when choosing the value of the Y offset, as larger text must be offset more to keep the
        // top of the text below the data point.
        //
        // Because the values are numeric, there are no descenders below the baseline (e.g., like
        // the descending loop of a lower-case "g"). To prevent the value text being drawn if it
        // touches or crosses the bottom horizontal axis, simply guard the call to "super.drawValue"
        // with a test of "mViewPortHandler.isInBoundsBottom(y + mValueYOffsetPX)". However, without
        // this bounds-checking, it looks quite nice: drawing the text outside the axis until the
        // point itself touches the axis. Zoom in and drag the chart around to experiment.

        // TODO: It might be worth considering if these should be painted in "reverse video" to
        // make them more readable, i.e., draw a rectangle in the "text color" and then draw the
        // text on top in a contrasting color (maybe just black or white).
        super.drawValue(c, formatter, value, entry, dataSetIndex, x, y + mValueYOffsetPX, color);
    }
}
