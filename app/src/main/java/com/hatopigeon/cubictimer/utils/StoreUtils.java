package com.hatopigeon.cubictimer.utils;

import android.content.res.Resources;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import androidx.annotation.RawRes;

/**
 * Created by Ari on 24/03/2016.
 */
public class StoreUtils {
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public static String getStringFromRaw(Resources res, @RawRes int rawFile) {
        InputStream inputStream = res.openRawResource(rawFile);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        int in;
        try {
            while ((in = inputStream.read()) != -1)
                byteArrayOutputStream.write(in);
            inputStream.close();

            return byteArrayOutputStream.toString();
        } catch (IOException e) {
            throw new Error("Could not read from raw file");
        }
    }
}
