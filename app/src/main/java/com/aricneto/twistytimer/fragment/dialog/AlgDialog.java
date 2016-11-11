package com.aricneto.twistytimer.fragment.dialog;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.AppCompatSeekBar;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.aricneto.twistify.R;
import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.database.DatabaseHandler;
import com.aricneto.twistytimer.items.Algorithm;
import com.aricneto.twistytimer.listener.DialogListener;
import com.aricneto.twistytimer.utils.AlgUtils;
import com.aricneto.twistytimer.utils.TTIntent;

import java.util.HashMap;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

/**
 * Shows the algList dialog
 */
public class AlgDialog extends DialogFragment {

    private Unbinder mUnbinder;

    @BindViews({
            R.id.sticker1,  R.id.sticker2,  R.id.sticker3,  R.id.sticker4,
            R.id.sticker5,  R.id.sticker6,  R.id.sticker7,  R.id.sticker8,
            R.id.sticker9,  R.id.sticker10, R.id.sticker11, R.id.sticker12,
            R.id.sticker13, R.id.sticker14, R.id.sticker15, R.id.sticker16,
            R.id.sticker17, R.id.sticker18, R.id.sticker19, R.id.sticker20,
            R.id.sticker21,
    }) View[] stickers;

    @BindView(R.id.editButton)     ImageView           editButton;
    @BindView(R.id.progressButton) ImageView           progressButton;
    @BindView(R.id.progressBar)    MaterialProgressBar progressBar;
    @BindView(R.id.algText)        TextView            algText;
    @BindView(R.id.nameText)       TextView            nameText;
    @BindView(R.id.revertButton)   ImageView           revertButton;
    @BindView(R.id.pll_arrows)     ImageView           pllArrows;

    private long            mId;
    private Algorithm       algorithm;
    private DialogListener  dialogListener;

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            final DatabaseHandler dbHandler = TwistyTimer.getDBHandler();

            switch (view.getId()) {
                case R.id.editButton:
                    MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                            .title(R.string.edit_algorithm)
                            .input("", algorithm.getAlgs(), new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(MaterialDialog dialog, CharSequence input) {
                                    algorithm.setAlgs(input.toString());
                                    dbHandler.updateAlgorithmAlg(mId, input.toString());
                                    algText.setText(input.toString());
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
                        editText.setLines(5);
                        editText.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
                        editText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
                    }
                    dialog.show();
                    break;

                case R.id.progressButton:
                    final AppCompatSeekBar seekBar = (AppCompatSeekBar) LayoutInflater.from(getContext()).inflate(R.layout.dialog_progress, null);
                    seekBar.setProgress(algorithm.getProgress());
                    new MaterialDialog.Builder(getContext())
                            .title(R.string.dialog_set_progress)
                            .customView(seekBar, false)
                            .positiveText(R.string.action_update)
                            .negativeText(R.string.action_cancel)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    int seekProgress = seekBar.getProgress();
                                    algorithm.setProgress(seekProgress);
                                    dbHandler.updateAlgorithmProgress(mId, seekProgress);
                                    progressBar.setProgress(seekProgress);
                                    updateList();
                                }
                            })
                            .show();
                    break;

                case R.id.revertButton:
                    new MaterialDialog.Builder(getContext())
                            .title(R.string.dialog_revert_title_confirmation)
                            .content(R.string.dialog_revert_content_confirmation)
                            .positiveText(R.string.action_reset)
                            .negativeText(R.string.action_cancel)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    algorithm.setAlgs(AlgUtils.getDefaultAlgs(algorithm.getSubset(), algorithm.getName()));
                                    dbHandler.updateAlgorithmAlg(mId, algorithm.getAlgs());
                                    algText.setText(algorithm.getAlgs());
                                }
                            })
                            .show();
                    break;
            }
        }
    };

    public static AlgDialog newInstance(long id) {
        AlgDialog timeDialog = new AlgDialog();
        Bundle args = new Bundle();
        args.putLong("id", id);
        timeDialog.setArguments(args);
        return timeDialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View dialogView = inflater.inflate(R.layout.dialog_alg_details, container);
        mUnbinder = ButterKnife.bind(this, dialogView);

        mId = getArguments().getLong("id");

        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        final Algorithm matchedAlgorithm = TwistyTimer.getDBHandler().getAlgorithm(mId);

        if (matchedAlgorithm != null) {
            algorithm = matchedAlgorithm;
            algText.setText(algorithm.getAlgs());
            nameText.setText(algorithm.getName());

            colorCube(algorithm.getState());

            progressBar.setProgress(algorithm.getProgress());

            revertButton.setOnClickListener(clickListener);
            progressButton.setOnClickListener(clickListener);
            editButton.setOnClickListener(clickListener);

            // If the subset is PLL, it'll need to show the pll arrows.
            if (algorithm.getSubset().equals("PLL")) {
                pllArrows.setImageDrawable(AlgUtils.getPllArrow(getContext(), algorithm.getName()));
                pllArrows.setVisibility(View.VISIBLE);
            }

        }

        return dialogView;
    }

    public void setDialogListener(DialogListener listener) {
        dialogListener = listener;
    }

    private void updateList() {
        TTIntent.broadcast(TTIntent.CATEGORY_ALG_DATA_CHANGES, TTIntent.ACTION_ALGS_MODIFIED);
        //dismiss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
        if (dialogListener != null)
            dialogListener.onDismissDialog();
    }

    private void colorCube(final String state) {
        // See the reference image to understand how this works
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        final HashMap<Character, Integer> colorHash = AlgUtils.getColorLetterHashMap(sp);

        ButterKnife.apply(stickers, new ButterKnife.Action<View>() {
            @Override
            public void apply(@NonNull View sticker, int index) {
                sticker.setBackgroundColor(colorHash.get(state.charAt(index)));
            }
        });
    }
}
