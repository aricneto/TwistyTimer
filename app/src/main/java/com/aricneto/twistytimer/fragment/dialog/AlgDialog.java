package com.aricneto.twistytimer.fragment.dialog;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
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
import com.aricneto.twistytimer.database.DatabaseHandler;
import com.aricneto.twistytimer.items.Algorithm;
import com.aricneto.twistytimer.listener.DialogListener;
import com.aricneto.twistytimer.utils.AlgUtils;

import java.util.HashMap;

import butterknife.Bind;
import butterknife.ButterKnife;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

/**
 * Shows the algList dialog
 */
public class AlgDialog extends DialogFragment {


    @Bind(R.id.editButton)     ImageView           editButton;
    @Bind(R.id.progressButton) ImageView           progressButton;
    @Bind(R.id.progressBar)    MaterialProgressBar progressBar;
    @Bind(R.id.algText)        TextView            algText;
    @Bind(R.id.nameText)       TextView            nameText;
    @Bind(R.id.revertButton)   ImageView           revertButton;
    @Bind(R.id.pll_arrows)     ImageView           pllArrows;


    private long            mId;
    private DatabaseHandler handler;

    private Algorithm       algorithm;
    private DialogListener  dialogListener;

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.editButton:
                    MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                            .title(R.string.edit_algorithm)
                            .input("", algorithm.getAlgs(), new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(MaterialDialog dialog, CharSequence input) {
                                    algorithm.setAlgs(input.toString());
                                    handler.updateAlgorithmAlg(mId, input.toString());
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
                                    handler.updateAlgorithmProgress(mId, seekProgress);
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
                                    handler.updateAlgorithmAlg(mId, algorithm.getAlgs());
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
        ButterKnife.bind(this, dialogView);

        mId = getArguments().getLong("id");
        handler = new DatabaseHandler(getActivity());

        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        if (handler.idExists(mId)) {
            algorithm = handler.getAlgorithm(mId);
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

    @Override
    public void onResume() {
        super.onResume();
    }

    private void updateList() {
        if (dialogListener != null) {
            dialogListener.onUpdateDialog();
        } else {
            Intent sendIntent = new Intent("ALGLIST");
            sendIntent.putExtra("action", "ALG ADDED");
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(sendIntent);
        }
        //dismiss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.closeDB();
        ButterKnife.unbind(this);
        if (dialogListener != null)
            dialogListener.onDismissDialog();
    }

    @Bind(R.id.sticker21) View sticker21;
    @Bind(R.id.sticker20) View sticker20;
    @Bind(R.id.sticker19) View sticker19;
    @Bind(R.id.sticker13) View sticker13;
    @Bind(R.id.sticker14) View sticker14;
    @Bind(R.id.sticker15) View sticker15;
    @Bind(R.id.sticker10) View sticker10;
    @Bind(R.id.sticker11) View sticker11;
    @Bind(R.id.sticker12) View sticker12;
    @Bind(R.id.sticker18) View sticker18;
    @Bind(R.id.sticker17) View sticker17;
    @Bind(R.id.sticker16) View sticker16;
    @Bind(R.id.sticker1)  View sticker1;
    @Bind(R.id.sticker2)  View sticker2;
    @Bind(R.id.sticker3)  View sticker3;
    @Bind(R.id.sticker4)  View sticker4;
    @Bind(R.id.sticker5)  View sticker5;
    @Bind(R.id.sticker6)  View sticker6;
    @Bind(R.id.sticker7)  View sticker7;
    @Bind(R.id.sticker8)  View sticker8;
    @Bind(R.id.sticker9)  View sticker9;

    private void colorCube(String state) {

        Context mContext = getContext();
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        HashMap<Character, Integer> colorHash = AlgUtils.getColorLetterHashMap(sp);
        /*See the reference image to understand how this works
        * Yeah, I know. It's shitty as hell */

        char[] charState = state.toCharArray();
        sticker1.setBackgroundColor(colorHash.get(charState[0]));
        sticker2.setBackgroundColor(colorHash.get(charState[1]));
        sticker3.setBackgroundColor(colorHash.get(charState[2]));
        sticker4.setBackgroundColor(colorHash.get(charState[3]));
        sticker5.setBackgroundColor(colorHash.get(charState[4]));
        sticker6.setBackgroundColor(colorHash.get(charState[5]));
        sticker7.setBackgroundColor(colorHash.get(charState[6]));
        sticker8.setBackgroundColor(colorHash.get(charState[7]));
        sticker9.setBackgroundColor(colorHash.get(charState[8]));
        sticker10.setBackgroundColor(colorHash.get(charState[9]));
        sticker11.setBackgroundColor(colorHash.get(charState[10]));
        sticker12.setBackgroundColor(colorHash.get(charState[11]));
        sticker13.setBackgroundColor(colorHash.get(charState[12]));
        sticker14.setBackgroundColor(colorHash.get(charState[13]));
        sticker15.setBackgroundColor(colorHash.get(charState[14]));
        sticker16.setBackgroundColor(colorHash.get(charState[15]));
        sticker17.setBackgroundColor(colorHash.get(charState[16]));
        sticker18.setBackgroundColor(colorHash.get(charState[17]));
        sticker19.setBackgroundColor(colorHash.get(charState[18]));
        sticker20.setBackgroundColor(colorHash.get(charState[19]));
        sticker21.setBackgroundColor(colorHash.get(charState[20]));

    }

}
