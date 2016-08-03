package com.aricneto.twistytimer.fragment;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
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
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.CardView;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
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
import com.aricneto.twistytimer.solver.RubiksCubeOptimalCross;
import com.aricneto.twistytimer.solver.RubiksCubeOptimalXCross;
import com.aricneto.twistytimer.utils.Broadcaster;
import com.aricneto.twistytimer.utils.PuzzleUtils;
import com.aricneto.twistytimer.utils.ScrambleGenerator;
import com.skyfishjy.library.RippleBackground;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import butterknife.Bind;
import butterknife.ButterKnife;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class TimerFragment extends BaseFragment {

    private static final String PUZZLE         = "puzzle";
    private static final String PUZZLE_SUBTYPE = "puzzle_type";

    /**
     * The time delay in milliseconds before starting the chronometer if the hold-for-start
     * preference is set.
     */
    private static final long HOLD_FOR_START_DELAY = 500L;

    private String currentPuzzle;
    private String currentPuzzleSubtype;

    private String currentScramble = "";
    private Solve  currentSolve    = null;

    private String realScramble;

    private DatabaseHandler dbHandler;

    CountDownTimer countdown;
    boolean countingDown = false;

    // True If the show toolbar animation is done
    boolean animationDone = true;

    // True if the user has pressed the chronometer for long enough for it to start
    boolean isReady = false;

    // True If the user has holdEnabled and held the DNF at the last second
    boolean holdingDNF;

    // Checks if the chronometer is running. Has to be public so the main fragment can access it
    public boolean isRunning = false;

    // Locks the chronometer so it doesn't start before a scramble sequence is generated
    boolean isLocked = true;

    // True If the chronometer has just been canceled
    private boolean isCanceled;

    // True If the scrambler is done calculating and can calculate a new hint.
    private boolean canShowHint = false;

    // True If the user pressed the undo button
    private boolean undone = false;

    private ScrambleGenerator generator;

    private GenerateScrambleSequence scrambleGeneratorAsync;
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

    @Bind(R.id.hintCard)         CardView            hintCard;
    @Bind(R.id.panelText)        TextView            panelText;
    @Bind(R.id.panelSpinner)     MaterialProgressBar panelSpinner;
    @Bind(R.id.panelSpinnerText) TextView            panelSpinnerText;

    @Bind(R.id.button_delete)        ImageView        deleteButton;
    @Bind(R.id.button_dnf)           ImageView        dnfButton;
    @Bind(R.id.button_plustwo)       ImageView        plusTwoButton;
    @Bind(R.id.button_comment)       ImageView        commentButton;
    @Bind(R.id.button_undo)          ImageView        undoButton;
    @Bind(R.id.quick_action_buttons) LinearLayout     quickActionButtons;
    @Bind(R.id.rippleBackground)     RippleBackground rippleBackground;

    @Bind(R.id.root)                  RelativeLayout       rootLayout;
    @Bind(R.id.startTimerLayout)      FrameLayout          startTimerLayout;
    @Bind(R.id.sliding_layout) public SlidingUpPanelLayout slidingLayout;

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
    private boolean showHints;
    private boolean showHintsXCross;

    // Global best/worst
    private int currentBestTime;
    private int currentWorstTime;

    // Receives broadcasts from the timer
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isAdded()) { // The fragment has to check if it is attached to an activity. Removing this will bug the app
                switch (intent.getStringExtra("action")) {
                    case "TIME UPDATED":
                    case "TIME ADDED":
                        updateStats();
                        break;
                    case "MOVED TO HISTORY":
                        updateStats();
                        break;
                    case "SCROLLED PAGE":
                        holdHandler.removeCallbacks(holdRunnable);
                        chronometer.setHighlighted(false);
                        chronometer.cancelHoldForStart();
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

    private RubiksCubeOptimalCross  optimalCross;
    private RubiksCubeOptimalXCross optimalXCross;
    private SharedPreferences       sharedPreferences;


    public TimerFragment() {
        // Required empty public constructor
    }

    private View.OnClickListener buttonClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.button_delete:
                    new MaterialDialog.Builder(getContext())
                            .content(R.string.delete_dialog_confirmation_title)
                            .positiveText(R.string.delete_dialog_confirmation_button)
                            .negativeText(R.string.delete_dialog_cancel_button)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog,
                                                    @NonNull DialogAction which) {
                                    dbHandler.deleteSolve(currentSolve);
                                    chronometer.reset(); // Reset to "0.00".
                                    congratsText.setVisibility(View.GONE);

                                    handleButtons(true);
                                }
                            })
                            .show();
                    break;
                case R.id.button_dnf:
                    currentSolve = PuzzleUtils.applyPenalty(currentSolve, PuzzleUtils.PENALTY_DNF);
                    chronometer.setPenalty(PuzzleUtils.PENALTY_DNF);
                    dbHandler.updateSolve(currentSolve);

                    handleButtons(true);
                    undoButton.setVisibility(View.VISIBLE);
                    break;
                case R.id.button_plustwo:
                    if (currentPenalty != PuzzleUtils.PENALTY_PLUSTWO) {
                        currentSolve = PuzzleUtils.applyPenalty(currentSolve, PuzzleUtils.PENALTY_PLUSTWO);
                        chronometer.setPenalty(PuzzleUtils.PENALTY_PLUSTWO);
                        dbHandler.updateSolve(currentSolve);
                    }

                    handleButtons(true);
                    undoButton.setVisibility(View.VISIBLE);
                    break;
                case R.id.button_comment:
                    MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                            .title(R.string.add_comment)
                            .input("", "", new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(@NonNull MaterialDialog dialog,
                                                    CharSequence input) {
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
                case R.id.hintCard:
                    if (canShowHint) {
                        panelText.setVisibility(View.GONE);
                        panelText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                        panelText.setGravity(Gravity.LEFT);
                        panelSpinner.setVisibility(View.VISIBLE);
                        panelSpinnerText.setVisibility(View.VISIBLE);
                        slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                        new GetOptimalCross().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                    break;
                case R.id.button_undo:
                    currentSolve = PuzzleUtils.applyPenalty(currentSolve, PuzzleUtils.NO_PENALTY);
                    chronometer.setPenalty(PuzzleUtils.NO_PENALTY);
                    dbHandler.updateSolve(currentSolve);
                    undoButton.setVisibility(View.GONE);
                    undone = true;
                    handleButtons(false);
                    break;
            }
        }
    };

    /**
     * Handle the delete/dnf/plus-two buttons and sends a broadcast
     */
    private void handleButtons(boolean hideButtons) {
        if (hideButtons) {
            quickActionButtons.setVisibility(View.GONE);
        } else {
            quickActionButtons.setVisibility(View.VISIBLE);
        }
        Broadcaster.broadcast(getActivity(), "TIMELIST", "TIME UPDATED");
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
    public void onResume() {
        super.onResume();
        slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
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
        updateBestAndWorst();

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
                zoomImageFromThumb(scrambleImg);
            }
        });

        // Retrieve and cache the system's default "short" animation time.
        mShortAnimationDuration = getResources().getInteger(
                android.R.integer.config_shortAnimTime);

        deleteButton.setOnClickListener(buttonClickListener);
        dnfButton.setOnClickListener(buttonClickListener);
        plusTwoButton.setOnClickListener(buttonClickListener);
        commentButton.setOnClickListener(buttonClickListener);
        hintCard.setOnClickListener(buttonClickListener);
        undoButton.setOnClickListener(buttonClickListener);

        // Preferences //
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final boolean inspectionEnabled = sharedPreferences.getBoolean("inspectionEnabled", false);
        final int inspectionTime = sharedPreferences.getInt("inspectionTime", 15);
        final boolean advancedEnabled = sharedPreferences.getBoolean("enableAdvanced", false);
        final float scrambleTextSize = ((float) sharedPreferences.getInt("scrambleTextSize", 100)) / 100f;
        final boolean quickActionLarge = sharedPreferences.getBoolean("quickActionLarge", false);
        final float timerTextSize = ((float) sharedPreferences.getInt("timerTextSize", 100)) / 100f;
        final float scrambleImageSize = ((float) sharedPreferences.getInt("scrambleImageSize", 100)) / 100f;
        final int timerTextOffset = sharedPreferences.getInt("timerTextOffset", 0);

        scrambleText.setTextSize(TypedValue.COMPLEX_UNIT_PX, scrambleText.getTextSize() * scrambleTextSize);

        if (advancedEnabled) {
            chronometer.setTextSize(TypedValue.COMPLEX_UNIT_PX, chronometer.getTextSize() * timerTextSize);

            if (quickActionLarge) {
                deleteButton.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_delete_white_36dp));
                commentButton.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_comment_white_36dp));
                dnfButton.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.dnflarge));
                plusTwoButton.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.plustwolarge));
            }

            scrambleImg.getLayoutParams().width *= scrambleImageSize;
            scrambleImg.getLayoutParams().height *= calculateScrambleImageHeightMultiplier(scrambleImageSize);

            chronometer.setY(chronometer.getY() - timerTextOffset);
            inspectionText.setY(inspectionText.getY() - timerTextOffset);
            quickActionButtons.setY(quickActionButtons.getY() - timerTextOffset);
            undoButton.setY(undoButton.getY() - timerTextOffset);
            congratsText.setY(congratsText.getY() - timerTextOffset);
        } else {
            scrambleImg.getLayoutParams().height *= calculateScrambleImageHeightMultiplier(1);
        }

        buttonsEnabled = sharedPreferences.getBoolean("buttonsEnabled", true);
        scrambleImgEnabled = sharedPreferences.getBoolean("scrambleImageEnabled", true);
        sessionStatsEnabled = sharedPreferences.getBoolean("sessionStatsEnabled", true);
        worstSolveEnabled = sharedPreferences.getBoolean("worstSolveEnabled", false);
        bestSolveEnabled = sharedPreferences.getBoolean("bestSolveEnabled", true);
        holdEnabled = sharedPreferences.getBoolean("holdEnabled", false);
        scrambleEnabled = sharedPreferences.getBoolean("scrambleEnabled", true);
        backgroundEnabled = sharedPreferences.getBoolean("backgroundEnabled", false);
        startCueEnabled = sharedPreferences.getBoolean("startCue", false);
        showHints = sharedPreferences.getBoolean("showHints", true);
        showHintsXCross = sharedPreferences.getBoolean("showHintsXCross", false);

        if (showHints && currentPuzzle.equals(PuzzleUtils.TYPE_333) && scrambleEnabled) {
            hintCard.setVisibility(View.VISIBLE);
            optimalCross = new RubiksCubeOptimalCross(getString(R.string.optimal_cross));
            optimalXCross = new RubiksCubeOptimalXCross(getString(R.string.optimal_x_cross));
        }

        if (scrambleEnabled) {
            scrambleGeneratorAsync.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
        if (inspectionEnabled) {
            countdown = new CountDownTimer(inspectionTime * 1000, 500) {
                @Override
                public void onTick(long l) {
                    chronometer.setText(String.valueOf((l / 1000) + 1));
                }

                @Override
                public void onFinish() {
                    chronometer.setText("+2");
                    // "+2" penalty is applied to "chronometer" when timer is eventually stopped.
                    currentPenalty = PuzzleUtils.PENALTY_PLUSTWO;
                    plusTwoCountdown.start();
                }
            };

            plusTwoCountdown = new CountDownTimer(2000, 500) {
                @Override
                public void onTick(long l) {
                    // The displayed value remains "+2" for the duration of this countdown.
                }

                @Override
                public void onFinish() {
                    // After counting down the inspection period, a "+2" penalty was counted down
                    // before the solve started, so this is a DNF. If the timer starts before this
                    // countdown ends, then "plusTwoCountdown" is cancelled before this happens.
                    countingDown = false;
                    isReady = false;
                    holdingDNF = true;
                    currentPenalty = PuzzleUtils.PENALTY_DNF;
                    chronometer.setPenalty(PuzzleUtils.PENALTY_DNF);
                    stopChronometer();
                    addNewSolve();
                    inspectionText.setVisibility(View.GONE);
                }
            };
        }

        // If hold-for-start is enabled, use the "isReady" flag to indicate if the hold was long
        // enough (0.5s) to trigger the starting of the timer.
        if (holdEnabled) {
            holdHandler = new Handler();
            holdRunnable = new Runnable() {
                public void run() {
                    isReady = true;
                    // Indicate to the user that the hold was long enough.
                    chronometer.setHighlighted(true);
                    if (! inspectionEnabled) {
                        // If inspection is enabled, the toolbar is already hidden.
                        hideToolbar();
                    }
                }
            };
        }

        // Chronometer
        startTimerLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (!animationDone || isLocked) {
                    // Not ready to start the timer, yet. May be waiting on the animation of the
                    // restoration of the tool-bars after the timer was stopped, or waiting on the
                    // generation of a scramble ("isLocked" flag). In these states, the timer cannot
                    // be running or counting down, so touches are ignored for now.
                    return false;
                }

                if (countingDown) { // "countingDown == true" => "inspectionEnabled == true"
                    switch (motionEvent.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            // During inspection, touching down changes the text highlight color
                            // to indicate readiness to start timing. If the hold-for-start delay
                            // is enabled, that color change will be delayed. The timer will not
                            // start until the touch is lifted up, but the inspection countdown
                            // will still continue in the meantime.
                            if (holdEnabled) {
                                isReady = false;
                                holdHandler.postDelayed(holdRunnable, HOLD_FOR_START_DELAY);
                            } else if (startCueEnabled) {
                                chronometer.setHighlighted(true);
                            }
                            // "chronometer.holdForStart" is not called here; it displays "0.00",
                            // which would interfere with the continuing countdown of the inspection
                            // period and, anyway, be overwritten by the next countdown "tick".
                            return true;

                        case MotionEvent.ACTION_UP:
                            // Counting down inspection period. User has already touched down after
                            // starting the inspection, so start the timer unless "hold-to-start"
                            // is enabled and the hold delay was not long enough.
                            if (holdEnabled && !isReady) {
                                holdHandler.removeCallbacks(holdRunnable);
                            } else {
                                stopInspectionCountdown();
                                startChronometer(); // Tool-bar is already hidden and remains so.
                            }
                            return false;
                    }
                } else if (! isRunning) { // Not running and not counting down.
                    switch (motionEvent.getAction()) {
                        case MotionEvent.ACTION_DOWN:

                            if (holdingDNF) {
                                holdingDNF = false;
                                chronometer.setHighlighted(false);
                            }

                            if (!inspectionEnabled) {
                                if (holdEnabled) {
                                    isReady = false;
                                    holdHandler.postDelayed(holdRunnable, HOLD_FOR_START_DELAY);
                                } else if (startCueEnabled) {
                                    chronometer.setHighlighted(true);
                                }
                                // Display "0.00" while holding in readiness for a new solve.
                                // This is not used above when inspection is enabled, as it would
                                // interfere with the countdown display.
                                chronometer.holdForStart();
                            }
                            return true;

                        case MotionEvent.ACTION_UP:

                            if (holdingDNF) { // Checks if the user was holding the screen in a previous DNF by inspection timeout
                                holdingDNF = false;
                            } else if (inspectionEnabled) {
                                hideToolbar();
                                startInspectionCountdown(inspectionTime);
                            } else if (holdEnabled && !isReady) {
                                // Not held for long enough. Replace "0.00" with previous value.
                                chronometer.cancelHoldForStart();
                                holdHandler.removeCallbacks(holdRunnable);
                            } else {
                                // Inspection disabled. Hold-for-start disabled, or hold-for-start
                                // enabled, but the hold time was long enough. In the latter case,
                                // the tool-bar will already have been hidden. Start timing!
                                if (!holdEnabled) {
                                    hideToolbar();
                                }
                                startChronometer();
                            }
                            return false;
                    }
                } else if (motionEvent.getAction() == MotionEvent.ACTION_DOWN
                        && chronometer.getElapsedTime() >= 80) { // => "isRunning == true"
                    // Chronometer is timing a solve (running, not counting down inspection period).
                    // Stop the timer if it has been running for long enough (80 ms) for this not to
                    // be an accidental touch as the user lifted up the touch to start the timer.
                    animationDone = false;
                    stopChronometer();
                    if (currentPenalty == PuzzleUtils.PENALTY_PLUSTWO) {
                        chronometer.setPenalty(PuzzleUtils.PENALTY_PLUSTWO);
                    }
                    addNewSolve();
                }
                return false;
            }
        });

        updateStats();

        return root;
    }

    /**
     * Stops the inspection period countdown (if it is active). This cancels the inspection
     * countdown timer and associated "+2" countdown timer and hides the inspection text.
     */
    private void stopInspectionCountdown() {
        // These timers may be null if inspection was not enabled when "onCreate" was called.
        if (countdown != null) {
            countdown.cancel();
        }
        if (plusTwoCountdown != null) {
            plusTwoCountdown.cancel();
        }

        inspectionText.setVisibility(View.GONE);
        countingDown = false;
    }

    /**
     * Starts the inspection period countdown.
     *
     * @param inspectionTime
     *     The inspection time in seconds.
     */
    private void startInspectionCountdown(int inspectionTime) {
        // The "countdown" timer may be null if inspection was not enabled when "onCreate" was
        // called. In that case this method will not be called from the touch listener.

        // So it doesn't flash the old time when the inspection starts
        chronometer.setText(String.valueOf(inspectionTime));
        inspectionText.setVisibility(View.VISIBLE);
        countdown.start();
        countingDown = true;
    }

    /**
     * Calculates scramble image height multiplier to respect aspect ratio
     *
     * @param multiplier the height multiplier (must be the same multiplier as the width)
     *
     * @return the height in px
     */
    private float calculateScrambleImageHeightMultiplier(float multiplier) {
        switch (currentPuzzle) {
            case PuzzleUtils.TYPE_777:
            case PuzzleUtils.TYPE_666:
            case PuzzleUtils.TYPE_555:
            case PuzzleUtils.TYPE_222:
            case PuzzleUtils.TYPE_444:
            case PuzzleUtils.TYPE_333:
                // 3 faces of the cube vertically divided by 4 faces horizontally (it draws the cube like a cross)
                return (multiplier / 4) * 3;
            case PuzzleUtils.TYPE_CLOCK:
                return multiplier / 2;
            case PuzzleUtils.TYPE_MEGA:
                return (multiplier / 2);
            case PuzzleUtils.TYPE_PYRA:
                // Just pythagoras. Height of an equilateral triangle
                return (float) (multiplier / Math.sqrt(1.25));
            case PuzzleUtils.TYPE_SKEWB:
                // This one is the same as the NxN cubes
                return (multiplier / 4) * 3;
            case PuzzleUtils.TYPE_SQUARE1: // Square-1
                return multiplier;
        }
        return multiplier;
    }

    private void addNewSolve() {
        currentSolve = new Solve(
                (int) chronometer.getElapsedTime(), // Includes any "+2" penalty. Is zero for "DNF".
                currentPuzzle, currentPuzzleSubtype,
                System.currentTimeMillis(), currentScramble, currentPenalty, "", false);

        currentSolve.setId(dbHandler.addSolve(currentSolve));

        Broadcaster.broadcast(getActivity(), "TIMELIST", "TIME ADDED");

        currentPenalty = PuzzleUtils.NO_PENALTY;
    }

    private void generateScrambleImage() {
        new GenerateScrambleImage().execute();
    }

    private void updateStats() {
        statCalculatorAsync.cancel(true);
        statCalculatorAsync = new CalculateStats();
        statCalculatorAsync.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void showToolbar() {
        unlockOrientation(getActivity());
        Intent sendIntent = new Intent("TIMER");
        sendIntent.putExtra("action", "TIMER STOPPED");
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(sendIntent);
    }

    private void showItems() {
        if (scrambleEnabled) {
            scrambleText.setEnabled(true);
            scrambleText.setVisibility(View.VISIBLE);
            if (scrambleImgEnabled) {
                scrambleImg.setEnabled(true);
                showImage();
            }
            if (showHints && currentPuzzle.equals(PuzzleUtils.TYPE_333) && scrambleEnabled) {
                hintCard.setEnabled(true);
                hintCard.setVisibility(View.VISIBLE);
                hintCard.animate()
                        .alpha(1)
                        .setDuration(300);
            }
        }
        if (sessionStatsEnabled) {
            detailLayout.setVisibility(View.VISIBLE);
            detailLayout.animate()
                    .alpha(1)
                    .setDuration(300);
        }
        if (buttonsEnabled && ! isCanceled) {
            quickActionButtons.setEnabled(true);
            quickActionButtons.setVisibility(View.VISIBLE);
            quickActionButtons.animate()
                    .alpha(1)
                    .setDuration(300);
        }
        isCanceled = false;
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
        undone = false;
        Intent sendIntent = new Intent("TIMER");
        sendIntent.putExtra("action", "TIMER STARTED");
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(sendIntent);

        congratsText.setVisibility(View.GONE);
        congratsText.setCompoundDrawables(null, null, null, null);

        if (scrambleEnabled) {
            scrambleText.setEnabled(false);
            scrambleText.setVisibility(View.INVISIBLE);
            if (scrambleImgEnabled) {
                scrambleImg.setEnabled(false);
                hideImage();
            }
            if (showHints && currentPuzzle.equals(PuzzleUtils.TYPE_333) && scrambleEnabled) {
                hintCard.setEnabled(false);
                hintCard.animate()
                        .alpha(0)
                        .setDuration(300)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                hintCard.setVisibility(View.INVISIBLE);
                            }
                        });
            }
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
            undoButton.setVisibility(View.GONE);
            quickActionButtons.setEnabled(false);
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
     * Starts the chronometer from zero and removes any color highlight.
     */
    private void startChronometer() {
        chronometer.reset(); // Start from "0.00"; do not resume from the previous time.
        chronometer.start();
        chronometer.setHighlighted(false); // Clear any start cue or hold-for-start highlight.

        if (scrambleEnabled) {
            currentScramble = realScramble;
            generateNewScramble();
        }

        isRunning = true;
    }

    /**
     * Stops the chronometer
     */
    private void stopChronometer() {
        chronometer.stop();
        chronometer.setHighlighted(false);
        isRunning = false;
        showToolbar();
    }

    /**
     * Cancels the chronometer and any inspection countdown. Nothing is saved and the timer is
     * reset to zero.
     */
    public void cancelChronometer() {
        stopInspectionCountdown();
        stopChronometer();

        chronometer.reset(); // Show "0.00".
        isCanceled = true;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // To fix memory leaks
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mReceiver);
        dbHandler.closeDB();
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
            scrambleGeneratorAsync.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private class GetOptimalCross extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... voids) {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            String text = "";
            text += optimalCross.getTip(realScramble);
            if (showHintsXCross) {
                text += "\n\n";
                text += optimalXCross.getTip(realScramble);
            }
            return text;
        }

        @Override
        protected void onPostExecute(String text) {
            super.onPostExecute(text);
            if (panelSpinnerText != null && panelText != null && ! isRunning) {
                panelText.setText(text);
                panelText.setVisibility(View.VISIBLE);
                panelSpinner.setVisibility(View.GONE);
                panelSpinnerText.setVisibility(View.GONE);
            }
        }
    }

    private class GenerateScrambleSequence extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            if (showHints && currentPuzzle.equals(PuzzleUtils.TYPE_333) && scrambleEnabled)
                slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
            canShowHint = false;
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
            return generator.getPuzzle().generateScramble();
        }

        @Override
        protected void onProgressUpdate(Void... values) {

        }

        @Override
        protected void onPostExecute(final String scramble) {
            scrambleText.setVisibility(View.INVISIBLE);
            scrambleText.setText(scramble);
            scrambleText.post(new Runnable() {
                @Override
                public void run() {
                    if (scrambleText != null) {
                        Rect scrambleRect = new Rect(scrambleText.getLeft(), scrambleText.getTop(), scrambleText.getRight(), scrambleText.getBottom());
                        Rect chronometerRect = new Rect(chronometer.getLeft(), chronometer.getTop(), chronometer.getRight(), chronometer.getBottom());
                        Rect congratsRect = new Rect(congratsText.getLeft(), congratsText.getTop(), congratsText.getRight(), congratsText.getBottom());

                        if ((Rect.intersects(scrambleRect, chronometerRect)) ||
                                (congratsText.getVisibility() == View.VISIBLE && Rect.intersects(scrambleRect, congratsRect))) {
                            scrambleText.setClickable(true);
                            scrambleText.setText(R.string.scramble_text_tap_hint);
                            scrambleText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_dice_white_24dp, 0, 0, 0);
                            scrambleText.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    panelText.setText(scramble);
                                    panelText.setTextSize(TypedValue.COMPLEX_UNIT_PX, scrambleText.getTextSize());
                                    panelText.setGravity(Gravity.CENTER);
                                    panelSpinner.setVisibility(View.GONE);
                                    panelSpinnerText.setVisibility(View.GONE);
                                    slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
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
            realScramble = scramble;
            if (scrambleImgEnabled)
                generateScrambleImage();
            else
                progressSpinner.setVisibility(View.GONE);
            isLocked = false;

            canShowHint = true;
        }
    }

    private class GenerateScrambleImage extends AsyncTask<Void, Void, Drawable> {

        @Override
        protected Drawable doInBackground(Void... voids) {
            return generator.generateImageFromScramble(sharedPreferences, realScramble);
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
            int bestSession = dbHandler.getBestOrWorstTime(true, true, currentPuzzle, currentPuzzleSubtype);
            int worstSession = dbHandler.getBestOrWorstTime(false, true, currentPuzzle, currentPuzzleSubtype);
            int count = dbHandler.getSolveCount(currentPuzzle, currentPuzzleSubtype, true);


            return new int[] { avg5, avg12, avg50, avg100, mean, bestSession, worstSession, count };
        }

        @SuppressLint("SetTextI18n")
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

            // The following code makes Android Studio throw a fit (so suppressed above), but it's
            // alright since we're not going to be translating NUMBERS.
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

            if (count == 3) {
                updateBestAndWorst();
            }

            // Check best/worst solve
            if (currentSolve != null && currentPenalty != PuzzleUtils.PENALTY_DNF && ! undone) {
                if (count >= 4 && currentSolve.getTime() != 0) { // Start counting records at 5 solves
                    if (bestSolveEnabled) {
                        if (currentSolve.getTime() < currentBestTime) { // best
                            rippleBackground.startRippleAnimation();
                            congratsText.setText(getString(R.string.personal_best_message,
                                    PuzzleUtils.convertTimeToString(currentBestTime - currentSolve.getTime())));
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
                    } if (worstSolveEnabled) {
                        if (currentSolve.getTime() > currentWorstTime) { // worst
                            congratsText.setText(getString(R.string.personal_worst_message,
                                    PuzzleUtils.convertTimeToString(currentSolve.getTime() - currentWorstTime)));

                            if (backgroundEnabled)
                                congratsText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_emoticon_poop_white_18dp, 0, R.drawable.ic_emoticon_poop_white_18dp, 0);
                            else
                                congratsText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_emoticon_poop_black_18dp, 0, R.drawable.ic_emoticon_poop_black_18dp, 0);
                            congratsText.setVisibility(View.VISIBLE);
                        }
                    }
                }
                updateBestAndWorst();
            }

        }
    }

    private class CalculateBestAndWorst extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            currentBestTime = dbHandler.getBestOrWorstTime(true, false, currentPuzzle, currentPuzzleSubtype);
            currentWorstTime = dbHandler.getBestOrWorstTime(false, false, currentPuzzle, currentPuzzleSubtype);
            return null;
        }
    }

    private void updateBestAndWorst() {
        if (dbHandler != null) {
            new CalculateBestAndWorst().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }


    private void zoomImageFromThumb(final View thumbView) {
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
        rootLayout.getGlobalVisibleRect(finalBounds, globalOffset);
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
