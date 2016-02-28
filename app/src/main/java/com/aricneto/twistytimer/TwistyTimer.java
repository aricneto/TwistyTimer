package com.aricneto.twistytimer;/**
 * Created by Ari on 28/07/2015.
 */

import android.app.Application;

import net.danlew.android.joda.JodaTimeAndroid;

public class TwistyTimer extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        JodaTimeAndroid.init(this);
        //LeakCanary.install(this);
    }

}
