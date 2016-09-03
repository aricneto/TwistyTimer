package com.aricneto.twistytimer.layout;

/*
 * The Android chronometer widget revised so as to count milliseconds
 */

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.Html;
import android.util.AttributeSet;
import android.widget.TextView;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.utils.Prefs;
import com.aricneto.twistytimer.utils.PuzzleUtils;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * A chronometer for twisty puzzles of all types. This supports timing in milliseconds, display of
 * the elapsed time to a high resolution (hundredths of a second) or low resolution (whole seconds),
 * addition of standard "+2" and "DNF" penalties, and "hold-for-start" behaviour that can restore a
 * previous time if the hold is cancelled.
 */
public class ChronometerMilli extends TextView {
    @SuppressWarnings("unused")
    private static final String TAG = "Chronometer";

    /**
     * Low resolution time format for times of one hour or greater.
     */
    private static final String TIME_FMT_HOURS_LR = "k':'mm'<small>:'ss'</small>'";

    /**
     * Low resolution time format for times from one minute (inclusive) to one hour (exclusive).
     */
    private static final String TIME_FMT_MINS_LR = "m'<small>:'ss'</small>'";

    /**
     * Low resolution time format for times less than one minute.
     */
    private static final String TIME_FMT_SECS_LR = "s";

    /**
     * High resolution time format for times from one minute (inclusive) to one hour (exclusive).
     */
    private static final String TIME_FMT_MINS_HR = "m':'ss'<small>.'SS'</small>'";

    /**
     * High resolution time format for times less than one minute.
     */
    private static final String TIME_FMT_SECS_HR = "s'<small>.'SS'</small>'";

    /**
     * The penalty time in milliseconds for a standard "+2" penalty.
     */
    private static final long TWO_SECOND_PENALTY_MS = 2_000L;

    /**
     * The number of milliseconds between updates to the displayed of a low-resolution elapsed time.
     */
    private static final long TICK_TIME_LR = 100L; // 0.1 seconds to avoid jerkiness.

    /**
     * The number of milliseconds between updates to the displayed of a high-resolution elapsed
     * time.
     */
    private static final long TICK_TIME_HR = 10L; // 0.01 seconds (100 fps). Probably overkill.

    private static final int TICK_WHAT = 2;

    private String hideTimeText;
    private boolean hideTimeEnabled;

    /**
     * The time (system elapsed real time in milliseconds) at which this chronometer was started.
     * Will be zero if the chronometer has not been started or has been reset.
     */
    private long mStartedAt;

    /**
     * The time (system elapsed real time in milliseconds) at which this chronometer was stopped.
     * Will be zero if the chronometer has not been stopped or has been reset.
     */
    private long mStoppedAt;

    /**
     * The code for additional penalty. Values from {@link PuzzleUtils} are supported.
     */
    private int mPenalty;

    private boolean mIsVisible;
    private boolean mIsStarted;
    private boolean mIsRunning;

    /**
     * Indicates if this chronometer is holding in readiness to be started once the minimum hold
     * period has elapsed.
     */
    private boolean mIsHoldingForStart;

    /**
     * The text that was being displayed by this chronometer before entering the hold-for-start
     * state. If the state is cancelled, this text will be restored.
     */
    private CharSequence mTextSavedBeforeHolding;

    /**
     * Indicates if seconds will be shown to a high resolution while the timer is started. If
     * enabled, fractions (hundredths) of a second will be displayed while the chronometer is
     * running. See {@link #updateText()} for details on how and when this preference is applied.
     */
    private boolean mShowHiRes;

    /**
     * The normal text color. This is saved before the text is highlighted and restored when
     * highlighting is turned off.
     */
    private int mNormalColor;

    public ChronometerMilli(Context context) {
        this(context, null, 0);
    }

    public ChronometerMilli(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChronometerMilli(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init();
    }

    private void init() {
        // Save the current (normal) text color, as it may be overwritten with the highlight color.
        // The highlight color is available from "getHighlightColor", as that is never changed.
        // The implementation assumes that different colors for different states will not be used.
        mNormalColor = getCurrentTextColor();

        mShowHiRes = Prefs.getBoolean(R.string.pk_show_hi_res_timer, true);
        hideTimeEnabled = Prefs.getBoolean(R.string.pk_hide_time_while_running, false);
        hideTimeText = getContext().getString(R.string.hideTimeText);

        // The initial state will cause "0.00" to be displayed.
        updateText();
    }

    /**
     * Sets the highlighted state of the time value text displayed by this chronometer. This can be
     * used with the start cue and hold-for-start behaviour.
     *
     * @param isHighlighted
     *     {@code true} to highlight the text in a different color; or {@code false} to restore the
     *     normal text color.
     */
    public void setHighlighted(boolean isHighlighted) {
        setTextColor(isHighlighted ? getHighlightColor() : mNormalColor);
    }

    /**
     * Indicates if this chronometer has been started. If started, it can be stopped, but it cannot
     * be started again or reset until it has been stopped. This should be tested before calling
     * {@link #start()}, {@link #stop()} or {@link #reset()}, if the state is not already known.
     *
     * @return
     *     {@code true} if this chronometer has been started; or {@code false} if it has not been
     *     started.
     */
    public boolean isStarted() {
        return mIsStarted;
    }

    /**
     * Gets the elapsed time (in milliseconds) measured by this chronometer including any additional
     * time penalty. This method may be called even if this chronometer is currently started. Any
     * penalty time set by {@link #setPenalty(int)} will be included in the reported elapsed time.
     * If a "DNF" penalty was applied, the elapsed time will be reported as zero.
     *
     * @return
     *     The elapsed time measured by this chronometer including any time penalty, or zero if
     *     the penalty is a "DNF".
     */
    public long getElapsedTime() {
        switch (mPenalty) {
            case PuzzleUtils.PENALTY_DNF:
                return 0L;

            case PuzzleUtils.PENALTY_PLUSTWO:
                return getElapsedTimeExcludingPenalties() + TWO_SECOND_PENALTY_MS;

            default:
                return getElapsedTimeExcludingPenalties();
        }
    }

    /**
     * Gets the elapsed time (in milliseconds) measured by this chronometer excluding any additional
     * time penalties. This method may be called even if this chronometer is currently started. Any
     * penalty time set by {@link #setPenalty(int)} will <i>not</i> be included in the reported
     * elapsed time.
     *
     * @return The elapsed time measured by this chronometer excluding penalties.
     */
    private long getElapsedTimeExcludingPenalties() {
        // If the chronometer is started, then the elapsed time is the difference between "now" and
        // "mStartedAt". If the chronometer has never been started, has been stopped, or has been
        // reset, then the difference between "mStoppedAt" and "mStartedAt" is used. This ensures
        // that the initial state or reset state will display "0.00".
        return (mIsStarted ? SystemClock.elapsedRealtime() : mStoppedAt) - mStartedAt;
    }

    /**
     * Holds the chronometer is a state ready to be started from zero. This will display a zero
     * start time, but, if {@link #cancelHoldForStart()} is called, the previously displayed value
     * be restored. If {@link #start()} is called subsequently, the recorded elapsed time and any
     * penalties will <i>not</i> be reset automatically, so be sure to call {@link #reset()} first,
     * if appropriate. Both of those methods also exit this state, so {@code cancelHoldForStart()}
     * will no longer have any effect.
     *
     * @throws IllegalStateException
     *     If the chronometer is already started.
     */
    public void holdForStart() {
        if (mIsStarted) {
            // There is no use case where the chronometer will be held *before* starting when it is
            // *already* started, so throw an exception to highlight a likely bug in the caller.
            throw new IllegalStateException("Cannot hold chronometer if already started.");
        }

        // "TimerFragment" directly sets the text on this chronometer view when doing an inspection
        // count-down, inspection penalty, or displaying "DNF". Those functions should really be
        // performed using a separate text view, or should be properly integrated into this class.
        // In the meantime, before holding for the start, save the displayed text (whatever it is)
        // and restore it if "cancelHoldForStart" is called. Do not call "updateText" to restore the
        // previous elapsed time, as "TimerFragment" may have hijacked this view to show something
        // else.
        //
        // Also, this "where-did-that-text-come-from?" condition can also be the result of the
        // default state-saving of this view, as full state saving and restoration of the elapsed
        // time, penalties, etc. is not yet implemented.
        mIsHoldingForStart = true;
        mTextSavedBeforeHolding = getText();
        updateText(); // Will display "0.00" because "mIsHoldingForStart" is set.
    }

    /**
     * Cancels the hold-for-start state and restores the value previously displayed by this
     * chronometer. If the chronometer is not in the hold-for-start state, this method will have
     * no effect.
     */
    public void cancelHoldForStart() {
        if (mIsHoldingForStart) {
            mIsHoldingForStart = false;
            if (mTextSavedBeforeHolding != null) {
                // Do not call "updateText" to restore the saved value, as the saved text may not
                // have been set by this chronometer.
                setText(mTextSavedBeforeHolding);
            }
        }
    }

    /**
     * Ends the hold-for-start state <i>without</i> restoring the value previously displayed by
     * this chronometer. If the chronometer is not in the hold-for-start state, this method will
     * have no effect. This method is called automatically if the chronometer is started or reset.
     */
    private void endHoldForStart() {
        if (mIsHoldingForStart) {
            mIsHoldingForStart = false;
            mTextSavedBeforeHolding = null;
        }
    }

    /**
     * Starts the chronometer, resuming the recording of the elapsed time from where it left off
     * when it was last stopped. To restart from zero and clear penalties, call {@link #reset()}
     * first. If this chronometer is already started, calling this method will have no effect.
     * This will also exit the "hold-for-start" state if it is active; the displayed text value
     * saved when that state was entered will not be restored.
     */
    public void start() {
        if (mIsStarted) {
            return;
        }

        // For some puzzle types, the elapsed time could be long (many minutes, or even hours), so
        // the need to support a "pause" feature during informal timing sessions may be useful.
        // Here, calculate the new "mStartedAt" value and then offset it into the past by the
        // amount of elapsed time already recorded, which will allow sequences of state changes
        // such as "reset-start-stop-start-stop-start-stop" to accumulate time as necessary. If
        // already started, "stop-start" is effectively "pause-resume". Do not include any penalty
        // time in the elapsed time offset, it will remain separate.
        mStartedAt = SystemClock.elapsedRealtime() - getElapsedTimeExcludingPenalties();
        mStoppedAt = 0L;
        mIsStarted = true;

        // If we were holding for a start, stop doing that now and discard any saved text.
        endHoldForStart();

        updateText();
        updateRunning();
    }

    /**
     * Stops the chronometer. The elapsed time will no longer be incremented until the chronometer
     * is started again. If this chronometer is already stopped, calling this method will have no
     * effect.
     *
     * @throws IllegalStateException
     *     If the chronometer is already started.
     */
    public void stop() {
        if (!mIsStarted) {
            return;
        }

        mIsStarted = false;
        mStoppedAt = SystemClock.elapsedRealtime();

        // Update the text to show the exact elapsed time at this precise moment.
        updateText();

        // Stop updating the display if necessary, as the chronometer is no longer running.
        updateRunning();
    }

    /**
     * Resets the time to zero. The chronometer must be stopped before it can be reset. This will
     * also exit the "hold-for-start" state if it is active; the displayed text value saved when
     * that state was entered will not be restored.
     *
     * @throws IllegalStateException
     *     If the chronometer is currently started.
     */
    public void reset() throws IllegalStateException {
        if (mIsStarted) {
            // There is no use case where the chronometer will be reset without first being
            // stopped, so throw an exception to highlight a likely bug in the caller.
            throw new IllegalStateException("Chronometer cannot be reset if it has been started.");
        }

        mStartedAt = 0L;
        mStoppedAt = 0L;
        mPenalty = PuzzleUtils.NO_PENALTY;

        // If we were holding for a start, stop doing that now and discard any saved text.
        endHoldForStart();

        // No need to call "updateRunning()", as we have not changed the "running" state.
        updateText();
    }

    /**
     * Sets a penalty to be applied to the currently recorded elapsed time. If a 2-second penalty
     * is applied, a "+" is appended to the display of the elapsed time to indicate that a penalty
     * time has been added and {@link #getElapsedTime()} will include the extra penalty. If a
     * did-not-finish penalty is set, "DNF" is displayed. Any previously set penalty is replaced
     * by the new penalty. The {@code NO_PENALTY} value can also be set to remove a 2-second or
     * DNF penalty and restore the elapsed time.
     *
     * @param penalty
     *     The code for the penalty to be applied. Use only {@link PuzzleUtils#NO_PENALTY},
     *     {@link PuzzleUtils#PENALTY_PLUSTWO} or {@link PuzzleUtils#PENALTY_DNF}.
     *
     * @throws IllegalArgumentException
     *     If the penalty code is not one of those supported by this method.
     */
    public void setPenalty(int penalty) {
        switch (penalty) {
            case PuzzleUtils.NO_PENALTY:
            case PuzzleUtils.PENALTY_PLUSTWO:
            case PuzzleUtils.PENALTY_DNF:
                mPenalty = penalty;
                break;

            default:
                throw new IllegalArgumentException("Penalty code is not allowed.");
        }

        // Show the new time with the included penalty and the "+" penalty indicator, if needed.
        updateText();
    }

    /**
     * <p>
     * Updates the text that displays the current elapsed time. The formatting of the time depends
     * on the state of the chronometer and the preference for showing fractional seconds values.
     * When the chronometer is stopped, fractional seconds values are shown. When the chronometer is
     * started (running), fractional seconds are only shown if the respective preference is enabled.
     * Fractional seconds are never shown for elapsed times of one hour or longer, regardless of the
     * state of the chronometer.
     * </p>
     * <p>
     * A preference to hide the elapsed time while the chronometer is running is also supported. If
     * the preference is enabled and the chronometer is started, then the elapsed time will not be
     * shown; a fixed string will be shown in its place.
     * </p>
     * <p>
     * If a "+2" penalty has been applied and the chronometer is stopped, "+" will be appended to
     * the display of the elapsed time. If a "DNF" penalty has been applied, "DNF" will be displayed
     * instead of the elapsed time.
     * </p>
     *
     * @return
     *     {@code true} if the displayed text presented a high-resolution, fractional value for
     *     the number of seconds, or {@code false} if only whole seconds were shown. This may be
     *     used to inform the necessary update frequency.
     */
    private synchronized boolean updateText() {
        // The displayed elapsed time will include any time penalty. If holding before starting,
        // then assume that the elapsed time will be started at zero and ignore the previously
        // recorded elapsed time and any current penalty.
        String timeText;
        final boolean isHiRes;

        if (mIsStarted && hideTimeEnabled) {
            timeText = hideTimeText;
            isHiRes = false;
        } else if (!mIsHoldingForStart && mPenalty == PuzzleUtils.PENALTY_DNF) {
            timeText = "DNF";
            isHiRes = false;
        } else {
            final long elapsedMS = mIsHoldingForStart ? 0L : getElapsedTime();
            final long hours = elapsedMS / (3_600_000L);
            final long minutes = (elapsedMS % (3_600_000L)) / (60_000L);
            final String timeFormat;

            isHiRes = (!mIsStarted || mShowHiRes) && hours == 0;

            if (hours > 0) {
                timeFormat = TIME_FMT_HOURS_LR; // Always low resolution when > 1 hour.
            } else if (minutes > 0) {
                timeFormat = isHiRes ? TIME_FMT_MINS_HR : TIME_FMT_MINS_LR;
            } else {
                timeFormat = isHiRes ? TIME_FMT_SECS_HR : TIME_FMT_SECS_LR;
            }

            timeText = new DateTime(elapsedMS, DateTimeZone.UTC).toString(timeFormat);

            // If a "+2" penalty has been applied and the chronometer is not started or holding,
            // append a small "+" to the time text to declare that a penalty has been added.
            if (!mIsStarted && !mIsHoldingForStart && mPenalty == PuzzleUtils.PENALTY_PLUSTWO) {
                timeText += " <small>+</small>";
            }
        }

        setText(Html.fromHtml(timeText));

        return isHiRes;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mIsVisible = false;
        updateRunning();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        mIsVisible = visibility == VISIBLE;
        updateRunning();
    }

    /**
     * Updates the running state of this chronometer. The chronometer is "running" if it is in the
     * started state and is visible. In the "running" state, the display of the current elapsed time
     * will be updated regularly.
     */
    private void updateRunning() {
        boolean running = mIsVisible && mIsStarted;

        if (running != mIsRunning) {
            // State has changed:
            //
            //   If the chronometer was not running but has now started running, then kick off a
            //   chain of messages that will update the display of the elapsed time at regular
            //   intervals. One message is queued here and then a new message is queued as each
            //   message is handled by "TimeUpdateHandler.handleMessage".
            //
            //   If the chronometer was running but has now stopped running, clear "mIsRunning"
            //   (which causes "TimeUpdateHandler.handleMessage" to break the chain of update
            //   messages) and then clear any other unhandled "tick" messages from the queue.
            //
            // If the state has not changed, then things can be left alone: either the message
            // chain is active and perpetuating itself, or it is inactive.
            mIsRunning = running;

            if (mIsRunning) {
                // Use a very short "tick" time (1 ms) before the very first update.
                mHandler.sendMessageDelayed(Message.obtain(mHandler, TICK_WHAT, this), 1L);
            } else {
                mHandler.removeMessages(TICK_WHAT);
            }
        }
    }

    private Handler mHandler = new TimeUpdateHandler();

    // "static" handler class to prevent memory leaks.
    private static final class TimeUpdateHandler extends Handler {
        public void handleMessage(Message m) {
            if (m.obj != null) {
                final ChronometerMilli chronometer = (ChronometerMilli) m.obj;

                // Update the time display before checking if the chronometer is still "running".
                // This ensures that the time display is up-to-date with the exact elapsed time.
                //
                // Adapt the interval between updates to the current resolution of the display
                // of the seconds value, i.e., update faster if showing 100ths of a second.
                final long tickTime = chronometer.updateText() ? TICK_TIME_HR : TICK_TIME_LR;

                if (chronometer.mIsRunning) {
                    // Only chain a new message for the next update if still running.
                    sendMessageDelayed(Message.obtain(this, TICK_WHAT, chronometer), tickTime);
                }
            }
        }
    }
}
