package com.aricneto.twistytimer.activity;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.folderselector.FileChooserDialog;
import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.PurchaseInfo;
import com.aricneto.twistify.BuildConfig;
import com.aricneto.twistify.R;
import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.database.DatabaseHandler;
import com.aricneto.twistytimer.fragment.AlgListFragment;
import com.aricneto.twistytimer.fragment.TimerFragment;
import com.aricneto.twistytimer.fragment.TimerFragmentMain;
import com.aricneto.twistytimer.fragment.dialog.DonateDialog;
import com.aricneto.twistytimer.fragment.dialog.ExportImportDialog;
import com.aricneto.twistytimer.fragment.dialog.PuzzleChooserDialog;
import com.aricneto.twistytimer.fragment.dialog.SchemeSelectDialogMain;
import com.aricneto.twistytimer.fragment.dialog.ThemeSelectDialog;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.listener.OnBackPressedInFragmentListener;
import com.aricneto.twistytimer.puzzle.TrainerScrambler;
import com.aricneto.twistytimer.utils.ExportImportUtils;
import com.aricneto.twistytimer.utils.LocaleUtils;
import com.aricneto.twistytimer.utils.Prefs;
import com.aricneto.twistytimer.utils.PuzzleUtils;
import com.aricneto.twistytimer.utils.StoreUtils;
import com.aricneto.twistytimer.utils.ThemeUtils;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.ExpandableDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.opencsv.CSVReader;

import org.joda.time.DateTime;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import butterknife.ButterKnife;

import static com.aricneto.twistytimer.database.DatabaseHandler.IDX_COMMENT;
import static com.aricneto.twistytimer.database.DatabaseHandler.IDX_DATE;
import static com.aricneto.twistytimer.database.DatabaseHandler.IDX_PENALTY;
import static com.aricneto.twistytimer.database.DatabaseHandler.IDX_SCRAMBLE;
import static com.aricneto.twistytimer.database.DatabaseHandler.IDX_SUBTYPE;
import static com.aricneto.twistytimer.database.DatabaseHandler.IDX_TIME;
import static com.aricneto.twistytimer.database.DatabaseHandler.IDX_TYPE;
import static com.aricneto.twistytimer.database.DatabaseHandler.ProgressListener;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_TIMES_MODIFIED;
import static com.aricneto.twistytimer.utils.TTIntent.CATEGORY_TIME_DATA_CHANGES;
import static com.aricneto.twistytimer.utils.TTIntent.broadcast;

public class MainActivity extends AppCompatActivity
        implements BillingProcessor.IBillingHandler, FileChooserDialog.FileCallback,
                   ExportImportDialog.ExportImportCallbacks, PuzzleChooserDialog.PuzzleCallback {
    /**
     * Flag to enable debug logging for this class.
     */
    private static final boolean DEBUG_ME = false;

    /**
     * A "tag" to identify this class in log messages.
     */
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int DEBUG_ID         = 11;
    private static final int TIMER_ID         = 1;
    private static final int THEME_ID         = 2;
    private static final int SCHEME_ID        = 9;
    private static final int OLL_ID           = 6;
    private static final int PLL_ID           = 7;
    private static final int DONATE_ID        = 8;
    private static final int EXPORT_IMPORT_ID = 10;
    private static final int ABOUT_ID         = 4;
    private static final int SETTINGS_ID      = 5;
    private static final int TRAINER_OLL_ID      = 14;
    private static final int TRAINER_PLL_ID      = 15;


    private static final int REQUEST_SETTING           = 42;
    private static final int REQUEST_ABOUT             = 23;
    private static final int STORAGE_PERMISSION_CODE   = 11;

    /**
     * The fragment tag identifying the export/import dialog fragment.
     */
    private static final String FRAG_TAG_EXIM_DIALOG = "export_import_dialog";

    // NOTE: Loader IDs used by fragments need to be unique within the context of an activity that
    // creates those fragments. Therefore, it is safer to define all of the IDs in the same place.

    /**
     * The loader ID for the loader that loads data presented in the statistics table on the timer
     * graph fragment and the summary statistics on the timer fragment.
     */
    public static final int STATISTICS_LOADER_ID = 101;

    /**
     * The loader ID for the loader that loads chart data presented in on the timer graph fragment.
     */
    public static final int CHART_DATA_LOADER_ID = 102;

    /**
     * The loader ID for the loader that loads the list of solve times for the timer list fragment.
     */
    public static final int TIME_LIST_LOADER_ID = 103;

    /**
     * The loader ID for the loader that loads the list of algorithms for the algorithm list
     * fragment.
     */
    public static final int ALG_LIST_LOADER_ID = 104;

    BillingProcessor bp;

    SmoothActionBarDrawerToggle mDrawerToggle;
    FragmentManager             fragmentManager;
    DrawerLayout                mDrawerLayout;

    private Drawer          mDrawer;

    // True if billing is initialized
    private boolean readyToPurchase = false;

    /**
     * Sets drawer lock mode
     * {@code DrawerLayout.LOCK_MODE_LOCKED_CLOSED} for force closed and
     * {@code DrawerLayout.LOCK_MODE_LOCKED_UNDEFINED} for default behaviour
     */
    public void setDrawerLock(int lockMode) {
        mDrawer.getDrawerLayout().setDrawerLockMode(lockMode);
    }

    public void openDrawer() {
        mDrawer.openDrawer();
    }

    public void closeDrawer() {
        mDrawer.closeDrawer();
    }

    public BillingProcessor getBp () {
        return bp;
    }

    public void purchase(String productId) {
        bp.purchase(MainActivity.this, productId);;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (DEBUG_ME) Log.d(TAG, "updateLocale(savedInstanceState="
                + savedInstanceState + "): " + this);

        setTheme(ThemeUtils.getPreferredTheme());

        // Set text styling
        if (!Prefs.getString(R.string.pk_text_style, "default").equals("default")) {
            getTheme().applyStyle(ThemeUtils.getPreferredTextStyle(), true);
        }

        // Set navigation bar tint
        if (Prefs.getBoolean(R.string.pk_tint_navigation_bar, false)) {
            getTheme().applyStyle(R.style.TintedNavigationBar, true);
            // Set navigation bar icon tint
            if (ThemeUtils.fetchAttrBool(this, ThemeUtils.getPreferredTheme(), R.styleable.BaseTwistyTheme_isLightTheme)) {
                getTheme().applyStyle(R.style.LightNavBarIconStyle, true);
            }
        }



        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        bp = new BillingProcessor(this, null, this);

        fragmentManager = getSupportFragmentManager();

        if (savedInstanceState == null) {
            fragmentManager
                .beginTransaction()
                .replace(R.id.main_activity_container, TimerFragmentMain.newInstance(PuzzleUtils.TYPE_333, "Normal", TimerFragment.TIMER_MODE_TIMER, TrainerScrambler.TrainerSubset.OLL), "fragment_main")
                .commit();
        }

        handleDrawer(savedInstanceState);
    }

    /* NOTE: Leaving this here (commented out) as it may be useful again (probably soon).
    @Override
    protected void onResume() {
        if (DEBUG_ME) Log.d(TAG, "onResume(): " + this);
        super.onResume();
    }

    @Override
    protected void onPause() {
        // Method overridden just for logging. Tracing issues on return from "Settings".
        if (DEBUG_ME) Log.d(TAG, "onPause(): " + this);
        super.onPause();
    }

    @Override
    protected void onStart() {
        // Method overridden just for logging. Tracing issues on return from "Settings".
        if (DEBUG_ME) Log.d(TAG, "onStart(): " + this);
        super.onStart();
    }

    @Override
    protected void onStop() {
        // Method overridden just for logging. Tracing issues on return from "Settings".
        if (DEBUG_ME) Log.d(TAG, "onStop(): " + this);
        super.onStop();
    }
   */

    private void handleDrawer(Bundle savedInstanceState) {
        ImageView headerView = (ImageView) View.inflate(this, R.layout.drawer_header, null);

        //headerView.setImageDrawable(
        //       ThemeUtils.fetchTintedDrawable(this, R.drawable.menu_header, R.attr.colorPrimary));

        // Setup drawer
        DrawerBuilder drawerBuilder = new DrawerBuilder()
                .withActivity(this)
                .withDelayOnDrawerClose(-1)
                .withHeader(headerView)
                .addDrawerItems(
                        new PrimaryDrawerItem()
                                .withName(R.string.drawer_title_timer)
                                .withIcon(R.drawable.ic_outline_timer_24px)
                                .withIconTintingEnabled(true)
                                .withIdentifier(TIMER_ID),

                        new ExpandableDrawerItem()
                                .withName(R.string.drawer_title_trainer)
                                .withIcon(R.drawable.ic_outline_control_camera_24px)
                                .withSelectable(false)
                                .withIconTintingEnabled(true)
                                .withSubItems(
                                        new SecondaryDrawerItem()
                                                .withName(R.string.drawer_title_oll)
                                                .withLevel(2)
                                                .withIcon(R.drawable.ic_oll_black_24dp)
                                                .withIconTintingEnabled(true)
                                                .withIdentifier(TRAINER_OLL_ID),
                                        new SecondaryDrawerItem()
                                                .withName(R.string.drawer_title_pll)
                                                .withLevel(2)
                                                .withIcon(R.drawable.ic_pll_black_24dp)
                                                .withIconTintingEnabled(true)
                                                .withIdentifier(TRAINER_PLL_ID)),

                        new ExpandableDrawerItem()
                                .withName(R.string.title_algorithms)
                                .withIcon(R.drawable.ic_outline_library_books_24px)
                                .withSelectable(false)
                                .withIconTintingEnabled(true)
                                .withSubItems(
                                        new SecondaryDrawerItem()
                                                .withName(R.string.drawer_title_oll)
                                                .withLevel(2)
                                                .withIcon(R.drawable.ic_oll_black_24dp)
                                                .withIconTintingEnabled(true)
                                                .withIdentifier(OLL_ID),
                                        new SecondaryDrawerItem()
                                                .withName(R.string.drawer_title_pll)
                                                .withLevel(2)
                                                .withIcon(R.drawable.ic_pll_black_24dp)
                                                .withIconTintingEnabled(true)
                                                .withIdentifier(PLL_ID)),

                        new SectionDrawerItem()
                                .withName(R.string.drawer_title_other),

                        new PrimaryDrawerItem()
                                .withName(R.string.drawer_title_export_import)
                                .withIcon(R.drawable.ic_outline_folder_24px)
                                .withIconTintingEnabled(true)
                                .withSelectable(false)
                                .withIdentifier(EXPORT_IMPORT_ID),

                        new PrimaryDrawerItem()
                                .withName(R.string.drawer_title_changeTheme)
                                .withIcon(R.drawable.ic_outline_palette_24px)
                                .withIconTintingEnabled(true)
                                .withSelectable(false)
                                .withIdentifier(THEME_ID),

                        new PrimaryDrawerItem()
                                .withName(R.string.drawer_title_changeColorScheme)
                                .withIcon(R.drawable.ic_outline_format_paint_24px)
                                .withIconTintingEnabled(true)
                                .withSelectable(false)
                                .withIdentifier(SCHEME_ID),

                        new DividerDrawerItem(),

                        new PrimaryDrawerItem()
                                .withName(R.string.action_settings)
                                .withIcon(R.drawable.ic_outline_settings_24px)
                                .withIconTintingEnabled(true)
                                .withSelectable(false)
                                .withIdentifier(SETTINGS_ID),

                        new PrimaryDrawerItem()
                                .withName(R.string.action_donate)
                                .withIcon(R.drawable.ic_outline_favorite_border_24px)
                                .withIconTintingEnabled(true)
                                .withSelectable(false)
                                .withIdentifier(DONATE_ID),

                        new PrimaryDrawerItem()
                                .withName(R.string.drawer_about)
                                .withIcon(R.drawable.ic_outline_help_outline_24px)
                                .withIconTintingEnabled(true)
                                .withSelectable(false)
                                .withIdentifier(ABOUT_ID)
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {

                        boolean closeDrawer = true;

                        switch ((int) drawerItem.getIdentifier()) {
                            default:
                                closeDrawer = false;
                            case TIMER_ID:
                                mDrawerToggle.runWhenIdle(new Runnable() {
                                    @Override
                                    public void run() {
                                        fragmentManager
                                                .beginTransaction()
                                                .replace(R.id.main_activity_container,
                                                         TimerFragmentMain.newInstance(PuzzleUtils.TYPE_333, "Normal", TimerFragment.TIMER_MODE_TIMER, TrainerScrambler.TrainerSubset.PLL), "fragment_main")
                                                .commit();
                                    }
                                });
                                break;

                            case TRAINER_OLL_ID:
                                mDrawerToggle.runWhenIdle(new Runnable() {
                                    @Override
                                    public void run() {
                                        fragmentManager
                                                .beginTransaction()
                                                .replace(R.id.main_activity_container,
                                                         TimerFragmentMain.newInstance(TrainerScrambler.TrainerSubset.OLL.name(), "Normal", TimerFragment.TIMER_MODE_TRAINER, TrainerScrambler.TrainerSubset.OLL), "fragment_main")
                                                .commit();
                                    }
                                });
                                break;

                            case TRAINER_PLL_ID:
                                mDrawerToggle.runWhenIdle(new Runnable() {
                                    @Override
                                    public void run() {
                                        fragmentManager
                                                .beginTransaction()
                                                .replace(R.id.main_activity_container,
                                                         TimerFragmentMain.newInstance(TrainerScrambler.TrainerSubset.PLL.name(), "Normal", TimerFragment.TIMER_MODE_TRAINER, TrainerScrambler.TrainerSubset.PLL), "fragment_main")
                                                .commit();
                                    }
                                });
                                break;

                            case OLL_ID:
                                mDrawerToggle.runWhenIdle(new Runnable() {
                                    @Override
                                    public void run() {
                                        fragmentManager
                                                .beginTransaction()
                                                .replace(R.id.main_activity_container,
                                                         AlgListFragment.newInstance(DatabaseHandler.SUBSET_OLL),
                                                         "fragment_algs_oll")
                                                .commit();
                                    }
                                });
                                break;

                            case PLL_ID:
                                mDrawerToggle.runWhenIdle(new Runnable() {
                                    @Override
                                    public void run() {
                                        fragmentManager
                                                .beginTransaction()
                                                .replace(R.id.main_activity_container,
                                                         AlgListFragment.newInstance(DatabaseHandler.SUBSET_PLL),
                                                         "fragment_algs_pll")
                                                .commit();
                                    }
                                });
                                break;

                            case EXPORT_IMPORT_ID:
                                if (ContextCompat.checkSelfPermission(MainActivity.this,
                                                                      Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    != PackageManager.PERMISSION_GRANTED) {

                                    // Should we show an explanation?
                                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                                            MainActivity.this,
                                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                                        ThemeUtils.roundAndShowDialog(MainActivity.this, new MaterialDialog.Builder(MainActivity.this)
                                                .content(R.string.permission_denied_explanation)
                                                .positiveText(R.string.action_ok)
                                                .negativeText(R.string.action_cancel)
                                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                                    @Override
                                                    public void onClick(
                                                            @NonNull MaterialDialog dialog,
                                                            @NonNull DialogAction which) {
                                                        ActivityCompat.requestPermissions(MainActivity.this,
                                                                                          new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                                                          STORAGE_PERMISSION_CODE);
                                                    }
                                                })
                                                .build());

                                    } else {
                                        ActivityCompat.requestPermissions(MainActivity.this,
                                                                          new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                                          STORAGE_PERMISSION_CODE);
                                    }

                                } else {
                                    ExportImportDialog.newInstance()
                                            .show(fragmentManager, FRAG_TAG_EXIM_DIALOG);
                                }
                                break;

                            case THEME_ID:
                                ThemeSelectDialog.newInstance().show(fragmentManager, "theme_dialog");
                                break;

                            case SCHEME_ID:
                                SchemeSelectDialogMain.newInstance()
                                        .show(fragmentManager, "scheme_dialog");
                                break;

                            case SETTINGS_ID:
                                mDrawerToggle.runWhenIdle(new Runnable() {
                                    @Override
                                    public void run() {
                                        startActivityForResult(new Intent(
                                                                       getApplicationContext(), SettingsActivity.class),
                                                               REQUEST_SETTING);
                                    }
                                });
                                break;

                            case DONATE_ID:
                                if (readyToPurchase && BillingProcessor.isIabServiceAvailable(MainActivity.this))
                                    DonateDialog.newInstance()
                                            .show(fragmentManager, "donate_dialog");
                                else
                                    Toast.makeText(MainActivity.this, "Google Play not available",
                                                   Toast.LENGTH_LONG).show();
                                break;

                            case ABOUT_ID:
                                mDrawerToggle.runWhenIdle(new Runnable() {
                                    @Override
                                    public void run() {
                                        startActivityForResult(new Intent(getApplicationContext(),
                                                                          AboutActivity.class), REQUEST_ABOUT);
                                    }
                                });
                                break;

                            case DEBUG_ID:
                                if (BuildConfig.DEBUG) {
                                    Random rand = new Random();
                                    DatabaseHandler dbHandler = TwistyTimer.getDBHandler();
                                    for (int i = 0; i < 10000; i++) {
                                        dbHandler.addSolve(new Solve(30000 + rand.nextInt(6000), "333",
                                                                     "|<<# DEBUG #>>|", 165165l+(i*10), "", 0, "", rand.nextBoolean()));
                                    }
                                }
                                break;
                        }
                        if (closeDrawer)
                            mDrawerLayout.closeDrawers();
                        return false;
                    }
                })
                .withSavedInstance(savedInstanceState);

        if (BuildConfig.DEBUG) {
            drawerBuilder.addDrawerItems(
                    new SectionDrawerItem()
                            .withName("DEBUG"),

                    new PrimaryDrawerItem()
                            .withName("DEBUG OPTION - ADD 10000 SOLVES")
                            .withIcon(R.drawable.ic_outline_help_outline_24px)
                            .withIconTintingEnabled(true)
                            .withSelectable(false)
                            .withIdentifier(DEBUG_ID)
            );
        }

        mDrawer = drawerBuilder.build();

        mDrawerLayout = mDrawer.getDrawerLayout();
        mDrawerToggle = new SmoothActionBarDrawerToggle(
                this, mDrawerLayout, null, R.string.drawer_open, R.string.drawer_close);
        mDrawerLayout.setDrawerListener(mDrawerToggle);

    }

    @Override
    public void onBillingInitialized() {
        /*
         * Called when BillingProcessor was initialized and it's ready to purchase
         */
        readyToPurchase = true;
    }

    @Override
    public void onProductPurchased(String productId, PurchaseInfo purchaseInfo) {
        /*
         * Called when requested PRODUCT ID was successfully purchased
         */
        bp.consumePurchaseAsync(productId, new BillingProcessor.IPurchasesResponseListener() {
            @Override
            public void onPurchasesSuccess() {
                Log.d(TAG, "onPurchasesSuccess: ");
            }

            @Override
            public void onPurchasesError() {
                Log.d(TAG, "onPurchasesError: ");
            }
        });
    }

    @Override
    public void onBillingError(int errorCode, Throwable error) {
        /*
         * Called when some error occurred. See Constants class for more details
         */
    }

    @Override
    public void onPurchaseHistoryRestored() {
        /*
         * Called when purchase history was restored and the list of all owned PRODUCT ID's
         * was loaded from Google Play
         */
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (DEBUG_ME) Log.d(TAG, "onActivityResult(requestCode=" + requestCode
                + ", resultCode=" + resultCode + ", data=" + data + "): " + this);
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_SETTING) {
            if (DEBUG_ME) Log.d(TAG, "  Returned from 'Settings'. Will recreate activity.");
                onRecreateRequired();
        }
    }

    /**
     * Handles the need to recreate this activity due to a major change affecting the activity and
     * its fragments. For example, if the theme is changed by {@link ThemeSelectDialog}, or if
     * unknown changes have been made to the preferences in {@link SettingsActivity}.
     */
    public void onRecreateRequired() {
        if (DEBUG_ME) Log.d(TAG, "onRecreationRequired(): " + this);

        // IMPORTANT: If this is not posted to the message queue, i.e., if "recreate()" is simply
        // called directly from "onRecreateRequired()" (or even if a flag is set here and
        // "recreate()" is called later from "onResume()", the recreation goes very wrong. After
        // the newly created activity instance calls "onResume()" it immediately calls "onPause()"
        // for no apparent reason whatsoever. The activity is clearly not "paused", as it the UI is
        // perfectly responsive. However, the next time it actually needs to pause, an exception is
        // logged complaining, "Performing pause of activity that is not resumed".
        //
        // Perhaps the issue is caused by an incorrect synchronisation of the destruction of the
        // old activity and the creation of the new activity. Whatever, simply posting the
        // "recreate()" call here seems to fix this. After posting, the (old) activity will
        // continue on and reach "onResume()" before then going through an orderly shutdown and
        // the new activity will be created and settle properly at "onResume()".
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if (DEBUG_ME) Log.d(TAG, "  Activity.recreate() NOW!: " + this);
                recreate();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (DEBUG_ME) Log.d(TAG, "onBackPressed()");

        if (mDrawer.isDrawerOpen()) {
            mDrawer.closeDrawer();
            return;
        }

        final Fragment mainFragment = fragmentManager.findFragmentByTag("fragment_main");

        if (mainFragment instanceof OnBackPressedInFragmentListener) { // => not null
            // If the main fragment is open, let it and its "child" fragments consume the "Back"
            // button press if necessary.
            if (((OnBackPressedInFragmentListener) mainFragment).onBackPressedInFragment()) {
                // Button press was consumed. Stop here.
                return;
            }
        }

        super.onBackPressed();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleUtils.updateLocale(newBase));
    }

    @Override
    protected void onDestroy() {
        if (DEBUG_ME) Log.d(TAG, "onDestroy(): " + this);
        if (bp != null)
            bp.release();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_overview, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (DEBUG_ME) Log.d(TAG, "onSaveInstanceState(): " + this);
        outState = mDrawer.saveInstanceState(outState);
        //outState.putBoolean(OPEN_EXPORT_IMPORT_DIALOG, openExportImportDialog);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onImportSolveTimes(File file, int fileFormat,
                                   String puzzleType, String puzzleCategory) {
        new ImportSolves(this, file, fileFormat, puzzleType, puzzleCategory)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onExportSolveTimes(int fileFormat, String puzzleType, String puzzleCategory) {
        if (!StoreUtils.isExternalStorageWritable()) {
            return;
        }

        if (fileFormat == ExportImportDialog.EXIM_FORMAT_BACKUP) {
            // Expect that all other parameters are null, otherwise something is very wrong.
            if (puzzleType != null || puzzleCategory != null) {
                throw new RuntimeException("Bug in the export code for the back-up format!");
            }

            final File file = ExportImportUtils.getBackupFileForExport();

            if (ExportImportUtils.ensureBackupExportDir()) {
                new ExportSolves(this, file, fileFormat, null, null)
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                // Unlikely, so just log it for now.
                Log.e(TAG, "Could not create output directory for back-up file: " + file);
            }
        } else if (fileFormat == ExportImportDialog.EXIM_FORMAT_EXTERNAL) {
            // Expect that all other parameters are non-null, otherwise something is very wrong.
            if (puzzleType == null || puzzleCategory == null) {
                throw new RuntimeException("Bug in the export code for the external format!");
            }

            final File file = ExportImportUtils.getExternalFileForExport(puzzleType, puzzleCategory);

            if (ExportImportUtils.ensureExternalExportDir()) {
                new ExportSolves(this, file, fileFormat, puzzleType, puzzleCategory)
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                // Unlikely, so just log it for now.
                Log.e(TAG, "Could not create output directory for back-up file: " + file);
            }
        } else {
            Log.e(TAG, "Unknown export file format: " + fileFormat);
        }
    }

    /**
     * Handles the call-back from a file chooser dialog when a file is selected. This is used for
     * communication between the {@code FileChooserDialog} and the export/import fragments. The
     * originating fragment that opened the file chooser dialog should set the "tag" of the file
     * chooser dialog to the value of the fragment tag that this activity uses to identify that
     * originating fragment. This activity will then forward this notification to that fragment,
     * which is expected to implement this same interface method.
     *
     * @param dialog
     *     The file chooser dialog that has reported the file selection.
     * @param file
     *     The file that was chosen.
     */
    @Override
    public void onFileSelection(@NonNull FileChooserDialog dialog, @NonNull File file) {
        // This "relay" scheme ensures that this activity is not embroiled in the gory details of
        // what the "destinationFrag" wanted with the file.
        final Fragment destinationFrag = fragmentManager.findFragmentByTag(FRAG_TAG_EXIM_DIALOG);

        if (destinationFrag instanceof FileChooserDialog.FileCallback) {
            ((FileChooserDialog.FileCallback) destinationFrag).onFileSelection(dialog, file);
        } else {
            // This is not expected unless there is a bug to be fixed.
            Log.e(TAG, "onFileSelection(): Unknown or incompatible fragment: " + destinationFrag.getTag());
        }
    }

    @Override
    public void onFileChooserDismissed(@NonNull FileChooserDialog dialog) {

    }

    /**
     * Handles the call-back from a fragment when a puzzle type and/or category are selected. This
     * is used for communication between the export/import fragments. The "source" fragment should
     * set the {@code tag} to the value of the fragment tag that this activity uses to identify
     * the "destination" fragment. This activity will then forward this notification to that
     * fragment, which is expected to implement this same interface method.
     */
    @Override
    public void onPuzzleSelected(
            @NonNull String tag, @NonNull String puzzleType, @NonNull String puzzleCategory) {
        // This "relay" scheme ensures that this activity is not embroiled in the gory details of
        // what the "destinationFrag" wanted with the puzzle type/category.
        final Fragment destinationFrag = fragmentManager.findFragmentByTag(tag);

        if (destinationFrag instanceof PuzzleChooserDialog.PuzzleCallback) {
            ((PuzzleChooserDialog.PuzzleCallback) destinationFrag)
                    .onPuzzleSelected(tag, puzzleType, puzzleCategory);
        } else {
            // This is not expected unless there is a bug to be fixed.
            Log.e(TAG, "onFileSelection(): Unknown or incompatible fragment: " + tag);
        }
    }

    private static class ExportSolves extends AsyncTask<Void, Integer, Boolean> {

        private final Activity  mContext;
        private final String   mPuzzleType;
        private final String   mPuzzleCategory;
        private final File     mFile;
        private final int      mFileFormat;

        private MaterialDialog mProgressDialog;

        /**
         * Creates a new task for exporting solve times to a file.
         *
         * @param context
         *     The context required to access resources and to report progress.
         * @param file
         *     The file to which to export the solve times.
         * @param fileFormat
         *     The solve file format, must be {@link ExportImportDialog#EXIM_FORMAT_EXTERNAL}, or
         *     {@link ExportImportDialog#EXIM_FORMAT_BACKUP}.
         * @param puzzleType
         *     The type of the puzzle whose times will be exported. This is required when
         *     {@code fileFormat} is {@code EXIM_FORMAT_EXTERNAL}. For {@code EXIM_FORMAT_BACKUP},
         *     it may be {@code null}, as it will not be used.
         * @param puzzleCategory
         *     The category (subtype) of the puzzle whose times will be exported. Required when
         *     {@code fileFormat} is {@code EXIM_FORMAT_EXTERNAL}. For {@code EXIM_FORMAT_BACKUP},
         *     it may be {@code null}, as it will not be used.
         */
        public ExportSolves(Activity context, File file, int fileFormat,
                            String puzzleType, String puzzleCategory) {
            mContext = context;
            mPuzzleType = puzzleType;
            mPuzzleCategory = puzzleCategory;
            mFile = file;
            mFileFormat = fileFormat;
        }

        @Override
        protected void onPreExecute() {
            mProgressDialog = ThemeUtils.roundDialog(mContext, new  MaterialDialog.Builder(mContext)
                .content(R.string.export_progress_title)
                .progress(false, 0, true)
                .cancelable(false)
                .build());
            mProgressDialog.show();
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (mProgressDialog.isShowing()) {
                if (values.length > 1) {
                    // values[1] is the number of solve times, which could legitimately be zero.
                    // Do not set max. to zero or it will display "NaN".
                    mProgressDialog.setMaxProgress(Math.max(values[1], 1));
                }
                mProgressDialog.setProgress(values[0]);
            }
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            Boolean returnCode;
            int exports = 0;

            try {
                final DatabaseHandler handler = TwistyTimer.getDBHandler();
                final Writer out = new BufferedWriter(new FileWriter(mFile));

                if (mFileFormat == ExportImportDialog.EXIM_FORMAT_BACKUP) {
                    String csvHeader
                            = "Puzzle,Category,Time(millis),Date(millis),Scramble,Penalty,Comment\n";
                    Cursor cursor = handler.getAllSolves();

                    try {
                        publishProgress(0, cursor.getCount());
                        out.write(csvHeader);

                        while (cursor.moveToNext()) {
                            out.write('"' + cursor.getString(IDX_TYPE)
                                    + "\";\"" + cursor.getString(IDX_SUBTYPE)
                                    + "\";\"" + cursor.getInt(IDX_TIME)
                                    + "\";\"" + cursor.getLong(IDX_DATE)
                                    + "\";\"" + cursor.getString(IDX_SCRAMBLE)
                                    + "\";\"" + cursor.getInt(IDX_PENALTY)
                                    + "\";\"" + cursor.getString(IDX_COMMENT)
                                    + "\"\n");
                            exports++;
                            publishProgress(exports);
                        }
                    } finally {
                        cursor.close();
                        out.close();
                    }
                    returnCode = true;
                } else if (mFileFormat == ExportImportDialog.EXIM_FORMAT_EXTERNAL) {
                    Cursor cursor = handler.getAllSolvesFrom(mPuzzleType, mPuzzleCategory);

                    try {
                        publishProgress(0, cursor.getCount());

                        while (cursor.moveToNext()) {
                            String csvValues
                                    = '"' + PuzzleUtils.convertTimeToString(cursor.getInt(IDX_TIME), PuzzleUtils.FORMAT_DEFAULT)
                                    + "\";\"" + cursor.getString(IDX_SCRAMBLE)
                                    + "\";\"" + new DateTime(cursor.getLong(IDX_DATE)).toString()
                                    + '"';

                            // Add optional "DNF" in fourth field.
                            if (cursor.getInt(IDX_PENALTY) == PuzzleUtils.PENALTY_DNF) {
                                csvValues += ";\"DNF\"";
                            }

                            csvValues += '\n';

                            out.write(csvValues);
                            exports++;
                            publishProgress(exports);
                        }
                    } finally {
                        cursor.close();
                        out.close();
                    }
                    returnCode = true;
                } else {
                    Log.e(TAG, "Unknown export file format: " + mFileFormat);
                    returnCode = false;
                }
            } catch (IOException e) {
                returnCode = false;
                Log.d("ERROR", "IOException: " + e.getMessage());
            }

            return returnCode;
        }

        @Override
        protected void onPostExecute(Boolean isExported) {
            if (mProgressDialog.isShowing()) {
                mProgressDialog.setActionButton(DialogAction.POSITIVE, R.string.action_done);

                if (isExported) {
                    mProgressDialog.setContent(
                            Html.fromHtml(mContext.getString(R.string.export_progress_complete)
                                          + "<br><br>" + "<small><tt>" + mFile.getAbsolutePath()
                                          + "</tt></small>"));
                    // Optional share action
                    mProgressDialog.setActionButton(DialogAction.NEUTRAL, R.string.list_options_item_share);
                    mProgressDialog.getBuilder().onNeutral((dialog, which) -> {
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);

                        Uri fileUri = FileProvider.getUriForFile(mContext,
                                mContext.getString(R.string.file_provider_authority), mFile);

                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                        shareIntent.setType("application/octet-stream");

                        // FileProvider can sometimes crash devices lower than Lollipop
                        // due to permission issues, so we have to do some magic to the intent
                        // This is explained in a Medium post by @quiro91
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                            shareIntent.setClipData(ClipData.newRawUri("", fileUri));
                            shareIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        }

                        mContext.startActivity(Intent.createChooser(shareIntent, "Share"));
                    });
                } else {
                    mProgressDialog.setContent(R.string.export_progress_error);
                }
            }
        }
    }

    private static class ImportSolves extends AsyncTask<Void, Integer, Void> {

        private final Context  mContext;
        private final File     mFile;
        private final int      mFileFormat;
        private final String   mPuzzleType;
        private final String   mPuzzleCategory;

        private MaterialDialog mProgressDialog;
        private int parseErrors = 0;
        private int duplicates  = 0;
        private int successes   = 0;

        /**
         * Creates a new task for importing solve times from a file.
         *
         * @param context
         *     The context required to access resources and to report progress.
         * @param file
         *     The file from which to import the solve times.
         * @param fileFormat
         *     The solve file format, must be {@link ExportImportDialog#EXIM_FORMAT_EXTERNAL}, or
         *     {@link ExportImportDialog#EXIM_FORMAT_BACKUP}.
         * @param puzzleType
         *     The type of the puzzle whose times will be imported. This is required when
         *     {@code fileFormat} is {@code EXIM_FORMAT_EXTERNAL}. For {@code EXIM_FORMAT_BACKUP},
         *     it may be {@code null}, as it will not be used.
         * @param puzzleCategory
         *     The category (subtype) of the puzzle whose times will be imported. Required when
         *     {@code fileFormat} is {@code EXIM_FORMAT_EXTERNAL}. For {@code EXIM_FORMAT_BACKUP},
         *     it may be {@code null}, as it will not be used.
         */
        public ImportSolves(Context context, File file, int fileFormat,
                            String puzzleType, String puzzleCategory) {
            mContext = context;
            mFile = file;
            mFileFormat = fileFormat;
            mPuzzleType = puzzleType;
            mPuzzleCategory = puzzleCategory;
        }

        @Override
        protected void onPreExecute() {
            mProgressDialog = ThemeUtils.roundDialog(mContext, new MaterialDialog.Builder(mContext)
                .content(R.string.import_progress_title)
                .progress(false, 0, true)
                .cancelable(false)
                .build());
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (mProgressDialog.isShowing()) {
                if (values.length > 1) {
                    // values[1] is the number of solve times, which could legitimately be zero.
                    // Do not set max. to zero or it will display "NaN".
                    mProgressDialog.setMaxProgress(Math.max(values[1], 1));
                }
                mProgressDialog.setProgress(values[0]);
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            List<Solve> solveList = new ArrayList<>();

            boolean importIntoArchive = Prefs.getBoolean(R.string.pk_import_archive, true);

            try {

                BufferedReader br = new BufferedReader(new FileReader(mFile));
                CSVReader csvReader = new CSVReader(br, ';', '"', true);
                String[] line;

                if (mFileFormat == ExportImportDialog.EXIM_FORMAT_BACKUP) {
                    // throw away the header
                    csvReader.readNext();

                    while ((line = csvReader.readNext()) != null) {
                        try {
                            solveList.add(new Solve(
                                Integer.parseInt(line[2]), line[0], line[1], Long.parseLong(line[3]),
                                line[4], Integer.parseInt(line[5]), line[6], importIntoArchive));
                        } catch (Exception e) {
                            parseErrors++;
                        }
                    }
                } else if (mFileFormat == ExportImportDialog.EXIM_FORMAT_EXTERNAL) {
                    final long now = DateTime.now().getMillis();

                    while ((line = csvReader.readNext()) != null) {
                        if (line.length <= 4) {
                            try {
                                Log.d("IMPORTING EXTERNAL", "time: " + line[0]);

                                int time = PuzzleUtils.parseTime(line[0]);
                                String scramble = "";
                                long date = now;
                                int penalty = PuzzleUtils.NO_PENALTY;

                                if (line.length >= 2) {
                                    scramble = line[1];
                                }
                                if (line.length >= 3) {
                                    try {
                                        date = DateTime.parse(line[2]).getMillis();
                                    } catch (Exception e) {
                                        // "date" remains equal to "now".
                                        e.printStackTrace();
                                    }
                                }
                                // Optional fourth field (index 3) may contain "DNF". If it is
                                // something else, ignore it.
                                if (line.length >= 4 && "DNF".equals(line[3])) {
                                    penalty = PuzzleUtils.PENALTY_DNF;
                                }

                                solveList.add(new Solve(
                                        time, mPuzzleType, mPuzzleCategory,
                                        date, scramble, penalty, "", importIntoArchive));
                            } catch (Exception e) {
                                parseErrors++;
                            }
                        } else {
                            parseErrors++;
                        }
                    }
                } else {
                    Log.e(TAG, "Unknown import file format: " + mFileFormat);
                }

                final DatabaseHandler handler = TwistyTimer.getDBHandler();

                // Perform a bulk insertion of the solves.
                successes = handler.addSolves(mFileFormat, solveList, new ProgressListener() {
                            @Override
                            public void onProgress(int numCompleted, int total) {
                                publishProgress(numCompleted, total);
                            }
                        });
                duplicates = solveList.size() - successes;
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (mProgressDialog.isShowing()) {
                mProgressDialog.setActionButton(DialogAction.POSITIVE, R.string.action_done);
                mProgressDialog.setContent(Html.fromHtml(
                        mContext.getString(R.string.import_progress_content)
                        + "<br><br><small><tt>"
                        + "<b>" + successes + "</b> "
                        + mContext.getString(R.string.import_progress_content_successful_imports)
                        + "<br><b>" + duplicates + "</b> "
                        + mContext.getString(R.string.import_progress_content_ignored_duplicates)
                        + "<br><b>" + parseErrors + "</b> "
                        + mContext.getString(R.string.import_progress_content_errors)
                        + "</small></tt>"));
            }
            broadcast(CATEGORY_TIME_DATA_CHANGES, ACTION_TIMES_MODIFIED);
        }
    }

    // So the drawer doesn't lag when closing
    private class SmoothActionBarDrawerToggle extends ActionBarDrawerToggle {

        private Runnable runnable;

        public SmoothActionBarDrawerToggle(Activity activity, DrawerLayout drawerLayout, Toolbar toolbar, int openDrawerContentDescRes, int closeDrawerContentDescRes) {
            super(activity, drawerLayout, toolbar, openDrawerContentDescRes, closeDrawerContentDescRes);
        }

        @Override
        public void onDrawerStateChanged(int newState) {
            super.onDrawerStateChanged(newState);
            if (runnable != null && newState == DrawerLayout.STATE_IDLE) {
                runnable.run();
                runnable = null;
            }
        }

        public void runWhenIdle(Runnable runnable) {
            this.runnable = runnable;
        }
    }
}
