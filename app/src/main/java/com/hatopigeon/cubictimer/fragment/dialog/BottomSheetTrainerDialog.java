package com.hatopigeon.cubictimer.fragment.dialog;

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

import com.hatopigeon.cubicify.R;
import com.hatopigeon.cubictimer.activity.MainActivity;
import com.hatopigeon.cubictimer.adapter.AlgCursorAdapter;
import com.hatopigeon.cubictimer.adapter.TrainerCursorAdapter;
import com.hatopigeon.cubictimer.database.AlgTaskLoader;
import com.hatopigeon.cubictimer.fragment.AlgListFragment;
import com.hatopigeon.cubictimer.items.Theme;
import com.hatopigeon.cubictimer.puzzle.TrainerScrambler;
import com.hatopigeon.cubictimer.utils.Prefs;
import com.hatopigeon.cubictimer.utils.TTIntent;
import com.hatopigeon.cubictimer.utils.ThemeUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static com.hatopigeon.cubictimer.utils.TTIntent.ACTION_ALGS_MODIFIED;
import static com.hatopigeon.cubictimer.utils.TTIntent.ACTION_CHANGED_CATEGORY;
import static com.hatopigeon.cubictimer.utils.TTIntent.ACTION_CHANGED_THEME;
import static com.hatopigeon.cubictimer.utils.TTIntent.ACTION_GENERATE_SCRAMBLE;
import static com.hatopigeon.cubictimer.utils.TTIntent.CATEGORY_ALG_DATA_CHANGES;
import static com.hatopigeon.cubictimer.utils.TTIntent.CATEGORY_UI_INTERACTIONS;
import static com.hatopigeon.cubictimer.utils.TTIntent.broadcast;
import static com.hatopigeon.cubictimer.utils.TTIntent.registerReceiver;

/**
 * TODO: REFACTOR
 * This code is very similar to {@link AlgListFragment}. We could generalize it to simplify
 * future changes
 */
public class BottomSheetTrainerDialog extends BottomSheetDialogFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String KEY_SUBSET = "subset";
    private static final String KEY_CATEGORY = "category";

    @BindView(R.id.title)
    TextView          titleView;
    @BindView(R.id.list)
    RecyclerView      recyclerView;
    @BindView(R.id.button)
    AppCompatTextView button;

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
        Drawable icon = ThemeUtils.tintDrawable(getContext(), R.drawable.ic_outline_control_camera_24px,
                                                ContextCompat.getColor(getContext(), R.color.md_blue_A700));
        titleView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);

        button.setVisibility(View.VISIBLE);
        button.setText(R.string.trainer_select_all);
        button.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        button.setOnClickListener(v -> {
            trainerCursorAdapter.selectAll();
            recyclerView.setAdapter(trainerCursorAdapter);
            dismiss();
        });

        setupRecyclerView();
        getLoaderManager().initLoader(MainActivity.ALG_LIST_LOADER_ID, null, this);

        registerReceiver(mAlgDataChangedReceiver);
        registerReceiver(mUIInteractionReceiver);

        return dialogView;
    }

    private void resetRecyclerView() {

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

