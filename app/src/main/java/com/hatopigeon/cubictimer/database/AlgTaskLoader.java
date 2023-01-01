package com.hatopigeon.cubictimer.database;

import android.database.Cursor;
import androidx.loader.content.CursorLoader;

import com.hatopigeon.cubictimer.CubicTimer;

public class AlgTaskLoader extends CursorLoader {

    String subset;

    public AlgTaskLoader(String subset) {
        super(CubicTimer.getAppContext());
        this.subset = subset;
    }

    @Override
    public Cursor loadInBackground() {
        return CubicTimer.getReadableDB().query(
                DatabaseHandler.TABLE_ALGS, null,
                DatabaseHandler.KEY_SUBSET + "=?",
                new String[] { subset }, null, null, null, null);
    }
}
