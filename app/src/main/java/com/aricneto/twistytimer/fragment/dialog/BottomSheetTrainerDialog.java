package com.aricneto.twistytimer.fragment.dialog;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
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
import com.aricneto.twistytimer.puzzle.TrainerScrambler;
import com.aricneto.twistytimer.utils.Prefs;
import com.aricneto.twistytimer.utils.TTIntent;
import com.aricneto.twistytimer.utils.ThemeUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static com.aricneto.twistytimer.utils.TTIntent.ACTION_ALGS_MODIFIED;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_CHANGED_CATEGORY;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_CHANGED_THEME;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_GENERATE_SCRAMBLE;
import static com.aricneto.twistytimer.utils.TTIntent.CATEGORY_ALG_DATA_CHANGES;
import static com.aricneto.twistytimer.utils.TTIntent.CATEGORY_UI_INTERACTIONS;
import static com.aricneto.twistytimer.utils.TTIntent.broadcast;
import static com.aricneto.twistytimer.utils.TTIntent.registerReceiver;

/**
 * TODO: REFACTOR
 * This code is very similar to {@link AlgListFragment}. We could generalize it to simplify
 * future changes
 */
public class BottomSheetTrainerDialog extends BottomSheetDialogFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String KEY_SUBSET = "subset";
    private static final String KEY_CATEGORY = "category";

    @BindView(R.id.title)
    TextView titleView;
    @BindView(R.id.list)
    RecyclerView recyclerView;

    private Unbinder mUnbinder;
    TrainerScrambler.TrainerSubset currentSubset;
    String                         currentCategory;
    TrainerCursorAdapter           trainerCursorAdapter;

    public BottomSheetTrainerDialog() {
    }

    // Receives broadcasts about changes to the algorithm data.
    private TTIntent.TTFragmentBroadcastReceiver mAlgDataChangedReceiver
            = new TTIntent.TTFragmentBroadcastReceiver(this, CATEGORY_ALG_DATA_CHANGES) {
        @Override
        public void onReceiveWhileAdded(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_ALGS_MODIFIED:
                    reloadList();
                    break;
            }
        }
    };
    // Receives broadcasts about changes to the time user interface.
    private TTIntent.TTFragmentBroadcastReceiver mUIInteractionReceiver
            = new TTIntent.TTFragmentBroadcastReceiver(this, CATEGORY_UI_INTERACTIONS) {
        @Override
        public void onReceiveWhileAdded(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_CHANGED_CATEGORY:
                    reloadList();
                    break;
            }
        }
    };

    public static BottomSheetTrainerDialog newInstance(TrainerScrambler.TrainerSubset subset, String category) {
        BottomSheetTrainerDialog fragment = new BottomSheetTrainerDialog();
        Bundle args = new Bundle();
        args.putSerializable(KEY_SUBSET, subset);
        args.putString(KEY_CATEGORY, category);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentSubset = (TrainerScrambler.TrainerSubset) getArguments().getSerializable(KEY_SUBSET);
            currentCategory = getArguments().getString(KEY_CATEGORY);
        }
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View dialogView = inflater.inflate(R.layout.dialog_bottomsheet_recycler, container);
        mUnbinder = ButterKnife.bind(this, dialogView);

        titleView.setText(R.string.trainer_spinner_title);
        Drawable icon = VectorDrawableCompat.create(getContext().getResources(), R.drawable.ic_outline_control_camera_24px, null);
        titleView.setCompoundDrawablesWithIntrinsicBounds(null, null, icon, null);

        setupRecyclerView();
        getLoaderManager().initLoader(MainActivity.ALG_LIST_LOADER_ID, null, this);

        registerReceiver(mAlgDataChangedReceiver);
        registerReceiver(mUIInteractionReceiver);

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
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        broadcast(CATEGORY_UI_INTERACTIONS, ACTION_GENERATE_SCRAMBLE);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new AlgTaskLoader(currentSubset.name());
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

        trainerCursorAdapter = new TrainerCursorAdapter(getActivity(), null, this, currentSubset, currentCategory);

        // Set different managers to support different orientations
        StaggeredGridLayoutManager gridLayoutManagerHorizontal =
                new StaggeredGridLayoutManager(4, StaggeredGridLayoutManager.VERTICAL);
        StaggeredGridLayoutManager gridLayoutManagerVertical =
                new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL);

        // Adapt to orientation
        if (parentActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
            recyclerView.setLayoutManager(gridLayoutManagerVertical);
        else
            recyclerView.setLayoutManager(gridLayoutManagerHorizontal);

        recyclerView.setAdapter(trainerCursorAdapter);
    }
}

