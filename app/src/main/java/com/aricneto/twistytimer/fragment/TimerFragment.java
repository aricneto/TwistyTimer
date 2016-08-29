package com.aricneto.twistytimer.fragment;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.text.InputType;
import android.util.Log;
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
import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.database.DatabaseHandler;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.layout.ChronometerMilli;
import com.aricneto.twistytimer.listener.OnBackPressedInFragmentListener;
import com.aricneto.twistytimer.solver.RubiksCubeOptimalCross;
import com.aricneto.twistytimer.solver.RubiksCubeOptimalXCross;
import com.aricneto.twistytimer.stats.Statistics;
import com.aricneto.twistytimer.stats.StatisticsCache;
import com.aricneto.twistytimer.utils.PuzzleUtils;
import com.aricneto.twistytimer.utils.ScrambleGenerator;
import com.aricneto.twistytimer.utils.ThemeUtils;
import com.skyfishjy.library.RippleBackground;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

import static com.aricneto.twistytimer.stats.AverageCalculator.tr;
import static com.aricneto.twistytimer.utils.PuzzleUtils.NO_PENALTY;
import static com.aricneto.twistytimer.utils.PuzzleUtils.PENALTY_DNF;
import static com.aricneto.twistytimer.utils.PuzzleUtils.PENALTY_PLUSTWO;
import static com.aricneto.twistytimer.utils.PuzzleUtils.convertTimeToString;
import static com.aricneto.twistytimer.utils.TTIntent.*;

public class TimerFragment extends BaseFragment
        implements OnBackPressedInFragmentListener, StatisticsCache.StatisticsObserver {
    /**
     * Flag to enable debug logging for this class.
     */
    private static final boolean DEBUG_ME = true;

    /**
     * A "tag" to identify this class in log messages.
     */
    private static final String TAG = TimerFragment.class.getSimpleName();

    private static final String PUZZLE         = "puzzle";
    private static final String PUZZLE_SUBTYPE = "puzzle_type";

    private String currentPuzzle;
    private String currentPuzzleSubtype;

    private String currentScramble = "";
    private Solve  currentSolve    = null;
    private long currentId;

    private String realScramble;

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

    private ScrambleGenerator generator;

    private GenerateScrambleSequence scrambleGeneratorAsync;
    private GenerateScrambleImage    scrambleImageGenerator;
    private GetOptimalCross          crossCalculator;

    private int currentPenalty = NO_PENALTY;

    private int      mShortAnimationDuration;
    private Animator mCurrentAnimator;

    private Unbinder mUnbinder;
    @BindView(R.id.sessionDetailTimesAvg)  TextView detailTimesAvg;
    @BindView(R.id.sessionDetailTimesMore) TextView detailTimesMore;
    @BindView(R.id.detailLayout)           View     detailLayout;

    @BindView(R.id.chronometer)     ChronometerMilli    chronometer;
    @BindView(R.id.scrambleText)    TextView            scrambleText;
    @BindView(R.id.scrambleImg)     ImageView           scrambleImg;
    @BindView(R.id.expanded_image)  ImageView           expandedImageView;
    @BindView(R.id.inspectionText)  TextView            inspectionText;
    @BindView(R.id.progressSpinner) MaterialProgressBar progressSpinner;

    @BindView(R.id.hintCard)         CardView            hintCard;
    @BindView(R.id.panelText)        TextView            panelText;
    @BindView(R.id.panelSpinner)     MaterialProgressBar panelSpinner;
    @BindView(R.id.panelSpinnerText) TextView            panelSpinnerText;

    @BindView(R.id.button_delete)        ImageView        deleteButton;
    @BindView(R.id.button_dnf)           ImageView        dnfButton;
    @BindView(R.id.button_plustwo)       ImageView        plusTwoButton;
    @BindView(R.id.button_comment)       ImageView        commentButton;
    @BindView(R.id.button_undo)          ImageView        undoButton;
    @BindView(R.id.quick_action_buttons) LinearLayout     quickActionButtons;
    @BindView(R.id.rippleBackground)     RippleBackground rippleBackground;

    @BindView(R.id.root)                  RelativeLayout       rootLayout;
    @BindView(R.id.startTimerLayout)      FrameLayout          startTimerLayout;
    @BindView(R.id.sliding_layout) public SlidingUpPanelLayout slidingLayout;

    @BindView(R.id.congratsText) TextView congratsText;

    private boolean buttonsEnabled;
    private boolean scrambleImgEnabled;
    private boolean sessionStatsEnabled;
    private boolean worstSolveEnabled;
    private boolean backgroundEnabled;
    private boolean bestSolveEnabled;
    private boolean scrambleEnabled;
    private boolean holdEnabled;
    private boolean startCueEnabled;
    private float   scrambleTextSize;
    private boolean advancedEnabled;
    private boolean showHints;
    private boolean showHintsXCross;

    /**
     * The most recently notified solve time statistics. When {@link #addNewSolve()} is called to
     * add a new time, the new time can be compared to these statistics to determine if the new
     * time sets a record.
     */
    private Statistics mRecentStatistics;

    // Receives broadcasts related to changes to the timer user interface.
    private TTFragmentBroadcastReceiver mUIInteractionReceiver
            = new TTFragmentBroadcastReceiver(this, CATEGORY_UI_INTERACTIONS) {
        @Override
        public void onReceiveWhileAdded(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_SCROLLED_PAGE:
                    holdHandler.removeCallbacks(holdRunnable);
                    chronometer.setTextColor(ThemeUtils.fetchAttrColor(getContext(), R.attr.colorTimerText));
                    isReady = false;
                    break;

                case ACTION_TOOLBAR_RESTORED:
                    showItems();
                    animationDone = true;
                    break;

                case ACTION_GENERATE_SCRAMBLE:
                    generateNewScramble();
                    break;
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
            final DatabaseHandler dbHandler = TwistyTimer.getDBHandler();

            // On most of these changes to the current solve, the Statistics and ChartStatistics
            // need to be updated to reflect the change. It would probably be too complicated to
            // add facilities to "AverageCalculator" to handle modification of the last added time
            // or an "undo" facility and then to integrate that into the loaders. Therefore, a full
            // reload will probably be required.

            switch (view.getId()) {
                case R.id.button_delete:
                    new MaterialDialog.Builder(getContext())
                            .content(R.string.delete_dialog_confirmation_title)
                            .positiveText(R.string.delete_dialog_confirmation_button)
                            .negativeText(R.string.delete_dialog_cancel_button)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(MaterialDialog dialog, DialogAction which) {
                                    dbHandler.deleteSolve(currentSolve);
                                    broadcast(CATEGORY_TIME_DATA_CHANGES, ACTION_TIMES_MODIFIED);
                                }
                            })
                            .show();
                    break;
                case R.id.button_dnf:
                    currentSolve = PuzzleUtils.applyPenalty(currentSolve, PENALTY_DNF);
                    chronometer.setText("DNF");
                    dbHandler.updateSolve(currentSolve);
                    hideButtons(true, false);
                    broadcast(CATEGORY_TIME_DATA_CHANGES, ACTION_TIMES_MODIFIED);
                    break;
                case R.id.button_plustwo:
                    if (currentPenalty != PENALTY_PLUSTWO) {
                        currentSolve = PuzzleUtils.applyPenalty(currentSolve, PENALTY_PLUSTWO);
                        chronometer.setText(Html.fromHtml(
                                PuzzleUtils.convertTimeToStringWithSmallDecimal(currentSolve.getTime()) + " <small>+</small>"));
                        dbHandler.updateSolve(currentSolve);
                        broadcast(CATEGORY_TIME_DATA_CHANGES, ACTION_TIMES_MODIFIED);
                    }
                    hideButtons(true, false);
                    break;
                case R.id.button_comment:
                    MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                            .title(R.string.add_comment)
                            .input("", "", new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(MaterialDialog dialog, CharSequence input) {
                                    currentSolve.setComment(input.toString());
                                    dbHandler.updateSolve(currentSolve);
                                    // NOTE: At present, the Statistics and ChartStatistics do not
                                    // need to know about changes to a comment, so a notification
                                    // of this change does not need to be broadcast.
                                    Toast.makeText(getContext(), getString(R.string.added_comment), Toast.LENGTH_SHORT).show();
                                    hideButtons(false, true);
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
                        slidingLayout.setPanelState(PanelState.EXPANDED);
                        crossCalculator = new GetOptimalCross();
                        crossCalculator.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                    break;
                case R.id.button_undo:
                    // Undo the setting of a DNF or +2 penalty (does not undo a delete or comment).
                    currentSolve = PuzzleUtils.applyPenalty(currentSolve, NO_PENALTY);
                    chronometer.setText(Html.fromHtml(PuzzleUtils.convertTimeToStringWithSmallDecimal(currentSolve.getTime())));
                    dbHandler.updateSolve(currentSolve);
                    hideButtons(false, true);
                    broadcast(CATEGORY_TIME_DATA_CHANGES, ACTION_TIMES_MODIFIED);
                    break;
            }
        }
    };

    /**
     * Hides (or shows) the delete/dnf/plus-two quick action buttons and the undo button.
     */
    private void hideButtons(boolean hideQuickActionButtons, boolean hideUndoButton) {
        quickActionButtons.setVisibility(hideQuickActionButtons ? View.GONE : View.VISIBLE);
        undoButton.setVisibility(hideUndoButton ? View.GONE : View.VISIBLE);
    }

    public static TimerFragment newInstance(String puzzle, String puzzleSubType) {
        TimerFragment fragment = new TimerFragment();
        Bundle args = new Bundle();
        args.putString(PUZZLE, puzzle);
        args.putString(PUZZLE_SUBTYPE, puzzleSubType);
        fragment.setArguments(args);
        if (DEBUG_ME) Log.d(TAG, "newInstance() -> " + fragment);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (DEBUG_ME) Log.d(TAG, "onCreate(savedInstanceState=" + savedInstanceState + ")");
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentPuzzle = getArguments().getString(PUZZLE);
            currentPuzzleSubtype = getArguments().getString(PUZZLE_SUBTYPE);
        }

        scrambleGeneratorAsync = new GenerateScrambleSequence();

        generator = new ScrambleGenerator(currentPuzzle);
        // Register a receiver to update if something has changed
        registerReceiver(mUIInteractionReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        if (DEBUG_ME) Log.d(TAG, "onCreateView(savedInstanceState=" + savedInstanceState + ")");

        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_timer, container, false);
        mUnbinder = ButterKnife.bind(this, root);

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
        hintCard.setOnClickListener(buttonClickListener);
        undoButton.setOnClickListener(buttonClickListener);

        // Preferences //
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final boolean inspectionEnabled = sharedPreferences.getBoolean("inspectionEnabled", false);
        final int inspectionTime = sharedPreferences.getInt("inspectionTime", 15);

        advancedEnabled = sharedPreferences.getBoolean("enableAdvanced", false);
        scrambleTextSize = ((float) sharedPreferences.getInt("scrambleTextSize", 100)) / 100f;
        final boolean quickActionLarge = sharedPreferences.getBoolean("quickActionLarge", false);
        final float timerTextSize = ((float) sharedPreferences.getInt("timerTextSize", 100)) / 100f;
        float scrambleImageSize = ((float) sharedPreferences.getInt("scrambleImageSize", 100)) / 100f;
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
                currentPenalty = PENALTY_PLUSTWO;
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
                chronometer.setTextColor(ThemeUtils.fetchAttrColor(getContext(), R.attr.colorTimerText));
                currentPenalty = PENALTY_DNF;
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
                        stopChronometer();
                        if (currentPenalty == PENALTY_PLUSTWO)
                            chronometer.setText(Html.fromHtml(PuzzleUtils.convertTimeToStringWithSmallDecimal((int) chronometer.getTimeElapsed() + 2000) + " <small>+</small>"));
                        addNewSolve();

                    }
                }

                return false;
            }
        });

        // If the statistics are already loaded, the update notification will have been missed,
        // so fire that notification now. If the statistics are non-null, they will be displayed.
        // If they are null (i.e., not yet loaded), nothing will be displayed until this fragment,
        // as a registered observer, is notified when loading is complete. Post the firing of the
        // event, so that it is received after "onCreateView" returns.
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                onStatisticsUpdated(StatisticsCache.getInstance().getStatistics());
            }
        });
        StatisticsCache.getInstance().registerObserver(this); // Unregistered in "onDestroyView".

        return root;
    }

    @Override
    public void onResume() {
        if (DEBUG_ME) Log.d(TAG, "onResume()");
        super.onResume();
        slidingLayout.setPanelState(PanelState.HIDDEN);
    }

    /**
     * Hides the sliding panel showing the scramble image, or stops the chronometer, if either
     * action is necessary.
     *
     * @return
     *     {@code true} if the "Back" button press was consumed to hide the scramble or stop the
     *     timer; or {@code false} if neither was necessary and the "Back" button press was ignored.
     */
    @Override
    public boolean onBackPressedInFragment() {
        if (DEBUG_ME) Log.d(TAG, "onBackPressedInFragment()");

        if (isResumed()) {
            if (isRunning) {
                cancelChronometer();
                return true;
            } else if (slidingLayout != null
                    && slidingLayout.getPanelState() != PanelState.HIDDEN
                    && slidingLayout.getPanelState() != PanelState.COLLAPSED) {
                slidingLayout.setPanelState(PanelState.HIDDEN);
                return true;
            }
        }
        return false;
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
        if (currentPenalty != PENALTY_DNF) {
            currentSolve = new Solve(
                    (currentPenalty == PENALTY_PLUSTWO) ?
                            ((int) chronometer.getTimeElapsed() + 2000) : (int) chronometer.getTimeElapsed()
                    , currentPuzzle, currentPuzzleSubtype,
                    System.currentTimeMillis(), currentScramble, currentPenalty, "", false);

            declareRecordTimes(currentSolve);
        } else {
            currentSolve = new Solve(0, currentPuzzle, currentPuzzleSubtype,
                    System.currentTimeMillis(), currentScramble, PENALTY_DNF, "", false);
        }

        currentId = TwistyTimer.getDBHandler().addSolve(currentSolve);
        currentSolve.setId(currentId);
        currentPenalty = NO_PENALTY;

        // The receiver might be able to use the new solve and avoid accessing the database, so
        // parcel it up in the intent.
        new BroadcastBuilder(CATEGORY_TIME_DATA_CHANGES, ACTION_TIME_ADDED)
                .solve(currentSolve)
                .broadcast();
    }

    /**
     * Declares a new all-time best or worst solve time, if the new solve time sets a record. The
     * first valid solve time will not set any records; it is itself the best and worst time and
     * only later times will be compared to it. If the solve time is not greater than zero, or if
     * the solve is a DNF, the solve will be ignored and no new records will be declared.
     *
     * @param solve The solve (time) to be tested.
     */
    private void declareRecordTimes(Solve solve) {
        // NOTE: The old approach did not check for PB/record solves until at least 4 previous
        // solves had been recorded for the *current session*. This seemed a bit arbitrary. Perhaps
        // it had to do with waiting for the best and worst times to be loaded. If a user records
        // their *first* solve for the current session and it beats the best time from *any* past
        // session, it should be reported *immediately*, not ignored just because the session has
        // only started. However, the limit should perhaps have been 4 previous solves in the full
        // history of all past and current sessions. If this is the first ever session, then it
        // would be annoying if each of the first few times were reported as a record of some sort.
        // Therefore, do not report PB records until at least 4 previous *non-DNF* times have been
        // recorded in the database across all sessions, including the current session.

        final long newTime = solve.getTime();

        if (solve.getPenalty() == PENALTY_DNF || newTime <= 0
                || mRecentStatistics == null
                || mRecentStatistics.getAllTimeNumSolves()
                   - mRecentStatistics.getAllTimeNumDNFSolves() < 4) {
            // Not a valid time, or there are no previous statistics, or not enough previous times
            // to make reporting meaningful (or non-annoying), so cannot check for a new PB.
            return;
        }

        if (bestSolveEnabled) {
            final long previousBestTime = mRecentStatistics.getAllTimeBestTime();

            // If "previousBestTime" is a DNF or UNKNOWN, it will be less than zero, so the new
            // solve time cannot better (i.e., lower).
            if (newTime < previousBestTime ) {
                rippleBackground.startRippleAnimation();
                congratsText.setText(getString(R.string.personal_best_message,
                        PuzzleUtils.convertTimeToString(previousBestTime - newTime)));
                congratsText.setVisibility(View.VISIBLE);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (rippleBackground != null)
                            rippleBackground.stopRippleAnimation();
                    }
                }, 2940);
            }
        }

        if (worstSolveEnabled) {
            final long previousWorstTime = mRecentStatistics.getAllTimeWorstTime();

            // If "previousWorstTime" is a DNF or UNKNOWN, it will be less than zero. Therefore,
            // make sure it is at least greater than zero before testing against the new time.
            if (previousWorstTime > 0 && newTime > previousWorstTime) {
                congratsText.setText(getString(R.string.personal_worst_message,
                        PuzzleUtils.convertTimeToString(newTime - previousWorstTime)));

                if (backgroundEnabled)
                    congratsText.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_emoticon_poop_white_18dp, 0,
                            R.drawable.ic_emoticon_poop_white_18dp, 0);
                else
                    congratsText.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_emoticon_poop_black_18dp, 0,
                            R.drawable.ic_emoticon_poop_black_18dp, 0);

                congratsText.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Refreshes the display of the statistics. If this fragment has no view, or if the given
     * statistics are {@code null}, no update will be attempted.
     *
     * @param stats
     *     The updated statistics. These will not be modified.
     */
    @SuppressLint("SetTextI18n")
    @Override
    public void onStatisticsUpdated(Statistics stats) {
        if (DEBUG_ME) Log.d(TAG, "onStatisticsUpdated(" + stats + ")");

        if (getView() == null) {
            // Must have arrived after "onDestroyView" was called, so do nothing.
            return;
        }

        // Save these for later. The best and worst times can be retrieved and compared to the next
        // new solve time to be added via "addNewSolve".
        mRecentStatistics = stats; // May be null.

        if (stats == null) {
            return;
        }

        String sessionCount
                = String.format(Locale.getDefault(), "%,d", stats.getSessionNumSolves());
        String sessionMeanTime = convertTimeToString(tr(stats.getSessionMeanTime()));
        String sessionBestTime = convertTimeToString(tr(stats.getSessionBestTime()));
        String sessionWorstTime = convertTimeToString(tr(stats.getSessionWorstTime()));

        String sessionCurrentAvg5 = convertTimeToString(
                tr(stats.getAverageOf(5, true).getCurrentAverage()));
        String sessionCurrentAvg12 = convertTimeToString(
                tr(stats.getAverageOf(12, true).getCurrentAverage()));
        String sessionCurrentAvg50 = convertTimeToString(
                tr(stats.getAverageOf(50, true).getCurrentAverage()));
        String sessionCurrentAvg100 = convertTimeToString(
                tr(stats.getAverageOf(100, true).getCurrentAverage()));

        detailTimesAvg.setText(
                sessionCurrentAvg5 + "\n" +
                        sessionCurrentAvg12 + "\n" +
                        sessionCurrentAvg50 + "\n" +
                        sessionCurrentAvg100);

        detailTimesMore.setText(
                sessionMeanTime + "\n" +
                        sessionBestTime + "\n" +
                        sessionWorstTime + "\n" +
                        sessionCount);
    }

    private void generateScrambleImage() {
        scrambleImageGenerator = new GenerateScrambleImage();
        scrambleImageGenerator.execute();
    }

    private void showToolbar() {
        unlockOrientation(getActivity());
        broadcast(CATEGORY_UI_INTERACTIONS, ACTION_TIMER_STOPPED);
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
        broadcast(CATEGORY_UI_INTERACTIONS, ACTION_TIMER_STARTED);

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

    /**
     * Cancels the chronometer (stop it without saving anything)
     */
    public void cancelChronometer() {
        chronometer.stop();
        chronometer.setText(Html.fromHtml("0<small>.00</small>"));
        isRunning = false;
        isCanceled = true;
        isReady = false; // Reset variable
        inspectionText.setVisibility(View.GONE);
        countdown.cancel();
        plusTwoCountdown.cancel();
        countingDown = false;
        showToolbar();
    }

    /**
     * Stops the chronometer
     */
    private void stopChronometer() {
        chronometer.stop();
        isRunning = false;
        showToolbar();
    }

    @Override
    public void onDetach() {
        if (DEBUG_ME) Log.d(TAG, "onDetach()");
        super.onDetach();
        // To fix memory leaks
        unregisterReceiver(mUIInteractionReceiver);
        scrambleGeneratorAsync.cancel(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
        StatisticsCache.getInstance().unregisterObserver(this);
        mRecentStatistics = null;
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
                slidingLayout.setPanelState(PanelState.HIDDEN);
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
                                    slidingLayout.setPanelState(PanelState.EXPANDED);
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
