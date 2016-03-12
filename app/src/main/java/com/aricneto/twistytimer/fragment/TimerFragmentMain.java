package com.aricneto.twistytimer.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
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
import com.aricneto.twistytimer.adapter.SpinnerAdapter;
import com.aricneto.twistytimer.database.DatabaseHandler;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.layout.LockedViewPager;
import com.aricneto.twistytimer.utils.Broadcaster;
import com.aricneto.twistytimer.utils.PuzzleUtils;
import com.aricneto.twistytimer.utils.ThemeUtils;
import com.github.ksoichiro.android.observablescrollview.CacheFragmentStatePagerAdapter;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;


public class TimerFragmentMain extends BaseFragment {
    
    @Bind(R.id.toolbar)       Toolbar         mToolbar;
    @Bind(R.id.pager)         LockedViewPager viewPager;
    @Bind(R.id.main_tabs)     TabLayout       tabLayout;
    @Bind(R.id.toolbarLayout) LinearLayout    toolbarLayout;

    private LinearLayout tabStrip;
    
    private NavigationAdapter viewPagerAdapter;
    private TimerFragmentMain fragReference = this;
    
    private static final String KEY_SAVEDSUBTYPE = "savedSubtype";
    
    private MaterialDialog removeSubtypeDialog;
    private MaterialDialog subtypeDialog;
    private MaterialDialog createSubtypeDialog;
    private MaterialDialog renameSubtypeDialog;
    
    DatabaseHandler dbHandler;

    ActionMode actionMode;
    
    int currentPage = 0;
    
    // Stores the current state of the list switch
    boolean historyChecked = false;
    
    // Stores the current puzzle being timed/shown
    private String currentPuzzle        = "333";
    private String currentPuzzleSubtype = "Normal";
    
    private boolean pagerEnabled;
    
    private int originalContentHeight;
    // Receives broadcasts from the timer
    private BroadcastReceiver mReceiver   = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isAdded()) { // The fragment has to check if it is attached to an activity. Removing this will bug the app
                switch (intent.getStringExtra("action")) {
                    case "TIMER STARTED": // This was taken from PlusTimer (thanks :D)
                        viewPager.setPagingEnabled(false);
                        activateTabLayout(false);
                        originalContentHeight = viewPager.getHeight();
                        ObjectAnimator hideToolbar = ObjectAnimator.ofFloat(toolbarLayout, View.TRANSLATION_Y, - toolbarLayout.getHeight());
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
                    case "TIMER STOPPED":
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
                                if (toolbarLayout!= null) {
                                    if (toolbarLayout.getTranslationY() == 0) {
                                        LinearLayout.LayoutParams params =
                                                (LinearLayout.LayoutParams) viewPager.getLayoutParams();
                                        params.height = originalContentHeight;
                                        viewPager.setLayoutParams(params);
                                        Intent sendIntent = new Intent("TIMELIST");
                                        sendIntent.putExtra("action", "TOOLBAR ENDED");
                                        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(sendIntent);
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
                    
                    case "SELECTIONMODE TRUE":
                        selectCount = 0;
                        actionMode = mToolbar.startActionMode(actionModeCallback);
                        break;
                    case "SELECTIONMODE FALSE":
                        selectCount = 0;
                        if (actionMode != null)
                            actionMode.finish();
                        break;
                    case "LISTITEM SELECTED":
                        selectCount += 1;
                        actionMode.setTitle(selectCount + " " + getString(R.string.selected_list));
                        break;
                    case "LISTITEM UNSELECTED":
                        selectCount -= 1;
                        actionMode.setTitle(selectCount + " " + getString(R.string.selected_list));
                        break;

                    case "BACK PRESSED":
                        if (currentTimerFragmentInstance.isRunning) {
                            currentTimerFragmentInstance.cancelChronometer();
                        } else if (currentTimerFragmentInstance.slidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED ||
                                currentTimerFragmentInstance.slidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED ||
                                currentTimerFragmentInstance.slidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.DRAGGING) {
                            currentTimerFragmentInstance.slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
                        } else {
                            Broadcaster.broadcast(getActivity(), "ACTIVITY", "GO BACK");
                        }
                        break;
                }
            }
        }
    };
    private int               selectCount = 0;
    
    public TimerFragmentMain() {
        // Required empty public constructor
    }
    
    public static TimerFragmentMain newInstance() {
        TimerFragmentMain fragment = new TimerFragmentMain();
        return fragment;
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("puzzle", currentPuzzle);
        outState.putString("subtype", currentPuzzleSubtype);
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            currentPuzzle = savedInstanceState.getString("puzzle");
            currentPuzzleSubtype = savedInstanceState.getString("subtype");
        }
    }
    
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_timer_main, container, false);
        ButterKnife.bind(this, root);
        
        handleHeaderSpinner();
        setupToolbarForFragment(mToolbar);
        
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        pagerEnabled = sharedPreferences.getBoolean("pagerEnabled", true);
        
        if (pagerEnabled)
            viewPager.setPagingEnabled(true);
        else
            viewPager.setPagingEnabled(false);
        
        
        viewPagerAdapter = new NavigationAdapter(getFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setOffscreenPageLimit(2);

        tabLayout.setupWithViewPager(viewPager);
        if (tabLayout.getTabCount() == 3) {
            tabLayout.getTabAt(0).setIcon(R.drawable.ic_timer_white_24dp);
            tabLayout.getTabAt(1).setIcon(R.drawable.ic_format_list_bulleted_white_24dp);
            tabLayout.getTabAt(2).setIcon(R.drawable.ic_timeline_white_24dp);
        }

        tabStrip = ((LinearLayout) tabLayout.getChildAt(0));

        dbHandler = new DatabaseHandler(getContext());

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
                Broadcaster.broadcast(getActivity(), "TIMELIST", "SCROLLED PAGE");
            }
        });

        // Sets up the toolbar with the timer icons
        mToolbar.post(new Runnable() {
            @Override
            public void run() {
                setupPage(0, inflater);
            }
        });

        // Register a receiver to update if something has changed
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mReceiver, new IntentFilter("TIMER"));

        return root;
    }

    private void handleIcons(int index) {
        switch (index) {
            case 0:
                tabLayout.getTabAt(0).getIcon().setAlpha(255);
                tabLayout.getTabAt(1).getIcon().setAlpha(153); // 70%
                tabLayout.getTabAt(2).getIcon().setAlpha(153);
                break;
            case 1:
                tabLayout.getTabAt(0).getIcon().setAlpha(153);
                tabLayout.getTabAt(1).getIcon().setAlpha(255);
                tabLayout.getTabAt(2).getIcon().setAlpha(153);
                break;
            case 2:
                tabLayout.getTabAt(0).getIcon().setAlpha(153);
                tabLayout.getTabAt(1).getIcon().setAlpha(153);
                tabLayout.getTabAt(2).getIcon().setAlpha(255);
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
        super.onResume();
        viewPager.setCurrentItem(0, false);
        handleIcons(0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mReceiver);
        ButterKnife.unbind(this);
        dbHandler.closeDB();
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
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        final SharedPreferences.Editor editor = sharedPreferences.edit();

        // Create Subtype dialog
        createSubtypeDialog = new MaterialDialog.Builder(getContext())
                .title(R.string.enter_type_name)
                .inputRange(0, 16)
                .input(R.string.enter_type_name, 0, false, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog materialDialog, CharSequence input) {
                        dbHandler.addSolve(new Solve(1, currentPuzzle, input.toString(), 0L, "", PuzzleUtils.PENALTY_HIDETIME, "", true));
                        historyChecked = false; // Resets the checked state of the switch
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
        removeSubtypeDialog = new MaterialDialog.Builder(fragReference.getContext())
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
        renameSubtypeDialog = new MaterialDialog.Builder(fragReference.getContext())
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
        subtypeDialog = new MaterialDialog.Builder(fragReference.getContext())
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
                        currentPuzzleSubtype = subtypeList.get(which);
                        editor.putString(KEY_SAVEDSUBTYPE + currentPuzzle, currentPuzzleSubtype);
                        editor.apply();
                        historyChecked = false; // Resets the checked state of the switch
                        viewPager.setAdapter(viewPagerAdapter);
                        viewPager.setCurrentItem(currentPage);
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


        if (historyChecked) {
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
                if (isChecked) {
                    switchCompat.setThumbDrawable(thumb_negative);
                    switchCompat.setTrackResource(R.drawable.track_negative);
                    historyChecked = true;
                } else {
                    switchCompat.setThumbDrawable(thumb_positive);
                    switchCompat.setTrackDrawable(track_positive);
                    historyChecked = false;
                }
                Intent sendIntent = new Intent("TIMELIST");
                sendIntent.putExtra("action", "HISTORY");
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(sendIntent);
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
        if (actionMode != null)
            actionMode.finish();
        switch (pageNum) {
            case 0:
                //((MainActivity) getActivity()).hideFAB();
                // Scramble icon
                if (mToolbar != null) {
                    mToolbar.getMenu().clear();
                    mToolbar.getMenu().add(0, 5, 0, R.string.scramble_action).setIcon(R.drawable.ic_dice_white_24dp)
                            .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem menuItem) {
                                    currentTimerFragmentInstance.generateNewScramble();
                                    return true;
                                }
                            })
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                    setupTypeDialogItem();
                }
                break;
            case 1:
                //((MainActivity) getActivity()).showFAB();
                // Add menu icons
                if (mToolbar != null) {
                    mToolbar.getMenu().clear();
                    setupHistorySwitchItem(inflater);
                    setupTypeDialogItem();
                }
                break;
            case 2:
                //((MainActivity) getActivity()).hideFAB();
                // Add menu icons
                if (mToolbar != null) {
                    mToolbar.getMenu().clear();
                    setupHistorySwitchItem(inflater);
                    setupTypeDialogItem();
                }
                break;
        }
    }

    private void updateCurrentSubtype() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        final List<String> subtypeList = dbHandler.getAllSubtypesFromType(currentPuzzle);
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

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: // 333
                        currentPuzzle = PuzzleUtils.TYPE_333;
                        break;
                    case 1: // 222
                        currentPuzzle = PuzzleUtils.TYPE_222;
                        break;
                    case 2: // 444
                        currentPuzzle = PuzzleUtils.TYPE_444;
                        break;
                    case 3: // 555
                        currentPuzzle = PuzzleUtils.TYPE_555;
                        break;
                    case 4: // 666
                        currentPuzzle = PuzzleUtils.TYPE_666;
                        break;
                    case 5: // 777
                        currentPuzzle = PuzzleUtils.TYPE_777;
                        break;
                    case 6: // Clock
                        currentPuzzle = PuzzleUtils.TYPE_CLOCK;
                        break;
                    case 7: // Mega
                        currentPuzzle = PuzzleUtils.TYPE_MEGA;
                        break;
                    case 8: // Pyra
                        currentPuzzle = PuzzleUtils.TYPE_PYRA;
                        break;
                    case 9: // Skewb
                        currentPuzzle = PuzzleUtils.TYPE_SKEWB;
                        break;
                    case 10: // Square-1
                        currentPuzzle = PuzzleUtils.TYPE_SQUARE1;
                        break;
                }
                updateCurrentSubtype();
                viewPager.setAdapter(viewPagerAdapter);
                viewPager.setCurrentItem(currentPage);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    TimerFragment      currentTimerFragmentInstance;
    TimerListFragment  currentTimerListFragmentInstance;
    TimerGraphFragment currentTimerGraphFragmentInstance;

    protected class NavigationAdapter extends CacheFragmentStatePagerAdapter {

        private int mScrollY;

        public NavigationAdapter(FragmentManager fm) {
            super(fm);
        }

        public void setScrollY(int scrollY) {
            mScrollY = scrollY;
        }

        @Override
        protected Fragment createItem(int position) {
            switch (position) {
                case 0:
                    currentTimerFragmentInstance =
                            TimerFragment.newInstance(currentPuzzle, currentPuzzleSubtype);
                    return currentTimerFragmentInstance;
                case 1:
                    currentTimerListFragmentInstance =
                            TimerListFragment.newInstance(currentPuzzle, currentPuzzleSubtype, historyChecked);
                    return currentTimerListFragmentInstance;
                case 2:
                    currentTimerGraphFragmentInstance =
                            TimerGraphFragment.newInstance(currentPuzzle, currentPuzzleSubtype, historyChecked);
                    return currentTimerGraphFragmentInstance;
            }
            return TimerFragment.newInstance("333", "normal");
        }

        @Override
        public int getCount() {
            return 3;
        }

    }

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
                    Intent sendIntent = new Intent("TIMELIST");
                    sendIntent.putExtra("action", "DELETE SELECTED");
                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(sendIntent);
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            Intent sendIntent = new Intent("TIMELIST");
            sendIntent.putExtra("action", "REFRESH TIME");
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(sendIntent);
        }
    };

}
