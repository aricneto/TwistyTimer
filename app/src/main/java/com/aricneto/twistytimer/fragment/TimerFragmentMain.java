package com.aricneto.twistytimer.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aricneto.twistytimer.fragment.dialog.CategorySelectDialog;
import com.aricneto.twistytimer.fragment.dialog.BottomSheetTrainerDialog;
import com.aricneto.twistytimer.fragment.dialog.PuzzleSelectDialog;
import com.aricneto.twistytimer.listener.DialogListenerMessage;
import com.aricneto.twistytimer.puzzle.TrainerScrambler;
import com.aricneto.twistytimer.utils.Prefs;
import com.aricneto.twistytimer.utils.ThemeUtils;
import com.google.android.material.tabs.TabLayout;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.cardview.widget.CardView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.viewpager.widget.ViewPager;

import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.activity.MainActivity;
import com.aricneto.twistytimer.layout.LockedViewPager;
import com.aricneto.twistytimer.listener.OnBackPressedInFragmentListener;
import com.aricneto.twistytimer.stats.Statistics;
import com.aricneto.twistytimer.stats.StatisticsCache;
import com.aricneto.twistytimer.stats.StatisticsLoader;
import com.aricneto.twistytimer.utils.PuzzleUtils;
import com.aricneto.twistytimer.utils.Wrapper;
import com.github.ksoichiro.android.observablescrollview.CacheFragmentStatePagerAdapter;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static com.aricneto.twistytimer.fragment.TimerFragment.TIMER_MODE_TIMER;
import static com.aricneto.twistytimer.fragment.TimerFragment.TIMER_MODE_TRAINER;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_CHANGED_CATEGORY;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_CHANGED_THEME;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_DELETE_SELECTED_TIMES;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_GENERATE_SCRAMBLE;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_HISTORY_TIMES_SHOWN;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_SCROLLED_PAGE;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_SELECTION_MODE_OFF;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_SELECTION_MODE_ON;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_SESSION_TIMES_SHOWN;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_TIMER_STARTED;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_TIMER_STOPPED;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_TIME_SELECTED;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_TIME_UNSELECTED;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_TOOLBAR_RESTORED;
import static com.aricneto.twistytimer.utils.TTIntent.CATEGORY_TIME_DATA_CHANGES;
import static com.aricneto.twistytimer.utils.TTIntent.CATEGORY_UI_INTERACTIONS;
import static com.aricneto.twistytimer.utils.TTIntent.TTFragmentBroadcastReceiver;
import static com.aricneto.twistytimer.utils.TTIntent.broadcast;
import static com.aricneto.twistytimer.utils.TTIntent.registerReceiver;
import static com.aricneto.twistytimer.utils.TTIntent.unregisterReceiver;

public class TimerFragmentMain extends BaseFragment implements OnBackPressedInFragmentListener, DialogListenerMessage {
    /**
     * Flag to enable debug logging for this class.
     */
    private static final boolean DEBUG_ME = true;

    /**
     * A "tag" to identify this class in log messages.
     */
    private static final String TAG = TimerFragmentMain.class.getSimpleName();

    /**
     * The zero-based position of the timer fragment/tab/page.
     */
    public static final int TIMER_PAGE = 0;

    /**
     * The zero-based position of the timer list fragment/tab/page.
     */
    public static final int LIST_PAGE = 1;

    /**
     * The zero-based position of the timer graph fragment/tab/page.
     */
    public static final int GRAPH_PAGE = 2;

    /**
     * The total number of pages.
     */
    private static final int NUM_PAGES = 3;

    private static final String PUZZLE         = "puzzle";
    private static final String PUZZLE_SUBTYPE = "puzzle_type";
    private static final String TIMER_MODE     = "timer_mode";
    private static final String TRAINER_SUBSET    = "trainer_subset";

    private static final String TAG_CATEGORY_DIALOG = "select_category_dialog";
    private static final String TAG_PUZZLE_DIALOG   = "puzzle_spinner_dialog_fragment";

    private Unbinder mUnbinder;

    @BindView(R.id.root)         RelativeLayout  rootLayout;
    @BindView(R.id.toolbar)      CardView        mToolbar;
    @BindView(R.id.pager)        LockedViewPager viewPager;
    @BindView(R.id.main_tabs)    TabLayout       tabLayout;
    @BindView(R.id.tab_view)    View            tabView;
    @BindView(R.id.puzzleSpinner)View            puzzleSpinnerLayout;

    @BindView(R.id.nav_button_settings) View      navButtonSettings;
    @BindView(R.id.nav_button_category) View      navButtonCategory;
    @BindView(R.id.nav_button_history) ImageView navButtonHistory;

    @BindView(R.id.puzzleCategory) TextView puzzleCategoryText;
    @BindView(R.id.puzzleName) TextView puzzleNameText;

    ActionMode actionMode;

    private LinearLayout      tabStrip;
    private NavigationAdapter viewPagerAdapter;

    // Stores the current puzzle being timed/shown
    private String                         currentPuzzle;
    private String                         currentPuzzleCategory;
    private TrainerScrambler.TrainerSubset currentPuzzleSubset;
    private String                         currentTimerMode;

    // Stores the current state of the list switch
    boolean history = false;

    int currentPage = TIMER_PAGE;
    private boolean pagerEnabled;

    private int selectCount = 0;

    private CategorySelectDialog categoryDialog;

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.nav_button_category:
                    categoryDialog = CategorySelectDialog.newInstance(currentPuzzle, currentPuzzleCategory, currentTimerMode, currentPuzzleSubset);
                    categoryDialog.setDialogListener(categoryDialogListener);
                    categoryDialog.show(mFragmentManager, TAG_CATEGORY_DIALOG);
                    break;
                case R.id.nav_button_history:
                    history = !history;

                    updateHistorySwitchItem();

                    broadcast(CATEGORY_TIME_DATA_CHANGES,
                              history ? ACTION_HISTORY_TIMES_SHOWN : ACTION_SESSION_TIMES_SHOWN);
                    break;
                case R.id.nav_button_settings:
                    getMainActivity().openDrawer();
                    break;
            }
        }
    };

    private DialogListenerMessage categoryDialogListener = new DialogListenerMessage() {
        @Override
        public void onUpdateDialog(String text) {
            currentPuzzleCategory = text;
            broadcast(CATEGORY_UI_INTERACTIONS, ACTION_CHANGED_CATEGORY);
        }
    };

    private ActionMode.Callback actionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_list_callback, menu);

            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                //getActivity().getWindow().setStatusBarColor(ThemeUtils.fetchAttrColor(mContext, R.attr.colorPrimaryDark));
            //}
            return true; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.delete:
                    // Receiver will delete times and then broadcast "ACTION_TIMES_MODIFIED".
                    broadcast(CATEGORY_UI_INTERACTIONS, ACTION_DELETE_SELECTED_TIMES);
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
        }
    };

    // Receives broadcasts about changes to the time user interface.
    private TTFragmentBroadcastReceiver mUIInteractionReceiver
            = new TTFragmentBroadcastReceiver(this, CATEGORY_UI_INTERACTIONS) {
        @Override
        public void onReceiveWhileAdded(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_CHANGED_THEME:
                    try {
                        // If the theme has been changed, then the activity will need to be recreated. The
                        // theme can only be applied properly during the inflation of the layouts, so it has
                        // to go back to "Activity.updateLocale()" to do that.
                        ((MainActivity) getActivity()).onRecreateRequired();
                    } catch (Exception e) {}
                    break;
                case ACTION_TIMER_STARTED:
                    getMainActivity().setDrawerLock(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                    viewPager.setPagingEnabled(false);
                    activateTabLayout(false);
                    mToolbar.animate()
                            .translationY(-mToolbar.getHeight())
                            .alpha(0)
                            .setDuration(mAnimationDuration);

                    tabView.animate()
                             .translationY(tabView.getHeight())
                             .alpha(0)
                             .setDuration(mAnimationDuration);
                    break;

                case ACTION_TIMER_STOPPED:
                    getMainActivity().setDrawerLock(DrawerLayout.LOCK_MODE_UNDEFINED);
                    mToolbar.animate()
                            .translationY(0)
                            .alpha(1)
                            .setDuration(mAnimationDuration);

                    tabView.animate()
                            .translationY(0)
                            .alpha(1)
                            .setDuration(mAnimationDuration)
                            .withEndAction(() -> broadcast(CATEGORY_UI_INTERACTIONS, ACTION_TOOLBAR_RESTORED));

                    activateTabLayout(true);
                    if (pagerEnabled)
                        viewPager.setPagingEnabled(true);
                    else
                        viewPager.setPagingEnabled(false);
                    break;

                case ACTION_SELECTION_MODE_ON:
                    selectCount = 0;
                    actionMode = mToolbar.startActionMode(actionModeCallback);

                    break;

                case ACTION_SELECTION_MODE_OFF:
                    selectCount = 0;
                    if (actionMode != null)
                        actionMode.finish();
                    break;

                case ACTION_TIME_SELECTED:
                    selectCount += 1;
                    actionMode.setTitle(getResources().getQuantityString(R.plurals.selected_list, selectCount, selectCount));
                    break;

                case ACTION_TIME_UNSELECTED:
                    selectCount -= 1;
                    actionMode.setTitle(getResources().getQuantityString(R.plurals.selected_list, selectCount, selectCount));
                    break;

                case ACTION_CHANGED_CATEGORY:
                    viewPager.setAdapter(viewPagerAdapter);
                    viewPager.setCurrentItem(currentPage);
                    updatePuzzleSpinnerHeader();
                    handleStatisticsLoader();

                    if (currentTimerMode.equals(TIMER_MODE_TRAINER))
                        broadcast(CATEGORY_UI_INTERACTIONS, ACTION_GENERATE_SCRAMBLE);
                    break;
            }
        }
    };

    private Context mContext;
    private FragmentManager mFragmentManager;

    private int mAnimationDuration;

    @SuppressLint("StringFormatInvalid")
    private void updatePuzzleSpinnerHeader() {
        history = false;
        updateHistorySwitchItem();
        puzzleCategoryText.setText(currentPuzzleCategory.toLowerCase());
        if (currentTimerMode.equals(TIMER_MODE_TRAINER))
            puzzleNameText.setText(getString(R.string.title_trainer, currentPuzzleSubset.name()));
        else
            puzzleNameText.setText(PuzzleUtils.getPuzzleNameFromType(currentPuzzle));
    }

    public TimerFragmentMain() {
        // Required empty public constructor
    }

    public static TimerFragmentMain newInstance(String puzzle, String category, String mode, TrainerScrambler.TrainerSubset subset) {
        final TimerFragmentMain fragment = new TimerFragmentMain();
        Bundle args = new Bundle();
        args.putString(PUZZLE, puzzle);
        args.putString(PUZZLE_SUBTYPE, category);
        args.putString(TIMER_MODE, mode);
        args.putSerializable(TRAINER_SUBSET, subset);
        fragment.setArguments(args);
        if (DEBUG_ME) Log.d(TAG, "newInstance() -> " + fragment);
        return fragment;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (DEBUG_ME) Log.d(TAG, "onSaveInstanceState()");
        super.onSaveInstanceState(outState);
        outState.putString("puzzle", currentPuzzle);
        outState.putString("subtype", currentPuzzleCategory);
        outState.putSerializable("subset", currentPuzzleSubset);
        outState.putString("mode", currentTimerMode);
        outState.putBoolean("history", history);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (DEBUG_ME) Log.d(TAG, "updateLocale(savedInstanceState=" + savedInstanceState + ")");
        super.onCreate(savedInstanceState);

        mContext = getContext();
        mFragmentManager = getFragmentManager();

        // Retrieve arguments
        if (getArguments() != null) {
            currentPuzzle = getArguments().getString(PUZZLE);
            currentPuzzleCategory = getArguments().getString(PUZZLE_SUBTYPE);
            currentTimerMode = getArguments().getString(TIMER_MODE);
            currentPuzzleSubset = (TrainerScrambler.TrainerSubset) getArguments().getSerializable(TRAINER_SUBSET);
        }

        // Retrieve instance state
        if (savedInstanceState != null) {
            currentPuzzle = savedInstanceState.getString("puzzle");
            currentPuzzleCategory = savedInstanceState.getString("subtype");
            currentTimerMode = savedInstanceState.getString("mode", TimerFragment.TIMER_MODE_TIMER);
            currentPuzzleSubset = (TrainerScrambler.TrainerSubset) savedInstanceState.getSerializable("subset");
            history = savedInstanceState.getBoolean("history");

            // Set the dialog listeners again, in case the dialogs are open.
            categoryDialog = (CategorySelectDialog) mFragmentManager
                    .findFragmentByTag(TAG_CATEGORY_DIALOG);
            if (categoryDialog != null)
                categoryDialog.setDialogListener(categoryDialogListener);

            PuzzleSelectDialog selectDialog = (PuzzleSelectDialog) mFragmentManager
                    .findFragmentByTag(TAG_PUZZLE_DIALOG);
            if (selectDialog != null)
                selectDialog.setDialogListener(this);
        }

        mAnimationDuration = Prefs.getInt(R.string.pk_timer_animation_duration, mContext.getResources().getInteger(R.integer.defaultAnimationDuration));
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (DEBUG_ME) Log.d(TAG, "onCreateView(savedInstanceState=" + savedInstanceState + ")");
        View root = inflater.inflate(R.layout.fragment_timer_main, container, false);
        mUnbinder = ButterKnife.bind(this, root);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // setup background gradient
        rootLayout.setBackground(ThemeUtils.fetchBackgroundGradient(mContext, ThemeUtils.getPreferredTheme()));

        navButtonCategory.setOnClickListener(clickListener);
        navButtonHistory.setOnClickListener(clickListener);
        navButtonSettings.setOnClickListener(clickListener);

        if (savedInstanceState == null) {
            // Remember last used puzzle
            currentPuzzle = Prefs.getString(R.string.pk_last_used_puzzle, PuzzleUtils.TYPE_333);
            updateCurrentCategory();
        }

        pagerEnabled = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(
                getString(R.string.pk_tab_swiping_enabled), true);

        viewPager.setPagingEnabled(pagerEnabled);

        // Menu bar background
        if (Prefs.getBoolean(R.string.pk_menu_background, false)) {
            mToolbar.setCardBackgroundColor(Color.TRANSPARENT);
            mToolbar.setCardElevation(0);
        }

        viewPagerAdapter = new NavigationAdapter(getChildFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setOffscreenPageLimit(NUM_PAGES - 1);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setSelectedTabIndicator(0);
        tabLayout.setTabIconTint(AppCompatResources.getColorStateList(mContext, R.color.tab_color));
        tabStrip = ((LinearLayout) tabLayout.getChildAt(0));

        tabLayout.getBackground().setColorFilter(ThemeUtils.fetchAttrColor(mContext, R.attr.colorTabBar), PorterDuff.Mode.SRC_IN);

        // Handle spinner AFTER reading from savedInstanceState, so we can correctly
        // fill the category field in the spinner
        handleHeaderSpinner();

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                setupPage(position);
                currentPage = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                broadcast(CATEGORY_UI_INTERACTIONS, ACTION_SCROLLED_PAGE);
            }
        });

        // Register a receiver to update if something has changed
        registerReceiver(mUIInteractionReceiver);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        handleStatisticsLoader();

    }

    private void handleStatisticsLoader() {
        // The "StatisticsLoader" is managed from this fragment, as it has the necessary access to
        // the puzzle type, subtype and history values.
        //
        // "restartLoader" ensures that any old loader with the wrong puzzle type/subtype will not
        // be reused. For now, those arguments are just passed via their respective fields to
        // "onCreateLoader".

        if (DEBUG_ME)
            Log.d(TAG, "Puzzle and subtype: " + currentPuzzle + " // " + currentPuzzleCategory);
        if (DEBUG_ME) Log.d(TAG, "onActivityCreated -> restartLoader: STATISTICS_LOADER_ID");
        getLoaderManager().restartLoader(MainActivity.STATISTICS_LOADER_ID, null,
                                         new LoaderManager.LoaderCallbacks<Wrapper<Statistics>>() {
                                             @Override
                                             public Loader<Wrapper<Statistics>> onCreateLoader(int id, Bundle args) {
                                                 if (DEBUG_ME)
                                                     Log.d(TAG, "onCreateLoader: STATISTICS_LOADER_ID");
                                                 return new StatisticsLoader(mContext, Statistics.newAllTimeStatistics(),
                                                                             currentPuzzle, currentPuzzleCategory);
                                             }

                                             @Override
                                             public void onLoadFinished(Loader<Wrapper<Statistics>> loader,
                                                                        Wrapper<Statistics> data) {
                                                 if (DEBUG_ME)
                                                     Log.d(TAG, "onLoadFinished: STATISTICS_LOADER_ID");
                                                 // Other fragments can get the statistics from the cache when they are
                                                 // created and can register themselves as observers of further updates.
                                                 StatisticsCache.getInstance().updateAndNotify(data.content());
                                             }

                                             @Override
                                             public void onLoaderReset(Loader<Wrapper<Statistics>> loader) {
                                                 if (DEBUG_ME)
                                                     Log.d(TAG, "onLoaderReset: STATISTICS_LOADER_ID");
                                                 // Clear the cache and notify all observers that the statistics are "null".
                                                 StatisticsCache.getInstance().updateAndNotify(null);
                                             }
                                         });
    }

    private void activateTabLayout(boolean b) {
        tabStrip.setEnabled(b);
        for (int i = 0; i < tabStrip.getChildCount(); i++) {
            tabStrip.getChildAt(i).setClickable(b);
        }
    }

    @Override
    public void onResume() {
        if (DEBUG_ME) Log.d(TAG, "onResume() : currentPage=" + currentPage);
        // Sets up the toolbar with the icons appropriate to the current page.
        mToolbar.post(() -> setupPage(currentPage));
        super.onResume();
    }

    /**
     * Passes on the "Back" button press event to subordinate fragments and indicates if any
     * fragment consumed the event.
     *
     * @return {@code true} if the "Back" button press was consumed and no further action should be
     * taken; or {@code false} if the "Back" button press was ignored and the caller should
     * propagate it to the next interested party.
     */
    @Override
    public boolean onBackPressedInFragment() {
        if (DEBUG_ME) Log.d(TAG, "onBackPressedInFragment()");

        return viewPagerAdapter != null && viewPagerAdapter.dispatchOnBackPressedInFragment();
    }

    @Override
    public void onDetach() {
        if (DEBUG_ME) Log.d(TAG, "onDetach()");
        super.onDetach();
        unregisterReceiver(mUIInteractionReceiver);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }

    private void updateHistorySwitchItem() {
        if (history) {
            navButtonHistory.setImageResource(R.drawable.ic_history_on);
            navButtonHistory.animate()
                    .rotation(-135)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .setDuration(300)
                    .start();
        } else {
            navButtonHistory.setImageResource(R.drawable.ic_history_off);
            navButtonHistory.animate()
                    .rotation(0)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .setDuration(300)
                    .start();
        }
    }

    /**
     * Sets the page's toolbar buttons and other things
     *
     * @param pageNum
     */
    private void setupPage(int pageNum) {
        if (DEBUG_ME) Log.d(TAG, "setupPage(pageNum=" + pageNum + ")");

        if (actionMode != null)
            actionMode.finish();

        if (mToolbar == null) {
            return;
        }

        switch (pageNum) {
            case TIMER_PAGE:
                if (navButtonHistory != null) {
                    navButtonHistory.animate()
                            .withStartAction(() -> navButtonHistory.setEnabled(false))
                            .alpha(0)
                            .setDuration(200)
                            .withEndAction(() -> {
                                // navButtonHistory may already be destroyed by the time we get
                                // to the end action, so we have to check if it still exists
                                if (navButtonHistory != null) navButtonHistory.setVisibility(View.GONE);
                            })
                            .start();
                }
                break;

            case LIST_PAGE:
            case GRAPH_PAGE:
                if (navButtonHistory != null) {
                    navButtonHistory.setVisibility(View.VISIBLE);
                    navButtonHistory.animate()
                            .withStartAction(() -> navButtonHistory.setEnabled(true))
                            .alpha(1)
                            .setDuration(200)
                            .start();
                }
                break;
        }
    }

    /**
     * The app saves the last subtype used for each puzzle. This function is called to both update
     * the last subtype when it's changed, and to set the subtipe.
     */
    private void updateCurrentCategory() {
        SharedPreferences        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(TwistyTimer.getAppContext());
        SharedPreferences.Editor editor            = sharedPreferences.edit();
        final List<String> subtypeList
                = TwistyTimer.getDBHandler().getAllSubtypesFromType(currentPuzzle);
        if (subtypeList.size() == 0) {
            currentPuzzleCategory = "Normal";
            editor.putString(getString(R.string.pk_last_used_category) + currentPuzzle, "Normal");
            editor.apply();
        } else {
            currentPuzzleCategory = sharedPreferences.getString(getString(R.string.pk_last_used_category) + currentPuzzle, "Normal");
        }
    }

    private void handleHeaderSpinner() {


        // Setup action bar click listener
        puzzleSpinnerLayout.setOnClickListener(v -> {
            if (currentTimerMode.equals(TimerFragment.TIMER_MODE_TRAINER)) {
                BottomSheetTrainerDialog bottomSheetTrainerDialog = BottomSheetTrainerDialog.newInstance(currentPuzzleSubset, currentPuzzleCategory);
                bottomSheetTrainerDialog.show(mFragmentManager, "trainer_dialog_fragment");
            }
            else {
                // Setup spinner dialog and adapter
                PuzzleSelectDialog puzzleSelectDialog = PuzzleSelectDialog.newInstance();
                puzzleSelectDialog.setDialogListener(this);
                puzzleSelectDialog.show(mFragmentManager, TAG_PUZZLE_DIALOG);
            }
        });

        updatePuzzleSpinnerHeader();

    }

    // A new puzzle has been selected
    @Override
    public void onUpdateDialog(String text) {
        currentPuzzle = text;
        Prefs.edit().putString(R.string.pk_last_used_puzzle, currentPuzzle).apply();
        updateCurrentCategory();
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setCurrentItem(currentPage);

        PuzzleSelectDialog selectDialog = (PuzzleSelectDialog) mFragmentManager.findFragmentByTag(TAG_PUZZLE_DIALOG);
        if (selectDialog != null)
            selectDialog.dismiss();

        //// update titles
        updatePuzzleSpinnerHeader();
        handleStatisticsLoader();
    }

    protected class NavigationAdapter extends CacheFragmentStatePagerAdapter {

        public NavigationAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        protected Fragment createItem(int position) {
            if (DEBUG_ME) Log.d(TAG, "NavigationAdapter.createItem(" + position + ")");

            switch (position) {
                case TIMER_PAGE:
                    return TimerFragment.newInstance(
                            currentPuzzle, currentPuzzleCategory, currentTimerMode, currentPuzzleSubset);
                case LIST_PAGE:
                    return TimerListFragment.newInstance(
                            currentPuzzle, currentPuzzleCategory, history);
                case GRAPH_PAGE:
                    return TimerGraphFragment.newInstance(
                            currentPuzzle, currentPuzzleCategory, history);
            }
            return TimerFragment.newInstance(PuzzleUtils.TYPE_333, "Normal", TIMER_MODE_TIMER, TrainerScrambler.TrainerSubset.OLL);
        }

        /**
         * Notifies each fragment (that is listening) that the "Back" button has been pressed.
         * Stops when the first fragment consumes the event.
         *
         * @return {@code true} if any fragment consumed the "Back" button press event; or {@code false}
         * if the event was not consumed by any fragment.
         */
        public boolean dispatchOnBackPressedInFragment() {
            if (DEBUG_ME) Log.d(TAG, "NavigationAdapter.dispatchOnBackPressedInFragment()");
            boolean isConsumed = false;

            for (int p = 0; p < NUM_PAGES && !isConsumed; p++) {
                final Fragment fragment = getItemAt(p);

                if (fragment instanceof OnBackPressedInFragmentListener) { // => not null
                    isConsumed = ((OnBackPressedInFragmentListener) fragment)
                            .onBackPressedInFragment();
                }
            }

            return isConsumed;
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }
}
