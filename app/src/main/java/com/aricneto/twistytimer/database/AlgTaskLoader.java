package com.aricneto.twistytimer.database;

import android.database.Cursor;
import androidx.loader.content.CursorLoader;

import com.aricneto.twistytimer.TwistyTimer;

public class AlgTaskLoader extends CursorLoader {

    String subset;
    int progress_filter = 100;
    public AlgTaskLoader(String subset, int progress_filter) {
        super(TwistyTimer.getAppContext());
        this.subset = subset;
        this.progress_filter = progress_filter;
    }

    @Override
    public Cursor loadInBackground() {
        return TwistyTimer.getReadableDB().query(
                DatabaseHandler.TABLE_ALGS, null,
                DatabaseHandler.KEY_SUBSET + "=? AND " + DatabaseHandler.KEY_PROGRESS +" <=?",
                new String[] { subset, Integer.toString(progress_filter) }, null, null, null, null);
    }
}
