package com.aricneto.twistytimer.activity;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
import com.anjlab.android.iab.v3.TransactionDetails;
import com.aricneto.twistify.R;
import com.aricneto.twistytimer.AppRater;
import com.aricneto.twistytimer.database.DatabaseHandler;
import com.aricneto.twistytimer.fragment.AlgListFragment;
import com.aricneto.twistytimer.fragment.TimerFragmentMain;
import com.aricneto.twistytimer.fragment.dialog.ExportImportDialog;
import com.aricneto.twistytimer.fragment.dialog.ExportImportSelectionDialog;
import com.aricneto.twistytimer.fragment.dialog.SchemeSelectDialogMain;
import com.aricneto.twistytimer.fragment.dialog.ThemeSelectDialog;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.listener.ExportImportDialogInterface;
import com.aricneto.twistytimer.utils.Broadcaster;
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

import butterknife.ButterKnife;

import static com.aricneto.twistytimer.database.DatabaseHandler.*;


public class MainActivity extends AppCompatActivity implements BillingProcessor.IBillingHandler,
    FileChooserDialog.FileCallback, ExportImportDialogInterface {

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

    private static final int REQUEST_SETTING           = 42;
    private static final int REQUEST_ABOUT             = 23;
    private static final int STORAGE_PERMISSION_CODE   = 11;

    private static final String OPEN_EXPORT_IMPORT_DIALOG = "open_export_import_dialog";

    final MainActivity mainActivity = this;

    BillingProcessor bp;

    SmoothActionBarDrawerToggle mDrawerToggle;
    FragmentManager             fragmentManager;
    DrawerLayout                mDrawerLayout;

    String exportImportPuzzle   = "333";
    String exportImportCategory = "Normal";
    String importTag;
    File   importFile;

    private Drawer          mDrawer;
    private MaterialDialog  progressDialog;
    private DatabaseHandler handler;

    private boolean goBack = false;
    private boolean openExportImportDialog;

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getStringExtra("action")) {
                case "GO BACK":
                    goBack = true;
                    onBackPressed();
                    break;
            }
        }
    };

    public void openDrawer() {
        mDrawer.openDrawer();
    }

    public void closeDrawer() {
        mDrawer.closeDrawer();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeUtils.getCurrentTheme(getBaseContext()));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        AppRater.app_launched(this);

        handler = new DatabaseHandler(this);

        bp = new BillingProcessor(this, null, this);

        fragmentManager = getSupportFragmentManager();

        if (savedInstanceState == null) {
            fragmentManager
                .beginTransaction()
                .replace(R.id.main_activity_container, TimerFragmentMain.newInstance(), "fragment_main")
                .commit();
            try {
                openExportImportDialog = savedInstanceState.getBoolean(OPEN_EXPORT_IMPORT_DIALOG);
            } catch (Exception e) {
                e.printStackTrace();
                openExportImportDialog = false;
            }
        }

        handleDrawer(savedInstanceState);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter("ACTIVITY"));

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (openExportImportDialog) {
            ExportImportDialog exportImportDialog = ExportImportDialog.newInstance();
            exportImportDialog.setDialogInterface(mainActivity);
            exportImportDialog.show(fragmentManager, "exportImport_dialog");
            openExportImportDialog = false;
        }
    }

    private void handleDrawer(Bundle savedInstanceState) {
        final Activity activity = this;

        ImageView headerView = (ImageView) View.inflate(activity, R.layout.drawer_header, null);

        headerView.setImageDrawable(ThemeUtils.tintDrawable(activity, R.drawable.header, R.attr.colorPrimary));

        // Setup drawer
        mDrawer = new DrawerBuilder()
            .withActivity(activity)
            .withDelayOnDrawerClose(- 1)
            .withHeader(headerView)
            .addDrawerItems(
                new PrimaryDrawerItem().withName(R.string.drawer_title_timer)
                    .withIcon(R.drawable.ic_timer_black_24dp).withIconTintingEnabled(true).withIdentifier(TIMER_ID),

                new ExpandableDrawerItem().withName(R.string.drawer_title_reference).withIcon(R.drawable.ic_cube_unfolded_black_24dp).withSelectable(false).withSubItems(
                    new SecondaryDrawerItem().withName(R.string.drawer_title_oll).withLevel(2)
                        .withIcon(R.drawable.ic_oll_black_24dp).withIconTintingEnabled(true).withIdentifier(OLL_ID),

                    new SecondaryDrawerItem().withName(R.string.drawer_title_pll).withLevel(2)
                        .withIcon(R.drawable.ic_pll_black_24dp).withIconTintingEnabled(true).withIdentifier(PLL_ID)
                ).withIconTintingEnabled(true),

                new SectionDrawerItem().withName(R.string.drawer_title_other),

                new PrimaryDrawerItem().withName(R.string.drawer_title_export_import)
                    .withIcon(R.drawable.ic_folder_open_black_24dp).withIconTintingEnabled(true).withSelectable(false)
                    .withIdentifier(EXPORT_IMPORT_ID),

                new PrimaryDrawerItem().withName(R.string.drawer_title_changeTheme)
                    .withIcon(R.drawable.ic_brush_black_24dp).withIconTintingEnabled(true).withSelectable(false)
                    .withIdentifier(THEME_ID),

                new PrimaryDrawerItem().withName(R.string.drawer_title_changeColorScheme)
                    .withIcon(R.drawable.ic_scheme_black_24dp).withIconTintingEnabled(true).withSelectable(false)
                    .withIdentifier(SCHEME_ID),

                new DividerDrawerItem(),

                new PrimaryDrawerItem().withName(R.string.action_settings)
                    .withIcon(R.drawable.ic_action_settings_black_24).withIconTintingEnabled(true).withSelectable(false)
                    .withIdentifier(SETTINGS_ID),

                new PrimaryDrawerItem().withName(R.string.action_donate)
                    .withIcon(R.drawable.ic_mood_black_24dp).withIconTintingEnabled(true).withSelectable(false)
                    .withIdentifier(DONATE_ID),

                new PrimaryDrawerItem().withName(R.string.drawer_about)
                    .withIcon(R.drawable.ic_action_help_black_24).withIconTintingEnabled(true).withSelectable(false)
                    .withIdentifier(ABOUT_ID)


                //,new PrimaryDrawerItem().withName("DEBUG OPTION - ADD 10000 SOLVES")
                //        .withIcon(R.drawable.ic_action_help_black_24).withIconTintingEnabled(true).withSelectable(false)
                //        .withIdentifier(DEBUG_ID)
                //
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
                                        .replace(R.id.main_activity_container, TimerFragmentMain.newInstance(), "fragment_main")
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
                                        .replace(R.id.main_activity_container, AlgListFragment.newInstance(DatabaseHandler.SUBSET_OLL), "fragment_algs_oll")
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
                                        .replace(R.id.main_activity_container, AlgListFragment.newInstance(DatabaseHandler.SUBSET_PLL), "fragment_algs_pll")
                                        .commit();

                                }
                            });
                            break;

                        case EXPORT_IMPORT_ID:
                            // Here, thisActivity is the current activity
                            if (ContextCompat.checkSelfPermission(activity,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                != PackageManager.PERMISSION_GRANTED) {

                                // Should we show an explanation?
                                if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                                    new MaterialDialog.Builder(activity)
                                        .content(R.string.permission_denied_explanation)
                                        .positiveText(R.string.action_ok)
                                        .negativeText(R.string.action_cancel)
                                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                                            @Override
                                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                ActivityCompat.requestPermissions(activity,
                                                    new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                                                    STORAGE_PERMISSION_CODE);
                                            }
                                        })
                                        .show();

                                } else {

                                    ActivityCompat.requestPermissions(activity,
                                        new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                                        STORAGE_PERMISSION_CODE);
                                }

                            } else {
                                ExportImportDialog exportImportDialog = ExportImportDialog.newInstance();
                                exportImportDialog.setDialogInterface(mainActivity);
                                exportImportDialog.show(fragmentManager, "exportImport_dialog");
                            }
                            break;

                        case THEME_ID:
                            ThemeSelectDialog themeSelectDialog = ThemeSelectDialog.newInstance();
                            themeSelectDialog.show(fragmentManager, "theme_dialog");
                            break;

                        case SCHEME_ID:
                            SchemeSelectDialogMain schemeSelectDialogMain = SchemeSelectDialogMain.newInstance();
                            schemeSelectDialogMain.show(fragmentManager, "scheme_dialog");
                            break;

                        case SETTINGS_ID:
                            final Intent settingsIntent = new Intent(getApplicationContext(), SettingsActivity.class);
                            mDrawerToggle.runWhenIdle(new Runnable() {
                                @Override
                                public void run() {
                                    startActivityForResult(settingsIntent, REQUEST_SETTING);
                                }
                            });
                            break;

                        case DONATE_ID:
                            if (BillingProcessor.isIabServiceAvailable(activity))
                                new MaterialDialog.Builder(activity)
                                    .title(R.string.choose_donation_amount)
                                    .items(R.array.donation_tiers)
                                    .itemsCallback(new MaterialDialog.ListCallback() {
                                        @Override
                                        public void onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                                            switch (which) {
                                                case 0:
                                                    bp.purchase(activity, "donation_tier1");
                                                    break;
                                                case 1:
                                                    bp.purchase(activity, "donation_tier2");
                                                    break;
                                                case 2:
                                                    bp.purchase(activity, "donation_tier3");
                                                    break;
                                                case 3:
                                                    bp.purchase(activity, "donation_tier4");
                                                    break;
                                            }
                                        }
                                    })
                                    .show();

                            else
                                Toast.makeText(activity, "Google Play not available", Toast.LENGTH_LONG).show();

                            break;

                        case ABOUT_ID:
                            final Intent aboutIntent = new Intent(getApplicationContext(), AboutActivity.class);
                            mDrawerToggle.runWhenIdle(new Runnable() {
                                @Override
                                public void run() {
                                    startActivityForResult(aboutIntent, REQUEST_ABOUT);
                                }
                            });
                            break;


                        //case DEBUG_ID:
                        //    Random rand = new Random();
                        //    DatabaseHandler db = new DatabaseHandler(activity);
                        //    for (int i = 0; i < 10000; i++) {
                        //        db.addSolve(new Solve(30000 + rand.nextInt(2000), "333", "Normal", 165165l, "", 0, "", rand.nextBoolean()));
                        //    }
                        //    break;
                    }
                    if (closeDrawer)
                        mDrawerLayout.closeDrawers();
                    return false;
                }
            })
            .withSavedInstance(savedInstanceState)
            .build();

        mDrawerLayout = mDrawer.getDrawerLayout();
        mDrawerToggle = new SmoothActionBarDrawerToggle(this, mDrawerLayout, null, R.string.drawer_open, R.string.drawer_close);
        mDrawerLayout.setDrawerListener(mDrawerToggle);

    }

    @Override
    public void onBillingInitialized() {
        /*
         * Called when BillingProcessor was initialized and it's ready to purchase
         */
    }

    @Override
    public void onProductPurchased(String productId, TransactionDetails details) {
        /*
         * Called when requested PRODUCT ID was successfully purchased
         */
        bp.consumePurchase(productId);
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
        if (! bp.handleActivityResult(requestCode, resultCode, data))
            super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SETTING) {
            finish();
            startActivity(new Intent(this, MainActivity.class));
        }
    }

    @Override
    public void onBackPressed() {
        if (mDrawer.isDrawerOpen()) {
            mDrawer.closeDrawer();
        } else if (goBack) {
            super.onBackPressed();
            goBack = false;
        } else if (fragmentManager.findFragmentByTag("fragment_main") != null) { // If the main fragment is open
            Broadcaster.broadcast(this, "TIMER", "BACK PRESSED"); // This broadcast goes to TimerFragmentMain
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (bp != null)
            bp.release();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        handler.closeDB();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_overview, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState = mDrawer.saveInstanceState(outState);
        //outState.putBoolean(OPEN_EXPORT_IMPORT_DIALOG, openExportImportDialog);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onImportExternal() {
        final ImportSolves importSolves = new ImportSolves(this, importTag, importFile);
        importSolves.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onExportExternal() {
        if (StoreUtils.isExternalStorageWritable()) {
            File fileDir = new File(Environment.getExternalStorageDirectory() + "/TwistyTimer");
            fileDir.mkdir();
            String fileName = "Solves_" + exportImportPuzzle + "_" + exportImportCategory + "_"
                + DateTime.now().toString("dd-MMM-y'_'kk-mm") + ".txt";

            final ExportSolves exportSolves = new ExportSolves(this, fileDir, fileName, false);
            exportSolves.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

    }

    @Override
    public void onExportBackup() {
        if (StoreUtils.isExternalStorageWritable()) {
            File fileDir = new File(Environment.getExternalStorageDirectory() + "/TwistyTimer/Backup");
            fileDir.mkdirs();
            String fileName = "Backup_" + DateTime.now().toString("dd-MMM-y'_'kk-mm") + ".txt";

            final ExportSolves exportSolves = new ExportSolves(this, fileDir, fileName, true);
            exportSolves.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    @Override
    public void onSelectPuzzle(String puzzle) {
        exportImportPuzzle = puzzle;
    }

    @Override
    public void onSelectCategory(String category) {
        exportImportCategory = category;
    }

    @Override
    public void onFileSelection(@NonNull FileChooserDialog dialog, @NonNull File file) {

        importFile = file;
        importTag = dialog.getTag();

        if (file.getName().toLowerCase().endsWith(".txt")) {
            if (importTag.equals("import_external")) {
                ExportImportSelectionDialog selectionDialog =
                    ExportImportSelectionDialog.newInstance(ExportImportSelectionDialog.TYPE_IMPORT);
                selectionDialog.setDialogInterface(this);
                selectionDialog.show(fragmentManager, "export_import_selection");
            } else {
                final ImportSolves importSolves = new ImportSolves(this, importTag, importFile);
                importSolves.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        } else {
            // TODO: ADD HELP
            new MaterialDialog.Builder(this)
                .title(R.string.file_selection_error_title)
                .content(R.string.file_selection_error_content, ".txt")
                .positiveText(R.string.action_ok)
                .show();
        }
    }

    private class ExportSolves extends AsyncTask<Void, Integer, Boolean> {

        private final Context mContext;
        private       File    fileDir;
        private       String  outFileName;
        private       boolean isBackup;

        public ExportSolves(Context context, File fileDir, String outFileName, boolean isBackup) {
            this.mContext = context;
            this.fileDir = fileDir;
            this.outFileName = outFileName;
            this.isBackup = isBackup;
        }

        @Override
        protected void onPreExecute() {
            progressDialog = new MaterialDialog.Builder(mContext)
                .content(R.string.export_progress_title)
                .progress(false, 0, true)
                .cancelable(false)
                .show();
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if (progressDialog.isShowing()) {
                if (progressDialog.getMaxProgress() == 0)
                    progressDialog.setMaxProgress(values[1]);

                progressDialog.setProgress(values[0]);
            }
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            File outFile = new File(fileDir, outFileName);
            Boolean returnCode;
            int exports = 0;

            try {
                final Writer out = new BufferedWriter(new FileWriter(outFile));

                if (isBackup) {
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
                } else {
                    Cursor cursor = handler.getAllSolvesFrom(exportImportPuzzle, exportImportCategory);

                    try {
                        publishProgress(0, cursor.getCount());

                        while (cursor.moveToNext()) {
                            String csvValues
                                    = '"' + PuzzleUtils.convertTimeToString(cursor.getInt(IDX_TIME))
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
                }
            } catch (IOException e) {
                returnCode = false;
                Log.d("ERROR", "IOException: " + e.getMessage());
            }

            return returnCode;
        }

        @Override
        protected void onPostExecute(Boolean isExported) {
            super.onPostExecute(isExported);
            if (progressDialog.isShowing()) {
                progressDialog.setActionButton(DialogAction.POSITIVE, R.string.action_done);

                if (isExported)
                    progressDialog.setContent(Html.fromHtml(getString(R.string.export_progress_complete)
                        + "<br><br>" + "<small><tt>" + fileDir.getAbsolutePath() + "/" + outFileName + "</tt></small>"));
                else
                    progressDialog.setContent(R.string.export_progress_error);
            }
        }
    }


    private class ImportSolves extends AsyncTask<Void, Integer, Void> {

        int parseErrors = 0;
        int duplicates  = 0;
        int successes   = 0;

        private Context mContext;
        private String  tag;
        private File    file;

        public ImportSolves(Context context, String tag, File file) {
            this.mContext = context;
            this.tag = tag;
            this.file = file;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new MaterialDialog.Builder(mContext)
                .content(R.string.import_progress_title)
                .progress(false, 0, true)
                .cancelable(false)
                .show();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if (progressDialog.isShowing()) {
                if (progressDialog.getMaxProgress() == 0)
                    progressDialog.setMaxProgress(values[1]);

                progressDialog.setProgress(values[0]);
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            List<Solve> solveList = new ArrayList<>();

            try {

                BufferedReader br = new BufferedReader(new FileReader(file));
                CSVReader csvReader = new CSVReader(br, ';');
                String[] line;

                if (tag.equals("import_backup")) {
                    // throw away the header
                    csvReader.readNext();

                    while ((line = csvReader.readNext()) != null) {
                        try {
                            solveList.add(new Solve(
                                Integer.parseInt(line[2]), line[0], line[1], Long.parseLong(line[3]),
                                line[4], Integer.parseInt(line[5]), line[6], true));
                        } catch (Exception e) {
                            parseErrors++;
                        }
                    }
                }

                if (tag.equals("import_external")) {
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
                                        time, exportImportPuzzle, exportImportCategory,
                                        date, scramble, penalty, "", true));
                            } catch (Exception e) {
                                parseErrors++;
                            }
                        } else {
                            parseErrors++;
                        }
                    }
                }

                // Perform a bulk insertion of the solves.
                successes = handler.addSolves(solveList, new ProgressListener() {
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
            super.onPostExecute(aVoid);
            if (progressDialog.isShowing()) {
                progressDialog.setActionButton(DialogAction.POSITIVE, R.string.action_done);
                progressDialog.setContent(Html.fromHtml(getString(R.string.import_progress_content)
                    + "<br><br><small><tt>"
                    + "<b>" + successes + "</b> " + getString(R.string.import_progress_content_successful_imports)
                    + "<br><b>" + duplicates + "</b> " + getString(R.string.import_progress_content_ignored_duplicates)
                    + "<br><b>" + parseErrors + "</b> " + getString(R.string.import_progress_content_errors)
                    + "</small></tt>"));
            }
            Broadcaster.broadcast(mainActivity, "TIMELIST", "REFRESH TIME");
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
