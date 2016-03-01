package com.aricneto.twistytimer.fragment;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.aricneto.twistify.R;
import com.aricneto.twistytimer.database.DatabaseHandler;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.layout.ChronometerMilli;
import com.aricneto.twistytimer.utils.PuzzleUtils;
import com.aricneto.twistytimer.utils.ScrambleGenerator;
import com.aricneto.twistytimer.utils.ThemeUtils;
import com.skyfishjy.library.RippleBackground;

import butterknife.Bind;
import butterknife.ButterKnife;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class TimerFragment extends BaseFragment {

    private static final String PUZZLE         = "puzzle";
    private static final String PUZZLE_SUBTYPE = "puzzle_type";

    private String currentPuzzle;
    private String currentPuzzleSubtype;

    private String currentScramble = "";
    private Solve currentSolve;

    private String realScramble;

    private DatabaseHandler dbHandler;

    CountDownTimer countdown;
    boolean countingDown = false;

    // If the show toolbar animation is done
    boolean animationDone = true;

    // Checks if the user has pressed the chronometer for long enough for it to start
    boolean isReady = false;

    // If the user has holdEnabled and held the DNF at the last second
    boolean holdingDNF;

    // Checks if the chronometer is running
    boolean isRunning = false;

    // Locks the chronometer so it doesn't start before a scramble sequence is generated
    boolean isLocked = true;


    private ScrambleGenerator generator;

    private GenerateScrambleSequence scrambleGeneratorAsync;
    private GenerateScrambleImage    scrambleImageGenerator;
    private CalculateStats           statCalculatorAsync;

    private int currentPenalty = PuzzleUtils.NO_PENALTY;

    private int      mShortAnimationDuration;
    private Animator mCurrentAnimator;

    @Bind(R.id.sessionDetailTimesAvg)  TextView detailTimesAvg;
    @Bind(R.id.sessionDetailTimesMore) TextView detailTimesMore;
    @Bind(R.id.detailLayout)           View     detailLayout;

    @Bind(R.id.chronometer)     ChronometerMilli    chronometer;
    @Bind(R.id.scrambleText)    TextView            scrambleText;
    @Bind(R.id.scrambleImg)     ImageView           scrambleImg;
    @Bind(R.id.expanded_image)  ImageView           expandedImageView;
    @Bind(R.id.inspectionText)  TextView            inspectionText;
    @Bind(R.id.progressSpinner) MaterialProgressBar progressSpinner;

    @Bind(R.id.button_delete)        ImageView        deleteButton;
    @Bind(R.id.button_dnf)           ImageView        dnfButton;
    @Bind(R.id.button_plustwo)       ImageView        plusTwoButton;
    @Bind(R.id.button_comment)       ImageView        commentButton;
    @Bind(R.id.quick_action_buttons) LinearLayout     quickActionButtons;
    @Bind(R.id.rippleBackground)     RippleBackground rippleBackground;

    @Bind(R.id.root)             RelativeLayout rootLayout;
    @Bind(R.id.startTimerLayout) FrameLayout    startTimerLayout;

    @Bind(R.id.congratsText) TextView congratsText;

    private boolean buttonsEnabled;
    private boolean scrambleImgEnabled;
    private boolean sessionStatsEnabled;
    private boolean worstSolveEnabled;
    private boolean backgroundEnabled;
    private boolean bestSolveEnabled;
    private boolean scrambleEnabled;
    private boolean holdEnabled;
    private boolean startCueEnabled;

    // Receives broadcasts from the timer
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isAdded()) { // The fragment has to check if it is attached to an activity. Removing this will bug the app
                switch (intent.getStringExtra("action")) {
                    case "TIME ADDED":
                        updateStats();
                        break;
                    case "MOVED TO HISTORY":
                        updateStats();
                        break;
                    case "SCROLLED PAGE":
                        holdHandler.removeCallbacks(holdRunnable);
                        isReady = false;
                        break;
                    case "TOOLBAR ENDED":
                        showItems();
                        animationDone = true;
                        break;
                }
            }
        }
    };

    private Runnable       holdRunnable;
    private Handler        holdHandler;
    private CountDownTimer plusTwoCountdown;


    public TimerFragment() {
        // Required empty public constructor
    }

    private View.OnClickListener buttonClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.button_delete:
                    new MaterialDialog.Builder(getContext())
                            .title(R.string.delete_dialog_confirmation_title)
                            .positiveText(R.string.delete_dialog_confirmation_button)
                            .negativeText(R.string.delete_dialog_cancel_button)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(MaterialDialog dialog, DialogAction which) {
                                    dbHandler.deleteSolveWithTimestamp(currentSolve);

                                    handleButtons(true);
                                }
                            })
                            .show();
                    break;
                case R.id.button_dnf:
                    currentSolve = PuzzleUtils.applyPenalty(currentSolve, PuzzleUtils.PENALTY_DNF);
                    chronometer.setText("DNF");
                    dbHandler.updateSolve(currentSolve);

                    handleButtons(true);
                    break;
                case R.id.button_plustwo:
                    if (currentPenalty != PuzzleUtils.PENALTY_PLUSTWO) {
                        currentSolve = PuzzleUtils.applyPenalty(currentSolve, PuzzleUtils.PENALTY_PLUSTWO);
                        chronometer.setText(PuzzleUtils.convertTimeToString(currentSolve.getTime()) + "+");
                        dbHandler.updateSolve(currentSolve);
                    }

                    handleButtons(true);
                    break;
                case R.id.button_comment:
                    MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                            .title(R.string.add_comment)
                            .input("", "", new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(MaterialDialog dialog, CharSequence input) {
                                    currentSolve.setComment(input.toString());
                                    dbHandler.updateSolve(currentSolve);
                                    Toast.makeText(getContext(), getString(R.string.added_comment), Toast.LENGTH_SHORT).show();
                                    handleButtons(false);
                                }
                            })
                            .inputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE)
                            .positiveText(R.string.action_done)
                            .negativeText(R.string.action_cancel)
                            .build();
                    EditText editText = dialog.getInputEditText();
                    if (editText != null) {
                        editText.setSingleLine(false);
                        editText.setLines(3);
                        editText.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
                        editText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
                    }
                    dialog.show();
                    break;
            }
        }
    };

    /**
     * Handle the delete/dnf/plustwo butons and sends a broadcast
     */
    private void handleButtons(boolean hideButtons) {
        if (hideButtons) {
            quickActionButtons.setVisibility(View.GONE);
        }
        Intent sendIntent = new Intent("TIMELIST");
        sendIntent.putExtra("action", "TIME ADDED");
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(sendIntent);
    }

    public static TimerFragment newInstance(String puzzle, String puzzleSubType) {
        TimerFragment fragment = new TimerFragment();
        Bundle args = new Bundle();
        args.putString(PUZZLE, puzzle);
        args.putString(PUZZLE_SUBTYPE, puzzleSubType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentPuzzle = getArguments().getString(PUZZLE);
            currentPuzzleSubtype = getArguments().getString(PUZZLE_SUBTYPE);
        }
        dbHandler = new DatabaseHandler(getContext());

        scrambleGeneratorAsync = new GenerateScrambleSequence();
        statCalculatorAsync = new CalculateStats();

        generator = new ScrambleGenerator(currentPuzzle);
        // Register a receiver to update if something has changed
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mReceiver, new IntentFilter("TIMELIST"));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_timer, container, false);
        ButterKnife.bind(this, root);

        statCalculatorAsync.execute();

        // Necessary for the scramble image to show
        scrambleImg.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        expandedImageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        // Set the zoom click listener
        scrambleImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                zoomImageFromThumb(scrambleImg, scrambleImg.getDrawable());
            }
        });

        // Retrieve and cache the system's default "short" animation time.
        mShortAnimationDuration = getResources().getInteger(
                android.R.integer.config_shortAnimTime);

        deleteButton.setOnClickListener(buttonClickListener);
        dnfButton.setOnClickListener(buttonClickListener);
        plusTwoButton.setOnClickListener(buttonClickListener);
        commentButton.setOnClickListener(buttonClickListener);

        // Preferences //
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final boolean inspectionEnabled = sharedPreferences.getBoolean("inspectionEnabled", false);
        final int inspectionTime = sharedPreferences.getInt("inspectionTime", 15);

        final float timerTextSize = ((float) sharedPreferences.getInt("timerTextSize", 10)) / 10f;
        final int timerTextOffset = sharedPreferences.getInt("timerTextOffset", 0);
        final boolean advancedEnabled = sharedPreferences.getBoolean("enableAdvanced", false);

        if (advancedEnabled) {
            chronometer.setTextSize(TypedValue.COMPLEX_UNIT_PX, chronometer.getTextSize() * timerTextSize);
            chronometer.setY(chronometer.getY() - timerTextOffset);
            inspectionText.setY(inspectionText.getY() - timerTextOffset);
            quickActionButtons.setY(quickActionButtons.getY() - timerTextOffset);
        }


        buttonsEnabled = sharedPreferences.getBoolean("buttonsEnabled", true);
        scrambleImgEnabled = sharedPreferences.getBoolean("scrambleImageEnabled", true);
        sessionStatsEnabled = sharedPreferences.getBoolean("sessionStatsEnabled", true);
        worstSolveEnabled = sharedPreferences.getBoolean("worstSolveEnabled", false);
        bestSolveEnabled = sharedPreferences.getBoolean("bestSolveEnabled", true);
        holdEnabled = sharedPreferences.getBoolean("holdEnabled", false);
        scrambleEnabled = sharedPreferences.getBoolean("scrambleEnabled", true);
        backgroundEnabled = sharedPreferences.getBoolean("backgroundEnabled", false);
        startCueEnabled = sharedPreferences.getBoolean("startCue", true);


        if (scrambleEnabled) {
            scrambleGeneratorAsync.execute();
        } else {
            scrambleText.setVisibility(View.GONE);
            scrambleImg.setVisibility(View.GONE);
            isLocked = false;
        }

        if (! scrambleImgEnabled)
            scrambleImg.setVisibility(View.GONE);
        if (! sessionStatsEnabled) {
            detailLayout.setVisibility(View.INVISIBLE);
        }
        // Preferences //

        // Inspection timer
        countdown = new CountDownTimer(inspectionTime * 1000, 500) {
            @Override
            public void onTick(long l) {
                if (chronometer != null) {
                    chronometer.setText(String.valueOf((l / 1000) + 1));
                }
            }

            @Override
            public void onFinish() {
                chronometer.setText("+2");
                currentPenalty = PuzzleUtils.PENALTY_PLUSTWO;
                plusTwoCountdown.start();
            }
        };

        plusTwoCountdown = new CountDownTimer(2000, 500) {
            @Override
            public void onTick(long l) {

            }

            @Override
            public void onFinish() {
                countingDown = false;
                isReady = false;
                isRunning = false;
                holdingDNF = true;
                chronometer.setText("DNF");
                showToolbar();
                unlockOrientation(getActivity());
                chronometer.setTextColor(ThemeUtils.fetchAttrColor(getContext(), R.attr.colorTimerText));
                currentPenalty = PuzzleUtils.PENALTY_DNF;
                addNewSolve();
                inspectionText.setVisibility(View.GONE);
            }
        };


        // Delay start
        holdHandler = new Handler();
        holdRunnable = new Runnable() {
            public void run() {
                isReady = true;
                chronometer.setTextColor(ThemeUtils.fetchAttrColor(getContext(), R.attr.colorAccent));
                if (! inspectionEnabled)
                    hideToolbar();
            }
        };

        // Chronometer
        startTimerLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (animationDone) {

                    if (countingDown) {

                        switch (motionEvent.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                if (holdEnabled && inspectionEnabled) {
                                    if (! isLocked)
                                        holdHandler.postDelayed(holdRunnable, 500);
                                } else {
                                    if (! isLocked) {
                                        if (startCueEnabled)
                                            chronometer.setTextColor(ThemeUtils.fetchAttrColor(getContext(), R.attr.colorAccent));
                                    }
                                }
                                return true;
                            case MotionEvent.ACTION_UP:
                                // Check if the user has hold delay enabled
                                if (holdEnabled && inspectionEnabled) {
                                    // Check if the user held the timer long enough
                                    if (isReady) {
                                        isReady = false; // Reset variable
                                        isRunning = true; // Set running to true to indicate we're running
                                        inspectionText.setVisibility(View.GONE);
                                        countdown.cancel();
                                        plusTwoCountdown.cancel();
                                        countingDown = false;
                                        startChronometer();
                                        chronometer.setTextColor(ThemeUtils.fetchAttrColor(getContext(), R.attr.colorTimerText));
                                    } else {
                                        holdHandler.removeCallbacks(holdRunnable);
                                        isReady = false;
                                    }
                                } else {
                                    inspectionText.setVisibility(View.GONE);
                                    countdown.cancel();
                                    plusTwoCountdown.cancel();
                                    isRunning = true; // Set running to true to indicate we're running
                                    countingDown = false;
                                    startChronometer();
                                }
                                return false;
                        }
                    } else if (! isRunning) {

                        switch (motionEvent.getAction()) {
                            case MotionEvent.ACTION_DOWN:

                                if (holdingDNF) {
                                    holdingDNF = false;
                                    chronometer.setTextColor(ThemeUtils.fetchAttrColor(getContext(), R.attr.colorTimerText));
                                }

                                if (holdEnabled && ! inspectionEnabled) {
                                    if (! isLocked)
                                        holdHandler.postDelayed(holdRunnable, 500);
                                } else if (! holdEnabled && ! inspectionEnabled) {
                                    if (! isLocked) {
                                        if (startCueEnabled)
                                            chronometer.setTextColor(ThemeUtils.fetchAttrColor(getContext(), R.attr.colorAccent));
                                    }
                                }

                                return true;
                            case MotionEvent.ACTION_UP:

                                if (holdingDNF) { // Checks if the user was holding the screen in a previous DNF by inspection timeout
                                    holdingDNF = false;
                                } else if (! isLocked) { // Before we start, check if the chronometer can start
                                    // Check if the user has hold delay enabled
                                    if (holdEnabled && ! inspectionEnabled) {
                                        // Check if the user held the timer long enough
                                        if (isReady) {
                                            isReady = false; // Reset variable
                                            isRunning = true; // Set running to true to indicate we're running
                                            startChronometer();
                                            chronometer.setTextColor(ThemeUtils.fetchAttrColor(getContext(), R.attr.colorTimerText));
                                        } else {
                                            holdHandler.removeCallbacks(holdRunnable);
                                            isReady = false;
                                        }

                                    } else if (holdEnabled && inspectionEnabled) {
                                        isRunning = true; // Set running to true to indicate we're running
                                        hideToolbar();
                                        // So it doesn't flash the old time when the inspection starts
                                        chronometer.setText(Integer.toString(inspectionTime));
                                        inspectionText.setVisibility(View.VISIBLE);
                                        countdown.start();
                                        countingDown = true;

                                    } else { // If he doesn't have hold delay enabled
                                        isRunning = true; // Set running to true to indicate we're running
                                        hideToolbar();
                                        if (inspectionEnabled) { // If inspection is enabled, start countdown
                                            // So it doesn't flash the old time when the inspection starts
                                            chronometer.setText(Integer.toString(inspectionTime));
                                            inspectionText.setVisibility(View.VISIBLE);
                                            countdown.start();
                                            countingDown = true;
                                        } else { // Else, start timer
                                            startChronometer();
                                        }
                                    }
                                }
                                return false;
                        }
                    } else if (motionEvent.getAction() == MotionEvent.ACTION_DOWN && chronometer.getTimeElapsed() >= 80) {
                        animationDone = false;
                        chronometer.stop();
                        isRunning = false;
                        unlockOrientation(getActivity());
                        showToolbar();
                        if (currentPenalty == PuzzleUtils.PENALTY_PLUSTWO)
                            chronometer.setText(PuzzleUtils.convertTimeToString((int) chronometer.getTimeElapsed() + 2000) + "+");
                        addNewSolve();

                    }
                }

                return false;
            }
        });

        updateStats();

        return root;
    }

    private void addNewSolve() {
        if (currentPenalty != PuzzleUtils.PENALTY_DNF) {
            currentSolve = new Solve(
                    currentPenalty == PuzzleUtils.PENALTY_PLUSTWO ?
                            (int) chronometer.getTimeElapsed() + 2000 : (int) chronometer.getTimeElapsed()
                    , currentPuzzle, currentPuzzleSubtype,
                    System.currentTimeMillis(), currentScramble, currentPenalty, "", false);
        } else {
            currentSolve = new Solve(0, currentPuzzle, currentPuzzleSubtype,
                    System.currentTimeMillis(), currentScramble, PuzzleUtils.PENALTY_DNF, "", false);
        }
        dbHandler.addSolve(currentSolve);

        updateStats();

        Intent sendIntent = new Intent("TIMELIST");
        sendIntent.putExtra("action", "TIME ADDED");
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(sendIntent);

        int bestGlobal = dbHandler.getBestOrWorstTime(true, false, currentPuzzle, currentPuzzleSubtype);
        int worstGlobal = dbHandler.getBestOrWorstTime(false, false, currentPuzzle, currentPuzzleSubtype);
        int solveCount = dbHandler.getSolveCount(currentPuzzle, currentPuzzleSubtype, false);

        // Checks if the current solve is the best or worst solve
        if (currentSolve != null && currentPenalty != PuzzleUtils.PENALTY_DNF) {
            if (solveCount >= 2) { // Start counting records at 2 solves
                if (bestSolveEnabled) {
                    if (currentSolve.getTime() == bestGlobal) { // best
                        rippleBackground.startRippleAnimation();
                        congratsText.setText(R.string.personal_best_message);
                        congratsText.setVisibility(View.VISIBLE);
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (rippleBackground != null)
                                    rippleBackground.stopRippleAnimation();
                            }
                        }, 2940);
                    }
                }
                if (worstSolveEnabled) {
                    if (currentSolve.getTime() == worstGlobal) { // worst
                        congratsText.setText(R.string.personal_worst_message);
                        if (backgroundEnabled)
                            congratsText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_emoticon_poop_white_18dp, 0, R.drawable.ic_emoticon_poop_white_18dp, 0);
                        else
                            congratsText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_emoticon_poop_black_18dp, 0, R.drawable.ic_emoticon_poop_black_18dp, 0);
                        congratsText.setVisibility(View.VISIBLE);
                    }
                }
            }
        }
        currentPenalty = PuzzleUtils.NO_PENALTY;
    }

    private void generateScrambleImage() {
        scrambleImageGenerator = new GenerateScrambleImage();
        scrambleImageGenerator.execute();
    }

    private void updateStats() {
        statCalculatorAsync.cancel(true);
        statCalculatorAsync = new CalculateStats();
        statCalculatorAsync.execute();
    }

    private void showToolbar() {
        Intent sendIntent = new Intent("TIMER");
        sendIntent.putExtra("action", "TIMER STOPPED");
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(sendIntent);
    }

    private void showItems() {
        scrambleImg.setEnabled(true);
        quickActionButtons.setEnabled(true);

        if (scrambleEnabled) {
            scrambleText.setVisibility(View.VISIBLE);
            if (scrambleImgEnabled)
                showImage();
        }
        if (sessionStatsEnabled) {
            detailLayout.setVisibility(View.VISIBLE);
            detailLayout.animate()
                    .alpha(1)
                    .setDuration(300);
        }
        if (buttonsEnabled) {
            quickActionButtons.setVisibility(View.VISIBLE);
            quickActionButtons.animate()
                    .alpha(1)
                    .setDuration(300);
        }
    }

    private void showImage() {
        scrambleImg.setVisibility(View.VISIBLE);
        scrambleImg.animate()
                .alpha(1)
                .setDuration(300);
    }

    private void hideImage() {
        scrambleImg.animate()
                .alpha(0)
                .setDuration(300)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if (scrambleImg != null)
                            scrambleImg.setVisibility(View.GONE);
                    }
                });
    }

    private void hideToolbar() {
        lockOrientation(getActivity());
        Intent sendIntent = new Intent("TIMER");
        sendIntent.putExtra("action", "TIMER STARTED");
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(sendIntent);

        congratsText.setVisibility(View.GONE);
        congratsText.setCompoundDrawables(null, null, null, null);

        scrambleImg.setEnabled(false);
        quickActionButtons.setEnabled(false);

        if (scrambleEnabled) {
            scrambleText.setVisibility(View.GONE);
            if (scrambleImgEnabled)
                hideImage();
        }
        if (sessionStatsEnabled) {
            detailLayout.animate()
                    .alpha(0)
                    .setDuration(300)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            detailLayout.setVisibility(View.INVISIBLE);
                        }
                    });
        }
        if (buttonsEnabled) {
            quickActionButtons.animate()
                    .alpha(0)
                    .setDuration(300)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            quickActionButtons.setVisibility(View.GONE);
                        }
                    });
        }
    }

    /**
     * Starts the chronometer
     */
    private void startChronometer() {
        if (chronometer != null) {
            chronometer.start();
            chronometer.setTextColor(ThemeUtils.fetchAttrColor(getContext(), R.attr.colorTimerText));
        }
        if (scrambleEnabled) {
            currentScramble = realScramble;
            generateNewScramble();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // To fix memory leaks
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mReceiver);
        dbHandler.close();
        ButterKnife.unbind(this);
        scrambleGeneratorAsync.cancel(true);
        statCalculatorAsync.cancel(true);

    }

    public static void lockOrientation(Activity activity) {
        Display display = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();
        int tempOrientation = activity.getResources().getConfiguration().orientation;
        int orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        switch (tempOrientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90)
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                else
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                break;
            case Configuration.ORIENTATION_PORTRAIT:
                if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_270)
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                else
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
        }
        activity.setRequestedOrientation(orientation);
    }

    private static void unlockOrientation(Activity activity) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    /**
     * Generates a new scramble and handles everything.
     */
    public void generateNewScramble() {
        if (scrambleEnabled) {
            scrambleGeneratorAsync.cancel(true);
            scrambleGeneratorAsync = new GenerateScrambleSequence();
            scrambleGeneratorAsync.execute();
        }
    }

    private class GenerateScrambleSequence extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            scrambleText.setText(R.string.generating_scramble);
            scrambleText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            scrambleText.setClickable(false);
            hideImage();
            if (! isRunning)
                progressSpinner.setVisibility(View.VISIBLE);
            isLocked = true;
        }

        @Override
        protected String doInBackground(String... params) {
            String scramble = generator.getPuzzle().generateScramble();
            return scramble;
        }

        @Override
        protected void onProgressUpdate(Void... values) {

        }

        @Override
        protected void onPostExecute(final String scramble) {
            if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                scrambleText.setText(scramble);
                scrambleText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                scrambleText.setClickable(false);
            } else {
                scrambleText.setVisibility(View.INVISIBLE);
                scrambleText.setText(scramble);
                scrambleText.post(new Runnable() {
                    @Override
                    public void run() {
                        if (scrambleText != null) {
                            if (scrambleText.getLineCount() > 1) {
                                scrambleText.setClickable(true);
                                scrambleText.setText(R.string.scramble_text_tap_hint);
                                scrambleText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_dice_white_24dp, 0, 0, 0);
                                scrambleText.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        new MaterialDialog.Builder(getContext())
                                                .content(scramble)
                                                .positiveText(R.string.action_ok)
                                                .show();
                                    }
                                });
                            } else {
                                scrambleText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                                scrambleText.setClickable(false);
                            }
                            if (! isRunning)
                                scrambleText.setVisibility(View.VISIBLE);
                        }
                    }
                });

            }
            realScramble = scramble;
            if (scrambleImgEnabled)
                generateScrambleImage();
            else
                progressSpinner.setVisibility(View.GONE);
            isLocked = false;
        }
    }

    private class GenerateScrambleImage extends AsyncTask<Void, Void, Drawable> {

        @Override
        protected Drawable doInBackground(Void... voids) {
            Drawable finalImg = generator.generateImageFromScramble(getContext(), realScramble);
            return finalImg;
        }

        @Override
        protected void onPostExecute(Drawable drawable) {
            super.onPostExecute(drawable);
            if (! isRunning) {
                if (scrambleImg != null)
                    showImage();
            }
            if (progressSpinner != null)
                progressSpinner.setVisibility(View.GONE);
            if (scrambleImg != null)
                scrambleImg.setImageDrawable(drawable);
            if (expandedImageView != null)
                expandedImageView.setImageDrawable(drawable);
        }
    }

    private class CalculateStats extends AsyncTask<Void, Void, int[]> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected int[] doInBackground(Void... voids) {
            int avg5 = dbHandler.getTruncatedAverageOf(5, currentPuzzle, currentPuzzleSubtype, true);
            int avg12 = dbHandler.getTruncatedAverageOf(12, currentPuzzle, currentPuzzleSubtype, true);
            int avg50 = dbHandler.getTruncatedAverageOf(50, currentPuzzle, currentPuzzleSubtype, true);
            int avg100 = dbHandler.getTruncatedAverageOf(100, currentPuzzle, currentPuzzleSubtype, false);
            int mean = dbHandler.getMean(true, currentPuzzle, currentPuzzleSubtype);
            int best = dbHandler.getBestOrWorstTime(true, true, currentPuzzle, currentPuzzleSubtype);
            int worst = dbHandler.getBestOrWorstTime(false, true, currentPuzzle, currentPuzzleSubtype);
            int count = dbHandler.getSolveCount(currentPuzzle, currentPuzzleSubtype, true);


            return new int[] { avg5, avg12, avg50, avg100, mean, best, worst, count };
        }

        @Override
        protected void onPostExecute(int[] times) {
            super.onPostExecute(times);

            String avg5 = PuzzleUtils.convertTimeToString(times[0]);
            String avg12 = PuzzleUtils.convertTimeToString(times[1]);
            String avg50 = PuzzleUtils.convertTimeToString(times[2]);
            String avg100 = PuzzleUtils.convertTimeToString(times[3]);
            String mean = PuzzleUtils.convertTimeToString(times[4]);
            String best = PuzzleUtils.convertTimeToString(times[5]);
            String worst = PuzzleUtils.convertTimeToString(times[6]);
            int count = times[7];

            // The following code makes androidstudio throw a fit, but it's alright since we're not going to be translating NUMBERS.
            detailTimesAvg.setText(
                    avg5 + "\n" +
                            avg12 + "\n" +
                            avg50 + "\n" +
                            avg100);

            detailTimesMore.setText(
                    mean + "\n" +
                            best + "\n" +
                            worst + "\n" +
                            count);


        }
    }


    private void zoomImageFromThumb(final View thumbView, Drawable image) {
        // If there's an animation in progress, cancel it
        // immediately and proceed with this one.
        if (mCurrentAnimator != null) {
            mCurrentAnimator.cancel();
        }


        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
        final Rect startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the container
        // view. Also set the container view's offset as the origin for the
        // bounds, since that's the origin for the positioning animation
        // properties (X, Y).
        thumbView.getGlobalVisibleRect(startBounds);
        rootLayout
                .getGlobalVisibleRect(finalBounds, globalOffset);
        startBounds.offset(- globalOffset.x, - globalOffset.y);
        finalBounds.offset(- globalOffset.x, - globalOffset.y);

        // Adjust the start bounds to be the same aspect ratio as the final
        // bounds using the "center crop" technique. This prevents undesirable
        // stretching during the animation. Also calculate the start scaling
        // factor (the end scaling factor is always 1.0).
        float startScale;
        if ((float) finalBounds.width() / finalBounds.height()
                > (float) startBounds.width() / startBounds.height()) {
            // Extend start bounds horizontally
            startScale = (float) startBounds.height() / finalBounds.height();
            float startWidth = startScale * finalBounds.width();
            float deltaWidth = (startWidth - startBounds.width()) / 2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;
        } else {
            // Extend start bounds vertically
            startScale = (float) startBounds.width() / finalBounds.width();
            float startHeight = startScale * finalBounds.height();
            float deltaHeight = (startHeight - startBounds.height()) / 2;
            startBounds.top -= deltaHeight;
            startBounds.bottom += deltaHeight;
        }

        // Hide the thumbnail and show the zoomed-in view. When the animation
        // begins, it will position the zoomed-in view in the place of the
        // thumbnail.
        thumbView.setAlpha(0f);
        expandedImageView.setVisibility(View.VISIBLE);

        // Set the pivot point for SCALE_X and SCALE_Y transformations
        // to the top-left corner of the zoomed-in view (the default
        // is the center of the view).
        expandedImageView.setPivotX(0f);
        expandedImageView.setPivotY(0f);

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y).
        AnimatorSet set = new AnimatorSet();
        set
                .play(ObjectAnimator.ofFloat(expandedImageView, View.X,
                        startBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.Y,
                        startBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X,
                        startScale, 1f)).with(ObjectAnimator.ofFloat(expandedImageView,
                View.SCALE_Y, startScale, 1f));
        set.setDuration(mShortAnimationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCurrentAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mCurrentAnimator = null;
            }
        });
        set.start();
        mCurrentAnimator = set;

        // Upon clicking the zoomed-in image, it should zoom back down
        // to the original bounds and show the thumbnail instead of
        // the expanded image.
        final float startScaleFinal = startScale;
        expandedImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCurrentAnimator != null) {
                    mCurrentAnimator.cancel();
                }

                // Animate the four positioning/sizing properties in parallel,
                // back to their original values.
                AnimatorSet set = new AnimatorSet();
                set.play(ObjectAnimator
                        .ofFloat(expandedImageView, View.X, startBounds.left))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.Y, startBounds.top))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.SCALE_X, startScaleFinal))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.SCALE_Y, startScaleFinal));
                set.setDuration(mShortAnimationDuration);
                set.setInterpolator(new DecelerateInterpolator());
                set.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        thumbView.setAlpha(1f);
                        expandedImageView.setVisibility(View.GONE);
                        mCurrentAnimator = null;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        thumbView.setAlpha(1f);
                        expandedImageView.setVisibility(View.GONE);
                        mCurrentAnimator = null;
                    }
                });
                set.start();
                mCurrentAnimator = set;
            }
        });
    }


}
