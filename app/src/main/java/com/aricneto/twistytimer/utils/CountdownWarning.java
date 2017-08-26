package com.aricneto.twistytimer.utils;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.CountDownTimer;
import android.os.Vibrator;

import com.aricneto.twistytimer.TwistyTimer;

/**
 * A class used to create {@link CountDownTimer}s that vibrates and emits a tone once a specific
 * time has passsed, depending on how it's built. Must be built using
 * {@link CountdownWarning.Builder}.
 */
public class CountdownWarning extends CountDownTimer {
    private Vibrator vibrator;
    private ToneGenerator toneGenerator;

    private final boolean vibrateEnabled;
    private final long vibrateDuration;

    private final boolean toneEnabled;
    private final int toneDuration;
    private final int toneCode;

    private CountdownWarning(Builder builder) {
        super(builder.secondsInFuture * 1000, 50);
        vibrator = (Vibrator) TwistyTimer.getAppContext().getSystemService(Context.VIBRATOR_SERVICE);

        this.vibrateEnabled = builder.vibrateEnabled;
        this.vibrateDuration = builder.vibrateDuration;

        this.toneEnabled = builder.toneEnabled;
        this.toneDuration = builder.toneDuration;
        this.toneCode = builder.toneCode;

        this.toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
    }


    @Override
    public void onTick(long l) {
    }

    @Override
    public void onFinish() {
        if (vibrateEnabled)
            vibrator.vibrate(vibrateDuration);
        if (toneEnabled)
            toneGenerator.startTone(toneCode, toneDuration);
    }

    public static class Builder {
        private final long secondsInFuture;

        private boolean vibrateEnabled = true;
        private long vibrateDuration = 300;

        private boolean toneEnabled = false;
        private int toneDuration = 300;
        private int toneCode = ToneGenerator.TONE_CDMA_PIP;

        /**
         * Build a {@link CountdownWarning} object
         *
         * @param secondsInFuture
         *      the countdown duration in seconds
         */
        public Builder(long secondsInFuture) {
            this.secondsInFuture = secondsInFuture;
        }

        /**
         *  If device should vibrate at the end of countdown
         *
         * @param vibrateEnabled
         *      true if device should vibrate
         */
        public Builder withVibrate(boolean vibrateEnabled) {
            this.vibrateEnabled = vibrateEnabled;
            return this;
        }

        /**
         *  If device should emit a tone at the end of countdown
         *
         * @param toneEnabled
         *      true if device should emit a tone
         */

        public Builder withTone(boolean toneEnabled) {
            this.toneEnabled = toneEnabled;
            return this;
        }

        /**
         *  Duration, in milliseconds of the vibration (if set)
         *
         * @param vibrateDuration
         *      vibrate duration in milliseconds
         */
        public Builder vibrateDuration(long vibrateDuration) {
            this.vibrateDuration = vibrateDuration;
            return this;
        }

        /**
         *  Duration, in milliseconds of the tone (if set)
         *
         * @param toneDuration
         *      tone duration in milliseconds
         */
        public Builder toneDuration(int toneDuration) {
            this.toneDuration = toneDuration;
            return this;
        }

        /**
         * Code for the tone that should play (if set)
         * Must be one of {@link ToneGenerator}s tone constants
         *
         * @param toneCode
         *      the tone code, a {@link ToneGenerator} constant
         */
        public Builder toneCode(int toneCode) {
            this.toneCode = toneCode;
            return this;
        }

        public CountdownWarning build() {
            return new CountdownWarning(this);
        }
    }
}
