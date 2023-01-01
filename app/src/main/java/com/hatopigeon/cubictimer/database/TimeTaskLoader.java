package com.hatopigeon.cubictimer.database;

import android.database.Cursor;

import androidx.loader.content.CursorLoader;

import com.hatopigeon.cubictimer.CubicTimer;
import com.hatopigeon.cubictimer.utils.PuzzleUtils;

public class TimeTaskLoader extends CursorLoader {

    public static final String DIR_DESC = "DESC";
    public static final String DIR_ASC = "ASC";

    private String puzzleType;
    private String puzzleSubtype;
    private String comment;
    private String orderByKey;
    private String orderByDir;

    boolean history = false;

    public TimeTaskLoader(String puzzleType, String puzzleSubtype, boolean history, String comment, String orderByKey, String orderByDir) {
        super(CubicTimer.getAppContext());
        this.puzzleType = puzzleType;
        this.puzzleSubtype = puzzleSubtype;
        this.history = history;
        this.comment = comment;
        this.orderByKey = orderByKey;
        this.orderByDir = orderByDir;
    }

    @Override
    public Cursor loadInBackground() {
        return CubicTimer.getReadableDB().query(DatabaseHandler.TABLE_TIMES, null,
                DatabaseHandler.KEY_PENALTY + "!=" + PuzzleUtils.PENALTY_HIDETIME + " AND "
                        + DatabaseHandler.KEY_TYPE + "=?" + " AND "
                        + DatabaseHandler.KEY_SUBTYPE + "=?" + " AND "
                        + DatabaseHandler.KEY_COMMENT + " LIKE ?" + " AND "
                        + DatabaseHandler.KEY_HISTORY + "=" + (history ? 1 : 0),
                new String[]{puzzleType, puzzleSubtype, "%" + comment + "%"}, null, null,
                orderByKey + " " + orderByDir, null);
    }
}
