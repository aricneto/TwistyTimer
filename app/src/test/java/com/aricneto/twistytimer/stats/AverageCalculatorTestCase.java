package com.aricneto.twistytimer.stats;

import com.aricneto.twistytimer.stats.AverageCalculator.AverageOfN;

import org.junit.Test;

import java.util.Random;

import static com.aricneto.twistytimer.stats.AverageCalculator.DNF;
import static com.aricneto.twistytimer.stats.AverageCalculator.UNKNOWN;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Tests the {@link AverageCalculator} class. Averages are tests for 1, 3 and 5 solves. This covers
 * the trivial case (1), the case where a simple arithmetic mean is used (3) and the case where a
 * truncated arithmetic mean is used (5). Values above 5 should be no different. Each average is
 * tested for both DNF handling modes (automatic disqualification or not).
 *
 * @author damo
 */
public class AverageCalculatorTestCase {
    @Test
    public void testCreateOne() throws Exception {
        final AverageCalculator ac = new AverageCalculator(1, 10, 10, false);

        assertEquals(1, ac.getN());
        assertEquals(0, ac.getNumSolves());
        assertEquals(0, ac.getNumDNFSolves());

        assertEquals(UNKNOWN, ac.getCurrentAverage());
        assertEquals(UNKNOWN, ac.getBestAverage());

        assertEquals(UNKNOWN, ac.getBestTime());
        assertEquals(UNKNOWN, ac.getWorstTime());
        assertEquals(UNKNOWN, ac.getTotalTime());
        assertEquals(UNKNOWN, ac.getMeanTime());
        assertEquals(UNKNOWN, ac.getStandardDeviation());
    }

    @Test
    public void testCreateThree() throws Exception {
        final AverageCalculator ac = new AverageCalculator(3, 0, 0,false);

        assertEquals(3, ac.getN());
        assertEquals(0, ac.getNumSolves());
        assertEquals(0, ac.getNumDNFSolves());

        assertEquals(UNKNOWN, ac.getCurrentAverage());
        assertEquals(UNKNOWN, ac.getBestAverage());

        assertEquals(UNKNOWN, ac.getBestTime());
        assertEquals(UNKNOWN, ac.getWorstTime());
        assertEquals(UNKNOWN, ac.getTotalTime());
        assertEquals(UNKNOWN, ac.getMeanTime());
        assertEquals(UNKNOWN, ac.getStandardDeviation());
    }

    @Test
    public void testCreateFive() throws Exception {
        final AverageCalculator ac = new AverageCalculator(5, 20, 20, false);

        assertEquals(5, ac.getN());
        assertEquals(0, ac.getNumSolves());
        assertEquals(0, ac.getNumDNFSolves());

        assertEquals(UNKNOWN, ac.getCurrentAverage());
        assertEquals(UNKNOWN, ac.getBestAverage());

        assertEquals(UNKNOWN, ac.getBestTime());
        assertEquals(UNKNOWN, ac.getWorstTime());
        assertEquals(UNKNOWN, ac.getTotalTime());
        assertEquals(UNKNOWN, ac.getMeanTime());
        assertEquals(UNKNOWN, ac.getStandardDeviation());
    }

    @Test
    public void testCreateFailure() throws Exception {
        try {
            new AverageCalculator(0, 0, 0, false);
            fail("Expected an exception when 'n' is zero.");
        } catch (IllegalArgumentException ignore) {
            // This is expected.
        } catch (Exception e) {
            fail("Unexpected exception type: " + e);
        }

        try {
            new AverageCalculator(-1, 0, 0,false);
            fail("Expected an exception when 'n' is negative.");
        } catch (IllegalArgumentException ignore) {
            // This is expected.
        } catch (Exception e) {
            fail("Unexpected exception type: " + e);
        }
    }

    @Test
    public void testAddTime() throws Exception {
        final AverageCalculator ac = new AverageCalculator(5, 20, 20, true);

        // Initial state is already checked in other test methods.
        // Just test that the counters, sums, best, worst, etc. are updated.
        ac.addTime(DNF);
        assertEquals(1, ac.getNumSolves());
        assertEquals(1, ac.getNumDNFSolves());
        assertEquals(UNKNOWN, ac.getBestTime());
        assertEquals(UNKNOWN, ac.getWorstTime());
        assertEquals(UNKNOWN, ac.getTotalTime());
        assertEquals(UNKNOWN, ac.getMeanTime());
        assertEquals(UNKNOWN, ac.getStandardDeviation());

        ac.addTime(500);
        assertEquals(2, ac.getNumSolves());
        assertEquals(500, ac.getBestTime());
        assertEquals(500, ac.getWorstTime());
        assertEquals(500, ac.getTotalTime());
        assertEquals(500, ac.getMeanTime());
        assertEquals(UNKNOWN, ac.getStandardDeviation());

        ac.addTime(300);
        assertEquals(3, ac.getNumSolves());
        assertEquals(300, ac.getBestTime());
        assertEquals(500, ac.getWorstTime());
        assertEquals(800, ac.getTotalTime());
        assertEquals(400, ac.getMeanTime());
        assertEquals(UNKNOWN, ac.getStandardDeviation());

        // Standard deviation should only be calculated once valid (non-DNF) solves > 2
        ac.addTime(1000);
        assertEquals(4, ac.getNumSolves());
        assertEquals(300, ac.getBestTime());
        assertEquals(1000, ac.getWorstTime());
        assertEquals(1800, ac.getTotalTime());
        assertEquals(600, ac.getMeanTime());
        assertEquals(360, ac.getStandardDeviation());

        ac.addTime(DNF);
        assertEquals(5, ac.getNumSolves());
        assertEquals(300, ac.getBestTime());
        assertEquals(1000, ac.getWorstTime());
        assertEquals(1800, ac.getTotalTime());
        assertEquals(600, ac.getMeanTime());
        assertEquals(360, ac.getStandardDeviation());
    }

    @Test
    public void testAddTimes() throws Exception {
        final AverageCalculator ac = new AverageCalculator(5, 20, 20, true);

        // Initial state is already checked in other test methods.
        // Just test that the counters, sums, best, worst, etc. are updated.
        ac.addTimes((long[]) null);
        assertEquals("Null array of times should be ignored.", 0, ac.getNumSolves());

        ac.addTimes();
        assertEquals("Empty array of times should be ignored.", 0, ac.getNumSolves());

        ac.addTimes(DNF, 500, 300, DNF);
        assertEquals(4, ac.getNumSolves());
        assertEquals(2, ac.getNumDNFSolves());
        assertEquals(300, ac.getBestTime());
        assertEquals(500, ac.getWorstTime());
        assertEquals(800, ac.getTotalTime());
        assertEquals(400, ac.getMeanTime());
        assertEquals(UNKNOWN, ac.getStandardDeviation());
    }

    /*
    // WARNING:
    // These tests will currently always return a fail, since the exception
    // has been disabled. See reason in the addTime function.
    // I've disable them for the time being

    @Test
    public void testAddTimeFailure() throws Exception {
        final AverageCalculator ac = new AverageCalculator(5, true);

        // Initial state is already checked in other test methods.
        try {
            ac.addTime(0);
            fail("Expected an exception when added time is zero.");
        } catch (IllegalArgumentException ignore) {
            // This is expected.
        } catch (Exception e) {
            fail("Unexpected exception type: " + e);
        }

        try {
            ac.addTime(-1);
            fail("Expected an exception when added time is negative.");
        } catch (IllegalArgumentException ignore) {
            // This is expected.
        } catch (Exception e) {
            fail("Unexpected exception type: " + e);
        }

        try {
            ac.addTime(UNKNOWN);
            fail("Expected an exception when added time is UNKNOWN.");
        } catch (IllegalArgumentException ignore) {
            // This is expected.
        } catch (Exception e) {
            fail("Unexpected exception type: " + e);
        }
    } */

    /**
     * Tests the trivial edge case where "n" is one. DNFs cause disqualification of the average,
     * but that should make no difference when "n" is one.
     *
     * @throws Exception If the test fails to run.
     */
    @Test
    public void testAverageOfOneDisqualifyDNFs() throws Exception {
        final AverageCalculator ac = new AverageCalculator(1, 0, 0, true);

        // Initial state is already checked in other test methods.
        ac.addTime(500);

        assertEquals(1, ac.getNumSolves());
        assertEquals(0, ac.getNumDNFSolves());

        assertEquals(500, ac.getCurrentAverage());
        assertEquals(500, ac.getBestAverage());

        assertEquals(500, ac.getBestTime());
        assertEquals(500, ac.getWorstTime());
        assertEquals(500, ac.getTotalTime());
        assertEquals(500, ac.getMeanTime());

        ac.addTime(300);

        assertEquals(2, ac.getNumSolves());
        assertEquals(0, ac.getNumDNFSolves());

        assertEquals(300, ac.getCurrentAverage());
        assertEquals(300, ac.getBestAverage());

        assertEquals(300, ac.getBestTime());
        assertEquals(500, ac.getWorstTime());
        assertEquals(800, ac.getTotalTime());
        assertEquals(400, ac.getMeanTime());

        ac.addTime(DNF);
        assertEquals(3, ac.getNumSolves());
        assertEquals(1, ac.getNumDNFSolves());

        assertEquals(DNF, ac.getCurrentAverage());
        assertEquals(300, ac.getBestAverage());

        assertEquals(300, ac.getBestTime());
        assertEquals(500, ac.getWorstTime());
        assertEquals(800, ac.getTotalTime());
        assertEquals(400, ac.getMeanTime());

        ac.addTime(1000);
        assertEquals(4, ac.getNumSolves());
        assertEquals(1, ac.getNumDNFSolves());

        assertEquals(1000, ac.getCurrentAverage());
        assertEquals(300, ac.getBestAverage());

        assertEquals(300, ac.getBestTime());
        assertEquals(1000, ac.getWorstTime());
        assertEquals(1800, ac.getTotalTime());
        assertEquals(600, ac.getMeanTime());
    }

    /**
     * Tests the trivial edge case where "n" is one. DNFs do not cause disqualification of the
     * average, but that should make no difference when "n" is one.
     *
     * @throws Exception If the test fails to run.
     */
    @Test
    public void testAverageOfOneAllowDNFs() throws Exception {
        final AverageCalculator ac = new AverageCalculator(1, 0, 0,false);

        // Initial state is already checked in other test methods.
        ac.addTime(500);

        assertEquals(1, ac.getNumSolves());
        assertEquals(0, ac.getNumDNFSolves());

        assertEquals(500, ac.getCurrentAverage());
        assertEquals(500, ac.getBestAverage());

        assertEquals(500, ac.getBestTime());
        assertEquals(500, ac.getWorstTime());
        assertEquals(500, ac.getTotalTime());
        assertEquals(500, ac.getMeanTime());

        ac.addTime(300);

        assertEquals(2, ac.getNumSolves());
        assertEquals(0, ac.getNumDNFSolves());

        assertEquals(300, ac.getCurrentAverage());
        assertEquals(300, ac.getBestAverage());

        assertEquals(300, ac.getBestTime());
        assertEquals(500, ac.getWorstTime());
        assertEquals(800, ac.getTotalTime());
        assertEquals(400, ac.getMeanTime());

        ac.addTime(DNF);
        assertEquals(3, ac.getNumSolves());
        assertEquals(1, ac.getNumDNFSolves());

        assertEquals(DNF, ac.getCurrentAverage());
        assertEquals(300, ac.getBestAverage());

        assertEquals(300, ac.getBestTime());
        assertEquals(500, ac.getWorstTime());
        assertEquals(800, ac.getTotalTime());
        assertEquals(400, ac.getMeanTime());

        ac.addTime(1000);
        assertEquals(4, ac.getNumSolves());
        assertEquals(1, ac.getNumDNFSolves());

        assertEquals(1000, ac.getCurrentAverage());
        assertEquals(300, ac.getBestAverage());

        assertEquals(300, ac.getBestTime());
        assertEquals(1000, ac.getWorstTime());
        assertEquals(1800, ac.getTotalTime());
        assertEquals(600, ac.getMeanTime());
    }

    /**
     * Tests the calculation of the average of three. For an average of three, a truncated mean
     * should not be calculated. Any DNF should cause disqualification of the average.
     *
     * @throws Exception If the test fails to run.
     */
    @Test
    public void testAverageOfThreeDisqualifyDNFs() throws Exception {
        final AverageCalculator ac = new AverageCalculator(3, 0, 0,true);

        // Initial state is already checked in other test methods.
        ac.addTimes(500, 250, 150);

        assertEquals(3, ac.getNumSolves());
        assertEquals(0, ac.getNumDNFSolves());

        assertEquals(300, ac.getCurrentAverage());
        assertEquals(300, ac.getBestAverage());

        assertEquals(150, ac.getBestTime());
        assertEquals(500, ac.getWorstTime());
        assertEquals(900, ac.getTotalTime());
        assertEquals(300, ac.getMeanTime());
        assertEquals(180, ac.getStandardDeviation());

        ac.addTimes(DNF, 800);

        assertEquals(5, ac.getNumSolves());
        assertEquals(1, ac.getNumDNFSolves());

        assertEquals(DNF, ac.getCurrentAverage());
        assertEquals(300, ac.getBestAverage());

        assertEquals(150, ac.getBestTime());
        assertEquals(800, ac.getWorstTime());
        assertEquals(1700, ac.getTotalTime());
        assertEquals(425, ac.getMeanTime()); // 1700 / 4 non-DNF solves.
        assertEquals(290, ac.getStandardDeviation());

        ac.addTimes(100);

        assertEquals(6, ac.getNumSolves());
        assertEquals(1, ac.getNumDNFSolves());

        assertEquals(DNF, ac.getCurrentAverage());
        assertEquals(300, ac.getBestAverage());

        assertEquals(100, ac.getBestTime());
        assertEquals(800, ac.getWorstTime());
        assertEquals(1800, ac.getTotalTime());
        assertEquals(360, ac.getMeanTime()); // 1800 / 5 non-DNF solves.
        assertEquals(290, ac.getStandardDeviation());

        // Third non-DNF time in a row should push change the current average to a non-DNF average.
        ac.addTimes(900);

        assertEquals(7, ac.getNumSolves());
        assertEquals(1, ac.getNumDNFSolves());

        assertEquals(600, ac.getCurrentAverage()); // Last three were 800, 100, 900.
        assertEquals(300, ac.getBestAverage());

        assertEquals(100, ac.getBestTime());
        assertEquals(900, ac.getWorstTime());
        assertEquals(2700, ac.getTotalTime());
        assertEquals(450, ac.getMeanTime()); // 2700 / 6 non-DNF solves.
        assertEquals(340, ac.getStandardDeviation());

        ac.addTimes(DNF);

        assertEquals(8, ac.getNumSolves());
        assertEquals(2, ac.getNumDNFSolves());

        assertEquals(DNF, ac.getCurrentAverage()); // Last three were 100, 900, DNF.
        assertEquals(300, ac.getBestAverage());

        assertEquals(100, ac.getBestTime());
        assertEquals(900, ac.getWorstTime());
        assertEquals(2700, ac.getTotalTime());
        assertEquals(450, ac.getMeanTime()); // 2700 / 6 non-DNF solves.
        assertEquals(340, ac.getStandardDeviation());

        // Set a new record for the average time.
        ac.addTimes(90, 210, 300);

        assertEquals(11, ac.getNumSolves());
        assertEquals(2, ac.getNumDNFSolves());

        assertEquals(200, ac.getCurrentAverage());
        assertEquals(200, ac.getBestAverage());

        assertEquals(90, ac.getBestTime());
        assertEquals(900, ac.getWorstTime());
        assertEquals(3300, ac.getTotalTime());
        assertEquals(366, ac.getMeanTime()); // 3300 / 9 non-DNF solves. 366.6666... is truncated.
        assertEquals(301, ac.getStandardDeviation());
    }

    /**
     * Tests the calculation of the average of three. For an average of three, a truncated mean
     * should not be calculated. DNFs will not cause automatic disqualification of the average.
     *
     * @throws Exception If the test fails to run.
     */
    @Test
    public void testAverageOfThreeAllowDNFs() throws Exception {
        final AverageCalculator ac = new AverageCalculator(3, 0, 0,false);

        // Initial state is already checked in other test methods.
        ac.addTimes(500, 250, 150);

        assertEquals(3, ac.getNumSolves());
        assertEquals(0, ac.getNumDNFSolves());

        assertEquals(300, ac.getCurrentAverage());
        assertEquals(300, ac.getBestAverage());

        assertEquals(150, ac.getBestTime());
        assertEquals(500, ac.getWorstTime());
        assertEquals(900, ac.getTotalTime());
        assertEquals(300, ac.getMeanTime());

        ac.addTimes(DNF, 800);

        assertEquals(5, ac.getNumSolves());
        assertEquals(1, ac.getNumDNFSolves());

        assertEquals(475, ac.getCurrentAverage()); // Last three were 150, DNF, 800. Ignore DNF.
        assertEquals(200, ac.getBestAverage()); // Earlier sequence 250, 150, DNF. Ignore DNF.

        assertEquals(150, ac.getBestTime());
        assertEquals(800, ac.getWorstTime());
        assertEquals(1700, ac.getTotalTime());
        assertEquals(425, ac.getMeanTime()); // 1700 / 4 non-DNF solves.

        ac.addTimes(100);

        assertEquals(6, ac.getNumSolves());
        assertEquals(1, ac.getNumDNFSolves());

        assertEquals(450, ac.getCurrentAverage()); // Last three were DNF, 800, 100. Ignore DNF.
        assertEquals(200, ac.getBestAverage());

        assertEquals(100, ac.getBestTime());
        assertEquals(800, ac.getWorstTime());
        assertEquals(1800, ac.getTotalTime());
        assertEquals(360, ac.getMeanTime()); // 1800 / 5 non-DNF solves.

        // Third non-DNF time in a row.
        ac.addTimes(900);

        assertEquals(7, ac.getNumSolves());
        assertEquals(1, ac.getNumDNFSolves());

        assertEquals(600, ac.getCurrentAverage()); // Last three were 800, 100, 900.
        assertEquals(200, ac.getBestAverage());

        assertEquals(100, ac.getBestTime());
        assertEquals(900, ac.getWorstTime());
        assertEquals(2700, ac.getTotalTime());
        assertEquals(450, ac.getMeanTime()); // 2700 / 6 non-DNF solves.

        ac.addTimes(DNF);

        assertEquals(8, ac.getNumSolves());
        assertEquals(2, ac.getNumDNFSolves());

        assertEquals(500, ac.getCurrentAverage()); // Last three were 100, 900, DNF.
        assertEquals(200, ac.getBestAverage());

        assertEquals(100, ac.getBestTime());
        assertEquals(900, ac.getWorstTime());
        assertEquals(2700, ac.getTotalTime());
        assertEquals(450, ac.getMeanTime()); // 2700 / 6 non-DNF solves.

        // Set a new record for the average time.
        ac.addTimes(90, 210, 300);

        assertEquals(11, ac.getNumSolves());
        assertEquals(2, ac.getNumDNFSolves());

        assertEquals(200, ac.getCurrentAverage());
        assertEquals(150, ac.getBestAverage()); // Average of DNF, 90, 201. Ignore DNF.

        assertEquals(90, ac.getBestTime());
        assertEquals(900, ac.getWorstTime());
        assertEquals(3300, ac.getTotalTime());
        assertEquals(366, ac.getMeanTime()); // 3300 / 9 non-DNF solves. 366.6666... is truncated.

        // All but one time is a DNF. Average is just that one non-DNF time.
        ac.addTimes(100, DNF, DNF);

        assertEquals(14, ac.getNumSolves());
        assertEquals(4, ac.getNumDNFSolves());

        assertEquals(100, ac.getCurrentAverage()); // Average of 100, DNF, DNF. Ignore DNFs.
        assertEquals(100, ac.getBestAverage()); // As above.

        assertEquals(90, ac.getBestTime());
        assertEquals(900, ac.getWorstTime());
        assertEquals(3400, ac.getTotalTime());
        assertEquals(340, ac.getMeanTime()); // 3400 / 10 non-DNF solves.

        // All times are DNF. Average must be a DNF.
        ac.addTimes(DNF);

        assertEquals(15, ac.getNumSolves());
        assertEquals(5, ac.getNumDNFSolves());

        assertEquals(DNF, ac.getCurrentAverage()); // Average of DNF, DNF, DNF. Ignore DNFs.
        assertEquals(100, ac.getBestAverage()); // Unchanged.

        assertEquals(90, ac.getBestTime());
        assertEquals(900, ac.getWorstTime());
        assertEquals(3400, ac.getTotalTime());
        assertEquals(340, ac.getMeanTime()); // 3400 / 10 non-DNF solves.
    }

    /**
     * Tests the calculation of the average of five. For an average of five, a truncated mean
     * should be calculated. Any DNF should cause disqualification of the average.
     *
     * @throws Exception If the test fails to run.
     */
    @Test
    public void testAverageOfFiveDisqualifyDNFs() throws Exception {
        final AverageCalculator ac = new AverageCalculator(5, 20, 20,true);

        // Initial state is already checked in other test methods.
        ac.addTimes(500, 250, 150, 400, 200);

        assertEquals(5, ac.getNumSolves());
        assertEquals(0, ac.getNumDNFSolves());

        assertEquals(283, ac.getCurrentAverage()); // (250+400+200) / 3. Exclude 150, 500.
        assertEquals(283, ac.getBestAverage());

        assertEquals(150, ac.getBestTime());
        assertEquals(500, ac.getWorstTime());
        assertEquals(1500, ac.getTotalTime());
        assertEquals(300, ac.getMeanTime());

        // One DNF should be tolerated and treated as the worst time when calculating the average.
        // (It is not the worst time reported, though, as that is always a non-DNF time.)
        ac.addTimes(DNF, 800); // Current: 150, 400, 200, DNF, 800

        assertEquals(7, ac.getNumSolves());
        assertEquals(1, ac.getNumDNFSolves());

        assertEquals(466, ac.getCurrentAverage()); // (400+200+800) / 3. Exclude 150, DNF.
        assertEquals(283, ac.getBestAverage());

        assertEquals(150, ac.getBestTime());
        assertEquals(800, ac.getWorstTime());
        assertEquals(2300, ac.getTotalTime());
        assertEquals(383, ac.getMeanTime()); // 2300 / 6 non-DNF solves.

        ac.addTimes(300); // Current: 400, 200, DNF, 800, 300

        assertEquals(8, ac.getNumSolves());
        assertEquals(1, ac.getNumDNFSolves());

        assertEquals(500, ac.getCurrentAverage()); // (400+800+300) / 3. Exclude 200, DNF.
        assertEquals(283, ac.getBestAverage());

        assertEquals(150, ac.getBestTime());
        assertEquals(800, ac.getWorstTime());
        assertEquals(2600, ac.getTotalTime());
        assertEquals(371, ac.getMeanTime()); // 2600 / 7 non-DNF solves.

        // Second DNF in "current" 5 times. Result should be disqualified.
        ac.addTimes(DNF); // Current: 200, DNF, 800, 300, DNF

        assertEquals(9, ac.getNumSolves());
        assertEquals(2, ac.getNumDNFSolves());

        assertEquals(DNF, ac.getCurrentAverage()); // More than one DNF.
        assertEquals(283, ac.getBestAverage());

        assertEquals(150, ac.getBestTime());
        assertEquals(800, ac.getWorstTime());
        assertEquals(2600, ac.getTotalTime());
        assertEquals(371, ac.getMeanTime()); // 2600 / 7 non-DNF solves.

        // Test the "reset()" method, too.
        ac.reset();

        assertEquals(5, ac.getN()); // Should not be changed by a reset.
        assertEquals(0, ac.getNumSolves());
        assertEquals(0, ac.getNumDNFSolves());

        assertEquals(UNKNOWN, ac.getCurrentAverage());
        assertEquals(UNKNOWN, ac.getBestAverage());

        assertEquals(UNKNOWN, ac.getBestTime());
        assertEquals(UNKNOWN, ac.getWorstTime());
        assertEquals(UNKNOWN, ac.getTotalTime());
        assertEquals(UNKNOWN, ac.getMeanTime());
    }

    /**
     * Tests the calculation of the average of five. For an average of five, a truncated mean
     * should be calculated. DNFs will not cause automatic disqualification of the average.
     *
     * @throws Exception If the test fails to run.
     */
    @Test
    public void testAverageOfFiveAllowDNFs() throws Exception {
        final AverageCalculator ac = new AverageCalculator(5, 20, 20,false);

        // Initial state is already checked in other test methods.
        ac.addTimes(500, 250, 150, 400, 200);

        assertEquals(5, ac.getNumSolves());
        assertEquals(0, ac.getNumDNFSolves());

        assertEquals(283, ac.getCurrentAverage()); // (250+400+200) / 3. Exclude 150, 500.
        assertEquals(283, ac.getBestAverage());

        assertEquals(150, ac.getBestTime());
        assertEquals(500, ac.getWorstTime());
        assertEquals(1500, ac.getTotalTime());
        assertEquals(300, ac.getMeanTime());

        // One DNF should be tolerated and treated as the worst time when calculating the average.
        // (It is not the worst time reported, though, as that is always a non-DNF time.)
        ac.addTimes(DNF, 800); // Current: 150, 400, 200, DNF, 800

        assertEquals(7, ac.getNumSolves());
        assertEquals(1, ac.getNumDNFSolves());

        assertEquals(466, ac.getCurrentAverage()); // (400+200+800) / 3. Exclude 150, DNF.
        assertEquals(283, ac.getBestAverage());

        assertEquals(150, ac.getBestTime());
        assertEquals(800, ac.getWorstTime());
        assertEquals(2300, ac.getTotalTime());
        assertEquals(383, ac.getMeanTime()); // 2300 / 6 non-DNF solves.

        ac.addTimes(300); // Current: 400, 200, DNF, 800, 300

        assertEquals(8, ac.getNumSolves());
        assertEquals(1, ac.getNumDNFSolves());

        assertEquals(500, ac.getCurrentAverage()); // (400+800+300) / 3. Exclude 200, DNF.
        assertEquals(283, ac.getBestAverage());

        assertEquals(150, ac.getBestTime());
        assertEquals(800, ac.getWorstTime());
        assertEquals(2600, ac.getTotalTime());
        assertEquals(371, ac.getMeanTime()); // 2600 / 7 non-DNF solves.

        // Second DNF in "current" 5 times. Result should still be tolerated. One DNF is treated
        // as the worst time and all other DNFs are ignored.
        ac.addTimes(DNF); // Current: 200, DNF, 800, 300, DNF

        assertEquals(9, ac.getNumSolves());
        assertEquals(2, ac.getNumDNFSolves());

        assertEquals(550, ac.getCurrentAverage()); // (800+300) / 2. Exclude 200, DNF. Ignore DNFs.
        assertEquals(283, ac.getBestAverage());

        assertEquals(150, ac.getBestTime());
        assertEquals(800, ac.getWorstTime());
        assertEquals(2600, ac.getTotalTime());
        assertEquals(371, ac.getMeanTime()); // 2600 / 7 non-DNF solves.

        // Third DNF in "current" 5 times. Result should still be tolerated. One DNF is treated
        // as the worst time and all other DNFs are ignored.
        ac.addTimes(DNF); // Current: DNF, 800, 300, DNF, DNF

        assertEquals(10, ac.getNumSolves());
        assertEquals(3, ac.getNumDNFSolves());

        assertEquals(800, ac.getCurrentAverage()); // (800) / 1. Exclude 300, DNF. Ignore DNFs.
        assertEquals(283, ac.getBestAverage());

        assertEquals(150, ac.getBestTime());
        assertEquals(800, ac.getWorstTime());
        assertEquals(2600, ac.getTotalTime());
        assertEquals(371, ac.getMeanTime()); // 2600 / 7 non-DNF solves.

        // Fourth DNF in "current" 5 times. Result should still be tolerated. One DNF is treated
        // as the worst time and all other DNFs are ignored. As only one non-DNF time remains, it
        // cannot be excluded as the best time (the worst being a DNF).
        ac.addTimes(DNF, DNF); // Current: 300, DNF, DNF, DNF, DNF

        assertEquals(12, ac.getNumSolves());
        assertEquals(5, ac.getNumDNFSolves());

        assertEquals(300, ac.getCurrentAverage()); // 300 is only non-DNF time left.
        assertEquals(283, ac.getBestAverage());

        assertEquals(150, ac.getBestTime());
        assertEquals(800, ac.getWorstTime());
        assertEquals(2600, ac.getTotalTime());
        assertEquals(371, ac.getMeanTime()); // 2600 / 7 non-DNF solves.

        // Fifth DNF in "current" 5 times. Average must be a DNF.
        ac.addTimes(DNF); // Current: DNF, DNF, DNF, DNF, DNF

        assertEquals(13, ac.getNumSolves());
        assertEquals(6, ac.getNumDNFSolves());

        assertEquals(DNF, ac.getCurrentAverage()); // 300 is only non-DNF time left.
        assertEquals(283, ac.getBestAverage());

        assertEquals(150, ac.getBestTime());
        assertEquals(800, ac.getWorstTime());
        assertEquals(2600, ac.getTotalTime());
        assertEquals(371, ac.getMeanTime()); // 2600 / 7 non-DNF solves.

        // New non-DNF time, and it is a record time, too.
        ac.addTimes(100); // Current: DNF, DNF, DNF, DNF, 100

        assertEquals(14, ac.getNumSolves());
        assertEquals(6, ac.getNumDNFSolves());

        assertEquals(100, ac.getCurrentAverage()); // 100 is only non-DNF time left.
        assertEquals(100, ac.getBestAverage());

        assertEquals(100, ac.getBestTime());
        assertEquals(800, ac.getWorstTime());
        assertEquals(2700, ac.getTotalTime());
        assertEquals(337, ac.getMeanTime()); // 2700 / 8 non-DNF solves.
    }

    /**
     * Tests the {@link AverageOfN} class to ensure it presents the times in the correct order
     * and identifies the best and worst times correctly. This test covers the case where best
     * and worst times should not be identified because the value of "N" is low. DNFs do not
     * disqualify the average unless all times are DNFs. The implementation is known to rely on
     * its parent class for much of the details, and those are tested in other methods, so these
     * tests are not comprehensive.
     *
     * @throws Exception If the test fails to run.
     */
    @Test
    public void testAverageOfNDetailsForThreeAllowDNFs() throws Exception {
        final AverageCalculator ac = new AverageCalculator(3, 0, 0,false);
        AverageOfN aoN;

        // Providing the times in the correct order (oldest first) is important. The source array
        // that is used to fill the result of "getTimes" is a circular queue, so test the cases
        // where the tail pointer ("AverageCalculator.mNext") is at the start, middle, end and just
        // beyond the end and then ensure that the tricky copy to "AverageOfN.mTimes" is correct.
        // Below, the possible values of "mNext" are 0, 1, 2, 3. It only matters once "N" times
        // have been added. The values of "mNext" are noted in comments to show that all are tested.
        // "mNext" is zero at the very start, but is 3 (just off the end of the array) instead of
        // zero after that, so the zero case is not directly testable.

        // Add less than the minimum required number of times. Average cannot be calculated.
        ac.addTimes(500, 250);
        aoN = ac.getAverageOfN();

        assertNull(aoN.getTimes());
        assertEquals(UNKNOWN, aoN.getAverage());
        assertEquals(-1, aoN.getBestTimeIndex());
        assertEquals(-1, aoN.getWorstTimeIndex());

        // Complete the first three times. Average can now be calculated.
        ac.addTime(150);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(new long[] { 500, 250, 150 }, aoN.getTimes()); // mNext == 3
        assertEquals(300, aoN.getAverage());
        assertEquals(-1, aoN.getBestTimeIndex());  // No elimination of best time for N=3.
        assertEquals(-1, aoN.getWorstTimeIndex()); // No elimination of worst time for N=3.

        // 1 DNF does not disqualify the result.
        ac.addTime(DNF);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(new long[] { 250, 150, DNF }, aoN.getTimes()); // mNext == 1
        assertEquals(200, aoN.getAverage());
        assertEquals(-1, aoN.getBestTimeIndex());  // No elimination of best time for N=3.
        assertEquals(-1, aoN.getWorstTimeIndex()); // No elimination of worst time for N=3.

        // 2 DNFs do not disqualify the result.
        ac.addTime(DNF);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(new long[] { 150, DNF, DNF }, aoN.getTimes()); // mNext == 2
        assertEquals(150, aoN.getAverage());
        assertEquals(-1, aoN.getBestTimeIndex());  // No elimination of best time for N=3.
        assertEquals(-1, aoN.getWorstTimeIndex()); // No elimination of worst time for N=3.

        // 3 DNFs disqualify the result.
        ac.addTime(DNF);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(new long[] { DNF, DNF, DNF }, aoN.getTimes()); // mNext == 3
        assertEquals(DNF, aoN.getAverage());
        assertEquals(-1, aoN.getBestTimeIndex());  // No elimination of best time for N=3.
        assertEquals(-1, aoN.getWorstTimeIndex()); // No elimination of worst time for N=3.
   }

    /**
     * Tests the {@link AverageOfN} class to ensure it presents the times in the correct order
     * and identifies the best and worst times correctly. This test covers the case where best
     * and worst times should not be identified because the value of "N" is low. DNFs disqualify
     * the average.
     *
     * @throws Exception If the test fails to run.
     */
    @Test
    public void testAverageOfNDetailsForThreeDisqualifyDNFs() throws Exception {
        final AverageCalculator ac = new AverageCalculator(3, 0, 0,true);
        AverageOfN aoN;

        // Add less than the minimum required number of times. Average cannot be calculated.
        ac.addTimes(500, 250);
        aoN = ac.getAverageOfN();

        assertNull(aoN.getTimes());
        assertEquals(UNKNOWN, aoN.getAverage());
        assertEquals(-1, aoN.getBestTimeIndex());
        assertEquals(-1, aoN.getWorstTimeIndex());

        // Complete the first three times. Average can now be calculated.
        ac.addTime(150);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(new long[] { 500, 250, 150 }, aoN.getTimes()); // mNext == 3
        assertEquals(300, aoN.getAverage());
        assertEquals(-1, aoN.getBestTimeIndex());  // No elimination of best time for N=3.
        assertEquals(-1, aoN.getWorstTimeIndex()); // No elimination of worst time for N=3.

        // 1 DNF disqualifies the result.
        ac.addTime(DNF);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(new long[] { 250, 150, DNF }, aoN.getTimes()); // mNext == 1
        assertEquals(DNF, aoN.getAverage());
        assertEquals(-1, aoN.getBestTimeIndex());  // No elimination of best time for N=3.
        assertEquals(-1, aoN.getWorstTimeIndex()); // No elimination of worst time for N=3.

        // 2 DNFs disqualify the result.
        ac.addTime(DNF);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(new long[] { 150, DNF, DNF }, aoN.getTimes()); // mNext == 2
        assertEquals(DNF, aoN.getAverage());
        assertEquals(-1, aoN.getBestTimeIndex());  // No elimination of best time for N=3.
        assertEquals(-1, aoN.getWorstTimeIndex()); // No elimination of worst time for N=3.

        // 3 DNFs disqualify the result.
        ac.addTime(DNF);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(new long[] { DNF, DNF, DNF }, aoN.getTimes()); // mNext == 3
        assertEquals(DNF, aoN.getAverage());
        assertEquals(-1, aoN.getBestTimeIndex());  // No elimination of best time for N=3.
        assertEquals(-1, aoN.getWorstTimeIndex()); // No elimination of worst time for N=3.

        // No DNFs and the result is valid again.
        ac.addTimes(100, 200, 600);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(new long[] { 100, 200, 600 }, aoN.getTimes()); // mNext == 3
        assertEquals(300, aoN.getAverage());
        assertEquals(-1, aoN.getBestTimeIndex());  // No elimination of best time for N=3.
        assertEquals(-1, aoN.getWorstTimeIndex()); // No elimination of worst time for N=3.
   }

    /**
     * Tests the {@link AverageOfN} class to ensure it presents the times in the correct order
     * and identifies the best and worst times correctly. This test covers the case where best
     * and worst times must be identified because the value of "N" is high enough to trigger the
     * calculation of a truncated mean. DNFs do not disqualify the average unless all times are
     * DNFs. The "best" time is not eliminated if there is only one non-DNF time present.
     *
     * @throws Exception If the test fails to run.
     */
    @Test
    public void testAverageOfNDetailsForFiveAllowDNFs() throws Exception {
        final AverageCalculator ac = new AverageCalculator(5, 20, 20,false);
        AverageOfN aoN;

        // Add less than the minimum required number of times. Average cannot be calculated.
        ac.addTimes(500, 150, 250, 600);
        aoN = ac.getAverageOfN();

        assertNull(aoN.getTimes());
        assertEquals(UNKNOWN, aoN.getAverage());
        assertEquals(-1, aoN.getBestTimeIndex());
        assertEquals(-1, aoN.getWorstTimeIndex());

        // Complete the first five times. Average can now be calculated.
        ac.addTime(350);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(new long[] { 500, 150, 250, 600, 350 }, aoN.getTimes()); // mNext == 5
        assertEquals(366, aoN.getAverage()); // Mean of 500+250+350. 150 and 600 are eliminated.
        assertEquals(1, aoN.getBestTimeIndex());  // 150
        assertEquals(3, aoN.getWorstTimeIndex()); // 600

        // 1 DNF does not disqualify the result. DNF becomes the "worst" time.
        ac.addTime(DNF);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(new long[] { 150, 250, 600, 350, DNF }, aoN.getTimes()); // mNext == 1
        assertEquals(400, aoN.getAverage()); // Mean of 250+600+350. 150 and DNF are eliminated.
        assertEquals(0, aoN.getBestTimeIndex());  // 150
        assertEquals(4, aoN.getWorstTimeIndex()); // DNF

        // 2 DNFs do not disqualify the result.
        ac.addTime(DNF);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(new long[] { 250, 600, 350, DNF, DNF }, aoN.getTimes()); // mNext == 2
        assertEquals(475, aoN.getAverage()); // Mean of 600+350. 250, DNF1 eliminated. DNF2 ignored.
        assertEquals(0, aoN.getBestTimeIndex());  // 250
        assertEquals(3, aoN.getWorstTimeIndex()); // First DNF

        // 3 DNFs do not disqualify the result.
        ac.addTime(DNF);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(new long[] { 600, 350, DNF, DNF, DNF }, aoN.getTimes()); // mNext == 3
        assertEquals(600, aoN.getAverage()); // 350, DNF1 eliminated. DNF2, DNF3 ignored.
        assertEquals(1, aoN.getBestTimeIndex());  // 350
        assertEquals(2, aoN.getWorstTimeIndex()); // First DNF

        // 4 DNFs do not disqualify the result, but no best time will be eliminated.
        ac.addTime(DNF);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(new long[] { 350, DNF, DNF, DNF, DNF }, aoN.getTimes()); // mNext == 4
        assertEquals(350, aoN.getAverage()); // All DNFs are ignored.
        assertEquals(-1, aoN.getBestTimeIndex()); // No elimination of the only non-DNF time.
        assertEquals(1, aoN.getWorstTimeIndex()); // First DNF

        // 5 DNFs disqualify the result. No eliminations.
        ac.addTime(DNF);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(new long[] { DNF, DNF, DNF, DNF, DNF }, aoN.getTimes()); // mNext == 5
        assertEquals(DNF, aoN.getAverage()); // Average is disqualified.
        assertEquals(-1, aoN.getBestTimeIndex());
        assertEquals(-1, aoN.getWorstTimeIndex());

        // Where all times are the same, the best and worst eliminations must not be the at the
        // same index. Expect the best to identified first and the worst second.
        ac.addTimes(100, 100, 100, 100, 100);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(new long[] { 100, 100, 100, 100, 100 }, aoN.getTimes());
        assertEquals(100, aoN.getAverage());
        assertEquals(0, aoN.getBestTimeIndex());  // First time is "best".
        assertEquals(1, aoN.getWorstTimeIndex()); // Next time is "worst".
   }

    /**
     * Tests the {@link AverageOfN} class to ensure it presents the times in the correct order
     * and identifies the best and worst times correctly. This test covers the case where best
     * and worst times must be identified because the value of "N" is high enough to trigger the
     * calculation of a truncated mean. More than one DNF disqualifies the average. The "best" and
     * worst times are identified in the same manner as when DNFs do not cause disqualifications,
     * even where the average is disqualified.
     *
     * @throws Exception If the test fails to run.
     */
    @Test
    public void testAverageOfNDetailsForFiveDisqualifyDNFs() throws Exception {
        final AverageCalculator ac = new AverageCalculator(5, 20, 20, true);
        AverageOfN aoN;

        // Add less than the minimum required number of times. Average cannot be calculated.
        ac.addTimes(500, 150, 250, 600);
        aoN = ac.getAverageOfN();

        assertNull(aoN.getTimes());
        assertEquals(UNKNOWN, aoN.getAverage());
        assertEquals(-1, aoN.getBestTimeIndex());
        assertEquals(-1, aoN.getWorstTimeIndex());

        // Complete the first five times. Average can now be calculated.
        ac.addTime(350);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(new long[] { 500, 150, 250, 600, 350 }, aoN.getTimes()); // mNext == 5
        assertEquals(366, aoN.getAverage()); // Mean of 500+250+350. 150 and 600 are eliminated.
        assertEquals(1, aoN.getBestTimeIndex());  // 150
        assertEquals(3, aoN.getWorstTimeIndex()); // 600

        // 1 DNF does not disqualify the result. DNF becomes the "worst" time.
        ac.addTime(DNF);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(new long[] { 150, 250, 600, 350, DNF }, aoN.getTimes()); // mNext == 1
        assertEquals(400, aoN.getAverage()); // Mean of 250+600+350. 150 and DNF are eliminated.
        assertEquals(0, aoN.getBestTimeIndex());  // 150
        assertEquals(4, aoN.getWorstTimeIndex()); // DNF

        // 2 DNFs disqualify the result.
        ac.addTime(DNF);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(new long[] { 250, 600, 350, DNF, DNF }, aoN.getTimes()); // mNext == 2
        assertEquals(DNF, aoN.getAverage());
        assertEquals(0, aoN.getBestTimeIndex());  // 250
        assertEquals(3, aoN.getWorstTimeIndex()); // First DNF

        // 3 DNFs disqualify the result.
        ac.addTime(DNF);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(new long[] { 600, 350, DNF, DNF, DNF }, aoN.getTimes()); // mNext == 3
        assertEquals(DNF, aoN.getAverage());
        assertEquals(1, aoN.getBestTimeIndex());  // 350
        assertEquals(2, aoN.getWorstTimeIndex()); // First DNF

        // 4 DNFs disqualify the result, and no best time will be identified.
        ac.addTime(DNF);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(new long[] { 350, DNF, DNF, DNF, DNF }, aoN.getTimes()); // mNext == 4
        assertEquals(DNF, aoN.getAverage());
        assertEquals(-1, aoN.getBestTimeIndex()); // No identification of the only non-DNF time.
        assertEquals(1, aoN.getWorstTimeIndex()); // First DNF

        // 5 DNFs disqualify the result. No eliminations.
        ac.addTime(DNF);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(new long[] { DNF, DNF, DNF, DNF, DNF }, aoN.getTimes()); // mNext == 5
        assertEquals(DNF, aoN.getAverage()); // Average is disqualified.
        assertEquals(-1, aoN.getBestTimeIndex());
        assertEquals(-1, aoN.getWorstTimeIndex());

        // Where all times are the same, the best and worst eliminations must not be the at the
        // same index. Expect the best to identified first and the worst second.
        ac.addTimes(100, 100, 100, 100, 100);
        aoN = ac.getAverageOfN();

        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(new long[] { 100, 100, 100, 100, 100 }, aoN.getTimes());
        assertEquals(100, aoN.getAverage());
        assertEquals(0, aoN.getBestTimeIndex());  // First time is "best".
        assertEquals(1, aoN.getWorstTimeIndex()); // Next time is "worst".
   }

    @Test
    public void testAssortedAverageOfHundredCalculations() throws Exception {
        final AverageCalculator ac = new AverageCalculator(100, 10, 10, false);
        AverageOfN aoN;

        // Add less than the minimum required number of times. Average cannot be calculated.
        ac.addTimes(4106, 6118, 7594, 5829, 4544, 8091, 3661, 7461, 9127, 4649, 6289, 5559, 4911, 2778, 3690, 7496, 6042, 4077, 6107, 9062, 8412, 7027, 2096, 8129, 7225, 7774, 9106, 8608, 2910, 3268, 6670, 9233, 5427, 4260, 2510, 4902, 2842, 1386, 9144, 2883, 6522, 1609, 3463, 7368, 2041, 8208, 5573, 2489, 3846, 9254, 4228, 6624, 6334, 2105, 7787, 2415, 7456, 8613, 3591, 7811, 2873, 4597, 7600, 3831, 1202, 8955, 3150, 1163, 7869, 4891, 3445, 4593, 1901, 8897, 9896, 8528, 4485, 9777, 5749, 8382, 3508, 1614, 6138, 2029, 4201, 4872, 7465, 8650, 8114, 6320, 7545, 9130, 3409, 1914, 7372, 4515, 5001, 3146, 7509);
        aoN = ac.getAverageOfN();

        assertNull(aoN.getTimes());
        assertEquals(UNKNOWN, aoN.getAverage());
        assertEquals(-1, aoN.getBestTimeIndex());
        assertEquals(-1, aoN.getWorstTimeIndex());

        ac.addTime(1007);
        aoN = ac.getAverageOfN();
        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(new long[] { 4106, 6118, 7594, 5829, 4544, 8091, 3661, 7461, 9127, 4649, 6289, 5559, 4911, 2778, 3690, 7496, 6042, 4077, 6107, 9062, 8412, 7027, 2096, 8129, 7225, 7774, 9106, 8608, 2910, 3268, 6670, 9233, 5427, 4260, 2510, 4902, 2842, 1386, 9144, 2883, 6522, 1609, 3463, 7368, 2041, 8208, 5573, 2489, 3846, 9254, 4228, 6624, 6334, 2105, 7787, 2415, 7456, 8613, 3591, 7811, 2873, 4597, 7600, 3831, 1202, 8955, 3150, 1163, 7869, 4891, 3445, 4593, 1901, 8897, 9896, 8528, 4485, 9777, 5749, 8382, 3508, 1614, 6138, 2029, 4201, 4872, 7465, 8650, 8114, 6320, 7545, 9130, 3409, 1914, 7372, 4515, 5001, 3146, 7509, 1007 }, aoN.getTimes()); // mNext == 5
        assertEquals(5562, aoN.getAverage());

        ac.addTime(9874); // Will eject 4106
        aoN = ac.getAverageOfN();
        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(new long[] { 6118, 7594, 5829, 4544, 8091, 3661, 7461, 9127, 4649, 6289, 5559, 4911, 2778, 3690, 7496, 6042, 4077, 6107, 9062, 8412, 7027, 2096, 8129, 7225, 7774, 9106, 8608, 2910, 3268, 6670, 9233, 5427, 4260, 2510, 4902, 2842, 1386, 9144, 2883, 6522, 1609, 3463, 7368, 2041, 8208, 5573, 2489, 3846, 9254, 4228, 6624, 6334, 2105, 7787, 2415, 7456, 8613, 3591, 7811, 2873, 4597, 7600, 3831, 1202, 8955, 3150, 1163, 7869, 4891, 3445, 4593, 1901, 8897, 9896, 8528, 4485, 9777, 5749, 8382, 3508, 1614, 6138, 2029, 4201, 4872, 7465, 8650, 8114, 6320, 7545, 9130, 3409, 1914, 7372, 4515, 5001, 3146, 7509, 1007, 9874 }, aoN.getTimes()); // mNext == 5
        assertEquals(15866, aoN.getmLowerTrimSum());
        assertEquals(93603, aoN.getmUpperTrimSum());
        assertEquals(559351 - (aoN.getmLowerTrimSum() + aoN.getmUpperTrimSum()), aoN.getmMiddleTrimSum());
        assertEquals(5623, aoN.getAverage());

        ac.addTimes(1678, 6298, 6); // Will eject 6118, 7594, 5829
        aoN = ac.getAverageOfN();
        assertEquals(ac.getN(), aoN.getTimes().length);
        assertArrayEquals(new long[] { 4544, 8091, 3661, 7461, 9127, 4649, 6289, 5559, 4911, 2778, 3690, 7496, 6042, 4077, 6107, 9062, 8412, 7027, 2096, 8129, 7225, 7774, 9106, 8608, 2910, 3268, 6670, 9233, 5427, 4260, 2510, 4902, 2842, 1386, 9144, 2883, 6522, 1609, 3463, 7368, 2041, 8208, 5573, 2489, 3846, 9254, 4228, 6624, 6334, 2105, 7787, 2415, 7456, 8613, 3591, 7811, 2873, 4597, 7600, 3831, 1202, 8955, 3150, 1163, 7869, 4891, 3445, 4593, 1901, 8897, 9896, 8528, 4485, 9777, 5749, 8382, 3508, 1614, 6138, 2029, 4201, 4872, 7465, 8650, 8114, 6320, 7545, 9130, 3409, 1914, 7372, 4515, 5001, 3146, 7509, 1007, 9874, 1678, 6298, 6 }, aoN.getTimes()); // mNext == 5
        assertEquals(13480, aoN.getmLowerTrimSum());
        assertEquals(93603, aoN.getmUpperTrimSum());
        assertEquals(547792 - (aoN.getmLowerTrimSum() + aoN.getmUpperTrimSum()), aoN.getmMiddleTrimSum());
        assertEquals(5508, aoN.getAverage());

    }


    @Test
    public void testLargeAverage() throws Exception {
        final AverageCalculator ac = new AverageCalculator(50, 5, 10, true);
        AverageOfN aoN;

        // Add less than the minimum required number of times. Average cannot be calculated.
        ac.addTimes(89950,95540,95990,72580,74560,92800,92420,83900,98010,89740,95070,82480,99060,81910,88290,72620,115280,96510,79570,79860,65980,79430,96970,89840,85730,74930,77310,91310,91990,97730,74350,66290,64820,78960,73680,86090,95390,75620,86390,79930,89150,88090,86570,73630,99780,91050,88750,89740,84670,92950,86830,78630,81930,86170,79480,87630,79190,90680,77230,80220,77070,79360,83350,100290,103240,80990,84190,75990,86490,77310,87960,72250,84340,82670,92400,97220,85430,87780,85710,94650,94970,80740,89290,75110,95410,111380,96660,74710,73920,90590,95820,103260,92030,87790,95400,99080,80910,90120,74520,89840,96060,74730,66320,88930,73740,84870,95960,105230,80370,80960,77450,103350,86730,106070,85510,72120,106750,84940,120410,97030,83840,94900,108510,87870,71520,82570,88600,101390,86790,84490,93170,93940,102440,99150,81370,85580,87860,94980,98780,81850,82610,78670,84810,89350,119210,76550,89270,98520,72340,99700,83060,70070,120210,78450,74580,84860,88730,84120,100840,98040,88520,106250,95910,90040,92360,83390,88580,81240,70700,103160,94160,107270,82590,79360,101450,92420,114950,83970,95780,102550,98690,73930,74890,85190,83980,72290,102640,77430,104500,130680,93820,89570,102470,93500,90470,113360,93550,99450,155980,121440,138660,113600,86400,96320,101420,106970,116600,109140,120990,144260,84500,92430,115610,104720,116010,170760,106910,118350,115150,123530,94250,116800,83410,90030,119140,86440,171490,176300,99300,113650,123400,123400,110880,124790,127890,125120,109420,119890,157070,108740,144950,130470,127060,103270,102450,124820,92750,99990,104990,123780,128360,95250,112700,99530,98620,116720,150670,107740,101990,144910,118340,134440,112190,103280,121440,114720,134100,106880,113970,113160,104740,73880,95690,85970,100150,102480,96730,67030,84900,86000,71500,88150,99320,92850,79970,103730,104490,77180,106040,115300,142720,88490,77750,89450,77590,170660,80350,88340,88030,102580,97660,88600,73960,84560,84880,84840,74140,98020,81770,95600);
        aoN = ac.getAverageOfN();
        assertEquals(ac.getN(), aoN.getTimes().length);
        assertEquals(83675, ac.getBestAverage());

        ac.addTimes(DNF, DNF, DNF, DNF, DNF); // The DNF threshold is 10% of N. With 5 DNFs, we should still be able to calculate a valid average
        assertEquals(5, ac.getNumDNFSolves());
        assertEquals(101912, ac.getCurrentAverage());

        ac.addTime(DNF); // 6 DNFs should disqualify the average.
        assertEquals(6, ac.getNumDNFSolves());
        assertEquals(DNF, ac.getCurrentAverage());

    }

    private Random rand            = new Random(0);
    private long[] mLargeTestTimes = rand.longs(100_000, 25_000, 30_000).toArray();

    /**
     * Used to test the efficiency of the algorithm only.
     * @throws Exception
     */
    @Test
    public void testVeryLargeAverage() throws Exception {
        final AverageCalculator ac = new AverageCalculator(1000, 5, 5, true);

        ac.addTimes(mLargeTestTimes);
    }

}
