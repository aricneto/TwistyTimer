package com.aricneto.twistytimer.database;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.CursorLoader;

import com.aricneto.twistytimer.TwistyTimer;

public class AlgTaskLoader extends CursorLoader {

    String subset;

    public AlgTaskLoader(Context context, String subset) {
        super(context);
        this.subset = subset;
    }

    @Override
    public Cursor loadInBackground() {
        return TwistyTimer.getReadableDB().query(
                DatabaseHandler.TABLE_ALGS, null,
                DatabaseHandler.KEY_SUBSET + "=?",
                new String[] { subset }, null, null, null, null);
    }

    @Override
    protected void onStopLoading() {
        super.onStopLoading();
    }
}
