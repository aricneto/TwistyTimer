package com.aricneto.twistytimer.solver;

public class StringUtils {
    public static String join(String separator, String[] values) {
        if (values.length == 0) {
            return "";
        }

        StringBuilder s = new StringBuilder();
        for (int i = 0; i < values.length - 1; i++) {
            s.append(values[i] + separator);
        }
        s.append(values[values.length - 1]);

        return s.toString();
    }
}