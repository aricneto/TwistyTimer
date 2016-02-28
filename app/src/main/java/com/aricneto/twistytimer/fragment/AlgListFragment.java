package com.aricneto.twistytimer.fragment;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.adapter.AlgCursorAdapter;
import com.aricneto.twistytimer.database.AlgTaskLoader;

import butterknife.Bind;
import butterknife.ButterKnife;

public class AlgListFragment extends BaseFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int    TASK_LOADER_ID = 14;
    private static final String KEY_SUBSET     = "subset";

    @Bind(R.id.toolbar)         Toolbar             toolbar;

    private String currentSubset;


    private AlgCursorAdapter algCursorAdapter;
    private AlgTaskLoader    algTaskLoader;

    @Bind(R.id.list) RecyclerView recyclerView;

    private Context mContext;

    // Receives broadcasts from the timer
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isAdded()) { // The fragment has to check if it is attached to an activity. Removing this will bug the app
                switch (intent.getStringExtra("action")) {
                    case "ALG ADDED":
                        resetList();
                        break;
                }
            }
        }
    };

    public AlgListFragment() {
        // Required empty public constructor
    }

    // We have to put a boolean history here because it resets when we change puzzles.
    public static AlgListFragment newInstance(String subset) {
        AlgListFragment fragment = new AlgListFragment();
        Bundle args = new Bundle();
        args.putString(KEY_SUBSET, subset);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentSubset = getArguments().getString(KEY_SUBSET);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_alg_list, container, false);
        ButterKnife.bind(this, rootView);
        mContext = getActivity().getApplicationContext();

        toolbar.setTitle(currentSubset);

        setupToolbarForFragment(toolbar);

        setupRecyclerView();

        getTaskLoader();

        getLoaderManager().initLoader(TASK_LOADER_ID, null, this);

        // Register a receiver to update if something has changed
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mReceiver, new IntentFilter("ALGLIST"));

        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // To fix memory leaks
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
        algTaskLoader = new AlgTaskLoader(mContext, currentSubset);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return algTaskLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        algCursorAdapter.swapCursor(cursor);
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        algCursorAdapter.swapCursor(null);
    }

    private void setupRecyclerView() {
        Activity parentActivity = getActivity();

        algCursorAdapter = new AlgCursorAdapter(getActivity(), null, this);

        // Set different managers to support different orientations
        StaggeredGridLayoutManager gridLayoutManagerHorizontal =
                new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL);
        StaggeredGridLayoutManager gridLayoutManagerVertical =
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);

        // Adapt to orientation
        if (parentActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
            recyclerView.setLayoutManager(gridLayoutManagerVertical);
        else
            recyclerView.setLayoutManager(gridLayoutManagerHorizontal);

        recyclerView.setAdapter(algCursorAdapter);

    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }
}
