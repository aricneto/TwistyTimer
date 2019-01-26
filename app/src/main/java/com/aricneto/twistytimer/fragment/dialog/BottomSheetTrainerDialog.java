package com.aricneto.twistytimer.fragment.dialog;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.activity.MainActivity;
import com.aricneto.twistytimer.adapter.AlgCursorAdapter;
import com.aricneto.twistytimer.adapter.TrainerCursorAdapter;
import com.aricneto.twistytimer.database.AlgTaskLoader;
import com.aricneto.twistytimer.fragment.AlgListFragment;
import com.aricneto.twistytimer.items.Theme;
import com.aricneto.twistytimer.utils.Prefs;
import com.aricneto.twistytimer.utils.TTIntent;
import com.aricneto.twistytimer.utils.ThemeUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static com.aricneto.twistytimer.utils.TTIntent.ACTION_CHANGED_THEME;
import static com.aricneto.twistytimer.utils.TTIntent.CATEGORY_UI_INTERACTIONS;

/**
 * TODO: REFACTOR
 * This code is very similar to {@link AlgListFragment}. We could generalize it to simplify
 * future changes
 */
public class BottomSheetTrainerDialog extends BottomSheetDialogFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String KEY_SUBSET = "subset";

    @BindView(R.id.title)
    TextView titleView;
    @BindView(R.id.list)
    RecyclerView recyclerView;

    private Unbinder mUnbinder;
    String currentSubset;
    TrainerCursorAdapter trainerCursorAdapter;

    public BottomSheetTrainerDialog() {
    }

    public static BottomSheetTrainerDialog newInstance(String subset) {
        BottomSheetTrainerDialog fragment = new BottomSheetTrainerDialog();
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View dialogView = inflater.inflate(R.layout.dialog_bottomsheet_recycler, container);
        mUnbinder = ButterKnife.bind(this, dialogView);



        setupRecyclerView();
        getLoaderManager().initLoader(MainActivity.ALG_LIST_LOADER_ID, null, this);

        return dialogView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
        getLoaderManager().destroyLoader(MainActivity.ALG_LIST_LOADER_ID);
    }

    public void reloadList() {
        getLoaderManager().restartLoader(MainActivity.ALG_LIST_LOADER_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new AlgTaskLoader(currentSubset);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        trainerCursorAdapter.swapCursor(cursor);
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        trainerCursorAdapter.swapCursor(null);
    }

    private void setupRecyclerView() {
        Activity parentActivity = getActivity();

        trainerCursorAdapter = new TrainerCursorAdapter(getActivity(), null, this);

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

        recyclerView.setAdapter(trainerCursorAdapter);
    }
}

