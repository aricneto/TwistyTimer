package com.aricneto.twistytimer.fragment;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Process;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.cardview.widget.CardView;

import android.text.Html;
import android.text.InputType;
import android.util.Log;
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
import com.aricneto.twistytimer.TwistyTimer;
import com.aricneto.twistytimer.database.DatabaseHandler;
import com.aricneto.twistytimer.fragment.dialog.BottomSheetDetailDialog;
import com.aricneto.twistytimer.items.Solve;
import com.aricneto.twistytimer.layout.ChronometerMilli;
import com.aricneto.twistytimer.listener.OnBackPressedInFragmentListener;
import com.aricneto.twistytimer.puzzle.TrainerScrambler;
import com.aricneto.twistytimer.solver.RubiksCubeOptimalCross;
import com.aricneto.twistytimer.solver.RubiksCubeOptimalXCross;
import com.aricneto.twistytimer.stats.Statistics;
import com.aricneto.twistytimer.stats.StatisticsCache;
import com.aricneto.twistytimer.utils.CountdownWarning;
import com.aricneto.twistytimer.utils.DefaultPrefs;
import com.aricneto.twistytimer.utils.Prefs;
import com.aricneto.twistytimer.utils.PuzzleUtils;
import com.aricneto.twistytimer.utils.ScrambleGenerator;
import com.aricneto.twistytimer.utils.ThemeUtils;
import com.skyfishjy.library.RippleBackground;

import java.util.Locale;

import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.FragmentManager;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

import static com.aricneto.twistytimer.stats.AverageCalculator.tr;
import static com.aricneto.twistytimer.utils.PuzzleUtils.FORMAT_DEFAULT;
import static com.aricneto.twistytimer.utils.PuzzleUtils.NO_PENALTY;
import static com.aricneto.twistytimer.utils.PuzzleUtils.PENALTY_DNF;
import static com.aricneto.twistytimer.utils.PuzzleUtils.PENALTY_PLUSTWO;
import static com.aricneto.twistytimer.utils.PuzzleUtils.TYPE_333;
import static com.aricneto.twistytimer.utils.PuzzleUtils.convertTimeToString;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_COMMENT_ADDED;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_GENERATE_SCRAMBLE;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_SCRAMBLE_MODIFIED;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_SCROLLED_PAGE;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_TIMER_STARTED;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_TIMER_STOPPED;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_TIMES_MODIFIED;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_TIME_ADDED;
import static com.aricneto.twistytimer.utils.TTIntent.ACTION_TOOLBAR_RESTORED;
import static com.aricneto.twistytimer.utils.TTIntent.BroadcastBuilder;
import static com.aricneto.twistytimer.utils.TTIntent.CATEGORY_TIME_DATA_CHANGES;
import static com.aricneto.twistytimer.utils.TTIntent.CATEGORY_UI_INTERACTIONS;
import static com.aricneto.twistytimer.utils.TTIntent.TTFragmentBroadcastReceiver;
import static com.aricneto.twistytimer.utils.TTIntent.broadcast;
import static com.aricneto.twistytimer.utils.TTIntent.registerReceiver;
import static com.aricneto.twistytimer.utils.TTIntent.unregisterReceiver;

public class                                                                                                                                                                               TimerFragment extends BaseFragment
        implements OnBackPressedInFragmentListener, StatisticsCache.StatisticsObserver {


    // Specifies the timer mode
    // i.e: Trainer mode generates only trainer scrambles, and changes the puzzle select spinner
    // Can be used for other features in the future
    public static final String TIMER_MODE_TIMER = "TIMER_MODE_TIMER";
    public static final String TIMER_MODE_TRAINER = "TIMER_MODE_TRAINER";

    /**
     * Flag to enable debug logging for this class.
     */
    private static final boolean DEBUG_ME = false;

    /**
     * A "tag" to identify this class in log messages.
     */
    private static final String TAG = TimerFragment.class.getSimpleName();

    private static final String PUZZLE         = "puzzle";
    private static final String PUZZLE_SUBTYPE = "puzzle_type";
    private static final String TRAINER_SUBSET = "trainer_subset";
    private static final String TIMER_MODE = "timer_mode";
    private static final String SCRAMBLE = "scramble";
    private static final String HAS_STOPPED_TIMER_ONCE = "has_stopped_timer_once";


    /**
     * The time delay in milliseconds before starting the chronometer if the hold-for-start
     * preference is set.
     */
    private static final long HOLD_FOR_START_DELAY = 500L;

    private String currentPuzzle;
    private String currentPuzzleCategory;
    private TrainerScrambler.TrainerSubset currentSubset;

    private String currentScramble = "";
    private Solve  currentSolve    = null;

    private String realScramble = null;

    CountDownTimer countdown;
    boolean countingDown = false;

    CountdownWarning firstWarning;
    CountdownWarning secondWarning;

    private Context mContext;

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
    private GetOptimalCross optimalCrossAsync;

    private int currentPenalty = NO_PENALTY;

    /**
     * Specifies the current TimerMode
     */
    private String currentTimerMode;

    private int      mShortAnimationDuration;
    private Animator mCurrentAnimator;

    private Unbinder mUnbinder;

    // Holds the localized strings related to each detail statistic, in order:
    // Ao5, Ao12, Ao50, Ao100, Deviation, Best, Mean, Count
    private String detailTextNamesArray[] = new String[8];

    @BindView(R.id.sessionDetailTextAverage) TextView detailTextAvg;

    @BindView(R.id.sessionDetailTextOther) TextView detailTextOther;

    @BindView(R.id.detail_average_record_message) View detailAverageRecordMesssage;

    @BindView(R.id.chronometer)      ChronometerMilli    chronometer;
    @BindView(R.id.scramble_box)     CardView                scrambleBox;
    @BindView(R.id.scramble_text)
                                     AppCompatTextView   scrambleText;
    @BindView(R.id.scramble_img)     ImageView           scrambleImg;
    @BindView(R.id.expanded_image)   ImageView           expandedImageView;
    @BindView(R.id.inspection_text)  TextView            inspectionText;
    @BindView(R.id.progressSpinner)  MaterialProgressBar progressSpinner;
    @BindView(R.id.scramble_progress)MaterialProgressBar scrambleProgress;

    @BindView(R.id.scramble_button_hint)  AppCompatImageView  scrambleButtonHint;
    @BindView(R.id.scramble_button_reset) AppCompatImageView scrambleButtonReset;
    @BindView(R.id.scramble_button_edit)  AppCompatImageView scrambleButtonEdit;

    @BindView(R.id.qa_remove)        ImageView        deleteButton;
    @BindView(R.id.qa_dnf)           ImageView        dnfButton;
    @BindView(R.id.qa_plustwo)       ImageView        plusTwoButton;
    @BindView(R.id.qa_comment)       ImageView        commentButton;
    @BindView(R.id.qa_undo)          CardView        undoButton;
    @BindView(R.id.qa_layout) LinearLayout     quickActionButtons;
    @BindView(R.id.rippleBackground)     RippleBackground rippleBackground;

    @BindView(R.id.root)                  RelativeLayout       rootLayout;
    @BindView(R.id.startTimerLayout)      FrameLayout          startTimerLayout;

    @BindView(R.id.congratsText) TextView congratsText;

    private boolean buttonsEnabled;
    private boolean scrambleImgEnabled;
    private boolean sessionStatsEnabled;
    private boolean worstSolveEnabled;
    private boolean bestSolveEnabled;
    private boolean scrambleEnabled;
    private boolean scrambleBackgroundEnabled;
    private boolean holdEnabled;
    private boolean backCancelEnabled;
    private boolean startCueEnabled;
    private boolean showHintsEnabled;
    private boolean showHintsXCrossEnabled;
    private boolean averageRecordsEnabled;

    private boolean inspectionAlertEnabled;
    private boolean inspectionVibrationAlertEnabled;
    private boolean inspectionSoundAlertEnabled;

    float scrambleTextSize;

    // True if the user has started (and stopped) the timer at least once. Used to trigger
    // Average highlights, so the user doesn't get a notification when they start the app
    private boolean hasStoppedTimerOnce = false;

    /**
     * The most recently notified solve time statistics. When {@link #addNewSolve()} is called to
     * add a new time, the new time can be compared to these statistics to determine if the new
     * time sets a record.
     */
    private Statistics mRecentStatistics;

    // Receives broadcasts related to changes to the timer user interface.
    private final TTFragmentBroadcastReceiver mUIInteractionReceiver
            = new TTFragmentBroadcastReceiver(this, CATEGORY_UI_INTERACTIONS) {
        @Override
        public void onReceiveWhileAdded(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_SCROLLED_PAGE:
                    if (holdEnabled) {
                        holdHandler.removeCallbacks(holdRunnable);
                    }
                    chronometer.setHighlighted(false);
                    chronometer.cancelHoldForStart();
                    isReady = false;
                    break;

                case ACTION_TOOLBAR_RESTORED:
                    showItems();
                    animationDone = true;
                    // Wait for animations to run before broadcasting solve to avoid UI stuttering
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!isCanceled) {
                                // Only broadcast a new solve if it hasn't been canceled
                                broadcastNewSolve();
                            } else {
                                // The detail stats are triggered by a stats update.
                                // Since the solve has been canceled, there's no new stats
                                // to load, and it must be triggered manually
                                showDetailStats();
                            }

                            // reset isCanceled state
                            isCanceled = false;
                        }
                    }, 320);
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
    private BottomSheetDetailDialog scrambleDialog;
    private FragmentManager mFragManager;

    public TimerFragment() {
        // Required empty public constructor
    }

    private final View.OnClickListener buttonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            final DatabaseHandler dbHandler = TwistyTimer.getDBHandler();

            // On most of these changes to the current solve, the Statistics and ChartStatistics
            // need to be updated to reflect the change. It would probably be too complicated to
            // add facilities to "AverageCalculator" to handle modification of the last added time
            // or an "undo" facility and then to integrate that into the loaders. Therefore, a full
            // reload will probably be required.

            switch (view.getId()) {
                case R.id.qa_remove:
                    new MaterialDialog.Builder(getContext())
                            .content(R.string.delete_dialog_confirmation_title)
                            .positiveText(R.string.delete_dialog_confirmation_button)
                            .negativeText(R.string.delete_dialog_cancel_button)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog,
                                                    @NonNull DialogAction which) {
                                    if (currentSolve != null) { // FIXME: if solve is null, it should just hide the buttons
                                        dbHandler.deleteSolve(currentSolve);
                                        if (!isRunning)
                                            chronometer.reset(); // Reset to "0.00".
                                        congratsText.setVisibility(View.GONE);
                                        broadcast(CATEGORY_TIME_DATA_CHANGES, ACTION_TIMES_MODIFIED);
                                    }
                                    hideButtons(true, true);
                                }
                            })
                            .show();
                    break;
                case R.id.qa_dnf:
                    currentSolve = PuzzleUtils.applyPenalty(currentSolve, PENALTY_DNF);
                    chronometer.setPenalty(PuzzleUtils.PENALTY_DNF);
                    dbHandler.updateSolve(currentSolve);
                    hideButtons(true, false);
                    broadcast(CATEGORY_TIME_DATA_CHANGES, ACTION_TIMES_MODIFIED);
                    break;
                case R.id.qa_plustwo:
                    if (currentPenalty != PENALTY_PLUSTWO) {
                        currentSolve = PuzzleUtils.applyPenalty(currentSolve, PENALTY_PLUSTWO);
                        chronometer.setPenalty(PuzzleUtils.PENALTY_PLUSTWO);
                        dbHandler.updateSolve(currentSolve);
                        broadcast(CATEGORY_TIME_DATA_CHANGES, ACTION_TIMES_MODIFIED);
                    }
                    hideButtons(true, false);
                    break;
                case R.id.qa_comment:
                    MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                            .title(R.string.add_comment)
                            .input("", "", new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(@NonNull MaterialDialog dialog,
                                                    CharSequence input) {
                                    currentSolve.setComment(input.toString());
                                    dbHandler.updateSolve(currentSolve);

                                    broadcast(CATEGORY_TIME_DATA_CHANGES, ACTION_COMMENT_ADDED);
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
                case R.id.qa_undo:
                    // Undo the setting of a DNF or +2 penalty (does not undo a delete or comment).
                    currentSolve = PuzzleUtils.applyPenalty(currentSolve, NO_PENALTY);
                    chronometer.setPenalty(PuzzleUtils.NO_PENALTY);
                    dbHandler.updateSolve(currentSolve);
                    hideButtons(false, true);
                    broadcast(CATEGORY_TIME_DATA_CHANGES, ACTION_TIMES_MODIFIED);
                    break;
                case R.id.scramble_button_reset:
                    broadcast(CATEGORY_UI_INTERACTIONS, ACTION_GENERATE_SCRAMBLE);
                    break;
                case R.id.scramble_button_edit:
                    MaterialDialog editScrambleDialog = new MaterialDialog.Builder(getContext())
                            .title(R.string.edit_scramble)
                            .input("", "", new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(@NonNull MaterialDialog dialog,
                                                    CharSequence input) {

                                    setScramble(input.toString());

                                    // The hint solver will crash if you give it invalid scrambles,
                                    // so we shouldn't calculate hints for custom scrambles.
                                    // TODO: We can use the scramble image generator (which has a scramble validity checker) to check a scramble before calling a hint
                                    canShowHint = false;
                                    hideButtons(true, true);
                                }
                            })
                            .inputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE)
                            .positiveText(R.string.action_done)
                            .negativeText(R.string.action_cancel)
                            .build();
                    EditText scrambleEditText = editScrambleDialog.getInputEditText();
                    if (scrambleEditText != null) {
                        scrambleEditText.setLines(3);
                        scrambleEditText.setSingleLine(false);
                        scrambleEditText.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
                        scrambleEditText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
                    }
                    editScrambleDialog.show();
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

    public static TimerFragment newInstance(String puzzle, String puzzleSubType, String timerMode, TrainerScrambler.TrainerSubset subset) {
        TimerFragment fragment = new TimerFragment();
        Bundle args = new Bundle();
        args.putString(PUZZLE, puzzle);
        args.putString(PUZZLE_SUBTYPE, puzzleSubType);
        args.putString(TIMER_MODE, timerMode);
        args.putSerializable(TRAINER_SUBSET, subset);
        fragment.setArguments(args);
        if (DEBUG_ME) Log.d(TAG, "newInstance() -> " + fragment);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (DEBUG_ME) Log.d(TAG, "updateLocale(savedInstanceState=" + savedInstanceState + ")");
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentPuzzle = getArguments().getString(PUZZLE);
            currentPuzzleCategory = getArguments().getString(PUZZLE_SUBTYPE);
            currentSubset = (TrainerScrambler.TrainerSubset) getArguments().getSerializable(TRAINER_SUBSET);
            currentTimerMode = getArguments().getString(TIMER_MODE);
        }

        if (savedInstanceState != null) {
            if (savedInstanceState.getString(PUZZLE) == getArguments().get(PUZZLE)) {
                realScramble = savedInstanceState.getString(SCRAMBLE);
            }
            //hasStoppedTimerOnce = savedInstanceState.getBoolean(HAS_STOPPED_TIMER_ONCE, false);
        }

        detailTextNamesArray = getResources().getStringArray(R.array.timer_detail_stats);

        scrambleGeneratorAsync = new GenerateScrambleSequence();
        optimalCrossAsync = new GetOptimalCross();

        mFragManager = getFragmentManager();

        generator = new ScrambleGenerator(currentPuzzle);
        // Register a receiver to update if something has changed
        registerReceiver(mUIInteractionReceiver);
    }

    @SuppressLint({"ClickableViewAccessibility", "RestrictedApi"})
    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        if (DEBUG_ME) Log.d(TAG, "onCreateView(savedInstanceState=" + savedInstanceState + ")");

        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_timer, container, false);
        mUnbinder = ButterKnife.bind(this, root);

        mContext = getContext();

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
        undoButton.setOnClickListener(buttonClickListener);
        scrambleButtonReset.setOnClickListener(buttonClickListener);
        scrambleButtonEdit.setOnClickListener(buttonClickListener);

        // Preferences //
        final boolean inspectionEnabled = Prefs.getBoolean(R.string.pk_inspection_enabled, false);
        final int inspectionTime = Prefs.getInt(R.string.pk_inspection_time, 15);
        final float timerTextSize = Prefs.getInt(R.string.pk_timer_text_size, 100) / 100f;
        float scrambleImageSize = Prefs.getInt(R.string.pk_scramble_image_size, 100) / 100f;
        scrambleTextSize = Prefs.getInt(R.string.pk_scramble_text_size, 100) / 100f;
        final boolean advancedEnabled
                = Prefs.getBoolean(R.string.pk_advanced_timer_settings_enabled, false);

        /*
        *  Scramble text size preference. It doesn't need to be in the "advanced" settings since
        *  it detects if it's clipping and automatically compensates for that by creating a button.
        */
        scrambleText.setTextSize(TypedValue.COMPLEX_UNIT_PX, scrambleText.getTextSize() * scrambleTextSize);

        if (advancedEnabled) {
            chronometer.setAutoSizeTextTypeUniformWithConfiguration(
                    (int) (90 * timerTextSize) / 2,
                    (int) (90 * timerTextSize),
                    2,
                    TypedValue.COMPLEX_UNIT_SP);

            scrambleImg.getLayoutParams().width *= scrambleImageSize;
            scrambleImg.getLayoutParams().height *= calculateScrambleImageHeightMultiplier(scrambleImageSize);
        } else {
            //scrambleImg.getLayoutParams().height *= calculateScrambleImageHeightMultiplier(1);
        }

        Resources res = getResources();

        averageRecordsEnabled = Prefs.getBoolean(R.string.pk_show_average_record_enabled,
                DefaultPrefs.getBoolean(R.bool.default_showAverageRecordEnabled));

        backCancelEnabled = Prefs.getBoolean(R.string.pk_back_button_cancel_solve_enabled, res.getBoolean(R.bool.default_backCancelEnabled));

        buttonsEnabled = Prefs.getBoolean(R.string.pk_show_quick_actions, res.getBoolean(R.bool.default_buttonEnabled));
        holdEnabled = Prefs.getBoolean(R.string.pk_hold_to_start_enabled, res.getBoolean(R.bool.default_holdEnabled));
        startCueEnabled = Prefs.getBoolean(R.string.pk_start_cue_enabled, res.getBoolean(R.bool.default_startCue));

        sessionStatsEnabled = Prefs.getBoolean(R.string.pk_show_session_stats, true);
        bestSolveEnabled = Prefs.getBoolean(R.string.pk_show_best_time, true);
        worstSolveEnabled = Prefs.getBoolean(R.string.pk_show_worst_time, false);

        scrambleEnabled = Prefs.getBoolean(R.string.pk_scramble_enabled, true);
        scrambleImgEnabled = Prefs.getBoolean(R.string.pk_show_scramble_image, true);
        showHintsEnabled = Prefs.getBoolean(R.string.pk_show_scramble_hints, true);
        showHintsXCrossEnabled = Prefs.getBoolean(R.string.pk_show_scramble_x_cross_hints, false);

        scrambleBackgroundEnabled = Prefs.getBoolean(R.string.pk_show_scramble_background, false);

        inspectionAlertEnabled = Prefs.getBoolean(R.string.pk_inspection_alert_enabled, false);
        final String vibrationAlert = getString(R.string.pk_inspection_alert_vibration);
        final String soundAlert = getString(R.string.pk_inspection_alert_sound);
        if (inspectionAlertEnabled) {
            String inspectionAlertType = Prefs.getString(R.string.pk_inspection_alert_type,
                    getString(R.string.pk_inspection_alert_vibration));
            if (inspectionAlertType.equals(vibrationAlert)) {
                inspectionVibrationAlertEnabled = true;
                inspectionSoundAlertEnabled = false;
            } else if (inspectionAlertType.equals(soundAlert)) {
                inspectionVibrationAlertEnabled = false;
                inspectionSoundAlertEnabled = true;
            } else {
                inspectionVibrationAlertEnabled = true;
                inspectionSoundAlertEnabled = true;
            }
        }

        if (!scrambleBackgroundEnabled) {
            scrambleBox.setBackgroundColor(Color.TRANSPARENT);
            scrambleBox.setCardElevation(0);
            scrambleText.setTextColor(ThemeUtils.fetchAttrColor(mContext, R.attr.colorTimerText));
            scrambleButtonEdit.setColorFilter(ThemeUtils.fetchAttrColor(mContext, R.attr.colorTimerText));
            scrambleButtonReset.setColorFilter(ThemeUtils.fetchAttrColor(mContext, R.attr.colorTimerText));
            scrambleButtonHint.setColorFilter(ThemeUtils.fetchAttrColor(mContext, R.attr.colorTimerText));
        }

        if (showHintsEnabled && currentPuzzle.equals(PuzzleUtils.TYPE_333) && scrambleEnabled) {
            scrambleButtonHint.setVisibility(View.VISIBLE);
            optimalCross = new RubiksCubeOptimalCross(getString(R.string.optimal_cross));
            optimalXCross = new RubiksCubeOptimalXCross(getString(R.string.optimal_x_cross));
        }

        if (scrambleEnabled) {
            if (realScramble == null) {
                generateNewScramble();
            } else {
                setScramble(realScramble);
            }
        } else {
            scrambleBox.setVisibility(View.GONE);
            scrambleImg.setVisibility(View.GONE);
            isLocked = false;
        }

        if (! scrambleImgEnabled)
            scrambleImg.setVisibility(View.GONE);
        if (! sessionStatsEnabled) {
            detailTextAvg.setVisibility(View.INVISIBLE);
            detailTextOther.setVisibility(View.INVISIBLE);
        }
        // Preferences //

        // Inspection timer
        if (inspectionEnabled) {
            if (inspectionAlertEnabled) {
                // If inspection time is 15 (the official WCA default), first warning should be
                // at 8 seconds in. Else, warn when half the time is up (8 is about 50% of 15)
                firstWarning = new CountdownWarning
                        .Builder(inspectionTime == 15 ? 8 : (int) (inspectionTime * 0.5f))
                        .withVibrate(inspectionVibrationAlertEnabled)
                        .withTone(inspectionSoundAlertEnabled)
                        .toneCode(ToneGenerator.TONE_CDMA_NETWORK_BUSY_ONE_SHOT)
                        .toneDuration(400)
                        .vibrateDuration(300)
                        .build();
                // If inspection time is default, warn at 12 seconds per competition rules, else,
                // warn at when 80% of the time is up (12 is 80% of 15)
                secondWarning = new CountdownWarning
                        .Builder(inspectionTime == 15 ? 12 : (int) (inspectionTime * 0.8f))
                        .withVibrate(inspectionVibrationAlertEnabled)
                        .withTone(inspectionSoundAlertEnabled)
                        .toneCode(ToneGenerator.TONE_CDMA_NETWORK_BUSY)
                        .toneDuration(800)
                        .vibrateDuration(600)
                        .build();
            }
            countdown = new CountDownTimer(inspectionTime * 1000, 500) {
                @Override
                public void onTick(long l) {
                    if (chronometer != null)
                        chronometer.setText(String.valueOf((l / 1000) + 1));
                }

                @Override
                public void onFinish() {
                    if (chronometer != null) {
                        chronometer.setText("+2");
                        // "+2" penalty is applied to "chronometer" when timer is eventually stopped.
                        currentPenalty = PuzzleUtils.PENALTY_PLUSTWO;
                        plusTwoCountdown.start();
                    }
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

                if (!animationDone || isLocked && !isRunning) {
                    // Not ready to start the timer, yet. May be waiting on the animation of the
                    // restoration of the tool-bars after the timer was stopped, or waiting on the
                    // generation of a scramble ("isLocked" flag).
                    // To compensate for long generating times, the timer generates a scramble
                    // while it is counting down. In this case, it's necessary to check if the timer
                    // is running, so the user can stop the it.
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
                            // which would interfere with the continuing countdown of the
                            // inspection
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

                            if (holdingDNF) {
                                // Checks if the user was holding the screen when the inspection
                                // timed out and saved a DNF
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
                        // If a user has inspection on and went past his inspection time, he has
                        // two extra seconds do start his time, but with a +2 penalty. This penalty
                        // is recorded above (see plusTwoCountdown), and the timer checks if it's true here.
                        chronometer.setPenalty(PuzzleUtils.PENALTY_PLUSTWO);
                    }
                    addNewSolve();
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
    }

    /**
     * Stops the chronometer on back press.
     *
     * @return
     *     {@code true} if the "Back" button press was consumed to hide the scramble or stop the
     *     timer; or {@code false} if neither was necessary and the "Back" button press was ignored.
     */
    @Override
    public boolean onBackPressedInFragment() {
        if (DEBUG_ME) Log.d(TAG, "onBackPressedInFragment()");

        if (isResumed()) {
            if (isRunning || countingDown) {
                cancelChronometer();
                return true;
            }
        }
        return false;
    }

    /**
     * Stops the inspection period countdown, and its warnings (if it is active). This cancels the
     * inspection countdown timer and associated "+2" countdown timer and hides the inspection text.
     */
    private void stopInspectionCountdown() {
        // These timers may be null if inspection was not enabled when "updateLocale" was called.
        if (countdown != null) {
            countdown.cancel();
        }

        if (plusTwoCountdown != null) {
            plusTwoCountdown.cancel();
        }

        if (firstWarning != null) {
            firstWarning.cancel();
        }

        if (secondWarning != null) {
            secondWarning.cancel();
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
        // The "countdown" timer may be null if inspection was not enabled when "updateLocale" was
        // called. In that case this method will not be called from the touch listener.

        // So it doesn't flash the old time when the inspection starts
        chronometer.setText(String.valueOf(inspectionTime));
        inspectionText.setVisibility(View.VISIBLE);
        countdown.start();
        if (firstWarning != null) {
            firstWarning.start();
        }
        if (secondWarning != null) {
            secondWarning.start();
        }
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
                currentPuzzle, currentPuzzleCategory,
                System.currentTimeMillis(), currentScramble, currentPenalty, "", false);

        if (currentPenalty != PENALTY_DNF) {
            declareRecordTimes(currentSolve);
        }

        currentSolve.setId(TwistyTimer.getDBHandler().addSolve(currentSolve));
        currentPenalty = NO_PENALTY;
    }

    private void broadcastNewSolve() {
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
                        PuzzleUtils.convertTimeToString(previousBestTime - newTime, PuzzleUtils.FORMAT_DEFAULT)));
                congratsText.setVisibility(View.VISIBLE);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (rippleBackground != null)
                            rippleBackground.stopRippleAnimation();
                    }
                }, 2900);
            }
        }

        if (worstSolveEnabled) {
            final long previousWorstTime = mRecentStatistics.getAllTimeWorstTime();

            // If "previousWorstTime" is a DNF or UNKNOWN, it will be less than zero. Therefore,
            // make sure it is at least greater than zero before testing against the new time.
            if (previousWorstTime > 0 && newTime > previousWorstTime) {
                congratsText.setText(getString(R.string.personal_worst_message,
                        PuzzleUtils.convertTimeToString(newTime - previousWorstTime, PuzzleUtils.FORMAT_DEFAULT)));

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

        if (getView() == null || !sessionStatsEnabled) {
            // Must have arrived after "onDestroyView" was called, so do nothing.
            return;
        }

        // Save these for later. The best and worst times can be retrieved and compared to the next
        // new solve time to be added via "addNewSolve".
        mRecentStatistics = stats; // May be null.

        if (stats == null) {
            return;
        }

        String sessionDeviation = convertTimeToString(tr(stats.getSessionStdDeviation()), PuzzleUtils
                .FORMAT_DEFAULT);
        String sessionCount = String.format(Locale.getDefault(), "%,d", stats.getSessionNumSolves());
        String sessionBestTime = convertTimeToString(tr(stats.getSessionBestTime()), PuzzleUtils.FORMAT_DEFAULT);
        String sessionMean = convertTimeToString(tr(stats.getSessionMeanTime()), PuzzleUtils.FORMAT_DEFAULT);

        long allTimeBestAvg[] = new long[4];
        long sessionCurrentAvg[] = new long[4];

        allTimeBestAvg[0] = tr(stats.getAverageOf(5, false).getBestAverage());
        allTimeBestAvg[1] = tr(stats.getAverageOf(12, false).getBestAverage());
        allTimeBestAvg[2] = tr(stats.getAverageOf(50, false).getBestAverage());
        allTimeBestAvg[3] = tr(stats.getAverageOf(100, false).getBestAverage());

        sessionCurrentAvg[0] = tr(stats.getAverageOf(5, true).getCurrentAverage());
        sessionCurrentAvg[1] = tr(stats.getAverageOf(12, true).getCurrentAverage());
        sessionCurrentAvg[2] = tr(stats.getAverageOf(50, true).getCurrentAverage());
        sessionCurrentAvg[3] = tr(stats.getAverageOf(100, true).getCurrentAverage());

        // detailTextNamesArray should be in the same order as shown in the timer
        // (keep R.arrays.timer_detail_stats in sync with the order!)
        StringBuilder stringDetailOther = new StringBuilder();
        stringDetailOther.append(detailTextNamesArray[4]).append(": ").append(sessionDeviation).append("\n");
        stringDetailOther.append(detailTextNamesArray[6]).append(": ").append(sessionMean).append("\n");
        stringDetailOther.append(detailTextNamesArray[5]).append(": ").append(sessionBestTime).append("\n");
        stringDetailOther.append(detailTextNamesArray[7]).append(": ").append(sessionCount);

        detailTextOther.setText(stringDetailOther.toString());

        // To prevent the record message being animated more than once in case the user sets
        // two or more average records at the same time.
        boolean hasShownRecordMessage = false;

        // reset card visibility
        detailAverageRecordMesssage.setVisibility(View.GONE);

        StringBuilder stringDetailAvg = new StringBuilder();
        // Iterate through averages and set respective TextViews
        for (int i = 0; i < 4; i++) {
            if (sessionStatsEnabled && averageRecordsEnabled && hasStoppedTimerOnce &&
                    sessionCurrentAvg[i] > 0 && sessionCurrentAvg[i] <= allTimeBestAvg[i]) {
                // Create string.
                stringDetailAvg.append("<u><b>").append(detailTextNamesArray[i]).append(": ").append(convertTimeToString(sessionCurrentAvg[i], FORMAT_DEFAULT)).append("</b></u>");

                // Show record message, if it was not shown before
                if (!hasShownRecordMessage && !isRunning && !countingDown) {
                    detailAverageRecordMesssage.setVisibility(View.VISIBLE);
                    detailAverageRecordMesssage
                            .animate()
                            .alpha(1)
                            .setDuration(300);
                    hasShownRecordMessage = true;
                }
            } else if (sessionStatsEnabled) {
                stringDetailAvg.append(detailTextNamesArray[i]).append(": ").append(convertTimeToString(sessionCurrentAvg[i], FORMAT_DEFAULT));
            }
            // append newline to every line but the last
            if (i < 3) {
                stringDetailAvg.append("<br>");
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            detailTextAvg.setText(Html.fromHtml(stringDetailAvg.toString(), Html.FROM_HTML_MODE_LEGACY));
        } else {
            detailTextAvg.setText(Html.fromHtml(stringDetailAvg.toString()));
        }

        if (!isRunning && !countingDown)
            showDetailStats();

    }

    private void generateScrambleImage() {
        new GenerateScrambleImage().execute();
    }

    private void showToolbar() {
        unlockOrientation(getActivity());
        broadcast(CATEGORY_UI_INTERACTIONS, ACTION_TIMER_STOPPED);
    }

    private void showItems() {

        // reset chronometer position
        chronometer.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300);
        inspectionText.animate()
                .translationY(0)
                .setDuration(300);

        if (scrambleEnabled) {
            scrambleBox.setVisibility(View.VISIBLE);
            scrambleBox.animate()
                    .alpha(1)
                    .translationY(0)
                    .setDuration(300);
            scrambleBox.setEnabled(true);
            if (scrambleImgEnabled) {
                scrambleImg.setEnabled(true);
                showImage();
            }
        }
        if (buttonsEnabled && ! isCanceled) {
            quickActionButtons.setEnabled(true);
            quickActionButtons.setVisibility(View.VISIBLE);
            quickActionButtons.animate()
                    .alpha(1)
                    .setDuration(300);
        }
    }

    private void showDetailStats() {
        detailTextAvg.setVisibility(View.VISIBLE);
        detailTextAvg.animate()
                .alpha(1)
                .translationY(0)
                .setDuration(300);

        detailTextOther.setVisibility(View.VISIBLE);
        detailTextOther.animate()
                .alpha(1)
                .translationY(0)
                .setDuration(300);
    }

    private void showImage() {
        scrambleImg.setVisibility(View.VISIBLE);
        scrambleImg.setEnabled(true);
        scrambleImg.animate()
                .alpha(1)
                .translationY(0)
                .setDuration(300);
    }

    private void hideImage() {
        scrambleImg.animate()
                .alpha(0)
                .translationY(scrambleImg.getHeight())
                .setDuration(300)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if (scrambleImg != null) {
                            scrambleImg.setVisibility(View.GONE);
                            scrambleImg.setEnabled(false);
                        }
                    }
                });
    }

    private void hideToolbar() {
        lockOrientation(getActivity());
        broadcast(CATEGORY_UI_INTERACTIONS, ACTION_TIMER_STARTED);

        congratsText.setVisibility(View.GONE);
        congratsText.setCompoundDrawables(null, null, null, null);

        // bring chronometer up a bit
        chronometer.animate()
                .scaleX(1.15f)
                .scaleY(1.15f)
                .setDuration(300);
        inspectionText.animate()
                .translationY(-getActionBarSize())
                .setDuration(300);

        if (scrambleEnabled) {
            scrambleBox.setEnabled(false);
            scrambleBox.animate()
                    .alpha(0)
                    .translationY(-scrambleBox.getHeight())
                    .setDuration(300)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            scrambleBox.setVisibility(View.INVISIBLE);
                        }
                    });
            if (scrambleImgEnabled) {
                scrambleImg.setEnabled(false);
                hideImage();
            }
        }
        if (sessionStatsEnabled) {
            detailTextAvg.animate()
                    .alpha(0)
                    .translationY(detailTextAvg.getHeight())
                    .setDuration(300)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            detailTextAvg.setVisibility(View.INVISIBLE);
                        }
                    });
            detailTextOther.animate()
                    .alpha(0)
                    .translationY(detailTextOther.getHeight())
                    .setDuration(300)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            detailTextOther.setVisibility(View.INVISIBLE);
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
        if (averageRecordsEnabled) {
            detailAverageRecordMesssage
                    .animate()
                    .alpha(0)
                    .setDuration(300)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            detailAverageRecordMesssage.setVisibility(View.GONE);
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

        // isRunning should be set before generateNewScramble so the loading spinner doesn't appear
        // during a solve, since generateNewScramble checks if isRunning is false before setting
        // the spinner to visible.
        isRunning = true;

        if (scrambleEnabled) {
            currentScramble = realScramble;
            generateNewScramble();
        }
    }

    /**
     * Stops the chronometer
     */
    private void stopChronometer() {
        chronometer.stop();
        chronometer.setHighlighted(false);
        isRunning = false;
        hasStoppedTimerOnce = true;
        showToolbar();
    }

    /**
     * Cancels the chronometer and any inspection countdown. Nothing is saved and the timer is
     * reset to zero.
     */
    public void cancelChronometer() {
        if (backCancelEnabled) {
            stopInspectionCountdown();
            stopChronometer();

            chronometer.reset(); // Show "0.00".
            isCanceled = true;
            currentPenalty = NO_PENALTY;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SCRAMBLE, realScramble);
        outState.putString(PUZZLE, currentPuzzle);
        outState.putBoolean(HAS_STOPPED_TIMER_ONCE, hasStoppedTimerOnce);
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
    public void onDestroy() {
        if (DEBUG_ME) Log.d(TAG, "onDestroy()");
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        if (DEBUG_ME) Log.d(TAG, "onDestroyView()");
        super.onDestroyView();
        mUnbinder.unbind();
        StatisticsCache.getInstance().unregisterObserver(this);
        mRecentStatistics = null;
    }

    public static void lockOrientation(Activity activity) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        } else {
            Display display         = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            int     rotation        = display.getRotation();
            int     tempOrientation = activity.getResources().getConfiguration().orientation;
            int     orientation     = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

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
    }

    private static void unlockOrientation(Activity activity) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    private void getNewOptimalCross() {
        if (showHintsEnabled) {
            optimalCrossAsync.cancel(true);
            optimalCrossAsync = new GetOptimalCross();
            optimalCrossAsync.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    /**
     * Generates a new scramble and handles everything.
     */
    public void generateNewScramble() {
        if (scrambleEnabled && currentTimerMode.equals(TIMER_MODE_TIMER)) {
            scrambleGeneratorAsync.cancel(true);
            scrambleGeneratorAsync = new GenerateScrambleSequence();
            scrambleGeneratorAsync.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else if (currentTimerMode.equals(TIMER_MODE_TRAINER)) {
            setScramble(TrainerScrambler.generateTrainerCase(getContext(), currentSubset, currentPuzzleCategory));
            canShowHint = false;
            hideButtons(true, true);
        }
    }


    private class GetOptimalCross extends AsyncTask<Void, Void, String> {
        @Override
        protected void onPreExecute() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_MORE_FAVORABLE);
            Log.d("OptimalCross onPre:",System.currentTimeMillis()+"");
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... voids) {
            String text = "";
            text += optimalCross.getTip(realScramble);
            if (showHintsXCrossEnabled) {
                text += "\n\n";
                text += optimalXCross.getTip(realScramble);
            }
            return text;
        }

        @Override
        protected void onPostExecute(String text) {
            super.onPostExecute(text);
            if (!isRunning) {
                // Set the hint text
                if(scrambleDialog != null) {
                    scrambleDialog.setHintText(text);
                    scrambleDialog.setHintVisibility(View.VISIBLE);
                }
            }
        }
    }

    private class GenerateScrambleSequence extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_MORE_FAVORABLE);
            if (showHintsEnabled && currentPuzzle.equals(PuzzleUtils.TYPE_333) && scrambleEnabled && scrambleDialog != null) {
                scrambleDialog.setHintVisibility(View.GONE);
                scrambleDialog.dismiss();
            }
            canShowHint = false;
            scrambleText.setText(R.string.generating_scramble);
            scrambleText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            scrambleText.setClickable(false);

            scrambleButtonHint.setVisibility(View.GONE);
            scrambleButtonEdit.setVisibility(View.GONE);
            scrambleButtonReset.setVisibility(View.GONE);
            scrambleProgress.setVisibility(View.VISIBLE);

            hideImage();
            if (! isRunning)
                progressSpinner.setVisibility(View.VISIBLE);
            isLocked = true;
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                return generator.getPuzzle().generateScramble();
            } catch (Exception e) {
                Log.e(TAG, "Invalid puzzle for generator");
            }
            return "An error has ocurred";
        }

        @Override
        protected void onProgressUpdate(Void... values) {

        }

        @Override
        protected void onPostExecute(String scramble) {
            setScramble(scramble);
        }
    }

    /**
     * Updates everything related to displaying the current scramble
     * Ex. scramble image, box, text, dialogs
     */
    private void setScramble(final String scramble) {
        realScramble = scramble;
        scrambleText.setText(scramble);
        scrambleText.post(new Runnable() {
            @Override
            public void run() {
                if (scrambleText != null) {
                    // Calculate surrounding layouts to make sure the scramble text doesn't intersect any element
                    // If it does, show only a "tap here to see more" hint instead of the scramble
                    chronometer.post(() -> {
                        Rect scrambleRect = new Rect(scrambleBox.getLeft(), scrambleBox.getTop(), scrambleBox.getRight(), scrambleBox.getBottom());
                        Rect chronometerRect = new Rect(chronometer.getLeft(),
                                                        (int) (chronometer.getTop() + chronometer.getBaseline() - chronometer.getTextSize()),
                                                        chronometer.getRight(),
                                                        chronometer.getBottom());
                        Log.d(TAG, "baseline: " + chronometer.getTop() + " | textSize: " + chronometer.getBottom());
                        Rect congratsRect = new Rect(congratsText.getLeft(), congratsText.getTop(), congratsText.getRight(), congratsText.getBottom());

                        if ((Rect.intersects(scrambleRect, chronometerRect)) ||
                            (congratsText.getVisibility() == View.VISIBLE && Rect.intersects(scrambleRect, congratsRect))) {
                            scrambleText.setText("[ " + getString(R.string.scramble_text_tap_hint) + " ]");
                            scrambleBox.setClickable(true);
                            scrambleBox.setOnClickListener(scrambleDetailClickListener);
                        } else {
                            scrambleBox.setOnClickListener(null);
                            scrambleBox.setClickable(false);
                            scrambleBox.setFocusable(false);
                        }
                        scrambleButtonHint.setOnClickListener(scrambleDetailClickListener);
                    });
                }
            }
        });

        if (showHintsEnabled && currentPuzzle.equals(PuzzleUtils.TYPE_333))
            scrambleButtonHint.setVisibility(View.VISIBLE);
        scrambleProgress.setVisibility(View.GONE);
        scrambleButtonEdit.setVisibility(View.VISIBLE);
        scrambleButtonReset.setVisibility(View.VISIBLE);

        if (scrambleImgEnabled)
            generateScrambleImage();
        else
            progressSpinner.setVisibility(View.INVISIBLE);
        isLocked = false;

        if (showHintsEnabled)
            canShowHint = true;

        // Broadcast the new scramble
        new BroadcastBuilder(CATEGORY_UI_INTERACTIONS, ACTION_SCRAMBLE_MODIFIED)
                .scramble(realScramble)
                .broadcast();
    }

    View.OnClickListener scrambleDetailClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            scrambleDialog = new BottomSheetDetailDialog();
            scrambleDialog.setDetailText(realScramble);
            scrambleDialog.setDetailTextSize(scrambleTextSize);
            if (canShowHint && showHintsEnabled && currentPuzzle.equals(TYPE_333)) {
                getNewOptimalCross();
                scrambleDialog.hasHints(true);
            }
            if (mFragManager != null)
                scrambleDialog.show(mFragManager, "fragment_dialog_scramble_detail");
        }
    };

    private class GenerateScrambleImage extends AsyncTask<Void, Void, Drawable> {

        @Override
        protected Drawable doInBackground(Void... voids) {
            return generator.generateImageFromScramble(
                    PreferenceManager.getDefaultSharedPreferences(TwistyTimer.getAppContext()),
                    realScramble);
        }

        @Override
        protected void onPostExecute(Drawable drawable) {
            super.onPostExecute(drawable);
            if (! isRunning) {
                if (scrambleImg != null)
                    showImage();
            }
            if (progressSpinner != null)
                progressSpinner.setVisibility(View.INVISIBLE);
            if (scrambleImg != null)
                scrambleImg.setImageDrawable(drawable);
            if (expandedImageView != null)
                expandedImageView.setImageDrawable(drawable);
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
        globalOffset.y -= scrambleBox.getHeight();
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
