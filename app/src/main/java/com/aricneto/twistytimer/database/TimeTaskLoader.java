package com.aricneto.twistytimer.database;

import android.database.Cursor;

import androidx.loader.content.CursorLoader;

import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.utils.PuzzleUtils;

public class TimeTaskLoader extends CursorLoader {

    String puzzleType;
    String puzzleSubtype;
    String comment;
    boolean history = false;

    public TimeTaskLoader(String puzzleType, String puzzleSubtype, boolean history, String comment) {
        super(TwistyTimer.getAppContext());
        this.puzzleType = puzzleType;
        this.puzzleSubtype = puzzleSubtype;
        this.history = history;
        this.comment = comment;
    }

    @Override
    public Cursor loadInBackground() {
        return TwistyTimer.getReadableDB().query(DatabaseHandler.TABLE_TIMES, null,
                DatabaseHandler.KEY_PENALTY + "!=" + PuzzleUtils.PENALTY_HIDETIME + " AND "
                        + DatabaseHandler.KEY_TYPE + "=?" + " AND "
                        + DatabaseHandler.KEY_SUBTYPE + "=?" + " AND "
                        + DatabaseHandler.KEY_COMMENT + " LIKE ?" + " AND "
                        + DatabaseHandler.KEY_HISTORY + "=" + (history ? 1 : 0),
                new String[]{puzzleType, puzzleSubtype, "%" + comment + "%"}, null, null,
                DatabaseHandler.KEY_DATE + " DESC", null);
    }
}
