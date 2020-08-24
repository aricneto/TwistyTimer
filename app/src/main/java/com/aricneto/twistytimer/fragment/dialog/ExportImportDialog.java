package com.aricneto.twistytimer.fragment.dialog;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.folderselector.FileChooserDialog;
import com.aricneto.twistify.R;
import com.aricneto.twistytimer.utils.AnimUtils;
import com.aricneto.twistytimer.utils.ExportImportUtils;
import com.aricneto.twistytimer.utils.ThemeUtils;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * <p>
 * A dialog fragment that initiates the import and export of solve times. This dialog presents the
 * user with options to export solve times or to import solve times, and for each of those options
 * to use the simple text ("external") format or the back-up format. If the user chooses to import
 * times, a file must be chosen. When exporting, the file name and location is chosen automatically
 * by the application. If the user chooses to use the "external" format, this dialog hands over to
 * {@link PuzzleChooserDialog} to allow the user to select the puzzle type and category. Both
 * the file chooser and the puzzle chooser fragments report their choices back to this fragment via
 * the common parent activity. Once this dialog has assembled all of the necessary details for the
 * operation, it calls back to the activity to initiate the import or export operation.
 * </p>
 * <p>
 * <i>This dialog fragment <b>must</b> be used in the context of an activity that implements the
 * {@link ExportImportCallbacks} and {@code FileChooserDialog.FileCallback} interfaces and extends
 * the {@code AppCompatActivity} class, or exceptions will occur.</i>
 * </p>
 */
public class ExportImportDialog extends DialogFragment
        implements FileChooserDialog.FileCallback, PuzzleChooserDialog.PuzzleCallback {
    /**
     * The file format for simple text import/export of solve times for a single puzzle type and
     * category that can be interchanged with external applications.
     */
    public static final int EXIM_FORMAT_EXTERNAL = 1;

    /**
     * The file format for full text import/export of all solve times used to back-up the database.
     */
    public static final int EXIM_FORMAT_BACKUP = 2;

    /**
     * The tag for the {@code FileChooserDialog} called by this fragment
     */
    public static final String FRAG_TAG_EXIM_FILECHOOSER = "exim_file_chooser";
    private Context mContext;


    @Override
    public void onFileChooserDismissed(@NonNull FileChooserDialog dialog) {

    }

    /**
     * A call-back interface that supports interaction between the import/export dialogs, file chooser
     * dialogs, and the main activity that will perform the actual import/export operations.
     */
    public interface ExportImportCallbacks {
        /**
         * Instructs the listener to begin importing solve times from a file.
         *
         * @param file
         *     The file from which to import the solve times.
         * @param fileFormat
         *     The solve file format. Must be either {@link #EXIM_FORMAT_EXTERNAL} or
         *     {@link #EXIM_FORMAT_BACKUP}.
         * @param puzzleType
         *     The type of the puzzle whose times will be imported. This is required when
         *     {@code fileFormat} is {@code EXIM_FORMAT_EXTERNAL}. It may be {@code null} if the
         *     format is {@code EXIM_FORMAT_BACKUP}, as it will not be used.
         * @param puzzleCategory
         *     The category (subtype) of the puzzle whose times will be imported. This is required
         *     when {@code fileFormat} is {@code EXIM_FORMAT_EXTERNAL}. It may be {@code null} if
         *     the format is {@code EXIM_FORMAT_BACKUP}, as it will not be used.
         */
        void onImportSolveTimes(
                File file, int fileFormat, String puzzleType, String puzzleCategory);

        /**
         * Instructs the listener to begin exporting solve times to a file. The export file name and
         * directory will be chosen automatically.
         *
         * @param fileFormat
         *     The solve file format. Must be either {@link #EXIM_FORMAT_EXTERNAL} or
         *     {@link #EXIM_FORMAT_BACKUP}.
         * @param puzzleType
         *     The type of the puzzle whose times will be exported. This is required when
         *     {@code fileFormat} is {@code EXIM_FORMAT_EXTERNAL}. It may be {@code null} if the
         *     format is {@code EXIM_FORMAT_BACKUP}, as it will not be used.
         * @param puzzleCategory
         *     The category (subtype) of the puzzle whose times will be exported. This is required
         *     when {@code fileFormat} is {@code EXIM_FORMAT_EXTERNAL}. It may be {@code null} if
         *     the format is {@code EXIM_FORMAT_BACKUP}, as it will not be used.
         */
        void onExportSolveTimes(int fileFormat, String puzzleType, String puzzleCategory);
    }

    private Unbinder mUnbinder;
    //@BindView(R.id.help_button)     View helpButton;
    @BindView(R.id.export_backup)   View exportBackup;
    @BindView(R.id.export_external) View exportExternal;
    @BindView(R.id.import_backup)   View importBackup;
    @BindView(R.id.import_external) View importExternal;
    @BindView(R.id.import_button)   View importButton;
    @BindView(R.id.export_button)   View exportButton;

    /**
     * The file chosen as the source for importing solve times.
     */
    private File mImportFile;

    /**
     * The format of the file to import.
     */
    private int mFileFormat;

    public static ExportImportDialog newInstance() {
        return new ExportImportDialog();
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // NOTE: The call-backs from the "FileChooserDialog" and "PuzzleChooserDialog"
            //
            switch (view.getId()) {
                case R.id.export_backup:
                    // All puzzle types and categories are exported to a single back-up file.
                    // There is no need to identify the export file or the puzzle type/category.
                    // Just invoke the activity and dismiss this dialog.
                    getExImActivity().onExportSolveTimes(EXIM_FORMAT_BACKUP, null, null);
                    dismiss();
                    break;

                case R.id.export_external:
                    mFileFormat = EXIM_FORMAT_EXTERNAL;
                    // Select the single puzzle type and category that will be exported. When the
                    // call-back from this puzzle chooser is received ("onPuzzleTypeSelected"),
                    // this dialog will exit and hand control back to the activity to perform the
                    // export. The call-back uses "getTag()" to tell the chooser to tell the
                    // activity that the chosen puzzle type/category should be relayed back to this
                    // dialog fragment.
                    PuzzleChooserDialog.newInstance(
                            R.string.action_export, ExportImportDialog.this.getTag())
                            .show(getActivity().getSupportFragmentManager(), null);
                    break;

                case R.id.import_backup:
                    mFileFormat = EXIM_FORMAT_BACKUP;
                    // Select the file to import. When the call-back from this file chooser is
                    // received ("onFileSelection"), this dialog will exit and hand control back
                    // to the activity to perform the export. The call-back uses "getTag()" to tell
                    // the chooser to tell the activity that the chosen puzzle type/category should
                    // be relayed back to this dialog fragment.
                    new FileChooserDialog.Builder(getExImActivity())
                            //.chooseButton(R.string.action_choose)
                            .tag(FRAG_TAG_EXIM_FILECHOOSER)
                            .extensionsFilter(".txt")
                            .build().show(getFragmentManager());
                    break;

                case R.id.import_external:
                    // Show a dialog that explains the required text format, then, when that is
                    // closed, select the file to import. When the call-back from this file chooser
                    // is received ("onFileSelection"), this dialog will check that the file name
                    // is valid. If valid, the puzzle chooser will be shown.  When the call-back
                    // from that puzzle chooser is received ("onPuzzleTypeSelected"), this dialog
                    // will exit and hand control back to the activity to perform the import.
                    mFileFormat = EXIM_FORMAT_EXTERNAL;
                    ThemeUtils.roundAndShowDialog(mContext, new MaterialDialog.Builder(mContext)
                            .title(R.string.import_external_title)
                            .content(R.string.import_external_content_first)
                            .positiveText(R.string.action_ok)
                            .onPositive((dialog, which) -> new FileChooserDialog.Builder(getExImActivity())
                                    .tag(FRAG_TAG_EXIM_FILECHOOSER)
                                    .extensionsFilter(".txt")
                                    .build().show(getFragmentManager()))
                            .build());
                    break;

                case R.id.export_button:
                    AnimUtils.toggleContentVisibility(exportBackup, exportExternal);
                    break;

                case R.id.import_button:
                    AnimUtils.toggleContentVisibility(importBackup, importExternal);
                    break;

                //case R.id.help_button:
                //    break;
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View dialogView = inflater.inflate(R.layout.dialog_export_import, container);
        mUnbinder = ButterKnife.bind(this, dialogView);
        mContext = getContext();

        exportBackup.setOnClickListener(clickListener);
        exportExternal.setOnClickListener(clickListener);
        importBackup.setOnClickListener(clickListener);
        importExternal.setOnClickListener(clickListener);
        //helpButton.setOnClickListener(clickListener);
        importButton.setOnClickListener(clickListener);
        exportButton.setOnClickListener(clickListener);

        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialogView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }

    /**
     * Gets the activity reference type cast to support the required interfaces and base classes
     * for export/import operations.
     *
     * @return The attached activity, or {@code null} if no activity is attached.
     */
    private <A extends AppCompatActivity & FileChooserDialog.FileCallback & ExportImportCallbacks>
    A getExImActivity() {
        //noinspection unchecked
        return (A) getActivity();
    }

    @Override
    public void onPuzzleSelected(
            @NonNull String tag, @NonNull String puzzleType, @NonNull String puzzleCategory) {
        // Importing or exporting to an "external" format file. The file will already have been
        // chosen if this is an import operation. Now that the puzzle type and category are known,
        // hand control back to the activity.
        if (mImportFile == null) {
            getExImActivity().onExportSolveTimes(mFileFormat, puzzleType, puzzleCategory);
        } else {
            getExImActivity().onImportSolveTimes(
                    mImportFile, mFileFormat, puzzleType, puzzleCategory);
        }
        dismiss();
    }

    /**
     * Handles the selection of a file when solve times are being imported. If the "external" file
     * format is chosen, the user must be further prompted to select the puzzle type and category
     * to assign to the imported solve times. If the chosen file does not end with the expected
     * file extension
     *
     * @param dialog The file chooser dialog that reported the selection.
     * @param file   The file that was chosen.
     */
    @Override
    public void onFileSelection(@NonNull FileChooserDialog dialog, @NonNull File file) {
        if (ExportImportUtils.isFileExtensionOK(file)) {
            mImportFile = file;

            if (mFileFormat == EXIM_FORMAT_EXTERNAL) {
                // Need to get the puzzle type and category before importing the data. There will
                // be a call-back to "onPuzzleSelected" before returning to the activity.
                PuzzleChooserDialog.newInstance(
                        R.string.action_import, ExportImportDialog.this.getTag())
                        .show(getActivity().getSupportFragmentManager(), null);
            } else {
                // Importing from a back-up file. There is no need to know the puzzle type and
                // category, as those are in the file. Hand back to the activity to do the import.
                getExImActivity().onImportSolveTimes(mImportFile, mFileFormat, null, null);
                dismiss();
            }
        } else {
            // TODO: ADD HELP
            ThemeUtils.roundAndShowDialog(mContext, new MaterialDialog.Builder(getActivity())
                .title(R.string.file_selection_error_title)
                .content(R.string.file_selection_error_content, ".txt")
                .positiveText(R.string.action_ok)
                .build());
            dismiss();
        }
    }
}
