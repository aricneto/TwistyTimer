package com.aricneto.twistytimer.fragment.dialog;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.items.AlgorithmModel;
import com.aricneto.twistytimer.layout.Cube2D;
import com.aricneto.twistytimer.layout.CubeIsometric;
import com.aricneto.twistytimer.listener.DialogListener;
import com.aricneto.twistytimer.utils.AlgUtils;
import com.aricneto.twistytimer.utils.TTIntent;
import com.aricneto.twistytimer.utils.ThemeUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

/**
 * Shows the algList dialog
 */
public class AlgDialog extends DialogFragment {

    @BindView(R.id.editButton)
    ImageView editButton;
    @BindView(R.id.progressButton)
    ImageView progressButton;
    @BindView(R.id.progressBar)
    MaterialProgressBar progressBar;
    @BindView(R.id.nameText)
    TextView nameText;
    @BindView(R.id.algList)
    ListView algList;
    @BindView(R.id.revertButton)
    ImageView revertButton;
    @BindView(R.id.root_layout)
    RelativeLayout root;

    private Unbinder mUnbinder;
    private Context  mContext;
    private AlgorithmModel.Case mCase;
    private String              mSubset;
    private String              mPuzzle;
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

    public static AlgDialog newInstance(String puzzle, String subset, AlgorithmModel.Case algCase) {
        AlgDialog timeDialog = new AlgDialog();
        Bundle args = new Bundle();
        args.putParcelable("case", algCase);
        args.putString("subset", subset);
        args.putString("puzzle", puzzle);
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
        mPuzzle = getArguments().getString("puzzle");

        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return dialogView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Cube
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ThemeUtils.dpToPix(108),
                ThemeUtils.dpToPix(108));
        params.addRule(RelativeLayout.BELOW, R.id.progressBar);
        params.topMargin = ThemeUtils.dpToPix(8);
        params.leftMargin = ThemeUtils.dpToPix(8);

        View cube;
        if (AlgUtils.isIsometricView(mPuzzle, mSubset))
            cube = new CubeIsometric(mContext).init(70, AlgUtils.getPuzzleSize(mPuzzle), mCase.getState());
        else
            cube = new Cube2D(mContext).init(AlgUtils.getPuzzleSize(mPuzzle), mCase.getState());

        cube.setId(R.id.cube);
        root.addView(cube, params);

        // If mSubset is PLL, it'll need to show the pll arrows.
        if (mSubset.equals("PLL")) {
            ImageView pllArrows = new ImageView(mContext);
            pllArrows.setImageDrawable(AlgUtils.getPllArrow(mContext, mCase.getName()));
            pllArrows.setScaleX(0.65f);
            pllArrows.setScaleY(0.65f);
            root.addView(pllArrows, params);
        }

        // List
        params = (RelativeLayout.LayoutParams) algList.getLayoutParams();
        params.addRule(RelativeLayout.ALIGN_TOP, R.id.cube);
        params.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.cube);
        params.addRule(RelativeLayout.RIGHT_OF, R.id.cube);

        algList.setLayoutParams(params);

        root.removeView(algList);
        root.addView(algList, params);

        ArrayAdapter<String> algAdapter =
                new ArrayAdapter<>(mContext, R.layout.item_alg_list_text, mCase.getAlgorithms());

        algList.setAdapter(algAdapter);
        nameText.setText(mCase.getName());

        algList.requestLayout();

        //progressBar.setProgress(algorithm.getProgress());

//        revertButton.setOnClickListener(clickListener);
//        progressButton.setOnClickListener(clickListener);
//        editButton.setOnClickListener(clickListener);
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
