package com.hatopigeon.cubictimer.fragment.dialog;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.hatopigeon.cubicify.R;
import com.hatopigeon.cubictimer.CubicTimer;
import com.hatopigeon.cubictimer.database.DatabaseHandler;
import com.hatopigeon.cubictimer.items.Solve;
import com.hatopigeon.cubictimer.utils.PuzzleUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * <p>
 * A dialog fragment that chooses a the puzzle type and category. Once the desired options are
 * chosen this dialog relays the selection through the parent activity to the "consumer" fragment
 * that initiated this fragment. The "consumer" fragment is identified by its fragment tag, which
 * it passes to this chooser in {@link #newInstance(int, String)} and which this chooser passes
 * back to the activity, so the activity can find that "consumer" fragment.
 * </p>
 * <p>
 * <i>This dialog fragment <b>must</b> be used in the context of an activity that implements the
 * {@link PuzzleCallback} interface, or exceptions will occur.</i>
 * </p>
 */
public class PuzzleChooserDialog extends DialogFragment {
    /**
     * An interface that allows fragments to communicate changes to the selected puzzle type and/or
     * category.
     */
    public interface PuzzleCallback {
        /**
         * Notifies the listener that a new puzzle type and/or category have been selected.
         *
         * @param tag
         *     The tag identifying the callback. When this interface is implemented by an activity
         *     as a means of communication between two fragments, the tag should be the fragment
         *     tag that identifies the fragment to which the activity should relay the message. The
         *     receiving fragment should also (probably) implement this interface.
         * @param puzzleType
         *     The name of the newly-selected puzzle type.
         * @param puzzleCategory
         *     The name of the newly-selected puzzle category.
         */
        void onPuzzleSelected(
                @NonNull String tag, @NonNull String puzzleType, @NonNull String puzzleCategory);
    }

    private Unbinder mUnbinder;

    @BindView(R.id.puzzleSpinner)   Spinner  puzzleSpinner;
    @BindView(R.id.categorySpinner) Spinner  categorySpinner;
    @BindView(R.id.selectButton)    TextView selectButton;

    private static final String CURRENT_CATEGORY = "Normal";

    /**
     * The name of the fragment argument holding a string resource ID for the text to be displayed
     * on the selection button that closes this chooser dialog.
     */
    private static final String ARG_BUTTON_TEXT_RES_ID = "buttonTextResourceID";

    /**
     * The name of the fragment argument holding the fragment tag of the fragment that instantiated
     * this puzzle chooser.
     */
    private static final String ARG_CONSUMER_TAG = "consumerTag";

    /**
     * The selected puzzle type.
     */
    private String mSelectedPuzzleType;

    /**
     * The selected puzzle category.
     */
    private String mSelectedPuzzleCategory;

    private ArrayAdapter<String> categoryAdapter;

    /**
     * Creates a new instance of this fragment.
     *
     * @param buttonTextResID
     *     The string resource ID of the string to be displayed on the select button that closes
     *     this fragment and reports the selection. If zero, the default "OK" will be shown.
     * @param consumerTag
     *     The fragment tag that identifies the fragment that instantiated this puzzle chooser.
     *     This "consumer" fragment will be informed, via the parent activity, of the selected
     *     puzzle type and category before this chooser is dismissed.
     *
     * @return
     *     The new instance of this puzzle chooser.
     */
    public static PuzzleChooserDialog newInstance(
            @StringRes int buttonTextResID, String consumerTag) {
        final PuzzleChooserDialog fragment = new PuzzleChooserDialog();
        final Bundle args = new Bundle();

        args.putInt(ARG_BUTTON_TEXT_RES_ID, buttonTextResID);
        args.putString(ARG_CONSUMER_TAG, consumerTag);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View dialogView = inflater.inflate(R.layout.dialog_puzzle_chooser_dialog, container);
        mUnbinder = ButterKnife.bind(this, dialogView);

        final @StringRes int buttonTextResID
                = getArguments() != null ? getArguments().getInt(ARG_BUTTON_TEXT_RES_ID, 0) : 0;

        if (buttonTextResID != 0) {
            // Override the default text.
            selectButton.setText(buttonTextResID);
        }

        final ArrayAdapter puzzleAdapter = ArrayAdapter.createFromResource(
                getContext(), R.array.puzzles, android.R.layout.simple_spinner_dropdown_item);

        puzzleSpinner.setAdapter(puzzleAdapter);

        // Be flexible with the initial value, as it depends on what is first in "R.array.puzzle".
        mSelectedPuzzleType = (String) puzzleSpinner.getSelectedItem();
        mSelectedPuzzleCategory = CURRENT_CATEGORY;

        updateCategoriesForType(mSelectedPuzzleType);

        puzzleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mSelectedPuzzleType = PuzzleUtils.getPuzzleInPosition(i);
                updateCategoriesForType(mSelectedPuzzleType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mSelectedPuzzleCategory = categoryAdapter.getItem(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Relay this information back to the fragment/activity that opened this chooser.
                getRelayActivity().onPuzzleSelected(
                        getArguments().getString(ARG_CONSUMER_TAG, "not set!"),
                        mSelectedPuzzleType, mSelectedPuzzleCategory);
                dismiss();
            }
        });

        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialogView;
    }

    private void updateCategoriesForType(String puzzleType) {
        final DatabaseHandler dbHandler = CubicTimer.getDBHandler();
        final List<String> subtypeList = dbHandler.getAllSubtypesFromType(puzzleType);

        if (subtypeList.size() == 0) {
            subtypeList.add(CURRENT_CATEGORY);
            dbHandler.addSolve(new Solve(1, puzzleType, CURRENT_CATEGORY,
                    0L, "", PuzzleUtils.PENALTY_HIDETIME, "", true));
        }
        categoryAdapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_dropdown_item, subtypeList);
        categorySpinner.setAdapter(categoryAdapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }

    /**
     * Gets the activity reference type cast to support the required interface for relaying the
     * puzzle type/category selection to another fragment.
     *
     * @return The attached activity, or {@code null} if no activity is attached.
     */
    private <A extends PuzzleCallback> A getRelayActivity() {
        //noinspection unchecked
        return (A) getActivity();
    }
}
