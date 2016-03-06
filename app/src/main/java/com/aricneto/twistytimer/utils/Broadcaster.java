package com.aricneto.twistytimer.utils;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Created by Ari on 03/03/2016.
 */
public class Broadcaster {

    public static void broadcast(Context context, String intent, String action) {
        Intent sendIntent = new Intent(intent);
        sendIntent.putExtra("action", action);
        LocalBroadcastManager.getInstance(context).sendBroadcast(sendIntent);
    }
}
