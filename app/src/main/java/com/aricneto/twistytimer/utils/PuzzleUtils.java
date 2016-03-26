package com.aricneto.twistytimer.utils;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.annotation.StringRes;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.database.DatabaseHandler;
import com.aricneto.twistytimer.items.Solve;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;

/**
 * Created by Ari on 17/01/2016.
 */
public class PuzzleUtils {
    public static final String TYPE_222     = "222";
    public static final String TYPE_333     = "333";
    public static final String TYPE_444     = "444";
    public static final String TYPE_555     = "555";
    public static final String TYPE_666     = "666";
    public static final String TYPE_777     = "777";
    public static final String TYPE_MEGA    = "mega";
    public static final String TYPE_PYRA    = "pyra";
    public static final String TYPE_SKEWB   = "skewb";
    public static final String TYPE_CLOCK   = "clock";
    public static final String TYPE_SQUARE1 = "sq1";

    public static final int NO_PENALTY       = 0;
    public static final int PENALTY_PLUSTWO  = 1;
    public static final int PENALTY_DNF      = 2;
    // The following penalty is a workaround to implement subtypes in the timer
    // Every time query should ignore every time that has a penalty of 10
    public static final int PENALTY_HIDETIME = 10;

    public static final int TIME_DNF = - 1;

    public PuzzleUtils() {
    }

    public static String getPuzzleInPosition(int position) {
        switch (position) {
            case 0: // 333
                return PuzzleUtils.TYPE_333;
            case 1: // 222
                return PuzzleUtils.TYPE_222;
            case 2: // 444
                return PuzzleUtils.TYPE_444;
            case 3: // 555
                return PuzzleUtils.TYPE_555;
            case 4: // 666
                return PuzzleUtils.TYPE_666;
            case 5: // 777
                return PuzzleUtils.TYPE_777;
            case 6: // Clock
                return PuzzleUtils.TYPE_CLOCK;
            case 7: // Mega
                return PuzzleUtils.TYPE_MEGA;
            case 8: // Pyra
                return PuzzleUtils.TYPE_PYRA;
            case 9: // Skewb
                return PuzzleUtils.TYPE_SKEWB;
            case 10: // Square-1
                return PuzzleUtils.TYPE_SQUARE1;
        }
        return PuzzleUtils.TYPE_333;
    }

    /**
     * Gets the string id of the name of a puzzle
     *
     * @param puzzle
     *
     * @return
     */
    public static
    @StringRes
    int getPuzzleName(String puzzle) {
        switch (puzzle) {
            case PuzzleUtils.TYPE_333: // 333
                return R.string.cube_333_informal;
            case PuzzleUtils.TYPE_222: // 222
                return R.string.cube_222_informal;
            case PuzzleUtils.TYPE_444: // 444
                return R.string.cube_444_informal;
            case PuzzleUtils.TYPE_555: // 555
                return R.string.cube_555_informal;
            case PuzzleUtils.TYPE_666: // 666
                return R.string.cube_666_informal;
            case PuzzleUtils.TYPE_777: // 777
                return R.string.cube_777_informal;
            case PuzzleUtils.TYPE_CLOCK: // Clock
                return R.string.cube_clock;
            case PuzzleUtils.TYPE_MEGA: // Mega
                return R.string.cube_mega;
            case PuzzleUtils.TYPE_PYRA: // Pyra
                return R.string.cube_pyra;
            case PuzzleUtils.TYPE_SKEWB: // Skewb
                return R.string.cube_skewb;
            case PuzzleUtils.TYPE_SQUARE1: // Square-1
                return R.string.cube_sq1;
        }
        return 0;
    }

    public static String convertTimeToString(int time) {

        if (time == TIME_DNF)
            return "DNF";
        if (time == 0)
            return "--";

        // Magic (not-so-magic actually) numbers below
        int hours = time / 3600000; // 3600 * 1000
        int remaining = time % 3600000; // 3600 * 1000
        int minutes = remaining / 60000; // 60 * 1000

        if (hours > 0)
            return new DateTime(time).toString("k':'mm':'ss");

        else if (minutes > 0)
            return new DateTime(time).toString("m':'ss'.'SS");

        else
            return new DateTime(time).toString("s'.'SS");
    }

    public static String convertTimeToStringWithSmallDecimal(int time) {

        if (time == TIME_DNF)
            return "DNF";
        if (time == 0)
            return "--";

        // Magic (not-so-magic actually) numbers below
        int hours = time / 3600000; // 3600 * 1000
        int remaining = time % 3600000; // 3600 * 1000
        int minutes = remaining / 60000; // 60 * 1000

        if (hours > 0)
            return new DateTime(time).toString("k':'mm'<small>:'ss'</small>'");

        else if (minutes > 0)
            return new DateTime(time).toString("m':'ss'<small>.'SS'</small>'");

        else
            return new DateTime(time).toString("s'<small>.'SS'</small>'");
    }

    public static String convertTimeToStringWithoutMilli(int time) {

        if (time == - 1)
            return "DNF";
        if (time == 0)
            return "--";

        // Magic (not-so-magic actually) numbers below
        int hours = time / 3600000; // 3600 * 1000
        int remaining = time % 3600000; // 3600 * 1000
        int minutes = remaining / 60000; // 60 * 1000

        if (hours > 0)
            return new DateTime(time).toString("kk':'mm':'ss");

        else if (minutes > 0)
            return new DateTime(time).toString("mm':'ss");

        else
            return new DateTime(time).toString("s");
    }


    public static String convertPenaltyToString(int penalty) {
        switch (penalty) {
            case NO_PENALTY:
                return "--";
            case PENALTY_PLUSTWO:
                return "+2";
            case PENALTY_DNF:
                return "DNF";
        }
        return "";
    }


    /**
     * Converts times such as 00:00.00 into int for storage
     * Code shamelessly stolen from Prisma Puzzle Timer (love you).
     *
     * @param input
     *
     * @return time in millis
     */
    public static int parseTime(String input) {
        Scanner scanner = new Scanner(input.trim());
        scanner.useLocale(Locale.ENGLISH);

        int time;

        // 00:00.00
        if (input.contains(":")) {
            scanner.useDelimiter(":");

            if (! scanner.hasNextLong()) {
                return 0;
            }

            long minutes = scanner.nextLong();
            if (minutes < 0) {
                return 0;
            }

            if (! scanner.hasNextDouble()) {
                return 0;
            }

            double seconds = scanner.nextDouble();
            if (seconds < 0.0 || seconds >= 60.0) {
                return 0;
            }

            time = (int) (60000 * minutes + 1000 * seconds);
        }

        // 00.00
        else {
            if (! scanner.hasNextDouble()) {
                return 0;
            }

            double seconds = scanner.nextDouble();
            if (seconds < 0.0) {
                return 0;
            }

            time = (int) (1000 * seconds);
        }

        return 10 * ((time + 5) / 10);
    }


    /**
     * This function applies a penalty to a solve
     *
     * @param solve   A {@link Solve}
     * @param penalty The penalty (refer to static constants on top)
     *
     * @return The solve with the penalty applied
     */
    public static Solve applyPenalty(Solve solve, int penalty) {
        switch (penalty) {
            case PENALTY_DNF:
                if (solve.getPenalty() == PENALTY_PLUSTWO)
                    solve.setTime(solve.getTime() - 2000);
                solve.setPenalty(PENALTY_DNF);
                break;
            case PENALTY_PLUSTWO:
                if (solve.getPenalty() != PENALTY_PLUSTWO)
                    solve.setTime(solve.getTime() + 2000);
                solve.setPenalty(PENALTY_PLUSTWO);
                break;
            case NO_PENALTY:
                if (solve.getPenalty() == PENALTY_PLUSTWO)
                    solve.setTime(solve.getTime() - 2000);
                solve.setPenalty(NO_PENALTY);
                break;
        }
        return solve;
    }

    /**
     * Returns the nearest even int
     *
     * @param num
     * @param roundUp True for rounding up, false for rounding douwn
     *
     * @return
     */

    public static int nearestEvenInt(int num, boolean roundUp) {
        if (num % 2 == 0) {
            return num;
        } else {
            if (roundUp)
                return num + 1;
            else
                return num - 1;
        }
    }


    /**
     * Creates a list of averages from a number.
     * Useful for sharing
     *
     * @param currentPuzzle
     * @param currentPuzzleSubtype
     * @param dbHandler
     * @param n
     *
     * @return the list
     */
    private static String createAverageList(int n, String currentPuzzle, String currentPuzzleSubtype, DatabaseHandler dbHandler) {
        int average;

        ArrayList<Integer> aoList = dbHandler.getListOfTruncatedAverageOf(n, currentPuzzle, currentPuzzleSubtype, true);
        average = aoList.get(n);
        aoList.remove(n);

        int max = Collections.max(aoList);
        int min = Collections.min(aoList);

        // So the last solves come first
        Collections.reverse(aoList);

        StringBuilder aoStringList = new StringBuilder(convertTimeToString(average) + " = ");

        boolean markedMax = false;
        boolean markedMin = false;

        for (int time : aoList) {
            if (time == max && ! markedMax) {
                aoStringList.append("(");
                aoStringList.append(convertTimeToString(time));
                aoStringList.append("), ");
                markedMax = true;
            } else if (time == min && ! markedMin) {
                aoStringList.append("(");
                aoStringList.append(convertTimeToString(time));
                aoStringList.append("), ");
                markedMin = true;
            } else {
                aoStringList.append(convertTimeToString(time));
                aoStringList.append(", ");
            }
        }

        // The substring is there to remove the last ", "
        return aoStringList.substring(0, aoStringList.length() - 2);
    }

    /**
     * Shares an average of n
     *
     * @param n
     * @param currentPuzzle
     * @param currentPuzzleSubtype
     * @param dbHandler
     * @param context
     *
     * @return True if it's possible to share
     */
    public static boolean shareAverageOf(int n, String currentPuzzle, String currentPuzzleSubtype, DatabaseHandler dbHandler, Context context) {
        if (dbHandler.getSolveCount(currentPuzzle, currentPuzzleSubtype, true) >= n) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                context.getString(PuzzleUtils.getPuzzleName(currentPuzzle)) + ": " +
                    PuzzleUtils.createAverageList(n, currentPuzzle, currentPuzzleSubtype, dbHandler));
            shareIntent.setType("text/plain");
            context.startActivity(shareIntent);
            return true;
        } else {
            return false;
        }
    }


    public static String createHistogramOf(String currentPuzzle, String currentPuzzleSubtype, DatabaseHandler dbHandler) {
        Cursor cursor = dbHandler.getAllSolvesFromWithLimit(100, currentPuzzle, currentPuzzleSubtype, false);


        ArrayList<Integer> timeList = new ArrayList<>();
        int columnIndex = cursor.getColumnIndex(DatabaseHandler.KEY_TIME);
        while (cursor.moveToNext()) {
            // Cut off decimals
            int time = cursor.getInt(columnIndex);
            time = time - (time % 1000);
            timeList.add(time);
        }

        StringBuilder histogram = new StringBuilder();

        Set<Integer> set = new HashSet<>(timeList);
        //HashMap<Integer, Integer> frequencies = new HashMap();
        for (int time : set) {
            //frequencies.put(time, Collections.frequency(timeList, time));
            histogram.append("\n" +
                PuzzleUtils.convertTimeToStringWithoutMilli(time) + ": " + convertToBars(Collections.frequency(timeList, time)));
        }

        return histogram.toString();
    }

    public static boolean shareHistogramOf(String currentPuzzle, String currentPuzzleSubtype, DatabaseHandler dbHandler, Context context) {
        int solveCount = dbHandler.getSolveCountWithLimit(100, currentPuzzle, currentPuzzleSubtype, true);
        if (solveCount > 0) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                context.getString(R.string.fab_share_histogram_solvecount,
                    context.getString(PuzzleUtils.getPuzzleName(currentPuzzle)), solveCount) + ":" +
                    PuzzleUtils.createHistogramOf(currentPuzzle, currentPuzzleSubtype, dbHandler));
            shareIntent.setType("text/plain");
            context.startActivity(shareIntent);
            return true;
        } else {
            return false;
        }
    }


    /**
     * Takes an int N and converts it to bars █. Used for histograms
     *
     * @param n
     *
     * @return
     */
    private static String convertToBars(int n) {
        StringBuilder temp = new StringBuilder();
        for (int i = 0; i < n; i++) {
            temp.append("█");
        }
        return temp.toString();
    }

}
