package com.aricneto.twistytimer.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.items.Algorithm;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.stats.ChartStatistics;
import com.aricneto.twistytimer.stats.Statistics;
import com.aricneto.twistytimer.utils.AlgUtils;
import com.aricneto.twistytimer.utils.Prefs;
import com.aricneto.twistytimer.utils.PuzzleUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Ari on 03/06/2015.
 */
public class DatabaseHandler extends SQLiteOpenHelper {

    public static final String TABLE_TIMES = "times";

    // Times table
    public static final String KEY_ID       = "_id";
    public static final String KEY_TYPE     = "type";
    public static final String KEY_SUBTYPE  = "subtype";
    public static final String KEY_TIME     = "time";
    public static final String KEY_DATE     = "date";
    public static final String KEY_SCRAMBLE = "scramble";
    public static final String KEY_PENALTY  = "penalty";
    public static final String KEY_COMMENT  = "comment";
    public static final String KEY_HISTORY  = "history";

    // Index value of the keys of the "times" table *only* for a full "SELECT * FROM times".
    // Added these to make code in places like "MainActivity" (export/import) a bit more readable,
    // as it was using "magic numbers". However, it would be better if such ad hoc reads were moved
    // back into this class.
    public static final int IDX_ID       = 0;
    public static final int IDX_TYPE     = 1;
    public static final int IDX_SUBTYPE  = 2;
    public static final int IDX_TIME     = 3;
    public static final int IDX_DATE     = 4;
    public static final int IDX_SCRAMBLE = 5;
    public static final int IDX_PENALTY  = 6;
    public static final int IDX_COMMENT  = 7;
    public static final int IDX_HISTORY  = 8;

    // Algs table
    public static final String TABLE_ALGS   = "algs";
    public static final String KEY_SUBSET   = "subset";
    public static final String KEY_NAME     = "name";
    public static final String KEY_STATE    = "state";
    public static final String KEY_ALGS     = "algs";
    public static final String KEY_PROGRESS = "progress";

    public static final String SUBSET_OLL = "OLL";
    public static final String SUBSET_PLL = "PLL";

    private static final String RED                = "R";
    private static final String GRE                = "G";
    private static final String BLU                = "B";
    private static final String ORA                = "O";
    private static final String WHI                = "W";
    private static final String YEL                = "Y";
    private static final String NUL                = "N";
    // Database Version
    private static final int    DATABASE_VERSION   = 9;
    // Database Name
    private static final String DATABASE_NAME      = "databaseManager";
    private static final String CREATE_TABLE_TIMES =
        "CREATE TABLE " + TABLE_TIMES + "("
            + KEY_ID + " INTEGER PRIMARY KEY,"
            + KEY_TYPE + " TEXT,"
            + KEY_SUBTYPE + " TEXT,"
            + KEY_TIME + " INTEGER,"
            + KEY_DATE + " INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),"
            + KEY_SCRAMBLE + " TEXT,"
            + KEY_PENALTY + " INTEGER,"
            + KEY_COMMENT + " TEXT,"
            + KEY_HISTORY + " BOOLEAN"
            + ")";
    private static final String CREATE_TABLE_ALGS  =
        "CREATE TABLE " + TABLE_ALGS + "("
            + KEY_ID + " INTEGER PRIMARY KEY,"
            + KEY_SUBSET + " TEXT,"
            + KEY_NAME + " TEXT,"
            + KEY_STATE + " TEXT,"
            + KEY_ALGS + " TEXT,"
            + KEY_PROGRESS + " INTEGER"
            + ")";

    /**
     * An interface for notification of the progress of bulk database operations.
     */
    public interface ProgressListener {
        /**
         * Notifies the listener of the progress of a bulk operation. This may be called many
         * times during the operation.
         *
         * @param numCompleted
         *     The number of sub-operations of the bulk operation that have been completed.
         * @param total
         *     The total number of sub-operations that must be completed before the the bulk
         *     operation is complete.
         */
        void onProgress(int numCompleted, int total);
    }

    public DatabaseHandler() {
        super(TwistyTimer.getAppContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_TIMES);
        db.execSQL(CREATE_TABLE_ALGS);
        createInitialAlgs(db);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older tables if existed
        Log.d("Database upgrade", "Upgrading from"
            + Integer.toString(oldVersion) + " to " + Integer.toString(newVersion));
        switch (oldVersion) {
            case 6:
                db.execSQL("ALTER TABLE times ADD COLUMN " + KEY_HISTORY + " BOOLEAN DEFAULT 0");
                // Fall through to the next upgrade step.
            case 8:
                Prefs.edit()
                        .putInt(R.string.pk_timer_text_size,
                                Prefs.getInt(R.string.pk_timer_text_size, 10) * 10)
                        .apply();
        }
    }

    private void createAlg(SQLiteDatabase db, String subset, String name, String state, String algs) {
        ContentValues values = new ContentValues();
        values.put(KEY_SUBSET, subset);
        values.put(KEY_NAME, name);
        values.put(KEY_STATE, state);
        values.put(KEY_ALGS, algs);
        values.put(KEY_PROGRESS, 0);
        db.insert(TABLE_ALGS, null, values);
    }

    /**
     * Loads an algorithm from the database for the given algorithm ID.
     *
     * @param algID
     *     The ID of the algorithm to be loaded.
     *
     * @return
     *     An {@link Algorithm} object created from the details loaded from the database for the
     *     algorithm record matching the given ID, or {@code null} if no algorithm matching the
     *     given ID was found.
     */
    public Algorithm getAlgorithm(long algID) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_ALGS,
                new String[] { KEY_ID, KEY_SUBSET, KEY_NAME, KEY_STATE, KEY_ALGS, KEY_PROGRESS },
                KEY_ID + "=?", new String[] { String.valueOf(algID) }, null, null, null, null);

        try {
            if (cursor.moveToFirst()) {
                return new Algorithm(
                        cursor.getLong(0),   // id
                        cursor.getString(1), // subset
                        cursor.getString(2), // name
                        cursor.getString(3), // state
                        cursor.getString(4), // algs
                        cursor.getInt(5));   // progress
            }

            // No algorithm matched the given ID.
            return null;
        } finally {
            cursor.close();
        }
    }

    public int updateAlgorithmAlg(long id, String alg) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_ALGS, alg);

        // Updating row
        return db.update(TABLE_ALGS, values, KEY_ID + " = ?",
            new String[] { String.valueOf(id) });
    }

    public int updateAlgorithmProgress(long id, int progress) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_PROGRESS, progress);

        // Updating row
        return db.update(TABLE_ALGS, values, KEY_ID + " = ?",
            new String[] { String.valueOf(id) });
    }

    /**
     * Returns all solves from puzzle and category
     * @param type
     * @param subtype
     * @return
     */
    public Cursor getAllSolvesFrom(String type, String subtype) {
        SQLiteDatabase db = this.getReadableDatabase();

        String sqlSelection;
        sqlSelection =
            " WHERE type =? AND subtype =? AND penalty!=" + PuzzleUtils.PENALTY_HIDETIME;

        return db.rawQuery("SELECT * FROM times" + sqlSelection, new String[] { type, subtype });
    }

    /**
     * Moves all current solves from puzzle and category to history
     *
     * @param type
     * @param subtype
     *
     * @return
     */
    public int moveAllSolvesToHistory(String type, String subtype) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_HISTORY, true);

        // Updating row
        return db.update(TABLE_TIMES, values, KEY_TYPE + " = ? AND " + KEY_SUBTYPE + " =?",
            new String[] { type, subtype });
    }

    /**
     * Adds a new solve to the database.
     *
     * @param solve The solve to be added to the database.
     * @return The new ID of the stored solve record.
     */
    public long addSolve(Solve solve) {
        return addSolveInternal(getWritableDatabase(), solve);
    }

    /**
     * Adds a new solve to the given database.
     *
     * @param db    The database to which to add the solve.
     * @param solve The solve to be added to the database.
     * @return The new ID of the stored solve record.
     */
    private long addSolveInternal(SQLiteDatabase db, Solve solve) {
        // Cutting off last digit to fix rounding errors
        int time = solve.getTime();
        time = time - (time % 10);

        ContentValues values = new ContentValues();

        values.put(KEY_TYPE, solve.getPuzzle());
        values.put(KEY_SUBTYPE, solve.getSubtype());
        values.put(KEY_TIME, time);
        values.put(KEY_DATE, solve.getDate());
        values.put(KEY_SCRAMBLE, solve.getScramble());
        values.put(KEY_PENALTY, solve.getPenalty());
        values.put(KEY_COMMENT, solve.getComment());
        values.put(KEY_HISTORY, solve.isHistory());

        // Inserting Row
        return db.insert(TABLE_TIMES, null, values);
    }

    /**
     * Adds a collection of new solves to the given database. The solves are added in a single
     * transaction, so this operation is much faster than adding them one-by-one using the
     * {@link #addSolve(Solve)} method. Any given solve that matches a solve already in the
     * database will not be inserted.
     *
     * @param solves
     *     The collection of solves to be added to the database. Must not be {@code null}, but may
     *     be empty.
     * @param listener
     *     An optional progress listener that will be notified as each new solve is inserted into
     *     the database. Before the first new solve is added, this will be called to report that
     *     zero of the total number of solves have been inserted (even if {@code solves} is empty).
     *     Thereafter, it will be notified after each insertion. May be {@code null} if no progress
     *     updates are required.
     *
     * @return
     *     The number of unique solves inserted. Solves that are duplicates of existing solves
     *     (by {@link #solveExists(Solve)}) are not inserted.
     */
    public int addSolves(Collection<Solve> solves, ProgressListener listener) {
        final int total = solves.size();
        int numProcessed = 0; // Whether inserted or not (i.e., includes duplicates).

        if (listener != null) {
            listener.onProgress(numProcessed, total);
        }

        int numInserted = 0; // Only those actually inserted (i.e., excludes duplicates).

        if (total > 0) {
            final SQLiteDatabase db = getWritableDatabase();

            try{
                // Wrapping the insertions in a transaction is about 50x faster!
                db.beginTransaction();

                for (Solve solve : solves) {
                    if (!solveExists(solve)) {
                        addSolveInternal(db, solve);
                        numInserted++;
                    }

                    if (listener != null) {
                        listener.onProgress(++numProcessed, total);
                    }
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        return numInserted;
    }

    public int updateSolve(Solve solve) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_TYPE, solve.getPuzzle());
        values.put(KEY_SUBTYPE, solve.getSubtype());
        values.put(KEY_TIME, solve.getTime());
        values.put(KEY_DATE, solve.getDate());
        values.put(KEY_SCRAMBLE, solve.getScramble());
        values.put(KEY_PENALTY, solve.getPenalty());
        values.put(KEY_COMMENT, solve.getComment());
        values.put(KEY_HISTORY, solve.isHistory());

        // Updating row
        return db.update(TABLE_TIMES, values, KEY_ID + " = ?",
            new String[] { String.valueOf(solve.getId()) });
    }

    /**
     * Loads a solve from the database for the given solve ID.
     *
     * @param solveID
     *     The ID of the solve to be loaded.
     *
     * @return
     *     A {@link Solve} object created from the details loaded from the database for the solve
     *     time matching the given ID, or {@code null} if no solve time matching the given ID was
     *     found.
     */
    public Solve getSolve(long solveID) {
        final Cursor cursor = getReadableDatabase().query(TABLE_TIMES,
                new String[] {
                        KEY_ID, KEY_TIME, KEY_TYPE, KEY_SUBTYPE, KEY_DATE, KEY_SCRAMBLE,
                        KEY_PENALTY, KEY_COMMENT, KEY_HISTORY },
                KEY_ID + "=?", new String[] { String.valueOf(solveID) }, null, null, null, null);

        try {
            if (cursor.moveToFirst()) {
                return new Solve(
                        cursor.getInt(0),
                        cursor.getInt(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getLong(4),
                        cursor.getString(5),
                        cursor.getInt(6),
                        cursor.getString(7),
                        getBoolean(cursor, 8));
            }

            // No solve matched the given ID.
            return null;
        } finally {
            cursor.close();
        }
    }

    public boolean getBoolean(Cursor cursor, int columnIndex) {
        return ! (cursor.isNull(columnIndex) || cursor.getShort(columnIndex) == 0);
    }

    public List<String> getAllSubtypesFromType(String type) {
        List<String> subtypesList = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT DISTINCT " + KEY_SUBTYPE + " FROM "
            + TABLE_TIMES + " WHERE " + KEY_TYPE + " ='" + type + "' ORDER BY " + KEY_SUBTYPE + " ASC", null);

        if (cursor.moveToFirst()) {
            do {
                subtypesList.add(cursor.getString(cursor.getColumnIndex(KEY_SUBTYPE)));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return subtypesList;
    }

    /**
     * Populates the collection of statistics (average calculators) with the solve times recorded
     * in the database. The statistics will manage the segregation of solves for the current session
     * only from those from all past and current sessions. If all average calculators are for the
     * current session only, only the times for the current session will be read from the database.
     *
     * @param puzzleType
     *     The name of the puzzle type.
     * @param puzzleSubtype
     *     The name of the puzzle subtype.
     * @param statistics
     *     The statistics in which to record the solve times. This may contain any mix of average
     *     calculators for all sessions or only the current session. The database read will be
     *     adapted automatically to read the minimum number of rows to satisfy the collection of
     *     the required statistics.
     */
    public void populateStatistics(
            String puzzleType, String puzzleSubtype, Statistics statistics) {
        final boolean isStatisticsForCurrentSessionOnly = statistics.isForCurrentSessionOnly();
        final String sql;

        // Sort into ascending order of date (oldest solves first), so that the "current"
        // average is, in the end, calculated to be that of the most recent solves.
        if (isStatisticsForCurrentSessionOnly) {
            sql = "SELECT " + KEY_TIME + ", " + KEY_PENALTY + " FROM " + TABLE_TIMES
                    + " WHERE " + KEY_TYPE + "=? AND " + KEY_SUBTYPE + "=? AND "
                    + KEY_PENALTY + "!=" + PuzzleUtils.PENALTY_HIDETIME + " AND "
                    + KEY_HISTORY + "=0 ORDER BY " + KEY_DATE + " ASC";
        } else {
            sql = "SELECT " + KEY_TIME + ", " + KEY_PENALTY + ", " + KEY_HISTORY
                    + " FROM " + TABLE_TIMES + " WHERE " + KEY_TYPE + "=? AND "
                    + KEY_SUBTYPE + "=? AND " + KEY_PENALTY + "!=" + PuzzleUtils.PENALTY_HIDETIME
                    + " ORDER BY " + KEY_DATE + " ASC";
        }

        final Cursor cursor
                = getReadableDatabase().rawQuery(sql, new String[] { puzzleType, puzzleSubtype });

        try {
            final int timeCol = cursor.getColumnIndex(KEY_TIME);
            final int penaltyCol = cursor.getColumnIndex(KEY_PENALTY);
            final int historyCol
                    = isStatisticsForCurrentSessionOnly ? -1 : cursor.getColumnIndex(KEY_HISTORY);

            while (cursor.moveToNext()) {
                final boolean isForCurrentSession
                        = isStatisticsForCurrentSessionOnly || cursor.getInt(historyCol) == 0;

                if (cursor.getInt(penaltyCol) == PuzzleUtils.PENALTY_DNF) {
                    statistics.addDNF(isForCurrentSession);
                } else {
                    statistics.addTime(cursor.getLong(timeCol), isForCurrentSession);
                }
            }
        } finally {
            // As elsewhere in this class, assume "cursor" is not null.
            cursor.close();
        }
    }

    /**
     * Populates the chart statistics with the solve times recorded in the database. If all
     * statistics are for the current session only, only the times for the current session will be
     * read from the database.
     *
     * @param puzzleType
     *     The name of the puzzle type.
     * @param puzzleSubtype
     *     The name of the puzzle subtype.
     * @param statistics
     *     The chart statistics in which to record the solve times. This may require solve times for
     *     all sessions or only the current session. The database read will be adapted automatically
     *     to read the minimum number of rows to satisfy the collection of the required statistics.
     */
    public void populateChartStatistics(
            String puzzleType, String puzzleSubtype, ChartStatistics statistics) {
        final boolean isStatisticsForCurrentSessionOnly = statistics.isForCurrentSessionOnly();
        final String sql;

        // Sort into ascending order of date (oldest solves first), so that the "current"
        // average is, in the end, calculated to be that of the most recent solves.
        if (isStatisticsForCurrentSessionOnly) {
            sql = "SELECT " + KEY_TIME + ", " + KEY_PENALTY + ", " + KEY_DATE
                    + " FROM " + TABLE_TIMES + " WHERE " + KEY_TYPE + "=? AND "
                    + KEY_SUBTYPE + "=? AND " + KEY_PENALTY + "!=" + PuzzleUtils.PENALTY_HIDETIME
                    + " AND " + KEY_HISTORY + "=0 ORDER BY " + KEY_DATE + " ASC";
        } else {
            // NOTE: A change from the old approach: the "all time" option include those from the
            // current session, too. This is consistent with the way "all time statistics" are
            // calculated for the table of statistics.
            sql = "SELECT " + KEY_TIME + ", " + KEY_PENALTY + ", " + KEY_DATE
                    + " FROM " + TABLE_TIMES + " WHERE " + KEY_TYPE + "=? AND "
                    + KEY_SUBTYPE + "=? AND " + KEY_PENALTY + "!=" + PuzzleUtils.PENALTY_HIDETIME
                    + " ORDER BY " + KEY_DATE + " ASC";
        }

        final Cursor cursor
                = getReadableDatabase().rawQuery(sql, new String[] { puzzleType, puzzleSubtype });

        try {
            final int timeCol = cursor.getColumnIndex(KEY_TIME);
            final int penaltyCol = cursor.getColumnIndex(KEY_PENALTY);
            final int dateCol = cursor.getColumnIndex(KEY_DATE);

            while (cursor.moveToNext()) {
                if (cursor.getInt(penaltyCol) == PuzzleUtils.PENALTY_DNF) {
                    statistics.addDNF(cursor.getLong(dateCol));
                } else {
                    statistics.addTime(cursor.getLong(timeCol), cursor.getLong(dateCol));
                }
            }
        } finally {
            // As elsewhere in this class, assume "cursor" is not null.
            cursor.close();
        }
    }

    /**
     * Deletes a single solve matching the given ID from the database.
     *
     * @param solveID
     *     The ID of the solve record in the "times" table of the database.
     *
     * @return
     *     The number of records deleted. If no record matches {@code solveID}, the result is zero.
     */
    public int deleteSolveByID(long solveID) {
        return deleteSolveByIDInternal(getWritableDatabase(), solveID);
    }

    /**
     * Deletes a single solve from the database.
     *
     * @param solve
     *     The solve to be deleted. The corresponding database record to be deleted from the "times"
     *     table is matched using the ID returned from {@link Solve#getId()}.
     *
     * @return
     *     The number of records deleted. If no record matches the ID of the solve, the result is
     *     zero.
     */
    public int deleteSolve(Solve solve) {
        return deleteSolveByIDInternal(getWritableDatabase(), solve.getId());
    }

    /**
     * Deletes multiple solves from the database that match the solve record IDs in the given
     * collection. The solves are deleted in the context of a single database transaction.
     *
     * @param solveIDs
     *     The IDs of the solve records in the "times" table of the database to be deleted. Must
     *     not be {@code null}, but may be empty.
     * @param listener
     *     An optional progress listener that will be notified as each solve is deleted from the
     *     database. Before the first solve is deleted, this will be called to report that zero of
     *     the total number of solves have been deleted (even if {@code solveIDs} is empty).
     *     Thereafter, it will be notified after each attempted deletion by ID, whether a matching
     *     solve was found or not. May be {@code null} if no progress reports are required.
     *
     * @return
     *     The number of records deleted. If an ID from {@code solveIDs} does not match any record,
     *     or if an ID is a duplicate of an ID that has already been deleted, that ID is ignored,
     *     so the result may be less than the number of solve IDs in the collection.
     */
    public int deleteSolvesByID(Collection<Long> solveIDs, ProgressListener listener) {
        final int total = solveIDs.size();
        int numProcessed = 0; // Whether deleted or not (i.e., includes RNF and duplicates).

        if (listener != null) {
            listener.onProgress(numProcessed, total);
        }

        int numDeleted = 0; // Only those actually deleted (i.e., excludes RNF and duplicates).

        if (total > 0) {
            final SQLiteDatabase db = getWritableDatabase();

            try{
                // Wrap the bulk delete operations in a transaction; it is *much* faster,
                db.beginTransaction();

                for (long id : solveIDs) {
                    // May not change if RNF or if ID is a duplicate and is already deleted.
                    numDeleted += deleteSolveByIDInternal(db, id);

                    if (listener != null) {
                        listener.onProgress(++numProcessed, total);
                    }
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        return numDeleted;
    }

    /**
     * Deletes a single solve matching the given ID from the database.
     *
     * @param db
     *     The database from which to delete the solve.
     * @param solveID
     *     The ID of the solve record in the "times" table of the database.
     *
     * @return
     *     The number of records deleted. If no record matches {@code solveID}, the result is zero.
     */
    private int deleteSolveByIDInternal(SQLiteDatabase db, long solveID) {
        return db.delete(TABLE_TIMES, KEY_ID + "=?", new String[] { Long.toString(solveID) });
    }

    // Delete entries from session
    public int deleteAllFromSession(String type, String subtype) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_TIMES, KEY_TYPE + "=? AND " + KEY_SUBTYPE + " = ? AND " + KEY_HISTORY + "=0", new String[] { type, subtype });
    }

    /**
     * Deletes all solves from a subtype, thus removing the subtype
     *
     * @param subtype
     */
    public int deleteSubtype(String type, String subtype) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_TIMES, KEY_TYPE + "=? AND " + KEY_SUBTYPE + " = ?",
            new String[] { type, subtype });
    }

    /**
     * Renames a subtype
     *
     * @param subtype
     */
    public int renameSubtype(String type, String subtype, String newName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_SUBTYPE, newName);
        return db.update(TABLE_TIMES, contentValues, KEY_TYPE + "=? AND " + KEY_SUBTYPE + "=?", new String[] { type, subtype });
    }

    public Cursor getAllSolves() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM times WHERE penalty!=" + PuzzleUtils.PENALTY_HIDETIME, null);
    }

    public boolean solveExists(Solve solve) {
        SQLiteDatabase db = this.getReadableDatabase();

        return DatabaseUtils.queryNumEntries(db, TABLE_TIMES, "type=? AND subtype =? AND time=? AND scramble=? AND date=?", new String[] { solve.getPuzzle(), solve.getSubtype(), String.valueOf(solve.getTime()), solve.getScramble(), String.valueOf(solve.getDate()) }) > 0;
    }

    // TODO: this info should REALLY be in a separate file. I'll get to it when I add other alg sets.

    private void createInitialAlgs(SQLiteDatabase db) {
        // OLL
        createAlg(db, SUBSET_OLL, "OLL 01", "NNNNYNNNNNYNYYYNYNYYY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 01"));
        createAlg(db, SUBSET_OLL, "OLL 02", "NNNNYNNNNNYYNYNYYNYYY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 02"));
        createAlg(db, SUBSET_OLL, "OLL 03", "NNNNYNYNNYYNYYNYYNNYN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 03"));
        createAlg(db, SUBSET_OLL, "OLL 04", "NNNNYNNNYNYYNYNNYYNYY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 04"));
        createAlg(db, SUBSET_OLL, "OLL 05", "NNNNYYNYYYYNYNNNNNYYN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 05"));
        createAlg(db, SUBSET_OLL, "OLL 06", "NYYNYYNNNNNNNNYNYYNYY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 06"));
        createAlg(db, SUBSET_OLL, "OLL 07", "NYNYYNYNNYNNYYNYYNNNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 07"));
        createAlg(db, SUBSET_OLL, "OLL 08", "NYNNYYNNYNNYNNNNYYNYY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 08"));
        createAlg(db, SUBSET_OLL, "OLL 09", "NNYYYNNYNNYNNYYNNYNNY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 09"));
        createAlg(db, SUBSET_OLL, "OLL 10", "NNYYYNNYNYYNNYNYNNYNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 10"));
        createAlg(db, SUBSET_OLL, "OLL 11", "NNNNYYYYNYYNYNNYNNNYN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 11"));
        createAlg(db, SUBSET_OLL, "OLL 12", "NNYNYYNYNNYNNNYNNYNYY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 12"));
        createAlg(db, SUBSET_OLL, "OLL 13", "NNNYYYYNNYYNYNNYYNNNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 13"));
        createAlg(db, SUBSET_OLL, "OLL 14", "NNNYYYNNYNYYNNNNYYNNY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 14"));
        createAlg(db, SUBSET_OLL, "OLL 15", "NNNYYYNNYYYNYNNNYNYNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 15"));
        createAlg(db, SUBSET_OLL, "OLL 16", "NNYYYYNNNNYNNNYNYYNNY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 16"));
        createAlg(db, SUBSET_OLL, "OLL 17", "YNNNYNNNYNYYNYNNYNYYN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 17"));
        createAlg(db, SUBSET_OLL, "OLL 18", "YNYNYNNNNNYNNYNYYYNYN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 18"));
        createAlg(db, SUBSET_OLL, "OLL 19", "YNYNYNNNNNYNNYYNYNYYN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 19"));
        createAlg(db, SUBSET_OLL, "OLL 20", "YNYNYNYNYNYNNYNNYNNYN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 20"));
        createAlg(db, SUBSET_OLL, "OLL 21", "NYNYYYNYNNNNYNYNNNYNY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 21"));
        createAlg(db, SUBSET_OLL, "OLL 22", "NYNYYYNYNNNYNNNYNNYNY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 22"));
        createAlg(db, SUBSET_OLL, "OLL 23", "YYYYYYNYNNNNNNNYNYNNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 23"));
        createAlg(db, SUBSET_OLL, "OLL 24", "NYYYYYNYYYNNNNNNNYNNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 24"));
        createAlg(db, SUBSET_OLL, "OLL 25", "YYNYYYNYYNNNYNNNNYNNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 25"));
        createAlg(db, SUBSET_OLL, "OLL 26", "YYNYYYNYNNNYNNYNNYNNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 26"));
        createAlg(db, SUBSET_OLL, "OLL 27", "NYNYYYYYNYNNYNNYNNNNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 27"));
        createAlg(db, SUBSET_OLL, "OLL 28", "YYYYYNYNYNNNNYNNYNNNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 28"));
        createAlg(db, SUBSET_OLL, "OLL 29", "YNYYYNNYNNYNNYYNNNYNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 29"));
        createAlg(db, SUBSET_OLL, "OLL 30", "YNYNYYNYNNYNNNYNNNYYN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 30"));
        createAlg(db, SUBSET_OLL, "OLL 31", "NYYNYYNNYYNNNNNNYYNYN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 31"));
        createAlg(db, SUBSET_OLL, "OLL 32", "NNYNYYNYYYYNNNNNNYNYN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 32"));
        createAlg(db, SUBSET_OLL, "OLL 33", "NNYYYYNNYYYNNNNNYYNNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 33"));
        createAlg(db, SUBSET_OLL, "OLL 34", "YNYYYYNNNNYNNNYNYNYNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 34"));
        createAlg(db, SUBSET_OLL, "OLL 35", "YNNNYYNYYNYNYNNNNYNYN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 35"));
        createAlg(db, SUBSET_OLL, "OLL 36", "YNNYYNNYYNYNYYNNNYNNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 36"));
        createAlg(db, SUBSET_OLL, "OLL 37", "YYNYYNNNYNNNYYNNYYNNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 37"));
        createAlg(db, SUBSET_OLL, "OLL 38", "NYYYYNYNNYNNNYYNYNNNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 38"));
        createAlg(db, SUBSET_OLL, "OLL 39", "YYNNYNNYYNNYNYNNNNYYN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 39"));
        createAlg(db, SUBSET_OLL, "OLL 40", "NYYNYNYYNNNNNYNYNNNYY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 40"));
        createAlg(db, SUBSET_OLL, "OLL 41", "YNYNYYNYNNYNNNNYNYNYN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 41"));
        createAlg(db, SUBSET_OLL, "OLL 42", "YNYYYNNYNNYNNYNYNYNNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 42"));
        createAlg(db, SUBSET_OLL, "OLL 43", "YNNYYNYYNNYNYYYNNNNNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 43"));
        createAlg(db, SUBSET_OLL, "OLL 44", "NNYNYYNYYNYNNNNNNNYYY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 44"));
        createAlg(db, SUBSET_OLL, "OLL 45", "NNYYYYNNYNYNNNNNYNYNY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 45"));
        createAlg(db, SUBSET_OLL, "OLL 46", "YYNNYNYYNNNNYYYNNNNYN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 46"));
        createAlg(db, SUBSET_OLL, "OLL 47", "NYNNYYNNNYNNYNYNYYNYN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 47"));
        createAlg(db, SUBSET_OLL, "OLL 48", "NYNYYNNNNNNYNYNYYNYNY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 48"));
        createAlg(db, SUBSET_OLL, "OLL 49", "NNNYYNNYNYYNYYYNNYNNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 49"));
        createAlg(db, SUBSET_OLL, "OLL 50", "NNNNYYNYNNYYNNNYNNYYY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 50"));
        createAlg(db, SUBSET_OLL, "OLL 51", "NNNYYYNNNNYYNNNYYNYNY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 51"));
        createAlg(db, SUBSET_OLL, "OLL 52", "NYNNYNNYNYNNYYYNNYNYN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 52"));
        createAlg(db, SUBSET_OLL, "OLL 53", "NNNNYYNYNNYNYNYNNNYYY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 53"));
        createAlg(db, SUBSET_OLL, "OLL 54", "NYNNYYNNNNNNYNYNYNYYY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 54"));
        createAlg(db, SUBSET_OLL, "OLL 55", "NYNNYNNYNNNNYYYNNNYYY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 55"));
        createAlg(db, SUBSET_OLL, "OLL 56", "NNNYYYNNNNYNYNYNYNYNY", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 56"));
        createAlg(db, SUBSET_OLL, "OLL 57", "YNYYYYYNYNYNNNNNYNNNN", AlgUtils.getDefaultAlgs(SUBSET_OLL, "OLL 57"));

        // PLL
        createAlg(db, SUBSET_PLL, "H", "YYYYYYYYYOROGBGRORBGB", AlgUtils.getDefaultAlgs(SUBSET_PLL, "H"));
        createAlg(db, SUBSET_PLL, "Ua", "YYYYYYYYYOBOGOGRRRBGB", AlgUtils.getDefaultAlgs(SUBSET_PLL, "Ua"));
        createAlg(db, SUBSET_PLL, "Ub", "YYYYYYYYYOGOGBGRRRBOB", AlgUtils.getDefaultAlgs(SUBSET_PLL, "Ub"));
        createAlg(db, SUBSET_PLL, "Z", "YYYYYYYYYOBOGRGRGRBOB", AlgUtils.getDefaultAlgs(SUBSET_PLL, "Z"));
        createAlg(db, SUBSET_PLL, "Aa", "YYYYYYYYYGOGRGBORRBBO", AlgUtils.getDefaultAlgs(SUBSET_PLL, "Aa"));
        createAlg(db, SUBSET_PLL, "Ab", "YYYYYYYYYOORBGOGRGRBB", AlgUtils.getDefaultAlgs(SUBSET_PLL, "Ab"));
        createAlg(db, SUBSET_PLL, "E", "YYYYYYYYYGOBOGRBRGRBO", AlgUtils.getDefaultAlgs(SUBSET_PLL, "E"));
        createAlg(db, SUBSET_PLL, "F", "YYYYYYYYYGOBOBGRRRBGO", AlgUtils.getDefaultAlgs(SUBSET_PLL, "F"));
        createAlg(db, SUBSET_PLL, "Ga", "YYYYYYYYYRBOGGRBOBORG", AlgUtils.getDefaultAlgs(SUBSET_PLL, "Ga"));
        createAlg(db, SUBSET_PLL, "Gb", "YYYYYYYYYBROGGBOBGROR", AlgUtils.getDefaultAlgs(SUBSET_PLL, "Gb"));
        createAlg(db, SUBSET_PLL, "Gc", "YYYYYYYYYOGRBROGOGRBB", AlgUtils.getDefaultAlgs(SUBSET_PLL, "Gc"));
        createAlg(db, SUBSET_PLL, "Gd", "YYYYYYYYYORGRORBGOGBB", AlgUtils.getDefaultAlgs(SUBSET_PLL, "Gd"));
        createAlg(db, SUBSET_PLL, "Ja", "YYYYYYYYYBOOGGGRBBORR", AlgUtils.getDefaultAlgs(SUBSET_PLL, "Ja"));
        createAlg(db, SUBSET_PLL, "Jb", "YYYYYYYYYOOGRROGGRBBB", AlgUtils.getDefaultAlgs(SUBSET_PLL, "Jb"));
        createAlg(db, SUBSET_PLL, "Na", "YYYYYYYYYOORBBGRROGGB", AlgUtils.getDefaultAlgs(SUBSET_PLL, "Na"));
        createAlg(db, SUBSET_PLL, "Nb", "YYYYYYYYYROOGBBORRBGG", AlgUtils.getDefaultAlgs(SUBSET_PLL, "Nb"));
        createAlg(db, SUBSET_PLL, "Ra", "YYYYYYYYYOGOGORBRGRBB", AlgUtils.getDefaultAlgs(SUBSET_PLL, "Ra"));
        createAlg(db, SUBSET_PLL, "Rb", "YYYYYYYYYGOBORGRGRBBO", AlgUtils.getDefaultAlgs(SUBSET_PLL, "Rb"));
        createAlg(db, SUBSET_PLL, "T", "YYYYYYYYYOOGRBOGRRBGB", AlgUtils.getDefaultAlgs(SUBSET_PLL, "T"));
        createAlg(db, SUBSET_PLL, "V", "YYYYYYYYYRGOGOBORRBBG", AlgUtils.getDefaultAlgs(SUBSET_PLL, "V"));
        createAlg(db, SUBSET_PLL, "Y", "YYYYYYYYYRBOGGBORRBOG", AlgUtils.getDefaultAlgs(SUBSET_PLL, "Y"));
    }
}
