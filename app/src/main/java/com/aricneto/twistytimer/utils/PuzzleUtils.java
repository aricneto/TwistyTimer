package com.aricneto.twistytimer.utils;

import android.support.annotation.StringRes;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.items.Solve;

import org.joda.time.DateTime;

import java.util.Locale;
import java.util.Scanner;

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

    public static final int TIME_DNF = -1;

    public PuzzleUtils() {
    }

    /**
     * Gets the position of the puzzle in the {@link com.aricneto.twistytimer.fragment.TimerFragmentMain} spinner list
     * @param puzzle
     * @return
     */
    public static int getPositionInSpinner(String puzzle) {
        switch (puzzle) {
            case PuzzleUtils.TYPE_333: // 333
                return 0;
            case PuzzleUtils.TYPE_222: // 222
                return 1;
            case PuzzleUtils.TYPE_444: // 444
                return 2;
            case PuzzleUtils.TYPE_555: // 555
                return 3;
            case PuzzleUtils.TYPE_666: // 666
                return 4;
            case PuzzleUtils.TYPE_777: // 777
                return 5;
            case PuzzleUtils.TYPE_CLOCK: // Clock
                return 6;
            case PuzzleUtils.TYPE_MEGA: // Mega
                return 7;
            case PuzzleUtils.TYPE_PYRA: // Pyra
                return 8;
            case PuzzleUtils.TYPE_SKEWB: // Skewb
                return 9;
            case PuzzleUtils.TYPE_SQUARE1: // Square-1
                return 10;
        }
        return 0;
    }

    /**
     * Gets the string id of the name of a puzzle
     * @param puzzle
     * @return
     */
    public static @StringRes int getPuzzleName(String puzzle) {
        switch (puzzle) {
            case PuzzleUtils.TYPE_333: // 333
                return R.string.cube_333;
            case PuzzleUtils.TYPE_222: // 222
                return R.string.cube_222;
            case PuzzleUtils.TYPE_444: // 444
                return R.string.cube_444;
            case PuzzleUtils.TYPE_555: // 555
                return R.string.cube_555;
            case PuzzleUtils.TYPE_666: // 666
                return R.string.cube_666;
            case PuzzleUtils.TYPE_777: // 777
                return R.string.cube_777;
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
            return new DateTime(time).toString("kk':'mm':'ss'.'SS");

        else if (minutes > 0)
            return new DateTime(time).toString("mm':'ss'.'SS");

        else
            return new DateTime(time).toString("s'.'SS");
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
     * @param input
     * @return time in millis
     */
    public static int parseTime(String input) {
        Scanner scanner = new Scanner(input.trim());
        scanner.useLocale(Locale.ENGLISH);

        int time;

        // 00:00.00
        if (input.contains(":")) {
            scanner.useDelimiter(":");

            if (!scanner.hasNextLong()) {
                return 0;
            }

            long minutes = scanner.nextLong();
            if (minutes < 0) {
                return 0;
            }

            if (!scanner.hasNextDouble()) {
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
            if (!scanner.hasNextDouble()) {
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

}
