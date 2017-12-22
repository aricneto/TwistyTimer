package com.aricneto.twistytimer.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Pair;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.aricneto.twistify.R;
import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.activity.MainActivity;
import com.aricneto.twistytimer.adapter.SpinnerAdapter;
import com.aricneto.twistytimer.database.DatabaseHandler;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.layout.LockedViewPager;
import com.aricneto.twistytimer.listener.OnBackPressedInFragmentListener;
import com.aricneto.twistytimer.stats.Statistics;
import com.aricneto.twistytimer.stats.StatisticsCache;
import com.aricneto.twistytimer.stats.StatisticsLoader;
import com.aricneto.twistytimer.utils.PuzzleUtils;
import com.aricneto.twistytimer.utils.ThemeUtils;
import com.aricneto.twistytimer.utils.Wrapper;
import com.github.ksoichiro.android.observablescrollview.CacheFragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

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
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_USE_SCRAMBLE;
import static com.aricneto.twistytimer.utils.TTIntent.CATEGORY_TIME_DATA_CHANGES;
import static com.aricneto.twistytimer.utils.TTIntent.CATEGORY_UI_INTERACTIONS;
import static com.aricneto.twistytimer.utils.TTIntent.TTFragmentBroadcastReceiver;
import static com.aricneto.twistytimer.utils.TTIntent.broadcast;
import static com.aricneto.twistytimer.utils.TTIntent.registerReceiver;
import static com.aricneto.twistytimer.utils.TTIntent.unregisterReceiver;

public class TimerFragmentMain extends BaseFragment implements OnBackPressedInFragmentListener {
    /**
     * Flag to enable debug logging for this class.
     */
    private static final boolean DEBUG_ME = false;

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

    private static final String KEY_SAVEDSUBTYPE = "savedSubtype";

    private Unbinder mUnbinder;

    @BindView(R.id.toolbar)       Toolbar         mToolbar;
    @BindView(R.id.pager)         LockedViewPager viewPager;
    @BindView(R.id.main_tabs)     TabLayout       tabLayout;
    @BindView(R.id.toolbarLayout) LinearLayout    toolbarLayout;
    ActionMode actionMode;

    private LinearLayout      tabStrip;
    private NavigationAdapter viewPagerAdapter;

    private MaterialDialog removeSubtypeDialog;
    private MaterialDialog subtypeDialog;
    private MaterialDialog createSubtypeDialog;
    private MaterialDialog renameSubtypeDialog;

    // Stores the current puzzle being timed/shown
    private String currentPuzzle        = PuzzleUtils.TYPE_333;
    private String currentPuzzleSubtype = "Normal";
    // Stores the current state of the list switch
    boolean history = false;

    int currentPage = TIMER_PAGE;
    private boolean pagerEnabled;

    private int originalContentHeight;
    private int selectCount = 0;

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getActivity().getWindow().setStatusBarColor(ThemeUtils.fetchAttrColor(getContext(), R.attr.colorPrimaryDark));
            }
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
                case ACTION_USE_SCRAMBLE:
                    viewPager.setCurrentItem(TIMER_PAGE);
                    break;
                case ACTION_TIMER_STARTED: // This was taken from PlusTimer (thanks :D)
                    viewPager.setPagingEnabled(false);
                    activateTabLayout(false);
                    originalContentHeight = viewPager.getHeight();
                    ObjectAnimator hideToolbar = ObjectAnimator.ofFloat(toolbarLayout, View.TRANSLATION_Y, -toolbarLayout.getHeight());
                    hideToolbar.setDuration(300);
                    hideToolbar.setInterpolator(new AccelerateInterpolator());
                    hideToolbar.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator valueAnimator) {
                            if (viewPager != null) {
                                LinearLayout.LayoutParams params =
                                        (LinearLayout.LayoutParams) viewPager.getLayoutParams();
                                params.height = originalContentHeight - (int) (float) valueAnimator.getAnimatedValue();
                                viewPager.setLayoutParams(params);
                                viewPager.setTranslationY((int) (float) valueAnimator.getAnimatedValue());
                            }
                        }
                    });
                    AnimatorSet toolbarSet = new AnimatorSet();
                    toolbarSet.play(hideToolbar);
                    toolbarSet.start();
                    break;

                case ACTION_TIMER_STOPPED:
                    ObjectAnimator showToolbar = ObjectAnimator.ofFloat(toolbarLayout, View.TRANSLATION_Y, 0);
                    showToolbar.setDuration(300);
                    showToolbar.setInterpolator(new DecelerateInterpolator());
                    showToolbar.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator valueAnimator) {
                            if (viewPager != null) {
                                LinearLayout.LayoutParams params =
                                        (LinearLayout.LayoutParams) viewPager.getLayoutParams();
                                params.height = originalContentHeight - (int) (float) valueAnimator.getAnimatedValue();
                                viewPager.setLayoutParams(params);
                                viewPager.setTranslationY((int) (float) valueAnimator.getAnimatedValue());
                            }
                        }
                    });
                    AnimatorSet animatorSet = new AnimatorSet();
                    animatorSet.play(showToolbar);
                    animatorSet.start();
                    animatorSet.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (toolbarLayout != null) {
                                if (toolbarLayout.getTranslationY() == 0) {
                                    LinearLayout.LayoutParams params =
                                            (LinearLayout.LayoutParams) viewPager.getLayoutParams();
                                    params.height = originalContentHeight;
                                    viewPager.setLayoutParams(params);
                                    broadcast(CATEGORY_UI_INTERACTIONS, ACTION_TOOLBAR_RESTORED);
                                }
                            }
                        }
                    });
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
            }
        }
    };

    public TimerFragmentMain() {
        // Required empty public constructor
    }

    public static TimerFragmentMain newInstance() {
        final TimerFragmentMain fragment = new TimerFragmentMain();
        if (DEBUG_ME) Log.d(TAG, "newInstance() -> " + fragment);
        return fragment;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (DEBUG_ME) Log.d(TAG, "onSaveInstanceState()");
        super.onSaveInstanceState(outState);
        outState.putString("puzzle", currentPuzzle);
        outState.putString("subtype", currentPuzzleSubtype);
        outState.putBoolean("history", history);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (DEBUG_ME) Log.d(TAG, "onCreate(savedInstanceState=" + savedInstanceState + ")");
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            currentPuzzle = savedInstanceState.getString("puzzle");
            currentPuzzleSubtype = savedInstanceState.getString("subtype");
            history = savedInstanceState.getBoolean("history");
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (DEBUG_ME) Log.d(TAG, "onCreateView(savedInstanceState=" + savedInstanceState + ")");
        View root = inflater.inflate(R.layout.fragment_timer_main, container, false);
        mUnbinder = ButterKnife.bind(this, root);

        handleHeaderSpinner();
        setupToolbarForFragment(mToolbar);

        pagerEnabled = PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(
                getString(R.string.pk_tab_swiping_enabled), true);

        if (pagerEnabled)
            viewPager.setPagingEnabled(true);
        else
            viewPager.setPagingEnabled(false);

        viewPagerAdapter = new NavigationAdapter(getChildFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setOffscreenPageLimit(NUM_PAGES - 1);
        tabLayout.setupWithViewPager(viewPager);
        tabStrip = ((LinearLayout) tabLayout.getChildAt(0));

        if (savedInstanceState == null) {
            updateCurrentSubtype();
        }

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                setupPage(position, inflater);
                handleIcons(position);
                currentPage = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                broadcast(CATEGORY_UI_INTERACTIONS, ACTION_SCROLLED_PAGE);
            }
        });

        // Sets up the toolbar with the icons appropriate to the current page.
        mToolbar.post(new Runnable() {
            @Override
            public void run() {
                setupPage(currentPage, inflater);
            }
        });

        // Register a receiver to update if something has changed
        registerReceiver(mUIInteractionReceiver);

        return root;
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
            Log.d(TAG, "Puzzle and subtype: " + currentPuzzle + " // " + currentPuzzleSubtype);
        if (DEBUG_ME) Log.d(TAG, "onActivityCreated -> restartLoader: STATISTICS_LOADER_ID");
        getLoaderManager().restartLoader(MainActivity.STATISTICS_LOADER_ID, null,
                                         new LoaderManager.LoaderCallbacks<Wrapper<Statistics>>() {
                                             @Override
                                             public Loader<Wrapper<Statistics>> onCreateLoader(int id, Bundle args) {
                                                 if (DEBUG_ME)
                                                     Log.d(TAG, "onCreateLoader: STATISTICS_LOADER_ID");
                                                 return new StatisticsLoader(getContext(), Statistics.newAllTimeStatistics(),
                                                                             currentPuzzle, currentPuzzleSubtype);
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

    private void handleIcons(int index) {
        // Icons are set in "TimerTabLayout".
        switch (index) {
            case TIMER_PAGE:
                tabLayout.getTabAt(TIMER_PAGE).getIcon().setAlpha(255);
                tabLayout.getTabAt(LIST_PAGE).getIcon().setAlpha(153); // 70%
                tabLayout.getTabAt(GRAPH_PAGE).getIcon().setAlpha(153);
                break;
            case LIST_PAGE:
                tabLayout.getTabAt(TIMER_PAGE).getIcon().setAlpha(153);
                tabLayout.getTabAt(LIST_PAGE).getIcon().setAlpha(255);
                tabLayout.getTabAt(GRAPH_PAGE).getIcon().setAlpha(153);
                break;
            case GRAPH_PAGE:
                tabLayout.getTabAt(TIMER_PAGE).getIcon().setAlpha(153);
                tabLayout.getTabAt(LIST_PAGE).getIcon().setAlpha(153);
                tabLayout.getTabAt(GRAPH_PAGE).getIcon().setAlpha(255);
                break;
        }
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
        super.onResume();
        handleIcons(currentPage);
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

    private void setupTypeDialogItem() {
        mToolbar.getMenu().add(0, 6, 0, R.string.type).setIcon(R.drawable.ic_tag_outline_white_24dp)
                .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        createDialogs();
                        subtypeDialog.show();
                        return true;
                    }
                })
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }

    private void createDialogs() {
        final DatabaseHandler          dbHandler         = TwistyTimer.getDBHandler();
        SharedPreferences              sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        final SharedPreferences.Editor editor            = sharedPreferences.edit();

        // Create Subtype dialog
        createSubtypeDialog = new MaterialDialog.Builder(getContext())
                .title(R.string.enter_type_name)
                .inputRange(0, 16)
                .input(R.string.enter_type_name, 0, false, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog materialDialog, CharSequence input) {
                        dbHandler.addSolve(new Solve(1, currentPuzzle, input.toString(), 0L, "", PuzzleUtils.PENALTY_HIDETIME, "", true));
                        history = false; // Resets the checked state of the switch
                        currentPuzzleSubtype = input.toString();
                        editor.putString(KEY_SAVEDSUBTYPE + currentPuzzle, currentPuzzleSubtype);
                        editor.apply();
                        viewPager.setAdapter(viewPagerAdapter);
                        Toast.makeText(getContext(), currentPuzzleSubtype, Toast.LENGTH_SHORT).show();
                    }
                })
                .build();

        final List<String> subtypeList = dbHandler.getAllSubtypesFromType(currentPuzzle);
        if (subtypeList.size() == 0) {
            subtypeList.add("Normal");
            dbHandler.addSolve(new Solve(1, currentPuzzle, "Normal", 0L, "", PuzzleUtils.PENALTY_HIDETIME, "", true));
        } else if (subtypeList.size() == 1) {
            currentPuzzleSubtype = subtypeList.get(0);
        }
        // Remove Subtype dialog
        removeSubtypeDialog = new MaterialDialog.Builder(getContext())
                .title(R.string.remove_subtype_title)
                .negativeText(R.string.action_cancel)
                .autoDismiss(true)
                .items(subtypeList.toArray(new CharSequence[subtypeList.size()]))
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog materialDialog, View view, int i, final CharSequence typeName) {
                        new MaterialDialog.Builder(getContext())
                                .title(R.string.remove_subtype_confirmation)
                                .content(getString(R.string.remove_subtype_confirmation_content) +
                                                 " \"" + typeName.toString() + "\"?\n" + getString(R.string.remove_subtype_confirmation_content_continuation))
                                .positiveText(R.string.action_remove)
                                .negativeText(R.string.action_cancel)
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(MaterialDialog dialog, DialogAction which) {
                                        dbHandler.deleteSubtype(currentPuzzle, typeName.toString());
                                        if (subtypeList.size() > 1) {
                                            currentPuzzleSubtype = dbHandler.getAllSubtypesFromType(currentPuzzle).get(0);
                                        } else {
                                            currentPuzzleSubtype = "Normal";
                                        }
                                        editor.putString(KEY_SAVEDSUBTYPE + currentPuzzle, currentPuzzleSubtype);
                                        editor.apply();
                                        viewPager.setAdapter(viewPagerAdapter);
                                        viewPager.setCurrentItem(currentPage);
                                    }
                                })
                                .show();
                    }
                })
                .build();

        //Rename subtype
        renameSubtypeDialog = new MaterialDialog.Builder(getContext())
                .title(R.string.rename_subtype_title)
                .negativeText(R.string.action_cancel)
                .autoDismiss(true)
                .items(subtypeList.toArray(new CharSequence[subtypeList.size()]))
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog materialDialog, View view, int i, final CharSequence typeName) {
                        new MaterialDialog.Builder(getContext())
                                .title(R.string.enter_new_name_dialog)
                                .input("", "", false, new MaterialDialog.InputCallback() {
                                    @Override
                                    public void onInput(MaterialDialog dialog, CharSequence input) {
                                        dbHandler.renameSubtype(currentPuzzle, typeName.toString(), input.toString());
                                        currentPuzzleSubtype = input.toString();
                                        editor.putString(KEY_SAVEDSUBTYPE + currentPuzzle, currentPuzzleSubtype);
                                        editor.apply();
                                        viewPager.setAdapter(viewPagerAdapter);
                                        viewPager.setCurrentItem(currentPage);
                                    }
                                })
                                .inputRange(0, 16)
                                .positiveText(R.string.action_done)
                                .negativeText(R.string.action_cancel)
                                .show();
                    }
                })
                .build();


        // Select subtype dialog
        subtypeDialog = new MaterialDialog.Builder(getContext())
                .title(R.string.select_solve_type)
                .positiveText(R.string.w_new_subtype)
                .negativeText(R.string.action_rename)
                .neutralText(R.string.action_remove)
                .neutralColor(ContextCompat.getColor(getContext(), R.color.black_secondary))
                .negativeColor(ContextCompat.getColor(getContext(), R.color.black_secondary))
                .items(subtypeList.toArray(new CharSequence[subtypeList.size()]))
                .alwaysCallSingleChoiceCallback()
                .itemsCallbackSingleChoice(subtypeList.indexOf(currentPuzzleSubtype), new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                        // A subtype was selected
                        currentPuzzleSubtype = subtypeList.get(which);
                        editor.putString(KEY_SAVEDSUBTYPE + currentPuzzle, currentPuzzleSubtype);
                        editor.apply();
                        history = false; // Resets the checked state of the switch
                        viewPager.setAdapter(viewPagerAdapter);
                        viewPager.setCurrentItem(currentPage);

                        handleStatisticsLoader();
                        subtypeDialog.dismiss();
                        return true;
                    }
                })
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        createSubtypeDialog.show();
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        super.onNegative(dialog);
                        renameSubtypeDialog.show();
                    }

                    @Override
                    public void onNeutral(MaterialDialog dialog) {
                        super.onNeutral(dialog);
                        removeSubtypeDialog.show();
                    }
                })
                .build();
    }

    private void setupHistorySwitchItem(LayoutInflater inflater) {
        final SwitchCompat switchCompat = (SwitchCompat) inflater.inflate(R.layout.toolbar_pin_switch, null);
        mToolbar.getMenu().add(0, 7, 0, "Scope").setActionView(switchCompat).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        final Drawable thumb_positive = ThemeUtils.tintPositiveThumb(getContext(), R.drawable.thumb_history_positive, R.attr.colorPrimaryDark);
        final Drawable thumb_negative = ThemeUtils.tintNegativeThumb(getContext(), R.drawable.thumb_history_negative, R.attr.colorPrimaryDark);
        final Drawable track_positive = ThemeUtils.tintDrawable(getContext(), R.drawable.track_positive, R.attr.colorPrimaryDark);

        if (history) {
            switchCompat.setChecked(true);
            switchCompat.setThumbDrawable(thumb_negative);
            switchCompat.setTrackResource(R.drawable.track_negative);
        } else {
            switchCompat.setChecked(false);
            switchCompat.setThumbDrawable(thumb_positive);
            switchCompat.setTrackDrawable(track_positive);
        }

        switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                history = isChecked;

                if (isChecked) {
                    switchCompat.setThumbDrawable(thumb_negative);
                    switchCompat.setTrackResource(R.drawable.track_negative);
                } else {
                    switchCompat.setThumbDrawable(thumb_positive);
                    switchCompat.setTrackDrawable(track_positive);
                }

                broadcast(CATEGORY_TIME_DATA_CHANGES,
                          isChecked ? ACTION_HISTORY_TIMES_SHOWN : ACTION_SESSION_TIMES_SHOWN);
            }
        });
    }

    /**
     * Sets the page's toolbar buttons and other things
     *
     * @param pageNum
     * @param inflater
     */
    private void setupPage(int pageNum, LayoutInflater inflater) {
        if (DEBUG_ME) Log.d(TAG, "setupPage(pageNum=" + pageNum + ")");

        if (actionMode != null)
            actionMode.finish();

        if (mToolbar == null) {
            return;
        }

        mToolbar.getMenu().clear();

        switch (pageNum) {
            case TIMER_PAGE:
                //((MainActivity) getActivity()).hideFAB();
                // Scramble icon
                mToolbar.getMenu()
                        .add(0, 5, 0, R.string.scramble_action)
                        .setIcon(R.drawable.ic_dice_white_24dp)
                        .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem menuItem) {
                                broadcast(CATEGORY_UI_INTERACTIONS, ACTION_GENERATE_SCRAMBLE);
                                return true;
                            }
                        })
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                break;

            case LIST_PAGE:
            case GRAPH_PAGE:
                // Add menu icons
                setupHistorySwitchItem(inflater);
                break;
        }

        setupTypeDialogItem();
    }

    /**
     * The app saves the last subtype used for each puzzle. This function is called to both update
     * the last subtype when it's changed, and to set the subtipe.
     */
    private void updateCurrentSubtype() {
        SharedPreferences        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(TwistyTimer.getAppContext());
        SharedPreferences.Editor editor            = sharedPreferences.edit();
        final List<String> subtypeList
                = TwistyTimer.getDBHandler().getAllSubtypesFromType(currentPuzzle);
        if (subtypeList.size() == 0) {
            currentPuzzleSubtype = "Normal";
            editor.putString(KEY_SAVEDSUBTYPE + currentPuzzle, "Normal");
            editor.apply();
        } else {
            currentPuzzleSubtype = sharedPreferences.getString(KEY_SAVEDSUBTYPE + currentPuzzle, "Normal");
        }
    }

    private void handleHeaderSpinner() {
        // Setup spinner
        View spinnerContainer = LayoutInflater.from(getActivity()).inflate(R.layout.toolbar_spinner, mToolbar, false);
        ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        mToolbar.addView(spinnerContainer, layoutParams);

        final List<Pair<String, String>> filterList = new ArrayList<>();
        filterList.add(Pair.create(getString(R.string.cube_333), ""));
        filterList.add(Pair.create(getString(R.string.cube_222), ""));
        filterList.add(Pair.create(getString(R.string.cube_444), ""));
        filterList.add(Pair.create(getString(R.string.cube_555), ""));
        filterList.add(Pair.create(getString(R.string.cube_666), ""));
        filterList.add(Pair.create(getString(R.string.cube_777), ""));
        filterList.add(Pair.create(getString(R.string.cube_clock), ""));
        filterList.add(Pair.create(getString(R.string.cube_mega), ""));
        filterList.add(Pair.create(getString(R.string.cube_pyra), ""));
        filterList.add(Pair.create(getString(R.string.cube_skewb), ""));
        filterList.add(Pair.create(getString(R.string.cube_sq1), ""));

        SpinnerAdapter spinnerAdapter = new SpinnerAdapter(getActivity());
        spinnerAdapter.addItems(filterList);

        Spinner spinner = (Spinner) spinnerContainer.findViewById(R.id.toolbar_spinner);
        spinner.setAdapter(spinnerAdapter);
        // Set the selected position before setting the listener. If the selected position is not
        // set, it will be set later during layout and fire the listener. That will cause the three
        // fragments nested in the ViewPager to be destroyed and re-created, together with all of
        // their loaders and background tasks, slowing down the start-up of the application.
        //
        // If "[Abs]Spinner.setSelection(int)" is called, this problem is not solved. Therefore,
        // call "[Abs]Spinner.setSelection(int, boolean)" and pass "false" to disable animation.
        // AFAIK, the former method will post a layout request, which will be handled after this
        // method ("handleHeaderSpinner") returns, but the latter method will perform a layout
        // directly before it returns to this method. It is the layout that triggers the unwanted
        // call to "onItemSelected", so the latter method ensures the layout completes before the
        // listener is added in the next statement. See http://stackoverflow.com/a/17336944.
        //
        // To see all this in action, enable debug logging in the fragments by setting "DEBUG_ME"
        // to true in each and then watch the log to see fragments being created twice when the
        // application starts up if the following "setSelection" call is commented out.
        spinner.setSelection(PuzzleUtils.getPositionOfPuzzle(currentPuzzle), false);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (DEBUG_ME) Log.d(TAG, "onItemSelected(position=" + position + ")");
                currentPuzzle = PuzzleUtils.getPuzzleInPosition(position);
                updateCurrentSubtype();
                viewPager.setAdapter(viewPagerAdapter);
                viewPager.setCurrentItem(currentPage);
                handleStatisticsLoader();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
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
                    return TimerFragment.newInstance(currentPuzzle, currentPuzzleSubtype);
                case LIST_PAGE:
                    return TimerListFragment.newInstance(
                            currentPuzzle, currentPuzzleSubtype, history);
                case GRAPH_PAGE:
                    return TimerGraphFragment.newInstance(
                            currentPuzzle, currentPuzzleSubtype, history);
            }
            return TimerFragment.newInstance(PuzzleUtils.TYPE_333, "Normal");
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
