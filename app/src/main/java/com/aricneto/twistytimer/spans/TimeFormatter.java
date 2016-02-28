package com.aricneto.twistytimer.spans;

import com.aricneto.twistytimer.utils.PuzzleUtils;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.formatter.YAxisValueFormatter;

/**
 * Created by Ari on 06/02/2016.
 */
public class TimeFormatter implements YAxisValueFormatter{

    public TimeFormatter() {
    }

    @Override
    public String getFormattedValue(float value, YAxis yAxis) {
        return PuzzleUtils.convertTimeToStringWithoutMilli((int) value*1000);
    }
}
