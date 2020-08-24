package com.aricneto.twistytimer.spans;

import com.aricneto.twistytimer.utils.PuzzleUtils;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

/**
 * Created by Ari on 06/02/2016.
 */
public class TimeFormatter implements IAxisValueFormatter {

    public TimeFormatter() {
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        return PuzzleUtils.convertTimeToString((long) (value * 1_000L), PuzzleUtils.FORMAT_NO_MILLI);
    }
}
