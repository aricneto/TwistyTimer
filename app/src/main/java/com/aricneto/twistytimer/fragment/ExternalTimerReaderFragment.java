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
        private static final float BITLENGTH = 44100/1200;

        public AudioRecorderThread() {
        }

        public void run() {
            running = true;
            paused = false;

            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
            int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_8BIT);

            int bufferLength = SAMPLE_RATE / 8;

            byte[] audioBuffer = new byte[bufferLength];
            //Double length in case packets are split between buffers
            boolean[] dataBuffer = new boolean[bufferLength * 2];

            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.UNPROCESSED,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_8BIT,
                    bufferSize);

            recorder.startRecording();

            int result;
            do {
                result = recorder.read(audioBuffer, 0, bufferLength);
            }
            while (result == 0);

            for (int i = 0; i < audioBuffer.length; i++) {
                dataBuffer[i] = audioBuffer[i] < 0 ? false : true;
            }

            while (running) {
                result = recorder.read(audioBuffer, 0, bufferLength);
                if (result == 0) {
                    try {
                        sleep(100);
                        continue;
                    }
                    catch (Exception e) {}
                }
                for (int i = 0; i < audioBuffer.length; i++) {
                    dataBuffer[bufferLength + i] = audioBuffer[i] < 0 ? false : true;
                }

                int onecount = 0;
                for (int i = 0; i < audioBuffer.length; i++) {
                    if (dataBuffer[i]) {
                        onecount++;
                    } else {
                        //Found packet start
                        if (onecount >= 9 * BITLENGTH) {
                            int limit = (int) (i + 90.5 * BITLENGTH); //8 bytes with 2 stop bits each
                            byte[] packet = new byte[8];

                            ArrayList<Run> runs = runLengthEncode(dataBuffer, i, limit);
                            ArrayList<Boolean> bits = getBitsFromRuns(runs);
                            byte[] bytes = getBytesfromBits(bits);

                            if (isValidPacket(bytes)) {
                                TimerState state = new TimerState(bytes);
                                if (!paused) {
                                    handler.obtainMessage(TIMER_PACKET_WHAT, state).sendToTarget();
                                }
                            }

                        }
                        onecount = 0;

                    }

                }

                //Shift data buffer
                for (int i = bufferLength; i < dataBuffer.length; i++) {
                    dataBuffer[i - bufferLength] = dataBuffer[i];
                }
            }
            recorder.stop();
        }

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

        private byte[] getBytesfromBits(ArrayList<Boolean> bits) {
            try {
                byte[] bytes = new byte[9];
                for (int i = 0; i < bytes.length; i++) {
                    //One byte is one startbit, 8 data bits and one stop bit
                    bytes[i] = 0;
                    for (int j = 0; j <= 8; j++) {
                        if (bits.get(i * 10 + j + 1)) {
                            bytes[i] |= 0x01 << j;
                        }
                    }
                }
                return bytes;
            }
            catch (Exception e) {
                return new byte[9];
            }
        }

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

        private ArrayList<Run> runLengthEncode(boolean[] array, int start, int limit) {
            ArrayList<Run> runs = new ArrayList<Run>();
            int fulllength = 0;
            int count = 0;
            boolean lastvalue = array[start];
            for (int i = start; i < limit; i++) {
                if (array[i] == lastvalue) {
                    count++;
                } else {
                    runs.add(new Run(lastvalue, count));
                    fulllength += count;
                    lastvalue = array[i];
                    count = 0;
                }
            }
            if (count != 0) {
                runs.add(new Run(lastvalue, count));

                fulllength += count;
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


