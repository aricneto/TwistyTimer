package com.aricneto.twistytimer.fragment;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.aricneto.twistify.R;
import com.aricneto.twistytimer.adapter.TimeCursorAdapter;
import com.aricneto.twistytimer.database.DatabaseHandler;
import com.aricneto.twistytimer.database.TimeTaskLoader;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.utils.PuzzleUtils;
import com.github.clans.fab.FloatingActionButton;

import org.joda.time.DateTime;

import butterknife.Bind;
import butterknife.ButterKnife;

public class TimerListFragment extends BaseFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String PUZZLE         = "puzzle";
    private static final String PUZZLE_SUBTYPE = "puzzle_type";
    private static final String HISTORY        = "history";

    private static final int TASK_LOADER_ID = 14;


    private String currentPuzzle;
    private String currentPuzzleSubtype;

    DatabaseHandler dbHandler;

    private TimeCursorAdapter timeCursorAdapter;
    private TimeTaskLoader    timeTaskLoader;

    // True if you want to search history, false if you only want to search session
    boolean history;

    @Bind(R.id.list)         RecyclerView         recyclerView;
    @Bind(R.id.nothing_here) ImageView            nothingHere;
    @Bind(R.id.nothing_text) TextView             nothingText;
    @Bind(R.id.fab_addTime)  FloatingActionButton fabAddTime;

    @Bind(R.id.send_to_history_card) CardView moveToHistory;
    @Bind(R.id.clear_button)         TextView clearButton;
    @Bind(R.id.divider01)            View     dividerView;
    @Bind(R.id.archive_button)       TextView archiveButton;

    private Context mContext;

    // Receives broadcasts from the timer
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isAdded()) { // The fragment has to check if it is attached to an activity. Removing this will bug the app
                switch (intent.getStringExtra("action")) {
                    case "REFRESH TIME":
                    case "TIME UPDATED":
                        resetList();
                        break;
                    case "TIME ADDED":
                        if (! history)
                            resetList();
                        break;
                    case "HISTORY":
                        history = ! history;
                        resetList();
                        break;

                    case "UNSELECT ALL":
                        resetList();
                        break;
                    case "DELETE SELECTED":
                        timeCursorAdapter.deleteAllSelected();
                        break;
                }
            }
        }
    };

    public TimerListFragment() {
        // Required empty public constructor
    }

    // We have to put a boolean history here because it resets when we change puzzles.
    public static TimerListFragment newInstance(String puzzle, String puzzleType, boolean history) {
        TimerListFragment fragment = new TimerListFragment();
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_time_list, container, false);
        ButterKnife.bind(this, rootView);
        mContext = getActivity().getApplicationContext();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean clearEnabled = sharedPreferences.getBoolean("clearEnabled", false);

        if (clearEnabled) {
            dividerView.setVisibility(View.VISIBLE);
            clearButton.setVisibility(View.VISIBLE);
            archiveButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_timer_sand_black_18dp, 0, 0, 0);
        }

        fabAddTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new MaterialDialog.Builder(getContext())
                        .title(R.string.add_time)
                        .input(getString(R.string.add_time_hint), "", false, new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(MaterialDialog dialog, CharSequence input) {
                                int time = PuzzleUtils.parseTime(input.toString());
                                dbHandler.addSolve(new Solve(time, currentPuzzle,
                                        currentPuzzleSubtype, new DateTime().getMillis(), "", PuzzleUtils.NO_PENALTY, "", false));
                                Intent sendIntent = new Intent("TIMELIST");
                                sendIntent.putExtra("action", "TIME ADDED");
                                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(sendIntent);

                            }
                        })
                        .positiveText(R.string.action_add)
                        .negativeText(R.string.action_cancel)
                        .show();
            }
        });

        archiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Spannable text = new SpannableString(getString(R.string.move_solves_to_history_content) + "  ");
                ImageSpan icon = new ImageSpan(getContext(), R.drawable.ic_icon_history_demonstration);
                text.setSpan(icon, text.length() - 1, text.length(), 0);

                new MaterialDialog.Builder(getContext())
                        .title(R.string.move_solves_to_history)
                        .content(text)
                        .positiveText(R.string.action_move)
                        .negativeText(R.string.action_cancel)
                        .neutralColor(ContextCompat.getColor(getContext(), R.color.black_secondary))
                        .negativeColor(ContextCompat.getColor(getContext(), R.color.black_secondary))
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(MaterialDialog dialog, DialogAction which) {
                                dbHandler.moveAllSolvesToHistory(currentPuzzle, currentPuzzleSubtype);
                                Intent sendIntent = new Intent("TIMELIST");
                                sendIntent.putExtra("action", "MOVED TO HISTORY");
                                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(sendIntent);

                                resetList();
                            }
                        })
                        .show();
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new MaterialDialog.Builder(getContext())
                        .title(R.string.remove_session_title)
                        .content(R.string.remove_session_confirmation_content)
                        .positiveText(R.string.action_remove)
                        .negativeText(R.string.action_cancel)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(MaterialDialog dialog, DialogAction which) {
                                dbHandler.deleteAllFromSession(currentPuzzle, currentPuzzleSubtype);

                                resetList();

                            }
                        })
                        .show();
            }
        });

        setupRecyclerView();

        getTaskLoader();

        getLoaderManager().initLoader(TASK_LOADER_ID, null, this);

        // Register a receiver to update if something has changed
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mReceiver, new IntentFilter("TIMELIST"));

        return rootView;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // To fix memory leaks
        dbHandler.closeDB();
        ButterKnife.unbind(this);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mReceiver);
        getLoaderManager().destroyLoader(TASK_LOADER_ID);
    }

    public void resetList() {
        getTaskLoader();
        getLoaderManager().restartLoader(TASK_LOADER_ID, null, this);
    }

    /**
     * This class gets the class loader appropriate to
     * the params set on newInstance
     */
    private void getTaskLoader() {
        timeTaskLoader = new TimeTaskLoader(mContext, currentPuzzle, currentPuzzleSubtype, history);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return timeTaskLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        timeCursorAdapter.swapCursor(cursor);
        recyclerView.getAdapter().notifyDataSetChanged();
        setEmptyState(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        timeCursorAdapter.swapCursor(null);
    }

    public void setEmptyState(Cursor cursor) {
        if (cursor.getCount() == 0) {
            nothingHere.setVisibility(View.VISIBLE);
            nothingText.setVisibility(View.VISIBLE);
            moveToHistory.setVisibility(View.GONE);
            if (history) {
                nothingHere.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.notherehistory));
                nothingText.setText(R.string.list_empty_state_message_history);
            } else {
                nothingHere.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.nothere2));
                nothingText.setText(R.string.list_empty_state_message);
            }
        } else {
            nothingHere.setVisibility(View.INVISIBLE);
            nothingText.setVisibility(View.INVISIBLE);
            if (history)
                moveToHistory.setVisibility(View.GONE);
            else
                moveToHistory.setVisibility(View.VISIBLE);
        }
    }

    private void setupRecyclerView() {
        Activity parentActivity = getActivity();

        timeCursorAdapter = new TimeCursorAdapter(getActivity(), null, this);

        // Set different managers to support different orientations
        StaggeredGridLayoutManager gridLayoutManagerHorizontal =
                new StaggeredGridLayoutManager(6, StaggeredGridLayoutManager.VERTICAL);
        StaggeredGridLayoutManager gridLayoutManagerVertical =
                new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL);

        // Adapt to orientation
        if (parentActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
            recyclerView.setLayoutManager(gridLayoutManagerVertical);
        else
            recyclerView.setLayoutManager(gridLayoutManagerHorizontal);

        recyclerView.setAdapter(timeCursorAdapter);

    }


}
