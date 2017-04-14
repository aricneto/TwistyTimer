package com.aricneto.twistytimer.fragment;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;

import com.aricneto.twistytimer.utils.TimerState;

import java.util.ArrayList;

/**
 * Created by adrian on 4/7/17.
 */

/**
 * Headless Fragment wrapping a custom thread. This fragment will be retained,
 * because we can't retain the parent fragment.
 */
public class ExternalTimerReaderFragment extends Fragment {

    public static final String TAG = "ExternalTimerReadFragment";
    public static final int TIMER_PACKET_WHAT = 1;

    AudioRecorderThread thread;
    Handler handler;

    private boolean running;
    private boolean paused;

    @SuppressLint("ValidFragment")
    public ExternalTimerReaderFragment(Handler handler) {
        this.handler = handler;
    }

    //This should never be called as the fragment is retained, but android requires it.
    public ExternalTimerReaderFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        running = false;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public void start() {
        thread = new AudioRecorderThread();
        thread.start();
    }

    public void stop() {
        running = false;
    }

    public void pause() {
        paused = true;
    }

    public void unpause() {
        paused = false;
    }

    private class AudioRecorderThread extends Thread {

        private static final int SAMPLE_RATE = 44100;
        private static final int BITRATE = 1200;
        private static final float BITLENGTH = (float) SAMPLE_RATE/BITRATE;
        private static final int bufferLength = SAMPLE_RATE / 8;
        private static final short peekThreshold = 28000;

        public AudioRecorderThread() {
        }

        public void run() {
            running = true;
            paused = false;

            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
            //Using 16bit instead of 8 bit because 8 bit is unsigned and would require us doing a 2s complement
            int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);

            //Double length in case packets are split between buffers
            short[] audioBuffer = new short[bufferLength * 2];

            final int audioSource = MediaRecorder.AudioSource.DEFAULT;

            AudioRecord recorder = new AudioRecord(audioSource,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize);

            recorder.startRecording();

            int result;
            do {
                result = recorder.read(audioBuffer, 0, bufferLength);
            }
            while (result <= 0);

            short threshold = 29000;

            while (running) {
                result = recorder.read(audioBuffer, bufferLength, bufferLength);
                if (result <= 0) {
                    try {
                        sleep(100);
                        continue;
                    }
                    catch (Exception e) {}
                }

                findPackets(audioBuffer);

                //Shift audio buffer
                for (int i = bufferLength; i < audioBuffer.length; i++) {
                    audioBuffer[i - bufferLength] = audioBuffer[i];
                }
            }
            recorder.stop();
            recorder.release();
            recorder = null;
        }

        private boolean findPackets(short[] audioBuffer) {
            boolean foundpacket = false;
            int zerocount = 0;

            for (int i = 0; i < audioBuffer.length-2; i++) {
                if (audioBuffer[i] < peekThreshold && audioBuffer[i] > -peekThreshold) { //No peek
                    zerocount++;
                } else {
                    //Found packet start
                    if (zerocount >= 9 * BITLENGTH) {
                        int limit = (int) (i + 90.5 * BITLENGTH); //8 bytes with 2 stop bits each. .5 for tolerance
                        if (limit < audioBuffer.length) {
                            ArrayList<Run> runs = runLengthEncodePeeks(audioBuffer, i, limit);
                            ArrayList<Boolean> bits = getBitsFromRuns(runs);
                            byte[] bytes = getBytesfromBits(bits);

                            if (isValidPacket(bytes)) {
                                foundpacket = true;
                                TimerState state = new TimerState(bytes);
                                if (!paused) {
                                    handler.obtainMessage(TIMER_PACKET_WHAT, state).sendToTarget();
                                }
                            }
                        }
                    }
                    zerocount = 0;
                }
            }

            return foundpacket;
        }

        /**
         * Checks if a packet is valid
         * @param bytes packet to check
         * @return if the packet is valid
         */
        private boolean isValidPacket(byte[] bytes) {
            boolean isValid = " ACILRS".contains(String.valueOf((char) bytes[0])); //Timer State

            int checksum = 0;
            for(int i = 1; i < 6; i++) {
                isValid &= Character.isDigit(bytes[i]); //Has to be a digit
                checksum += bytes[i] - '0'; //Convert charcode to digit
            }
            checksum += 64;

            isValid &= bytes[6] == checksum;
            isValid &= bytes[7] == '\n';
            isValid &= bytes[8] == '\r';

            return isValid;
        }

        /**
         * Turns an ArrayList of bits into an array of 9 bytes.
         * @param bits Bits to convert
         * @return array of 9 bytes or empty byte array if input isn't valid
         */
        private byte[] getBytesfromBits(ArrayList<Boolean> bits) {
            if (bits.size() >= 90) {
                byte[] bytes = new byte[9];
                for (int i = 0; i < bytes.length; i++) {
                    //One startbit, 8 data bits and one stop bit
                    if (bits.get(i*10) == false && bits.get(i*10+9) == true) {
                        bytes[i] = 0;
                        for (int j = 0; j <= 8; j++) {
                            if (bits.get(i * 10 + j + 1)) {
                                bytes[i] |= 0x01 << j; //Most significant bit last
                            }
                        }
                    }
                    else {
                        return new byte[9];
                    }
                }
                return bytes;
            }
            else {
                return new byte[9];
            }
        }

        /**
         * Turn runs into an ArrayList of bits
         * @param runs Runs to convert
         * @return ArrayList of bits
         */
        private ArrayList<Boolean> getBitsFromRuns(ArrayList<Run> runs) {
            ArrayList<Boolean> bits = new ArrayList<Boolean>();
            for(int i = 0; i < runs.size(); i++) {
                int bitcount = Math.round(runs.get(i).length / BITLENGTH);
                for(int j = 0; j < bitcount; j++) {
                    bits.add(runs.get(i).value);
                }
            }
            return bits;
        }

        /**
         * Returns run lengths from audioBuffer when AudioSource.UNPROCESSED is used.
         * Currently unused, as AudioSource.UNPROCESSED is unly supported by API level 24 and newer.
         *
         * @param audioBuffer audioBuffer as returned by a AudioRecord with audiosource set to UNPROCESSED
         * @param start Start index of the packet
         * @param limit end index of the packet
         * @return An ArrayList of Runs
         */
        @SuppressWarnings("unused")
        private ArrayList<Run> runLengthEncode(short[] audioBuffer, int start, int limit) {
            boolean dataBuffer[] = new boolean[limit-start];
            for (int i = start; i < limit; i++) {
                dataBuffer[i-start] = audioBuffer[i] < 0;
            }

            ArrayList<Run> runs = new ArrayList<Run>();
            int count = 0;
            boolean lastvalue = dataBuffer[0];
            for (int i = 0; i < dataBuffer.length; i++) {
                if (dataBuffer[i] == lastvalue) {
                    count++;
                } else {
                    runs.add(new Run(lastvalue, count));
                    lastvalue = dataBuffer[i];
                    count = 0;
                }
            }
            if (count != 0) {
                runs.add(new Run(lastvalue, count));
            }
            return runs;
        }

        /**
         * Returns run lengths from audioBuffer when AudioSource.DEFAULT is used.
         *
         * @param audioBuffer audioBuffer as returned by a AudioRecord with audiosource set to DEFAULT
         * @param start Start index of the packet
         * @param limit end index of the packet
         * @return
         */
        private ArrayList<Run> runLengthEncodePeeks(short[] audioBuffer, int start, int limit) {
            ArrayList<Run> runs = new ArrayList<Run>();
            int count = 0;
            int fulllength = 0;
            boolean lastvalue = audioBuffer[start] < 0;

            for (int i = start; i < limit-2; i++) {
                count++;
                if (lastvalue) {
                    if (audioBuffer[i] > peekThreshold) {
                        if(audioBuffer[i-5] > 25000) {
                            continue;
                        }
                        while (audioBuffer[i + 1] >= audioBuffer[i] && i < limit - 2) {
                            i++;
                            count++;
                        }
                        runs.add(new Run(lastvalue, count));
                        lastvalue = audioBuffer[i] < 0;
                        count = 0;
                    }
                }
                else {
                    if (audioBuffer[i] < -peekThreshold) {
                        if(audioBuffer[i-5] < -25000) {
                            continue;
                        }
                        while (audioBuffer[i + 1] <= audioBuffer[i] && i < limit - 2) {
                            i++;
                            count++;
                        }
                        runs.add(new Run(lastvalue, count));
                        lastvalue = audioBuffer[i] < 0;
                        count = 0;
                    }
                }
            }

            if (count != 0) {
                runs.add(new Run(lastvalue, count));
            }
            return runs;
        }

        private class Run {
            private boolean value;
            private int length;

            public Run(boolean value, int length) {
                this.value = value;
                this.length = length;
            }

            public boolean getValue() {
                return value;
            }

            public int getLength() {
                return length;
            }
        }
    }

}


