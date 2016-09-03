package com.aricneto.twistytimer.utils;

import android.os.Environment;
import android.support.annotation.NonNull;

import org.joda.time.DateTime;

import java.io.File;

/**
 * Utility methods and constants for the export and import of solve times to files.
 *
 * @author damo
 */
public final class ExportImportUtils {
    /**
     * The name of the export directory for "external" files in the simple interchange format. This
     * directory is relative to the external storage directory identified by the system.
     */
    private static final String EXTERNAL_EXPORT_DIR_NAME = "TwistyTimer";

    /**
     * The name of the export directory for back-up files. This directory is relative to the
     * external storage directory identified by the system.
     */
    private static final String BACKUP_EXPORT_DIR_NAME = EXTERNAL_EXPORT_DIR_NAME + "/Backup";

    /**
     * The expected file extension for imported files and the extension used for exported files.
     * The value includes the leading dot character.
     */
    // NOTE: While this has always been ".txt", maybe ".csv" would be a better choice.
    private static final String FILE_NAME_EXT = ".txt";

    /**
     * The file name template for "external" format files. The are three string placeholders: the
     * first is for the puzzle type, the second for the puzzle category, and the third for the
     * time-stamp.
     */
    private static final String EXTERNAL_FILE_NAME_TEMPLATE = "Solves_%s_%s_%s" + FILE_NAME_EXT;

    /**
     * The file name template for "external" format files. There is one string placeholder for the
     * time-stamp.
     */
    private static final String BACKUP_FILE_NAME_TEMPLATE = "Backup_%s" + FILE_NAME_EXT;

    /**
     * The characters that might be problematic if used in file names on some filesystems. Most
     * common filesystems allows most of these characters, but some command shells, file managers,
     * file transfer application, or other tools can have difficulty with some of the characters.
     * It is best to just replace these with underscores.
     */
    private static final String PROBLEM_CHARACTERS = "?[]/\\=<>:;,'\"&$#*()|~`!{}%+\0";

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private ExportImportUtils() {
    }

    /**
     * Ensures that the export directory for "external" format files exists. If the directory does
     * not exist, it will be created. Call this before attempting to write to an export file.
     *
     * @return
     *     {@code true} if the directory is now ensured to exist; or {@code false} if the directory
     *     did not exist and could not be created.
     */
    public static boolean ensureExternalExportDir() {
        final File dir = new File(
                Environment.getExternalStorageDirectory(), EXTERNAL_EXPORT_DIR_NAME);

        // If the directory is not created, it is OK if that is because it already exists, but
        // it is a big problem if we tried to create it and it was not created.
        return dir.mkdirs() || dir.exists();
    }

    /**
     * Ensures that the export directory for back-up files exists. If the directory does not exist,
     * it will be created. Call this before attempting to write to an export file.
     *
     * @return
     *     {@code true} if the directory is now ensured to exist; or {@code false} if the directory
     *     did not exist and could not be created.
     */
    public static boolean ensureBackupExportDir() {
        final File dir = new File(
                Environment.getExternalStorageDirectory(), BACKUP_EXPORT_DIR_NAME);

        return dir.mkdirs() || dir.exists();
    }

    /**
     * Gets the file to create when exporting solve times in the "external" format. The file name
     * will include the puzzle type and category. Call {@link #ensureExternalExportDir()} before
     * attempting to create this file. Note, the file name will include a time-stamp in the local
     * time zone with a resolution of minutes; if files are created rapidly, or if there is a
     * change to the time (e.g., a daylight-savings change-over or system clock correction), the
     * name of an existing file may be returned. This is considered unlikely enough, and not
     * serious enough, that it can be disregarded.
     *
     * @param puzzleType     The name of the puzzle type.
     * @param puzzleCategory The name of the puzzle category.
     *
     * @return The file (with fill path) to use when exporting the solve times.
     */
    public static File getExternalFileForExport(String puzzleType, String puzzleCategory) {
        // Assume that the puzzle type is sanitary, but the category may be entered by the user,
        // so it may cause problems unless it is sanitized first.
        return new File(
                new File(Environment.getExternalStorageDirectory(), EXTERNAL_EXPORT_DIR_NAME),
                String.format(EXTERNAL_FILE_NAME_TEMPLATE,
                        puzzleType, sanitizeFileName(puzzleCategory), getFileTimeStamp()));
    }

    /**
     * Gets the file to create when exporting all solve times in the back-up format. The file name
     * will include a time-stamp. See {@link #getExternalFileForExport(String, String)} for a
     * caution about time-stamp. Call {@link #ensureBackupExportDir()} before attempting to create
     * this file.
     *
     * @return The file (with fill path) to use when exporting the solve times.
     */
    public static File getBackupFileForExport() {
        return new File(
                new File(Environment.getExternalStorageDirectory(), BACKUP_EXPORT_DIR_NAME),
                String.format(BACKUP_FILE_NAME_TEMPLATE, getFileTimeStamp()));
    }

    /**
     * Checks if the file name extension of the given file to confirm if it matches the expected
     * extension. This does not guarantee that the file format will be correct, but it will help
     * to weed out obviously erroneous extensions. The check is not case-sensitive.
     *
     * @param file The file whose extension is to be checked.
     *
     * @return {@code true} if the file extension is acceptable; or {@code false} if it is not.
     */
    public static boolean isFileExtensionOK(@NonNull File file) {
        return file.getName().toLowerCase().endsWith(FILE_NAME_EXT);
    }

    /**
     * Gets the time-stamp in the format appropriate for inclusion in the file names.
     *
     * @return The time-stamp.
     */
    private static String getFileTimeStamp() {
        return DateTime.now().toString("dd-MMM-y'_'kk-mm");
    }

    /**
     * Sanitizes the given string, so that it should be safe to use in the name of a file. The
     * translation assumes that the file name should be compatible with the common Android
     * filesystems (internal storage and external SD cards) or copied from an Android device to
     * the filesystem of a different device, such as PC. The process is not reversible; potentially
     * problematic characters are simply replaced with underscores.
     *
     * @param name
     *     The file name, or component of a file name, to be sanitized.
     *
     * @return
     *     A sanitized string with problematic characters converted to underscores; or an empty
     *     string if {@code name} is empty or {@code null}.
     */
    private static CharSequence sanitizeFileName(CharSequence name) {
        final StringBuilder s = new StringBuilder(name != null ? name : "");

        for (int i = 0; i < s.length(); i++) {
            if (PROBLEM_CHARACTERS.indexOf(s.charAt(i)) != -1) {
                s.setCharAt(i, '_');
            }
        }

        return s;
    }
}
