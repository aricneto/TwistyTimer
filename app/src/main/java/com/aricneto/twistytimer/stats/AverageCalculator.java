package com.aricneto.twistytimer.stats;

import android.util.Log;

import com.aricneto.twistytimer.items.AverageComponent;
import com.aricneto.twistytimer.utils.PuzzleUtils;
import com.aricneto.twistytimer.utils.StatUtils;
import com.google.common.collect.BoundType;
import com.google.common.collect.TreeMultiset;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
    // sure that the value will be negative and will not clash with "UNKNOWN". If changing these,
    // use values that can also be represented as in "int".
    public static final long DNF = Long.MAX_VALUE;

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
     * An array holding a list of times, sorted lowest to highest
     */
    private long[] mSortedTimes;

    // TODO: comment these variables
    public AverageComponent mUpperTrim;
    public AverageComponent mLowerTrim;
    public AverageComponent mMiddleTrim;
    private int mLowerTrimBound;
    private int mUpperTrimBound;
    private int mTrimSize;

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
     * The Welford algorithm for variance is one of the most well-known and used online methods of
     * calculating the variance of a given sample data. The algorithm itself has several
     * variables, which are listed here. I won't pretend to fully understand it myself, but there
     * are a lot of references online for it.
     */
    private double mMean = 0;
    private double mVarianceDelta = 0;
    private double mVarianceDelta2 = 0;
    private double mVarianceM2 = 0;

    /**
     * The current variance of all solves ever recorded. A value of {@link #UNKNOWN} indicates
     * the sample size is not enough for it to be calculated yet.
     */
    private long mVariance;

    /**
     * The sum of all non-DNF results currently recorded in {@code #mTimes}. The number of such
     * results is given by {@code Math.min(mN, mNumSolves) - mNumCurrentDNFs}. A value of
     * {@link #UNKNOWN} indicates that there are no non-DNF results recorded.
     */
    private long mCurrentSum;

    /**
     * The sum of all non-DNF results ever recorded in {@code #mTimes}. The number of such results
     * is given by {@code mNumSolves - mNumAllTimeDNFs}. A value of {@link #UNKNOWN} indicates
     * that there are no non-DNF results recorded.
     */
    private long mAllTimeSum;

    /**
     * The best time currently recorded in {@code #mTimes}. A value of {@link #UNKNOWN} indicates
     * that there is no non-DNF result recorded.
     */
    private long mCurrentBestTime;

    /**
     * The worst time (not a DNF) currently recorded in {@code #mTimes}. If any DNF is present, a
     * DNF will be taken instead as the worst time, if the calculation needs to exclude one. A
     * value of {@link #UNKNOWN} indicates that there is no non-DNF result recorded.
     */
    private long mCurrentWorstTime;

    /**
     * The current average value calculated from all times stored in {@link #mTimes}. A value of
     * {@link #UNKNOWN} indicates insufficient results have been added to calculate the required
     * average, or that the calculation has not been performed. A value of {@link #DNF} indicates
     * that too many DNF results are present and the average is disqualified.
     */
    private long mCurrentAverage;

    /**
     * The best time ever added to this calculator. This time may not currently be recorded in
     * {@code #mTimes}, as it may have been overwritten. A value of {@link #UNKNOWN} indicates that
     * there is no non-DNF result recorded.
     */
    private long mAllTimeBestTime;

    /**
     * The worst time (not a DNF) ever added to this calculator. This time may not currently be
     * recorded in {@code #mTimes}, as it may have been overwritten. A value of {@link #UNKNOWN}
     * indicates that there is no non-DNF result recorded.
     */
    private long mAllTimeWorstTime;

    /**
     * The best average value calculated from all times added to date. A value of {@link #UNKNOWN}
     * indicates that insufficient results have been added to calculate the required average. A
     * value of {@link #DNF} indicates that averages could be calculated, but that every average
     * was disqualified as a DNF average.
     */
    private long mAllTimeBestAverage;

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
        mSortedTimes = new long[n];
        mDisqualifyDNFs = disqualifyDNFs;

        mTrimSize = 1;
        mLowerTrimBound = mTrimSize;
        mUpperTrimBound = mN - mTrimSize;

        mUpperTrim = new AverageComponent();
        mMiddleTrim = new AverageComponent();
        mLowerTrim = new AverageComponent();

        // As "reset()" needs to be supported to ensure a sane state can be guaranteed before
        // populating statistics from the database, it makes sense to use it to initialise the
        // fields in one place.
        reset();
    }

    /**
     * Resets all statistics and averages that have been collected previously.
     */
    public void reset() {
        Arrays.fill(mTimes, 0L);
        Arrays.fill(mSortedTimes, 0L);
        mNext = 0;
        mNumSolves = 0;
        mNumCurrentDNFs = 0;
        mNumAllTimeDNFs = 0;

        // Variance variables
        mMean = 0;
        mVarianceDelta = 0;
        mVarianceDelta2 = 0;
        mVarianceM2 = 0;

        // FIXME: also reset best, worst and tree
        mMiddleTrim.sum = UNKNOWN;
        mLowerTrim.sum = UNKNOWN;
        mUpperTrim.sum = UNKNOWN;

        mCurrentSum = UNKNOWN;
        mAllTimeSum = UNKNOWN;
        mCurrentBestTime = UNKNOWN;
        mCurrentWorstTime = UNKNOWN;
        mCurrentAverage = UNKNOWN;
        mAllTimeBestTime = UNKNOWN;
        mAllTimeWorstTime = UNKNOWN;
        mAllTimeBestAverage = UNKNOWN;
        mVariance = UNKNOWN;
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
            // FIXME: throwing an IllegalArgumentException here is too harsh for the user. If the app
            // incorrectly imports an illegal solve, the app will keep crashing and the only way for
            // the user to fix this is to clear the app data. I'm commenting this off for the time
            // being until the import algorithm gets sorted out.
            // TODO: Should the app automatically remove illegal solves?

            Log.e("AverageCalculator", "Time must be > 0 or be 'DNF': " + time);

            // throw new IllegalArgumentException("Time must be > 0 or be 'DNF': " + time);
        } else {

            mNumSolves++;

            final long ejectedTime;

            // If the array has just been filled, store a sorted version of it
            // If the array is full, "mNext" points to the oldest result that needs to be ejected first.
            //      We also need to remove the oldest solve from the sorted array and insert the new solve
            // If the array is not full, then "mNext" points to an empty entry, so no special handling
            // is needed.
            if (mNumSolves >= mN) {
                if (mNext == mN) {
                    // Need to wrap around to the start (index zero).
                    mNext = 0;
                }
                ejectedTime = mTimes[mNext]; // May be DNF.

                // Create the sorted list as soon as numSolves reaches N
                // We only need to sort it once. Subsequent added solves will use
                // binary search to insert the new solves in the correct (sorted) position
                if (mNumSolves == mN) {
                    mTimes[mNext] = time;
                    mSortedTimes = mTimes.clone();
                    Arrays.sort(mSortedTimes);
                }

            } else {
                // "mNext" must be less than "mN" if "mNumSolves" is less than "mN".
                ejectedTime = UNKNOWN; // Nothing ejected.
            }

            mTimes[mNext] = time;
            mNext++;

            //Log.d("AverageCalculator", "N: " + mN + " | Set: " + mSortedTimes);

            // Order is important here, as these methods change fields and some methods depend on the
            // fields being updated by other methods before they are called. All depend on the new
            // time being stored already (see above) and any ejected time being known (also above).
            updateDNFCounts(time, ejectedTime);
            updateCurrentBestAndWorstTimes(time, ejectedTime);
            updateSums(time, ejectedTime);
            updateCurrentTrims(time, ejectedTime);
            updateVariance(time);
            updateCurrentAverage();

            updateAllTimeBestAndWorstTimes();
            updateAllTimeBestAverage();
        }
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

    // FIXME: account for DNFs (DNF = MAXVALUE)
    private void updateCurrentTrims(long addedTime, long ejectedTime) {
        if (mNumSolves >= mN) {
            // As soon as mNumSolves reaches N, we should begin counting up the sum of all
            // stored solves.
            // First, we loop through the entire trim bound array. For subsequent times, we only
            // have to remove the oldest time, and recalculate the sum.
            if (mNumSolves == mN) {
                for (int i = 0; i < mLowerTrimBound; i++) {
                    mLowerTrim.add(mSortedTimes[i]);
                    //Log.d("AverageCalculator", "Lower trim | added: " + mSortedTimes[i] + " | sum: " + mLowerTrim.sum);
                }
                for (int i = mUpperTrimBound; i < mN; i++) {
                    mUpperTrim.add(mSortedTimes[i]);
                    //Log.d("AverageCalculator", "Upper trim | added: " + mSortedTimes[i] + " | sum: " + mUpperTrim.sum);
                }
            } else {
                /**
                 * The number of solves {@link mNumSolves} is bigger than {@link mN}.
                 * Now, find where the ejected time lies (lower, middle or upper trim) and remove
                 * it from the sum and the list
                  */
                int ejectedTimeIndex = Arrays.binarySearch(mSortedTimes, ejectedTime);
                mSortedTimes = ArrayUtils.remove(mSortedTimes, ejectedTimeIndex);
                int addedTimeIndex = Arrays.binarySearch(mSortedTimes, addedTime);
                if (addedTimeIndex < 0) {
                    /**
                     * An equal time was not found in the array.
                     * BinarySearch returns (-(insertionPoint) - 1) in this case
                     * (read the {@link Arrays} doc on binarySearch for more )
                     */
                    addedTimeIndex = Math.abs(addedTimeIndex + 1);
                }
                 mSortedTimes = ArrayUtils.add(mSortedTimes, addedTimeIndex, addedTime);

                /**
                 * Update trims based on location of ejected time
                 */
                if (ejectedTimeIndex < mLowerTrimBound) {
                    // Time was ejected from lower trim
                    mLowerTrim.sub(ejectedTime);

                    if (addedTimeIndex >= mLowerTrimBound) {
                        // The new time was added outside the lower trim, so the time that will
                        // take the ejected time's place will be the last one on the lower trim
                        mLowerTrim.add(mSortedTimes[mLowerTrimBound - 1]);

                        // If the time was added in the upper trim, update that trim too
                        if (addedTimeIndex >= mUpperTrimBound) {
                            // Remove the time that was pushed away
                            mUpperTrim.sub(mSortedTimes[mUpperTrimBound - 1]);
                            // Add the new time
                            mUpperTrim.add(addedTime);
                        }
                    } else {
                        // The new time was added inside the lower trim. Just sum the new time
                        mLowerTrim.add(addedTime);
                    }
                } else if (ejectedTimeIndex >= mUpperTrimBound) {
                    // Time was ejected from upper trim
                    mUpperTrim.sub(ejectedTime);

                    if (addedTimeIndex < mUpperTrimBound) {
                        // The new time was added outside the upper trim, so the time that will
                        // take the ejected time's place will be the first one on the upper trim
                        mUpperTrim.add(mSortedTimes[mUpperTrimBound]);

                        // If the time was added in the lower trim, update that trim too
                        if (addedTimeIndex < mLowerTrimBound) {
                            // Remove the time that was pushed away
                            mLowerTrim.sub(mSortedTimes[mLowerTrimBound]);
                            // Add the new time
                            mLowerTrim.add(addedTime);
                        }
                    } else {
                        // The new time was added inside the upper trim. Just sum the new time
                        mUpperTrim.add(addedTime);
                    }
                } else {
                    // If neither of those are true, the ejected and added time both belonged
                    // to the middle trim, so no other action is necessary
                    if (addedTimeIndex < mLowerTrimBound) {
                        // Time was added inside lower trim.
                        // Push the last time on the trim out, and add the new one in.
                        mLowerTrim.sub(mSortedTimes[mLowerTrimBound - 1]);
                        mLowerTrim.add(addedTime);
                    } else if (addedTimeIndex >= mUpperTrimBound) {
                        // Time was added inside upper trim.
                        // Push the first time on the trim out, and add the new one in.
                        mUpperTrim.sub(mSortedTimes[mUpperTrimBound - 1]);
                        mUpperTrim.add(addedTime);
                    }
                }
            }
            mMiddleTrim.sum = mCurrentSum - (mLowerTrim.sum + mUpperTrim.sum);
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
     * The Welford algorithm for variance is one of the most well-known and used online methods of
     * calculating the variance of a given sample data. I won't pretend to fully understand it
     * myself, but there are a lot of references online for it.
     */
    private void updateVariance(long addedTime) {
        long totalValidSolves = (mNumSolves - mNumAllTimeDNFs);
        if (addedTime != DNF) {
            mVarianceDelta = ((double) addedTime) - mMean;
            mMean += mVarianceDelta / totalValidSolves;
            mVarianceDelta2 = ((double) addedTime) - mMean;
            mVarianceM2 += mVarianceDelta * mVarianceDelta2;
        }
        if (totalValidSolves > 2) {
            mVariance = (long) (mVarianceM2 / (totalValidSolves - 1));
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
                mCurrentAverage = mMiddleTrim.sum /
                                  (mN - (mTrimSize * 2) -
                                   (mNumCurrentDNFs > 1 ? mNumCurrentDNFs - 1 : 0));
                //mCurrentAverage
                //        = (mCurrentSum - mCurrentBestTime
                //               - (mNumCurrentDNFs == 0 ? mCurrentWorstTime : 0))
                //          / (mN - 2 - (mNumCurrentDNFs > 1 ? mNumCurrentDNFs - 1 : 0));
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
     *     times have been added that the number required, the result will be {@link #UNKNOWN}.
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
     *     added that the number required, the result will be {@link #UNKNOWN}. If too many DNF
     *     solves are included in <i>all</i> sequences of times, the result is {@link #DNF}.
     */
    public long getBestAverage() {
        return mAllTimeBestAverage;
    }

    /**
     * Gets the best time of all those added to this calculator.
     *
     * @return
     *     The best time ever added to this calculator. The result will be {@link #UNKNOWN} if no
     *     times have been added, or if all added times were {@link #DNF}s.
     */
    public long getBestTime() {
        return mAllTimeBestTime;
    }

    /**
     * Gets the worst time (not a DNF) of all those added to this calculator.
     *
     * @return
     *     The worst time ever added to this calculator. The result will be {@link #UNKNOWN} if no
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
     *     will be {@link #UNKNOWN} if no times have been added, or if all added times were
     *     {@link #DNF}s.
     */
    public long getTotalTime() {
        return mAllTimeSum;
    }

    /**
     * Gets the current Sample Standard Deviation of all non-DNF solves that were added to this
     * calculator
     *
     * @return
     *      The Sample Standard Deviation of all non-DNF solves that were added to this calculator
     *      Will be {@link #UNKNOWN} if no times have been added, or if all added solve times
     *      were {@link #DNF}s.
     */
    public long getStandardDeviation() { return mVariance != UNKNOWN ? (long) Math.sqrt
            (mVariance) : UNKNOWN; }

    /**
     * Gets the simple arithmetic mean time of all non-DNF solves that were added to this
     * calculator. The returned millisecond value is truncated to a whole milliseconds value, not
     * rounded.
     *
     * @return
     *     The mean time of all non-DNF solves that were added to this calculator. The result
     *     will be {@link #UNKNOWN} if no times have been added, or if all added times were
     *     {@link #DNF}s.
     */
    public long getMeanTime() {
        return (long) mMean != 0 ? (long) mMean : UNKNOWN;
    }

    /**
     * Captures the details of the average-of-N calculation including the most recently added time.
     *
     * @return The details for the average-of-N calculation.
     */
    public AverageOfN getAverageOfN() {
        return new AverageOfN(this);
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

    /**
     * A summary of the average of the mostly recently added times. All times are provided in an
     * array and the calculated average, best time and worst time are identified, if appropriate.
     */
    public static class AverageOfN {
        /**
         * The array of values that contributed to the calculation of the average-of-N. If too few
         * values have been recorded (less than "N"), the array will be {@code null}. The times
         * will be ordered with the oldest recorded time first.
         */
        private final long[] mTimes;

        /**
         * The index within {@link #mTimes} of the best time. The index will be -1 if that array is
         * {@code null}, or if the average calculation for the value of "N" does not eliminate the
         * best time, or if all of the times are DNFs, or if DNFs do not cause disqualification,
         * but there is only one non-DNF time recorded.
         */
        private final int mBestTimeIndex;

        /**
         * The index within {@link #mTimes} of the worst time. The index will be -1 if that array is
         * {@code null}, or if the average calculation for the value of "N" does not eliminate the
         * best time, or if all of the times are DNFs. The worst time may be a DNF.
         */
        private final int mWorstTimeIndex;

        /**
         * The average-of-N value calculated for the times. May be {@link #DNF} if there are too
         * many DNF solves, or {@link #UNKNOWN} if the are too few times (less than "N").
         */
        private final long mAverage;

        /**
         * Creates a new record of the most recent "average-of-N" recorded by an average calculator.
         *
         * @param ac The average calculator from which to capture the information.
         */
        private AverageOfN(AverageCalculator ac) {
            final int n = ac.getN();

            mAverage = ac.getCurrentAverage();

            if (mAverage != UNKNOWN && ac.getNumSolves() >= n) {
                mTimes = new long[n];

                // The oldest time recorded in "ac.mTimes" is not necessarily the first one, as
                // that array operates as a circular queue. "ac.mNext" marks one index past the
                // last added time. However, the array should be full, so this should also be the
                // index of the first (oldest) time. If "ac.mNext" equals "n", then we wrap around
                // to start at index zero.
                final int oldestIndex = ac.mNext == n ? 0 : ac.mNext;

                System.arraycopy(ac.mTimes, oldestIndex, mTimes, 0, n - oldestIndex);
                System.arraycopy(ac.mTimes, 0, mTimes, n - oldestIndex, oldestIndex);

                // "-1" is the convention for an unknown *index*, so "UNKNOWN" is not used.
                int bestIdx = -1;
                int worstIdx = -1;

                // IF the threshold value of "N" for the calculation of a truncated mean is not
                // reached, no best or worst times will be identified for elimination.
                //
                // IF all times are DNFs, no best or worst times will be identified for elimination.
                //
                // IF only one time is not a DNF, no best time will be identified for elimination.
                //
                // IF DNFs are present, a DNF will be identified as the worst time instead of
                // "mCurrentWorstTime", as "mCurrentWorstTime" is never set to DNF.
                if (n >= MIN_N_TO_ALLOW_ONE_DNF && n > ac.mNumCurrentDNFs) { // At least 1 non-DNF.
                    // Do not identify the only non-DNF time as the best time.
                    final long bestTime
                            = (n - ac.mNumCurrentDNFs > 1) ? ac.mCurrentBestTime : UNKNOWN;
                    // Identify a DNF as the worst time if DNFs are present.
                    final long worstTime = ac.mNumCurrentDNFs == 0 ? ac.mCurrentWorstTime : DNF;

                    for (int i = 0; i < n && (bestIdx == -1 || worstIdx == -1); i++) {
                        // Use if...else... here to ensure that the best and worst times are not
                        // recorded at the same index (e.g., if all times are DNFs or all equal).
                        if (bestIdx == -1 && mTimes[i] == bestTime) {
                            bestIdx = i;
                        } else if (worstIdx == -1 && mTimes[i] == worstTime) {
                            worstIdx = i;
                        }
                    }
                }

                mBestTimeIndex = bestIdx;
                mWorstTimeIndex = worstIdx;
            } else {
                mTimes = null;
                mBestTimeIndex = -1;
                mWorstTimeIndex = -1;
            }
        }

        /**
         * Gets the array of values that contributed to the calculation of the average-of-N. If
         * too few values have been recorded (less than "N"), the array will be {@code null}. The
         * times will be ordered with the oldest recorded time first and may include {@link #DNF}
         * values. The best and worst times can be identified with {@link #getBestTimeIndex()} and
         * {@link #getWorstTimeIndex()}.
         *
         * @return
         *     The array of times used to calculate the average. May be {@code null}.
         */
        public long[] getTimes() {
            return mTimes;
        }

        /**
         * Gets the calculated average-of-N value. The calculation follows the normal rules for
         * the value of "N" that are applied by the average calculator from which this object was
         * captured.
         *
         * @return
         *     The average-of-N value. May be {@link #DNF} if the average was disqualified, or
         *     {@link #UNKNOWN} if too few times have been recorded (i.e., less than "N").
         */
        public long getAverage() {
            return mAverage;
        }

        /**
         * Gets the index within the array returned by {@link #getTimes()} of the best time
         * eliminated for the average-of-N calculation. If there were insufficient times, or if the
         * value of "N" is lower than the threshold where best times are eliminated, or if there
         * are less than two non-DNF times, the result will be -1. When DNFs do not disqualify the
         * average and there is only one non-DNF time, that time is not identified as the "best"
         * time, so it is not eliminated; instead that single time becomes the average time.
         *
         * @return The index of the best time value, or -1 if it is not known.
         */
        public int getBestTimeIndex() {
            return mBestTimeIndex;
        }

        /**
         * Gets the index within the array returned by {@link #getTimes()} of the worst time
         * eliminated for the average-of-N calculation. If there were insufficient times, or if the
         * value of "N" is lower than the threshold where worst times are eliminated, or if all of
         * the times are DNF times, the result will be -1. If DNFs are present and at least one
         * time is not a DNF, the first DNF will be marked as the worst time.
         *
         * @return The index of the worst time value, or -1 if it is not known.
         */
        public int getWorstTimeIndex() {
            return mWorstTimeIndex;
        }
    }
}
