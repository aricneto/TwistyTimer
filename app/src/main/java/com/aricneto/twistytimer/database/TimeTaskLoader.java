package com.aricneto.twistytimer.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.CursorLoader;

import com.aricneto.twistytimer.utils.PuzzleUtils;

public class TimeTaskLoader extends CursorLoader {

    private DatabaseHandler handler;
    private SQLiteDatabase  db;

    String puzzleType;
    String puzzleSubtype;
    boolean history = false;

    public TimeTaskLoader(Context context, String puzzleType, String puzzleSubtype, boolean history) {
        super(context);
        this.puzzleType = puzzleType;
        this.puzzleSubtype = puzzleSubtype;
        this.history = history;
    }

    @Override
    public Cursor loadInBackground() {
        handler = new DatabaseHandler(getContext());
        db = handler.getReadableDatabase();

        if (history)
            return db.query(DatabaseHandler.TABLE_TIMES, null,
                    DatabaseHandler.KEY_PENALTY + "!=" + PuzzleUtils.PENALTY_HIDETIME + " AND "
                            + DatabaseHandler.KEY_TYPE + "=?" + " AND "
                            + DatabaseHandler.KEY_SUBTYPE + "=?" + " AND history = 1",
                    new String[] { puzzleType, puzzleSubtype, }, null, null,
                    DatabaseHandler.KEY_DATE + " DESC", null);
        else
            return db.query(DatabaseHandler.TABLE_TIMES, null,
                    DatabaseHandler.KEY_PENALTY + "!=" + PuzzleUtils.PENALTY_HIDETIME + " AND "
                            + DatabaseHandler.KEY_TYPE + "=?" + " AND "
                            + DatabaseHandler.KEY_SUBTYPE + "=?" + " AND history = 0",
                    new String[] { puzzleType, puzzleSubtype, }, null, null,
                    DatabaseHandler.KEY_DATE + " DESC", null);
    }

    @Override
    protected void onStopLoading() {
        //db.close();
        //handler.close();
        super.onStopLoading();
    }
}