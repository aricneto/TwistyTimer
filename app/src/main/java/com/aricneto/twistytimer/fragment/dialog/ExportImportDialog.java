package com.aricneto.twistytimer.fragment.dialog;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;

import com.afollestad.materialdialogs.folderselector.FileChooserDialog;
import com.aricneto.twistify.R;
import com.aricneto.twistytimer.activity.MainActivity;
import com.aricneto.twistytimer.database.DatabaseHandler;
import com.aricneto.twistytimer.utils.AnimUtils;

import org.joda.time.DateTime;

import java.io.File;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Ari on 09/02/2016.
 */
public class ExportImportDialog extends DialogFragment {


    @Bind(R.id.help_button)     View helpButton;
    @Bind(R.id.export_backup)   View exportBackup;
    @Bind(R.id.export_external) View exportExternal;
    @Bind(R.id.import_backup)   View importBackup;
    @Bind(R.id.import_external) View importExternal;
    @Bind(R.id.import_button)   View importButton;
    @Bind(R.id.export_button)   View exportButton;

    public static ExportImportDialog newInstance() {
        ExportImportDialog exportImportDialog = new ExportImportDialog();
        return exportImportDialog;
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            switch (view.getId()) {
                case R.id.export_backup:
                    if (isExternalStorageWritable()) {
                        File fileDir = new File(Environment.getExternalStorageDirectory() + "/TwistyTimer");
                        fileDir.mkdir();
                        DatabaseHandler handler = new DatabaseHandler(getContext());
                        if (handler.backupDatabaseCSV(fileDir, "Solves_" + DateTime.now().toString("yMMdd'_'kkmmss") + ".csv")) {
                            Toast.makeText(getContext(), getString(R.string.saved_to) + " " + fileDir.getAbsolutePath()
                                    + "/Solves_" + DateTime.now().toString("yMMdd'_'kkmmss") + ".csv", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getContext(), getString(R.string.save_error), Toast.LENGTH_LONG).show();
                        }
                    }
                    dismiss();
                    break;
                case R.id.import_backup:
                    new FileChooserDialog.Builder((MainActivity) getActivity())
                            .chooseButton(R.string.action_choose)
                            .tag("import_backup")
                            .show();
                    dismiss();
                    break;
                case R.id.export_button:
                    AnimUtils.toggleContentVisibility(exportBackup, exportExternal);
                    break;
                case R.id.import_button:
                    AnimUtils.toggleContentVisibility(importBackup, importExternal);
                    break;
                case R.id.help_button:
                    break;
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View dialogView = inflater.inflate(R.layout.dialog_export_import, container);
        ButterKnife.bind(this, dialogView);

        exportBackup.setOnClickListener(clickListener);
        exportExternal.setOnClickListener(clickListener);
        importBackup.setOnClickListener(clickListener);
        importExternal.setOnClickListener(clickListener);
        helpButton.setOnClickListener(clickListener);
        importButton.setOnClickListener(clickListener);
        exportButton.setOnClickListener(clickListener);

        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialogView;
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private void quitDialog() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.finish();
            activity.startActivity(new Intent(activity, MainActivity.class));
        }
    }
}
