package com.aricneto.twistytimer.stats;

import com.aricneto.twistytimer.utils.PuzzleUtils;

/**
 * Calculates the average time of a number of puzzle solves. Running averages are easily calculated
 * as each new solve is added. If the number of solve times is five or greater, the best and worst
 * times are discarded before returning the truncated arithmetic mean (aka "trimmed mean" or
 * "modified mean) of the remaining times. All times and averages are in milliseconds. The mean,
 * minimum (best) and maximum (worst) of all added times, and the best average from all values of
 * the running average are also made available.
 *
 * @author damo
 */
public final class AverageCalculator {
    // NOTE: This implementation is reasonably efficient, as it can calculate an average without
    // iterating over the array of recorded times. Iteration is only required when the known best
    // or worst values are ejected to make room for new times. Storing times in sorted order would
    // reduce this explicit iteration, but a second data structure would be required to record the
    // insertion order and the insertion operations would be more costly, as the sort order would
    // need to be maintained.
    //
    // Discarding only the single best and single worst values is standard for average-of-5
    // calculations, but for higher values of "n", it might be better to exclude, say, the best 10%
    // and worst 10%. However, that would make the implementation significantly more complicated if
    // arrays of best and worst values (or the limits of the ranges of each set of values and the
    // sums of those sets) were to be maintained. One approach might be to support calculation of
    // the median value instead, which would be relatively simple to do: sort the times, eliminate
    // the allowed DNFs and then find the time value in the middle position of the remaining times
    // in their sorted order (or the mean of the two middle values, if there are an even number of
    // remaining times).
    //
    // There are several alternative "streaming" algorithms that would not require the times to
    // be stored. However, all of those would introduce some amount of error due to rounding or
    // precision errors. As the largest value of "n" is likely to be 1,000, the approach used here
    // should be good enough and not use memory excessively.

    /**
     * A special time value that represents a solve that "did-not-finish" (DNF). This is also used
     * to represent the calculated value of an average where too many solves included in the
     * average were DNF solves.
     */
    // Deliberately avoiding using "PuzzleUtils.TIME_DNF" as the canonical flag value, as there is
    // no guarantee that it will remain with its current value of "-1" and this class needs to be
    // sure that the value will be negative and will not clash with "UNKNOWN".
    public static final long DNF = -665L;

    /**
     * A value that indicates that a calculated time is unknown. This is usually the case when not
     * enough times have been recorded to satisfy the required number of solves to be included in
     * the calculation of the average, or when all recorded solves are {@link #DNF}s.
     */
    public static final long UNKNOWN = -666L;

    /**
     * The minimum number of times to include in the average before a single DNF will not result
     * in disqualification.
     */
    private static final int MIN_N_TO_ALLOW_ONE_DNF = 5;

    /**
     * The number of solve times to include in the average.
     */
    private final int mN;

    /**
     * Indicates if averages should be reported as {@link #DNF}s if too many solve times are DNFs.
     * The number of DNFs that constitute "too many" varies with the value of {@link #mN}.
     */
    private final boolean mDisqualifyDNFs;

    /**
     * The array holding the most recently added solve times. A solve time can also be recorded as
     * a {@link #DNF}. This is managed as a circular queue. Once full, the oldest added time is
     * overwritten when the next new time is added.
     */
    private final long[] mTimes;

    /**
     * The index in {@link #mTimes} at which to add the next time. If this is equal to the length
     * of the array, it will be wrapped back to zero.
     */
    private int mNext;

    /**
     * The total number of solve times that have been added to the array. This may exceed
     * {@link #mN}, but no more than that number of solve times will be stored at any one time.
     */
    private int mNumSolves;

    /**
     * The number of DNF results currently recorded in {@code #mTimes}.
     */
    private int mNumCurrentDNFs;

    /**
     * The number of DNF results ever recorded in {@code #mTimes}.
     */
    private int mNumAllTimeDNFs;

    /**
     * The sum of all non-DNF results currently recorded in {@code #mTimes}. The number of such
     * results is given by {@code Math.min(mN, mNumSolves) - mNumCurrentDNFs}. A value of
     * {@link #UNKNOWN} indicates that there are no non-DNF results recorded.
     */
    private long mCurrentSum = UNKNOWN;

    /**
     * The sum of all non-DNF results ever recorded in {@code #mTimes}. The number of such results
     * is given by {@code mNumSolves - mNumAllTimeDNFs}. A value of {@link #UNKNOWN} indicates
     * that there are no non-DNF results recorded.
     */
    private long mAllTimeSum = UNKNOWN;

    /**
     * The best time currently recorded in {@code #mTimes}. A value of {@link #UNKNOWN} indicates
     * that there is no non-DNF result recorded.
     */
    private long mCurrentBestTime = UNKNOWN;

    /**
     * The worst time (not a DNF) currently recorded in {@code #mTimes}. If any DNF is present, a
     * DNF will be taken instead as the worst time, if the calculation needs to exclude one. A
     * value of {@link #UNKNOWN} indicates that there is no non-DNF result recorded.
     */
    private long mCurrentWorstTime = UNKNOWN;

    /**
     * The current average value calculated from all times stored in {@link #mTimes}. A value of
     * {@link #UNKNOWN} indicates insufficient results have been added to calculate the required
     * average, or that the calculation has not been performed. A value of {@link #DNF} indicates
     * that too many DNF results are present and the average is disqualified.
     */
    private long mCurrentAverage = UNKNOWN;

    /**
     * The best time ever added to this calculator. This time may not currently be recorded in
     * {@code #mTimes}, as it may have been overwritten. A value of {@link #UNKNOWN} indicates that
     * there is no non-DNF result recorded.
     */
    private long mAllTimeBestTime = UNKNOWN;

    /**
     * The worst time (not a DNF) ever added to this calculator. This time may not currently be
     * recorded in {@code #mTimes}, as it may have been overwritten. A value of {@link #UNKNOWN}
     * indicates that there is no non-DNF result recorded.
     */
    private long mAllTimeWorstTime = UNKNOWN;

    /**
     * The best average value calculated from all times added to date. A value of {@link #UNKNOWN}
     * indicates that insufficient results have been added to calculate the required average. A
     * value of {@link #DNF} indicates that averages could be calculated, but that every average
     * was disqualified as a DNF average.
     */
    private long mAllTimeBestAverage = UNKNOWN;

    /**
     * Creates a new calculator for the "average of <i>n</i>" solve times.
     *
     * @param n
     *     The number of solve times that will be averaged (e.g., 3, 5, 12, ...). Must be greater
     *     than zero.
     * @param disqualifyDNFs
     *     {@code true} if an average should be disqualified if too many {@link #DNF}s are present,
     *     or {@code false} if DNFs should be ignored. See {@link #getCurrentAverage()} for more
     *     details on the calculation.
     *
     * @throws IllegalArgumentException
     *     If {@code n} is not greater than zero.
     */
    public AverageCalculator(int n, boolean disqualifyDNFs) {
        if (n <= 0) {
            throw new IllegalArgumentException("Number of solves must be > 0: " + n);
        }

        mN = n;
        mTimes = new long[n];
        mDisqualifyDNFs = disqualifyDNFs;
    }

    /**
     * Gets the number of solve times that are included in the average. This is inclusive of any
     * times, such as the best and worst times, that are excluded when calculating the truncated
     * mean. For example, for an "average of 12", the best time and the worst time are trimmed
     * before getting the truncated arithmetic mean of the remaining 10 times, but this method
     * returns 12. This may be greater than the number of times added so far and given by
     * {@link #getNumSolves()}.
     *
     * @return The number of times that must be considered in the calculation of the average.
     */
    public int getN() {
        return mN;
    }

    /**
     * Adds a solve time to be included in the calculation of the average. Solve times should be
     * added in chronological order (i.e., by solve time-stamp, not solve time).
     *
     * @param time
     *     The solve time in milliseconds. The time must be greater than zero. Use {@link #DNF} to
     *     represent a DNF solve.
     *
     * @throws IllegalArgumentException
     *     If the added time is not greater than zero and is not {@code DNF}.
     */
    public void addTime(long time) throws IllegalArgumentException {
        if (time <= 0L && time != DNF) {
            throw new IllegalArgumentException("Time must be > 0 or be 'DNF': " + time);
        }

        mNumSolves++;

        final long ejectedTime;

        // If the array is full, "mNext" points to the oldest result that needs to be ejected first.
        // If the array is not full, then "mNext" points to an empty entry, so no special handling
        // is needed.
        if (mNumSolves >= mN) {
            if (mNext == mN) {
                // Need to wrap around to the start (index zero).
                mNext = 0;
            }
            ejectedTime = mTimes[mNext]; // May be DNF.
        } else {
            // "mNext" must be less than "mN" if "mNumSolves" is less than "mN".
            ejectedTime = UNKNOWN; // Nothing ejected.
        }

        mTimes[mNext] = time;
        mNext++;

        // Order is important here, as these methods change fields and some methods depend on the
        // fields being updated by other methods before they are called. All depend on the new
        // time being stored already (see above) and any ejected time being known (also above).
        updateDNFCounts(time, ejectedTime);
        updateCurrentBestAndWorstTimes(time, ejectedTime);
        updateSums(time, ejectedTime);
        updateCurrentAverage();

        updateAllTimeBestAndWorstTimes();
        updateAllTimeBestAverage();
    }

    /**
     * Adds solve times to be included in the calculation of the average. Solve times should be
     * added in chronological order (i.e., by solve time-stamp, not solve time). This method can be
     * called repeatedly to add any number of solve times over any number of calls.
     *
     * @param times
     *     Zero or more solve times in milliseconds. Times must be greater than zero. Use
     *     {@link #DNF} to represent each DNF. If this is {@code null} or empty, it will be ignored
     *     and this method will have no effect.
     *
     * @throws IllegalArgumentException
     *     If any added time is not greater than zero and is not {@code DNF}.
     */
    public void addTimes(long... times) throws IllegalArgumentException {
        // The variable arguments list makes it easier to write compact test cases; it does not
        // really make life any easier when adding times via a database cursor. In non-test
        // contexts, it will be more efficient to call "addTime", as each call will not need to
        // create a "long[]" object.
        if (times != null) {
            for (final long time : times) {
                addTime(time); // May throw IAE.
            }
        }
    }

    /**
     * Updates the current and all-time counts of DNF solves.
     *
     * @param addedTime
     *     The newly added time. May be {@link #DNF}.
     * @param ejectedTime
     *     An old time that was ejected to make room for the newly added time. May be {@code DNF}.
     *     Use {@link #UNKNOWN} if no old time was ejected.
     */
    private void updateDNFCounts(long addedTime, long ejectedTime) {
        if (addedTime == DNF) {
            mNumCurrentDNFs++;
            mNumAllTimeDNFs++;
        }

        if (ejectedTime == DNF) {
            mNumCurrentDNFs--;
        }
    }

    /**
     * Updates the current best and worst times after a new time is added. The count of DNFs must
     * be updated by {@link #updateDNFCounts(long, long)} before calling this method.
     *
     * @param addedTime
     *     The newly added time. May be {@link #DNF}. Must already be stored.
     * @param ejectedTime
     *     An old time that was ejected to make room for the newly added time. May be {@code DNF}.
     *     Use {@link #UNKNOWN} if no old time was ejected.
     */
    private void updateCurrentBestAndWorstTimes(long addedTime, long ejectedTime) {
        // The logic here will set one or both of "mCurrentBestTime" and "mCurrentWorstTime" to
        // "UNKNOWN" if knowledge of the best or worst time has been lost. If either value becomes
        // "UNKNOWN", a new iteration over "mTimes" will recalculate both values.

        if (addedTime == DNF) {
            // Newly added time does not change the current best or worst time, but has either of
            // the best or worst times (not a DNF) just been ejected and is recalculation required?
            if (ejectedTime == mCurrentBestTime || ejectedTime == mCurrentWorstTime) {
                // It does not matter which has been ejected, just recalculate both.
                mCurrentBestTime = UNKNOWN;
                mCurrentWorstTime = UNKNOWN;
            }
        } else {
            // Newly added time is not a DNF and may be the new best or worst time (or both).
            // However, if it is not the new (or equal) best or worst time, then check if we are
            // ejecting the old best or worst time. If either is ejected, there may be another best
            // or worst time in "mTimes" (with respect to "addedTime") and it must be found. There
            // if no need to check if "ejectedTime" is DNF or UNKNOWN.
            if (mCurrentBestTime == UNKNOWN || addedTime <= mCurrentBestTime) {
                mCurrentBestTime = addedTime;
            } else if (ejectedTime == mCurrentBestTime) {
                mCurrentBestTime = UNKNOWN;
            }

            if (mCurrentWorstTime == UNKNOWN || addedTime >= mCurrentWorstTime) {
                mCurrentWorstTime = addedTime;
            } else if (ejectedTime == mCurrentWorstTime) {
                mCurrentWorstTime = UNKNOWN;
            }
        }

        // Recalculate the best and worst times. We can skip this if every stored time is a DNF.
        // In that case, both "mCurrentBestTime" and "mCurrentWorstTime" will remain UNKNOWN.
        final int numCurrentSolves = Math.min(mNumSolves, mN);

        if (mNumCurrentDNFs != numCurrentSolves
                && (mCurrentBestTime == UNKNOWN || mCurrentWorstTime == UNKNOWN)) {
            // At least one stored time is not a DNF and is > 0, so reset the fields and rescan.
            mCurrentBestTime = Long.MAX_VALUE;
            mCurrentWorstTime = 0L;

            // There is no need to follow the chronological insertion order here. The array may
            // not be full yet.
            for (int i = 0; i < numCurrentSolves; i++) {
                final long time = mTimes[i];

                if (time != DNF) {
                    mCurrentBestTime = Math.min(mCurrentBestTime, time);
                    mCurrentWorstTime = Math.max(mCurrentWorstTime, time);
                }
            }
        }
    }

    /**
     * Updates the sum of all times currently stored and the sum of all times ever added. Any
     * {@link #DNF} results are ignored. If all recorded times have been DNFs, the sums will set
     * to {@link #UNKNOWN}.
     *
     * @param addedTime
     *     The newly added time. May be {@code DNF}. Must already be stored.
     * @param ejectedTime
     *     An old time that was ejected to make room for the newly added time. May be {@code DNF}.
     *     Use {@code UNKNOWN} if no old time was ejected.
     */
    private void updateSums(long addedTime, long ejectedTime) {
        if (addedTime != DNF) {
            mCurrentSum = addedTime + (mCurrentSum == UNKNOWN ? 0L : mCurrentSum);
            mAllTimeSum = addedTime + (mAllTimeSum == UNKNOWN ? 0L : mAllTimeSum);
        }
        if (ejectedTime != DNF && ejectedTime != UNKNOWN) {
            mCurrentSum -= ejectedTime;
        }

        // Returned from a state with at least one non-DNF time to a state where all times are DNFs.
        // Flag the new state properly. ("mAllTimeSum" cannot return to zero.)
        if (mCurrentSum == 0L) {
            mCurrentSum = UNKNOWN;
        }
    }

    /**
     * Updates the average value of the most recently added times. See {@link #getCurrentAverage()}
     * for details. The sum, best and worst values and other fields must be updated by first calling
     * {@link #updateSums(long, long)} and {@link #updateCurrentBestAndWorstTimes(long, long)} and
     * their dependent methods before calling this method.
     */
    private void updateCurrentAverage() {
        if (mNumSolves < mN) {
            // Not enough times added to calculate the average.
            mCurrentAverage = UNKNOWN;
        } else if (mNumCurrentDNFs == mN) {
            // Enough times have been added, but all of the currently stored ones are DNFs.
            mCurrentAverage = DNF;
        } else if (!mDisqualifyDNFs && mNumCurrentDNFs == mN - 1) {
            // More than one DNF is not an automatic disqualification, but there is only one
            // non-DNF time present. Just use that time as the average.
            mCurrentAverage = mCurrentBestTime;
        } else if (mN >= MIN_N_TO_ALLOW_ONE_DNF) {
            if (mDisqualifyDNFs && mNumCurrentDNFs > 1) {
                // Disqualify the average: there is more than one DNF and only one DNF is allowed.
                mCurrentAverage = DNF;
            } else {
                // There is no more than one DNF, or there is more than one DNF, but that will not
                // cause automatic disqualification. There are at least two non-DNF times present.
                // Calculate a truncated arithmetic mean. "mCurrentSum" is the sum of all non-DNF
                // times. Discard the best and worst time, using one DNF as the worst time if any
                // DNFs are present, so at least one non-DNF time will remain after discarding the
                // outliers. Discard all other DNFs, if any. One DNF may already have been
                // discarded as the worst time; do not discard it twice.
                mCurrentAverage
                        = (mCurrentSum - mCurrentBestTime
                               - (mNumCurrentDNFs == 0 ? mCurrentWorstTime : 0))
                          / (mN - 2 - (mNumCurrentDNFs > 1 ? mNumCurrentDNFs - 1 : 0));
            }
        } else { // mN < MIN_N_TO_ALLOW_ONE_DNF
            // NOTE: "mN" could be as low as 1, but will not be zero (see the constructor).
            if (mDisqualifyDNFs && mNumCurrentDNFs > 0) {
                // Disqualify the average as *no* DNF (not even one) is allowed for small "n".
                mCurrentAverage = DNF;
            } else {
                // There is no DNF, or there are DNFs, but that will not cause automatic
                // disqualification. There is at least one non-DNF time present. Calculate the
                // (not truncated) arithmetic mean.
                mCurrentAverage = mCurrentSum / (mN - mNumCurrentDNFs);
            }
        }
    }

    /**
     * Updates the all-time best and worst times after a new time is added. The current best and
     * worst times must be updated by {@link #updateCurrentBestAndWorstTimes(long, long)} before
     * calling this method.
     */
    private void updateAllTimeBestAndWorstTimes() {
        if (mAllTimeBestTime == UNKNOWN) {
            mAllTimeBestTime = mCurrentBestTime; // May still be UNKNOWN.
        } else if (mCurrentBestTime != UNKNOWN) {
            // "mCurrentBestTime" is never set to "DNF".
            mAllTimeBestTime = Math.min(mAllTimeBestTime, mCurrentBestTime);
        }

        if (mAllTimeWorstTime == UNKNOWN) {
            mAllTimeWorstTime = mCurrentWorstTime; // May still be UNKNOWN.
        } else if (mCurrentWorstTime != UNKNOWN) {
            mAllTimeWorstTime = Math.max(mAllTimeWorstTime, mCurrentWorstTime);
        }
    }

    /**
     * Updates the all-time best average after a new time is added. The current average must be
     * updated by {@link #updateCurrentAverage()} before calling this method.
     */
    private void updateAllTimeBestAverage() {
        if (mAllTimeBestAverage == UNKNOWN || mAllTimeBestAverage == DNF) {
            // "mCurrentAverage" may still be UNKNOWN or DNF, but cannot change back to UNKNOWN once
            // set to a different value, as UNKNOWN is cleared once "mN" solves have been added.
            // Therefore, we never set "mAllTimeBestAverage" to a value worse than it already has.
            mAllTimeBestAverage = mCurrentAverage;
        } else if (mCurrentAverage != DNF) {
            mAllTimeBestAverage = Math.min(mAllTimeBestAverage, mCurrentAverage);
        }
    }

    /**
     * <p>
     * Gets the current value of the average. This is calculated from the most recently added
     * times. The number of times considered is given by {@link #getN()} ("n").
     * </p>
     * <p>
     * Where the value of "n" is less than 5, the average is the arithmetic mean of the currently
     * stored values, not a truncated mean. If any currently recorded solve is a {@link #DNF},
     * the average is disqualified as a DNF unless configured so that DNFs do not automatically
     * disqualify averages (by passing {@code false} as the value of the {@code disqualifyDNFs}
     * parameter to {@link #AverageCalculator(int, boolean)}). If DNFs are allowed, then the
     * average is the average time of all non-DNF solves, but will still be a DNF average if all
     * solves are DNFs.
     * </p>
     * <p>
     * Where the value of "n" is 5 or greater, the average is the truncated arithmetic mean of the
     * currently stored values. The single best and single worst solve times are discarded and the
     * average is calculated from the remaining times. If one DNF is present, it is taken as the
     * worst solve time and discarded. If more than one DNF is present, the average is disqualified
     * as a DNF unless configured so that DNFs do not automatically disqualify averages. If more
     * DNFs are allowed, one will be taken as the worst solve time and the other DNFs will be
     * ignored. If only a single non-DNF time remains, it will not be discarded as the best time
     * and will be returned as the average time.
     * </p>
     *
     * @return
     *     The current (truncated( arithmetic mean of the most recently added values. If fewer
     *     times have been added that the number required, the result will be {@link #UNKNOWN).
     *     If too many DNF solves are included in the recently-added times, the result is
     *     {@code DNF}. The returned integer value of the average is truncated (rounded down).
     */
    public long getCurrentAverage() {
        return mCurrentAverage;
    }

    /**
     * Gets the best value of the average. This is calculated across all added times. The number of
     * times considered is given by {@link #getN()}. The average for each consecutive sequence of
     * that number of times (including DNFs) is calculated as each new time is added and the best
     * average of all of those sequences is returned. See {@link #getCurrentAverage()} for more
     * details.
     *
     * @return
     *     The best truncated arithmetic mean across all added values. If fewer times have been
     *     added that the number required, the result will be {@link #UNKNOWN). If too many DNF
     *     solves are included in <i>all</i> sequences of times, the result is {@link #DNF}.
     */
    public long getBestAverage() {
        return mAllTimeBestAverage;
    }

    /**
     * Gets the best time of all those added to this calculator.
     *
     * @return
     *     The best time ever added to this calculator. The result will be {@link #UNKNOWN) if no
     *     times have been added, or if all added times were {@link #DNF}s.
     */
    public long getBestTime() {
        return mAllTimeBestTime;
    }

    /**
     * Gets the worst time (not a DNF) of all those added to this calculator.
     *
     * @return
     *     The worst time ever added to this calculator. The result will be {@link #UNKNOWN) if no
     *     times have been added, or if all added times were {@link #DNF}s.
     */
    public long getWorstTime() {
        return mAllTimeWorstTime;
    }

    /**
     * Gets the total number of solve times (including DNFs) that were added to this calculator.
     * This may be greater than the number (given by {@link #getN}) that are included in the
     * calculation of the average. Subtract the value from {@link #getNumDNFSolves()} to get the
     * total number of non-DNF solves.
     *
     * @return The number of solve times that were added to this calculator.
     */
    public int getNumSolves() {
        return mNumSolves;
    }

    /**
     * Gets the total number of DNF solves that were added to this calculator.
     *
     * @return The number of DNF solves that were added to this calculator.
     */
    public int getNumDNFSolves() {
        return mNumAllTimeDNFs;
    }

    /**
     * Gets the total time of all non-DNF solves that were added to this calculator.
     *
     * @return
     *     The total time of all non-DNF solves that were added to this calculator. The result
     *     will be {@link #UNKNOWN) if no times have been added, or if all added times were
     *     {@link #DNF}s.
     */
    public long getTotalTime() {
        return mAllTimeSum;
    }

    /**
     * Gets the simple arithmetic mean time of all non-DNF solves that were added to this
     * calculator. The returned millisecond value is truncated to a whole milliseconds value, not
     * rounded.
     *
     * @return
     *     The mean time of all non-DNF solves that were added to this calculator. The result
     *     will be {@link #UNKNOWN) if no times have been added, or if all added times were
     *     {@link #DNF}s.
     */
    public long getMeanTime() {
        return mAllTimeSum != UNKNOWN ? mAllTimeSum / (mNumSolves - mNumAllTimeDNFs) : UNKNOWN;
    }

    /**
     * Translates a time value that may be {@link #UNKNOWN} or {@link #DNF} into a time value
     * that is compatible with methods such as the {@code PuzzleUtils.convertTimeToString*}
     * methods.
     *
     * @param time The time value to be translated.
     *
     * @return
     *     The translated time value; {@code UNKNOWN} is translated to zero and {@code DNF} is
     *     translated to {@link PuzzleUtils#TIME_DNF}.
     */
    public static long tr(long time) {
        if (time == UNKNOWN) {
            return 0L;
        }
        if (time == DNF) {
            return PuzzleUtils.TIME_DNF;
        }
        return time;
    }
}
