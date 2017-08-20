package com.aricneto.twistytimer.stats;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static com.aricneto.twistytimer.stats.AverageCalculator.DNF;
import static com.aricneto.twistytimer.stats.AverageCalculator.UNKNOWN;

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
     * The frequencies of solve times across all sessions. The keys are the solve times in
     * milliseconds, but truncated to whole seconds, and the values are the number of solve times.
     * {@link AverageCalculator#DNF} may also be a key.
     */
    // NOTE: A "TreeMap" ensures that the entries are ordered by key (time) value.
    private final TreeMap<Long, Integer> mAllTimeTimeFreqs = new TreeMap<>();

    /**
     * The frequencies of solve times for the current session. The keys are the solve times in
     * milliseconds, but truncated to whole seconds and the values are the number of solve times.
     * {@link AverageCalculator#DNF} may also be a key.
     */
    private final TreeMap<Long, Integer> mSessionTimeFreqs = new TreeMap<>();

    /**
     * An average calculator for solves across all past sessions and the current session. May be
     * {@code null}.
     */
    private AverageCalculator mOneAllTimeAC;

    /**
     * An average calculator for solves only in the current session. May be {@code null}.
     */
    private AverageCalculator mOneSessionAC;

    /**
     * Creates a new collection for statistics. Use a factory method to create a standard set of
     * statistics.
     */
    private Statistics() {
    }

    /**
     * Creates a new set of statistical averages for the detailed table of all-time and session
     * statistics reported on the statistics/graph tab. Averages of 3, 5, 12, 50, 100 and 1,000 are
     * added for all sessions and for the current session only. The average of 3 permits no DNF
     * solves. The averages of 5 and 12 permit no more than one DNF solves. The averages of 50, 100
     * and 1,000 permit all but one solve to be a DNF solve.
     *
     * @return The detailed set of all-time solve time statistics for the statistics/graph tab.
     */
    public static Statistics newAllTimeStatistics() {
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
     * Creates a new set of statistical averages for the averages displayed in the graph of the
     * times from the all past and current sessions. Averages of 50 and 100 are added for the
     * <i>current session only</i> (a requirement for the {@link ChartStatistics} constructor, even
     * though the all-time data will be graphed). These averages permit all but one solves to be
     * DNFs.
     *
     * @return The solve time statistics for graphing the all-time averages.
     */
    static Statistics newAllTimeAveragesChartStatistics() {
        final Statistics stats = new Statistics();

        // Averages for the current session only IS NOT A MISTAKE! The "ChartStatistics" class
        // passes all data in for the "current session", but makes a distinction using its own API.
        stats.addAverageOf(50, false, true);
        stats.addAverageOf(100, false, true);

        return stats;
    }

    /**
     * Creates a new set of statistical averages for the averages displayed in the graph of the
     * times from the current session. Averages of 5 and 12 are added for the current session
     * only. These averages permit no more than one DNF solve.
     *
     * @return The solve time statistics for graphing the current session averages.
     */
    static Statistics newCurrentSessionAveragesChartStatistics() {
        final Statistics stats = new Statistics();

        stats.addAverageOf(5, true, true);
        stats.addAverageOf(12, true, true);

        return stats;
    }

    /**
     * Resets all statistics and averages that have been collected previously. The average-of-N
     * calculators and time frequencies are reset, but the average-of-N calculators are not removed.
     */
    public void reset() {
        for (final AverageCalculator allTimeAC : mAllTimeACs.values()) {
            allTimeAC.reset();
        }

        for (final AverageCalculator sessionAC : mSessionACs.values()) {
            sessionAC.reset();
        }

        mAllTimeTimeFreqs.clear();
        mSessionTimeFreqs.clear();
    }

    /**
     * Indicates if all of the solve time averages required are across the current session only. If
     * only times for the current session are required, a more efficient approach may be taken to
     * load the saved solve times.
     *
     * @return
     *     {@code true} if all required averages apply only to solve times for the current session;
     *     or {@code false} if the averages include at least one average for solve times across all
     *     past and current sessions. If no averages for either set of solve times are required,
     *     the result will be {@code true}.
     */
    public boolean isForCurrentSessionOnly() {
        return mOneAllTimeAC == null;
    }

    /**
     * Gets an array of all distinct values of "N" for which calculators for the "average-of-N"
     * have been added to these statistics. There is no distinction between current-session-only
     * and all-time average calculators; the distinct set of values of "N" across both sets of
     * calculators is returned.
     *
     * @return
     *     The distinction values of "N" for all "average-of-N" calculators in these statistics.
     */
    public int[] getNsOfAverages() {
        // NOTE: This is intended only to support the needs of "ChartStatistics", which assumes
        // that the union of all average calculators (created by appropriate factory methods in
        // this "Statistics" class) are exclusively for the current session or exclusively for all
        // sessions, not a mixture of both. "ChartStatistics" just needs to get all values of "N"
        // for which averages are required, so that it can create corresponding objects to store
        // the chart data. This ensures that it is not coupled to the details of the factory
        // methods in this class. An efficient implementation is not of much concern here.
        final Set<Integer> distinctNs = new TreeSet<>();

        distinctNs.addAll(mAllTimeACs.keySet());
        distinctNs.addAll(mSessionACs.keySet());

        final int[] ns = new int[distinctNs.size()];
        int i = 0;

        for (final Integer n : distinctNs) {
            ns[i++] = n;
        }

        return ns;
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
     *     {@code true} if the solve was added during the current session; or {@code false} if
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

        // Updated the time frequencies.
        final long timeForFreq = time == DNF ? DNF : (time - time % 1_000);
        Integer oldFreq;

        oldFreq = mAllTimeTimeFreqs.get(timeForFreq);
        mAllTimeTimeFreqs.put(timeForFreq, oldFreq == null ? 1 : oldFreq + 1);

        if (isForCurrentSession) {
            oldFreq = mSessionTimeFreqs.get(timeForFreq);
            mSessionTimeFreqs.put(timeForFreq, oldFreq == null ? 1 : oldFreq + 1);
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
     * for the current session. To get the number of non-DNF solves, subtract the result of
     * {@link #getSessionNumDNFSolves()}.
     *
     * @return The number of solve times that were added for the current session.
     */
    public int getSessionNumSolves() {
        return mOneSessionAC != null ? mOneSessionAC.getNumSolves() : 0;
    }

    /**
     * Gets the total number of DNF solves that were added to these statistics for the current
     * session.
     *
     * @return The number of DNF solves that were added for the current session.
     */
    public int getSessionNumDNFSolves() {
        return mOneSessionAC != null ? mOneSessionAC.getNumDNFSolves() : 0;
    }

    /**
     * Gets the simple arithmetic mean time of all non-DNF solves that were added to these
     * statistics for the current session. The returned millisecond value is truncated to a whole
     * milliseconds value, not rounded.
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
     * Gets the total time (sum of all times) of all non-DNF solves that were added to these
     * statistics for the current session.
     *
     * @return
     *     The total time of all non-DNF solves that were added for the current session. The result
     *     will be {@link AverageCalculator#UNKNOWN} if no times have been added, or if all added
     *     times were DNFs.
     */
    public long getSessionTotalTime() {
        return mOneSessionAC != null? mOneSessionAC.getTotalTime() : UNKNOWN;
    }

    /**
     * Gets the solve time frequencies for the current sessions. The times are truncated to whole
     * seconds, but still expressed as milliseconds. The keys are the times (and
     * {@link AverageCalculator#DNF} can be a key), and the values are the number of solves times
     * that fell into the one-second interval for that key. For example, if the key is "4", the
     * value is the number of solve times of "four-point-something seconds".
     *
     * @return
     *     The solve time frequencies. The iteration order of the map begins with any DNF solves
     *     and then continues in increasing order of time value. This may be modified freely.
     */
    public Map<Long, Integer> getSessionTimeFrequencies() {
        return new TreeMap<>(mSessionTimeFreqs);
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
     * for all past and current sessions. To get the number of non-DNF solves, subtract the result
     * of {@link #getAllTimeNumDNFSolves()}.
     *
     * @return The number of solve times that were added for all past and current sessions.
     */
    public int getAllTimeNumSolves() {
        return mOneAllTimeAC != null ? mOneAllTimeAC.getNumSolves() : 0;
    }

    /**
     * Gets the total number of DNF solves that were added to these statistics for all past and
     * current sessions.
     *
     * @return The number of DNF solves that were added for all past and current sessions.
     */
    public int getAllTimeNumDNFSolves() {
        return mOneAllTimeAC != null ? mOneAllTimeAC.getNumDNFSolves() : 0;
    }

    /**
     * Gets the simple arithmetic mean time of all non-DNF solves that were added to these
     * statistics for all past and current sessions. The returned millisecond value is truncated
     * to a whole milliseconds value, not rounded.
     *
     * @return
     *     The mean time of all non-DNF solves that were added for all past and current sessions.
     *     The result will be {@link AverageCalculator#UNKNOWN} if no times have been added, or if
     *     all added times were DNFs.
     */
    public long getAllTimeMeanTime() {
        return mOneAllTimeAC != null ? mOneAllTimeAC.getMeanTime() : UNKNOWN;
    }

    /**
     * Gets the total time (sum of all times) of all non-DNF solves that were added to these
     * statistics for all past and current sessions.
     *
     * @return
     *     The total time of all non-DNF solves that were added for all past and current sessions
     *     The result will be {@link AverageCalculator#UNKNOWN} if no times have been added, or
     *     if all added times were DNFs.
     */
    public long getAllTimeTotalTime() {
        return mOneAllTimeAC != null? mOneAllTimeAC.getTotalTime() : UNKNOWN;
    }

    /**
     * Gets the solve time frequencies for all past and current sessions. The times are truncated
     * to whole seconds, but still expressed as milliseconds. The keys are the times (and
     * {@link AverageCalculator#DNF} can be a key), and the values are the number of solves times
     * that fell into the one-second interval for that key. For example, if the key is "4", the
     * value is the number of solve times of "four-point-something seconds".
     *
     * @return
     *     The solve time frequencies. The iteration order of the map begins with any DNF solves
     *     and then continues in increasing order of time value. This may be modified freely.
     */
    public Map<Long, Integer> getAllTimeTimeFrequencies() {
        return new TreeMap<>(mAllTimeTimeFreqs);
    }
}
