package com.aricneto.twistytimer.fragment.dialog;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.aricneto.twistify.R;
import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.adapter.BottomSheetSpinnerAdapter;
import com.aricneto.twistytimer.database.DatabaseHandler;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.listener.DialogListenerMessage;
import com.aricneto.twistytimer.utils.PuzzleUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static com.aricneto.twistytimer.utils.TTIntent.ACTION_CHANGED_CATEGORY;
import static com.aricneto.twistytimer.utils.TTIntent.CATEGORY_UI_INTERACTIONS;
import static com.aricneto.twistytimer.utils.TTIntent.broadcast;

public class BottomSheetCategoryDialog extends BottomSheetDialogFragment {

    private static final String KEY_SAVEDSUBTYPE = "savedSubtype";

    @BindView(R.id.add_category)
    View addCategoryButton;
    @BindView(R.id.list)
    ListView listView;

    // Puzzle and subtypes that are currently selected
    String currentPuzzle;
    String currentSubtype;
    List<String> subtypeList;

    // Subtype that's currently being edited
    String currentEditSubtype = "";

    private Unbinder mUnbinder;

    private DialogListenerMessage dialogListenerMessage;

    private BottomSheetSpinnerAdapter mAdapter;
    private AdapterView.OnItemClickListener mItemClickListener;
    private AdapterView.OnItemLongClickListener mItemLongClickListener;
    private View.OnClickListener mOnClickListener;

    public static BottomSheetCategoryDialog newInstance(String currentPuzzle, String currentPuzzleSubtype) {
        BottomSheetCategoryDialog dialog = new BottomSheetCategoryDialog();
        Bundle args = new Bundle();
        args.putString("puzzle", currentPuzzle);
        args.putString("subtype", currentPuzzleSubtype);
        dialog.setArguments(args);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_category_select, container, false);
        mUnbinder = ButterKnife.bind(this, view);

        return view;
    }

    @SuppressWarnings("RestrictedApi")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // retrieve arguments
        currentPuzzle = getArguments().getString("puzzle");
        currentSubtype = getArguments().getString("subtype");

        DatabaseHandler dbHandler = TwistyTimer.getDBHandler();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // get a list of all subtypes
        subtypeList = dbHandler.getAllSubtypesFromType(currentPuzzle);

        if (subtypeList.size() == 0) {
            // if subtype list is empty, create a new entry
            dbHandler.addSolve(new Solve(1, currentPuzzle, "Normal", 0L, "", PuzzleUtils.PENALTY_HIDETIME, "", true));
        } else if (subtypeList.size() == 1) {
            currentSubtype = subtypeList.get(0);
        }

        updateList(dbHandler);

        // Create subtype
        MaterialDialog createSubtypeDialog = new MaterialDialog.Builder(getContext())
                .title(R.string.enter_type_name)
                .inputRange(2, 16)
                .input(R.string.enter_type_name, 0, false, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog materialDialog, CharSequence input) {
                        // add a single hidden solve with that category name to save it
                        dbHandler.addSolve(new Solve(1, currentPuzzle, input.toString(), 0L, "", PuzzleUtils.PENALTY_HIDETIME, "", true));
                        currentSubtype = input.toString();
                        updateList(dbHandler);

                        editor.putString(KEY_SAVEDSUBTYPE + currentPuzzle, currentSubtype);
                        editor.apply();
                        dialogListenerMessage.onUpdateDialog(currentSubtype);
                    }
                })
                .build();

        Context context = getContext();

        // click listeners
        // select a subtype
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // A subtype was selected
                currentSubtype = subtypeList.get(position);

                editor.putString(KEY_SAVEDSUBTYPE + currentPuzzle, currentSubtype);
                editor.apply();
                dialogListenerMessage.onUpdateDialog(currentSubtype);
                dismiss();
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                // Create a popup menu for each entry
                MenuBuilder menuBuilder = new MenuBuilder(context);
                MenuInflater inflater = new MenuInflater(context);

                inflater.inflate(R.menu.menu_category_options, menuBuilder);
                MenuPopupHelper popupHelper = new MenuPopupHelper(context, menuBuilder, view);
                popupHelper.setForceShowIcon(true);
                popupHelper.setGravity(Gravity.RIGHT);

                menuBuilder.setCallback(new MenuBuilder.Callback() {
                    @Override
                    public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.rename:
                                currentEditSubtype = subtypeList.get(position);
                                //Rename subtype
                                new MaterialDialog.Builder(getContext())
                                        .title(R.string.enter_new_name_dialog)
                                        .input("", "", false, new MaterialDialog.InputCallback() {
                                            @Override
                                            public void onInput(MaterialDialog dialog, CharSequence input) {
                                                dbHandler.renameSubtype(currentPuzzle, currentEditSubtype, input.toString());
                                                currentSubtype = input.toString();
                                                updateList(dbHandler);

                                                editor.putString(KEY_SAVEDSUBTYPE + currentPuzzle, currentSubtype);
                                                editor.apply();
                                                dialogListenerMessage.onUpdateDialog(currentSubtype);
                                            }
                                        })
                                        .inputRange(2, 16)
                                        .positiveText(R.string.action_done)
                                        .negativeText(R.string.action_cancel)
                                        .show();
                                break;
                            case R.id.remove:
                                currentEditSubtype = subtypeList.get(position);
                                // Remove Subtype dialog
                                new MaterialDialog.Builder(getContext())
                                        .title(R.string.remove_subtype_confirmation)
                                        .content(getString(R.string.remove_subtype_confirmation_content) + " \"" + currentEditSubtype + "\"?\n" + getString(R.string.remove_subtype_confirmation_content_continuation))
                                        .positiveText(R.string.action_remove)
                                        .negativeText(R.string.action_cancel)
                                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                                            @Override
                                            public void onClick(MaterialDialog dialog, DialogAction which) {
                                                dbHandler.deleteSubtype(currentPuzzle, currentEditSubtype);
                                                // After removing, change current subtype to first of list, if none exist, create "Normal" subtype
                                                if (subtypeList.size() > 1) {
                                                    currentSubtype = dbHandler.getAllSubtypesFromType(currentPuzzle).get(0);
                                                } else {
                                                    currentSubtype = "Normal";
                                                }
                                                updateList(dbHandler);

                                                editor.putString(KEY_SAVEDSUBTYPE + currentPuzzle, currentSubtype);
                                                editor.apply();
                                                dialogListenerMessage.onUpdateDialog(currentSubtype);
                                            }
                                        })
                                        .show();
                                break;
                        }
                        return false;
                    }

                    @Override
                    public void onMenuModeChange(MenuBuilder menu) {

                    }
                });

                popupHelper.show();
                return true;
            }
        });
        addCategoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createSubtypeDialog.show();
            }
        });
    }

    private void updateList(DatabaseHandler dbHandler) {
        subtypeList = dbHandler.getAllSubtypesFromType(currentPuzzle);
        int[] icons = {};
        mAdapter = new BottomSheetSpinnerAdapter(getContext(), subtypeList.toArray(new String[0]), icons);
        listView.setAdapter(mAdapter);
    }

    public void setDialogListener(DialogListenerMessage dialogListener) {
        this.dialogListenerMessage = dialogListener;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }
}
