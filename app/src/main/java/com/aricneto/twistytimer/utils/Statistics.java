package com.aricneto.twistytimer.utils;

import java.util.HashMap;
import java.util.Map;

import static com.aricneto.twistytimer.utils.AverageCalculator.DNF;
import static com.aricneto.twistytimer.utils.AverageCalculator.UNKNOWN;

/**
 * A collection of {@link AverageCalculator} instances that distributes solve times to each
 * calculator. The calculators can be segregated into averages for times from the current session
 * only, and averages for times from all past sessions (including the current session). This also
 * provides a simple API to access the all-time and session mean, best and worst times and solve
 * count.
 *
 * @author damo
 */
public class Statistics {
    /**
     * The average calculators for averages of times across all sessions. The calculators are keyed
     * by the number of times used to calculate the average.
     */
    private final Map<Integer, AverageCalculator> mAllTimeACs = new HashMap<>();

    /**
     * The average calculators for averages of times in the current session only. The calculators
     * are keyed by the number of times used to calculate the average.
     */
    private final Map<Integer, AverageCalculator> mSessionACs = new HashMap<>();

    /**
     * An average calculator for solves only in the current session. May be {@code null}.
     */
    private AverageCalculator mOneSessionAC;

    /**
     * An average calculator for solves across all past sessions and the current session. May be
     * {@code null}.
     */
    private AverageCalculator mOneAllTimeAC;

    /**
     * Creates a new collection for statistics. Use a factory method to create a standard set of
     * statistics.
     */
    private Statistics() {
    }

    /**
     * Creates a new set of statistical averages for the standard reported statistics. Averages of
     * 3, 5, 12, 50, 100 and 1,000 are added for all sessions and for the current session only. The
     * average of 3 permits no DNF solves. The averages of 5 and 12 permit no more than one DNF
     * solves. The averages of 100 and 1,000 permit all but one solve to be a DNF solve.
     *
     * @return The standard set of solve time statistics.
     */
    public static Statistics newStandardStatistics() {
        final Statistics stats = new Statistics();

        // Averages for all sessions.
        stats.addAverageOf(3, true, false);
        stats.addAverageOf(5, true, false);
        stats.addAverageOf(12, true, false);
        stats.addAverageOf(50, false, false);
        stats.addAverageOf(100, false, false);
        stats.addAverageOf(1_000, false, false);

        // Averages for the current session only.
        stats.addAverageOf(3, true, true);
        stats.addAverageOf(5, true, true);
        stats.addAverageOf(12, true, true);
        stats.addAverageOf(50, false, true);
        stats.addAverageOf(100, false, true);
        stats.addAverageOf(1_000, false, true);

        return stats;
    }

    /**
     * Creates a new calculator for the "average of <i>n</i>" solve times.
     *
     * @param n
     *     The number of solve times that will be averaged (e.g., 3, 5, 12, ...). Must be greater
     *     than zero. If a calculator for the same value of {@code n} has been added for the same
     *     value of {@code isForCurrentSessionOnly}, the previous calculator will be overwritten.
     * @param disqualifyDNFs
     *     {@code true} if an average is disqualified if there are too many DNFs, or {@code false}
     *     if DNFs should be ignored (mostly). See {@link AverageCalculator#getCurrentAverage()}
     *     for more details on the calculation.
     * @param isForCurrentSessionOnly
     *     {@code true} to collect times only for the current session, or {@code false} to collect
     *     times across all past and current sessions.
     *
     * @throws IllegalArgumentException
     *     If {@code n} is not greater than zero.
     */
    private void addAverageOf(int n, boolean disqualifyDNFs, boolean isForCurrentSessionOnly) {
        final AverageCalculator ac = new AverageCalculator(n, disqualifyDNFs);

        if (isForCurrentSessionOnly) {
            mSessionACs.put(n, ac);
            if (mOneSessionAC == null) {
                mOneSessionAC = ac;
            }
        } else {
            mAllTimeACs.put(n, ac);
            if (mOneAllTimeAC == null) {
                mOneAllTimeAC = ac;
            }
        }
    }

    /**
     * Gets the calculator for the "average of <i>n</i>" solve times.
     *
     * @param n
     *     The number of solve times that were averaged (e.g., 3, 5, 12, ...).
     * @param isForCurrentSessionOnly
     *     {@code true} for the calculator that collected times only for the current session, or
     *     {@code false} to for the calculator that collected times across all past and current
     *     sessions.
     *
     * @return
     *     The requested average calculator, or {@code null} if no such calculator was defined for
     *     these statistics.
     */
    public AverageCalculator getAverageOf(int n, boolean isForCurrentSessionOnly) {
        if (isForCurrentSessionOnly) {
            return mSessionACs.get(n);
        }
        return mAllTimeACs.get(n);
    }

    /**
     * Records a solve time. The time value should be in milliseconds. If the solve is a DNF,
     * call {@link #addDNF} instead.
     *
     * @param time
     *     The solve time in milliseconds. Must be positive (though {@link AverageCalculator#DNF}
     *     is also accepted).
     * @param isForCurrentSession
     *     {@code true} if the DNF solve was added during the current session; or {@code false} if
     *     the solve was added in a previous session.
     *
     * @throws IllegalArgumentException
     *     If the time is not greater than zero and is not {@code DNF}.
     */
    public void addTime(long time, boolean isForCurrentSession) throws IllegalArgumentException {
        // "time" is validated on the first call to "AverageCalculator.addTime".
        for (final AverageCalculator allTimeAC : mAllTimeACs.values()) {
            allTimeAC.addTime(time);
        }

        if (isForCurrentSession) {
            for (final AverageCalculator sessionAC : mSessionACs.values()) {
                sessionAC.addTime(time);
            }
        }
    }

    /**
     * Records a did-not-finish (DNF) solve, one where no time was recorded.
     *
     * @param isForCurrentSession
     *     {@code true} if the DNF solve was added during the current session; or {@code false} if
     *     the solve was added in a previous session.
     */
    // This methods takes away any confusion about what time value represents a DNF.
    public void addDNF(boolean isForCurrentSession) {
        addTime(DNF, isForCurrentSession);
    }

    /**
     * Gets the best solve time of all those added to these statistics for a solve in the current
     * session.
     *
     * @return
     *     The best time ever added for the current session. The result will be
     *     {@link AverageCalculator#UNKNOWN} if no times have been added, or if all added times
     *     were DNFs.
     */
    public long getSessionBestTime() {
        return mOneSessionAC != null ? mOneSessionAC.getBestTime() : UNKNOWN;
    }

    /**
     * Gets the worst time (not a DNF) of all those added to these statistics for a solve in the
     * current session.
     *
     * @return
     *     The worst time ever added for the current session. The result will be
     *     {@link AverageCalculator#UNKNOWN} if no times have been added, or if all added times
     *     were DNFs.
     */
    public long getSessionWorstTime() {
        return mOneSessionAC != null ? mOneSessionAC.getWorstTime() : UNKNOWN;
    }

    /**
     * Gets the total number of solve times (including DNFs) that were added to these statistics
     * for the current session.
     *
     * @return The number of solve times that were added for the current session.
     */
    public int getSessionNumSolves() {
        return mOneSessionAC != null ? mOneSessionAC.getNumSolves() : 0;
    }

    /**
     * Gets the simple arithmetic mean time of all non-DNF solves that were added to these
     * statistics for the current session. The returned millisecond value is truncated to a whole
     * milliseconds values, not rounded.
     *
     * @return
     *     The mean time of all non-DNF solves that were added for the current session. The result
     *     will be {@link AverageCalculator#UNKNOWN} if no times have been added, or if all added
     *     times were DNFs.
     */
    public long getSessionMeanTime() {
        return mOneSessionAC != null ? mOneSessionAC.getMeanTime() : UNKNOWN;
    }

    /**
     * Gets the best solve time of all those added to these statistics for a solve in all past
     * and current sessions.
     *
     * @return
     *     The best time ever added for all past and current sessions. The result will be
     *     {@link AverageCalculator#UNKNOWN} if no times have been added, or if all added times
     *     were DNFs.
     */
    public long getAllTimeBestTime() {
        return mOneAllTimeAC != null ? mOneAllTimeAC.getBestTime() : UNKNOWN;
    }

    /**
     * Gets the worst time (not a DNF) of all those added to these statistics for a solve in all
     * past and current sessions.
     *
     * @return
     *     The worst time ever added for all past and current sessions. The result will be
     *     {@link AverageCalculator#UNKNOWN} if no times have been added, or if all added times
     *     were DNFs.
     */
    public long getAllTimeWorstTime() {
        return mOneAllTimeAC != null ? mOneAllTimeAC.getWorstTime() : UNKNOWN;
    }

    /**
     * Gets the total number of solve times (including DNFs) that were added to these statistics
     * for all past and current sessions.
     *
     * @return The number of solve times that were added for all past and current sessions.
     */
    public int getAllTimeNumSolves() {
        return mOneAllTimeAC != null ? mOneAllTimeAC.getNumSolves() : 0;
    }

    /**
     * Gets the simple arithmetic mean time of all non-DNF solves that were added to these
     * statistics for all past and current sessions. The returned millisecond value is truncated
     * to a whole milliseconds values, not rounded.
     *
     * @return
     *     The mean time of all non-DNF solves that were added for all past and current sessions.
     *     The result will be {@link AverageCalculator#UNKNOWN} if no times have been added, or if
     *     all added times were DNFs.
     */
    public long getAllTimeMeanTime() {
        return mOneAllTimeAC != null ? mOneAllTimeAC.getMeanTime() : UNKNOWN;
    }
}
