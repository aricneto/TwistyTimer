package com.aricneto.twistytimer.utils;

import java.util.AbstractList;
import java.util.List;

public class StatUtils {
    public static List<Long> asList(final long[] l) {
        return new AbstractList<Long>() {
            public Long get(int i) {return l[i];}
            // throws NPE if val == null
            public Long set(int i, Long val) {
                Long oldVal = l[i];
                l[i] = val;
                return oldVal;
            }
            public int size() { return l.length;}
        };
    }
}
