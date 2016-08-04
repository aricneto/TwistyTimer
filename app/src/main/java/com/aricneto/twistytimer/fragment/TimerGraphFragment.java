package com.aricneto.twistytimer.fragment;


import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.CardView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.database.DatabaseHandler;
import com.aricneto.twistytimer.spans.TimeFormatter;
import com.aricneto.twistytimer.utils.Statistics;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

import static com.aricneto.twistytimer.utils.AverageCalculator.tr;
import static com.aricneto.twistytimer.utils.PuzzleUtils.convertTimeToString;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link TimerGraphFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TimerGraphFragment extends Fragment {

    private static final String PUZZLE         = "puzzle";
    private static final String PUZZLE_SUBTYPE = "puzzle_type";
    private static final String HISTORY        = "history";

    private String  currentPuzzle;
    private String  currentPuzzleSubtype;
    private Context mContext;

    @Bind(R.id.linechart)           LineChart lineChartView;
    @Bind(R.id.personalBestTimes)   TextView  personalBestTimes;
    @Bind(R.id.sessionBestTimes)    TextView  sessionBestTimes;
    @Bind(R.id.sessionCurrentTimes) TextView  sessionCurrentTimes;

    @Bind(R.id.progressSpinner) MaterialProgressBar progressBar;
    @Bind(R.id.refreshText)     TextView            refreshText;
    @Bind(R.id.statsTable)      CardView            statsTable;

    // Things that must be hidden/shown when refreshing the card.
    // The names don't matter since we're just going to show/hide them anyway
    @Bind(R.id.personalBestTitle)   View v1;
    @Bind(R.id.sessionBestTitle)    View v2;
    @Bind(R.id.sessionCurrentTitle) View v3;
    @Bind(R.id.horizontalDivider02) View v4;
    @Bind(R.id.verticalDivider02)   View v5;
    @Bind(R.id.verticalDivider03)   View v6;

    DatabaseHandler dbHandler;

    private boolean history;

    // To prevent an user from crashing the app by refreshing really fast
    private boolean refreshLocked;

    // Receives broadcasts from the timer
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isAdded()) { // The fragment has to check if it is attached to an activity. Removing this will bug the app
                switch (intent.getStringExtra("action")) {
                    case "MOVED TO HISTORY":
                    case "TIME UPDATED":
                    case "REFRESH TIME":
                        generateChart();
                        if (refreshText.getVisibility() == View.GONE) {
                            refreshText.setVisibility(View.VISIBLE);
                            toggleCardStats(View.GONE);
                        }
                        break;
                    case "TIME ADDED":
                        if (! history)
                            generateChart();
                        if (refreshText.getVisibility() == View.GONE) {
                            refreshText.setVisibility(View.VISIBLE);
                            toggleCardStats(View.GONE);
                        }
                        break;
                    case "HISTORY":
                        history = ! history;
                        generateChart();
                        break;
                }
            }
        }
    };


    public TimerGraphFragment() {
        // Required empty public constructor
    }

    // We have to put a boolean history here because it resets when we change puzzles.
    public static TimerGraphFragment newInstance(String puzzle, String puzzleType, boolean history) {
        TimerGraphFragment fragment = new TimerGraphFragment();
        Bundle args = new Bundle();
        args.putString(PUZZLE, puzzle);
        args.putBoolean(HISTORY, history);
        args.putString(PUZZLE_SUBTYPE, puzzleType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentPuzzle = getArguments().getString(PUZZLE);
            currentPuzzleSubtype = getArguments().getString(PUZZLE_SUBTYPE);
            history = getArguments().getBoolean(HISTORY);
        }
        dbHandler = new DatabaseHandler(getContext());
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mReceiver, new IntentFilter("TIMELIST"));
        mContext = getContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_timer_graph, container, false);
        ButterKnife.bind(this, root);

        statsTable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (! refreshLocked) {
                    refreshLocked = true;
                    calculateStats();
                }
            }
        });

        // Setting for landscape mode
        if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            root.post(new Runnable() {
                @Override
                public void run() {
                    if (container != null) {
                        LineChart.LayoutParams params = lineChartView.getLayoutParams();
                        params.height = container.getHeight();
                        lineChartView.setLayoutParams(params);
                        lineChartView.requestLayout();
                        root.findViewById(R.id.graphScroll).setScrollY(params.height / 2);
                    }
                }
            });
        }

        // Setting the chart up
        lineChartView.setPinchZoom(true);
        YAxis axisLeft = lineChartView.getAxisLeft();
        XAxis xAxis = lineChartView.getXAxis();

        lineChartView.setBackgroundColor(Color.TRANSPARENT);
        lineChartView.setDrawGridBackground(false);
        xAxis.setDrawGridLines(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setAxisLineColor(ContextCompat.getColor(mContext, R.color.white_secondary_icon));
        xAxis.setDrawAxisLine(true);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setAvoidFirstLastClipping(true);
        lineChartView.getAxisRight().setEnabled(false);
        lineChartView.getLegend().setEnabled(false);
        lineChartView.setDescription("");

        axisLeft.setDrawLimitLinesBehindData(true);
        axisLeft.setDrawGridLines(true);
        axisLeft.setDrawAxisLine(true);
        axisLeft.setTextColor(Color.WHITE);
        axisLeft.enableGridDashedLine(10f, 8f, 0f);
        axisLeft.setValueFormatter(new TimeFormatter());
        axisLeft.setAxisLineColor(ContextCompat.getColor(mContext, R.color.white_secondary_icon));
        axisLeft.setGridColor(ContextCompat.getColor(mContext, R.color.white_secondary_icon));

        generateChart();

        return root;
    }

    private void calculateStats() {
        new CalculateStats().execute();
    }

    private void generateChart() {
        new GenerateSolveList().execute(currentPuzzle, currentPuzzleSubtype);
    }

    private void toggleCardStats(int visibility) {
        v1.setVisibility(visibility);
        v2.setVisibility(visibility);
        v3.setVisibility(visibility);
        v4.setVisibility(visibility);
        v5.setVisibility(visibility);
        v6.setVisibility(visibility);

        personalBestTimes.setVisibility(visibility);
        sessionBestTimes.setVisibility(visibility);
        sessionCurrentTimes.setVisibility(visibility);
    }

    /**
     * Generate a list of solves for the chart
     */
    private class GenerateSolveList extends AsyncTask<String, Void, Pair<ArrayList<Entry>, ArrayList<String>>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Pair<ArrayList<Entry>, ArrayList<String>> doInBackground(String... params) {
            ArrayList<Entry> yVals = new ArrayList<>();
            ArrayList<String> xVals = new ArrayList<>();

            Pair<ArrayList<Entry>, ArrayList<String>> tempPair = new Pair<>(yVals, xVals);

            Cursor cursor = dbHandler.getAllSolvesFrom(currentPuzzle, currentPuzzleSubtype, history);

            // Looping through all rows and adding to list
            int timeIndex = cursor.getColumnIndex(DatabaseHandler.KEY_TIME);
            int penaltyIndex = cursor.getColumnIndex(DatabaseHandler.KEY_DATE);
            int count = 0;
            if (cursor.getCount() > 0) {
                if (cursor.moveToFirst()) {
                    do {
                        tempPair.first.add(new Entry((float) cursor.getInt(timeIndex) / 1000,
                                count));
                        tempPair.second.add(new DateTime(cursor.getLong(penaltyIndex)).toString("dd'/'MM"));
                        count++;
                    } while (cursor.moveToNext());
                }
            }

            // Adding the mean to the string arraylist, so we don't have
            // to create another variable to store it in (remember to remove it from the list in the next step)
            tempPair.second.add(String.valueOf(dbHandler.getMean(! history, currentPuzzle, currentPuzzleSubtype) / 1000));

            cursor.close();
            return tempPair;
        }

        @Override
        protected void onPostExecute(Pair<ArrayList<Entry>, ArrayList<String>> objects) {
            super.onPostExecute(objects);

            //Getting the mean and removing it from the list so it doesn't interfere with the times
            float mean = Float.parseFloat(objects.second.get(objects.second.size() - 1));
            objects.second.remove(objects.second.size() - 1);

            LineDataSet lineDataSet = new LineDataSet(objects.first, "yVals");

            lineChartView.getAxisLeft().removeAllLimitLines();
            // Mean line
            LimitLine ll = new LimitLine(mean, mContext.getString(R.string.graph_mean));
            ll.setLineColor(ContextCompat.getColor(mContext, R.color.yellow_material_700));
            ll.setLineWidth(1f);
            ll.enableDashedLine(20f, 10f, 0f);
            ll.setLabelPosition(LimitLine.LimitLabelPosition.LEFT_TOP);
            ll.setTextColor(ContextCompat.getColor(mContext, R.color.yellow_material_700));
            ll.setTextSize(12f);
            lineChartView.getAxisLeft().addLimitLine(ll);

            lineDataSet.setLineWidth(2f);
            lineDataSet.enableDashedLine(10f, 10f, 0);
            lineDataSet.setDrawCircles(false);
            //lineDataSet.setCircleRadius(3f);
            lineDataSet.setColor(Color.WHITE);
            lineDataSet.setHighlightEnabled(false);
            //lineDataSet.setCircleColor(Color.WHITE);
            lineDataSet.setDrawValues(false);

            LineData lineData = new LineData(objects.second, lineDataSet);

            lineChartView.setData(lineData);
            // Animates and refreshes the chart
            lineChartView.animateY(1000);
        }
    }

    private class CalculateStats extends AsyncTask<Void, Void, Statistics> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            refreshText.setVisibility(View.GONE);
            toggleCardStats(View.GONE);
        }

        @Override
        protected Statistics doInBackground(Void... voids) {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

            final Statistics stats = Statistics.newStandardStatistics();

            dbHandler.populateAllTimeStatistics(currentPuzzle, currentPuzzleSubtype, stats);

            return stats;
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(Statistics stats) {
            super.onPostExecute(stats);

            // "tr()" converts from "AverageCalculator.UNKNOWN" and "AverageCalculator.DNF" to the
            // values needed by "convertTimeToString".

            String allTimeBestAvg3 = convertTimeToString(
                    tr(stats.getAverageOf(3, false).getBestAverage()));
            String allTimeBestAvg5 = convertTimeToString(
                    tr(stats.getAverageOf(5, false).getBestAverage()));
            String allTimeBestAvg12 = convertTimeToString(
                    tr(stats.getAverageOf(12, false).getBestAverage()));
            String allTimeBestAvg50 = convertTimeToString(
                    tr(stats.getAverageOf(50, false).getBestAverage()));
            String allTimeBestAvg100 = convertTimeToString(
                    tr(stats.getAverageOf(100, false).getBestAverage()));
            String allTimeBestAvg1000 = convertTimeToString(
                    tr(stats.getAverageOf(1_000, false).getBestAverage()));

            String allTimeMeanTime = convertTimeToString(tr(stats.getAllTimeMeanTime()));
            String allTimeBestTime = convertTimeToString(tr(stats.getAllTimeBestTime()));
            String allTimeWorstTime = convertTimeToString(tr(stats.getAllTimeWorstTime()));
            // Format count using appropriate grouping separators, e.g., "1,234", not "1234".
            String allTimeCount
                    = String.format(Locale.getDefault(), "%,d", stats.getAllTimeNumSolves());

            String sessionBestAvg3 = convertTimeToString(
                    tr(stats.getAverageOf(3, true).getBestAverage()));
            String sessionBestAvg5 = convertTimeToString(
                    tr(stats.getAverageOf(5, true).getBestAverage()));
            String sessionBestAvg12 = convertTimeToString(
                    tr(stats.getAverageOf(12, true).getBestAverage()));
            String sessionBestAvg50 = convertTimeToString(
                    tr(stats.getAverageOf(50, true).getBestAverage()));
            String sessionBestAvg100 = convertTimeToString(
                    tr(stats.getAverageOf(100, true).getBestAverage()));
            String sessionBestAvg1000 = convertTimeToString(
                    tr(stats.getAverageOf(1_000, true).getBestAverage()));

            String sessionMeanTime = convertTimeToString(tr(stats.getSessionMeanTime()));
            String sessionBestTime = convertTimeToString(tr(stats.getSessionBestTime()));
            String sessionWorstTime = convertTimeToString(tr(stats.getSessionWorstTime()));
            String sessionCount
                    = String.format(Locale.getDefault(), "%,d", stats.getSessionNumSolves());

            String sessionCurrentAvg3 = convertTimeToString(
                    tr(stats.getAverageOf(3, true).getCurrentAverage()));
            String sessionCurrentAvg5 = convertTimeToString(
                    tr(stats.getAverageOf(5, true).getCurrentAverage()));
            String sessionCurrentAvg12 = convertTimeToString(
                    tr(stats.getAverageOf(12, true).getCurrentAverage()));
            String sessionCurrentAvg50 = convertTimeToString(
                    tr(stats.getAverageOf(50, true).getCurrentAverage()));
            String sessionCurrentAvg100 = convertTimeToString(
                    tr(stats.getAverageOf(100, true).getCurrentAverage()));
            String sessionCurrentAvg1000 = convertTimeToString(
                    tr(stats.getAverageOf(1_000, true).getCurrentAverage()));

            personalBestTimes.setText(
                    allTimeBestAvg3 + "\n" +
                            allTimeBestAvg5 + "\n" +
                            allTimeBestAvg12 + "\n" +
                            allTimeBestAvg50 + "\n" +
                            allTimeBestAvg100 + "\n" +
                            allTimeBestAvg1000 + "\n" +
                            allTimeMeanTime + "\n" +
                            allTimeBestTime + "\n" +
                            allTimeWorstTime + "\n" +
                            allTimeCount);
            sessionBestTimes.setText(
                    sessionBestAvg3 + "\n" +
                            sessionBestAvg5 + "\n" +
                            sessionBestAvg12 + "\n" +
                            sessionBestAvg50 + "\n" +
                            sessionBestAvg100 + "\n" +
                            sessionBestAvg1000 + "\n" +
                            sessionMeanTime + "\n" +
                            sessionBestTime + "\n" +
                            sessionWorstTime + "\n" +
                            sessionCount);
            sessionCurrentTimes.setText(
                    sessionCurrentAvg3 + "\n" +
                            sessionCurrentAvg5 + "\n" +
                            sessionCurrentAvg12 + "\n" +
                            sessionCurrentAvg50 + "\n" +
                            sessionCurrentAvg100 + "\n" +
                            sessionCurrentAvg1000 + "\n" +
                            // Last few are the same for "Session Best" and "Session Current".
                            sessionMeanTime + "\n" +
                            sessionBestTime + "\n" +
                            sessionWorstTime + "\n" +
                            sessionCount);

            toggleCardStats(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            refreshLocked = false;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        dbHandler.closeDB();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mReceiver);
    }
}
