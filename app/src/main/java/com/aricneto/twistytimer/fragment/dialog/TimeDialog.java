package com.aricneto.twistytimer.fragment.dialog;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.PopupMenu;
import android.text.Html;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.aricneto.twistify.R;
import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.database.DatabaseHandler;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.listener.DialogListener;
import com.aricneto.twistytimer.utils.PuzzleUtils;
import com.aricneto.twistytimer.utils.ScrambleGenerator;
import com.aricneto.twistytimer.utils.TTIntent;

import org.joda.time.DateTime;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Shows the timeList dialog
 */
public class TimeDialog extends DialogFragment {

    private Unbinder mUnbinder;

    @BindView(R.id.timeText)          TextView  timeText;
    @BindView(R.id.puzzlePenaltyText) TextView  penaltyText;
    @BindView(R.id.dateText)          TextView  dateText;
    @BindView(R.id.scrambleText)      TextView  scrambleText;
    @BindView(R.id.editButton)        ImageView editButton;
    @BindView(R.id.commentButton)     ImageView commentButton;
    @BindView(R.id.commentText)       TextView  commentText;
    @BindView(R.id.overflowButton)    ImageView overflowButton;

    private long            mId;
    private Solve           solve;
    private DialogListener  dialogListener;

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            final DatabaseHandler dbHandler = TwistyTimer.getDBHandler();

            switch (view.getId()) {
                case R.id.overflowButton:
                    PopupMenu popupMenu = new PopupMenu(getActivity(), overflowButton);
                    if (solve.isHistory())
                        popupMenu.getMenuInflater().inflate(R.menu.menu_list_detail_history, popupMenu.getMenu());
                    else
                        popupMenu.getMenuInflater().inflate(R.menu.menu_list_detail, popupMenu.getMenu());
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.share:
                                    Intent shareIntent = new Intent();
                                    shareIntent.setAction(Intent.ACTION_SEND);
                                    shareIntent.putExtra(Intent.EXTRA_TEXT, PuzzleUtils.convertTimeToString(solve.getTime(), PuzzleUtils.FORMAT_DEFAULT) + "s.\n" + solve.getComment() + "\n" + solve.getScramble());
                                    shareIntent.setType("text/plain");
                                    getContext().startActivity(shareIntent);
                                    break;
                                case R.id.remove:
                                    dbHandler.deleteSolveByID(mId);
                                    updateList();
                                    break;
                                case R.id.use:
                                    new TTIntent.BroadcastBuilder(TTIntent.CATEGORY_UI_INTERACTIONS, TTIntent.ACTION_USE_SCRAMBLE)
                                            .solve(solve)
                                            .broadcast();
                                    dismiss();
                                    break;
                                case R.id.history_to:
                                    solve.setHistory(true);
                                    Toast.makeText(getContext(), getString(R.string.sent_to_history), Toast.LENGTH_SHORT).show();
                                    dbHandler.updateSolve(solve);
                                    updateList();
                                    dismiss();
                                    break;
                                case R.id.history_from:
                                    solve.setHistory(false);
                                    Toast.makeText(getContext(), getString(R.string.sent_to_session), Toast.LENGTH_SHORT).show();
                                    dbHandler.updateSolve(solve);
                                    updateList();
                                    dismiss();
                                    break;
                            }
                            return true;
                        }
                    });
                    popupMenu.show();
                    break;
                case R.id.editButton:
                    new MaterialDialog.Builder(getContext())
                            .title(R.string.select_penalty)
                            .items(R.array.array_penalties)
                            .itemsCallbackSingleChoice(solve.getPenalty(), new MaterialDialog.ListCallbackSingleChoice() {
                                @Override
                                public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                                    switch (which) {
                                        case 0: // No penalty
                                            solve = PuzzleUtils.applyPenalty(solve, PuzzleUtils.NO_PENALTY);
                                            break;
                                        case 1: // +2
                                            solve = PuzzleUtils.applyPenalty(solve, PuzzleUtils.PENALTY_PLUSTWO);
                                            break;
                                        case 2: // DNF
                                            solve = PuzzleUtils.applyPenalty(solve, PuzzleUtils.PENALTY_DNF);
                                            break;
                                    }
                                    dbHandler.updateSolve(solve);
                                    // dismiss dialog
                                    updateList();
                                    return true;
                                }
                            })
                            .negativeText(R.string.action_cancel)
                            .show();
                    break;
                case R.id.commentButton:
                    MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                            .title(R.string.edit_comment)
                            .input("", solve.getComment(), new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(MaterialDialog dialog, CharSequence input) {
                                    solve.setComment(input.toString());
                                    dbHandler.updateSolve(solve);
                                    Toast.makeText(getContext(), getString(R.string.added_comment), Toast.LENGTH_SHORT).show();
                                    updateList();
                                }
                            })
                            .inputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE)
                            .positiveText(R.string.action_done)
                            .negativeText(R.string.action_cancel)
                            .build();
                    EditText editText = dialog.getInputEditText();
                    if (editText != null) {
                        editText.setSingleLine(false);
                        editText.setLines(3);
                        editText.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
                        editText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
                    }
                    dialog.show();
                    break;
                case R.id.scrambleText:
                    ScrambleGenerator generator = new ScrambleGenerator(solve.getPuzzle());
                    MaterialDialog scrambleDialog = new MaterialDialog.Builder(getContext())
                            .customView(R.layout.item_scramble_img, false)
                            .show();
                    ImageView imageView = (ImageView) scrambleDialog.getView().findViewById(R.id.scrambleImg);
                    imageView.setImageDrawable(generator.generateImageFromScramble(PreferenceManager.getDefaultSharedPreferences(getContext()), solve.getScramble()));
                    break;
            }
        }
    };

    public static TimeDialog newInstance(long id) {
        TimeDialog timeDialog = new TimeDialog();
        Bundle args = new Bundle();
        args.putLong("id", id);
        timeDialog.setArguments(args);
        return timeDialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View dialogView = inflater.inflate(R.layout.dialog_time_details, container);
        //this.setEnterTransition(R.anim.activity_slide_in);
        mUnbinder = ButterKnife.bind(this, dialogView);

        mId = getArguments().getLong("id");

        //Log.d("TIME DIALOG", "mId: " + mId + "\nexists: " + handler.idExists(mId));

        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        //getDialog().getWindow().setWindowAnimations(R.style.DialogAnimationScale);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        final Solve matchedSolve = TwistyTimer.getDBHandler().getSolve(mId);

        if (matchedSolve != null) {
            solve = matchedSolve;

            timeText.setText(Html.fromHtml(PuzzleUtils.convertTimeToString(solve.getTime(), PuzzleUtils.FORMAT_SMALL_MILLI)));
            dateText.setText(new DateTime(solve.getDate()).toString("d MMM y'\n'H':'mm"));

            scrambleText.setText(solve.getScramble());

            if (solve.getPenalty() == PuzzleUtils.PENALTY_DNF)
                penaltyText.setText("DNF");
            else if (solve.getPenalty() == PuzzleUtils.PENALTY_PLUSTWO)
                penaltyText.setText("+2");
            else
                penaltyText.setVisibility(View.GONE);

            if (solve.getComment() != null) {
                if (! solve.getComment().equals("")) {
                    commentText.setText(solve.getComment());
                    commentText.setVisibility(View.VISIBLE);
                }
            }

            if (solve.getScramble() != null) {
                if (solve.getScramble().equals(""))
                    scrambleText.setVisibility(View.GONE);
            }

            scrambleText.setOnClickListener(clickListener);
            overflowButton.setOnClickListener(clickListener);
            editButton.setOnClickListener(clickListener);
            commentButton.setOnClickListener(clickListener);

        }

        return dialogView;
    }

    public void setDialogListener(DialogListener listener) {
        dialogListener = listener;
    }

    private void updateList() {
        if (dialogListener != null) {
            dialogListener.onUpdateDialog();
        } else {
            TTIntent.broadcast(TTIntent.CATEGORY_TIME_DATA_CHANGES, TTIntent.ACTION_TIMES_MODIFIED);
        }
        dismiss();
    }

    @Override
    public void onDestroyView() {
        mUnbinder.unbind();
        if (dialogListener != null)
            dialogListener.onDismissDialog();
        super.onDestroyView();
    }
}
