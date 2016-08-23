package com.aricneto.twistytimer.fragment;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.adapter.AlgCursorAdapter;
import com.aricneto.twistytimer.database.AlgTaskLoader;
import com.aricneto.twistytimer.utils.TTIntent.TTFragmentBroadcastReceiver;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static com.aricneto.twistytimer.utils.TTIntent.ACTION_ALGS_MODIFIED;
import static com.aricneto.twistytimer.utils.TTIntent.CATEGORY_ALG_DATA_CHANGES;
import static com.aricneto.twistytimer.utils.TTIntent.registerReceiver;
import static com.aricneto.twistytimer.utils.TTIntent.unregisterReceiver;

public class AlgListFragment extends BaseFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int    TASK_LOADER_ID = 14;
    private static final String KEY_SUBSET     = "subset";

    private Unbinder mUnbinder;
    @BindView(R.id.toolbar) Toolbar toolbar;

    private String currentSubset;


    private AlgCursorAdapter algCursorAdapter;
    private AlgTaskLoader    algTaskLoader;

    @BindView(R.id.list) RecyclerView recyclerView;

    private Context mContext;

    // Receives broadcasts about changes to the algorithm data.
    private TTFragmentBroadcastReceiver mAlgDataChangedReceiver
            = new TTFragmentBroadcastReceiver(this, CATEGORY_ALG_DATA_CHANGES) {
        @Override
        public void onReceiveWhileAdded(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_ALGS_MODIFIED:
                    resetList();
                    break;
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
        mUnbinder = ButterKnife.bind(this, rootView);
        mContext = getActivity().getApplicationContext();

        toolbar.setTitle(currentSubset);

        setupToolbarForFragment(toolbar);

        setupRecyclerView();

        getTaskLoader();

        getLoaderManager().initLoader(TASK_LOADER_ID, null, this);

        // Register a receiver to update if something has changed
        registerReceiver(mAlgDataChangedReceiver);

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // To fix memory leaks
        unregisterReceiver(mAlgDataChangedReceiver);
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
}
