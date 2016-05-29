package com.aricneto.twistytimer.listener;

/**
 * Created by Ari on 22/03/2016.
 */
public interface ExportImportDialogInterface {

    void onImportExternal();
    void onExportExternal();
    void onExportBackup();
    void onSelectPuzzle(String puzzle);
    void onSelectCategory(String category);
}
