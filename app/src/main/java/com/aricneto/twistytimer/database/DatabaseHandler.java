package com.aricneto.twistytimer.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;
import android.util.Log;

import com.aricneto.twistytimer.items.Algorithm;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.utils.AlgUtils;
import com.aricneto.twistytimer.utils.PuzzleUtils;

import java.util.ArrayList;
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
    private Context mContext;


    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
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
            case 8:
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("timerTextSize", sharedPreferences.getInt("timerTextSize", 10) * 10);
                editor.apply();
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

    public Algorithm getAlgorithm(long id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_ALGS, new String[] { KEY_ID, KEY_SUBSET, KEY_NAME, KEY_STATE, KEY_ALGS, KEY_PROGRESS }, KEY_ID + "=?",
            new String[] { String.valueOf(id) }, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();

        Algorithm algorithm = new Algorithm(
            cursor.getLong(0),  // id
            cursor.getString(1), // subset
            cursor.getString(2), // name
            cursor.getString(3), // state
            cursor.getString(4), // algs
            cursor.getInt(5)); // progress

        // Return alg
        cursor.close();
        return algorithm;
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

    public Cursor getAllSolvesFrom(String type, String subtype, boolean history) {
        SQLiteDatabase db = this.getReadableDatabase();

        String sqlSelection;
        if (! history)
            sqlSelection =
                " WHERE type =? AND subtype =? AND penalty!=10 AND penalty!="
                    + PuzzleUtils.PENALTY_DNF + " AND history = 0 ORDER BY date ASC ";
        else
            sqlSelection =
                " WHERE type =? AND subtype =? AND penalty!=10 AND penalty!="
                    + PuzzleUtils.PENALTY_DNF + " AND history = 1 ORDER BY date ASC ";

        return db.rawQuery("SELECT * FROM times" + sqlSelection, new String[] { type, subtype });
    }

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


    // Adding new solve
    public long addSolve(Solve solve) {
        SQLiteDatabase db = this.getWritableDatabase();

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

    public Solve getSolve(long id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_TIMES, new String[] { KEY_ID, KEY_TIME, KEY_TYPE, KEY_SUBTYPE, KEY_DATE, KEY_SCRAMBLE, KEY_PENALTY, KEY_COMMENT, KEY_HISTORY }, KEY_ID + "=?",
            new String[] { String.valueOf(id) }, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();

        Solve solve = new Solve(
            cursor.getInt(0),
            cursor.getInt(1),
            cursor.getString(2),
            cursor.getString(3),
            cursor.getLong(4),
            cursor.getString(5),
            cursor.getInt(6),
            cursor.getString(7),
            getBoolean(cursor, 8));

        // Return solve
        cursor.close();
        return solve;
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
     * Returns the solve count
     *
     * @return
     */
    public int getSolveCount(String type, String subtype, boolean session) {
        String sqlSelection;
        if (session)
            sqlSelection =
                " WHERE type =? AND subtype =? AND penalty!=10 AND history = 0";
        else
            sqlSelection =
                " WHERE type =? AND subtype =? AND penalty!=10";

        String countQuery = "SELECT * FROM " + TABLE_TIMES + sqlSelection;
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(countQuery, new String[] { type, subtype });

        int count = cursor.getCount();
        cursor.close();
        // Return count
        return count;
    }

    /**
     * Gets the worst time
     *
     * @param best    True if you want the best time, false if worse
     * @param session True if time should be from this session
     * @param puzzle  The puzzle name in database
     * @param subtype The puzzle subtype (category) in database
     *
     * @return The time
     */
    public int getBestOrWorstTime(boolean best, boolean session, String puzzle, String subtype) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor;
        int time = 0;

        String minOrMax;
        if (best)
            minOrMax = "MIN(time)";
        else
            minOrMax = "MAX(time)";

        String sqlSelection;
        if (session)
            sqlSelection =
                " WHERE type =? AND subtype =? AND penalty!=10 AND penalty!="
                    + PuzzleUtils.PENALTY_DNF + " AND history = 0 ORDER BY date DESC ";
        else
            sqlSelection =
                " WHERE type =? AND subtype =? AND penalty!=10 AND penalty!="
                    + PuzzleUtils.PENALTY_DNF;

        try {
            cursor = db.rawQuery("SELECT " + minOrMax + " FROM " + TABLE_TIMES + sqlSelection,
                new String[] { puzzle, subtype });

            if (cursor.moveToFirst())
                time = cursor.getInt(0);
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return time;
    }

    public int getMean(boolean session, String puzzle, String type) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor;
        int mean = 0;

        String sqlSelection;
        if (session)
            sqlSelection =
                " WHERE type =? AND subtype =? AND penalty!=10 AND penalty!="
                    + PuzzleUtils.PENALTY_DNF + " AND history = 0";
        else
            sqlSelection =
                " WHERE type =? AND subtype =? AND penalty!=10 AND penalty!="
                    + PuzzleUtils.PENALTY_DNF;

        cursor = db.rawQuery("SELECT AVG(time) FROM " + TABLE_TIMES + sqlSelection,
            new String[] { puzzle, type });
        cursor.moveToFirst();

        if (cursor.getCount() != 0)
            mean = cursor.getInt(0);

        cursor.close();
        return mean;
    }

    /**
     * Returns a truncated average of n.
     *
     * @param n             The "average of" (5, 12...)
     * @param puzzle        The puzzle name in database
     * @param type          The puzzle type in database
     * @param disqualifyDNF True 2 DNFs disqualify the attempt
     *
     * @return
     */

    public int getTruncatedAverageOf(int n, String puzzle, String type, boolean disqualifyDNF) {
        SQLiteDatabase db = this.getReadableDatabase();

        String sqlSelection =
            " WHERE type =? AND subtype =? AND penalty!=10 AND history = 0 ORDER BY date DESC ";

        Cursor cursor;

        cursor = db.rawQuery("SELECT time, penalty FROM " + TABLE_TIMES + sqlSelection + "LIMIT " + n,
            new String[] { puzzle, type });

        if (cursor.getCount() >= n) {

            int timeIndex = cursor.getColumnIndex(KEY_TIME);
            int penaltyIndex = cursor.getColumnIndex(KEY_PENALTY);

            if (cursor.moveToFirst()) {
                int worst = 0;
                int best = Integer.MAX_VALUE;
                int sum = 0;
                int dnfCount = 0;
                for (int i = 0; i < n; i++) {
                    int time = cursor.getInt(timeIndex); // time
                    sum += time;

                    if (time > worst && dnfCount == 0)
                        worst = time;
                    if (time < best && cursor.getInt(penaltyIndex) != PuzzleUtils.PENALTY_DNF)
                        best = time;

                    if (cursor.getInt(penaltyIndex) == PuzzleUtils.PENALTY_DNF) { // penalty
                        worst = time;
                        dnfCount += 1;
                    }

                    cursor.moveToNext();
                }
                cursor.close();
                if (disqualifyDNF && dnfCount > 1)
                    return - 1;
                else
                    return (sum - worst - best) / (n - 2);
            }
        }
        cursor.close();
        return 0;
    }

    /**
     * Returns an average of n. It's faster than other functions but it isn't a real "best of".
     *
     * @param n       The "average of" (5, 12...)
     * @param puzzle  The puzzle name in database
     * @param type    The puzzle type in database
     * @param session True if it's in session
     *
     * @return
     */

    public int getFastAverageOf(int n, String puzzle, String type, boolean session) {
        SQLiteDatabase db = this.getReadableDatabase();
        int time = 0;

        String sqlSelection;
        if (session)
            sqlSelection =
                " WHERE type =? AND subtype =? AND penalty!=10 AND penalty!="
                    + PuzzleUtils.PENALTY_DNF + " AND history = 0";
        else
            sqlSelection =
                " WHERE type =? AND subtype =? AND penalty!=10 AND penalty!="
                    + PuzzleUtils.PENALTY_DNF;

        Cursor cursor;

        cursor = db.rawQuery("SELECT time FROM " + TABLE_TIMES + sqlSelection + " LIMIT " + n,
            new String[] { puzzle, type });

        if (cursor.getCount() >= n) {
            cursor = db.rawQuery("SELECT AVG(time) FROM (SELECT * FROM " + TABLE_TIMES + sqlSelection + " ORDER BY date DESC LIMIT " + n + ")",
                new String[] { puzzle, type });
            if (cursor.moveToFirst())
                time = cursor.getInt(0);
        }

        cursor.close();
        return time;
    }


    /**
     * Returns a truncated average of n, along with a list containing all times from that average.
     * Best and worst time are marked.
     *
     * @param n             The "average of" (5, 12...)
     * @param puzzle        The puzzle name in database
     * @param type          The puzzle type in database
     * @param disqualifyDNF True 2 DNFs disqualify the attempt
     *
     * @return
     */

    public ArrayList<Integer> getListOfTruncatedAverageOf(int n, String puzzle, String type, boolean disqualifyDNF) {
        SQLiteDatabase db = this.getReadableDatabase();

        ArrayList<Integer> timeList = new ArrayList<>(n + 1);

        String sqlSelection =
            " WHERE type =? AND subtype =? AND penalty!=10 AND history = 0 ORDER BY date DESC ";

        Cursor cursor;

        cursor = db.rawQuery("SELECT time, penalty FROM " + TABLE_TIMES + sqlSelection + "LIMIT " + n,
            new String[] { puzzle, type });

        if (cursor.getCount() >= n) {

            int timeIndex = cursor.getColumnIndex(KEY_TIME);
            int penaltyIndex = cursor.getColumnIndex(KEY_PENALTY);

            if (cursor.moveToFirst()) {
                int worst = 0;
                int best = Integer.MAX_VALUE;
                int sum = 0;
                int dnfCount = 0;
                for (int i = 0; i < n; i++) {
                    int time = cursor.getInt(timeIndex); // time
                    sum += time;

                    if (time > worst && dnfCount == 0)
                        worst = time;
                    if (time < best && cursor.getInt(penaltyIndex) != PuzzleUtils.PENALTY_DNF)
                        best = time;

                    if (cursor.getInt(penaltyIndex) == PuzzleUtils.PENALTY_DNF) { // penalty
                        worst = time;
                        dnfCount += 1;
                    }

                    timeList.add(time);

                    cursor.moveToNext();
                }
                if (disqualifyDNF && dnfCount > 1)
                    timeList.add(- 1);
                else
                    timeList.add((sum - worst - best) / (n - 2));
            }
        }
        cursor.close();
        return timeList;
    }

    /**
     * Returns best average of n.
     *
     * @param n             The "average of" (5, 12...)
     * @param puzzle        The puzzle name in database
     * @param type          The puzzle type in database
     * @param disqualifyDNF True if 2 DNFs disqualify the attempt
     *
     * @return
     */

    public int getBestAverageOf(int n, String puzzle, String type, boolean disqualifyDNF) {
        SQLiteDatabase db = this.getReadableDatabase();

        String sqlSelection =
            " WHERE type =? AND subtype =? AND penalty!=10 ORDER BY date DESC";

        Cursor cursor;

        cursor = db.rawQuery("SELECT time, penalty FROM " + TABLE_TIMES + sqlSelection,
            new String[] { puzzle, type });
        cursor.moveToFirst();

        int bestAverage = Integer.MAX_VALUE;
        int count = cursor.getCount();

        if (count >= n) {
            int timeIndex = cursor.getColumnIndex(KEY_TIME);
            int penaltyIndex = cursor.getColumnIndex(KEY_PENALTY);

            if (cursor.moveToFirst()) {

                for (int i = 0; i < count - n + 1; i++) {
                    int worst = Integer.MIN_VALUE;
                    int best = Integer.MAX_VALUE;
                    int sum = 0;
                    int dnfCount = 0;

                    for (int j = 0; j < n; j++) {
                        cursor.moveToPosition(i + j);
                        int time = cursor.getInt(timeIndex);
                        sum += time;

                        if (time > worst && dnfCount == 0)
                            worst = time;
                        if (time < best && cursor.getInt(penaltyIndex) != PuzzleUtils.PENALTY_DNF)
                            best = time;

                        if (disqualifyDNF) {
                            if (cursor.getInt(penaltyIndex) == PuzzleUtils.PENALTY_DNF) {
                                worst = time;
                                dnfCount += 1;
                            }
                        }

                    }

                    if (! (disqualifyDNF && dnfCount > 1)) {
                        int average = Integer.MAX_VALUE;

                        if (n == 3) {
                            if (dnfCount == 0)
                                average = sum / 3;
                        } else
                            average = (sum - worst - best) / (n - 2);

                        if (average < bestAverage)
                            bestAverage = average;
                    }

                }
                cursor.close();
                if (bestAverage == Integer.MAX_VALUE)
                    return 0;
                else
                    return bestAverage;
            }
        }
        cursor.close();
        return 0;
    }


    // Delete an entry with an id
    public int deleteFromId(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_TIMES, KEY_ID + " = ?", new String[] { String.valueOf(id) });
    }

    // Delete entries with an id list
    public void deleteAllFromList(List<Long> idList) {
        SQLiteDatabase db = this.getWritableDatabase();
        for (int i = 0; i < idList.size(); i++) {
            db.delete(TABLE_TIMES, KEY_ID + " = ?", new String[] { Long.toString(idList.get(i)) });
        }
    }

    // Delete entries from session
    public int deleteAllFromSession(String type, String subtype) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_TIMES, KEY_TYPE + "=? AND " + KEY_SUBTYPE + " = ? AND " + KEY_HISTORY + "=0", new String[] { type, subtype });
    }

    // Delete a single solve
    public int deleteSolve(Solve solve) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_TIMES, KEY_ID + " = ?", new String[] { String.valueOf(solve.getId()) });
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
     * Check if a record exists
     */

    public boolean idExists(long _id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT 1 FROM algs WHERE _id=" + _id, null);
        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        return exists;
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
        createAlg(db, SUBSET_PLL, "Ab", "YYYYYYYYYOORBGOGRGRBB", AlgUtils.getDefaultAlgs(SUBSET_PLL, "Aa"));
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

    public void closeDB() {
        SQLiteDatabase db = this.getReadableDatabase();
        if (db != null && db.isOpen())
            db.close();
    }

}
