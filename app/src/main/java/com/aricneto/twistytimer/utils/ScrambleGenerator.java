package com.aricneto.twistytimer.utils;

import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;

import com.aricneto.twistytimer.puzzle.NbyNCubePuzzle;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import net.gnehzr.tnoodle.scrambles.InvalidScrambleException;
import net.gnehzr.tnoodle.scrambles.Puzzle;

import puzzle.ClockPuzzle;
import puzzle.MegaminxPuzzle;
import puzzle.NoInspectionFiveByFiveCubePuzzle;
import puzzle.NoInspectionFourByFourCubePuzzle;
import puzzle.PyraminxPuzzle;
import puzzle.SkewbPuzzle;
import puzzle.SquareOneUnfilteredPuzzle;
import puzzle.ThreeByThreeCubePuzzle;
import puzzle.TwoByTwoCubePuzzle;

/**
 * Util for generating and drawing scrambles
 */
public class ScrambleGenerator {
    private Puzzle puzzle;
    private String finalScramble;
    private String puzzleType;

    public ScrambleGenerator(String type) {
        puzzleType = type;
        switch (type) {
            case PuzzleUtils.TYPE_222:
                puzzle = new TwoByTwoCubePuzzle();
                break;
            case PuzzleUtils.TYPE_333:
                puzzle = new ThreeByThreeCubePuzzle();
                break;
            case PuzzleUtils.TYPE_444:
                puzzle = new NoInspectionFourByFourCubePuzzle();
                break;
            case PuzzleUtils.TYPE_555:
                puzzle = new NoInspectionFiveByFiveCubePuzzle();
                break;
            case PuzzleUtils.TYPE_666:
                puzzle = new NbyNCubePuzzle(6);
                break;
            case PuzzleUtils.TYPE_777:
                puzzle = new NbyNCubePuzzle(7);
                break;
            case PuzzleUtils.TYPE_MEGA:
                puzzle = new MegaminxPuzzle();
                break;
            case PuzzleUtils.TYPE_PYRA:
                puzzle = new PyraminxPuzzle();
                break;
            case PuzzleUtils.TYPE_SKEWB:
                puzzle = new SkewbPuzzle();
                break;
            case PuzzleUtils.TYPE_CLOCK:
                puzzle = new ClockPuzzle();
                break;
            case PuzzleUtils.TYPE_SQUARE1:
                puzzle = new SquareOneUnfilteredPuzzle();
                break;
        }
    }

    public Puzzle getPuzzle() {
        return puzzle;
    }

    public void setPuzzle(Puzzle puzzle) {
        this.puzzle = puzzle;
    }

    /**
     * Returns a scramble drawable showing the puzzled scrambled
     * Uses Tnoodle lib
     *
     * @return
     */

    public Drawable generateImageFromScramble(SharedPreferences sp, String scramble) {
        // Getting the color scheme
        String top;
        String left;
        String front;
        String right;
        String back;
        String down;
        if (! puzzleType.equals(PuzzleUtils.TYPE_SKEWB)) {
            top = sp.getString("cubeTop", "FFFFFF");
            left = sp.getString("cubeLeft", "FF8B24");
            front = sp.getString("cubeFront", "02D040");
            right = sp.getString("cubeRight", "EC0000");
            back = sp.getString("cubeBack", "304FFE");
            down = sp.getString("cubeDown", "FDD835");
        } else { //skewb has a different scheme
            top = sp.getString("cubeTop", "FFFFFF");
            left = sp.getString("cubeFront", "FF8B24");
            front = sp.getString("cubeRight", "02D040");
            right = sp.getString("cubeBack", "EC0000");
            back = sp.getString("cubeLeft", "304FFE");
            down = sp.getString("cubeDown", "FDD835");
        }

        String cubeImg = null;
        SVG cubeSvg = null;
        Drawable pic = null;

        try {
            cubeImg = puzzle.drawScramble(scramble, puzzle.parseColorScheme(back + "," + down + "," + front + "," + left + "," + right + "," + top)).toString();
        } catch (InvalidScrambleException e) {
            e.printStackTrace();
        }

        if (cubeImg != null) {
            try {
                cubeSvg = SVG.getFromString(cubeImg);
                pic = new PictureDrawable(cubeSvg.renderToPicture());
            } catch (SVGParseException e) {
                e.printStackTrace();
            }
        }

        return pic;
    }
}
