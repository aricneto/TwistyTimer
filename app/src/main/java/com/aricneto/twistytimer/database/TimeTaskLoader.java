package com.aricneto.twistytimer.database;

import android.database.Cursor;
import android.support.v4.content.CursorLoader;

import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.utils.PuzzleUtils;

public class TimeTaskLoader extends CursorLoader {

    String puzzleType;
    String puzzleSubtype;
    boolean history = false;

    public TimeTaskLoader(String puzzleType, String puzzleSubtype, boolean history) {
        super(TwistyTimer.getAppContext());
        this.puzzleType = puzzleType;
        this.puzzleSubtype = puzzleSubtype;
        this.history = history;
    }

    @Override
    public Cursor loadInBackground() {
        if (history)
            return TwistyTimer.getReadableDB().query(DatabaseHandler.TABLE_TIMES, null,
                    DatabaseHandler.KEY_PENALTY + "!=" + PuzzleUtils.PENALTY_HIDETIME + " AND "
                            + DatabaseHandler.KEY_TYPE + "=?" + " AND "
                            + DatabaseHandler.KEY_SUBTYPE + "=?" + " AND history = 1",
                    new String[] { puzzleType, puzzleSubtype, }, null, null,
                    DatabaseHandler.KEY_DATE + " DESC", null);
        else
            return TwistyTimer.getReadableDB().query(DatabaseHandler.TABLE_TIMES, null,
                    DatabaseHandler.KEY_PENALTY + "!=" + PuzzleUtils.PENALTY_HIDETIME + " AND "
                            + DatabaseHandler.KEY_TYPE + "=?" + " AND "
                            + DatabaseHandler.KEY_SUBTYPE + "=?" + " AND history = 0",
                    new String[] { puzzleType, puzzleSubtype, }, null, null,
                    DatabaseHandler.KEY_DATE + " DESC", null);
    }
}
