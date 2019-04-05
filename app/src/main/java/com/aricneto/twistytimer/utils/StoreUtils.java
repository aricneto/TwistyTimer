package com.aricneto.twistytimer.utils;

import android.content.Context;
import android.os.Environment;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Ari on 24/03/2016.
 */
public class StoreUtils {
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public static String inputStreamToString (InputStream inputStream) {
        try {
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes, 0, bytes.length);
            String json = new String(bytes);
            return json;
        } catch (IOException e) {
            return null;
        }
    }
}
