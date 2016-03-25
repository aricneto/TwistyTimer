package com.aricneto.twistytimer.fragment.dialog;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.database.DatabaseHandler;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.listener.ExportImportDialogInterface;
import com.aricneto.twistytimer.utils.PuzzleUtils;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Ari on 22/03/2016.
 */
public class ExportImportSelectionDialog extends DialogFragment {

    ExportImportDialogInterface dialogInterface;
    @Bind(R.id.puzzleSpinner)   Spinner  puzzleSpinner;
    @Bind(R.id.categorySpinner) Spinner  categorySpinner;
    @Bind(R.id.dialogTitle)     TextView dialogTitle;
    @Bind(R.id.importButton)    TextView importButton;

    public final static int TYPE_EXPORT = 1;
    public final static int TYPE_IMPORT = 2;

    private final static String TYPE = "type";

    private int type;

    String currentPuzzle   = "333";
    String currentCategory = "Normal";

    ArrayAdapter<String> categoryAdapter;

    DatabaseHandler handler;

    public static ExportImportSelectionDialog newInstance(int type) {
        ExportImportSelectionDialog exportImportSelectionDialog = new ExportImportSelectionDialog();
        Bundle args = new Bundle();
        args.putInt(TYPE, type);
        exportImportSelectionDialog.setArguments(args);
        return exportImportSelectionDialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View dialogView = inflater.inflate(R.layout.dialog_export_import_selection_dialog, container);
        ButterKnife.bind(this, dialogView);
        handler = new DatabaseHandler(getContext());
        if (getArguments() != null) {
            type = getArguments().getInt(TYPE);
        }

        if (type == TYPE_EXPORT) {
            importButton.setText(R.string.action_export);
            dialogTitle.setText(getString(R.string.dialog_export_import_selection_title, getString(R.string.action_export).toLowerCase()));
        } else {
            importButton.setText(R.string.action_import);
            dialogTitle.setText(getString(R.string.dialog_export_import_selection_title, getString(R.string.action_import).toLowerCase()));
        }

        final ArrayAdapter puzzleAdapter = ArrayAdapter.createFromResource(getContext(), R.array.puzzles, android.R.layout.simple_spinner_dropdown_item);

        puzzleSpinner.setAdapter(puzzleAdapter);

        updateCategories();

        puzzleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                currentPuzzle = PuzzleUtils.getPuzzleInPosition(i);
                dialogInterface.onSelectPuzzle(currentPuzzle);
                updateCategories();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                dialogInterface.onSelectCategory(categoryAdapter.getItem(i));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        importButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (type == TYPE_EXPORT)
                    dialogInterface.onExportExternal();
                else
                    dialogInterface.onImportExternal();
                dismiss();
            }
        });

        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialogView;
    }

    private void updateCategories() {
        final List<String> subtypeList = handler.getAllSubtypesFromType(currentPuzzle);
        if (subtypeList.size() == 0) {
            subtypeList.add("Normal");
            handler.addSolve(new Solve(1, currentPuzzle, "Normal", 0L, "", PuzzleUtils.PENALTY_HIDETIME, "", true));
        }
        categoryAdapter =
                new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_dropdown_item, subtypeList);
        categorySpinner.setAdapter(categoryAdapter);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
        handler.closeDB();
    }

    public void setDialogInterface(ExportImportDialogInterface dialogInterface) {
        this.dialogInterface = dialogInterface;
    }
}
