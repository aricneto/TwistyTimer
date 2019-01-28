package com.aricneto.twistytimer;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.aricneto.twistytimer.database.DatabaseHandler;
import com.aricneto.twistytimer.utils.LocaleUtils;

import net.danlew.android.joda.JodaTimeAndroid;

/**
 * Created by Ari on 28/07/2015.
 */
public class TwistyTimer extends Application {
    /**
     * The singleton instance of the database access handler.
     */
    private static DatabaseHandler sDBHandler;

    /**
     * The cached reference to the application context.
     */
    private static Context sAppContext;

    @Override
    public void onCreate() {
        super.onCreate();
        JodaTimeAndroid.init(this);
        //LeakCanary.install(this);

        sAppContext = getApplicationContext();

        // Create a singleton instance of the "DatabaseHandler" using the application context. This
        // avoids memory leaks elsewhere and is more convenient. There is ABSOLUTELY NO NEED to
        // close the database EVER. Closing it in an ad hoc fashion (as was done in fragments and
        // activities) leads to all sorts of concurrency problems, race conditions, etc. SQLite
        // handles all of the concurrency itself, so there is no problem there. Android/Linux will
        // ensure that the database is closed if the application process exits.
        //
        // Note that the database is not opened here; it will not be opened until the first call to
        // "getReadableDatabase" or "getWritableDatabase" and then the open database instance will
        // be cached (by "SQLiteOpenHelper", which is the base class of "DatabaseHandler"). Those
        // two methods should really only be called from a background task, though, as opening the
        // database (particularly for the first time) can take some time.
        sDBHandler = new DatabaseHandler();

        LocaleUtils.updateLocale(getAppContext());
    }

    /**
     * Gets the singleton instance of the database access handler. Do not close any database after
     * use!
     *
     * @return The database handler.
     */
    public static DatabaseHandler getDBHandler() {
        return sDBHandler;
    }

    /**
     * Gets a read-only database handle. Do <i>not</i> close the database when it is no longer
     * needed.
     *
     * @return A handle on a readable database.
     */
    public static SQLiteDatabase getReadableDB() {
        return getDBHandler().getReadableDatabase();
    }

    /**
     * Gets a read/write database handle. Do <i>not</i> close the database when it is no longer
     * needed.
     *
     * @return A handle on a readable and writable database.
     */
    public static SQLiteDatabase getWritableDB() {
        return getDBHandler().getWritableDatabase();
    }

    /**
     * Gets the application context. This is a convenience for cases where a full activity context
     * is not required. A full activity context is required to access theme attributes or inflate
     * layouts, but for other uses, such as accessing string resources, databases, broadcast
     * receivers, the application context is sufficient. Using the application context also avoid
     * memory leaks that can occur if an activity context is used inappropriately.
     *
     * @return The application context.
     */
    public static Context getAppContext() {
        return sAppContext;
    }
}
