package com.aricneto.twistytimer.fragment;


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
import com.aricneto.twistytimer.utils.PuzzleUtils;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.joda.time.DateTime;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

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

    @Bind(R.id.linechart)         LineChart lineChartView;
    @Bind(R.id.personalBestTimes) TextView  personalBestTimes;
    @Bind(R.id.sessionBestTimes)  TextView  sessionBestTimes;

    @Bind(R.id.progressSpinner) MaterialProgressBar progressBar;
    @Bind(R.id.refreshText)     TextView            refreshText;
    @Bind(R.id.bestCard)        CardView            bestsCard;

    // Things that must be hidden/shown when refreshing the card.
    // The names don't matter since we're just going to show/hide them anyway
    @Bind(R.id.globalBestTitle)  View rl1;
    @Bind(R.id.divider03)        View rl2;
    @Bind(R.id.divider04)        View rl3;
    @Bind(R.id.sessionBestTitle) View r14;

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
                    case "TIME ADDED":
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

        // Bests stats
        bestsCard.setOnClickListener(new View.OnClickListener() {
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
                        root.findViewById(R.id.graphScroll).setScrollY(params.height/2);
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
        rl1.setVisibility(visibility);
        rl2.setVisibility(visibility);
        rl3.setVisibility(visibility);
        r14.setVisibility(visibility);
        personalBestTimes.setVisibility(visibility);
        sessionBestTimes.setVisibility(visibility);
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
            lineDataSet.setCircleRadius(3f);
            lineDataSet.setColor(Color.WHITE);
            lineDataSet.setHighlightEnabled(false);
            lineDataSet.setCircleColor(Color.WHITE);
            lineDataSet.setDrawValues(false);

            LineData lineData = new LineData(objects.second, lineDataSet);

            lineChartView.setData(lineData);
            // Animates and refreshes the chart
            lineChartView.animateXY(1000, 1000);
        }
    }

    private class CalculateStats extends AsyncTask<Void, Void, int[]> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            refreshText.setVisibility(View.GONE);
            toggleCardStats(View.GONE);
        }

        @Override
        protected int[] doInBackground(Void... voids) {
            int BestAvg3 = dbHandler.getBestAverageOf(3, currentPuzzle, currentPuzzleSubtype, true);
            int BestAvg5 = dbHandler.getBestAverageOf(5, currentPuzzle, currentPuzzleSubtype, true);
            int BestAvg12 = dbHandler.getBestAverageOf(12, currentPuzzle, currentPuzzleSubtype, true);
            int BestAvg100 = dbHandler.getBestAverageOf(100, currentPuzzle, currentPuzzleSubtype, false);
            int BestAvg50 = dbHandler.getBestAverageOf(50, currentPuzzle, currentPuzzleSubtype, false);
            int BestAvg1000 = dbHandler.getBestAverageOf(1000, currentPuzzle, currentPuzzleSubtype, false);
            int BestMean = dbHandler.getMean(false, currentPuzzle, currentPuzzleSubtype);
            int BestBest = dbHandler.getBestOrWorstTime(true, false, currentPuzzle, currentPuzzleSubtype);
            int BestWorst = dbHandler.getBestOrWorstTime(false, false, currentPuzzle, currentPuzzleSubtype);
            int BestSolveCount = dbHandler.getSolveCount(currentPuzzle, currentPuzzleSubtype, false);

            int SessionAvg3 = dbHandler.getFastAverageOf(3, currentPuzzle, currentPuzzleSubtype, true);
            int SessionAvg5 = dbHandler.getTruncatedAverageOf(5, currentPuzzle, currentPuzzleSubtype, true);
            int SessionAvg12 = dbHandler.getTruncatedAverageOf(12, currentPuzzle, currentPuzzleSubtype, true);
            int SessionAvg100 = dbHandler.getTruncatedAverageOf(100, currentPuzzle, currentPuzzleSubtype, false);
            int SessionAvg50 = dbHandler.getTruncatedAverageOf(50, currentPuzzle, currentPuzzleSubtype, false);
            int SessionAvg1000 = dbHandler.getTruncatedAverageOf(1000, currentPuzzle, currentPuzzleSubtype, false);
            int SessionMean = dbHandler.getMean(true, currentPuzzle, currentPuzzleSubtype);
            int SessionBest = dbHandler.getBestOrWorstTime(true, true, currentPuzzle, currentPuzzleSubtype);
            int SessionWorst = dbHandler.getBestOrWorstTime(false, true, currentPuzzle, currentPuzzleSubtype);
            int SessionSolveCount = dbHandler.getSolveCount(currentPuzzle, currentPuzzleSubtype, true);

            return new int[] { BestAvg5, BestAvg12, BestAvg100, BestMean, BestBest, BestWorst, BestSolveCount,
                               SessionAvg5, SessionAvg12, SessionAvg100, SessionMean, SessionBest, SessionWorst, SessionSolveCount,
                               BestAvg50, BestAvg1000, SessionAvg50, SessionAvg1000, BestAvg3, SessionAvg3 };
        }

        @Override
        protected void onPostExecute(int[] times) {
            super.onPostExecute(times);

            String BestAvg3 = PuzzleUtils.convertTimeToString(times[18]);
            String BestAvg5 = PuzzleUtils.convertTimeToString(times[0]);
            String BestAvg12 = PuzzleUtils.convertTimeToString(times[1]);
            String BestAvg50 = PuzzleUtils.convertTimeToString(times[14]);
            String BestAvg100 = PuzzleUtils.convertTimeToString(times[2]);
            String BestAvg1000 = PuzzleUtils.convertTimeToString(times[15]);
            String BestMean = PuzzleUtils.convertTimeToString(times[3]);
            String BestBest = PuzzleUtils.convertTimeToString(times[4]);
            String BestWorst = PuzzleUtils.convertTimeToString(times[5]);

            String SessionAvg3 = PuzzleUtils.convertTimeToString(times[19]);
            String SessionAvg5 = PuzzleUtils.convertTimeToString(times[7]);
            String SessionAvg12 = PuzzleUtils.convertTimeToString(times[8]);
            String SessionAvg50 = PuzzleUtils.convertTimeToString(times[16]);
            String SessionAvg100 = PuzzleUtils.convertTimeToString(times[9]);
            String SessionAvg1000 = PuzzleUtils.convertTimeToString(times[17]);
            String SessionMean = PuzzleUtils.convertTimeToString(times[10]);
            String SessionBest = PuzzleUtils.convertTimeToString(times[11]);
            String SessionWorst = PuzzleUtils.convertTimeToString(times[12]);

            // The following code makes androidstudio throw a fit, but it's alright since we're not going to be translating NUMBERS.
            personalBestTimes.setText(
                    BestAvg3 + "\n" +
                            BestAvg5 + "\n" +
                            BestAvg12 + "\n" +
                            BestAvg50 + "\n" +
                            BestAvg100 + "\n" +
                            BestAvg1000 + "\n" +
                            BestMean + "\n" +
                            BestBest + "\n" +
                            BestWorst + "\n" +
                            times[6]);
            sessionBestTimes.setText(
                    SessionAvg3 + "\n" +
                            SessionAvg5 + "\n" +
                            SessionAvg12 + "\n" +
                            SessionAvg50 + "\n" +
                            SessionAvg100 + "\n" +
                            SessionAvg1000 + "\n" +
                            SessionMean + "\n" +
                            SessionBest + "\n" +
                            SessionWorst + "\n" +
                            times[13]);

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
