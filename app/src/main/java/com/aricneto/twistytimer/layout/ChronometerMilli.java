package com.aricneto.twistytimer.layout;

/*
 * The Android chronometer widget revised so as to count milliseconds
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.widget.TextView;

import com.aricneto.twistify.R;

import org.joda.time.DateTime;

public class ChronometerMilli extends TextView {
    @SuppressWarnings("unused")
    private static final String TAG = "Chronometer";
    private String hideTimeText;
    private boolean hideTimeEnabled;

    public interface OnChronometerTickListener {

        void onChronometerTick(ChronometerMilli chronometer);
    }

    private long    mBase;
    private boolean mVisible;
    private boolean mStarted;
    private boolean mRunning;

    private boolean millisecondsEnabled;

    private OnChronometerTickListener mOnChronometerTickListener;

    private static final int TICK_WHAT = 2;

    private long timeElapsed;

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
        mBase = SystemClock.elapsedRealtime();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        millisecondsEnabled = sharedPreferences.getBoolean("millisecondsEnabled", true);
        hideTimeEnabled = sharedPreferences.getBoolean("hideTimeEnabled", false);
        hideTimeText = getContext().getString(R.string.hideTimeText);
        //updateText(mBase);
    }

    public void setBase(long base) {
        mBase = base;
        dispatchChronometerTick();
        updateText(SystemClock.elapsedRealtime());
    }

    public long getBase() {
        return mBase;
    }

    public void setOnChronometerTickListener(
            OnChronometerTickListener listener) {
        mOnChronometerTickListener = listener;
    }

    public OnChronometerTickListener getOnChronometerTickListener() {
        return mOnChronometerTickListener;
    }

    public void start() {
        mBase = SystemClock.elapsedRealtime();
        mStarted = true;
        updateRunning();
    }

    public void stop() {
        mStarted = false;


        int hours = (int) (timeElapsed / (3600 * 1000));
        int remaining = (int) (timeElapsed % (3600 * 1000));

        int minutes = remaining / (60 * 1000);

        String text = "";

        if (hours > 0)
            text = new DateTime(timeElapsed).toString("kk':'mm':'ss'.'SS");

        else if (minutes > 0)
            text = new DateTime(timeElapsed).toString("mm':'ss'.'SS");

        else
            text = new DateTime(timeElapsed).toString("s'.'SS");

        setText(text);

        updateRunning();
    }


    public void setStarted(boolean started) {
        mStarted = started;
        updateRunning();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mVisible = false;
        updateRunning();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        mVisible = visibility == VISIBLE;
        updateRunning();
    }

    private synchronized void updateText(long now) {
        timeElapsed = now - mBase;

        int hours = (int) (timeElapsed / (3600 * 1000));
        int remaining = (int) (timeElapsed % (3600 * 1000));

        int minutes = remaining / (60 * 1000);

        String text = "";

        if (hideTimeEnabled) {
            text = hideTimeText;
        } else if (millisecondsEnabled) {
            if (hours > 0)
                text = new DateTime(timeElapsed).toString("kk':'mm':'ss'.'SS");

            else if (minutes > 0)
                text = new DateTime(timeElapsed).toString("mm':'ss'.'SS");

            else
                text = new DateTime(timeElapsed).toString("s'.'SS");

        } else {
            if (hours > 0)
                text = new DateTime(timeElapsed).toString("kk':'mm':'ss");

            else if (minutes > 0)
                text = new DateTime(timeElapsed).toString("mm':'ss");

            else
                text = new DateTime(timeElapsed).toString("s");
        }

        setText(text);
    }

    private void updateRunning() {
        boolean running = mVisible && mStarted;
        if (running != mRunning) {
            if (running) {
                updateText(SystemClock.elapsedRealtime());
                dispatchChronometerTick();
                mHandler.sendMessageDelayed(Message.obtain(mHandler,
                        TICK_WHAT), 1);
            } else {
                mHandler.removeMessages(TICK_WHAT);
            }
            mRunning = running;
        }
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message m) {
            if (mRunning) {
                updateText(SystemClock.elapsedRealtime());
                dispatchChronometerTick();
                sendMessageDelayed(Message.obtain(this, TICK_WHAT),
                        1);
            }
        }
    };

    void dispatchChronometerTick() {
        if (mOnChronometerTickListener != null) {
            mOnChronometerTickListener.onChronometerTick(this);
        }
    }

    public long getTimeElapsed() {
        return timeElapsed;
    }

}
