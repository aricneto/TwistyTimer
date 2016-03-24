package com.aricneto.twistytimer.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
import com.aricneto.twistytimer.interfaces.ExportImportDialogInterface;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.utils.Broadcaster;
import com.aricneto.twistytimer.utils.PuzzleUtils;
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
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;


public class MainActivity extends AppCompatActivity implements BillingProcessor.IBillingHandler,
        FileChooserDialog.FileCallback, ExportImportDialogInterface {

    private static final int DEBUG_ID = 11;
    BillingProcessor bp;

    private static final int TIMER_ID         = 1;
    private static final int THEME_ID         = 2;
    private static final int SCHEME_ID        = 9;
    private static final int OLL_ID           = 6;
    private static final int PLL_ID           = 7;
    private static final int DONATE_ID        = 8;
    private static final int EXPORT_IMPORT_ID = 10;
    private static final int ABOUT_ID         = 4;
    private static final int SETTINGS_ID      = 5;

    private static final int REQUEST_SETTING = 42;
    private static final int REQUEST_ABOUT   = 23;

    SmoothActionBarDrawerToggle mDrawerToggle;
    FragmentManager             fragmentManager;
    DrawerLayout                mDrawerLayout;

    private Drawer mDrawer;

    private MaterialDialog  progressDialog;
    private DatabaseHandler handler;
    final MainActivity mainActivity = this;

    public void openDrawer() {
        mDrawer.openDrawer();
    }

    public void closeDrawer() {
        mDrawer.closeDrawer();
    }


    private boolean goBack = false;
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
        }

        handleDrawer(savedInstanceState);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter("ACTIVITY"));

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
                                ExportImportDialog exportImportDialog = ExportImportDialog.newInstance();
                                exportImportDialog.show(fragmentManager, "exportImport_dialog");
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
        super.onSaveInstanceState(outState);
    }


    String importPuzzle   = "333";
    String importCategory = "Normal";
    String importTag;
    File importFile;

    @Override
    public void onImportExternal() {
        final ImportSolves importSolves = new ImportSolves(this, importTag, importFile);
        importSolves.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onSelectPuzzle(String puzzle) {
        importPuzzle = puzzle;
    }

    @Override
    public void onSelectCategory(String category) {
        importCategory = category;
    }

    @Override
    public void onFileSelection(@NonNull FileChooserDialog dialog, @NonNull File file) {

        importFile = file;
        importTag = dialog.getTag();

        if (file.getName().toLowerCase().endsWith(".txt")) {
            if (importTag.equals("import_external")) {
                ExportImportSelectionDialog selectionDialog = ExportImportSelectionDialog.newInstance();
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
                    .neutralText(R.string.action_help)
                    .positiveText(R.string.action_ok)
                    .show();
        }
    }


    private class ImportSolves extends AsyncTask<Void, Integer, Void> {

        private Context mContext;
        private String  tag;
        private File    file;

        int parseErrors = 0;
        int duplicates  = 0;

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
                if (progressDialog.getCurrentProgress() == progressDialog.getMaxProgress()) {
                    progressDialog.setActionButton(DialogAction.POSITIVE, R.string.action_done);
                    progressDialog.setContent(getString(R.string.import_progress_content)
                            + " " + duplicates + " " + getString(R.string.ignored_duplicates)
                            + " " + getString(R.string.and)
                            + " " + parseErrors + " " + getString(R.string.errors) + ".");
                }

            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            List<Solve> solveList = new ArrayList<>();
            int imports = 0;

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

                    while ((line = csvReader.readNext()) != null) {
                        if (line.length <= 3) {
                            try {
                                Log.d("IMPORTING EXTERNAL", "time: " + line[0]);

                                int time = PuzzleUtils.parseTime(line[0]);
                                String scramble = "";
                                long date = DateTime.now().getMillis();;
                                if (line.length >= 2) {
                                    scramble = line[1];
                                }
                                if (line.length == 3) {
                                    try {
                                        date = DateTime.parse(line[2]).getMillis();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }

                                solveList.add(new Solve(time, importPuzzle, importCategory, date, scramble, PuzzleUtils.NO_PENALTY, "", true));
                            } catch (Exception e) {
                                parseErrors++;
                            }
                        } else {
                            parseErrors++;
                        }
                    }

                }

                publishProgress(imports, solveList.size());

                for (Solve solve : solveList) {
                    if (! handler.solveExists(solve))
                        handler.addSolve(solve);
                    else
                        duplicates++;
                    imports++;
                    publishProgress(imports);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
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
