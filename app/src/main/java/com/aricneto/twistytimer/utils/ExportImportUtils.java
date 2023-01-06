package com.aricneto.twistytimer.utils;

import org.joda.time.DateTime;

/**
 * Utility methods and constants for the export and import of solve times to files.
 *
 * @author damo
 */
public final class ExportImportUtils {
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
     * Gets the time-stamp in the format appropriate for inclusion in the file names.
     *
     * @return The time-stamp.
     */
    private static String getFileTimeStamp() {
        return DateTime.now().toString("y-MM-dd'_'kk-mm");
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

    /**
     * Gets the file name to create when exporting all solve times in the back-up format. The file
     * name will include a time-stamp. See {@link #getExternalFileNameForExport(String, String)} for
     * a caution about time-stamp
     *
     * @return The file name (with fill path) to use when exporting the solve times.
     */
    public static String getBackupFileNameForExport() {
        return String.format(BACKUP_FILE_NAME_TEMPLATE, getFileTimeStamp());
    }

    /**
     * Gets the file name to create when exporting solve times in the "external" format. The file
     * name will include the puzzle type and category.
     * Note, the file name will include a time-stamp in the local
     * time zone with a resolution of minutes; if files are created rapidly, or if there is a
     * change to the time (e.g., a daylight-savings change-over or system clock correction), the
     * name of an existing file may be returned.
     * In that case, ACTION_CREATE_DOCUMENT changes the file name.
     *
     * @param puzzleType     The name of the puzzle type.
     * @param puzzleCategory The name of the puzzle category.
     *
     * @return The file name (with fill path) to use when exporting the solve times.
     */
    public static String getExternalFileNameForExport(String puzzleType, String puzzleCategory) {
        // Assume that the puzzle type is sanitary, but the category may be entered by the user,
        // so it may cause problems unless it is sanitized first.
        return String.format(EXTERNAL_FILE_NAME_TEMPLATE,
                        puzzleType, sanitizeFileName(puzzleCategory), getFileTimeStamp());
    }
}
