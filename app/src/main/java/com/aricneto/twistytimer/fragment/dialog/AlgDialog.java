package com.aricneto.twistytimer.fragment.dialog;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.items.AlgorithmModel;
import com.aricneto.twistytimer.layout.Cube;
import com.aricneto.twistytimer.listener.DialogListener;
import com.aricneto.twistytimer.utils.AlgUtils;
import com.aricneto.twistytimer.utils.TTIntent;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

/**
 * Shows the algList dialog
 */
public class AlgDialog extends DialogFragment {

    private Unbinder mUnbinder;
    private Context mContext;

    @BindView(R.id.editButton)    ImageView           editButton;
    @BindView(R.id.progressButton)ImageView           progressButton;
    @BindView(R.id.progressBar) MaterialProgressBar progressBar;
    @BindView(R.id.algList)     ListView            algList;
    @BindView(R.id.nameText)    TextView            nameText;
    @BindView(R.id.revertButton)ImageView           revertButton;
    @BindView(R.id.pll_arrows)  ImageView           pllArrows;
    @BindView(R.id.cube)        Cube                cube;

    private AlgorithmModel.Case mCase;
    private String mSubset;
    private DialogListener      dialogListener;
/*
    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            final DatabaseHandler dbHandler = TwistyTimer.getDBHandler();

            switch (view.getId()) {
                case R.id.editButton:
                    MaterialDialog dialog = ThemeUtils.roundDialog(mContext, new MaterialDialog.Builder(mContext)
                            .title(R.string.edit_algorithm)
                            .input("", algorithm.getAlgorithms(), (dialog1, input) -> {
                                algorithm.setAlgs(input.toString());
                                dbHandler.updateAlgorithmAlg(mId, input.toString());
                                algList.setText(input.toString());
                                updateList();
                            })
                            .inputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE)
                            .positiveText(R.string.action_done)
                            .negativeText(R.string.action_cancel)
                            .build());
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
                    final AppCompatSeekBar seekBar = (AppCompatSeekBar) LayoutInflater.from(mContext).inflate(R.layout.dialog_progress, null);
                    seekBar.setProgress(algorithm.getProgress());
                    ThemeUtils.roundAndShowDialog(mContext, new MaterialDialog.Builder(mContext)
                            .title(R.string.dialog_set_progress)
                            .customView(seekBar, false)
                            .positiveText(R.string.action_update)
                            .negativeText(R.string.action_cancel)
                            .onPositive((dialog12, which) -> {
                                int seekProgress = seekBar.getProgress();
                                algorithm.setProgress(seekProgress);
                                dbHandler.updateAlgorithmProgress(mId, seekProgress);
                                progressBar.setProgress(seekProgress);
                                updateList();
                            })
                            .build());
                    break;

                case R.id.revertButton:
                    ThemeUtils.roundAndShowDialog(mContext, new MaterialDialog.Builder(mContext)
                            .title(R.string.dialog_revert_title_confirmation)
                            .content(R.string.dialog_revert_content_confirmation)
                            .positiveText(R.string.action_reset)
                            .negativeText(R.string.action_cancel)
                            .onPositive((dialog13, which) -> {
                                algorithm.setAlgs(AlgUtils.getDefaultAlgs(algorithm.getSubset(), algorithm.getName()));
                                dbHandler.updateAlgorithmAlg(mId, algorithm.getAlgs());
                                algList.setText(algorithm.getAlgs());
                            })
                            .build());
                    break;
            }
        }
    };*/

    public static AlgDialog newInstance(String subset, AlgorithmModel.Case algCase) {
        AlgDialog timeDialog = new AlgDialog();
        Bundle args = new Bundle();
        args.putParcelable("case", algCase);
        args.putString("subset", subset);
        timeDialog.setArguments(args);
        return timeDialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View dialogView = inflater.inflate(R.layout.dialog_alg_details, container);
        mUnbinder = ButterKnife.bind(this, dialogView);

        mContext = getContext();
        mCase = getArguments().getParcelable("case");
        mSubset = getArguments().getString("subset");

        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return dialogView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ArrayAdapter<String> algAdapter =
                new ArrayAdapter<>(mContext, R.layout.item_alg_list_text, mCase.getAlgorithms());

        algList.setAdapter(algAdapter);
        nameText.setText(mCase.getName());

        cube.setCubeState(mCase.getState());

        //progressBar.setProgress(algorithm.getProgress());

//        revertButton.setOnClickListener(clickListener);
//        progressButton.setOnClickListener(clickListener);
//        editButton.setOnClickListener(clickListener);

        // If the subset is PLL, it'll need to show the pll arrows.
        if (mSubset.equals("PLL")) {
            pllArrows.setImageDrawable(AlgUtils.getPllArrow(getContext(), mCase.getName()));
            pllArrows.setVisibility(View.VISIBLE);
        }
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
}
