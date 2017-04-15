package com.aricneto.twistytimer.utils;

/**
 * Created by adrian on 4/4/17.
 */

public class TimerState {
    private static final int CHARCODE_OFFSET = 48;

    private int minutes;
    private int seconds;
    private int hundredths;
    private boolean running = false;
    //stopped is only true when the timer is stopped and is displaying the final time
    private boolean stopped = false;
    private boolean ready = false;
    private boolean reset = false;
    private boolean pressed = false;

    public TimerState(byte[] data) {
        boolean maybeRunning = false;
        switch ((char) data[0]) {
            case ' ': //Running
                running = true;
                break;
            case 'I': //Reset
                reset = true;
                break;
            case 'S': //Stopped
                stopped = true;
                break;
            //L, R and C don't specify if the timer is running or not.
            case 'L': //Left Hand on
                maybeRunning = true;
                break;
            case 'R': //Right Hand on
                maybeRunning = true;
                break;
            case 'C': //Both Hands on but not ready
                maybeRunning = true;
                pressed = true;
                break;
            case 'A': //Both Hands on and ready
                ready = true;
                break;
        }

        minutes = data[1] - CHARCODE_OFFSET;
        seconds = 10*(data[2] - CHARCODE_OFFSET) + (data[3] - CHARCODE_OFFSET);
        hundredths = 10*(data[4] - CHARCODE_OFFSET) + (data[5] - CHARCODE_OFFSET);

        if(maybeRunning && (minutes != 0 || seconds != 0 || hundredths !=0)) {
            running = true;
        }
    }

    public int getMinutes() {
        return minutes;
    }

    public int getSeconds() {
        return seconds;
    }

    public int getHundredths() {
        return hundredths;
    }

    public int getMS() {
        return hundredths*10 + seconds*1000 + minutes*60000;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isStopped() {
        return stopped;
    }

    public boolean isReady() {
        return ready;
    }

    public boolean isReset() {
        return reset;
    }

    public boolean isPressed() {
        return pressed;
    }
}