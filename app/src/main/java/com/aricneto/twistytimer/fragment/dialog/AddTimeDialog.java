package com.aricneto.twistytimer.fragment.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.fragment.app.DialogFragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.listener.DialogListener;
import com.aricneto.twistytimer.utils.PuzzleUtils;
import com.aricneto.twistytimer.utils.TTIntent;
import com.aricneto.twistytimer.watcher.SolveTimeNumberTextWatcher;

import org.joda.time.DateTime;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static com.aricneto.twistytimer.utils.TTIntent.ACTION_GENERATE_SCRAMBLE;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_TIME_ADDED;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_TIME_ADDED_MANUALLY;
import static com.aricneto.twistytimer.utils.TTIntent.CATEGORY_TIME_DATA_CHANGES;
import static com.aricneto.twistytimer.utils.TTIntent.CATEGORY_UI_INTERACTIONS;
import static com.aricneto.twistytimer.utils.TTIntent.broadcast;

/**
 * Shows the algList dialog
 */
public class AddTimeDialog extends DialogFragment {

    private Unbinder mUnbinder;

    @BindView(R.id.edit_text_time)
    AppCompatEditText timeEditText;

    @BindView(R.id.check_scramble)
    AppCompatCheckBox useCurrentScramble;

    @BindView(R.id.button_save)
    View saveButton;

    private DialogListener  dialogListener;

    private String currentPuzzle;
    private String currentScramble;
    private String currentPuzzleSubtype;

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            switch (view.getId()) {
                case R.id.button_save:
                    if (timeEditText.getText().toString().length() > 0) {
                        int time = (int) PuzzleUtils.parseAddedTime(timeEditText.getText().toString());
                        final Solve solve = new Solve(time, currentPuzzle,
                                                      currentPuzzleSubtype, new DateTime().getMillis(),
                                                      useCurrentScramble.isChecked() ? currentScramble : "",
                                                      PuzzleUtils.NO_PENALTY, "", false);

                        TwistyTimer.getDBHandler().addSolve(solve);
                        // The receiver might be able to use the new solve and avoid
                        // accessing the database.
                        new TTIntent.BroadcastBuilder(CATEGORY_TIME_DATA_CHANGES, ACTION_TIME_ADDED)
                                .solve(solve)
                                .broadcast();

                        // Generate new scramble
                        broadcast(CATEGORY_UI_INTERACTIONS, ACTION_GENERATE_SCRAMBLE);

                        dismiss();
                    } else {
                        dismiss();
                    }
                    break;
            }
        }
    };

    public static AddTimeDialog newInstance(String currentPuzzle, String currentPuzzleSubtype, String currentScramble) {
        AddTimeDialog timeDialog = new AddTimeDialog();
        Bundle args = new Bundle();
        args.putString("puzzle", currentPuzzle);
        args.putString("category", currentPuzzleSubtype);
        args.putString("scramble", currentScramble);
        timeDialog.setArguments(args);
        return timeDialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentPuzzle = getArguments().getString("puzzle");
        currentPuzzleSubtype = getArguments().getString("category");
        currentScramble = getArguments().getString("scramble");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View dialogView = inflater.inflate(R.layout.dialog_add_time, container);
        mUnbinder = ButterKnife.bind(this, dialogView);

        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        saveButton.setOnClickListener(clickListener);
        timeEditText.addTextChangedListener(new SolveTimeNumberTextWatcher());

        // Focus on editText and request keyboard
        timeEditText.requestFocus();

        try {
            timeEditText.postDelayed(() ->
                    ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                        .showSoftInput(timeEditText, InputMethodManager.SHOW_IMPLICIT), 400);
        } catch (Exception e) {
            Log.e("AddTimeDialog", "Error showing keyboard: " + e);
        }

        return dialogView;
    }

    public void setDialogListener(DialogListener listener) {
        dialogListener = listener;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);

        // Hide keyboard
        try {
            ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(timeEditText.getWindowToken(), 0);
        } catch (Exception e) {
            Log.e("AddTimeDialog", "Error hiding keyboard: " + e);
        }

        if (dialogListener != null)
            dialogListener.onDismissDialog();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }
}
