package com.aricneto.twistytimer.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.CursorLoader;

public class AlgTaskLoader extends CursorLoader {

    String subset;
    private DatabaseHandler handler;

    public AlgTaskLoader(Context context, String subset) {
        super(context);
        this.subset = subset;
    }



    @Override
    public Cursor loadInBackground() {
        handler = new DatabaseHandler(getContext());
        SQLiteDatabase db = handler.getReadableDatabase();

        return db.query(DatabaseHandler.TABLE_ALGS, null,
                DatabaseHandler.KEY_SUBSET + "=?",
                new String[] { subset }, null, null, null, null);
    }

    @Override
    protected void onStopLoading() {
        if (handler != null) {
            handler.closeDB();
        }
        super.onStopLoading();
    }
}