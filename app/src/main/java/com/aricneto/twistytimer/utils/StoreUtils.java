package com.aricneto.twistytimer.utils;

import android.os.Environment;

/**
 * Created by Ari on 24/03/2016.
 */
public class StoreUtils {
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
}
