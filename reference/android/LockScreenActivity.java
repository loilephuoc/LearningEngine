package com.OPD2nd.popup;

import static com.google.android.material.internal.ViewUtils.showKeyboard;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import android.view.ViewGroup;
import com.OPD2nd.popup.RandomHelper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.vectordrawable.graphics.drawable.ArgbEvaluator;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import android.widget.LinearLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Space;

import android.view.ViewParent;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.view.animation.LinearInterpolator;

import android.view.PointerIcon;


import android.text.StaticLayout;
import android.text.TextPaint;




public class LockScreenActivity extends AppCompatActivity {
    private List<Sentence> sentences = new ArrayList<>();  // theo topic hiện tại
    private List<Sentence> globalSentences = new ArrayList<>();    // toàn bộ data đã import
    private Sentence currentSentence = null;
    private MediaPlayer currentMediaPlayer = null;
    private Handler handler = new Handler(Looper.getMainLooper());
    private List<String> shownHistoryKeys = new ArrayList<>();
    private TopicInfo currentTopicOnLockscreen;
    private AlertDialog currentExampleDialog;
    private boolean isDraggingFab = false;

    private static final String PREFS_AUDIO_LOOP = "audio_loop_prefs";
    private static final String KEY_LOOP_DELAY_MS = "loop_delay_ms";

    private static final String KEY_GLOBAL_AUDIO_DELAY_SECONDS = "delay";

    private LinearLayout srsButtonsContainer;
    private TextView tvVietnameseTop;
    private TextView tvVietnamese;
    private TextView tvEnglish;
    private TextView ipaText;

    private MaterialButton btnPrevLesson;
    private MaterialButton btnNextLesson;

    private TextView tvNowPlaying;


    private static final String KEY_LOCK_CURRENT_LABEL = "lock_current_selection_label";
    private static final String KEY_LOCK_CURRENT_LESSON_KEYS = "lock_current_lesson_keys";
    private static final String KEY_LOCK_CURRENT_GROUP = "lock_current_group";
    private static final String KEY_LOCK_CURRENT_SECTION = "lock_current_section";


    private TextView tabDisplaySettings, tabAudioSettings, tabControlSettings;
    private LinearLayout panelDisplaySettings, panelAudioSettings, panelControlSettings;


    private boolean lessonAutoLoopEnabled = false;
    private boolean lessonAutoLoopRunning = false;
    private int lessonAutoLoopIndex = 0;

    private boolean dpadUpDownLongPressed = false;

    private boolean isApplyingLessonAutoLoopState = false;

    private boolean hasShownAnswerForCurrentSentence = false;
    private GestureDetector cardGestureDetector;  // ⭐ Vuốt dọc trên card

    private GestureDetector exampleGestureDetector;

    // ⭐ Đánh dấu đã từng play example audio cho câu hiện tại
    private boolean hasPlayedExampleForCurrentSentence = false;

    private SwitchMaterial switchAutoMode;

    // App settings keys
    public static final String KEY_WAIT_EN_BEFORE_NEXT = "wait_en_before_next";


    // Số lần single-tap lên image ở câu hiện tại (khi chưa show đáp án)
    private int imageSingleTapCountForCurrentSentence = 0;

    private boolean isTouchLocked = false;
    private FloatingActionButton lockTouchBubble;
    private GestureDetector lockBubbleDetector;
    private SwitchMaterial switchLockTouch;

    private CompoundButton.OnCheckedChangeListener lockTouchCheckedChangeListener;

    private boolean tempPlayMaleOnce = false;
    private static final int TAG_IMAGE_UNIQUE_KEY = R.id.imageView; // hoặc bất kỳ id resource nào

    private boolean isExampleLooping = false;

    private static final int REQ_TYPING = 1001;

    private ObjectAnimator englishGlowAnimator;

    private LinearLayout floatingNoteRoot;
    private SwitchMaterial switchPlayViOnNext;

    private boolean openedFromNotification = false;

    private SwitchMaterial switchAutoPlayOnNext;
    private boolean autoPlayOnNext;
    // ✅ THỐNG NHẤT TÊN PREFS VÀ KEY
    private static final String PREFS_APP_SETTINGS = "app_settings";
    private static final String PREFS_DAILY_COUNTERS = "daily_counters";

    private static final String KEY_QUOTA_NEW = "quota_new_cards";
    private static final String KEY_QUOTA_REVIEW = "quota_review_cards";
    private static final String KEY_NEW_COUNT = "new_count";
    private static final String KEY_REVIEW_COUNT = "review_count";
    private static final String KEY_COUNTER_DATE = "counter_date";
    private static final String KEY_PLAY_EN_AUDIO_ON_NEXT = "play_en_audio_on_next";
    // Khi true: lần KEYCODE_M tiếp theo sau tap image (full đáp án) sẽ bị chặn
    private boolean suppressNextMaleFromImageTap = false;

    private boolean isViAudioPlaying = false;
    // delay 2s chặn rating khi vừa next câu
    private long lastRatingHandledTime = 0L;
    private static final long MIN_RATING_INTERVAL_MS = 2000; // 2 giây

    // Đánh dấu đã show đáp án cho câu hiện tại chưa
    private boolean hasShownAnswer = false;
    // Thống kê
    private int totalAgain = 0;
    private int totalHard  = 0;
    private int totalGood  = 0;
    private int totalEasy  = 0;

    private Set<String> lockExpandedSectionKeys = new HashSet<>();
    private Set<String> lockCurrentLessonKeys = new HashSet<>();
    private String lockCurrentSelectionLabel = "";

    private static final int DEFAULT_LOCKSCREEN_AUTO_NEXT_SEC = 5; // mặc định 5 giây
    private int lockscreenAutoNextMs = DEFAULT_LOCKSCREEN_AUTO_NEXT_SEC * 1000;









    private static final String KEY_AUDIO_VI_VOLUME = "audio_vi_volume";
    private static final float DEFAULT_AUDIO_VI_VOLUME = 0.70f;

    private float audioViVolume = DEFAULT_AUDIO_VI_VOLUME;





    // ===== Current lesson selection =====
    private String lockCurrentGroupName = "";
    private String lockCurrentSectionName = "";

    private TextView tvTrackCounter;





    private boolean reviewLearnedMode = false;

    private static final String KEY_REVIEW_LEARNED_MODE = "review_learned_mode";

    private boolean isSentencesReceiverRegistered = false;
    private String lastLoadedLockTopicId = null;
    private String lastLoadedLockTopicFile = null;

    // Learning phase constants
    private static final int SWIPE_PHASE_NORMAL = 0;
    private static final int SWIPE_PHASE_AGAIN  = 1;
    private static final int SWIPE_PHASE_HARD   = 2;


    private static final long MINUTE = 60L * 1000L;
    private static final long HOUR   = 60L * MINUTE;
    private static final long DAY    = 24L * HOUR;

    private static final long[] AGAIN_STEPS_MS = {
            1 * MINUTE,
            2 * MINUTE,
            5 * MINUTE,
            10 * MINUTE,
            30 * MINUTE,
            60 * MINUTE,
            120 * MINUTE,
            240 * MINUTE,
            480 * MINUTE,
            960 * MINUTE,
            1 * DAY,
            2 * DAY,
            2 * DAY,
            3 * DAY
    };

    private static final long[] HARD_STEPS_MS = {
            5 * MINUTE,
            5 * MINUTE,
            10 * MINUTE,
            20 * MINUTE,
            60 * MINUTE,
            4 * HOUR,
            1 * DAY,
            2 * DAY,
            3 * DAY,
            4 * DAY,
            5 * DAY
    };

    // Danh sách new hôm nay (đã shuffle) + vị trí hiện tại
    private final List<Sentence> todayNewPool = new ArrayList<>();
    private int todayNewIndex = 0;


    private ObjectAnimator exampleLoopAnimator;

    private ObjectAnimator loopPulseAnimator;

    private SwitchMaterial switchShowVietnamese;
    private TextView viText;
    private static final String KEY_LOCKS_SHOW_VI = "locks_show_vi";

    private static final String KEY_SHOW_VOICE_LOOP = "show_voice_loop";

    private boolean isImageFocused = false;
    private boolean isUpdatingSuggestions = false;


    // double vuốt
    // Cho swipe xuống
    private int swipeDownCount = 0;
    private Runnable singleSwipeDownRunnable;

    // Cho swipe lên
    private int swipeUpCount = 0;
    private Runnable singleSwipeUpRunnable;

    private static final long DOUBLE_SWIPE_INTERVAL_MS = 300;
    private final Handler swipeHandler = new Handler(Looper.getMainLooper());

    //gesture riêng cho tap/double nút ô vuông nhỏ ở giữa imageview
    private GestureDetector centerTapDetector;
    private int centerTapCount = 0;
    private static final int CENTER_DOUBLE_TAP_INTERVAL_MS = 300;
    private Handler centerTapHandler = new Handler(Looper.getMainLooper());
    private Runnable centerSingleTapRunnable;



    // ⭐ BIẾN ĐẾM DOUBLE SWIPE CHO FEMALE/MALE
    private int swipeFemaleCount = 0;
    private int swipeMaleCount = 0;
    private Runnable singleSwipeFemaleRunnable;
    private Runnable singleSwipeMaleRunnable;






// double press cho nút chụp hình

    private static final long DOUBLE_PRESS_INTERVAL_MS = 250; // 200–300 tuỳ tay

    private long lastVolumeKeyTime = 0L;
    private int lastVolumeKeyCode = -1;

    // tự ẩn bong bóng Lock Touch
    private final Handler lockBubbleHandler = new Handler(Looper.getMainLooper());
    private Runnable hideLockBubbleRunnable;

    private GestureDetector globalLockGestureDetector;


    private static final String KEY_PLAY_VI_AUDIO = "play_vi_audio";

    private String currentAudioEnFile = null;
    private String currentAudioViFile = null;


    private static final String PREFS_APP_STATE = "srs_app_state";
    private static final String KEY_LAST_SHOWN_SENTENCE_ID = "last_shown_sentence_id";

    private boolean isSwitchingSentence = false;

    // ==== Fields cho nhóm switch expand/collapse ====
    private boolean switchPanelExpanded = false;
    private ScrollView switchPanel;
    private LinearLayout switchHeader;
    private ImageView ivSwitchArrow;
    private TextView tvSwitchHeaderSummary;


    // ⭐ true khi bạn đang dùng PC mode (ví dụ mở bằng Ctrl+N)
    private boolean isPcMode = false;



    private static final int AGAIN_LAPS_BEFORE_TYPING = 2;   // số lần Again trước khi bắt gõ
    private static final int HARD_LAPS_BEFORE_TYPING  = 2;   // số lần Hard trước khi bắt gõ

    private static final int TYPING_STAGE_NONE          = 0;
    private static final int TYPING_STAGE_AGAIN_TO_HARD = 1;
    private static final int TYPING_STAGE_HARD_TO_GOOD  = 2;


    // ⭐ Giới hạn số câu learning liên tiếp (Again/Hard) trong phiên
    private int consecutiveLearningShown = 0;
    private static final int MAX_CONSECUTIVE_LEARNING = 2;

    // ===== Floating Note Fields =====
    private View floatingNoteView;
    private EditText edtFloatingNote;
    private boolean isFloatingNoteVisible = false;

    private float noteLastX = -1f;
    private float noteLastY = -1f;
    private float noteDx, noteDy;

    // ===== LISTENING SEQUENTIAL MODE (nghe theo thứ tự, bỏ SRS) =====
    private static final String KEY_LISTENING_SEQUENTIAL_MODE = "listening_sequential_mode";

    private boolean listeningSequentialMode = false;
    private int listeningSequentialIndex = 0; // index câu trong danh sách chủ đề hiện tại

    // ⭐ THÊM VÀO ĐẦU CLASS (cùng chỗ với currentSentence, sentences, ...)
    private boolean isImageAnimating = false;

    private ImageView imgSentence;
    private int shownHistoryIdx = -1;

    private List<Sentence> historyCache = new ArrayList<>();
    private SwitchMaterial switchShowFavorites;
    private AutoCompleteTextView searchView;
    private ArrayAdapter<String> autoAdapter;
    private List<String> suggestionList = new ArrayList<>();
    private float yDown = 0;
    private final float SWIPE_THRESHOLD = 100; // tùy chỉnh độ dài vuốt (px)

    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    private GestureDetector swipeDetector;

    // Để phân biệt single/double press volume
    private static final long VOLUME_SINGLE_DELAY_MS = 220L; // hoặc cùng giá trị với DOUBLE_PRESS_INTERVAL_MS

    private Handler volumeHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingVolumeSingleRunnable = null;



    private boolean currentCardFromReviewLoop = false;

    // Trong class LockScreenActivity (field)
    private static final long EXAMPLE_DOUBLE_TAP_TIMEOUT = 250L; // ms
    private long lastExampleTapTime = 0L;
    private boolean pendingSingleExampleTap = false;



    private ActivityResultLauncher<String> audioPickerLauncher;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private static final int IMAGE_CORNER_RADIUS_DP = 6; // Đặt giá trị bo góc toàn cục
    private Uri selectedAudioUri;
    private String selectedAudioFileName;
    private TextView currentAudioTextView;
    private Uri selectedImageUri;
    private ImageView currentEditImageView;
    private AlertDialog editDialog;

    private int selectedTopicIndexInDialog = -1;

    private SwitchMaterial switchShowEnglish;
    private TextView enText;



    // Nút tắt Typing tốt nghiệp
    private static final String PREFS_NAME = "srs_prefs";
    private static final String KEY_ENABLE_TYPING_CHECK = "enable_typing_check";

    private SwitchCompat switchTypingCheck;


    private boolean enableTypingCheck = true;


    private SwitchMaterial switchWaitEnBeforeNext;




    private static final String KEY_PLAY_EN_MALE_ON_NEXT = "play_en_male_on_next";

    private SwitchMaterial switchPlayEnMaleOnNext;
    private boolean playEnMaleOnNext = false;




    // Idle screen-off
    private long inactivityTimeoutMs;          // đọc từ prefs "settings"
    private Handler inactivityHandler = new Handler();
    private Runnable inactivityRunnable;


    private TopicPackageManager topicPackageManager;



    private TextView tvExample;

    private ActivityResultLauncher<String> importLauncher;

    private boolean isAudioLooping = false;
    private Handler audioLoopHandler = new Handler(Looper.getMainLooper());
    private Runnable audioLoopRunnable;

    // --- Thêm biến delay cho audio loop ---
    private static final int DEFAULT_AUDIO_LOOP_DELAY_MS = 1000;

    private int delayBetweenAudioLoopMs =
            DEFAULT_AUDIO_LOOP_DELAY_MS;



    // ⭐ Loop riêng cho EN Female / Male
    private boolean isFemaleLooping = false;
    private boolean isMaleLooping = false;
    private Handler voiceLoopHandler = new Handler(Looper.getMainLooper());
    private Runnable femaleLoopRunnable;
    private Runnable maleLoopRunnable;



    private Set<String> lockExpandedGroupKeys = new HashSet<>();
    private int autoNextInterval = 10;   // default 10 giây




    private SwitchMaterial switchFocusFloatingNote;
    private static final String KEY_FOCUS_NOTE_ON_NEXT = "focus_note_on_next";


    private androidx.cardview.widget.CardView cardSentence;
    private int originalColor;
    private int pressedColor;



    private boolean isTypingDialogShowing = false;


    private MaterialButton btnLoopFemale;
    private MaterialButton btnLoopMale;

    private View fabReplayColumn;
    private FloatingActionButton fabReplay;


    private float fabLastX = -1f;
    private float fabLastY = -1f;
    private float dX, dY;

    private static final String PREFS_FLOATING = "floating_prefs";
    private static final String KEY_FAB_X = "fab_x";
    private static final String KEY_FAB_Y = "fab_y";

    private boolean isLoopingReplay = false;
    private static final int DRAG_SLOP = 10;          // px
    private static final long DOUBLE_TAP_TIMEOUT = 300L; // ms

    private long lastTapTime = 0L;


    private int imageOriginalWidth = -1;
    private int imageOriginalHeight = -1;
    private int imageShrinkWidth = -1;
    private int imageShrinkHeight = -1;



    // 25% khi Play En ON
    private int imageShrinkWidthEnOn = -1;
    private int imageShrinkHeightEnOn = -1;

    // 70% khi Play En OFF
    private int imageShrinkWidthEnOff = -1;
    private int imageShrinkHeightEnOff = -1;


    // Lưu rating Hard/Good/Easy user chọn trước khi gõ
    private int pendingRatingAfterTyping = -1;

    // Câu đang được kiểm tra gõ (an toàn nếu bạn cần)
    private Sentence typingCheckSentence = null;

    // Hàng đợi các câu đang ở learning, cần quay lại trong phiên LockScreen
    private final List<Sentence> inSessionLearningQueue = new ArrayList<>();


    private boolean showImageInPopup = true;
    private SharedPreferences.OnSharedPreferenceChangeListener prefListener;

    private SwitchMaterial switchShowImage;
    private ShapeableImageView imageView;

    private static final String PREF_LOCKSCREEN = "lockscreen_prefs";
    private static final String KEY_SHOW_IMAGE = "show_image";


    // Khi true: lần KEYCODE_M tiếp theo sẽ bị chặn (không click btnLoopMale)
    private boolean suppressNextMaleFromKey = false;

    private LinearLayout exampleContainer;
    private TextView tvExampleEnInline;
    private TextView tvExampleViInline;


    private TextWatcher autoScaleWatcher = null;
    private Handler autoScaleHandler = new Handler(Looper.getMainLooper());
    private Runnable autoScaleRunnable = null;



    // Đánh dấu đã restore quota từ prefs hay chưa trong phiên hiện tại
    private boolean hasRestoredTopicStats = false;


    // Đánh dấu: card này vừa fail typing, đang hiển thị full đáp án
    private boolean typingFailedAndShowingAnswer = false;


    private final Handler remoteLongPressHandler = new Handler(Looper.getMainLooper());
    private Runnable dpadUpLongPressRunnable;
    private Runnable dpadDownLongPressRunnable;

    private boolean dpadUpLongPressTriggered = false;
    private boolean dpadDownLongPressTriggered = false;

    private static final long REMOTE_LONG_PRESS_MS = 500;



    // Sau khi tap image bằng M (swipe lên/xuống), F kế tiếp sẽ được coi là "double tap image"
    private boolean nextFReservedForImage = false;




    private GestureDetector remoteGestureDetector;





    // =========================================================
// STUDY MODE: VOCABULARY / LISTENING
// =========================================================

    private enum StudyMode {
        VOCABULARY,
        LISTENING
    }

    private SwitchMaterial switchStudyMode;
    private TextView tvVocabularyMode;
    private TextView tvListeningMode;

    private SwitchMaterial switchListeningSequential;
    private SwitchMaterial switchShowVoiceLoop;

    private LinearLayout voiceLoopContainer;



    private SwitchMaterial switchAutoViThenEn;

    private static final String KEY_AUTO_VI_THEN_EN =
            "auto_vi_then_en";

    private boolean autoViThenEnEnabled = false;


    private SwitchMaterial switchAutoPlayExample;
    private boolean autoPlayExampleEnabled;

    private static final String KEY_AUTO_PLAY_EXAMPLE =
            "auto_play_example";








    private boolean isApplyingStudyModePreset = false;
    private boolean hasAppliedInitialStudyModePreset = false;
    private boolean suppressPresetAutoAudioOnce = false;





    // Switch chuyển chố độ gamepad
    private View swipePriorityArea;
    private GestureDetector swipePriorityDetector;


    private View currentSwipeView;



    private SwitchMaterial switchRemoteGesture;
    private static final String KEY_REMOTE_GESTURE = "remote_gesture";



    private int getGlobalAudioDelayMs() {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        float seconds;

        if (prefs.contains("delay")) {
            try {
                // bản mới: lưu float
                seconds = prefs.getFloat("delay", 2f);
            } catch (ClassCastException e) {
                // fallback nếu máy vẫn đang có int cũ
                int oldInt = prefs.getInt("delay", 2);
                seconds = oldInt;
            }
        } else {
            seconds = 2f;
        }

        if (seconds < 0f) seconds = 0f;
        return (int) (seconds * 1000f);
    }






    //nhóm hàm tab hiển thị cài đặt

    private void showSwitchSettingsTab(int tab) {
        if (panelDisplaySettings == null
                || panelAudioSettings == null
                || panelControlSettings == null) {
            return;
        }

        panelDisplaySettings.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        panelAudioSettings.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        panelControlSettings.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);

        if (tabDisplaySettings != null) {
            tabDisplaySettings.setTextColor(tab == 0
                    ? Color.parseColor("#1976D2")
                    : Color.parseColor("#777777"));
            tabDisplaySettings.setTypeface(null, tab == 0 ? Typeface.BOLD : Typeface.NORMAL);
            tabDisplaySettings.setBackgroundColor(tab == 0 ? Color.WHITE : Color.TRANSPARENT);
        }

        if (tabAudioSettings != null) {
            tabAudioSettings.setTextColor(tab == 1
                    ? Color.parseColor("#1976D2")
                    : Color.parseColor("#777777"));
            tabAudioSettings.setTypeface(null, tab == 1 ? Typeface.BOLD : Typeface.NORMAL);
            tabAudioSettings.setBackgroundColor(tab == 1 ? Color.WHITE : Color.TRANSPARENT);
        }

        if (tabControlSettings != null) {
            tabControlSettings.setTextColor(tab == 2
                    ? Color.parseColor("#1976D2")
                    : Color.parseColor("#777777"));
            tabControlSettings.setTypeface(null, tab == 2 ? Typeface.BOLD : Typeface.NORMAL);
            tabControlSettings.setBackgroundColor(tab == 2 ? Color.WHITE : Color.TRANSPARENT);
        }

        if (switchPanel != null && switchPanelExpanded) {
            switchPanel.postDelayed(() -> {
                View content = findViewById(R.id.switchPanelContent);

                if (content != null) {
                    content.requestLayout();
                    content.invalidate();
                }

                ViewGroup.LayoutParams lp = switchPanel.getLayoutParams();
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                switchPanel.setLayoutParams(lp);

                switchPanel.requestLayout();
                switchPanel.invalidate();

                View card = findViewById(R.id.cardSentence);
                if (card != null) {
                    card.requestLayout();
                    card.invalidate();
                }
            }, 80);
        }
    }

    private void setupSwitchTabsAndLessonAutoLoop() {
        tabDisplaySettings = findViewById(R.id.tabDisplaySettings);
        tabAudioSettings = findViewById(R.id.tabAudioSettings);
        tabControlSettings = findViewById(R.id.tabControlSettings);

        panelDisplaySettings = findViewById(R.id.panelDisplaySettings);
        panelAudioSettings = findViewById(R.id.panelAudioSettings);
        panelControlSettings = findViewById(R.id.panelControlSettings);

        if (tabDisplaySettings != null) {
            tabDisplaySettings.setOnClickListener(v -> showSwitchSettingsTab(0));
        }

        if (tabAudioSettings != null) {
            tabAudioSettings.setOnClickListener(v -> showSwitchSettingsTab(1));
        }

        if (tabControlSettings != null) {
            tabControlSettings.setOnClickListener(v -> showSwitchSettingsTab(2));
        }

        showSwitchSettingsTab(0);

        SharedPreferences appPrefs = getSharedPreferences(PREFS_APP_SETTINGS, MODE_PRIVATE);

        // ===== Lesson Auto Loop =====
        MaterialButton btnLessonAutoLoop = findViewById(R.id.btnLessonAutoLoop);

        lessonAutoLoopEnabled = false;
        lessonAutoLoopRunning = false;

        appPrefs.edit()
                .putBoolean("lesson_auto_loop_enabled", false)
                .apply();

        updateLessonLoopButton();

        if (btnLessonAutoLoop != null) {
            btnLessonAutoLoop.setOnClickListener(v -> {
                toggleLessonAutoLoopFromRemote();
            });
        }

        // ===== Prev Lesson =====
        btnPrevLesson = findViewById(R.id.btnPrevLesson);
        if (btnPrevLesson != null) {
            btnPrevLesson.setOnClickListener(v -> {
                resetReplayFabState();

                if (!goToAdjacentLockscreenLesson(-1)) {
                    Toast.makeText(this, "Không còn lesson trước.", Toast.LENGTH_SHORT).show();
                }
            });
        }

// ===== Next Lesson =====
        btnNextLesson = findViewById(R.id.btnNextLesson);
        if (btnNextLesson != null) {
            btnNextLesson.setOnClickListener(v -> {
                resetReplayFabState();

                if (!goToAdjacentLockscreenLesson(1)) {
                    Toast.makeText(this, "Không còn lesson kế tiếp.", Toast.LENGTH_SHORT).show();
                }
            });
        }



        updateSwitchHeaderSummary();
    }



    private void updateNowPlayingHeader() {
        if (currentSentence == null) return;

        int current = 1;
        int total = 0;

        if (sentences != null && !sentences.isEmpty()) {
            total = sentences.size();
            String curKey = makeSentenceKey(currentSentence);

            for (int i = 0; i < sentences.size(); i++) {
                Sentence s = sentences.get(i);
                if (s == currentSentence || (s != null && makeSentenceKey(s).equals(curKey))) {
                    current = i + 1;
                    break;
                }
            }
        }

        if (tvTrackCounter != null) {
            tvTrackCounter.setText(String.format(Locale.US, "%02d/%02d", current, total));
        }

        if (tvNowPlaying != null) {
            if (!TextUtils.isEmpty(lockCurrentSelectionLabel)) {
                tvNowPlaying.setText("🎧 " + lockCurrentSelectionLabel);
            } else if (currentTopicOnLockscreen != null && !TextUtils.isEmpty(currentTopicOnLockscreen.name)) {
                tvNowPlaying.setText("🎧 " + currentTopicOnLockscreen.name);
            } else {
                tvNowPlaying.setText("🎧 Now Playing");
            }
            tvNowPlaying.setSelected(true);
        }
    }



    private void updateSwitchHeaderSummary() {
        // Header cũ tvSwitchHeaderSummary đã bỏ khỏi XML.
        // Giữ hàm này để các chỗ đang gọi không lỗi.
        // Now Playing sẽ được cập nhật riêng trong updateNowPlayingInfo().
        if (tvNowPlaying != null && currentSentence != null) {
            updateNowPlayingInfo(currentSentence);
        }
    }


    private void updateLessonLoopButton() {

        MaterialButton btn = findViewById(R.id.btnLessonAutoLoop);
        if (btn == null) return;

        int total = (sentences == null) ? 0 : sentences.size();

        if (lessonAutoLoopRunning && total > 0) {

            int current = lessonAutoLoopIndex + 1;

            if (current < 1) current = 1;
            if (current > total) current = total;

            btn.setText(String.format(
                    Locale.US,
                    "⏹ Looping %02d/%02d",
                    current,
                    total
            ));

        } else {

            btn.setText(String.format(
                    Locale.US,
                    "▶ Loop Lesson (%d)",
                    total
            ));
        }
    }

    private void startLessonAutoLoop() {
        if (sentences == null || sentences.isEmpty()) {
            lessonAutoLoopEnabled = false;
            lessonAutoLoopRunning = false;

            getSharedPreferences(PREFS_APP_SETTINGS, MODE_PRIVATE)
                    .edit()
                    .putBoolean("lesson_auto_loop_enabled", false)
                    .apply();

            updateLessonLoopButton();
            updateSwitchHeaderSummary();

            Toast.makeText(this,
                    "Không có track nào trong lesson hiện tại.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        lessonAutoLoopEnabled = true;
        lessonAutoLoopRunning = true;

        getSharedPreferences(PREFS_APP_SETTINGS, MODE_PRIVATE)
                .edit()
                .putBoolean("lesson_auto_loop_enabled", true)
                .apply();

        initLessonAutoLoopIndex();
        updateLessonLoopButton();

        Toast.makeText(this,
                "Lesson Auto Loop ON (" + sentences.size() + " track)",
                Toast.LENGTH_SHORT).show();

        playCurrentLessonAutoLoopTrack();
    }


    private void stopLessonAutoLoop() {
        lessonAutoLoopEnabled = false;
        lessonAutoLoopRunning = false;

        getSharedPreferences(PREFS_APP_SETTINGS, MODE_PRIVATE)
                .edit()
                .putBoolean("lesson_auto_loop_enabled", false)
                .apply();

        stopAudioLoop();
        stopAllVoiceLoops();
        stopCurrentMediaPlayerSafely();

        updateLessonLoopButton();
        updateSwitchHeaderSummary();

        Toast.makeText(this, "Lesson Auto Loop OFF", Toast.LENGTH_SHORT).show();
    }


    private void applyLessonAutoLoopState(boolean enabled, boolean updateButton) {

        if (isApplyingLessonAutoLoopState) {
            return;
        }

        isApplyingLessonAutoLoopState = true;

        lessonAutoLoopEnabled = enabled;

        getSharedPreferences(PREFS_APP_SETTINGS, MODE_PRIVATE)
                .edit()
                .putBoolean("lesson_auto_loop_enabled", enabled)
                .apply();

        if (updateButton) {
            MaterialButton btnLessonAutoLoop = findViewById(R.id.btnLessonAutoLoop);

            if (btnLessonAutoLoop != null) {
                btnLessonAutoLoop.setText(
                        enabled
                                ? "⏹ Stop Loop"
                                : "🔁 Loop Lesson"
                );
            }
        }

        if (enabled) {
            startLessonAutoLoop();
        } else {
            stopLessonAutoLoop();
        }

        updateSwitchHeaderSummary();

        isApplyingLessonAutoLoopState = false;
    }


    private void initLessonAutoLoopIndex() {
        // ⭐ Mỗi lần bật Lesson Auto Loop luôn chạy từ track đầu tiên
        lessonAutoLoopIndex = 0;

        if (sentences == null || sentences.isEmpty()) {
            return;
        }

        currentSentence = sentences.get(0);

        hasShownAnswer = false;
        hasShownAnswerForCurrentSentence = false;
        hasPlayedExampleForCurrentSentence = false;
        isViAudioPlaying = false;

        currentAudioEnFile = currentSentence.audio;
        currentAudioViFile = currentSentence.audio_vi;
        saveCurrentLockscreenPosition();

        updateCardViewWithSentence(currentSentence);
        updateNowPlayingHeader();
    }

    private void playCurrentLessonAutoLoopTrack() {
        if (!lessonAutoLoopEnabled || !lessonAutoLoopRunning) return;
        if (sentences == null || sentences.isEmpty()) return;

        if (lessonAutoLoopIndex < 0 || lessonAutoLoopIndex >= sentences.size()) {
            lessonAutoLoopIndex = 0;
        }

        updateLessonLoopButton();

        Sentence s = sentences.get(lessonAutoLoopIndex);
        if (s == null) {
            goNextLessonAutoLoopTrack();
            return;
        }

        currentSentence = s;
        currentAudioEnFile = s.audio;
        currentAudioViFile = s.audio_vi;

        hasShownAnswer = false;
        hasShownAnswerForCurrentSentence = false;
        hasPlayedExampleForCurrentSentence = false;
        isViAudioPlaying = false;

        saveCurrentLockscreenPosition();

        updateCardViewWithSentence(s);
        updateNowPlayingHeader();
        updateNowPlayingInfo(s);

        String audioFile = null;

        if (!TextUtils.isEmpty(s.audio)) {
            audioFile = s.audio;
        } else if (!TextUtils.isEmpty(s.audio_female)) {
            audioFile = s.audio_female;
        } else if (!TextUtils.isEmpty(s.audio_male)) {
            audioFile = s.audio_male;
        } else if (!TextUtils.isEmpty(s.example_audio)) {
            audioFile = s.example_audio;
        }

        if (TextUtils.isEmpty(audioFile)) {
            Log.w("LESSON_AUTO_LOOP",
                    "Missing audio index=" + lessonAutoLoopIndex + ", en=" + s.en);
            goNextLessonAutoLoopTrack();
            return;
        }

        final String fileToPlay = audioFile;

        Log.d("LESSON_AUTO_LOOP",
                "Play " + (lessonAutoLoopIndex + 1)
                        + "/" + sentences.size()
                        + " file=" + fileToPlay);

        playAudio(fileToPlay, false, () -> {
            if (!lessonAutoLoopEnabled || !lessonAutoLoopRunning) return;
            handler.postDelayed(this::goNextLessonAutoLoopTrack, 500);
        });
    }

    private void goNextLessonAutoLoopTrack() {
        if (!lessonAutoLoopEnabled || !lessonAutoLoopRunning) return;
        if (sentences == null || sentences.isEmpty()) return;

        if (listeningSequentialMode) {

            // Sequential ON → chạy 1,2,3... trong lesson hiện tại
            lessonAutoLoopIndex++;

            if (lessonAutoLoopIndex >= sentences.size()) {
                // Hết lesson thì quay lại track đầu tiên
                lessonAutoLoopIndex = 0;
            }

            Log.d("LESSON_AUTO_LOOP",
                    "Sequential next index="
                            + lessonAutoLoopIndex
                            + "/"
                            + sentences.size());

        } else {

            // Sequential OFF → random trong lesson hiện tại
            if (sentences.size() <= 1) {
                lessonAutoLoopIndex = 0;
            } else {
                int oldIndex = lessonAutoLoopIndex;
                int newIndex;

                do {
                    newIndex = new Random().nextInt(sentences.size());
                } while (newIndex == oldIndex);

                lessonAutoLoopIndex = newIndex;
            }

            Log.d("LESSON_AUTO_LOOP",
                    "Random next index="
                            + lessonAutoLoopIndex
                            + "/"
                            + sentences.size());
        }

        playCurrentLessonAutoLoopTrack();
    }


    private boolean goToAdjacentLockscreenLesson(int direction) {
        ensureCurrentLessonState();

        String currentLessonKey = getCurrentSingleLessonKey();

        if (TextUtils.isEmpty(currentLessonKey)) {
            Toast.makeText(this,
                    "Chỉ dùng được khi đang chọn 1 lesson đơn.",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        if (currentTopicOnLockscreen == null) {
            Toast.makeText(this,
                    "Chưa có topic hiện tại.",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        List<TopicInfo> topics = TopicManager.getTopics(this);
        if (topics == null || topics.isEmpty()) {
            Toast.makeText(this,
                    "Không có danh sách topic.",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        String targetGroup = !TextUtils.isEmpty(lockCurrentGroupName)
                ? lockCurrentGroupName.trim()
                : (currentTopicOnLockscreen.group != null
                ? currentTopicOnLockscreen.group.trim()
                : "");

        String targetSection = !TextUtils.isEmpty(lockCurrentSectionName)
                ? lockCurrentSectionName.trim()
                : (currentTopicOnLockscreen.section != null
                ? currentTopicOnLockscreen.section.trim()
                : "");

        List<LockDisplayNode> lessonNodes = new ArrayList<>();

        for (TopicInfo topic : topics) {
            if (topic == null) continue;

            String topicGroup = topic.group != null ? topic.group.trim() : "";
            String topicSection = topic.section != null ? topic.section.trim() : "";

            if (TextUtils.isEmpty(topicGroup)) topicGroup = "Khác";

            if (topic.lessons == null || topic.lessons.isEmpty()) continue;

            for (LessonInfo lesson : topic.lessons) {
                if (lesson == null) continue;

                String lessonGroup = lesson.group != null ? lesson.group.trim() : "";
                String lessonSection = lesson.section != null ? lesson.section.trim() : "";

                if (TextUtils.isEmpty(lessonGroup)) lessonGroup = topicGroup;
                if (TextUtils.isEmpty(lessonSection)) lessonSection = topicSection;

                if (!TextUtils.equals(lessonGroup, targetGroup)
                        || !TextUtils.equals(lessonSection, targetSection)) {
                    continue;
                }

                lessonNodes.add(new LockDisplayNode(
                        2,
                        "       • " + lesson.name + "  (" + lesson.count + " câu)",
                        lessonGroup,
                        lessonSection,
                        buildLockSectionKey(lessonGroup, lessonSection),
                        topic,
                        lesson,
                        null,
                        false
                ));
            }
        }

        if (lessonNodes.isEmpty()) {
            Toast.makeText(this,
                    "Section hiện tại không có lesson liền kề.",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        int currentPos = -1;

        for (int i = 0; i < lessonNodes.size(); i++) {
            LockDisplayNode n = lessonNodes.get(i);

            if (n.lesson != null
                    && n.lesson.key != null
                    && n.lesson.key.equals(currentLessonKey)) {
                currentPos = i;
                break;
            }
        }

        if (currentPos < 0) {
            Toast.makeText(this,
                    "Không tìm thấy lesson hiện tại trong section.",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        int nextPos = currentPos + direction;

        if (nextPos >= lessonNodes.size()) {
            nextPos = 0;
        }

        if (nextPos < 0) {
            nextPos = lessonNodes.size() - 1;
        }

        LockDisplayNode nextNode = lessonNodes.get(nextPos);

        Log.d("LOCK_NEXT_LESSON",
                "Move lesson " + currentPos + " -> " + nextPos
                        + " | next=" + (nextNode.lesson != null ? nextNode.lesson.name : "null"));

        if (lessonAutoLoopEnabled || lessonAutoLoopRunning) {
            applyLessonAutoLoopState(false, true);
        }

        resetReplayFabState();

        // ⭐ Quan trọng:
        // selectLockscreenNode() đã filter lesson mới,
        // đã save selection label,
        // đã showFirstTrackOfCurrentLesson(true)
        // nên KHÔNG cần postDelayed set currentSentence lần nữa.
        selectLockscreenNode(nextNode, null);

        if (currentSentence != null) {
            updateCardViewWithSentence(currentSentence);
            updateNowPlayingHeader();
            updateNowPlayingInfo(currentSentence);
            syncSequentialIndexWithCurrentSentence();
            saveCurrentLockscreenPosition();
        }

        return true;
    }




    private void jumpToSequentialTrack(int targetIndex) {
        if (sentences == null || sentences.isEmpty()) return;

        if (targetIndex < 0) targetIndex = 0;
        if (targetIndex >= sentences.size()) targetIndex = sentences.size() - 1;

        listeningSequentialIndex = targetIndex;
        lessonAutoLoopIndex = targetIndex;

        currentSentence = sentences.get(targetIndex);

        hasShownAnswer = false;
        hasShownAnswerForCurrentSentence = false;
        hasPlayedExampleForCurrentSentence = false;
        isViAudioPlaying = false;

        currentAudioEnFile = currentSentence.audio;
        currentAudioViFile = currentSentence.audio_vi;

        saveCurrentLockscreenPosition();

        updateCardViewWithSentence(currentSentence);
        updateNowPlayingHeader();
        updateNowPlayingInfo(currentSentence);

        if (!isMuted && !TextUtils.isEmpty(currentAudioEnFile)) {
            playAudio(currentAudioEnFile, false, null);
        }
    }


    private String getCurrentSingleLessonKey() {
        if (lockCurrentLessonKeys == null || lockCurrentLessonKeys.size() != 1) {
            return null;
        }

        for (String key : lockCurrentLessonKeys) {
            return key;
        }

        return null;
    }



    private void playFirstAudioAfterLessonChanged() {
        stopAudioLoop();
        stopAllVoiceLoops();
        if (currentSentence == null) return;

        String audioFile = null;

        if (!TextUtils.isEmpty(currentSentence.audio)) {
            audioFile = currentSentence.audio;
        } else if (!TextUtils.isEmpty(currentSentence.audio_female)) {
            audioFile = currentSentence.audio_female;
        } else if (!TextUtils.isEmpty(currentSentence.audio_male)) {
            audioFile = currentSentence.audio_male;
        } else if (!TextUtils.isEmpty(currentSentence.example_audio)) {
            audioFile = currentSentence.example_audio;
        }

        if (TextUtils.isEmpty(audioFile)) {
            Toast.makeText(this,
                    "Lesson mới không có audio để phát.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        playAudio(audioFile, false, null);
    }


    private void showFirstTrackOfCurrentLesson(boolean autoPlay) {
        if (sentences == null || sentences.isEmpty()) return;

        stopAudioLoop();
        stopAllVoiceLoops();
        stopCurrentMediaPlayerSafely();

        lessonAutoLoopIndex = 0;
        listeningSequentialIndex = 0;

        currentSentence = sentences.get(0);

        hasShownAnswer = false;
        hasShownAnswerForCurrentSentence = false;
        hasPlayedExampleForCurrentSentence = false;
        isViAudioPlaying = false;

        currentAudioEnFile = currentSentence.audio;
        currentAudioViFile = currentSentence.audio_vi;

        shownHistoryKeys.clear();
        shownHistoryIdx = -1;
        historyCache.clear();

        shownHistoryKeys.add(makeSentenceKey(currentSentence));
        shownHistoryIdx = 0;
        historyCache.add(currentSentence);

        saveCurrentLockscreenPosition();

        updateCardViewWithSentence(currentSentence);
        updateNowPlayingHeader();
        updateNowPlayingInfo(currentSentence);

        updateLessonLoopButton();   // <-- thêm dòng này

        updateStats();
        updateSrsStatsBar();

        if (autoPlay) {
            playFirstAudioAfterLessonChanged();
        }
    }


    // screen off khi không tương tác

    private long INACTIVITY_TIMEOUT_MS;


    ///reloadTopic

    private void saveLockscreenSelectionState(String label, Set<String> lessonKeys,
                                              String groupName, String sectionName) {
        SharedPreferences sp = getSharedPreferences("lockscreen_prefs", MODE_PRIVATE);

        StringBuilder sb = new StringBuilder();
        if (lessonKeys != null) {
            for (String k : lessonKeys) {
                if (TextUtils.isEmpty(k)) continue;
                if (sb.length() > 0) sb.append(";;");
                sb.append(k);
            }
        }

        sp.edit()
                .putString(KEY_LOCK_CURRENT_LABEL, label != null ? label : "")
                .putString(KEY_LOCK_CURRENT_LESSON_KEYS, sb.toString())
                .putString(KEY_LOCK_CURRENT_GROUP, groupName != null ? groupName : "")
                .putString(KEY_LOCK_CURRENT_SECTION, sectionName != null ? sectionName : "")
                .apply();
    }

    private void restoreLockscreenSelectionState() {
        SharedPreferences sp = getSharedPreferences("lockscreen_prefs", MODE_PRIVATE);

        lockCurrentSelectionLabel = sp.getString(KEY_LOCK_CURRENT_LABEL, "");

        // ✅ SỬA LỖI KEY KHÔNG KHỚP
        lockCurrentGroupName = sp.getString(KEY_LOCK_CURRENT_GROUP, "");
        lockCurrentSectionName = sp.getString(KEY_LOCK_CURRENT_SECTION, "");

        lockCurrentLessonKeys.clear();

        String rawKeys = sp.getString(KEY_LOCK_CURRENT_LESSON_KEYS, "");
        if (!TextUtils.isEmpty(rawKeys)) {
            String[] arr = rawKeys.split(";;");
            for (String k : arr) {
                if (!TextUtils.isEmpty(k)) {
                    lockCurrentLessonKeys.add(k.trim());
                }
            }
        }

        Log.d("LOCK_RESTORE",
                "label=" + lockCurrentSelectionLabel
                        + " | group=" + lockCurrentGroupName
                        + " | section=" + lockCurrentSectionName
                        + " | keys=" + lockCurrentLessonKeys);
    }

    private void ensureCurrentLessonState() {
        if (currentTopicOnLockscreen == null || currentSentence == null) return;

        // Nếu đang chọn multi-lesson thì không ép thành single lesson
        if (lockCurrentLessonKeys != null && lockCurrentLessonKeys.size() > 1) return;

        // Nếu đã có single lesson rồi thì chỉ đảm bảo group/section/label có dữ liệu
        if (lockCurrentLessonKeys != null && lockCurrentLessonKeys.size() == 1
                && !TextUtils.isEmpty(lockCurrentGroupName)
                && !TextUtils.isEmpty(lockCurrentSectionName)
                && !TextUtils.isEmpty(lockCurrentSelectionLabel)) {
            return;
        }

        List<Sentence> fullList = loadSentencesForTopicPreviewOnLockScreen(currentTopicOnLockscreen);
        if (fullList == null || fullList.isEmpty()) return;

        String currentKey = makeSentenceKey(currentSentence);

        LessonInfo foundLesson = null;
        int foundIndex = -1;

        for (int i = 0; i < fullList.size(); i++) {
            Sentence s = fullList.get(i);
            if (s == null) continue;

            if (makeSentenceKey(s).equals(currentKey)) {
                foundIndex = i;
                foundLesson = TopicTreeManager.parseLessonInfoFromSentence(s, i);
                break;
            }
        }

        if (foundLesson == null || TextUtils.isEmpty(foundLesson.key)) {
            Log.w("LOCK_ENSURE",
                    "Cannot infer lesson from currentSentence. index=" + foundIndex
                            + " | en=" + currentSentence.en);
            return;
        }

        String topicGroup = currentTopicOnLockscreen.group != null
                ? currentTopicOnLockscreen.group.trim()
                : "";

        String topicSection = currentTopicOnLockscreen.section != null
                ? currentTopicOnLockscreen.section.trim()
                : "";

        if (TextUtils.isEmpty(topicGroup)) topicGroup = "Khác";

        String lessonGroup = foundLesson.group != null ? foundLesson.group.trim() : "";
        String lessonSection = foundLesson.section != null ? foundLesson.section.trim() : "";

        if (TextUtils.isEmpty(lessonGroup)) lessonGroup = topicGroup;
        if (TextUtils.isEmpty(lessonSection)) lessonSection = topicSection;

        lockCurrentLessonKeys.clear();
        lockCurrentLessonKeys.add(foundLesson.key);

        lockCurrentGroupName = lessonGroup;
        lockCurrentSectionName = lessonSection;

        if (TextUtils.isEmpty(lessonSection)) {
            lockCurrentSelectionLabel = lessonGroup + " / " + foundLesson.name;
        } else {
            lockCurrentSelectionLabel =
                    lessonGroup + " / " + lessonSection + " / " + foundLesson.name;
        }

        saveLockscreenSelectionState(
                lockCurrentSelectionLabel,
                lockCurrentLessonKeys,
                lockCurrentGroupName,
                lockCurrentSectionName
        );

        Log.d("LOCK_ENSURE",
                "Rebuilt lesson state"
                        + " | lessonKey=" + foundLesson.key
                        + " | label=" + lockCurrentSelectionLabel
                        + " | group=" + lockCurrentGroupName
                        + " | section=" + lockCurrentSectionName);
    }

    private void reloadLockscreenTopicIfChanged() {
        SharedPreferences lsPrefs =
                getSharedPreferences(
                        "lockscreen_prefs",
                        MODE_PRIVATE
                );

        String topicId =
                lsPrefs.getString(
                        "current_topic_id",
                        null
                );

        String topicFile =
                lsPrefs.getString(
                        "current_topic_file",
                        null
                );

        // Không có topic hợp lệ thì không reload
        if (TextUtils.isEmpty(topicId)
                || TextUtils.isEmpty(topicFile)) {

            Log.w(
                    "LOCK_TOPIC",
                    "reloadLockscreenTopicIfChanged: missing topic"
                            + " | topicId=" + topicId
                            + " | topicFile=" + topicFile
            );

            return;
        }

        boolean changed =
                !TextUtils.equals(
                        topicId,
                        lastLoadedLockTopicId
                )
                        || !TextUtils.equals(
                        topicFile,
                        lastLoadedLockTopicFile
                );

        // Topic hiện tại vẫn là topic đã load
        if (!changed) {
            Log.d(
                    "LOCK_TOPIC",
                    "Topic unchanged"
                            + " | topicId=" + topicId
                            + " | topicFile=" + topicFile
            );

            return;
        }

        Log.d(
                "LOCK_TOPIC",
                "Topic changed -> reload"
                        + " | topicId=" + topicId
                        + " | topicFile=" + topicFile
                        + " | oldTopicId=" + lastLoadedLockTopicId
                        + " | oldTopicFile=" + lastLoadedLockTopicFile
        );

        // =========================================================
        // 1. DỪNG AUDIO / LOOP CŨ
        // =========================================================
        stopAllLoopsAndTimers();
        stopCurrentMediaPlayerSafely();

        // =========================================================
        // 2. ĐÓNG TOPIC PACKAGE CŨ
        // =========================================================
        if (topicPackageManager != null) {
            try {
                topicPackageManager.close();
            } catch (Exception e) {
                Log.w(
                        "LOCK_TOPIC",
                        "Error closing old TopicPackageManager",
                        e
                );
            }

            topicPackageManager = null;
        }

        // =========================================================
        // 3. TÌM TOPIC THẬT
        // =========================================================
        TopicInfo topic =
                TopicManager.getTopicById(
                        this,
                        topicId
                );

        if (topic == null) {
            Log.e(
                    "LOCK_TOPIC",
                    "TopicInfo null"
                            + " | topicId=" + topicId
                            + " | topicFile=" + topicFile
            );

            return;
        }

        currentTopicOnLockscreen =
                topic;

        Log.d(
                "LOCK_TOPIC",
                "Found TopicInfo"
                        + " | id=" + topic.id
                        + " | name=" + topic.name
                        + " | file=" + topic.fileName
        );

        // =========================================================
        // 4. TẠO LẠI TOPIC PACKAGE MANAGER
        // =========================================================
        try {
            String packageTopicName =
                    getTopicPackageNameFromJsonFile(
                            topic.fileName
                    );

            topicPackageManager =
                    new TopicPackageManager(
                            this,
                            packageTopicName
                    );

            Log.d(
                    "LOCK_TOPIC",
                    "TopicPackageManager ready"
                            + " | package="
                            + packageTopicName
            );

        } catch (Exception e) {
            topicPackageManager = null;

            Log.e(
                    "LOCK_TOPIC",
                    "Cannot create TopicPackageManager"
                            + " | topicId=" + topic.id
                            + " | topicFile=" + topic.fileName,
                    e
            );
        }

        // =========================================================
        // 5. KHÔI PHỤC SELECTION TRƯỚC KHI LOAD SENTENCES
        // =========================================================
        restoreLockscreenSelectionState();

        Log.d(
                "LOCK_TOPIC",
                "Restored selection"
                        + " | label=" + lockCurrentSelectionLabel
                        + " | group=" + lockCurrentGroupName
                        + " | section=" + lockCurrentSectionName
                        + " | lessonKeys=" + lockCurrentLessonKeys
        );

        // =========================================================
        // 6. RESET STATE CŨ
        // =========================================================
        currentSentence = null;

        hasShownAnswer = false;
        hasShownAnswerForCurrentSentence = false;
        hasPlayedExampleForCurrentSentence = false;
        isViAudioPlaying = false;

        currentAudioEnFile = null;
        currentAudioViFile = null;

        shownHistoryKeys.clear();
        shownHistoryIdx = -1;
        historyCache.clear();

        listeningSequentialIndex = 0;
        lessonAutoLoopIndex = 0;

        // =========================================================
        // 7. LOAD SENTENCES ĐÚNG TOPIC + SELECTION
        // =========================================================
        loadSentencesForTopicOnLockScreen(
                topic
        );

        restoreTopicStats(
                topic.id
        );

        Log.d(
                "LOCK_TOPIC",
                "Sentences loaded"
                        + " | size="
                        + (
                        sentences != null
                                ? sentences.size()
                                : 0
                )
        );

        // =========================================================
        // 8. ĐÁNH DẤU TOPIC ĐÃ LOAD
        // =========================================================
        lastLoadedLockTopicId =
                topicId;

        lastLoadedLockTopicFile =
                topicFile;

        // =========================================================
        // 9. KHÔI PHỤC LESSON + CÂU TRƯỚC ĐÓ
        // =========================================================

        /*
         * Trong restoreSelectedLessonIfNeeded(),
         * updateCardViewWithSentence() có thể được gọi.
         *
         * Không cho trạng thái Play Male cũ phát
         * trước khi topic mới được tự nhận diện.
         */
        suppressPresetAutoAudioOnce = true;

        restoreSelectedLessonIfNeeded();
        restoreCurrentLockscreenPosition();

        // Nếu không restore được câu cũ thì mới fallback câu đầu
        if (currentSentence == null
                && sentences != null
                && !sentences.isEmpty()) {

            currentSentence =
                    sentences.get(0);

            Log.d(
                    "LOCK_TOPIC",
                    "Cannot restore previous sentence"
                            + " -> fallback index 0"
            );
        }

        // Đồng bộ audio trước khi áp preset
        if (currentSentence != null) {
            currentAudioEnFile =
                    currentSentence.audio;

            currentAudioViFile =
                    currentSentence.audio_vi;
        }

        // =========================================================
        // 10. TOPIC THẬT SỰ ĐÃ ĐỔI → TỰ NHẬN DIỆN MODE
        // =========================================================

        boolean studyModeApplied = false;

        if (sentences != null
                && !sentences.isEmpty()) {

            studyModeApplied = true;

            detectAndApplyStudyModeForCurrentSelection(
                    "reloadLockscreenTopicIfChanged"
            );
        }

        // =========================================================
        // 11. ĐỒNG BỘ STATE VỚI CÂU ĐÃ RESTORE
        // =========================================================
        if (currentSentence != null) {
            syncSequentialIndexWithCurrentSentence();

            lessonAutoLoopIndex =
                    listeningSequentialIndex;

            /*
             * Nếu preset vừa chạy thì preset đã bind lại UI.
             * Không gọi updateCardViewWithSentence() lần hai,
             * tránh Play Male phát ngoài ý muốn.
             */
            if (!studyModeApplied) {
                updateCardViewWithSentence(
                        currentSentence
                );
            }

            updateNowPlayingHeader();
            updateNowPlayingInfo(
                    currentSentence
            );

            saveCurrentLockscreenPosition();

            Log.d(
                    "LOCK_TOPIC",
                    "Restore completed"
                            + " | index="
                            + listeningSequentialIndex
                            + " | en="
                            + currentSentence.en
                            + " | audio="
                            + currentAudioEnFile
                            + " | studyModeApplied="
                            + studyModeApplied
            );

        } else {
            Log.w(
                    "LOCK_TOPIC",
                    "Reload completed but currentSentence is null"
            );
        }

        // =========================================================
        // 12. UPDATE UI / STATS
        // =========================================================
        updateStats();
        updateSrsStatsBar();
    }


    // Lưu history bằng key (en + vi) để đảm bảo duy nhất, không giữ reference object


    // trong LockScreenActivity

    private boolean isMuted = false;

    private void ensureSentencesJsonExists() {
        File file = new File(getFilesDir(), "sentences.json");
        if (!file.exists()) {
            try {
                InputStream is = getAssets().open("sentences.json");
                FileOutputStream fos = openFileOutput("sentences.json", MODE_PRIVATE);
                byte[] buffer = new byte[4096];
                int len;
                while ((len = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                is.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void initEnglishGlowEffect() {
        if (enText == null) return;

        // Dùng shadow để tạo glow vàng quanh chữ
        enText.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT);

        englishGlowAnimator = ObjectAnimator.ofFloat(enText, "shadowRadius", 0f, 25f);
        englishGlowAnimator.setDuration(600);      // 0.6s cho 1 chu kỳ
        englishGlowAnimator.setRepeatMode(ValueAnimator.REVERSE);
        englishGlowAnimator.setRepeatCount(ValueAnimator.INFINITE);

        // Ta cần đồng thời đổi màu shadow sang vàng
        englishGlowAnimator.addUpdateListener(animation -> {
            float radius = (float) animation.getAnimatedValue();
            enText.setShadowLayer(radius, 0f, 0f, Color.YELLOW);
        });
    }

    private void resetFloatingNoteForNewSentence() {
        if (edtFloatingNote != null) {
            Editable editable = edtFloatingNote.getText();
            if (editable != null) {
                editable.clear();      // xóa text
                // Nếu trước đây bạn có dùng span thì mới cần clearSpans
                // editable.clearSpans();
            } else {
                edtFloatingNote.setText("");
            }
            edtFloatingNote.clearFocus();
        }
    }


    private void resetFloatingNotePositionAndScale() {
        if (floatingNoteView == null || !isFloatingNoteVisible) return;

        floatingNoteView.animate()
                .x(noteLastX)
                .y(noteLastY)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(150)
                .start();
    }


    private void recalcRatingCounters() {
        totalAgain = totalHard = totalGood = totalEasy = 0;
        if (sentences == null) {
            Log.d("SRS_DEBUG", "⚠️ recalcRatingCounters: sentences = NULL");
            return;
        }

        Log.d("SRS_DEBUG", "🔢 recalcRatingCounters: Đếm " + sentences.size() + " câu...");

        int debugCount = 0;
        for (Sentence s : sentences) {
            switch (s.lastRating) {
                case 0:
                    totalAgain++;
                    if (debugCount < 3) {  // Chỉ log 3 câu đầu
                        Log.d("SRS_DEBUG", "  [Again] " + s.en + " @ " + System.identityHashCode(s));
                        debugCount++;
                    }
                    break;
                case 1: totalHard++;  break;
                case 2: totalGood++;  break;
                case 3: totalEasy++;  break;
            }
        }

        Log.d("SRS_DEBUG", "🔢 KẾT QUẢ: Again=" + totalAgain
                + ", Hard=" + totalHard
                + ", Good=" + totalGood
                + ", Easy=" + totalEasy
                + ", Total=" + (totalAgain + totalHard + totalGood + totalEasy));
    }


    private void startExampleLoopVisualEffect() {


        if (exampleLoopAnimator != null && exampleLoopAnimator.isRunning()) {
            return; // đang chạy rồi
        }

        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 1f, 1.18f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 1f, 1.18f);



        exampleLoopAnimator.setDuration(320);
        exampleLoopAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        exampleLoopAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        exampleLoopAnimator.start();
    }

    private void stopExampleLoopVisualEffect() {
        if (exampleLoopAnimator != null) {
            exampleLoopAnimator.cancel();
            exampleLoopAnimator = null;
        }

    }



    private void startLoopVisualEffect() {
        if (enText == null) return;

        // Thiết lập shadow cho cảm giác 3D + glow
        // params: radius, dx, dy, color
        enText.setLayerType(View.LAYER_TYPE_SOFTWARE, null); // cần để shadowLayer mượt
        enText.setShadowLayer(
                8f,                    // radius
                0f, 0f,                // offset x, y
                Color.parseColor("#FFEB3B") // vàng sáng, bạn đổi theo theme
        );

        if (loopPulseAnimator == null) {
            PropertyValuesHolder scaleX =
                    PropertyValuesHolder.ofFloat("scaleX", 1f, 1.10f);
            PropertyValuesHolder scaleY =
                    PropertyValuesHolder.ofFloat("scaleY", 1f, 1.10f);
            PropertyValuesHolder alphaHolder =
                    PropertyValuesHolder.ofFloat("alpha", 0.8f, 1.0f);

            loopPulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
                    enText,
                    scaleX,
                    scaleY,
                    alphaHolder
            );
            loopPulseAnimator.setDuration(600);              // nhịp khá rõ
            loopPulseAnimator.setRepeatMode(ObjectAnimator.REVERSE);
            loopPulseAnimator.setRepeatCount(ObjectAnimator.INFINITE);
            loopPulseAnimator.setInterpolator(new LinearInterpolator());
        }

        if (!loopPulseAnimator.isStarted()) {
            loopPulseAnimator.start();
        }
    }




    private void stopLoopVisualEffect() {
        if (loopPulseAnimator != null) {
            loopPulseAnimator.cancel();
        }
        if (enText != null) {
            enText.setScaleX(1f);
            enText.setScaleY(1f);
            enText.setAlpha(1f);
            // Tắt shadow để text về bình thường
            enText.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT);
            enText.setLayerType(View.LAYER_TYPE_NONE, null);
        }
    }
    // ⭐ Hiệu ứng blow nhẹ, lặp lại cho English text (không quá chói)
    // ⭐ Hiệu ứng blow + 3D nhẹ, lặp lại cho English text
    private ObjectAnimator enBlowAnimator;

    private void startEnglishBlowEffect() {
        if (enText == null) return;

        // Bật layer software để shadow mượt
        enText.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        // Shadow 3D + glow vàng rõ hơn
        enText.setShadowLayer(
                10f,                         // radius lớn hơn cho viền rõ hơn
                0f, 0f,                      // không lệch, glow đều xung quanh
                Color.parseColor("#FFFFD54F") // vàng nhạt (Amber 300), bạn có thể thử "#FFFFFF00" nếu muốn vàng chói
        );

        if (enBlowAnimator == null) {
            PropertyValuesHolder scaleX =
                    PropertyValuesHolder.ofFloat("scaleX", 1f, 1.05f);
            PropertyValuesHolder scaleY =
                    PropertyValuesHolder.ofFloat("scaleY", 1f, 1.05f);
            PropertyValuesHolder alphaHolder =
                    PropertyValuesHolder.ofFloat("alpha", 0.92f, 1.0f);

            enBlowAnimator = ObjectAnimator.ofPropertyValuesHolder(
                    enText,
                    scaleX,
                    scaleY,
                    alphaHolder
            );
            enBlowAnimator.setDuration(800);                  // nhịp chậm, nhẹ nhàng
            enBlowAnimator.setRepeatMode(ObjectAnimator.REVERSE);
            enBlowAnimator.setRepeatCount(ObjectAnimator.INFINITE);
            enBlowAnimator.setInterpolator(new LinearInterpolator());
        }

        if (!enBlowAnimator.isStarted()) {
            enBlowAnimator.start();
        }
    }

    private void stopEnglishBlowEffect() {
        if (enBlowAnimator != null) {
            enBlowAnimator.cancel();
        }
        if (enText != null) {
            enText.setScaleX(1f);
            enText.setScaleY(1f);
            enText.setAlpha(1f);
            // Tắt shadow 3D, trả về bình thường
            enText.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT);
            enText.setLayerType(View.LAYER_TYPE_NONE, null);
        }
    }




    private void moveFloatingNoteToCenter() {
        if (floatingNoteView == null || edtFloatingNote == null) return;

        ConstraintLayout rootLayout = findViewById(R.id.rootLayout);
        if (rootLayout == null) return;

        rootLayout.post(() -> {
            int parentWidth = rootLayout.getWidth();
            int parentHeight = rootLayout.getHeight();

            if (parentWidth <= 0 || parentHeight <= 0) {
                return;
            }

            float scaleX = 1.9f;
            float scaleY = 1.5f;

            View header = floatingNoteView.findViewById(R.id.floatingNoteHeader);
            if (header != null) {
                header.setVisibility(View.GONE);
            }

            edtFloatingNote.setEllipsize(null);
            edtFloatingNote.setGravity(Gravity.CENTER);

            ViewGroup.LayoutParams lpEdt = edtFloatingNote.getLayoutParams();
            lpEdt.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            edtFloatingNote.setLayoutParams(lpEdt);

            float density = getResources().getDisplayMetrics().density;
            String text = edtFloatingNote.getText().toString();

            // ===== PC MODE =====
            if (isPcMode && text != null && !text.isEmpty()) {
                edtFloatingNote.setSingleLine(true);
                edtFloatingNote.setMaxLines(1);

                // ⭐ MỞ RỘNG NOTE NGANG ≈ 98% MÀN HÌNH
                ViewGroup.LayoutParams lpNote = floatingNoteView.getLayoutParams();
                lpNote.width = (int) (parentWidth * 0.98f);
                floatingNoteView.setLayoutParams(lpNote);

                // Đo lại width sau khi set 98%
                floatingNoteView.measure(
                        View.MeasureSpec.makeMeasureSpec(parentWidth, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(parentHeight, View.MeasureSpec.AT_MOST)
                );
                int noteWidth = floatingNoteView.getMeasuredWidth();
                if (noteWidth <= 0) noteWidth = floatingNoteView.getWidth();
                if (noteWidth <= 0) noteWidth = (int) (parentWidth * 0.98f);

                int paddingH = (int) (8 * density);
                int maxTextWidth = noteWidth - paddingH * 2;

                Paint paint = new Paint();
                paint.setTypeface(edtFloatingNote.getTypeface());

                float maxSp = 38f;
                float minSp = 14f;
                float stepSp = 1f;

                float chosenSp = minSp;
                for (float sp = maxSp; sp >= minSp; sp -= stepSp) {
                    float px = spToPx(sp);
                    paint.setTextSize(px);
                    float w = paint.measureText(text);
                    if (w <= maxTextWidth) {
                        chosenSp = sp;
                        break;
                    }
                }

                edtFloatingNote.setTextSize(TypedValue.COMPLEX_UNIT_SP, chosenSp);
                edtFloatingNote.setPadding(paddingH, (int) (4 * density), paddingH, (int) (4 * density));

                ViewGroup.LayoutParams p = edtFloatingNote.getLayoutParams();
                p.width = ViewGroup.LayoutParams.MATCH_PARENT;
                edtFloatingNote.setLayoutParams(p);

                if (autoScaleWatcher != null) {
                    edtFloatingNote.removeTextChangedListener(autoScaleWatcher);
                    autoScaleWatcher = null;
                }
            } else {
                // ===== MOBILE (giữ nguyên) =====
                edtFloatingNote.setSingleLine(true);
                edtFloatingNote.setMaxLines(1);

                autoScaleTextIfNeeded(edtFloatingNote, parentWidth, scaleX);

                int paddingAll = (int) (4 * density);
                edtFloatingNote.setPadding(paddingAll, paddingAll, paddingAll, paddingAll);

                if (autoScaleWatcher != null) {
                    edtFloatingNote.removeTextChangedListener(autoScaleWatcher);
                }
                autoScaleWatcher = new TextWatcher() {
                    @Override
                    public void afterTextChanged(Editable s) {
                        autoScaleTextIfNeeded(edtFloatingNote, parentWidth, scaleX);
                    }
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                };
                edtFloatingNote.addTextChangedListener(autoScaleWatcher);
            }

            // ===== TÍNH VỊ TRÍ VÀ ANIMATE =====
            floatingNoteView.post(() -> {
                int noteWidth = floatingNoteView.getWidth();
                int noteHeight = floatingNoteView.getHeight();

                if (noteWidth <= 0 || noteHeight <= 0) {
                    return;
                }

                floatingNoteView.setPivotX(noteWidth / 2f);
                floatingNoteView.setPivotY(noteHeight / 2f);

                float centerX = (parentWidth - noteWidth) / 2f;
                float targetY = parentHeight * 0.47f - noteHeight / 2f;
                if (targetY < 0) {
                    targetY = (parentHeight - noteHeight) / 2f;
                }

                if (isPcMode) {
                    floatingNoteView.animate()
                            .x(centerX)
                            .y(targetY)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(200)
                            .start();
                } else {
                    floatingNoteView.animate()
                            .x(centerX)
                            .y(targetY)
                            .scaleX(scaleX)
                            .scaleY(scaleY)
                            .setDuration(200)
                            .start();
                }
            });
        });
    }





    /**
     * ⭐ AUTO-SCALE TEXT CHỈ KHI VỰT 2 BIÊN MÀN HÌNH (SAU KHI ZOOM)
     */
    private void autoScaleTextIfNeeded(EditText editText, int parentWidth, float scaleX) {
        String text = editText.getText().toString();
        if (text.isEmpty()) {
            editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
            editText.setHorizontallyScrolling(false);
            return;
        }

        int cursorPosition = editText.getSelectionStart();
        float density = getResources().getDisplayMetrics().density;

        // ✅✅✅ TEXT CỰC DÀI (> 150 CHARS) → SCROLL NGANG ✅✅✅
        if (text.length() > 150) {
            Log.d("AutoScale", "⚠️ Text cực dài (" + text.length() + " chars), bật scroll");

            editText.setHorizontallyScrolling(true);
            editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);

            ViewGroup.LayoutParams params = editText.getLayoutParams();
            params.width = (int) ((parentWidth * 0.95f) / scaleX);
            editText.setLayoutParams(params);

            restoreCursorPosition(editText, cursorPosition);

            editText.post(() -> {
                Layout layout = editText.getLayout();
                if (layout != null) {
                    int cursorX = (int) layout.getPrimaryHorizontal(editText.getSelectionStart());
                    editText.scrollTo(Math.max(0, cursorX - editText.getWidth() / 2), 0);
                }
            });

            return;
        }

        // ✅ TEXT BÌNH THƯỜNG
        editText.setHorizontallyScrolling(false);

        // ===== NHÁNH PC MODE: cho text to hơn, co ít hơn =====
        if (isPcMode) {
            // Cho PC: default to 24sp, chỉ co nếu thật sự quá rộng
            float maxSp = 26f;       // text ngắn sẽ dùng size này
            float minSp = 14f;       // không co quá nhỏ trên PC
            float stepSp = 1f;

            // Dùng gần như full width note (không chia scaleX vì PC không scale thêm)
            int paddingExtra = editText.getPaddingLeft() + editText.getPaddingRight()
                    + (int) (16 * density); // padding nhỏ hơn mobile một chút
            int maxAllowedWidth = (int) (parentWidth * 0.9f) - paddingExtra;

            Paint paint = new Paint();
            paint.setTypeface(editText.getTypeface());

            float chosenPx = spToPx(maxSp);
            for (float sp = maxSp; sp >= minSp; sp -= stepSp) {
                float px = spToPx(sp);
                paint.setTextSize(px);
                float w = paint.measureText(text);
                if (w <= maxAllowedWidth) {
                    chosenPx = px;
                    break;
                }
            }

            editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, chosenPx);

            ViewGroup.LayoutParams params = editText.getLayoutParams();
            // cho width wrap để khung ôm vừa text (đã có padding)
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            editText.setLayoutParams(params);

            restoreCursorPosition(editText, cursorPosition);
            return;
        }

        // ===== MOBILE MODE: giữ nguyên logic cũ =====
        int paddingExtra = editText.getPaddingLeft() + editText.getPaddingRight()
                + (int) (32 * density);
        int maxAllowedWidth = (int) ((parentWidth * 0.95f) / scaleX) - paddingExtra;

        float textSize = 24 * density;
        float minTextSize = 8 * density;  // ✅ Giảm từ 12sp

        Paint paint = new Paint();
        paint.setTypeface(editText.getTypeface());
        paint.setTextSize(textSize);
        float textWidth = paint.measureText(text);

        if (textWidth <= maxAllowedWidth) {
            editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);

            ViewGroup.LayoutParams params = editText.getLayoutParams();
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            editText.setLayoutParams(params);

            restoreCursorPosition(editText, cursorPosition);
            return;
        }

        while (textWidth > maxAllowedWidth && textSize > minTextSize) {
            textSize -= 0.5f * density;
            paint.setTextSize(textSize);
            textWidth = paint.measureText(text);
        }

        editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        ViewGroup.LayoutParams params = editText.getLayoutParams();
        params.width = (int) textWidth + (int) (16 * density);
        editText.setLayoutParams(params);

        restoreCursorPosition(editText, cursorPosition);
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }

    /**
     * ⭐ RESTORE CURSOR POSITION AN TOÀN
     */
    private void restoreCursorPosition(EditText editText, int position) {
        try {
            int textLength = editText.getText().length();
            if (position >= 0 && position <= textLength) {
                editText.setSelection(position);
            } else if (position > textLength) {
                editText.setSelection(textLength);
            }
        } catch (Exception e) {
            Log.e("AutoScale", "Lỗi restore cursor: " + e.getMessage());
        }
    }

    /**
     * ⭐ CẬP NHẬT LẠI VỊ TRÍ CENTER SAU KHI LAYOUT THAY ĐỔI
     */
    private void recenterFloatingNote(int parentWidth) {
        if (floatingNoteView == null) return;

        floatingNoteView.post(() -> {
            ConstraintLayout rootLayout = findViewById(R.id.rootLayout);
            if (rootLayout == null) return;

            int parentHeight = rootLayout.getHeight();
            int noteWidth = floatingNoteView.getWidth();
            int noteHeight = floatingNoteView.getHeight();

            if (noteWidth <= 0 || noteHeight <= 0) {
                return;
            }

            // ✅ Tính lại vị trí center
            float centerX = (parentWidth - noteWidth) / 2f;
            float targetY = parentHeight * 0.51f - noteHeight / 2f;

            if (targetY < 0) {
                targetY = (parentHeight - noteHeight) / 2f;
            }

            // ✅ Cập nhật vị trí KHÔNG dùng animation (để smooth)
            floatingNoteView.setX(centerX);
            floatingNoteView.setY(targetY);

            Log.d("Recenter", "Note size: " + noteWidth + "×" + noteHeight);
            Log.d("Recenter", "Position: " + centerX + "," + targetY);
        });
    }

    /**
     * ⭐ TỰ ĐỘNG SCALE TEXT SIZE ĐỂ VỪA KHUNG (KHÔNG XUỐNG DÒNG)
     */
    private void autoScaleTextToFit(EditText editText, int maxWidth) {
        String text = editText.getText().toString();
        if (text.isEmpty()) {
            editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);  // Size mặc định
            return;
        }

        Paint paint = new Paint();
        float density = getResources().getDisplayMetrics().density;

        // Bắt đầu từ size lớn (24sp)
        float textSize = 24 * density;
        float minTextSize = 12 * density;  // Size tối thiểu

        paint.setTextSize(textSize);
        float textWidth = paint.measureText(text);

        // ✅ Giảm dần text size cho đến khi vừa khung
        while (textWidth > maxWidth && textSize > minTextSize) {
            textSize -= 0.5f * density;  // Giảm 0.5sp mỗi lần
            paint.setTextSize(textSize);
            textWidth = paint.measureText(text);
        }

        // Set text size mới (đơn vị PX)
        editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
    }















    @SuppressLint("ClickableViewAccessibility")
    private View.OnTouchListener createFabColumnDragTouchListener() {
        return new View.OnTouchListener() {
            private float dX, dY;
            private float downRawX, downRawY;
            private boolean isDragging = false;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (fabReplayColumn == null) return false;

                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downRawX = event.getRawX();
                        downRawY = event.getRawY();
                        dX = fabReplayColumn.getX() - downRawX;
                        dY = fabReplayColumn.getY() - downRawY;
                        isDragging = false;
                        isDraggingFab = false;
                        return true;

                    case MotionEvent.ACTION_MOVE: {
                        float moveRawX = event.getRawX();
                        float moveRawY = event.getRawY();

                        float diffX = moveRawX - downRawX;
                        float diffY = moveRawY - downRawY;

                        if (!isDragging &&
                                (Math.abs(diffX) > DRAG_SLOP || Math.abs(diffY) > DRAG_SLOP)) {
                            isDragging = true;
                            isDraggingFab = true;
                        }

                        if (isDragging) {
                            float newX = moveRawX + dX;
                            float newY = moveRawY + dY;

                            View parent = (View) fabReplayColumn.getParent();
                            int parentWidth = parent.getWidth();
                            int parentHeight = parent.getHeight();

                            int viewWidth = fabReplayColumn.getWidth();
                            int viewHeight = fabReplayColumn.getHeight();

                            newX = Math.max(0, Math.min(newX, parentWidth - viewWidth));
                            newY = Math.max(0, Math.min(newY, parentHeight - viewHeight));

                            fabReplayColumn.setX(newX);
                            fabReplayColumn.setY(newY);
                        }

                        return true;
                    }

                    case MotionEvent.ACTION_UP: {
                        if (isDragging) {
                            SharedPreferences fp = getSharedPreferences(PREFS_FLOATING, MODE_PRIVATE);
                            fp.edit()
                                    .putFloat(KEY_FAB_X, fabReplayColumn.getX())
                                    .putFloat(KEY_FAB_Y, fabReplayColumn.getY())
                                    .apply();

                            isDragging = false;
                            view.postDelayed(() -> isDraggingFab = false, 50);
                            return true;
                        }

                        isDraggingFab = false;

                        // Không drag → cho phép VIEW xử lý tiếp (để onClick chạy)
                        view.performClick();  // gọi click cho FAB (loop/toggle)
                        return true;
                    }

                    case MotionEvent.ACTION_CANCEL:
                        isDragging = false;
                        isDraggingFab = false;
                        return true;
                }
                return false;
            }
        };
    }

    private void initLockTouchListener() {
        lockTouchCheckedChangeListener = (buttonView, isChecked) -> {
            Log.d("LOCK_TOUCH", "onCheckedChanged: isChecked=" + isChecked
                    + ", panelVis=" + (switchPanel != null ? switchPanel.getVisibility() : -1));

            setLockTouchEnabled(isChecked, false);

            if (isChecked && switchPanel != null && switchPanel.getVisibility() == View.VISIBLE) {
                Log.d("LOCK_TOUCH", "toggleSwitchPanel() called from lock");
                toggleSwitchPanel(); // bật lock → tự thu gọn panel
            }
        };
    }




    private void restoreSelectedLessonIfNeeded() {
        if (currentTopicOnLockscreen == null) return;
        if (lockCurrentLessonKeys == null || lockCurrentLessonKeys.isEmpty()) return;

        List<Sentence> fullList = loadSentencesForTopicPreviewOnLockScreen(currentTopicOnLockscreen);
        if (fullList == null || fullList.isEmpty()) return;

        List<Sentence> restored = new ArrayList<>();

        for (String lessonKey : lockCurrentLessonKeys) {
            if (TextUtils.isEmpty(lessonKey)) continue;

            if (lessonKey.startsWith("topic_")) {
                restored.addAll(fullList);
                continue;
            }

            for (int i = 0; i < fullList.size(); i++) {
                Sentence s = fullList.get(i);
                LessonInfo parsed = TopicTreeManager.parseLessonInfoFromSentence(s, i);

                if (parsed != null && lessonKey.equals(parsed.key)) {
                    restored.add(s);
                }
            }
        }

        if (restored.isEmpty()) {
            Log.e("LOCK_RESTORE",
                    "restoreSelectedLessonIfNeeded empty"
                            + " | keys=" + lockCurrentLessonKeys
                            + " | topic=" + currentTopicOnLockscreen.name);
            return;
        }

        sentences.clear();
        sentences.addAll(restored);

        for (int i = 0; i < sentences.size(); i++) {
            Sentence s = sentences.get(i);
            if (s != null && s.id == -1) {
                s.id = i;
            }
        }

        restoreCurrentLockscreenPosition();

        if (currentSentence == null && !sentences.isEmpty()) {
            currentSentence = sentences.get(0);
        }

        if (currentSentence != null) {
            currentAudioEnFile = currentSentence.audio;
            currentAudioViFile = currentSentence.audio_vi;

            updateCardViewWithSentence(currentSentence);
            updateNowPlayingHeader();
            updateNowPlayingInfo(currentSentence);
            syncSequentialIndexWithCurrentSentence();
        }

        updateStats();
        updateSrsStatsBar();

        Log.d("LOCK_RESTORE",
                "Restored selected lesson/list"
                        + " | size=" + sentences.size()
                        + " | label=" + lockCurrentSelectionLabel
                        + " | keys=" + lockCurrentLessonKeys);
    }







    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e("LockScreenActivity", "### onCreate CALLED ###");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_screen);

        // =========================================================
// KHÔI PHỤC THỜI GIAN NGHỈ GIỮA CÁC LẦN LOOP AUDIO
// =========================================================
        SharedPreferences audioLoopPrefs =
                getSharedPreferences(
                        PREFS_AUDIO_LOOP,
                        MODE_PRIVATE
                );

        delayBetweenAudioLoopMs =
                audioLoopPrefs.getInt(
                        KEY_LOOP_DELAY_MS,
                        DEFAULT_AUDIO_LOOP_DELAY_MS
                );

        audioViVolume =
                audioLoopPrefs.getFloat(
                        KEY_AUDIO_VI_VOLUME,
                        DEFAULT_AUDIO_VI_VOLUME
                );


        restoreLockscreenSelectionState();


        audioPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) return;

                    selectedAudioUri = uri;

                    String name = getFileNameFromUri(uri);
                    selectedAudioFileName = name;

                    if (currentAudioTextView != null) {
                        currentAudioTextView.setText(name != null ? name : "Đã chọn file audio");
                    }
                }
        );

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) return;
                    selectedImageUri = uri;

                    if (currentEditImageView != null) {
                        try {
                            Bitmap bm = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                            loadImageWithRoundedCorners(currentEditImageView, bm);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
        );

        // ⭐ Đọc idle timeout (giây) từ PREFS_SETTINGS = "settings"
        SharedPreferences settingsPrefs = getSharedPreferences("settings", MODE_PRIVATE);
        int inactivityTimeoutSeconds = settingsPrefs.getInt("inactivity_timeout_seconds", 60);
        INACTIVITY_TIMEOUT_MS = inactivityTimeoutSeconds * 1000L;

        // ⭐ Bắt đầu đếm từ lúc vào LockScreen
        startInactivityTimer();

        // Ẩn cursor chuột trong LockScreenActivity
        View root = findViewById(android.R.id.content);
        if (root != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                PointerIcon nullIcon = PointerIcon.getSystemIcon(this, PointerIcon.TYPE_NULL);
                root.setPointerIcon(nullIcon);
            }
        }

        ensureSentencesJsonExists();
        Log.d("REMINDER", "📍 onCreate: About to schedule alarms...");
        scheduleDailyReminder();
        Log.d("REMINDER", "📍 onCreate: Alarms scheduled");

        // ⭐ Reset quota new/review nếu sang ngày mới
        resetDailyCountersIfNeeded();

        // Hiển thị trên màn hình khóa
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            );
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // KHỞI TẠO SEARCHVIEW TRƯỚC
        searchView = findViewById(R.id.searchView);
        if (searchView == null) {
            Log.e("DEBUG_SEARCH", "searchView is NULL sau findViewById");
        } else {
            Log.d("DEBUG_SEARCH", "searchView findViewById OK, class=" + searchView.getClass().getSimpleName());
        }

        // ====== CÁC SWITCH SETTINGS DÙNG PREFS_APP_SETTINGS ======
        switchPlayViOnNext = findViewById(R.id.switchPlayViOnNext);
        if (switchPlayViOnNext != null) {
            SharedPreferences appPrefsLocal = getSharedPreferences(PREFS_APP_SETTINGS, MODE_PRIVATE);
            boolean playVi = appPrefsLocal.getBoolean(KEY_PLAY_VI_AUDIO, true);

            switchPlayViOnNext.setChecked(playVi);
            switchPlayViOnNext.setOnCheckedChangeListener((buttonView, isChecked) -> {
                appPrefsLocal.edit().putBoolean(KEY_PLAY_VI_AUDIO, isChecked).apply();

                Intent syncIntent = new Intent("com.OPD2nd.popup.SETTINGS_CHANGED");
                syncIntent.putExtra("key", KEY_PLAY_VI_AUDIO);
                syncIntent.putExtra("value", isChecked);
                sendBroadcast(syncIntent);
            });
        }

        switchWaitEnBeforeNext = findViewById(R.id.switchWaitEnBeforeNext);
        if (switchWaitEnBeforeNext != null) {
            SharedPreferences appPrefsLocal = getSharedPreferences(PREFS_APP_SETTINGS, MODE_PRIVATE);
            boolean waitEnBeforeNext = appPrefsLocal.getBoolean(KEY_WAIT_EN_BEFORE_NEXT, true);
            Log.d("SRS_DEBUG", "goNextWithRating: waitEnBeforeNext=" + waitEnBeforeNext);

            switchWaitEnBeforeNext.setOnCheckedChangeListener(null);
            switchWaitEnBeforeNext.setChecked(waitEnBeforeNext);
            switchWaitEnBeforeNext.setOnCheckedChangeListener((buttonView, isChecked) -> {
                appPrefsLocal.edit()
                        .putBoolean(KEY_WAIT_EN_BEFORE_NEXT, isChecked)
                        .apply();
            });
        }

        switchFocusFloatingNote = findViewById(R.id.switchFocusFloatingNote);
        if (switchFocusFloatingNote != null) {
            SharedPreferences appPrefsLocal = getSharedPreferences(PREFS_APP_SETTINGS, MODE_PRIVATE);
            boolean focusNoteOnNext = appPrefsLocal.getBoolean(KEY_FOCUS_NOTE_ON_NEXT, false);
            switchFocusFloatingNote.setChecked(focusNoteOnNext);

            switchFocusFloatingNote.setOnCheckedChangeListener((buttonView, isChecked) -> {
                appPrefsLocal.edit()
                        .putBoolean(KEY_FOCUS_NOTE_ON_NEXT, isChecked)
                        .apply();
            });
        }

        // Tự ẩn Lock Touch Bubble
        globalLockGestureDetector = new GestureDetector(
                this,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true;
                    }

                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        if (isTouchLocked && lockTouchBubble != null) {
                            animateBubbleShow();
                            scheduleHideLockBubbleDelayed();
                            vibrateShort();
                        }
                        return true;
                    }
                }
        );

        // ====== LOCK TOUCH (remote mode) ======
        lockTouchBubble   = findViewById(R.id.lockTouchBubble);
        switchLockTouch   = findViewById(R.id.switchLockTouch);
        switchPanel       = findViewById(R.id.switchPanel);
        switchHeader      = findViewById(R.id.switchHeader);
        ivSwitchArrow     = findViewById(R.id.ivSwitchArrow);

        tvTrackCounter = findViewById(R.id.tvTrackCounter);
        tvNowPlaying = findViewById(R.id.tvNowPlaying);
        setupNowPlayingMarquee();

        // Trạng thái ban đầu panel: GONE
        if (switchPanel != null) {
            switchPanel.setVisibility(View.GONE);
        }
        switchPanelExpanded = false;
        if (ivSwitchArrow != null) {
            ivSwitchArrow.setRotation(0f);
        }
        if (switchHeader != null) {
            switchHeader.setOnClickListener(v -> toggleSwitchPanel());
        }

        // Prefs chung
        SharedPreferences prefs = getSharedPreferences(PREFS_APP_SETTINGS, MODE_PRIVATE);

        // Tạo listener lock touch
        initLockTouchListener();

        // Đọc trạng thái lock
        boolean lockTouch = prefs.getBoolean("lock_touch_mode", false);

        // Gán listener + state cho switch
        if (switchLockTouch != null) {
            switchLockTouch.setOnCheckedChangeListener(lockTouchCheckedChangeListener);
            switchLockTouch.setChecked(lockTouch);
        }

        // Sync isTouchLocked + bubble
        setLockTouchEnabled(lockTouch, true);

        switchAutoMode = findViewById(R.id.switchAutoMode);
        if (switchAutoMode != null) {
            switchAutoMode.setChecked(false);

            switchAutoMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isChecked) {
                    return;
                }

                boolean hasAdmin = isDeviceAdminActive();
                if (!hasAdmin) {
                    requestDeviceAdminIfNeeded();
                }

                stopAllLoopsAndTimers();
                Log.d("AUTO_MODE",
                        "Auto clicked"
                                + " | topic=" + (currentTopicOnLockscreen != null ? currentTopicOnLockscreen.name : "null")
                                + " | file=" + (currentTopicOnLockscreen != null ? currentTopicOnLockscreen.fileName : "null")
                                + " | sentences=" + (sentences != null ? sentences.size() : -1)
                                + " | label=" + lockCurrentSelectionLabel);

                startLearningEnglishOnlySafe();

                SharedPreferences sleepPrefs = getSharedPreferences("settings", MODE_PRIVATE);
                int sleepMinutes = sleepPrefs.getInt("auto_sleep_minutes", 60);

                Log.d("LockScreenActivity", "Auto mode: sleepMinutes=" + sleepMinutes);

                LearningMediaService.startSleepTimer(LockScreenActivity.this, sleepMinutes);

                playAutoModeOnSound();
                Toast.makeText(LockScreenActivity.this,
                        "Auto EN-only, Sleep sau " + sleepMinutes + " phút",
                        Toast.LENGTH_SHORT).show();

                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory(Intent.CATEGORY_HOME);
                homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(homeIntent);

                if (hasAdmin) {
                    DevicePolicyManager dpm =
                            (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
                    if (dpm != null) {
                        dpm.lockNow();
                    }
                }

                finish();
            });
        }

        // ⭐ Nút mở Typing mode
        ImageButton btnTypingMode = findViewById(R.id.btnTypingMode);
        if (btnTypingMode != null) {
            btnTypingMode.setOnClickListener(v -> {
                Log.d("TypingMode", "btnTypingMode clicked -> mở TypingActivity");
                Intent intent = new Intent(LockScreenActivity.this, TypingActivity.class);
                startActivity(intent);
            });
        } else {
            Log.e("TypingMode", "btnTypingMode là NULL sau findViewById!");

        }




        switchRemoteGesture = findViewById(R.id.switchRemoteGesture);
        SharedPreferences remotePrefs = getSharedPreferences(PREFS_APP_SETTINGS, MODE_PRIVATE);

        boolean remoteGestureEnabled = remotePrefs.getBoolean(KEY_REMOTE_GESTURE, false);

        if (switchRemoteGesture != null) {
            switchRemoteGesture.setChecked(remoteGestureEnabled);

            switchRemoteGesture.setOnCheckedChangeListener((buttonView, isChecked) -> {
                remotePrefs.edit()
                        .putBoolean(KEY_REMOTE_GESTURE, isChecked)
                        .apply();
            });
        }




        // vuốt 4 nút gamepad và tap/double
        setupSwipePriorityAreaGesture();

        // hàng text vn nằm trên hình hỗ trợ typing mode
        tvVietnameseTop = findViewById(R.id.tvVietnameseTop);
        tvVietnamese    = findViewById(R.id.tvVietnamese);
        tvEnglish       = findViewById(R.id.tvEnglish);
        enText          = findViewById(R.id.tvEnglish);
        ipaText         = findViewById(R.id.ipaText);

        switchShowEnglish = findViewById(R.id.switchShowEnglish);

        initEnglishGlowEffect();

        if (switchShowEnglish != null && enText != null) {
            SharedPreferences appPrefsLocal =
                    getSharedPreferences(PREFS_APP_SETTINGS, MODE_PRIVATE);

            // ON = hiện English, mặc định hiện
            boolean showEnglish =
                    appPrefsLocal.getBoolean("lock_show_english", true);

            switchShowEnglish.setOnCheckedChangeListener(null);
            switchShowEnglish.setChecked(showEnglish);

            enText.setVisibility(showEnglish ? View.VISIBLE : View.GONE);

            if (ipaText != null) {
                ipaText.setVisibility(showEnglish ? View.VISIBLE : View.GONE);
            }

            switchShowEnglish.setOnCheckedChangeListener((buttonView, isChecked) -> {
                appPrefsLocal.edit()
                        .putBoolean("lock_show_english", isChecked)
                        .apply();

                enText.setVisibility(isChecked ? View.VISIBLE : View.GONE);

                if (ipaText != null) {
                    ipaText.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                }
            });
        }
        // Bubble lock/unlock (FloatingActionButton)
        lockTouchBubble = findViewById(R.id.lockTouchBubble);

        if (lockTouchBubble != null) {
            final GestureDetector bubbleDetector = new GestureDetector(
                    this,
                    new GestureDetector.SimpleOnGestureListener() {
                        @Override
                        public boolean onDown(MotionEvent e) {
                            return true;
                        }

                        @Override
                        public boolean onDoubleTap(MotionEvent e) {
                            Log.d("LOCK_BUBBLE", "onDoubleTap -> TOGGLE LOCK, current=" + isTouchLocked);

                            boolean newState = !isTouchLocked;
                            setLockTouchEnabled(newState, true);

                            return true;
                        }
                    }
            );

            lockTouchBubble.setOnTouchListener(new View.OnTouchListener() {
                float dX, dY;
                boolean isDragging = false;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    bubbleDetector.onTouchEvent(event);

                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            Log.d("LOCK_BUBBLE", "bubble ACTION_DOWN -> cancel hide");
                            cancelHideLockBubble();

                            dX = v.getX() - event.getRawX();
                            dY = v.getY() - event.getRawY();
                            isDragging = false;
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            float newX = event.getRawX() + dX;
                            float newY = event.getRawY() + dY;
                            v.setX(newX);
                            v.setY(newY);
                            isDragging = true;
                            return true;

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            Log.d("LOCK_BUBBLE", "bubble ACTION_UP/CANCEL isDragging=" + isDragging);

                            scheduleHideLockBubbleDelayed();
                            return isDragging;

                        default:
                            return false;
                    }
                }
            });
        }

        // Switch tắt typing tốt nghệp
        enableTypingCheck = prefs.getBoolean(KEY_ENABLE_TYPING_CHECK, true);

        switchTypingCheck = findViewById(R.id.switchTypingCheck);
        if (switchTypingCheck != null) {
            switchTypingCheck.setChecked(enableTypingCheck);

            switchTypingCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
                enableTypingCheck = isChecked;

                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(KEY_ENABLE_TYPING_CHECK, enableTypingCheck);
                editor.apply();
            });
        }

        // ===== KHỞI TẠO CARD & MÀU =====
        cardSentence = findViewById(R.id.cardSentence);
        originalColor = cardSentence.getCardBackgroundColor().getDefaultColor();
        pressedColor = Color.parseColor("#BBDEFB");
        View cardRoot = findViewById(R.id.cardRoot);
        View textBlockBelowImage = findViewById(R.id.textBlockBelowImage);

        // ===== GESTURE DETECTOR CHUNG CHO TOÀN CARD =====
        cardGestureDetector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {
                    private static final int SWIPE_THRESHOLD = 15;
                    private static final int SWIPE_VELOCITY_THRESHOLD = 10;

                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true;
                    }

                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent e) {
                        startInactivityTimer();

                        if (currentSentence == null) {
                            return true;
                        }

                        Log.d(
                                "CARD_SINGLE_TAP",
                                "Single tap card -> toggle example audio loop"
                                        + " | en=" + currentSentence.en
                                        + " | exampleAudio=" + currentSentence.example_audio
                        );

                        /*
                         * Single tap CardView:
                         * bật/tắt loop example_audio.
                         */
                        toggleExampleAudioLoopFromDoubleTap();

                        /*
                         * Giữ hiệu ứng nhấn nhẹ cho CardView.
                         */
                        runCardTapEffect();

                        return true;
                    }




                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        startInactivityTimer();

                        if (currentSentence == null) {
                            return true;
                        }

                        showEnglishHideVietnamese();
                        showInlineExampleIfAny();

                        toggleExampleAudioLoopFromDoubleTap();

                        shrinkImageForAnswer();

                        return true;
                    }

                    @Override
                    public void onLongPress(MotionEvent e) {
                        if (currentSentence == null) {
                            return;
                        }

                        startInactivityTimer();

                        Log.d(
                                "CARD_LONG_PRESS",
                                "Long press card -> rating AGAIN"
                                        + " | en=" + currentSentence.en
                        );

                        // Dừng các audio loop đang chạy trước khi rating
                        if (isExampleLooping) {
                            isExampleLooping = false;
                            isAudioLooping = false;

                            stopAudioLoop();
                            resetExampleLoopState();

                        } else if (isAudioLooping) {
                            isAudioLooping = false;
                            stopAudioLoop();
                        }

                        resetReplayFabState();

                        // 0 = Again
                        performRatingAction(0);

                        // Hiệu ứng nhấn nhẹ giống khi thao tác trên ảnh
                        runCardTapEffect();
                    }

                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2,
                                           float velocityX, float velocityY) {
                        if (e1 == null || e2 == null) return false;

                        float diffX = e2.getX() - e1.getX();
                        float diffY = e2.getY() - e1.getY();

                        if (Math.abs(diffY) > Math.abs(diffX)
                                && Math.abs(diffY) > SWIPE_THRESHOLD
                                && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {

                            if (diffY < 0) {
                                resetReplayFabState();
                                if (currentSentence != null) {
                                    showNextSentence();
                                }
                            } else {
                                resetReplayFabState();
                                showPrevSentence();
                            }
                            return true;
                        }

                        if (Math.abs(diffX) > Math.abs(diffY)
                                && Math.abs(diffX) > SWIPE_THRESHOLD
                                && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {

                            resetReplayFabState();
                            if (diffX > 0) {
                                showPrevSentence();
                            } else {
                                showNextSentence();
                            }
                            return true;
                        }

                        return false;
                    }
                });

        // ===== TOUCH LISTENER CHO TEXT BLOCK BELOW IMAGE =====
        View.OnTouchListener textBlockTouchListener = (v, event) -> {
            ViewParent parent = v.getParent();
            if (parent != null) {
                parent.requestDisallowInterceptTouchEvent(true);
            }

            cardGestureDetector.onTouchEvent(event);

            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_UP
                    || action == MotionEvent.ACTION_CANCEL) {
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(false);
                }
            }

            return true;
        };

        if (textBlockBelowImage != null) {
            textBlockBelowImage.setOnTouchListener(textBlockTouchListener);
        }

        // ===== Touch cho cardRoot =====
        if (cardRoot != null) {
            cardRoot.setOnTouchListener((v, event) -> {
                ViewParent parent = v.getParent();
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(true);
                }

                cardGestureDetector.onTouchEvent(event);

                int action = event.getActionMasked();
                if (action == MotionEvent.ACTION_UP
                        || action == MotionEvent.ACTION_CANCEL) {
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(false);
                    }
                }
                return true;
            });
        }






        // ===== CỤM EXAMPLE CONTAINER & GESTURE RIÊNG =====
        exampleContainer   = findViewById(R.id.exampleContainer);
        tvExampleEnInline  = findViewById(R.id.tvExampleEnInline);
        tvExampleViInline  = findViewById(R.id.tvExampleViInline);

        if (exampleContainer != null) {
            exampleContainer.setOnTouchListener(new View.OnTouchListener() {
                float downX, downY;
                boolean isSwipe = false;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            downX = event.getX();
                            downY = event.getY();
                            isSwipe = false;
                            cardGestureDetector.onTouchEvent(event);
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            float dx = event.getX() - downX;
                            float dy = event.getY() - downY;

                            if (Math.abs(dx) > 25 || Math.abs(dy) > 25) {
                                isSwipe = true;
                                cardGestureDetector.onTouchEvent(event);
                                return true;
                            }
                            return true;

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            if (isSwipe) {
                                cardGestureDetector.onTouchEvent(event);
                                return true;
                            }
                            return false;
                    }
                    return false;
                }
            });
        }

        // HÀM: SINGLE TAP → LOOP example_audio (EN)
        Runnable exampleSingleTapAction = () -> {
            if (currentSentence == null) return;

            if (exampleContainer != null) {
                animateExampleTap(exampleContainer);
            }

            if (TextUtils.isEmpty(currentSentence.example_audio)) {
                Toast.makeText(LockScreenActivity.this,
                        "Không có example audio cho câu này.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (isExampleLooping) {
                isExampleLooping = false;
                isAudioLooping   = false;
                stopAudioLoop();
                resetExampleLoopState();
                return;
            }

            isAudioLooping   = false;
            stopAudioLoop();

            isExampleLooping = true;
            isAudioLooping   = true;
            startExampleAudioLoop();
        };

// HÀM: DOUBLE TAP → PLAY example_audio_vi 1 lần
        Runnable exampleDoubleTapAction = () -> {
            if (currentSentence == null) return;

            // Đặt đúng field cho audio VI của example:
            String viAudio = currentSentence.example_audio_vi; // nếu bạn đặt khác (audio_vi) thì sửa tên này

            if (TextUtils.isEmpty(viAudio)) {
                Toast.makeText(LockScreenActivity.this,
                        "Không có example VI audio cho câu này.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            isExampleLooping = false;
            isAudioLooping   = false;
            stopAudioLoop();

            playAudio(viAudio, false, null);
        };

// CLICK LISTENER: tự phân biệt single vs double tap
        View.OnClickListener exampleClickListener = v -> {
            long now = System.currentTimeMillis();

            if (now - lastExampleTapTime <= EXAMPLE_DOUBLE_TAP_TIMEOUT) {
                // TAP THỨ 2 TRONG TIMEOUT → DOUBLE TAP
                lastExampleTapTime = 0L;
                pendingSingleExampleTap = false;

                Log.d("EXAMPLE_TAP", "DOUBLE TAP → play example VI");
                exampleDoubleTapAction.run();
            } else {
                // TAP THỨ 1 → set pending single, chờ xem có tap thứ 2 không
                lastExampleTapTime = now;
                pendingSingleExampleTap = true;

                handler.postDelayed(() -> {
                    if (pendingSingleExampleTap
                            && System.currentTimeMillis() - lastExampleTapTime >= EXAMPLE_DOUBLE_TAP_TIMEOUT) {
                        Log.d("EXAMPLE_TAP", "SINGLE TAP → loop example EN");
                        pendingSingleExampleTap = false;
                        exampleSingleTapAction.run();
                    }
                }, EXAMPLE_DOUBLE_TAP_TIMEOUT + 20);
            }
        };



// =========================================================
// LISTENER RIÊNG CHO EXAMPLE EN / VI
// Không chuyển touch sang cardGestureDetector,
// tránh double tap example kích hoạt luôn double tap của card.
// =========================================================

        if (tvExampleEnInline != null) {
            tvExampleEnInline.setClickable(true);
            tvExampleEnInline.setLongClickable(true);

            // Chỉ dùng click listener riêng của example
            tvExampleEnInline.setOnClickListener(exampleClickListener);

            tvExampleEnInline.setOnLongClickListener(v -> {
                showEditExampleDialog(tvExampleEnInline, true);
                return true;
            });
        }

        if (tvExampleViInline != null) {
            tvExampleViInline.setClickable(true);
            tvExampleViInline.setLongClickable(true);

            // Chỉ dùng click listener riêng của example
            tvExampleViInline.setOnClickListener(exampleClickListener);

            tvExampleViInline.setOnLongClickListener(v -> {
                showEditExampleDialog(tvExampleViInline, false);
                return true;
            });
        }















        // ✅✅✅ LONG PRESS HÀNG QUOTA ĐỂ MỞ SETTINGS ✅✅✅
        TextView tvSrsStatsQuota = findViewById(R.id.tvSrsStatsQuota);
        if (tvSrsStatsQuota != null) {
            tvSrsStatsQuota.setOnLongClickListener(v -> {
                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        vibrator.vibrate(50);
                    }
                }
                showQuotaSettingsDialog();
                return true;
            });

            tvSrsStatsQuota.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setAlpha(0.6f);
                        v.setScaleX(0.95f);
                        v.setScaleY(0.95f);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.setAlpha(1.0f);
                        v.setScaleX(1.0f);
                        v.setScaleY(1.0f);
                        break;
                }
                return false;
            });
        }
        showQuotaHintIfNeeded();

        // ==== INIT FLOATING NOTE ====
        ConstraintLayout rootLayout = findViewById(R.id.rootLayout);
        LayoutInflater inflater = LayoutInflater.from(this);
        floatingNoteView = inflater.inflate(R.layout.view_floating_note, rootLayout, false);
        floatingNoteRoot = floatingNoteView.findViewById(R.id.floatingNoteRoot);
        floatingNoteView.setScaleX(1f);
        floatingNoteView.setScaleY(1f);

        edtFloatingNote = floatingNoteView.findViewById(R.id.edtFloatingNote);
        edtFloatingNote.setSingleLine(false);
        edtFloatingNote.setRawInputType(InputType.TYPE_CLASS_TEXT);
        edtFloatingNote.setImeOptions(EditorInfo.IME_ACTION_DONE);

        edtFloatingNote.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitFloatingNoteViaEnter();
                return true;
            }
            return false;
        });

        edtFloatingNote.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN) {
                return false;
            }
            return false;
        });

        ImageView btnCloseFloatingNote = floatingNoteView.findViewById(R.id.btnCloseFloatingNote);

        edtFloatingNote.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                recenterFloatingNote();
            }
        });

        floatingNoteView.setVisibility(View.GONE);
        isFloatingNoteVisible = false;
        rootLayout.addView(floatingNoteView);

        rootLayout.post(() -> {
            int parentWidth = rootLayout.getWidth();
            if (parentWidth > 0 && edtFloatingNote != null) {
                float density = getResources().getDisplayMetrics().density;
                int padding = (int) (16 * density);
                int maxWidth = parentWidth - padding * 2;
                if (maxWidth > 0) {
                    edtFloatingNote.setMaxWidth(maxWidth);
                }
            }
        });

        // Nút show phím tắt
        LinearLayout layoutShortcuts = findViewById(R.id.layoutShortcuts);
        if (layoutShortcuts != null) {
            layoutShortcuts.setOnClickListener(v -> showShortcutsDialog());
        }

        ImageButton btnNote = findViewById(R.id.btnNote);
        btnNote.setOnClickListener(v -> toggleFloatingNote());
        btnCloseFloatingNote.setOnClickListener(v -> hideFloatingNote());

        View noteHeader = floatingNoteView.findViewById(R.id.floatingNoteHeader);
        noteHeader.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    noteDx = floatingNoteView.getX() - event.getRawX();
                    noteDy = floatingNoteView.getY() - event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float newX = event.getRawX() + noteDx;
                    float newY = event.getRawY() + noteDy;
                    floatingNoteView.setX(newX);
                    floatingNoteView.setY(newY);
                    return true;
                case MotionEvent.ACTION_UP:
                    noteLastX = floatingNoteView.getX();
                    noteLastY = floatingNoteView.getY();

                    float screenHeight = rootLayout.getHeight();
                    if (noteLastY > screenHeight * 0.75f) {
                        hideFloatingNote();
                    }
                    return true;
            }
            return false;
        });

        // ⭐ Load auto next interval từ settings
        autoNextInterval = prefs.getInt("popup_auto_next_interval", 4);

        // ⭐ Lưu kích thước gốc và 2 mức thu nhỏ cho ảnh
        float density = getResources().getDisplayMetrics().density;
        imageOriginalWidth = (int) (280 * density);
        imageOriginalHeight = (int) (280 * density);

        float SHRINK_RATIO_ON = 0.25f;
        imageShrinkWidthEnOn = (int) (imageOriginalWidth * SHRINK_RATIO_ON);
        imageShrinkHeightEnOn = (int) (imageOriginalHeight * SHRINK_RATIO_ON);

        float SHRINK_RATIO_OFF = 0.6f;
        imageShrinkWidthEnOff = (int) (imageOriginalWidth * SHRINK_RATIO_OFF);
        imageShrinkHeightEnOff = (int) (imageOriginalHeight * SHRINK_RATIO_OFF);

        cardSentence = findViewById(R.id.cardSentence);
        originalColor = cardSentence.getCardBackgroundColor().getDefaultColor();
        pressedColor = Color.parseColor("#BBDEFB");

        exampleContainer = findViewById(R.id.exampleContainer);
        tvExampleEnInline = findViewById(R.id.tvExampleEnInline);
        tvExampleViInline = findViewById(R.id.tvExampleViInline);

        // ================== EXPAND/COLLAPSE NHÓM SWITCH TRÊN CÙNG ==================
        switchPanel = findViewById(R.id.switchPanel);
        switchHeader = findViewById(R.id.switchHeader);
        ivSwitchArrow = findViewById(R.id.ivSwitchArrow);

        setupSwitchTabsAndLessonAutoLoop();
        setupStudyModeSwitch();


        if (switchPanel != null) {
            switchPanel.setVisibility(View.GONE);
        }
        switchPanelExpanded = false;
        if (ivSwitchArrow != null) {
            ivSwitchArrow.setRotation(0f);
        }

        if (switchHeader != null) {
            switchHeader.setOnClickListener(v -> toggleSwitchPanel());
        }

        // ⭐ Nút đánh giá mức độ nhớ (SRS kiểu Anki)
        MaterialButton btnAgain = findViewById(R.id.btnAgain);
        MaterialButton btnHard  = findViewById(R.id.btnHard);
        MaterialButton btnGood  = findViewById(R.id.btnGood);
        MaterialButton btnEasy  = findViewById(R.id.btnEasy);

        View.OnClickListener srsClickListener = v -> {
            if (currentSentence == null) {
                return;
            }

            /*
             * Dừng mọi audio/loop trước khi rating.
             */
            if (isExampleLooping) {
                isExampleLooping = false;
                isAudioLooping = false;

                stopAudioLoop();
                resetExampleLoopState();

            } else if (isAudioLooping) {
                isAudioLooping = false;
                stopAudioLoop();
            }

            resetReplayFabState();

            int rating;
            int id = v.getId();

            if (id == R.id.btnAgain) {
                rating = 0;

            } else if (id == R.id.btnHard) {
                rating = 1;

            } else if (id == R.id.btnGood) {
                rating = 2;

            } else if (id == R.id.btnEasy) {
                rating = 3;

            } else {
                return;
            }

            Log.d(
                    "SRS_DEBUG",
                    "SRS button clicked"
                            + " | en=" + currentSentence.en
                            + " | rating=" + rating
                            + " | lastRatingBefore=" + currentSentence.lastRating
                            + " | againBefore=" + currentSentence.againCount
                            + " | hardBefore=" + currentSentence.hardCount
            );

            /*
             * Chỉ highlight UI.
             *
             * Không tự sửa:
             * - swipePhase
             * - swipeStep
             * - lastRating
             * - againCount
             * - hardCount
             *
             * Toàn bộ state phải được xử lý trong pipeline SRS.
             */
            highlightSelectedButton(
                    (MaterialButton) v,
                    btnAgain,
                    btnHard,
                    btnGood,
                    btnEasy
            );

            handleSrsButton(
                    rating
            );
        };

        if (btnAgain != null) btnAgain.setOnClickListener(srsClickListener);
        if (btnHard  != null) btnHard.setOnClickListener(srsClickListener);
        if (btnGood  != null) btnGood.setOnClickListener(srsClickListener);
        if (btnEasy  != null) btnEasy.setOnClickListener(srsClickListener);

        // ====== PREFS CHO CÁC SWITCH (app_settings + lockscreen_prefs) ======
        SharedPreferences appPrefs = getSharedPreferences(PREFS_APP_SETTINGS, MODE_PRIVATE);
        SharedPreferences lsPrefs = getSharedPreferences("lockscreen_prefs", MODE_PRIVATE);

        // ⭐ Switch ẩn/hiện hình (Pic) trên LockScreen
        imgSentence = findViewById(R.id.imageView);
        imgSentence.post(() -> {
            if (imageOriginalWidth <= 0 || imageOriginalHeight <= 0) {
                imageOriginalWidth = imgSentence.getWidth();
                imageOriginalHeight = imgSentence.getHeight();
                Log.d("IMG_DEBUG", "originalW=" + imageOriginalWidth + " H=" + imageOriginalHeight);
            }
        });
        switchShowImage = findViewById(R.id.switchShowImage);

        boolean showImage = lsPrefs.getBoolean("show_image", true);
        if (switchShowImage != null && imgSentence != null) {
            switchShowImage.setChecked(showImage);
            imgSentence.setVisibility(showImage ? View.VISIBLE : View.GONE);
            switchShowImage.setOnCheckedChangeListener((buttonView, isChecked) -> {
                imgSentence.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                lsPrefs.edit().putBoolean("show_image", isChecked).apply();
            });
        }

        // ⭐ Switch auto play EN audio khi next
        switchAutoPlayOnNext = findViewById(R.id.switchAutoPlayOnNext);
        autoPlayOnNext = appPrefs.getBoolean(KEY_PLAY_EN_AUDIO_ON_NEXT, false);

        if (switchAutoPlayOnNext != null) {
            switchAutoPlayOnNext.setChecked(autoPlayOnNext);
            switchAutoPlayOnNext.setOnCheckedChangeListener((buttonView, isChecked) -> {
                autoPlayOnNext = isChecked;
                appPrefs.edit().putBoolean(KEY_PLAY_EN_AUDIO_ON_NEXT, isChecked).apply();
            });
        }

        // Switch hiển thị tiếng Việt
        TextView viTextLocal = findViewById(R.id.tvVietnamese);
        viText = viTextLocal;
        switchShowVietnamese = findViewById(R.id.switchShowVietnamese);

        boolean showVi = prefs.getBoolean(KEY_LOCKS_SHOW_VI, true);
        if (switchShowVietnamese != null && viText != null) {
            switchShowVietnamese.setChecked(showVi);
            viText.setVisibility(showVi ? View.VISIBLE : View.GONE);
            switchShowVietnamese.setOnCheckedChangeListener((buttonView, isChecked) -> {
                viText.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                prefs.edit().putBoolean(KEY_LOCKS_SHOW_VI, isChecked).apply();
            });
        }

        // ⭐ Switch show/hide Female/Male buttons
        voiceLoopContainer =
                findViewById(R.id.voiceLoopContainer);
        switchShowVoiceLoop =
                findViewById(R.id.switchShowVoiceLoop);

        boolean showVoiceLoop = appPrefs.getBoolean(KEY_SHOW_VOICE_LOOP, true);
        if (voiceLoopContainer != null) {
            voiceLoopContainer.setVisibility(showVoiceLoop ? View.VISIBLE : View.GONE);
        }
        if (switchShowVoiceLoop != null) {
            switchShowVoiceLoop.setChecked(showVoiceLoop);
            switchShowVoiceLoop.setOnCheckedChangeListener((buttonView, isChecked) -> {
                appPrefs.edit()
                        .putBoolean(KEY_SHOW_VOICE_LOOP, isChecked)
                        .apply();

                if (voiceLoopContainer != null) {
                    voiceLoopContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                }
            });
        }


        // =========================================================
// AUTO MODE: PHÁT VI RỒI EN
// =========================================================

        switchAutoViThenEn =
                findViewById(R.id.switchAutoViThenEn);

        autoViThenEnEnabled =
                appPrefs.getBoolean(
                        KEY_AUTO_VI_THEN_EN,
                        false
                );

        if (switchAutoViThenEn != null) {
            switchAutoViThenEn.setOnCheckedChangeListener(null);

            switchAutoViThenEn.setChecked(
                    autoViThenEnEnabled
            );

            switchAutoViThenEn.setOnCheckedChangeListener(
                    (buttonView, isChecked) -> {
                        autoViThenEnEnabled =
                                isChecked;

                        appPrefs.edit()
                                .putBoolean(
                                        KEY_AUTO_VI_THEN_EN,
                                        isChecked
                                )
                                .apply();

                        Toast.makeText(
                                LockScreenActivity.this,
                                isChecked
                                        ? "Auto mode: VI → EN"
                                        : "Auto mode: chỉ EN",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
            );
        }




        // =========================================================
// AUTO MODE: PHÁT AUDIO EXAMPLE SAU AUDIO EN
// =========================================================
        switchAutoPlayExample =
                findViewById(
                        R.id.switchAutoPlayExample
                );

        autoPlayExampleEnabled =
                appPrefs.getBoolean(
                        KEY_AUTO_PLAY_EXAMPLE,
                        false
                );

        if (switchAutoPlayExample != null) {
            switchAutoPlayExample.setOnCheckedChangeListener(
                    null
            );

            switchAutoPlayExample.setChecked(
                    autoPlayExampleEnabled
            );

            switchAutoPlayExample.setOnCheckedChangeListener(
                    (buttonView, isChecked) -> {
                        autoPlayExampleEnabled =
                                isChecked;

                        appPrefs.edit()
                                .putBoolean(
                                        KEY_AUTO_PLAY_EXAMPLE,
                                        isChecked
                                )
                                .apply();

                        Toast.makeText(
                                LockScreenActivity.this,
                                isChecked
                                        ? "Auto mode: phát thêm Example"
                                        : "Auto mode: không phát Example",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
            );
        }


        // ⭐ Switch: Play En Male when Next
        switchPlayEnMaleOnNext = findViewById(R.id.switchPlayEnMaleOnNext);
        playEnMaleOnNext = appPrefs.getBoolean(KEY_PLAY_EN_MALE_ON_NEXT, false);

        if (switchPlayEnMaleOnNext != null) {
            switchPlayEnMaleOnNext.setChecked(playEnMaleOnNext);
            switchPlayEnMaleOnNext.setOnCheckedChangeListener((buttonView, isChecked) -> {
                playEnMaleOnNext = isChecked;
                appPrefs.edit()
                        .putBoolean(KEY_PLAY_EN_MALE_ON_NEXT, isChecked)
                        .apply();
            });
        }




        // =========================================================
// SWITCH LISTENING SEQUENTIAL
// =========================================================

        switchListeningSequential =
                findViewById(R.id.switchListeningSequential);

        srsButtonsContainer =
                findViewById(R.id.srsButtonsContainer);

// Đọc trạng thái đã lưu
        listeningSequentialMode =
                appPrefs.getBoolean(
                        KEY_LISTENING_SEQUENTIAL_MODE,
                        false
                );

// Áp dụng trạng thái ban đầu cho cụm nút SRS
        if (srsButtonsContainer != null) {
            srsButtonsContainer.setVisibility(
                    listeningSequentialMode
                            ? View.GONE
                            : View.VISIBLE
            );
        }

        Log.d(
                "LISTENING_SEQ",
                "init: listeningSequentialMode="
                        + listeningSequentialMode
                        + " | srsButtonsContainer="
                        + srsButtonsContainer
        );

        if (switchListeningSequential != null) {

            // Tạm tháo listener trong lúc set trạng thái ban đầu
            switchListeningSequential.setOnCheckedChangeListener(null);

            switchListeningSequential.setChecked(
                    listeningSequentialMode
            );

            switchListeningSequential.setOnCheckedChangeListener(
                    (buttonView, isChecked) -> {

                        // Đồng bộ biến runtime
                        listeningSequentialMode =
                                isChecked;

                        Log.d(
                                "LISTENING_SEQ",
                                "switch changed="
                                        + isChecked
                                        + " | applyingPreset="
                                        + isApplyingStudyModePreset
                                        + " | srsButtonsContainer="
                                        + srsButtonsContainer
                        );

                        // Lưu trạng thái
                        appPrefs.edit()
                                .putBoolean(
                                        KEY_LISTENING_SEQUENTIAL_MODE,
                                        isChecked
                                )
                                .apply();

                        // ON → ẩn nút SRS, OFF → hiện nút SRS
                        if (srsButtonsContainer != null) {
                            srsButtonsContainer.setVisibility(
                                    isChecked
                                            ? View.GONE
                                            : View.VISIBLE
                            );
                        }

                        /*
                         * Nếu switch đang được đổi tự động bởi preset,
                         * dừng ở đây để không chạy thêm logic phụ.
                         */
                        if (isApplyingStudyModePreset) {
                            return;
                        }

                        /*
                         * Chỉ khi người dùng tự bật Sequential
                         * mới khởi tạo lại index.
                         */
                        if (isChecked) {
                            initListeningSequentialIndex();
                        }

                        if (currentSentence != null) {
                            updateNowPlayingHeader();
                            updateNowPlayingInfo(currentSentence);
                        }
                    }
            );
        }



        // ⭐ Floating replay audio kéo thả
        fabReplayColumn  = findViewById(R.id.fabReplayColumn);
        fabReplay        = findViewById(R.id.fabReplay);

        fabReplayColumn.post(() -> {
            fabReplayColumn.bringToFront();
            fabReplayColumn.setElevation(16f);
            fabReplayColumn.setTranslationZ(16f);
        });

        SharedPreferences floatPrefs = getSharedPreferences(PREFS_FLOATING, MODE_PRIVATE);
        fabLastX = floatPrefs.getFloat(KEY_FAB_X, -1f);
        fabLastY = floatPrefs.getFloat(KEY_FAB_Y, -1f);

        fabReplayColumn.post(() -> {
            if (fabLastX >= 0 && fabLastY >= 0) {
                fabReplayColumn.setX(fabLastX);
                fabReplayColumn.setY(fabLastY);
            }
        });

        View.OnTouchListener dragListener = createFabColumnDragTouchListener();
        if (fabReplay != null) {
            fabReplay.setOnTouchListener(dragListener);
        }

        if (fabReplay != null) {
            fabReplay.setOnClickListener(v -> {
                long now = System.currentTimeMillis();
                if (now - lastTapTime <= DOUBLE_TAP_TIMEOUT) {
                    lastTapTime = 0L;
                    toggleReplayLoop();
                } else {
                    lastTapTime = now;
                    playReplayOnce();
                    animateFabSingleTap();
                    if (isLoopingReplay) {
                        isLoopingReplay = false;
                        isAudioLooping = false;
                        stopAudioLoop();
                        fabReplay.setImageResource(R.drawable.ic_replay_once);
                        Toast.makeText(LockScreenActivity.this,
                                "Loop OFF", Toast.LENGTH_SHORT).show();
                        stopLoopVisualEffect();
                    }
                }
            });
        }

        // Gesture cho ImageView
        final GestureDetector imgGestureDetector = new GestureDetector(
                this,
                new GestureDetector.SimpleOnGestureListener() {
                    private static final int SWIPE_THRESHOLD = 30;
                    private static final int SWIPE_VELOCITY_THRESHOLD = 20;

                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true;
                    }

                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent e) {
                        startInactivityTimer();

                        if (currentSentence == null) {
                            return true;
                        }

                        // =========================================================
                        // TAP LẦN ĐẦU: SHOW FULL ĐÁP ÁN
                        // =========================================================
                        if (!hasShownAnswerForCurrentSentence) {

                            /*
                             * Dừng mọi loop đang chạy trước khi mở đáp án.
                             */
                            if (isExampleLooping) {
                                isExampleLooping = false;
                                isAudioLooping = false;

                                stopAudioLoop();
                                resetExampleLoopState();

                            } else if (isAudioLooping) {
                                isAudioLooping = false;
                                stopAudioLoop();
                            }

                            Log.d(
                                    "IMG_SINGLE_TAP",
                                    "First tap image -> show full answer"
                                            + " | en=" + currentSentence.en
                            );

                            /*
                             * Hiện EN, VI, IPA, example và thu nhỏ ảnh
                             * theo đúng flow cũ.
                             */
                            handleTapOrEnterShowAnswer();

                            runCardTapEffect();

                            return true;
                        }

                        // =========================================================
                        // ĐÃ SHOW FULL ĐÁP ÁN: BẬT/TẮT LOOP AUDIO CHÍNH
                        // =========================================================
                        Log.d(
                                "IMG_SINGLE_TAP",
                                "Full answer already shown"
                                        + " -> toggle sentence audio loop"
                                        + " | en=" + currentSentence.en
                                        + " | audio=" + currentSentence.audio
                        );

                        /*
                         * Nếu đang loop example thì dừng trước,
                         * tránh example_audio và audio chính chồng nhau.
                         */
                        if (isExampleLooping) {
                            isExampleLooping = false;
                            isAudioLooping = false;

                            stopAudioLoop();
                            resetExampleLoopState();
                        }

                        /*
                         * Tap tiếp theo vào image:
                         * bật/tắt loop audio chính của câu.
                         */
                        toggleAudioLoopFromUI();

                        /*
                         * Chỉ hiệu ứng nhấn nhẹ.
                         * Không gọi runCardTapEffect() ở nhánh này để tránh
                         * tiếp tục shrink ảnh sau khi đáp án đã hiện.
                         */
                        if (imgSentence != null) {
                            imgSentence.animate()
                                    .scaleX(0.94f)
                                    .scaleY(0.94f)
                                    .alpha(0.85f)
                                    .setDuration(70)
                                    .withEndAction(() ->
                                            imgSentence.animate()
                                                    .scaleX(1f)
                                                    .scaleY(1f)
                                                    .alpha(1f)
                                                    .setDuration(120)
                                                    .start()
                                    )
                                    .start();
                        }

                        return true;
                    }

                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        startInactivityTimer();

                        if (currentSentence == null) {
                            return true;
                        }

                        Log.d(
                                "IMG_DOUBLE_TAP",
                                "Double tap image -> play sentence audio_vi"
                                        + " | en=" + currentSentence.en
                                        + " | audio_vi=" + currentSentence.audio_vi
                        );

                        // Dừng loop example/audio hiện tại trước
                        if (isExampleLooping) {
                            isExampleLooping = false;
                            isAudioLooping = false;
                            stopAudioLoop();
                            resetExampleLoopState();
                        } else {
                            stopAudioLoop();
                        }

                        // Ưu tiên audio_vi của chính câu hiện tại
                        String viAudio = currentSentence.audio_vi;

                        // Fallback sang biến runtime nếu cần
                        if (TextUtils.isEmpty(viAudio)) {
                            viAudio = currentAudioViFile;
                        }

                        if (!TextUtils.isEmpty(viAudio)) {
                            playAudio(
                                    viAudio,
                                    false,
                                    null,
                                    audioViVolume
                            );
                        } else {
                            Toast.makeText(
                                    LockScreenActivity.this,
                                    "Không có audio VI cho câu này.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }

                        runCardTapEffect();

                        return true;
                    }
                    @Override
                    public void onLongPress(MotionEvent e) {
                        if (currentSentence == null) {
                            return;
                        }

                        startInactivityTimer();

                        Log.d(
                                "IMG_LONG_PRESS",
                                "Long press image -> rating AGAIN"
                        );

                        performRatingAction(0);

                        runCardTapEffect();
                    }

                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2,
                                           float velocityX, float velocityY) {
                        float diffX = e2.getX() - e1.getX();
                        float diffY = e2.getY() - e1.getY();

                        if (Math.abs(diffY) > Math.abs(diffX)
                                && Math.abs(diffY) > SWIPE_THRESHOLD
                                && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {

                            if (diffY < 0) {
                                Log.d("GESTURE_DEBUG", "ImageView Swipe UP → NEXT");
                                resetReplayFabState();
                                showNextSentence();
                            } else {
                                Log.d("GESTURE_DEBUG", "ImageView Swipe DOWN → PREV");
                                resetReplayFabState();
                                showPrevSentence();
                            }
                            return true;

                        } else if (Math.abs(diffX) > Math.abs(diffY)
                                && Math.abs(diffX) > SWIPE_THRESHOLD
                                && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {

                            if (diffX > 0) {
                                Log.d("GESTURE_DEBUG", "ImageView Swipe RIGHT → PREV");
                                resetReplayFabState();
                                showPrevSentence();
                            } else {
                                Log.d("GESTURE_DEBUG", "ImageView Swipe LEFT → NEXT");
                                resetReplayFabState();
                                showNextSentence();
                            }
                            return true;
                        }
                        return false;
                    }
                }
        );

        // imgSentence vẫn dùng để set ảnh
        imgSentence = findViewById(R.id.imageView);

        // Container nhận touch
        FrameLayout imageTapArea = findViewById(R.id.imageTapArea);

        if (imageTapArea != null) {
            imageTapArea.setClickable(true);
            imageTapArea.setFocusable(true);
            imageTapArea.setFocusableInTouchMode(true);

            imageTapArea.setOnTouchListener((v, event) -> {
                startInactivityTimer();

                ViewParent parent = v.getParent();
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(true);
                }

                imgGestureDetector.onTouchEvent(event);

                if (event.getActionMasked() == MotionEvent.ACTION_UP
                        || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(false);
                    }
                }
                return true;
            });

            imageTapArea.setOnFocusChangeListener((v, hasFocus) -> {
                isImageFocused = hasFocus;
                Log.d("FOCUS_DEBUG", "imageTapArea focus=" + hasFocus);
            });
        }

        // Gesture cho Nút ô vuông nhỏ giữa 2 thanh dọc
        View swipeCenterTap = findViewById(R.id.swipeCenterTap);
        if (swipeCenterTap != null) {
            swipeCenterTap.setClickable(true);
            swipeCenterTap.setFocusable(true);
            swipeCenterTap.setFocusableInTouchMode(true);

            final GestureDetector centerTapDetector = new GestureDetector(
                    this,
                    new GestureDetector.SimpleOnGestureListener() {
                        @Override
                        public boolean onDown(MotionEvent e) {
                            return true;
                        }

                        @Override
                        public boolean onSingleTapConfirmed(MotionEvent e) {
                            Log.d("CENTER_TAP", "onSingleTapConfirmed → TOGGLE LOCK, current=" + isTouchLocked);
                            boolean newState = !isTouchLocked;
                            setLockTouchEnabled(newState, false);
                            return true;
                        }

                        @Override
                        public boolean onDoubleTap(MotionEvent e) {
                            Log.d("CENTER_TAP", "onDoubleTap → RATING HARD");
                            if (currentSentence != null) {
                                resetReplayFabState();
                                performRatingAction(1);
                            }
                            return true;
                        }
                    }
            );

            swipeCenterTap.setOnTouchListener((v, event) -> {
                startInactivityTimer();

                ViewParent parent = v.getParent();
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(true);
                }

                boolean handled = centerTapDetector.onTouchEvent(event);

                if (event.getActionMasked() == MotionEvent.ACTION_UP
                        || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(false);
                    }
                }

                return true;
            });
        }

        // Nút loop Female/Male
        btnLoopFemale = findViewById(R.id.btnLoopFemale);
        btnLoopMale   = findViewById(R.id.btnLoopMale);

        if (btnLoopFemale != null) {
            btnLoopFemale.setOnClickListener(v -> {
                if (!isFemaleLooping) {
                    startVoiceLoop("female");
                } else {
                    stopVoiceLoop("female");
                }
                updateVoiceLoopButtonsUi();
            });
        }

        if (btnLoopMale != null) {
            btnLoopMale.setOnClickListener(v -> {
                if (!isMaleLooping) {
                    startVoiceLoop("male");
                } else {
                    stopVoiceLoop("male");
                }
                updateVoiceLoopButtonsUi();
            });
        }

        updateVoiceLoopButtonsUi();

        // Nút chọn TOPIC
        MaterialButton btnImport = findViewById(R.id.btnImport);
        btnImport.setOnClickListener(v -> {
            stopAllLoopsAndTimers();
            showLockscreenTopicTreeDialog();
        });

        isAudioLooping = false;
        audioLoopHandler = new Handler(Looper.getMainLooper());

        // Load topic và dữ liệu ban đầu
        String topicId   = lsPrefs.getString("current_topic_id", null);
        String topicFile = lsPrefs.getString("current_topic_file", null);

        if (topicId != null && topicFile != null) {
            TopicInfo t = TopicManager.getTopicById(this, topicId);
            if (t != null) {
                currentTopicOnLockscreen = t;

                String packageTopicName = getTopicPackageNameFromJsonFile(t.fileName);
                topicPackageManager = new TopicPackageManager(this, packageTopicName);

                loadSentencesForTopicOnLockScreen(t);
                restoreTopicStats(t.id);
                lastLoadedLockTopicId = t.id;
                lastLoadedLockTopicFile = t.fileName;
            } else {
                loadSentencesFromAssets();
            }
        } else {
            TopicInfo def = TopicManager.getDefaultTopic(this);
            if (def != null) {
                currentTopicOnLockscreen = def;

                String packageTopicName = getTopicPackageNameFromJsonFile(def.fileName);
                topicPackageManager = new TopicPackageManager(this, packageTopicName);

                loadSentencesForTopicOnLockScreen(def);
                restoreTopicStats(def.id);
                lastLoadedLockTopicId = def.id;
                lastLoadedLockTopicFile = def.fileName;
            } else {
                loadSentencesFromAssets();
            }
        }

        if (sentences != null) {
            for (Sentence s2 : sentences) {
                s2.sessionLearningLaps = 0;
            }
        }

        loadGlobalSentences();
        updateSuggestionList();

        autoAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, suggestionList);
        searchView.setAdapter(autoAdapter);



        switchShowFavorites = findViewById(R.id.switchShowFavorites);
        if (switchShowFavorites != null) {
            boolean favOnly = prefs.getBoolean("locks_favorites_only", false);
            switchShowFavorites.setChecked(favOnly);

            switchShowFavorites.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit()
                        .putBoolean(
                                "locks_favorites_only",
                                isChecked
                        )
                        .apply();

                if (isApplyingStudyModePreset) {
                    return;
                }

                shownHistoryKeys.clear();
                shownHistoryIdx = -1;
                historyCache.clear();

                showSentence(false);
            });
        }


















        // =========================================================
// KHÔI PHỤC TOPIC / LESSON / CÂU HIỆN TẠI
// =========================================================

// Không cho trạng thái Play Male cũ tự phát trong lúc restore
        suppressPresetAutoAudioOnce = true;

        restoreLockscreenSelectionState();
        restoreSelectedLessonIfNeeded();
        restoreCurrentLockscreenPosition();

        if (currentSentence == null
                && sentences != null
                && !sentences.isEmpty()) {

            currentSentence = sentences.get(0);
        }

        ensureCurrentLessonState();

// Đồng bộ audio của câu hiện tại trước khi áp preset
        if (currentSentence != null) {
            currentAudioEnFile = currentSentence.audio;
            currentAudioViFile = currentSentence.audio_vi;
        }

// =========================================================
// TỰ NHẬN DIỆN MODE KHI MỞ ACTIVITY LẦN ĐẦU
// =========================================================

        boolean initialStudyModeApplied = false;

        if (!hasAppliedInitialStudyModePreset
                && sentences != null
                && !sentences.isEmpty()) {

            hasAppliedInitialStudyModePreset = true;
            initialStudyModeApplied = true;

            detectAndApplyStudyModeForCurrentSelection(
                    "onCreate initial selection"
            );
        }

// =========================================================
// HIỂN THỊ LẠI CÂU HIỆN TẠI
// =========================================================

        if (currentSentence != null) {

            /*
             * Nếu preset vừa được áp dụng thì applyStudyModePreset()
             * đã gọi updateCardViewWithSentence() rồi.
             */
            if (!initialStudyModeApplied) {
                updateCardViewWithSentence(currentSentence);
            }

            updateNowPlayingHeader();
            updateNowPlayingInfo(currentSentence);
            syncSequentialIndexWithCurrentSentence();
            saveCurrentLockscreenPosition();
        }

        updateSrsStatsBar();














        // Nút NEXT
        ImageButton btnNext = findViewById(R.id.btnNext);
        btnNext.setOnClickListener(v -> {
            resetReplayFabState();
            showNextSentence();
            updateStats();
        });

        // Nút PREV
        ImageButton btnPrev = findViewById(R.id.btnPrev);
        if (btnPrev != null) {
            btnPrev.setOnClickListener(v -> {
                resetReplayFabState();
                showPrevSentence();
            });
        }

        ImageButton btnSpeaker = findViewById(R.id.btnSpeaker);
        btnSpeaker.setImageResource(R.drawable.ic_speaker);
        btnSpeaker.setOnClickListener(v -> {
            if (currentSentence == null) {
                Toast.makeText(this, "Chưa có câu nào", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(currentSentence.example_audio)) {
                Toast.makeText(this, "Không có example audio cho câu này.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isExampleLooping) {
                isExampleLooping = false;
                isAudioLooping = false;
                stopAudioLoop();
                resetExampleLoopState();
                Toast.makeText(this, "⏸️ Example Loop OFF", Toast.LENGTH_SHORT).show();
            } else {
                stopAudioLoop();
                isExampleLooping = true;
                isAudioLooping = true;
                startExampleAudioLoop();
                Toast.makeText(this, "🔁 Example Loop ON", Toast.LENGTH_SHORT).show();
            }
        });
        btnSpeaker.setOnLongClickListener(v -> {
            showAudioSettingsDialog();
            return true;
        });

        ImageButton btnMute = findViewById(R.id.btnMute);
        btnMute.setImageResource(R.drawable.ic_volume_on);
        btnMute.setOnClickListener(v -> {
            isMuted = !isMuted;
            btnMute.setImageResource(isMuted ? R.drawable.ic_volume_off : R.drawable.ic_volume_on);
            if (currentMediaPlayer != null) {
                float vol = isMuted ? 0f : 1f;
                currentMediaPlayer.setVolume(vol, vol);
            }
        });

        ImageButton btnFavorite = findViewById(R.id.btnFavorite);
        btnFavorite.setOnClickListener(v -> toggleFavoriteCurrentSentence());

        MaterialButton btnExit = findViewById(R.id.btnExit);
        if (btnExit != null) {
            btnExit.setOnClickListener(v -> {
                saveCurrentLockscreenPosition();
                stopAllLoopsAndTimers();
                finishAffinity();
            });
        }









        // SearchView -> SearchActivity
        searchView.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            stopAudioLoop();
            hideKeyboard();

            Intent intent = new Intent(LockScreenActivity.this, SearchActivity.class);
            intent.putExtra("initial_keyword", selected);
            startActivity(intent);
        });

        searchView.setOnClickListener(v -> {
            String keyword = searchView.getText().toString().trim();
            stopAudioLoop();
            hideKeyboard();

            Intent intent = new Intent(LockScreenActivity.this, SearchActivity.class);
            intent.putExtra("initial_keyword", keyword);
            startActivity(intent);
        });

        TextView viTextView = findViewById(R.id.tvVietnamese);

        // Gesture riêng cho text VI
        GestureDetector viTextGestureDetector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {

                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true;
                    }

                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent e) {
                        if (currentSentence == null) return true;

                        if (!hasShownAnswer) {
                            if (isExampleLooping) {
                                isExampleLooping = false;
                                stopAudioLoop();
                                resetExampleLoopState();
                            }
                            handleTapOrEnterShowAnswer();
                            return true;
                        }

                        Log.d("TEXT_GESTURE", "Single tap VI at full answer → play EN audio");

                        if (currentAudioEnFile != null && !currentAudioEnFile.isEmpty()) {
                            playAudio(currentAudioEnFile, false, null);
                        } else if (currentSentence.audio != null && !currentSentence.audio.isEmpty()) {
                            playAudio(currentSentence.audio, false, null);
                        } else {
                            Toast.makeText(LockScreenActivity.this,
                                    "Không có audio EN cho câu này.",
                                    Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    }

                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        if (currentSentence == null) return true;

                        Log.d("TEXT_GESTURE", "Double tap VI → play VI audio");

                        if (!TextUtils.isEmpty(currentAudioViFile)) {

                            playAudio(
                                    currentAudioViFile,
                                    false,
                                    null,
                                    audioViVolume
                            );

                        } else if (currentSentence != null
                                && !TextUtils.isEmpty(currentSentence.audio_vi)) {

                            playAudio(
                                    currentSentence.audio_vi,
                                    false,
                                    null,
                                    audioViVolume
                            );

                        } else {
                            Toast.makeText(
                                    LockScreenActivity.this,
                                    "Không có audio VI cho câu này.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                        return true;
                    }

                    @Override
                    public void onLongPress(MotionEvent e) {
                        if (currentSentence != null) {
                            showEditDialog(currentSentence);
                        }
                    }
                });

        // EN: chỉ delegate swipe/tap chung cho card
        View.OnTouchListener enTextTouchListener = (v, event) -> {
            cardGestureDetector.onTouchEvent(event);
            return false;
        };

        // VI: vừa cho card (swipe), vừa cho viTextGestureDetector (tap/double)
        View.OnTouchListener viTextTouchListener = (v, event) -> {
            boolean handledByCard = cardGestureDetector.onTouchEvent(event);
            boolean handledByText = viTextGestureDetector.onTouchEvent(event);
            return handledByCard || handledByText;
        };

        enText.setOnTouchListener(enTextTouchListener);
        viTextView.setOnTouchListener(viTextTouchListener);

// dùng field srsButtonsContainer, không khai báo lại
        srsButtonsContainer = findViewById(R.id.srsButtonsContainer);
        LinearLayout groupControls       = findViewById(R.id.groupControls);
        LinearLayout exampleContainerLocal    = findViewById(R.id.exampleContainer);

        View.OnTouchListener delegateSwipeToCard = new View.OnTouchListener() {
            float startX, startY;
            boolean isMoving;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getX();
                        startY = event.getY();
                        isMoving = false;
                        cardGestureDetector.onTouchEvent(event);
                        return false;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getX() - startX;
                        float dy = event.getY() - startY;
                        if (!isMoving && (Math.abs(dx) > 20 || Math.abs(dy) > 20)) {
                            isMoving = true;
                        }
                        if (isMoving) {
                            return cardGestureDetector.onTouchEvent(event);
                        }
                        return false;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (isMoving) {
                            return cardGestureDetector.onTouchEvent(event);
                        }
                        return false;
                }
                return false;
            }
        };

        if (srsButtonsContainer != null) srsButtonsContainer.setOnTouchListener(delegateSwipeToCard);
        if (groupControls != null)       groupControls.setOnTouchListener(delegateSwipeToCard);

        findViewById(R.id.btnSetting).setOnClickListener(v -> {
            stopAllLoopsAndTimers();
            Intent intent = new Intent(LockScreenActivity.this, MainActivity.class);
            startActivity(intent);
        });

        // Vùng vuốt lên để thoát app
        View swipeUpArea = findViewById(R.id.swipeUpArea);
        swipeUpArea.setOnTouchListener(new View.OnTouchListener() {
            float yDown;
            final float SWIPE_THRESHOLD = 200f;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        yDown = event.getY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        float yUp = event.getY();
                        if (yDown - yUp > SWIPE_THRESHOLD) {
                            finishAffinity();
                            return true;
                        }
                        return false;
                }
                return false;
            }
        });

        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("from_notification_click", false)) {
            openedFromNotification = true;
            handleNotificationClickIntent(intent);
        }

        if (!openedFromNotification && currentSentence == null) {
            showSentence(false);
        }
    }







    private void setupStudyModeSwitch() {
        switchStudyMode = findViewById(R.id.switchStudyMode);
        tvVocabularyMode = findViewById(R.id.tvVocabularyMode);
        tvListeningMode = findViewById(R.id.tvListeningMode);

        if (switchStudyMode == null) {
            Log.e("STUDY_MODE", "switchStudyMode is null");
            return;
        }

        /*
         * Không đọc mode từ SharedPreferences ở đây.
         *
         * Mode sẽ được xác định từ topic/lesson đang được load,
         * sau khi danh sách sentences cuối cùng đã sẵn sàng.
         */
        switchStudyMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isApplyingStudyModePreset) {
                return;
            }

            StudyMode selectedMode =
                    isChecked
                            ? StudyMode.LISTENING
                            : StudyMode.VOCABULARY;

            Log.d(
                    "STUDY_MODE",
                    "User selected mode=" + selectedMode
            );

            applyStudyModePreset(
                    selectedMode,
                    true
            );
        });

        /*
         * Cho phép bấm trực tiếp vào chữ hai bên.
         */
        if (tvVocabularyMode != null) {
            tvVocabularyMode.setOnClickListener(v -> {
                if (switchStudyMode != null
                        && switchStudyMode.isChecked()) {
                    switchStudyMode.setChecked(false);
                } else {
                    applyStudyModePreset(
                            StudyMode.VOCABULARY,
                            true
                    );
                }
            });
        }

        if (tvListeningMode != null) {
            tvListeningMode.setOnClickListener(v -> {
                if (switchStudyMode != null
                        && !switchStudyMode.isChecked()) {
                    switchStudyMode.setChecked(true);
                } else {
                    applyStudyModePreset(
                            StudyMode.LISTENING,
                            true
                    );
                }
            });
        }

        updateStudyModeHeaderUi(
                StudyMode.VOCABULARY
        );
    }

    private String normalizeStudyModeImageName(String imageValue) {
        if (TextUtils.isEmpty(imageValue)) {
            return "";
        }

        String normalized =
                imageValue.trim()
                        .replace("\\", "/")
                        .toLowerCase(Locale.ROOT);

        int slashIndex =
                normalized.lastIndexOf('/');

        if (slashIndex >= 0
                && slashIndex < normalized.length() - 1) {

            normalized =
                    normalized.substring(
                            slashIndex + 1
                    );
        }

        /*
         * Bỏ query hoặc fragment nếu image được lưu dạng URI.
         */
        int queryIndex =
                normalized.indexOf('?');

        if (queryIndex >= 0) {
            normalized =
                    normalized.substring(
                            0,
                            queryIndex
                    );
        }

        int fragmentIndex =
                normalized.indexOf('#');

        if (fragmentIndex >= 0) {
            normalized =
                    normalized.substring(
                            0,
                            fragmentIndex
                    );
        }

        return normalized.trim();
    }


    private StudyMode detectStudyModeFromSentences(
            List<Sentence> source
    ) {
        if (source == null || source.isEmpty()) {
            Log.d(
                    "STUDY_MODE",
                    "No sentences -> fallback VOCABULARY"
            );

            return StudyMode.VOCABULARY;
        }

        int totalSentenceCount = 0;
        int logoSentenceCount = 0;

        Set<String> uniqueNonEmptyImages =
                new HashSet<>();

        for (Sentence sentence : source) {
            if (sentence == null) {
                continue;
            }

            totalSentenceCount++;

            String imageName =
                    normalizeStudyModeImageName(
                            sentence.image
                    );

            if (TextUtils.isEmpty(imageName)) {
                continue;
            }

            uniqueNonEmptyImages.add(
                    imageName
            );

            if ("ic_logo.png".equals(imageName)) {
                logoSentenceCount++;
            }
        }

        if (totalSentenceCount <= 0) {
            return StudyMode.VOCABULARY;
        }

        float logoRatio =
                (float) logoSentenceCount
                        / (float) totalSentenceCount;

        boolean isListening =
                logoRatio >= 0.80f
                        && uniqueNonEmptyImages.size() <= 1;

        StudyMode result =
                isListening
                        ? StudyMode.LISTENING
                        : StudyMode.VOCABULARY;

        Log.d(
                "STUDY_MODE",
                "Detected mode=" + result
                        + " | total=" + totalSentenceCount
                        + " | logo=" + logoSentenceCount
                        + " | ratio=" + logoRatio
                        + " | uniqueImages="
                        + uniqueNonEmptyImages
        );

        return result;
    }

    private void detectAndApplyStudyModeForCurrentSelection(
            String reason
    ) {
        if (sentences == null || sentences.isEmpty()) {
            Log.w(
                    "STUDY_MODE",
                    "Skip detect: sentences empty"
                            + " | reason=" + reason
            );

            return;
        }

        StudyMode detectedMode =
                detectStudyModeFromSentences(
                        sentences
                );

        Log.d(
                "STUDY_MODE",
                "Auto apply"
                        + " | mode=" + detectedMode
                        + " | reason=" + reason
                        + " | size=" + sentences.size()
                        + " | topic="
                        + (
                        currentTopicOnLockscreen != null
                                ? currentTopicOnLockscreen.name
                                : "null"
                )
                        + " | label="
                        + lockCurrentSelectionLabel
        );

        applyStudyModePreset(
                detectedMode,
                false
        );
    }


    private void updateStudyModeHeaderUi(
            StudyMode mode
    ) {
        int activeColor =
                Color.parseColor("#1976D2");

        int inactiveColor =
                Color.parseColor("#777777");

        boolean listening =
                mode == StudyMode.LISTENING;

        if (tvVocabularyMode != null) {
            tvVocabularyMode.setTextColor(
                    listening
                            ? inactiveColor
                            : activeColor
            );

            tvVocabularyMode.setTypeface(
                    tvVocabularyMode.getTypeface(),
                    listening
                            ? Typeface.NORMAL
                            : Typeface.BOLD
            );
        }

        if (tvListeningMode != null) {
            tvListeningMode.setTextColor(
                    listening
                            ? activeColor
                            : inactiveColor
            );

            tvListeningMode.setTypeface(
                    tvListeningMode.getTypeface(),
                    listening
                            ? Typeface.BOLD
                            : Typeface.NORMAL
            );
        }
    }


    private void applyStudyModePreset(
            StudyMode mode,
            boolean fromUser
    ) {
        if (mode == null) {
            mode = StudyMode.VOCABULARY;
        }

        /*
         * Lần refresh UI kế tiếp là do áp preset,
         * không được tự phát Male audio.
         */
        suppressPresetAutoAudioOnce = true;

        isApplyingStudyModePreset = true;

        try {
            boolean listening =
                    mode == StudyMode.LISTENING;

            /*
             * =====================================================
             * PRESET LISTENING
             * =====================================================
             *
             * English       ON
             * Vietnamese    OFF
             * Picture       ON
             * Favorite      OFF
             *
             * Play EN       OFF
             * Play VI       OFF
             * Play Male     ON
             * Wait EN       OFF
             * VI → EN       OFF
             * Play Example  OFF
             *
             * Sequential    ON
             * Auto mode     OFF
             * Voice buttons ON
             *
             * =====================================================
             * PRESET VOCABULARY
             * =====================================================
             *
             * English       OFF
             * Vietnamese    ON
             * Picture       ON
             * Favorite      OFF
             *
             * Play EN       OFF
             * Play VI       ON
             * Play Male     OFF
             * Wait EN       ON
             * VI → EN       ON
             * Play Example  ON
             *
             * Sequential    OFF
             * Auto mode     OFF
             * Voice buttons OFF
             */

            boolean showEnglish =
                    listening;

            boolean showVietnamese =
                    !listening;

            boolean showImage =
                    true;

            boolean favoritesOnly =
                    false;

            boolean playEn =
                    false;

            boolean playVi =
                    !listening;

            boolean playMale =
                    listening;

            boolean waitEn =
                    !listening;

            boolean autoViThenEn =
                    !listening;

            boolean autoPlayExample =
                    !listening;

            boolean sequential =
                    listening;

            boolean autoMode =
                    false;

            boolean showVoiceButtons =
                    listening;

            // =====================================================
            // 1. GÁN CÁC BIẾN RUNTIME
            // =====================================================

            autoPlayOnNext =
                    playEn;

            playEnMaleOnNext =
                    playMale;

            listeningSequentialMode =
                    sequential;

            autoViThenEnEnabled =
                    autoViThenEn;

            autoPlayExampleEnabled =
                    autoPlayExample;

            // =====================================================
            // 2. LƯU SHARED PREFERENCES
            // =====================================================

            SharedPreferences appPrefs =
                    getSharedPreferences(
                            PREFS_APP_SETTINGS,
                            MODE_PRIVATE
                    );

            appPrefs.edit()
                    .putBoolean(
                            "lock_show_english",
                            showEnglish
                    )
                    .putBoolean(
                            KEY_LOCKS_SHOW_VI,
                            showVietnamese
                    )
                    .putBoolean(
                            KEY_PLAY_EN_AUDIO_ON_NEXT,
                            playEn
                    )
                    .putBoolean(
                            KEY_PLAY_VI_AUDIO,
                            playVi
                    )
                    .putBoolean(
                            KEY_PLAY_EN_MALE_ON_NEXT,
                            playMale
                    )
                    .putBoolean(
                            KEY_WAIT_EN_BEFORE_NEXT,
                            waitEn
                    )
                    .putBoolean(
                            KEY_AUTO_VI_THEN_EN,
                            autoViThenEn
                    )
                    .putBoolean(
                            KEY_AUTO_PLAY_EXAMPLE,
                            autoPlayExample
                    )
                    .putBoolean(
                            KEY_LISTENING_SEQUENTIAL_MODE,
                            sequential
                    )
                    .putBoolean(
                            KEY_SHOW_VOICE_LOOP,
                            showVoiceButtons
                    )
                    .apply();

            SharedPreferences lockscreenPrefs =
                    getSharedPreferences(
                            "lockscreen_prefs",
                            MODE_PRIVATE
                    );

            lockscreenPrefs.edit()
                    .putBoolean(
                            "show_image",
                            showImage
                    )
                    .apply();

            SharedPreferences settingsPrefs =
                    getSharedPreferences(
                            "settings",
                            MODE_PRIVATE
                    );

            settingsPrefs.edit()
                    .putBoolean(
                            "locks_favorites_only",
                            favoritesOnly
                    )
                    .apply();

            // =====================================================
            // 3. CẬP NHẬT SWITCH MODE
            // =====================================================

            if (switchStudyMode != null
                    && switchStudyMode.isChecked() != listening) {

                switchStudyMode.setChecked(
                        listening
                );
            }

            updateStudyModeHeaderUi(
                    mode
            );

            // =====================================================
            // 4. CẬP NHẬT CÁC SWITCH CON
            // =====================================================

            setSwitchCheckedIfDifferent(
                    switchShowEnglish,
                    showEnglish
            );

            setSwitchCheckedIfDifferent(
                    switchShowVietnamese,
                    showVietnamese
            );

            setSwitchCheckedIfDifferent(
                    switchShowImage,
                    showImage
            );

            setSwitchCheckedIfDifferent(
                    switchShowFavorites,
                    favoritesOnly
            );

            setSwitchCheckedIfDifferent(
                    switchAutoPlayOnNext,
                    playEn
            );

            setSwitchCheckedIfDifferent(
                    switchPlayViOnNext,
                    playVi
            );

            setSwitchCheckedIfDifferent(
                    switchPlayEnMaleOnNext,
                    playMale
            );

            setSwitchCheckedIfDifferent(
                    switchWaitEnBeforeNext,
                    waitEn
            );

            setSwitchCheckedIfDifferent(
                    switchAutoViThenEn,
                    autoViThenEn
            );

            setSwitchCheckedIfDifferent(
                    switchAutoPlayExample,
                    autoPlayExample
            );

            setSwitchCheckedIfDifferent(
                    switchListeningSequential,
                    sequential
            );

            setSwitchCheckedIfDifferent(
                    switchAutoMode,
                    autoMode
            );

            setSwitchCheckedIfDifferent(
                    switchShowVoiceLoop,
                    showVoiceButtons
            );

            // =====================================================
            // 5. CẬP NHẬT VISIBILITY TRỰC TIẾP
            // =====================================================

            if (enText != null) {
                enText.setVisibility(
                        showEnglish
                                ? View.VISIBLE
                                : View.GONE
                );
            }

            if (ipaText != null) {
                ipaText.setVisibility(
                        showEnglish
                                ? View.VISIBLE
                                : View.GONE
                );
            }

            if (tvVietnameseTop != null) {
                tvVietnameseTop.setVisibility(
                        showVietnamese
                                ? View.VISIBLE
                                : View.GONE
                );
            }

            if (tvVietnamese != null) {
                tvVietnamese.setVisibility(
                        View.GONE
                );
            }

            if (imgSentence != null) {
                imgSentence.setVisibility(
                        showImage
                                ? View.VISIBLE
                                : View.GONE
                );
            }

            if (voiceLoopContainer != null) {
                voiceLoopContainer.setVisibility(
                        showVoiceButtons
                                ? View.VISIBLE
                                : View.GONE
                );
            }

            if (srsButtonsContainer != null) {
                srsButtonsContainer.setVisibility(
                        sequential
                                ? View.GONE
                                : View.VISIBLE
                );
            }

            if (sequential) {
                initListeningSequentialIndex();
            }

            Log.d(
                    "STUDY_MODE",
                    "Preset applied"
                            + " | mode=" + mode
                            + " | fromUser=" + fromUser
                            + " | playVi=" + playVi
                            + " | waitEn=" + waitEn
                            + " | viThenEn=" + autoViThenEn
                            + " | playExample=" + autoPlayExample
            );

        } finally {
            isApplyingStudyModePreset = false;
        }

        /*
         * Refresh câu hiện tại đúng một lần sau khi đã set xong.
         */
        if (currentSentence != null) {
            updateCardViewWithSentence(
                    currentSentence
            );

            updateNowPlayingHeader();

            updateNowPlayingInfo(
                    currentSentence
            );
        }

        updateStats();
        updateSrsStatsBar();

        if (fromUser) {
            Toast.makeText(
                    this,
                    mode == StudyMode.LISTENING
                            ? "🎧 Đã áp dụng chế độ Luyện nghe"
                            : "📚 Đã áp dụng chế độ Luyện từ vựng",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void setSwitchCheckedIfDifferent(
            CompoundButton switchView,
            boolean checked
    ) {
        if (switchView == null) {
            return;
        }

        if (switchView.isChecked() != checked) {
            switchView.setChecked(
                    checked
            );
        }
    }



    private void saveCurrentLockscreenPosition() {
        if (currentSentence == null
                || currentTopicOnLockscreen == null) {
            return;
        }

        String topicId =
                !TextUtils.isEmpty(currentTopicOnLockscreen.id)
                        ? currentTopicOnLockscreen.id
                        : currentTopicOnLockscreen.fileName;

        /*
         * Lưu riêng cho từng selection.
         *
         * Ví dụ:
         * Topic
         * Topic + Lesson A
         * Topic + Lesson B
         */
        String selectionId;

        if (lockCurrentLessonKeys != null
                && !lockCurrentLessonKeys.isEmpty()) {

            List<String> lessonKeys =
                    new ArrayList<>(lockCurrentLessonKeys);

            Collections.sort(lessonKeys);

            selectionId =
                    topicId + "__LESSON__"
                            + TextUtils.join(
                            "|",
                            lessonKeys
                    );

        } else {

            selectionId =
                    topicId;
        }

        String key =
                "last_sentence_key_" + selectionId;

        getSharedPreferences(
                "lockscreen_prefs",
                MODE_PRIVATE
        )
                .edit()
                .putString(
                        key,
                        makeSentenceKey(currentSentence)
                )
                .apply();
    }

    private void restoreCurrentLockscreenPosition() {
        if (sentences == null || sentences.isEmpty()) {
            return;
        }

        /*
         * Nhận diện trực tiếp từ danh sách câu vừa load.
         * Không phụ thuộc trạng thái switch của topic trước.
         */
        StudyMode detectedMode =
                detectStudyModeFromSentences(
                        sentences
                );

        // =========================================================
        // LISTENING: LUÔN BẮT ĐẦU TỪ CÂU ĐẦU TIÊN
        // =========================================================
        if (detectedMode == StudyMode.LISTENING) {
            currentSentence =
                    sentences.get(0);

            listeningSequentialIndex =
                    0;

            lessonAutoLoopIndex =
                    0;

            Log.d(
                    "LOCK_POSITION",
                    "Listening mode -> start from first sentence"
                            + " | topic="
                            + (
                            currentTopicOnLockscreen != null
                                    ? currentTopicOnLockscreen.id
                                    : "null"
                    )
            );

            return;
        }

        // =========================================================
        // VOCABULARY: PHẢI CÓ TOPIC HIỆN TẠI
        // =========================================================
        if (currentTopicOnLockscreen == null) {
            currentSentence =
                    sentences.get(0);

            listeningSequentialIndex =
                    0;

            lessonAutoLoopIndex =
                    0;

            Log.d(
                    "LOCK_POSITION",
                    "Vocabulary but topic is null"
                            + " -> fallback first sentence"
            );

            return;
        }

        String topicId =
                !TextUtils.isEmpty(currentTopicOnLockscreen.id)
                        ? currentTopicOnLockscreen.id
                        : currentTopicOnLockscreen.fileName;

        /*
         * Tạo selectionId giống hệt saveCurrentLockscreenPosition().
         */
        String selectionId;

        if (lockCurrentLessonKeys != null
                && !lockCurrentLessonKeys.isEmpty()) {

            List<String> lessonKeys =
                    new ArrayList<>(
                            lockCurrentLessonKeys
                    );

            Collections.sort(
                    lessonKeys
            );

            selectionId =
                    topicId
                            + "__LESSON__"
                            + TextUtils.join(
                            "|",
                            lessonKeys
                    );

        } else {
            selectionId =
                    topicId;
        }

        String preferenceKey =
                "last_sentence_key_" + selectionId;

        SharedPreferences sp =
                getSharedPreferences(
                        "lockscreen_prefs",
                        MODE_PRIVATE
                );

        String lastKey =
                sp.getString(
                        preferenceKey,
                        ""
                );

        /*
         * Tìm lại đúng câu đã lưu trong selection hiện tại.
         */
        if (!TextUtils.isEmpty(lastKey)) {
            for (int i = 0; i < sentences.size(); i++) {
                Sentence sentence =
                        sentences.get(i);

                if (sentence != null
                        && makeSentenceKey(sentence).equals(lastKey)) {

                    currentSentence =
                            sentence;

                    listeningSequentialIndex =
                            i;

                    lessonAutoLoopIndex =
                            i;

                    Log.d(
                            "LOCK_POSITION",
                            "Vocabulary position restored"
                                    + " | selection=" + selectionId
                                    + " | index=" + i
                                    + "/" + sentences.size()
                                    + " | en=" + sentence.en
                    );

                    return;
                }
            }

            Log.w(
                    "LOCK_POSITION",
                    "Saved vocabulary sentence not found"
                            + " | selection=" + selectionId
                            + " | savedKey=" + lastKey
            );
        }

        /*
         * Selection Vocabulary chưa từng được học,
         * hoặc câu đã lưu không còn tồn tại.
         */
        currentSentence =
                sentences.get(0);

        listeningSequentialIndex =
                0;

        lessonAutoLoopIndex =
                0;

        Log.d(
                "LOCK_POSITION",
                "Vocabulary has no valid saved position"
                        + " -> start from first sentence"
                        + " | selection=" + selectionId
        );
    }

    /**
     Hàm vuốt chung lên cardview để next câu
     */

    private View.OnTouchListener createSwipeOnlyToCardListener() {
        return new View.OnTouchListener() {
            float downX, downY;
            boolean isSwipe;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getX();
                        downY = event.getY();
                        isSwipe = false;

                        cardGestureDetector.onTouchEvent(event);
                        return false;

                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getX() - downX;
                        float dy = event.getY() - downY;

                        if (Math.abs(dx) > 25 || Math.abs(dy) > 25) {
                            isSwipe = true;
                            cardGestureDetector.onTouchEvent(event);
                            return true;
                        }

                        return false;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (isSwipe) {
                            cardGestureDetector.onTouchEvent(event);
                            return true;
                        }
                        return false;
                }

                return false;
            }
        };
    }



    /**
     * ⌨️ XỬ LÝ PHÍM TẮT CHO PC (qua Scrcpy)
     *
     * - 5: Toggle Floating Note (PC mode - không hiện keyboard ảo)
     *  Ctrl+N: Vuốt next câu
     * - Enter: Giống tap vào CardView (check đáp án + show full + loop audio)
     * - 1/2/3/4: Rating Again/Hard/Good/Easy
     * - ↑: Next sentence
     * - ↓: Previous sentence
     * - Ctrl+L: Loop audio example (giống bấm btnSpeaker)
     */


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.e("KEY_DEBUG", "★★★ onKeyDown ACTIVITY CALLED: keyCode=" + keyCode
                + ", action=" + event.getAction()
                + ", repeat=" + event.getRepeatCount()
                + ", isCtrl=" + event.isCtrlPressed());

        // ===== CTRL+N: NEXT CÂU (PC MODE) =====
        if (isPcMode && keyCode == KeyEvent.KEYCODE_N && event.isCtrlPressed()) {
            resetReplayFabState();
            showNextSentence();

            return true;
        }

        // ===== PHÍM 5: TOGGLE FLOATING NOTE (PC MODE) =====
        if (isPcMode && keyCode == KeyEvent.KEYCODE_5) {
            toggleFloatingNoteForPC();
            return true;
        }

        // ===== CTRL+L: LOOP AUDIO EXAMPLE (GIỐNG BẤM btnSpeaker) =====
        if (isPcMode && keyCode == KeyEvent.KEYCODE_L && event.isCtrlPressed()) {
            ImageButton btnSpeaker = findViewById(R.id.btnSpeaker);
            if (btnSpeaker != null) {
                btnSpeaker.performClick();
            }
            return true;
        }

        // ===== ENTER: SHOW ANSWER (GIỐNG SINGLE TAP) =====
        if (keyCode == KeyEvent.KEYCODE_ENTER) {

            // Nếu đang loop example → tắt loop như cũ
            if (isExampleLooping) {
                isExampleLooping = false;
                stopAudioLoop();
                resetExampleLoopState();
            }

            // Đang gõ trong Floating Note → Enter = show đáp án + ẩn keyboard
            if (edtFloatingNote != null && edtFloatingNote.hasFocus()) {
                InputMethodManager imm =
                        (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(edtFloatingNote.getWindowToken(), 0);
                }
                edtFloatingNote.clearFocus();

                handleTapOrEnterShowAnswer();
                return true;
            }

            // Các trường hợp khác → giống single tap vào card
            handleTapOrEnterShowAnswer();
            return true;
        }

        // ===== PHÍM 6: FOCUS VÀO FLOATING NOTE (PC MODE) =====
        if (isPcMode && keyCode == KeyEvent.KEYCODE_6) {

            // Nếu note đang ẩn thì bật lên luôn (PC layout)
            if (!isFloatingNoteVisible) {
                toggleFloatingNoteForPC();
            }

            if (edtFloatingNote != null) {
                edtFloatingNote.requestFocus();

                // PC: ẩn keyboard mềm nếu lỡ đang mở
                InputMethodManager imm =
                        (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(edtFloatingNote.getWindowToken(), 0);
                }

                Editable text = edtFloatingNote.getText();
                if (text != null && text.length() > 0) {
                    edtFloatingNote.setSelection(0, text.length());
                }
            }

            return true;
        }

        // Nếu đang gõ note, các phím khác cho EditText xử lý
        if (edtFloatingNote != null && edtFloatingNote.hasFocus()) {
            return super.onKeyDown(keyCode, event);
        }

        // Các phím còn lại để hệ thống / dispatchKeyEvent xử lý tiếp
        return super.onKeyDown(keyCode, event);
    }




    private boolean canHandleRatingNow() {
        long now = System.currentTimeMillis();
        return (now - lastRatingHandledTime) >= MIN_RATING_INTERVAL_MS;
    }

    private void onRatingHotkeyPressed(int rating) {
        if (!canHandleRatingNow()) {
            // Trong thời gian cooldown, bỏ qua rating
            Log.d("SRS_DEBUG", "Rating blocked by cooldown, rating=" + rating);
            return;
        }

        lastRatingHandledTime = System.currentTimeMillis();

        // Bấm nút tương ứng (để giữ UI/animation cũ)
        int btnId;
        if (rating == 0)       btnId = R.id.btnAgain;
        else if (rating == 1)  btnId = R.id.btnHard;
        else if (rating == 2)  btnId = R.id.btnGood;
        else if (rating == 3)  btnId = R.id.btnEasy;
        else return;

        View btn = findViewById(btnId);
        if (btn != null) btn.performClick();
    }




    private boolean isReadyForImageRating() {
        return hasShownAnswer && currentSentence != null && isImageFocused;

    }


    // ===== 8BITDO MICRO KEY MAP =====
    private boolean isLikelyGameController(KeyEvent event) {
        if (event == null) return false;

        int source = event.getSource();

        return (source & android.view.InputDevice.SOURCE_GAMEPAD) == android.view.InputDevice.SOURCE_GAMEPAD
                || (source & android.view.InputDevice.SOURCE_JOYSTICK) == android.view.InputDevice.SOURCE_JOYSTICK
                || (source & android.view.InputDevice.SOURCE_DPAD) == android.view.InputDevice.SOURCE_DPAD;
    }

    // Bỏ các key phụ của 8BitDo Micro: B=4, Y=62, -=82, +=23
    private boolean isIgnored8BitDoSecondaryKey(int keyCode, KeyEvent event) {
        if (!isLikelyGameController(event)) return false;

        return keyCode == KeyEvent.KEYCODE_BACK          // 4  - phụ của B
                || keyCode == KeyEvent.KEYCODE_SPACE    // 62 - phụ của Y
                || keyCode == KeyEvent.KEYCODE_MENU     // 82 - phụ của -
                || keyCode == KeyEvent.KEYCODE_DPAD_CENTER; // 23 - phụ của +
    }

    private boolean handle8BitDoMicroHotkey(int keyCode, KeyEvent event) {
        int action = event.getAction();

        if (action == KeyEvent.ACTION_DOWN) {

            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                if (event.getRepeatCount() == 0) {
                    dpadUpLongPressTriggered = false;

                    remoteLongPressHandler.removeCallbacks(dpadUpLongPressRunnable);
                    dpadUpLongPressRunnable = () -> {
                        dpadUpLongPressTriggered = true;
                        resetReplayFabState();
                        jumpToSequentialTrack(0);
                    };

                    remoteLongPressHandler.postDelayed(
                            dpadUpLongPressRunnable,
                            REMOTE_LONG_PRESS_MS
                    );
                }
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                if (event.getRepeatCount() == 0) {
                    dpadDownLongPressTriggered = false;

                    remoteLongPressHandler.removeCallbacks(dpadDownLongPressRunnable);
                    dpadDownLongPressRunnable = () -> {
                        dpadDownLongPressTriggered = true;
                        resetReplayFabState();
                        jumpToSequentialTrack(sentences != null ? sentences.size() - 1 : 0);
                    };

                    remoteLongPressHandler.postDelayed(
                            dpadDownLongPressRunnable,
                            REMOTE_LONG_PRESS_MS
                    );
                }
                return true;
            }

            return is8BitDoMicroKey(keyCode);
        }

        if (action != KeyEvent.ACTION_UP) {
            return false;
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            remoteLongPressHandler.removeCallbacks(dpadUpLongPressRunnable);

            if (dpadUpLongPressTriggered) {
                dpadUpLongPressTriggered = false;
                return true;
            }

            resetReplayFabState();

            if (currentSentence != null && !TextUtils.isEmpty(currentSentence.example_audio_vi)) {
                playExampleViOnceFrom8BitDo();
            } else {
                syncSequentialIndexWithCurrentSentence();
                showPrevSentence();
            }

            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            remoteLongPressHandler.removeCallbacks(dpadDownLongPressRunnable);

            if (dpadDownLongPressTriggered) {
                dpadDownLongPressTriggered = false;
                return true;
            }

            resetReplayFabState();

            if (currentSentence != null && !TextUtils.isEmpty(currentSentence.example_audio)) {
                ImageButton btnSpeaker = findViewById(R.id.btnSpeaker);
                if (btnSpeaker != null) btnSpeaker.performClick();
            } else {
                syncSequentialIndexWithCurrentSentence();
                showNextSentence();
            }

            return true;
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_BUTTON_A:
                handleSwipeDownAsGood();
                return true;

            case KeyEvent.KEYCODE_BUTTON_B:
                handleSwipeAsMale();
                return true;

            case KeyEvent.KEYCODE_BUTTON_X:
                handleSwipeAsFemale();
                return true;

            case KeyEvent.KEYCODE_BUTTON_Y:
                handleSwipeUpAsPlayVi();
                return true;

            case KeyEvent.KEYCODE_DPAD_LEFT:
                resetReplayFabState();

                if (!TextUtils.isEmpty(getCurrentSingleLessonKey())
                        && goToAdjacentLockscreenLesson(-1)) {
                    return true;
                }

                Toast.makeText(this, "Không còn lesson trước.", Toast.LENGTH_SHORT).show();
                return true;

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                resetReplayFabState();

                if (!TextUtils.isEmpty(getCurrentSingleLessonKey())
                        && goToAdjacentLockscreenLesson(1)) {
                    return true;
                }

                Toast.makeText(this, "Không còn lesson kế tiếp.", Toast.LENGTH_SHORT).show();
                return true;

            case KeyEvent.KEYCODE_BUTTON_L1:
                adjustMusicVolumeFrom8BitDo(false);
                return true;

            case KeyEvent.KEYCODE_BUTTON_L2:
                adjustMusicVolumeFrom8BitDo(true);
                return true;

            case KeyEvent.KEYCODE_BUTTON_R1:
                View btnMute = findViewById(R.id.btnMute);
                if (btnMute != null) btnMute.performClick();
                return true;

            case KeyEvent.KEYCODE_BUTTON_R2:
                toggleLessonAutoLoopFromRemote();
                return true;

            case KeyEvent.KEYCODE_BUTTON_SELECT:
                setLockTouchEnabled(!isTouchLocked, true);
                return true;

            case KeyEvent.KEYCODE_BUTTON_START:
                SwitchMaterial switchAutoMode = findViewById(R.id.switchAutoMode);
                if (switchAutoMode != null) switchAutoMode.toggle();
                return true;

            case KeyEvent.KEYCODE_BUTTON_MODE:
                saveCurrentLockscreenPosition();
                stopAllLoopsAndTimers();

                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory(Intent.CATEGORY_HOME);
                homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(homeIntent);

                finishAffinity();
                return true;

            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_SPACE:
            case KeyEvent.KEYCODE_MENU:
                return true;

            default:
                return false;
        }
    }

    private boolean is8BitDoMicroKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BUTTON_A:
            case KeyEvent.KEYCODE_BUTTON_B:
            case KeyEvent.KEYCODE_BUTTON_X:
            case KeyEvent.KEYCODE_BUTTON_Y:
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_BUTTON_L1:
            case KeyEvent.KEYCODE_BUTTON_L2:
            case KeyEvent.KEYCODE_BUTTON_R1:
            case KeyEvent.KEYCODE_BUTTON_R2:
            case KeyEvent.KEYCODE_BUTTON_SELECT:
            case KeyEvent.KEYCODE_BUTTON_START:
            case KeyEvent.KEYCODE_BUTTON_MODE:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_SPACE:
            case KeyEvent.KEYCODE_MENU:
                return true;
            default:
                return false;
        }
    }



    private void playExampleViOnceFrom8BitDo() {
        if (currentSentence == null) return;

        String viAudio = currentSentence.example_audio_vi;

        if (TextUtils.isEmpty(viAudio)) {
            Toast.makeText(this,
                    "Không có example VI audio cho câu này.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        isExampleLooping = false;
        isAudioLooping = false;

        stopAudioLoop();
        stopAllVoiceLoops();
        stopCurrentMediaPlayerSafely();

        playAudio(viAudio, false, null);
    }

    private void adjustMusicVolumeFrom8BitDo(boolean increase) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) return;

        audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                increase ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI
        );
    }

    private boolean is8BitDoMicroMainKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_BUTTON_A
                || keyCode == KeyEvent.KEYCODE_BUTTON_B
                || keyCode == KeyEvent.KEYCODE_BUTTON_X
                || keyCode == KeyEvent.KEYCODE_BUTTON_Y
                || keyCode == KeyEvent.KEYCODE_DPAD_UP
                || keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                || keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                || keyCode == KeyEvent.KEYCODE_BUTTON_SELECT
                || keyCode == KeyEvent.KEYCODE_BUTTON_START
                || keyCode == KeyEvent.KEYCODE_BUTTON_L1
                || keyCode == KeyEvent.KEYCODE_BUTTON_L2
                || keyCode == KeyEvent.KEYCODE_BUTTON_R1
                || keyCode == KeyEvent.KEYCODE_BUTTON_R2
                || keyCode == KeyEvent.KEYCODE_BUTTON_MODE;
    }


    // ⭐ Xử lý tất cả hotkey F/M/N/P/... ở một chỗ
    private boolean handleHotkey(int keyCode, KeyEvent event) {
        Log.e("KEY_DEBUG", "handleHotkey keyCode=" + keyCode
                + " action=" + event.getAction());

        // ✅ ĐẶT NGAY ĐÂY, TRƯỚC switch cũ
        if (handle8BitDoMicroHotkey(keyCode, event)) {
            Log.e("8BITDO", "HANDLED keyCode=" + keyCode
                    + " action=" + event.getAction()
                    + " scanCode=" + event.getScanCode()
                    + " source=0x" + Integer.toHexString(event.getSource()));
            return true;
        }

        switch (keyCode) {
            // code cũ giữ nguyên


            // ====== VOLUME_UP / VOLUME_DOWN: Single = MUTE, Double = toggle Auto Mode ======
            case KeyEvent.KEYCODE_VOLUME_UP:     // 24
            case KeyEvent.KEYCODE_VOLUME_DOWN: { // 25

                // Chặn hệ thống đổi volume
                if (event.getAction() != KeyEvent.ACTION_UP) {
                    Log.d("KEY_DEBUG", "VOLUME key=" + keyCode + " action!=UP, ignore");
                    return true;
                }

                long now = System.currentTimeMillis();
                long delta = now - lastVolumeKeyTime;

                Log.d("KEY_DEBUG",
                        "VOLUME RAW event: keyCode=" + keyCode
                                + " now=" + now
                                + " delta=" + delta
                                + " lastVolumeKeyCode=" + lastVolumeKeyCode
                                + " lastVolumeKeyTime=" + lastVolumeKeyTime);

                boolean isDoublePress =
                        lastVolumeKeyCode != -1
                                && delta > 0 && delta < DOUBLE_PRESS_INTERVAL_MS
                                && lastVolumeKeyCode != keyCode;

                if (isDoublePress) {
                    Log.d("KEY_DEBUG", "VOLUME DOUBLE-PRESS DETECTED: " + lastVolumeKeyCode + " -> " + keyCode
                            + " delta=" + delta + "ms -> toggle Auto Mode");

                    // Hủy pending SINGLE nếu có
                    if (pendingVolumeSingleRunnable != null) {
                        volumeHandler.removeCallbacks(pendingVolumeSingleRunnable);
                        pendingVolumeSingleRunnable = null;
                    }

                    // DOUBLE-PRESS: toggle Auto Mode
                    SwitchMaterial switchAutoMode = findViewById(R.id.switchAutoMode);
                    if (switchAutoMode != null) {
                        switchAutoMode.toggle();
                    }

                    // reset state
                    lastVolumeKeyCode = -1;
                    lastVolumeKeyTime = 0L;
                    return true;
                }

                // Không phải double-press → chỉ "có khả năng" là single
                Log.d("KEY_DEBUG", "VOLUME MAYBE SINGLE: keyCode=" + keyCode
                        + " delta=" + delta + "ms -> schedule SINGLE");

                lastVolumeKeyCode = keyCode;
                lastVolumeKeyTime = now;

                // Hủy runnable cũ nếu có
                if (pendingVolumeSingleRunnable != null) {
                    volumeHandler.removeCallbacks(pendingVolumeSingleRunnable);
                }

                pendingVolumeSingleRunnable = () -> {
                    Log.d("KEY_DEBUG", "VOLUME CONFIRMED SINGLE-PRESS: keyCode=" + keyCode
                            + " -> MUTE/UNMUTE (giống phím S)");

                    // SINGLE-PRESS: MUTE / UNMUTE (giống KEYCODE_S)
                    View btnMute = findViewById(R.id.btnMute);
                    if (btnMute != null) {
                        btnMute.performClick();
                    }

                    // Xử lý xong single → clear runnable
                    pendingVolumeSingleRunnable = null;
                };

                // Nếu trong khoảng này có lần bấm thứ 2 → nhánh isDoublePress ở trên sẽ hủy runnable này
                volumeHandler.postDelayed(pendingVolumeSingleRunnable, VOLUME_SINGLE_DELAY_MS);

                return true;
            }
            // ====== N: "vuốt lên" = Good + next câu ======
            case KeyEvent.KEYCODE_N: {
                if (event.getAction() != KeyEvent.ACTION_UP) return true;

                if (!canHandleRatingNow()) {
                    Log.d("SRS_DEBUG", "Rating blocked by cooldown (N -> Good)");
                    return true;
                }
                lastRatingHandledTime = System.currentTimeMillis();

                View btn = findViewById(R.id.btnGood);
                if (btn != null) {
                    btn.performClick();
                }
                return true;
            }

            // ====== P: PREV CÂU ======
            case KeyEvent.KEYCODE_P: {
                if (event.getAction() != KeyEvent.ACTION_UP) return true;

                resetReplayFabState();
                showPrevSentence();

                return true;
            }

            // ====== F: female ======
            case KeyEvent.KEYCODE_F: {
                if (event.getAction() != KeyEvent.ACTION_UP) return true;

                if (nextFReservedForImage && hasShownAnswer) {
                    Log.d("KEY_DEBUG", "F reserved for image double-tap, ignore female audio");
                    nextFReservedForImage = false;
                    return true;
                }

                Log.d("KEY_DEBUG", "handleHotkey F -> female audio, hasShownAnswer=" + hasShownAnswer);
                performFemaleAction();
                return true;
            }

            // ====== M: MALE LOOP ======
            case KeyEvent.KEYCODE_M: {
                if (event.getAction() != KeyEvent.ACTION_UP) return true;

                if (suppressNextMaleFromImageTap) {
                    Log.d("KEY_DEBUG", "KEYCODE_M suppressed after image tap on full answer");
                    suppressNextMaleFromImageTap = false;
                    return true;
                }

                if (suppressNextMaleFromKey) {
                    Log.d("KEY_DEBUG", "KEYCODE_M suppressed after image tap on new card");
                    suppressNextMaleFromKey = false;
                    return true;
                }

                nextFReservedForImage = false;

                performMaleAction();
                return true;
            }

            // ====== S: MUTE / UNMUTE ======
            case KeyEvent.KEYCODE_S: {
                if (event.getAction() != KeyEvent.ACTION_UP) return true;

                View btnMute = findViewById(R.id.btnMute);
                if (btnMute != null) {
                    btnMute.performClick();
                }
                return true;
            }

            // ====== V: toggle Play Vi On Next / play VI ======
            case KeyEvent.KEYCODE_V: {
                if (event.getAction() != KeyEvent.ACTION_UP) return true;

                if (event.isCtrlPressed()) {
                    if (switchPlayViOnNext != null) {
                        switchPlayViOnNext.toggle();
                    }
                } else {
                    playViAudioOnceForCurrentSentence();
                }
                return true;
            }

            // ====== 1..4: SRS rating ======
            case KeyEvent.KEYCODE_1: {
                if (event.getAction() != KeyEvent.ACTION_UP) return true;

                Log.d("KEY_DEBUG", "KEYCODE_1 -> Again");
                performRatingAction(0);
                return true;
            }
            case KeyEvent.KEYCODE_2: {
                if (event.getAction() != KeyEvent.ACTION_UP) return true;

                Log.d("KEY_DEBUG", "KEYCODE_2 -> Hard");
                performRatingAction(1);
                return true;
            }
            case KeyEvent.KEYCODE_3: {
                if (event.getAction() != KeyEvent.ACTION_UP) return true;

                Log.d("KEY_DEBUG", "KEYCODE_3 -> Good");
                performRatingAction(2);
                return true;
            }
            case KeyEvent.KEYCODE_4: {
                if (event.getAction() != KeyEvent.ACTION_UP) return true;

                Log.d("KEY_DEBUG", "KEYCODE_4 -> Easy");
                performRatingAction(3);
                return true;
            }

            // ====== KEYCODE_FORWARD (125) – phím extra trên chuột nếu có ======
            case KeyEvent.KEYCODE_FORWARD: { // 125
                if (event.getAction() != KeyEvent.ACTION_UP) return true;

                resetReplayFabState();
                showNextSentence();

                return true;
            }

            default:
                return false;
        }
    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // ⭐ Bất kỳ key nào (remote, volume, bàn phím...) đều reset timer
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            startInactivityTimer();
        }


        Log.e("KEY_DEBUG",
                ">>> dispatchKeyEvent action=" + event.getAction()
                        + " keyCode=" + event.getKeyCode()
                        + " scanCode=" + event.getScanCode()
                        + " flags=0x" + Integer.toHexString(event.getFlags())
                        + " source=0x" + Integer.toHexString(event.getSource())
                        + " deviceId=" + event.getDeviceId());

        int keyCode = event.getKeyCode();

        // ENTER: xử lý riêng cho show answer
        if (event.getAction() == KeyEvent.ACTION_UP
                && keyCode == KeyEvent.KEYCODE_ENTER) {

            if (edtFloatingNote != null && edtFloatingNote.hasFocus()) {
                // Cho EditText xử lý ENTER bình thường
            } else {
                handleTapOrEnterShowAnswer();
                Log.e("KEY_DEBUG", ">>> ENTER handled by handleTapOrEnterShowAnswer()");
                return true;
            }
        }

        // Cho handleHotkey xử lý tất cả phím
        boolean handled = handleHotkey(keyCode, event);
        if (handled) {
            Log.e("KEY_DEBUG", ">>> handleHotkey HANDLED keyCode=" + keyCode
                    + " action=" + event.getAction());
            // CHẶN hệ thống xử lý (kể cả tăng/giảm volume)
            return true;
        }

        return super.dispatchKeyEvent(event);
    }




    // Female: logic chung cho phím F + swipe Female
    private void performFemaleAction() {
        Log.d("REMOTE_SWIPE", "performFemaleAction, hasShownAnswer=" + hasShownAnswer);

        View btn = findViewById(R.id.btnLoopFemale);
        if (btn == null) return;

        // Chưa full đáp án: play female 1 lần (tuỳ logic bạn)
        if (!hasShownAnswer) {
            btn.performClick();
            return;
        }

        // Full đáp án: loop female
        btn.performClick();
    }

    // Male: logic chung cho phím M + swipe Male
    private void performMaleAction() {
        Log.d("REMOTE_SWIPE", "performMaleAction, hasShownAnswer=" + hasShownAnswer);

        View btn = findViewById(R.id.btnLoopMale);
        if (btn == null) return;

        if (!hasShownAnswer) {
            btn.performClick(); // single male
            return;
        }

        btn.performClick(); // loop male
    }

    // Rating: dùng hàm này thay cho onRatingHotkeyPressed (nếu muốn gom chung)
    private void performRatingAction(int rating) {
        Log.d("SRS_DEBUG", "performRatingAction(" + rating + ") CALLED (cooldown DISABLED FOR TEST)");

        // TẠM THỜI bỏ cooldown để test
        // if (!canHandleRatingNow()) {
        //     Log.d("SRS_DEBUG", "Rating blocked by cooldown, rating=" + rating);
        //     return;
        // }
        // lastRatingHandledTime = System.currentTimeMillis();

        int btnId;
        if (rating == 0)       btnId = R.id.btnAgain;
        else if (rating == 1)  btnId = R.id.btnHard;
        else if (rating == 2)  btnId = R.id.btnGood;
        else if (rating == 3)  btnId = R.id.btnEasy;
        else return;

        View btn = findViewById(btnId);
        Log.d("SRS_DEBUG", "performRatingAction -> btnId=" + btnId + ", view=" + btn);
        if (btn != null) {
            btn.performClick();
            Log.d("SRS_DEBUG", "performRatingAction -> btn.performClick() DONE");
        }
    }





    // Play audio vào chế độ auto mode
    private void playAutoModeOnSound() {
        try {
            MediaPlayer mp = MediaPlayer.create(this, R.raw.auto_mode_on);
            if (mp != null) {
                // ⭐ Volume riêng cho auto mode: 0.2 (20%)
                mp.setVolume(0.2f, 0.2f);

                mp.setOnCompletionListener(MediaPlayer::release);
                mp.setOnErrorListener((player, what, extra) -> {
                    player.release();
                    return true;
                });

                mp.start();
            }
        } catch (Exception e) {
            Log.e("AUTO_MODE", "Error playing auto_mode_on sound", e);
        }
    }


    // DEBUG: reset quota về 0 để test
    private void debugForceResetCounters() {
        SharedPreferences counterPrefs = getSharedPreferences(PREFS_DAILY_COUNTERS, MODE_PRIVATE);
        counterPrefs.edit()
                .putInt(KEY_NEW_COUNT, 0)
                .putInt(KEY_REVIEW_COUNT, 0)
                .apply();

        Log.d("DEBUG_QUOTA", "Force reset counters to 0 in " + PREFS_DAILY_COUNTERS);

        updateSrsStatsBar();

        Toast.makeText(this, "Debug: Reset counters về 0", Toast.LENGTH_SHORT).show();
    }




    // Đặt đâu đó trong LockScreenActivity (ngoài onCreate)
    private void submitFloatingNoteViaEnter() {
        if (edtFloatingNote != null) {
            edtFloatingNote.clearFocus();
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(edtFloatingNote.getWindowToken(), 0);
            }
        }

        // Nếu đang loop example → tắt (cho đồng bộ với tap card)
        if (isExampleLooping) {
            isExampleLooping = false;
            stopAudioLoop();
            resetExampleLoopState();
        }

        // ⭐ QUAN TRỌNG: dùng chung luồng với tap / Enter PC
        handleTapOrEnterShowAnswer();
    }

    // ⭐ Sau khi next câu, nếu Floating Note đang mở → ép focus lại cho ô nhập
    private void forceFocusFloatingNoteIfVisible() {
        if (!isFloatingNoteVisible || edtFloatingNote == null || floatingNoteView == null) return;

        // 1) Đảm bảo view đang visible và focusable
        floatingNoteView.setVisibility(View.VISIBLE);
        edtFloatingNote.setFocusable(true);
        edtFloatingNote.setFocusableInTouchMode(true);

        // 2) Request focus
        edtFloatingNote.requestFocus();
        edtFloatingNote.requestFocusFromTouch();

        // 3) Thông báo cho hệ thống đây là view nhận input từ hardware keyboard
        InputMethodManager imm =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            // Với hardware keyboard thì không nhất thiết phải show IME,
            // nhưng lệnh này giúp hệ thống "gắn" input vào EditText.
            imm.viewClicked(edtFloatingNote);
            // Nếu bạn không muốn hiện keyboard ảo, có thể không gọi showSoftInput.
            // Nếu muốn hiện luôn keyboard ảo khi next, bỏ comment dòng dưới:
            // imm.showSoftInput(edtFloatingNote, InputMethodManager.SHOW_IMPLICIT);
        }

        // 4) (tuỳ chọn) Đưa con trỏ về cuối hoặc select toàn bộ text
        CharSequence text = edtFloatingNote.getText();
        if (text != null) {
            edtFloatingNote.setSelection(text.length());
        }
    }




    // ⭐ Sau khi next câu, nếu Floating Note đang mở VÀ đang ở PC mode → ép focus lại ô nhập
    private void refocusFloatingNoteIfVisible() {
        if (!isPcMode) return;
        if (!isFloatingNoteVisible || edtFloatingNote == null || floatingNoteView == null) return;

        edtFloatingNote.setFocusable(true);
        edtFloatingNote.setFocusableInTouchMode(true);

        View root = getWindow().getDecorView();
        if (root != null) {
            root.clearFocus();
        }

        edtFloatingNote.requestFocus();
        edtFloatingNote.requestFocusFromTouch();

        InputMethodManager imm =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.viewClicked(edtFloatingNote);
            imm.hideSoftInputFromWindow(edtFloatingNote.getWindowToken(), 0);
        }

        CharSequence text = edtFloatingNote.getText();
        if (text != null) {
            edtFloatingNote.setSelection(text.length());
        }

        Log.d("FLOATING_NOTE", "refocusFloatingNoteIfVisible: focused edtFloatingNote (PC mode)");
    }



    /**
     * 🖥️ TOGGLE FLOATING NOTE CHO PC (Ctrl+N)
     * - Focus vào EditText
     * - KHÔNG HIỆN keyboard ảo
     */
    private void toggleFloatingNoteForPC() {
        if (floatingNoteView == null) return;

        if (!isFloatingNoteVisible) {
            isPcMode = true;
            isFloatingNoteVisible = true;
            floatingNoteView.setVisibility(View.VISIBLE);

            ConstraintLayout root = findViewById(R.id.rootLayout);
            if (root != null) {
                root.post(() -> {
                    int parentWidth = root.getWidth();
                    int parentHeight = root.getHeight();

                    int noteWidth = (int) (parentWidth * 0.8f);
                    ViewGroup.LayoutParams lp = floatingNoteView.getLayoutParams();
                    lp.width = noteWidth;
                    floatingNoteView.setLayoutParams(lp);

                    float targetX = (parentWidth - noteWidth) / 2f;
                    float targetY = parentHeight * 0.8f; // 80% chiều cao

                    floatingNoteView.setX(targetX);
                    floatingNoteView.setY(targetY);

                    // ⭐ NHỚ LẠI VỊ TRÍ MẶC ĐỊNH NÀY
                    noteLastX = targetX;
                    noteLastY = targetY;
                });
            }

            if (edtFloatingNote != null) {
                edtFloatingNote.requestFocus();
                edtFloatingNote.post(() -> {
                    InputMethodManager imm =
                            (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(edtFloatingNote.getWindowToken(), 0);
                    }
                });
            }
        } else {
            isPcMode = false;
            hideFloatingNote();
        }
    }




    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter f = new IntentFilter("com.OPD2nd.popup.TOPIC_CHANGED");
        // Topic_CHANGED là broadcast nội bộ app → dùng RECEIVER_NOT_EXPORTED
        registerReceiver(
                topicChangedReceiver,
                f,
                Context.RECEIVER_NOT_EXPORTED
        );
    }


    private void toggleSwitchPanel() {
        if (switchPanelExpanded) {
            collapseView(switchPanel);
            switchPanelExpanded = false;
            ivSwitchArrow.animate().rotation(0f).setDuration(200).start();
        } else {
            switchPanel.setVisibility(View.VISIBLE);

            ViewGroup.LayoutParams lp = switchPanel.getLayoutParams();
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            switchPanel.setLayoutParams(lp);

            switchPanel.requestLayout();

            switchPanelExpanded = true;
            ivSwitchArrow.animate().rotation(180f).setDuration(200).start();
        }
    }


    private void showEditExampleDialog(TextView targetView, boolean isEnglish) {
        if (currentSentence == null) return;

        Context context = this;
        CharSequence currentText = targetView.getText();

        final EditText input = new EditText(context);
        input.setText(currentText);
        input.setSelection(input.getText().length());
        input.setSingleLine(false);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(2);
        input.setMaxLines(6);
        input.setGravity(Gravity.TOP | Gravity.START);

        int padding = (int) (8 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);  // ✅ left, top, right, bottom
        new AlertDialog.Builder(context)
                .setTitle(isEnglish ? "Sửa example EN" : "Sửa example VI")
                .setView(input)
                .setPositiveButton("Lưu", (d, which) -> {
                    String newText = input.getText().toString().trim();

                    // ✅ UPDATE TextView
                    targetView.setText(newText);

                    // ✅✅✅ UPDATE currentSentence.example ✅✅✅
                    if (tvExampleEnInline != null && tvExampleViInline != null) {
                        String en = tvExampleEnInline.getText().toString().trim();
                        String vi = tvExampleViInline.getText().toString().trim();

                        // ✅ NỐI LẠI THÀNH "EN\\nVI"
                        if (!en.isEmpty() || !vi.isEmpty()) {
                            currentSentence.example = en + "\\\\n" + vi;
                        } else {
                            currentSentence.example = "";
                        }

                        Log.d("EDIT_EXAMPLE", "Updated example: " + currentSentence.example);

                        // ✅✅✅ LƯU NGAY ✅✅✅
                        saveSentencesToFile();

                        Toast.makeText(this, "✅ Đã lưu example", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }



    private void updateVoiceLoopButtonsUi() {
        if (btnLoopFemale != null) {
            if (isFemaleLooping) {
                btnLoopFemale.setAlpha(1.0f);
                btnLoopFemale.setBackgroundTintList(
                        // TRƯỚC: "#FFFBE9E7"
                        ColorStateList.valueOf(Color.parseColor("#FFFFF3E0"))  // dùng màu cũ của Male
                );
            } else {
                btnLoopFemale.setAlpha(0.7f);
                btnLoopFemale.setBackgroundTintList(
                        ColorStateList.valueOf(Color.parseColor("#FFFFFFFF"))  // trắng khi OFF
                );
            }
        }

        if (btnLoopMale != null) {
            if (isMaleLooping) {
                btnLoopMale.setAlpha(1.0f);
                btnLoopMale.setBackgroundTintList(
                        // TRƯỚC: "#FFFFF3E0"
                        ColorStateList.valueOf(Color.parseColor("#FFFBE9E7"))  // dùng màu cũ của Female
                );
            } else {
                btnLoopMale.setAlpha(0.7f);
                btnLoopMale.setBackgroundTintList(
                        ColorStateList.valueOf(Color.parseColor("#FFFFFFFF"))  // trắng khi OFF
                );
            }
        }
    }







    private void showShortcutsDialog() {
        // Tạo WebView
        WebView webView = new WebView(this);
        webView.setBackgroundColor(Color.TRANSPARENT);

        String html = "<!DOCTYPE html>" +
                "<html><head><meta name='viewport' content='width=device-width'>" +
                "<style>" +
                "body { font-family: 'Roboto', sans-serif; padding: 12px; margin: 0; background: #f5f5f5; }" +
                ".section { margin-bottom: 16px; background: white; border-radius: 10px; padding: 10px 14px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }" +
                ".section-title { font-size: 15px; font-weight: bold; color: #1976D2; margin-bottom: 8px; border-bottom: 2px solid #E3F2FD; padding-bottom: 4px; }" +
                ".item { display: flex; align-items: center; padding: 6px 0; border-bottom: 1px solid #f0f0f0; }" +
                ".item:last-child { border-bottom: none; }" +
                ".key { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 3px 8px; border-radius: 5px; font-weight: bold; min-width: 55px; text-align: center; font-size: 12px; margin-right: 10px; box-shadow: 0 2px 3px rgba(0,0,0,0.2); }" +
                ".key.rating { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }" +
                ".key.audio { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); }" +
                ".key.control { background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); }" +
                ".key.gesture { background: linear-gradient(135deg, #fa709a 0%, #fee140 100%); }" +
                ".desc { flex: 1; color: #424242; font-size: 13px; }" +
                "</style></head><body>" +

                "<div class='section'>" +
                "<div class='section-title'>⭐ SRS Rating</div>" +
                "<div class='item'><div class='key rating'>N</div><div class='desc'>Good + next câu</div></div>" +
                "<div class='item'><div class='key rating'>1 / ⬆️⬆️</div><div class='desc'>Again (double vuốt lên)</div></div>" +
                "<div class='item'><div class='key rating'>2 / ❤️❤️</div><div class='desc'>Hard (double press thả tym)</div></div>" +
                "<div class='item'><div class='key rating'>3 / ⬇️⬇️</div><div class='desc'>Good (double vuốt xuống)</div></div>" +
                "<div class='item'><div class='key rating'>4 / ➡️➡️</div><div class='desc'>Easy (double vuốt phải)</div></div>" +
                "</div>" +

                "<div class='section'>" +
                "<div class='section-title'>🎵 Audio Control</div>" +
                "<div class='item'><div class='key audio'>F</div><div class='desc'>Loop Female audio</div></div>" +
                "<div class='item'><div class='key audio'>M</div><div class='desc'>Loop Male audio</div></div>" +
                "<div class='item'><div class='key audio'>V / ⬆️</div><div class='desc'>Play audio tiếng Việt</div></div>" +
                "<div class='item'><div class='key audio'>S / 📷</div><div class='desc'>Mute / Unmute</div></div>" +
                "<div class='item'><div class='key audio'>Enter</div><div class='desc'>Full đáp án + loop audio</div></div>" +
                "</div>" +

                // ⭐ THÊM SECTION GESTURE TRÊN ẢNH
                "<div class='section'>" +
                "<div class='section-title'>🖼 Gesture trên hình</div>" +
                "<div class='item'><div class='key gesture'>Tap</div><div class='desc'>Single tap lên hình → Female audio (giống nút F / nữ)</div></div>" +
                "<div class='item'><div class='key gesture'>Tap Tap</div><div class='desc'>Double tap lên hình → Male audio (giống nút M / nam)</div></div>" +
                "<div class='item'><div class='key gesture'>Long Tap</div><div class='desc'>Nhấn giữ lên hình → Show full đáp án</div></div>" +
                "</div>" +

                "<div class='section'>" +
                "<div class='section-title'>🎮 Navigation</div>" +
                "<div class='item'><div class='key control'>P / ⬅️⬅️</div><div class='desc'>Câu trước (double vuốt trái)</div></div>" +
                "<div class='item'><div class='key control'>Forward</div><div class='desc'>Câu kế tiếp (nút 125)</div></div>" +
                "</div>" +

                "<div class='section'>" +
                "<div class='section-title'>🔧 Special Actions</div>" +
                "<div class='item'><div class='key gesture'>❤️</div><div class='desc'>Lock/Unlock Touch (single press thả tym)</div></div>" +
                "<div class='item'><div class='key gesture'>📷📷</div><div class='desc'>Chế độ Auto (double press chụp hình)</div></div>" +
                "</div>" +

                "</body></html>";

        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);

        new AlertDialog.Builder(this)
                .setTitle("⌨️ Phím tắt & Gesture")
                .setView(webView)
                .setPositiveButton("Đóng", null)
                .show();
    }

    // Khai báo trên class



    // Vuốt khi dùng 4 nút gamepad
    private String motionActionName(int action) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:   return "DOWN";
            case MotionEvent.ACTION_UP:     return "UP";
            case MotionEvent.ACTION_MOVE:   return "MOVE";
            case MotionEvent.ACTION_CANCEL: return "CANCEL";
            default: return "OTHER(" + action + ")";
        }
    }

    private void setupSwipePriorityAreaGesture() {
        View swipeHorizontalBar     = findViewById(R.id.swipeHorizontalBar);
        View swipeVerticalBarTop    = findViewById(R.id.swipeVerticalBarTop);
        View swipeVerticalBarBottom = findViewById(R.id.swipeVerticalBarBottom);
        View swipeCenterTap         = findViewById(R.id.swipeCenterTap);

        if (swipeHorizontalBar == null ||
                swipeVerticalBarTop == null ||
                swipeVerticalBarBottom == null ||
                swipeCenterTap == null) {
            Log.e("SWIPE_PRIORITY", "One of swipe views is NULL");
            return;
        }

        swipePriorityDetector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {
                    private static final int SWIPE_THRESHOLD = 30;
                    private static final int SWIPE_VELOCITY_THRESHOLD = 20;

                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true;
                    }

                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2,
                                           float velocityX, float velocityY) {
                        if (e1 == null || e2 == null) return false;

                        float diffX = e2.getX() - e1.getX();
                        float diffY = e2.getY() - e1.getY();

                        boolean isVertical = Math.abs(diffY) > Math.abs(diffX)
                                && Math.abs(diffY) > SWIPE_THRESHOLD
                                && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD;

                        boolean isHorizontal = Math.abs(diffX) > Math.abs(diffY)
                                && Math.abs(diffX) > SWIPE_THRESHOLD
                                && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD;

                        Log.d("SWIPE_PRIORITY", "onFling dx=" + diffX + " dy=" + diffY
                                + " fromView=" + (currentSwipeView != null ? currentSwipeView.getId() : -1));

                        if (isHorizontal) {
                            if (diffX > 0) {
                                handleSwipeAsFemale();
                            } else {
                                handleSwipeAsMale();
                            }
                            return true;
                        } else if (isVertical) {
                            if (diffY < 0) {
                                Log.e("BUG_TRACE", "CALL handleSwipeUpAsPlayVi from swipePriorityDetector"
                                        + " | fromView=" + (currentSwipeView != null ? currentSwipeView.getId() : -1)
                                        + " | diffY=" + diffY
                                        + " | velocityY=" + velocityY);

                                handleSwipeUpAsPlayVi();
                            } else {
                                handleSwipeDownAsGood();
                            }
                            return true;
                        }

                        return false;
                    }
                });

        View.OnTouchListener barSwipeListener = (v, event) -> {
            if (switchRemoteGesture == null || !switchRemoteGesture.isChecked()) {
                return false;
            }

            currentSwipeView = v;

            int action = event.getActionMasked();
            Log.d("SWIPE_PRIORITY",
                    "touch viewId=" + v.getId()
                            + " action=" + action
                            + " [" + motionActionName(action) + "]"
                            + " x=" + event.getX()
                            + " y=" + event.getY());

            startInactivityTimer();

            swipePriorityDetector.onTouchEvent(event);
            return true;
        };
        swipeHorizontalBar.setOnTouchListener(barSwipeListener);
        swipeVerticalBarTop.setOnTouchListener(barSwipeListener);
        swipeVerticalBarBottom.setOnTouchListener(barSwipeListener);
    }
// Hàm phụ trợ cho vuốt


    private boolean hasExampleAudioForSwipe(Sentence s) {
        if (s == null) return false;

        return !TextUtils.isEmpty(s.audio_female)
                || !TextUtils.isEmpty(s.audio_male)
                || !TextUtils.isEmpty(s.example_audio);
    }

    // ====== SWIPE FEMALE: SINGLE = PLAY FEMALE | DOUBLE = PREV ======


    private void handleSwipeAsFemale() {
        if (currentSentence == null) return;

        // ⭐ Listening theo thứ tự: vuốt phải = bật/tắt loop Female ngay
        if (listeningSequentialMode) {
            Log.d("REMOTE_SWIPE", "Listening sequential -> toggle FEMALE loop");

            if (isFemaleLooping) {
                isFemaleLooping = false;
                stopAllVoiceLoops();
                updateVoiceLoopButtonsUi();
            } else {
                isMaleLooping = false;
                startVoiceLoop("female");
            }

            return;
        }

        swipeFemaleCount++;

        if (swipeFemaleCount == 1) {
            singleSwipeFemaleRunnable = () -> {
                if (swipeFemaleCount == 1) {
                    Log.d("REMOTE_SWIPE", "Single swipe Female → play female audio");
                    performFemaleAction();
                }
                swipeFemaleCount = 0;
            };
            swipeHandler.postDelayed(singleSwipeFemaleRunnable, DOUBLE_SWIPE_INTERVAL_MS);

        } else if (swipeFemaleCount == 2) {
            Log.d("REMOTE_SWIPE", "Double swipe Female → PREV sentence");
            swipeHandler.removeCallbacks(singleSwipeFemaleRunnable);
            swipeFemaleCount = 0;

            resetReplayFabState();
            showPrevSentence();
        }
    }

    // ====== SWIPE MALE: SINGLE = PLAY MALE | DOUBLE = EASY + NEXT ======

    private void handleSwipeAsMale() {
        if (currentSentence == null) return;

        swipeMaleCount++;

        if (swipeMaleCount == 1) {
            singleSwipeMaleRunnable = () -> {
                if (swipeMaleCount == 1) {

                    if (listeningSequentialMode) {
                        Log.d("REMOTE_SWIPE", "Single swipe Male + Sequential → toggle MALE loop");

                        if (isMaleLooping) {
                            isMaleLooping = false;
                            stopAllVoiceLoops();
                            updateVoiceLoopButtonsUi();
                        } else {
                            isFemaleLooping = false;
                            startVoiceLoop("male");
                        }

                    } else {
                        Log.d("REMOTE_SWIPE", "Single swipe Male → play male audio");
                        performMaleAction();
                    }
                }

                swipeMaleCount = 0;
            };

            swipeHandler.postDelayed(singleSwipeMaleRunnable, DOUBLE_SWIPE_INTERVAL_MS);

        } else if (swipeMaleCount == 2) {
            Log.d("REMOTE_SWIPE", "Double swipe Male → TOGGLE Lesson Auto Loop");

            if (singleSwipeMaleRunnable != null) {
                swipeHandler.removeCallbacks(singleSwipeMaleRunnable);
            }

            swipeMaleCount = 0;

            resetReplayFabState();
            toggleLessonAutoLoopFromRemote();
        }
    }

    private void handleSwipeUpAsPlayVi() {
        Log.e("BUG_TRACE", "ENTER handleSwipeUpAsPlayVi"
                + " | current=" + (currentSentence != null ? currentSentence.en : "null")
                + " | hasShownAnswer=" + hasShownAnswer
                + " | hasPlayedExample=" + hasPlayedExampleForCurrentSentence
                + " | swipeUpCount=" + swipeUpCount);

        if (currentSentence == null) return;

        swipeUpCount++;

        if (swipeUpCount == 1) {
            singleSwipeUpRunnable = () -> {
                if (swipeUpCount == 1) {
                    Log.d("REMOTE_SWIPE", "Single Swipe Up");

                    if (!hasShownAnswer) {
                        handleTapOrEnterShowAnswer();
                    }

                    playViAudioOnceForCurrentSentence();
                }

                swipeUpCount = 0;
            };

            swipeHandler.postDelayed(singleSwipeUpRunnable, DOUBLE_SWIPE_INTERVAL_MS);

        } else if (swipeUpCount == 2) {
            swipeHandler.removeCallbacks(singleSwipeUpRunnable);
            swipeUpCount = 0;

            resetReplayFabState();

            Log.d("REMOTE_SWIPE", "Double Swipe Up -> Try Next Lesson immediately");

            String currentLessonKey = getCurrentSingleLessonKey();

            if (!TextUtils.isEmpty(currentLessonKey)) {
                boolean moved = goToAdjacentLockscreenLesson(1);

                if (moved) {
                    Log.d("REMOTE_SWIPE", "Double Swipe Up -> Next Lesson OK");
                    return;
                }
            }

            Log.d("REMOTE_SWIPE", "Double Swipe Up -> Not single lesson, Next Sentence");
            showNextSentence();
        }
    }

    private void handleSwipeDownAsGood() {
        if (currentSentence == null) return;

        Log.d("REMOTE_SWIPE", "handleSwipeDownAsGood, hasShownAnswer="
                + hasShownAnswer
                + ", listeningSequentialMode=" + listeningSequentialMode);

        // Listening theo thứ tự: vuốt xuống 1 lần = next ngay
        if (!hasShownAnswer && listeningSequentialMode) {
            Log.d("REMOTE_SWIPE", "Listening sequential → swipe down NEXT immediately");

            resetReplayFabState();
            showNextSentence();
            return;
        }

        if (!hasShownAnswer) {
            swipeDownCount++;

            if (swipeDownCount == 1) {
                singleSwipeDownRunnable = () -> {
                    if (swipeDownCount == 1) {
                        Log.d("REMOTE_SWIPE", "Single swipe down → show answer");
                        handleTapOrEnterShowAnswer();
                    }
                    swipeDownCount = 0;
                };

                swipeHandler.postDelayed(singleSwipeDownRunnable, DOUBLE_SWIPE_INTERVAL_MS);

            } else if (swipeDownCount == 2) {
                Log.d("REMOTE_SWIPE", "Double swipe down → GOOD + next");
                swipeHandler.removeCallbacks(singleSwipeDownRunnable);
                swipeDownCount = 0;

                resetReplayFabState();
                performRatingAction(2);
            }

            return;
        }

        resetReplayFabState();

        if (listeningSequentialMode) {
            showNextSentence();
        } else {
            performRatingAction(2);
        }
    }



    private void toggleLessonAutoLoopFromRemote() {
        boolean newState = !lessonAutoLoopEnabled;

        Log.d("REMOTE_SWIPE", "toggleLessonAutoLoopFromRemote -> " + newState);

        applyLessonAutoLoopState(newState, true);

        Toast.makeText(this,
                newState ? "Lesson Auto Loop ON" : "Lesson Auto Loop OFF",
                Toast.LENGTH_SHORT).show();
    }


// Hàm tiện tích kiểm tra điểm touch có nằm trong View không

    private boolean isTouchInsideView(View v, MotionEvent ev) {
        if (v == null) return false;

        int[] location = new int[2];
        v.getLocationOnScreen(location);
        int left   = location[0];
        int top    = location[1];
        int right  = left + v.getWidth();
        int bottom = top  + v.getHeight();

        float rawX = ev.getRawX();
        float rawY = ev.getRawY();

        return rawX >= left && rawX <= right && rawY >= top && rawY <= bottom;
    }

    //Hàm chung để bật/tắt Lock touch
    private void setLockTouchEnabled(boolean enabled, boolean fromBubble) {
        Log.d("LOCK_BUBBLE", "setLockTouchEnabled enabled=" + enabled + " fromBubble=" + fromBubble);
        isTouchLocked = enabled;

        if (switchLockTouch != null && fromBubble) {
            switchLockTouch.setOnCheckedChangeListener(null);
            switchLockTouch.setChecked(enabled);
            switchLockTouch.setOnCheckedChangeListener(lockTouchCheckedChangeListener);
        }

        SharedPreferences appPrefsLocal = getSharedPreferences(PREFS_APP_SETTINGS, MODE_PRIVATE);
        appPrefsLocal.edit().putBoolean("lock_touch_mode", enabled).apply();

        if (lockTouchBubble != null) {
            Log.d("LOCK_BUBBLE", "show bubble (" + (enabled ? "LOCK" : "UNLOCK") + ") + schedule hide");

            // Chọn icon + màu theo trạng thái
            if (enabled) {
                // ĐANG LOCK
                lockTouchBubble.setImageResource(R.drawable.ic_lock);
                int bgColor = Color.parseColor("#FF9800"); // cam
                lockTouchBubble.setBackgroundTintList(ColorStateList.valueOf(bgColor));
            } else {
                // ĐANG UNLOCK
                lockTouchBubble.setImageResource(R.drawable.ic_lock_open);
                int bgColor = Color.parseColor("#4CAF50"); // xanh lá
                lockTouchBubble.setBackgroundTintList(ColorStateList.valueOf(bgColor));
            }

            // Hiệu ứng xuất hiện
            animateBubbleShow();

            // LUÔN lên lịch auto-hide 2s cho cả lock & unlock
            scheduleHideLockBubbleDelayed();

        } else {
            Log.d("LOCK_BUBBLE", "lockTouchBubble == null in setLockTouchEnabled");
        }

        // Haptic
        if (enabled) {
            vibrateLockOn();
        } else {
            vibrateLockOff();
        }
    }

    private void scheduleHideLockBubbleDelayed() {
        cancelHideLockBubble();
        if (hideLockBubbleRunnable == null) {
            hideLockBubbleRunnable = () -> {
                if (lockTouchBubble != null) {
                    Log.d("LOCK_BUBBLE", "Auto hide bubble after 2s, isTouchLocked=" + isTouchLocked);
                    lockTouchBubble.setVisibility(View.GONE);
                } else {
                    Log.d("LOCK_BUBBLE", "Runnable fired but bubble=null");
                }
            };
        }
        Log.d("LOCK_BUBBLE", "scheduleHideLockBubbleDelayed posted");
        lockBubbleHandler.postDelayed(hideLockBubbleRunnable, 2000); // 2s
    }

    private void cancelHideLockBubble() {
        if (hideLockBubbleRunnable != null) {
            lockBubbleHandler.removeCallbacks(hideLockBubbleRunnable);
        }
    }

    private void vibrateShort() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(
                    40,  // ms
                    VibrationEffect.DEFAULT_AMPLITUDE
            ));
        } else {
            vibrator.vibrate(40);
        }
    }

    // BẢN MỚI: cho phép truyền duration tuỳ ý
    private void vibrateShort(long durationMs) {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(
                    durationMs,
                    VibrationEffect.DEFAULT_AMPLITUDE
            ));
        } else {
            vibrator.vibrate(durationMs);
        }
    }


    private void vibrateUnlockPattern() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            long[] timings = new long[] {
                    0,    // bắt đầu ngay
                    90,   // rung 1
                    120,  // nghỉ
                    90    // rung 2
            };
            int[] amplitudes;
            if (vibrator.hasAmplitudeControl()) {
                amplitudes = new int[] {
                        0,
                        255,
                        0,
                        255
                };
            } else {
                amplitudes = new int[] {
                        0,
                        VibrationEffect.DEFAULT_AMPLITUDE,
                        0,
                        VibrationEffect.DEFAULT_AMPLITUDE
                };
            }

            // 👉 CHỈ để dòng này, KHÔNG có [web:xxx] phía sau
            VibrationEffect effect =
                    VibrationEffect.createWaveform(timings, amplitudes, -1);
            vibrator.vibrate(effect);
        } else {
            long[] pattern = new long[] { 0, 90, 120, 90 };
            vibrator.vibrate(pattern, -1);
        }
    }


    private void vibrateLockOn() {
        // 1 nhịp dài hơn cho LOCK
        vibrateShort(80);
    }

    private void vibrateLockOff() {
        vibrateUnlockPattern();
    }


    // Hiệu ứng bubble touch lock
    private void animateBubbleShow() {
        if (lockTouchBubble == null) return;

        // Bắt đầu rất nhỏ và trong suốt
        lockTouchBubble.setScaleX(0.4f);
        lockTouchBubble.setScaleY(0.4f);
        lockTouchBubble.setAlpha(0f);
        lockTouchBubble.setVisibility(View.VISIBLE);

        // Pha 1: zoom mạnh + fade in
        lockTouchBubble.animate()
                .scaleX(1.15f)
                .scaleY(1.15f)
                .alpha(1f)
                .setDuration(180)
                .withEndAction(() -> {
                    // Pha 2: nảy nhẹ xuống 0.9
                    lockTouchBubble.animate()
                            .scaleX(0.9f)
                            .scaleY(0.9f)
                            .setDuration(80)
                            .withEndAction(() -> {
                                // Pha 3: về 1.0 cho tự nhiên
                                lockTouchBubble.animate()
                                        .scaleX(1f)
                                        .scaleY(1f)
                                        .setDuration(80)
                                        .start();
                            })
                            .start();
                })
                .start();
    }




    private String getTopicPackageNameFromJsonFile(String jsonFileName) {
        if (jsonFileName == null) return "";

        String name = jsonFileName.trim();

        if (name.startsWith("topics/")) {
            name = name.substring("topics/".length());
        }

        int slashIndex = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slashIndex >= 0) {
            name = name.substring(slashIndex + 1);
        }

        if (name.toLowerCase().endsWith(".json")) {
            name = name.substring(0, name.length() - 5);
        }

        return name;
    }


//hàm phụ trợ cho auto mode bật LearningMediaSession


    private boolean wasStoppedBySleepTimer() {
        SharedPreferences sp = getSharedPreferences("learning_media_prefs", MODE_PRIVATE);
        return sp.getBoolean("stopped_by_sleep", false);
    }

    private boolean isLearningMediaPlaying() {
        SharedPreferences sp = getSharedPreferences("media_flags", MODE_PRIVATE);
        return sp.getBoolean("media_playing", false);
    }

    private void startLearningEnglishOnlySafe() {
        if (wasStoppedBySleepTimer()) {
            Log.d("AUTO_MODE", "Last session was stopped by sleep timer");

            getSharedPreferences("learning_media_prefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("stopped_by_sleep", false)
                    .apply();
        }

        if (isLearningMediaPlaying()) {
            Log.d(
                    "AUTO_MODE",
                    "LearningMediaService already playing, ignore duplicate start"
            );
            return;
        }

        if (currentTopicOnLockscreen == null) {
            Toast.makeText(
                    this,
                    "Chưa chọn topic/lesson để nghe.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (sentences == null || sentences.isEmpty()) {
            Toast.makeText(
                    this,
                    "Danh sách nghe hiện tại đang rỗng.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        // =========================================================
        // Bảo đảm state của lesson hiện tại đã đầy đủ
        // =========================================================
        ensureCurrentLessonState();

        // Lưu đúng vị trí câu đang học
        saveCurrentLockscreenPosition();

        // Lưu selection hiện tại
        saveLockscreenSelectionState(
                lockCurrentSelectionLabel,
                lockCurrentLessonKeys,
                lockCurrentGroupName,
                lockCurrentSectionName
        );

        SharedPreferences lockPrefs =
                getSharedPreferences(
                        "lockscreen_prefs",
                        MODE_PRIVATE
                );

        lockPrefs.edit()
                .putString(
                        "current_topic_id",
                        currentTopicOnLockscreen.id != null
                                ? currentTopicOnLockscreen.id
                                : ""
                )
                .putString(
                        "current_topic_file",
                        currentTopicOnLockscreen.fileName != null
                                ? currentTopicOnLockscreen.fileName
                                : ""
                )
                .putBoolean(
                        "use_lock_filtered_selection",
                        true
                )
                .commit();

        // =========================================================
        // Ghi playlist trước khi khởi chạy Service
        // =========================================================
        saveCurrentLockscreenListeningListForService();

        // =========================================================
        // Ghi các option Auto Mode cho Service
        // =========================================================
        SharedPreferences mediaPrefs =
                getSharedPreferences(
                        "learning_media_prefs",
                        MODE_PRIVATE
                );

        mediaPrefs.edit()
                .putBoolean(
                        "running",
                        true
                )
                .putBoolean(
                        "stopped_by_sleep",
                        false
                )
                .putBoolean(
                        "use_lock_filtered_selection",
                        true
                )
                .putBoolean(
                        "listening_sequential_mode",
                        listeningSequentialMode
                )
                .putBoolean(
                        KEY_AUTO_VI_THEN_EN,
                        autoViThenEnEnabled
                )
                .putBoolean(
                        KEY_AUTO_PLAY_EXAMPLE,
                        autoPlayExampleEnabled
                )
                .commit();

        Log.d(
                "AUTO_MODE",
                "Start Service"
                        + " | topicId=" + currentTopicOnLockscreen.id
                        + " | topicFile=" + currentTopicOnLockscreen.fileName
                        + " | group=" + lockCurrentGroupName
                        + " | section=" + lockCurrentSectionName
                        + " | label=" + lockCurrentSelectionLabel
                        + " | keys=" + lockCurrentLessonKeys
                        + " | size=" + sentences.size()
                        + " | viThenEn=" + autoViThenEnEnabled
                        + " | playExample=" + autoPlayExampleEnabled
        );

        LearningMediaService.startAndPlayEnglishOnly(
                this,
                listeningSequentialMode
        );
    }



    private void saveCurrentLockscreenListeningListForService() {
        if (sentences == null || sentences.isEmpty()) return;

        try {
            String json = new Gson().toJson(sentences);

            FileOutputStream fos = openFileOutput("lockscreen_current_listening.json", MODE_PRIVATE);
            fos.write(json.getBytes("UTF-8"));
            fos.close();

            Log.d("AUTO_MODE",
                    "Saved lockscreen_current_listening.json size=" + sentences.size());

        } catch (Exception e) {
            Log.e("AUTO_MODE", "Cannot save current listening list for service", e);
        }
    }

    private void runCardTapEffect() {
        if (cardSentence == null) return;

        cardSentence.setCardBackgroundColor(pressedColor);
        cardSentence.animate().scaleX(0.96f).scaleY(0.96f).alpha(0.8f)
                .setDuration(70)
                .withEndAction(() -> {
                    cardSentence.animate().scaleX(1f).scaleY(1f).alpha(1f)
                            .setDuration(120).start();
                    handler.postDelayed(
                            () -> cardSentence.setCardBackgroundColor(originalColor),
                            120
                    );
                }).start();

        shrinkImageForAnswer();
    }




    // Helper cho NowPlaying và Setting


    private void playRawSoundThen(int rawResId, Runnable onComplete) {
        try {

            if (currentMediaPlayer != null) {
                try {
                    currentMediaPlayer.stop();
                } catch (Exception ignored) {}

                try {
                    currentMediaPlayer.release();
                } catch (Exception ignored) {}

                currentMediaPlayer = null;
            }

            MediaPlayer mp = MediaPlayer.create(this, rawResId);

            if (mp == null) {
                if (onComplete != null) {
                    onComplete.run();
                }
                return;
            }

            currentMediaPlayer = mp;

            // ⭐ Chỉ phát 30% âm lượng
            mp.setVolume(0.4f, 0.4f);

            mp.setOnCompletionListener(player -> {

                if (currentMediaPlayer == player) {
                    currentMediaPlayer = null;
                }

                try {
                    player.release();
                } catch (Exception ignored) {}

                if (onComplete != null) {
                    handler.post(onComplete);
                }
            });

            mp.setOnErrorListener((player, what, extra) -> {

                Log.e("RAW_SOUND",
                        "MediaPlayer error what=" + what + " extra=" + extra);

                if (currentMediaPlayer == player) {
                    currentMediaPlayer = null;
                }

                try {
                    player.release();
                } catch (Exception ignored) {}

                if (onComplete != null) {
                    handler.post(onComplete);
                }

                return true;
            });

            mp.start();

        } catch (Exception e) {

            Log.e("RAW_SOUND", "playRawSoundThen error", e);

            currentMediaPlayer = null;

            if (onComplete != null) {
                onComplete.run();
            }
        }
    }

    private void setupNowPlayingMarquee() {
        if (tvNowPlaying == null) return;

        tvNowPlaying.setSingleLine(true);
        tvNowPlaying.setHorizontallyScrolling(true);
        tvNowPlaying.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        tvNowPlaying.setMarqueeRepeatLimit(-1);
        tvNowPlaying.setSelected(true);
    }


    private void updateNowPlayingInfo(Sentence s) {
        if (tvNowPlaying == null) return;

        String topic = "";

        if (!TextUtils.isEmpty(lockCurrentSelectionLabel)) {
            topic = lockCurrentSelectionLabel;
        } else if (currentTopicOnLockscreen != null && !TextUtils.isEmpty(currentTopicOnLockscreen.name)) {
            topic = currentTopicOnLockscreen.name;
        }

        String prefix = (isAudioLooping || isFemaleLooping || isMaleLooping || isExampleLooping)
                ? "🔁"
                : (listeningSequentialMode ? "📚" : "🎧");

        tvNowPlaying.setText(TextUtils.isEmpty(topic) ? prefix + " Now Playing" : prefix + " " + topic);
        tvNowPlaying.setSelected(true);
    }


//Helper chỉnh thời gian tự Screen off khi không tương tác







    // hàm Helper screen off khi không tương tác
    private void cancelInactivityTimer() {
        if (inactivityHandler != null && inactivityRunnable != null) {
            inactivityHandler.removeCallbacks(inactivityRunnable);
        }
    }

    private void startInactivityTimer() {
        // Disabled: không tự về Home / không tự lock màn hình khi không tương tác
    }







    // ==== BẮT ĐẦU: THÊM 3 HÀM helper cho Screen Off====

    private ComponentName getAdminComponent() {
        return new ComponentName(this, MyDeviceAdminReceiver.class);
    }

    private boolean isDeviceAdminActive() {
        DevicePolicyManager dpm =
                (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm == null) return false;
        return dpm.isAdminActive(getAdminComponent());
    }

    /**
     * Gọi hàm này để yêu cầu bật quyền admin.
     * Nếu đã bật rồi thì return true, không mở màn hình xin quyền nữa.
     * Nếu chưa bật thì mở màn hình xin quyền (ACTION_ADD_DEVICE_ADMIN) và return false.
     */
    private boolean requestDeviceAdminIfNeeded() {
        if (isDeviceAdminActive()) {
            return true; // đã có quyền
        }

        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, getAdminComponent());
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Cho phép OPD 2nd khoá màn hình khi bật Auto Mode để nghe audio.");
        startActivity(intent);

        return false; // chưa có quyền, vừa mở màn hình xin quyền
    }

    // ==== KẾT THÚC ĐOẠN THÊM MỚI ====










    // ==== Switch Typing Check Tốt Nghiệp HELPERS ====
    private boolean isTypingCheckEnabledGlobally() {
        return enableTypingCheck;   // flag đọc từ switch
    }

















    private boolean isTypingCheckEnabledForCurrentSentence() {
        if (!enableTypingCheck) return false; // switch OFF = tắt hết

        // nếu muốn disable theo topic nữa thì thêm:
        // if (isListeningTopic(currentTopicOnLockscreen)) return false;

        return true;
    }


    // ==== FLOATING NOTE HELPERS ====

    private void toggleFloatingNote() {
        if (floatingNoteView == null) return;

        if (floatingNoteView.getVisibility() == View.VISIBLE) {
            hideFloatingNote();
        } else {
            showFloatingNote();
        }
    }


    private void recenterFloatingNote() {
        ConstraintLayout rootLayout = findViewById(R.id.rootLayout);
        if (rootLayout == null || floatingNoteView == null) return;

        rootLayout.post(() -> {
            int parentWidth = rootLayout.getWidth();
            int noteWidth = floatingNoteView.getWidth();
            if (parentWidth <= 0 || noteWidth <= 0) return;

            float centerX = (parentWidth - noteWidth) / 2f;
            floatingNoteView.setX(centerX);
            noteLastX = centerX;
        });
    }




    private void resetFloatingNoteToDefaultPosition() {
        if (floatingNoteView == null) return;

        // Scale về 1
        floatingNoteView.setScaleX(1f);
        floatingNoteView.setScaleY(1f);

        // Vị trí mặc định (ví dụ góc trên trái, chừa 16dp)
        ConstraintLayout rootLayout = findViewById(R.id.rootLayout);
        if (rootLayout != null) {
            float density = getResources().getDisplayMetrics().density;
            float margin = 16 * density;

            // Tuỳ bạn: góc trái trên
            floatingNoteView.setX(margin);
            floatingNoteView.setY(margin);

            // Cập nhật lại noteLastX/Y cho lần sau
            noteLastX = floatingNoteView.getX();
            noteLastY = floatingNoteView.getY();
        }

        // Hiện header
        View header = floatingNoteView.findViewById(R.id.floatingNoteHeader);
        if (header != null) {
            header.setVisibility(View.VISIBLE);
        }

        // Trả gravity & background
        if (edtFloatingNote != null) {
            edtFloatingNote.setGravity(Gravity.TOP | Gravity.START);
        }
        resetFloatingNoteBackground(); // nền vàng normal
    }

    private void zoomFloatingNoteToCenter(float scaleFactor) {
        if (floatingNoteView == null) return;

        ConstraintLayout rootLayout = findViewById(R.id.rootLayout);

        rootLayout.post(() -> {
            int parentWidth  = rootLayout.getWidth();
            int parentHeight = rootLayout.getHeight();

            int noteWidth  = floatingNoteView.getWidth();
            int noteHeight = floatingNoteView.getHeight();

            if (parentWidth == 0 || parentHeight == 0 || noteWidth == 0 || noteHeight == 0) {
                return;
            }

            // Tọa độ để note nằm giữa parent
            float targetX = (parentWidth  - noteWidth)  / 2f;
            float targetY = (parentHeight - noteHeight) / 2f;

            // Animate tới giữa + phóng to
            floatingNoteView.animate()
                    .x(targetX)
                    .y(targetY)
                    .scaleX(scaleFactor)
                    .scaleY(scaleFactor)
                    .setDuration(200)
                    .start();
        });
    }


    // ⭐ Chỉ dùng cho Android, không đụng luồng PC
    private void maybeFocusFloatingNoteOnNext() {
        // Tránh ảnh hưởng PC (scrcpy, keyboard cứng)
        if (isPcMode) return;

        if (!isFloatingNoteVisible || floatingNoteView == null || edtFloatingNote == null) return;
        if (switchFocusFloatingNote == null || !switchFocusFloatingNote.isChecked()) return;

        // Focus EditText + hiện keyboard ảo
        edtFloatingNote.requestFocus();
        edtFloatingNote.post(() -> {
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(edtFloatingNote, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }


    private void markFloatingNoteWrong() {
        if (floatingNoteRoot != null) {
            floatingNoteRoot.setBackgroundResource(R.drawable.bg_floating_note_wrong);
        }
    }

    private void resetFloatingNoteBackground() {
        if (floatingNoteRoot != null) {
            floatingNoteRoot.setBackgroundResource(R.drawable.bg_floating_note_normal);
        }
    }

    private void checkFloatingNoteAnswer() {
        if (currentSentence == null || edtFloatingNote == null) return;

        String userRaw = edtFloatingNote.getText().toString();
        String correctRaw = currentSentence.en != null ? currentSentence.en : "";

        String user = normalizeForCompare(userRaw);
        String correct = normalizeForCompare(correctRaw);

        boolean isCorrect = false;

        // 1) Trường hợp chuẩn: khớp hoàn toàn sau normalize
        if (!user.isEmpty() && user.equals(correct)) {
            isCorrect = true;
        } else if (!user.isEmpty()) {
            // 2) (OPTION) Cho phép user là 1 đoạn con liên tiếp trong câu đúng
            //    Ví dụ: "harbor am tour" trong "go on a harbour harbor am tour"
            String[] userTokens = user.split(" ");
            String[] correctTokens = correct.split(" ");

            if (userTokens.length <= correctTokens.length) {
                outer:
                for (int start = 0; start <= correctTokens.length - userTokens.length; start++) {
                    for (int i = 0; i < userTokens.length; i++) {
                        if (!userTokens[i].equals(correctTokens[start + i])) {
                            continue outer;
                        }
                    }
                    // Tìm được đoạn con liên tiếp
                    isCorrect = true;
                    break;
                }
            }
        }

        if (isCorrect) {
            markFloatingNoteCorrect();
        } else {
            markFloatingNoteWrong();
        }
    }

    /**
     * Đổi nền floating note sang XANH (đúng đáp án)
     * Giống màu nút Good trong rating
     */
    private void markFloatingNoteCorrect() {
        if (floatingNoteRoot != null) {
            floatingNoteRoot.setBackgroundResource(R.drawable.bg_floating_note_correct);
        }
    }

    private String normalizeForCompare(String s) {
        if (s == null) return "";

        // 1) Đưa về lowercase + trim
        String result = s.toLowerCase(Locale.ROOT).trim();

        // 2) Bỏ mọi ký tự KHÔNG phải: a-z, 0-9, hoặc whitespace
        //    (tức là bỏ: dấu câu, ngoặc, slash, gạch nối, v.v.)
        result = result.replaceAll("[^a-z0-9\\s]", "");

        // 3) Gom nhiều whitespace -> 1 space, rồi trim lại
        result = result.replaceAll("\\s+", " ").trim();

        return result;
    }


    // ⭐ HÀM CHUNG: XỬ LÝ FLOATING NOTE SAU KHI TRẢ LỜI (NẾU ĐANG MỞ)
    private void handleFloatingNoteAfterAnswer() {
        // Nếu không mở note thì thôi
        if (!isFloatingNoteVisible || floatingNoteView == null || edtFloatingNote == null) {
            return;
        }

        // 1) So sánh đáp án -> đổi nền xanh/đỏ
        checkFloatingNoteAnswer();      // đã gọi markFloatingNoteCorrect()/Wrong()

        // 2) Đưa note vào giữa + auto scale text
        moveFloatingNoteToCenter();
    }

    // ⭐ HÀM DÙNG CHUNG: SAU KHI USER MUỐN XEM ĐÁP ÁN (tap hoặc Enter)
    private void handleTapOrEnterShowAnswer() {
        if (currentSentence == null) return;

        // Đánh dấu: câu hiện tại đã show đáp án
        hasShownAnswerForCurrentSentence = true;
        hasShownAnswer = true; // nếu bạn đang dùng biến này cho swipe/next

        // 1) Nếu Floating Note đang mở: so đáp án + đổi nền + move center
        if (isFloatingNoteVisible && floatingNoteView != null && edtFloatingNote != null) {
            checkFloatingNoteAnswer();
            moveFloatingNoteToCenter();
        }

        // 2) Card: show EN/VI, example, audio, hiệu ứng
        showAnswerForCurrentSentenceFromTap();

        // Thu nhỏ ảnh + kéo block hình lên một chút để giảm gap
        shrinkImageForAnswer();
        adjustImageContainerForAnswer(true);

        // 3) Khi full đáp án: ẩn VI trên image, hiện VI dưới IPA
        if (tvVietnameseTop != null) {
            tvVietnameseTop.setVisibility(View.GONE);
        }
        if (tvVietnamese != null) {
            tvVietnamese.setVisibility(View.VISIBLE);
        }
    }






    private void showFloatingNote() {
        if (floatingNoteView == null) return;

        floatingNoteView.setScaleX(1f);
        floatingNoteView.setScaleY(1f);

        View header = floatingNoteView.findViewById(R.id.floatingNoteHeader);
        if (header != null) {
            header.setVisibility(View.VISIBLE);
        }

        if (edtFloatingNote != null) {
            float density = getResources().getDisplayMetrics().density;

            int paddingAll = (int) (4 * density);
            edtFloatingNote.setPadding(paddingAll, paddingAll, paddingAll, paddingAll);

            edtFloatingNote.setGravity(Gravity.CENTER);

            // ✅✅✅ CHỈ RESET WIDTH, KHÔNG RESET HEIGHT (GIỮ 48dp TỪ XML) ✅✅✅
            ViewGroup.LayoutParams params = edtFloatingNote.getLayoutParams();
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            // ❌ BỎ: params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            edtFloatingNote.setLayoutParams(params);

            edtFloatingNote.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);

            edtFloatingNote.setSingleLine(false);
            edtFloatingNote.setMaxLines(5);
            edtFloatingNote.setMaxWidth((int) (260 * density));

            if (autoScaleWatcher != null) {
                edtFloatingNote.removeTextChangedListener(autoScaleWatcher);
                autoScaleWatcher = null;
            }

            if (autoScaleRunnable != null) {
                autoScaleHandler.removeCallbacks(autoScaleRunnable);
                autoScaleRunnable = null;
            }
        }

        resetFloatingNoteBackground();

        floatingNoteView.setVisibility(View.VISIBLE);
        isFloatingNoteVisible = true;

        ConstraintLayout rootLayout = findViewById(R.id.rootLayout);

        if (noteLastX < 0 || noteLastY < 0) {
            floatingNoteView.post(() -> {
                if (rootLayout == null) return;

                int parentWidth = rootLayout.getWidth();
                int parentHeight = rootLayout.getHeight();
                int noteWidth = floatingNoteView.getWidth();
                int noteHeight = floatingNoteView.getHeight();

                if (parentWidth <= 0 || parentHeight <= 0
                        || noteWidth <= 0 || noteHeight <= 0) {
                    return;
                }

                float centerX = (parentWidth - noteWidth) / 2f;
                float centerY = parentHeight * 0.61f - noteHeight / 2f;
                if (centerY < 0) centerY = 0;

                floatingNoteView.setX(centerX);
                floatingNoteView.setY(centerY);

                noteLastX = centerX;
                noteLastY = centerY;
            });
        } else {
            floatingNoteView.setX(noteLastX);
            floatingNoteView.setY(noteLastY);
        }

        if (edtFloatingNote != null) {
            edtFloatingNote.requestFocus();
            edtFloatingNote.post(() -> {
                CharSequence text = edtFloatingNote.getText();
                if (text != null && text.length() > 0) {
                    edtFloatingNote.setSelection(0, text.length());
                }
                showKeyboard(edtFloatingNote);
            });
        }
    }

    private void hideFloatingNote() {
        if (floatingNoteView == null) return;

        isFloatingNoteVisible = false;

        floatingNoteView.setScaleX(1f);
        floatingNoteView.setScaleY(1f);
        floatingNoteView.setAlpha(1f);
        floatingNoteView.setVisibility(View.GONE);

        if (edtFloatingNote != null) {
            hideKeyboard(edtFloatingNote);
            edtFloatingNote.clearFocus();
        }

        // Khi tắt note, tạm coi như thoát PC mode
        isPcMode = false;
    }

    private void showKeyboard(View target) {
        if (target == null) return;
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        target.requestFocus();
        imm.showSoftInput(target, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideKeyboard(View target) {
        if (target == null) return;
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        IBinder windowToken = target.getWindowToken();
        if (windowToken != null) {
            imm.hideSoftInputFromWindow(windowToken, 0);
        }
    }


    private void rebuildTodayNewPool() {
        todayNewPool.clear();
        todayNewIndex = 0;

        if (sentences == null || sentences.isEmpty()) return;

        for (Sentence s : sentences) {
            if (s.srsReps == 0) {
                todayNewPool.add(s);
            }
        }

        if (!todayNewPool.isEmpty()) {
            Collections.shuffle(todayNewPool);
        }
    }





    // ⭐ Chọn file audio EN theo giọng (female/male/default)
    private String getEnAudioForVoice(Sentence s, String voiceType) {
        if (s == null) return null;

        if ("female".equals(voiceType)
                && s.audio_female != null && !s.audio_female.isEmpty()) {
            return s.audio_female;
        } else if ("male".equals(voiceType)
                && s.audio_male != null && !s.audio_male.isEmpty()) {
            return s.audio_male;
        } else {
            // fallback: dùng audio gốc
            return s.audio;
        }
    }



    // ⭐ Bắt đầu loop theo "voice" (female/male) với fallback riêng
    private void startVoiceLoop(String voiceType) {
        if (currentSentence == null) {
            Toast.makeText(this, "Không có câu để phát.", Toast.LENGTH_SHORT).show();
            return;
        }

        String file = null;

        // 1) Chọn file theo rule
        if ("female".equals(voiceType)) {
            // Female: audio_female → audio EN
            if (currentSentence.audio_female != null && !currentSentence.audio_female.isEmpty()) {
                file = currentSentence.audio_female;
                Log.d("VOICE_LOOP", "Female: using audio_female");
            } else if (currentSentence.audio != null && !currentSentence.audio.isEmpty()) {
                file = currentSentence.audio;
                Log.d("VOICE_LOOP", "Female: fallback to audio EN");
            }

        } else if ("male".equals(voiceType)) {
            // ⭐ Male: ưu tiên audio_male → example_audio → audio EN
            if (currentSentence.audio_male != null && !currentSentence.audio_male.isEmpty()) {
                file = currentSentence.audio_male;
                Log.d("VOICE_LOOP", "Male: using audio_male");

            } else if (currentSentence.example_audio != null && !currentSentence.example_audio.isEmpty()) {
                file = currentSentence.example_audio;
                Log.d("VOICE_LOOP", "Male: fallback to example_audio");

                hasPlayedExampleForCurrentSentence = true;
                Log.d("EXAMPLE_AUDIO", "startVoiceLoop male → fallback example, hasPlayedExampleForCurrentSentence=true, file=" + file);

            } else if (currentSentence.audio != null && !currentSentence.audio.isEmpty()) {
                file = currentSentence.audio;
                Log.d("VOICE_LOOP", "Male: fallback to audio EN");
            }
        }

        // 2) Nếu vẫn không có file → báo lỗi
        if (file == null || file.isEmpty()) {
            Toast.makeText(this, "Không có audio " + voiceType + " cho câu này.", Toast.LENGTH_SHORT).show();

            if ("female".equals(voiceType)) {
                isFemaleLooping = false;
            } else if ("male".equals(voiceType)) {
                isMaleLooping = false;
            }

            updateVoiceLoopButtonsUi();
            return;
        }

        // 3) Đảm bảo không chồng loop
        stopAudioLoop();
        stopAllVoiceLoops();

        if ("female".equals(voiceType)) {
            isFemaleLooping = true;
        } else if ("male".equals(voiceType)) {
            isMaleLooping = true;
        }

        final String currentFile = file;

        playAudio(file, false, () -> {
            if ("male".equals(voiceType) && tempPlayMaleOnce) {
                tempPlayMaleOnce = false;
                isMaleLooping = false;
                updateVoiceLoopButtonsUi();
                return;
            }

            boolean isUsingAudioVi = currentFile != null
                    && currentSentence.audio_vi != null
                    && currentFile.equals(currentSentence.audio_vi);

            if (isUsingAudioVi) {
                Log.d("VOICE_LOOP", "audio_vi fallback: single play only, stop loop");
                if ("male".equals(voiceType)) {
                    isMaleLooping = false;
                }
                updateVoiceLoopButtonsUi();
                return;
            }

            if (!hasShownAnswerForCurrentSentence && !listeningSequentialMode) {
                if ("female".equals(voiceType)) {
                    isFemaleLooping = false;
                } else if ("male".equals(voiceType)) {
                    isMaleLooping = false;
                }
                updateVoiceLoopButtonsUi();
                return;
            }

            if ("female".equals(voiceType) && isFemaleLooping) {
                if (femaleLoopRunnable == null) {
                    femaleLoopRunnable = () -> startVoiceLoop("female");
                }
                voiceLoopHandler.postDelayed(femaleLoopRunnable, delayBetweenAudioLoopMs);

            } else if ("male".equals(voiceType) && isMaleLooping) {
                if (maleLoopRunnable == null) {
                    maleLoopRunnable = () -> startVoiceLoop("male");
                }
                voiceLoopHandler.postDelayed(maleLoopRunnable, delayBetweenAudioLoopMs);
            }
        });
    }



    private void playMaleOrExampleOrViOnceFromImageDoubleTap() {
        if (currentSentence == null) return;

        String file = null;

        if (currentSentence.audio_male != null && !currentSentence.audio_male.isEmpty()) {
            file = currentSentence.audio_male;
            Log.d("IMG_DOUBLE_TAP", "using audio_male");

        } else if (currentSentence.example_audio != null && !currentSentence.example_audio.isEmpty()) {
            file = currentSentence.example_audio;
            hasPlayedExampleForCurrentSentence = true;
            Log.d("IMG_DOUBLE_TAP", "fallback to example_audio");

        } else if (currentSentence.audio_vi != null && !currentSentence.audio_vi.isEmpty()) {
            file = currentSentence.audio_vi;
            Log.d("IMG_DOUBLE_TAP", "fallback to audio_vi");
        }

        if (file == null || file.isEmpty()) {
            Toast.makeText(this, "Không có audio male/example/VI cho câu này.", Toast.LENGTH_SHORT).show();
            return;
        }

        stopAudioLoop();
        stopAllVoiceLoops();
        stopCurrentMediaPlayerSafely();

        playAudio(file, false, null);
    }

    // ⭐ Dừng loop cho 1 voice
    private void stopVoiceLoop(String voiceType) {
        if ("female".equals(voiceType)) {
            isFemaleLooping = false;
            if (femaleLoopRunnable != null) {
                voiceLoopHandler.removeCallbacks(femaleLoopRunnable);
            }
        } else if ("male".equals(voiceType)) {
            isMaleLooping = false;
            if (maleLoopRunnable != null) {
                voiceLoopHandler.removeCallbacks(maleLoopRunnable);
            }
        }
    }

    // ⭐ Dừng cả 2 loop voice (gọi khi đổi câu, onDestroy, v.v.)
    private void stopAllVoiceLoops() {
        stopVoiceLoop("female");
        stopVoiceLoop("male");
    }










    /**
     * ⭐ LOGIC NEXT CHUNG (EN/VI theo switch + audio EN + animation + SRS rating)
     */
    private void goNextWithRating(int rating) {
        TextView viTv  = findViewById(R.id.tvVietnamese);
        TextView ipaTv = findViewById(R.id.ipaText);

        Log.d("SRS_DEBUG", "goNextWithRating: rating=" + rating
                + " | hasShownAnswer=" + hasShownAnswer
                + " | audio=" + currentAudioEnFile);

        // Đọc flag từ app_settings
        SharedPreferences appPrefs = getSharedPreferences(PREFS_APP_SETTINGS, MODE_PRIVATE);
        boolean waitEnBeforeNext = appPrefs.getBoolean(KEY_WAIT_EN_BEFORE_NEXT, true);
        Log.d("SRS_DEBUG", "goNextWithRating: waitEnBeforeNext=" + waitEnBeforeNext);


        // Hành động next chung
        Runnable nextAction = () -> {
            Log.d("SRS_DEBUG", "  -> performNextSentence(" + rating + ")");
            performNextSentence(rating);
        };

        // 1) CHƯA XEM ĐÁP ÁN → dùng logic switch EN/VI
        if (!hasShownAnswer) {
            boolean enOn = (switchShowEnglish != null && switchShowEnglish.isChecked());
            boolean viOn = (switchShowVietnamese != null && switchShowVietnamese.isChecked());

            // Case A: VI ON, EN OFF → tắt VI, bật EN
            if (viOn && !enOn) {
                hideTextWithEffect(viTv);
                if (ipaTv != null) ipaTv.setVisibility(View.VISIBLE);
                revealTextWithEffect(enText);

                if (currentAudioEnFile != null && !currentAudioEnFile.isEmpty() && !isMuted) {
                    if (waitEnBeforeNext) {
                        // Giữ behaviour cũ: nghe EN xong rồi mới next
                        playAudioWithBounceEffect(currentAudioEnFile, nextAction);
                    } else {
                        // Không chờ: next ngay
                        nextAction.run();
                    }
                } else {
                    nextAction.run();
                }
                return;
            }

            // Case B: EN ON, VI OFF → tắt EN, bật VI
            if (enOn && !viOn) {
                hideTextWithEffect(enText);
                if (ipaTv != null) ipaTv.setVisibility(View.GONE);
                revealTextWithEffect(viTv);

                if (currentAudioEnFile != null && !currentAudioEnFile.isEmpty() && !isMuted) {
                    if (waitEnBeforeNext) {
                        playAudioWithBounceEffect(currentAudioEnFile, nextAction);
                    } else {
                        nextAction.run();
                    }
                } else {
                    nextAction.run();
                }
                return;
            }

            // Case C: cả hai OFF hoặc cả hai ON → bật cả EN + VI
            revealBothWithCascade(enText, viTv);
            if (ipaTv != null) ipaTv.setVisibility(View.VISIBLE);

            if (currentAudioEnFile != null && !currentAudioEnFile.isEmpty() && !isMuted) {
                if (waitEnBeforeNext) {
                    playAudioWithBounceEffect(currentAudioEnFile, nextAction);
                } else {
                    nextAction.run();
                }
            } else {
                nextAction.run();
            }
            return;
        }

        // 2) ĐÃ XEM ĐÁP ÁN → bỏ qua switch EN/VI, chỉ play EN + next
        if (currentAudioEnFile != null && !currentAudioEnFile.isEmpty() && !isMuted) {
            if (waitEnBeforeNext) {
                playAudioWithBounceEffect(currentAudioEnFile, nextAction);
            } else {
                nextAction.run();
            }
        } else {
            nextAction.run();
        }
    }


    private void animateExampleTap(View exampleContainer) {
        if (exampleContainer == null) return;

        exampleContainer.animate()
                .scaleX(0.97f)
                .scaleY(0.97f)
                .alpha(0.85f)
                .setDuration(70)
                .withEndAction(() -> exampleContainer.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setDuration(120)
                        .start()
                )
                .start();
    }




    /**
     * ⭐ HIỆU ỨNG ĐỠN GIẢN - PHÓNG TO NÚT ĐƯỢC BẤM
     */
    private void highlightSelectedButton(MaterialButton selectedBtn,
                                         MaterialButton btnAgain,
                                         MaterialButton btnHard,
                                         MaterialButton btnGood,
                                         MaterialButton btnEasy) {

        // ⭐ MỜ TẤT CẢ NÚT
        MaterialButton[] allButtons = {btnAgain, btnHard, btnGood, btnEasy};
        for (MaterialButton btn : allButtons) {
            if (btn != null) {
                btn.setAlpha(0.4f);  // ⭐ MỜ NGAY LẬP TỨC
                btn.setScaleX(0.95f);
                btn.setScaleY(0.95f);
            }
        }

        // ⭐ NỔI BẬT NÚT ĐƯỢC BẤM
        if (selectedBtn != null) {
            selectedBtn.setAlpha(1f);      // ⭐ SÁNG
            selectedBtn.setScaleX(1.1f);   // ⭐ TO HƠN
            selectedBtn.setScaleY(1.1f);
        }
    }


    private void underlineRatingButton(MaterialButton selectedBtn,
                                       MaterialButton btnAgain,
                                       MaterialButton btnHard,
                                       MaterialButton btnGood,
                                       MaterialButton btnEasy) {

        MaterialButton[] all = { btnAgain, btnHard, btnGood, btnEasy };

        for (MaterialButton btn : all) {
            if (btn == null) continue;

            // Reset về trạng thái bình thường
            btn.setAlpha(1f);
            btn.setScaleX(1f);
            btn.setScaleY(1f);
            btn.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
            btn.setPaintFlags(
                    btn.getPaintFlags() & (~Paint.UNDERLINE_TEXT_FLAG)
            );
            // Nếu bạn có màu nền mặc định riêng thì set lại ở đây
            // btn.setBackgroundTintList(ColorStateList.valueOf(defaultColor));
        }

        if (selectedBtn != null) {
            // Gạch chân + đậm để biết đây là rating đang “active”
            selectedBtn.setTypeface(Typeface.DEFAULT_BOLD);
            selectedBtn.setPaintFlags(
                    selectedBtn.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG
            );

            // Nhẹ thêm tí background để nổi hơn 3 nút còn lại (tùy bạn)
            // Ví dụ màu cam nhạt:
            // selectedBtn.setBackgroundTintList(
            //ColorStateList.valueOf(Color.parseColor("#FFE0B2"))
            //);
        }
    }



    private void resetSrsButtonsState() {
        MaterialButton btnAgain = findViewById(R.id.btnAgain);
        MaterialButton btnHard  = findViewById(R.id.btnHard);
        MaterialButton btnGood  = findViewById(R.id.btnGood);
        MaterialButton btnEasy  = findViewById(R.id.btnEasy);

        MaterialButton[] allButtons = {btnAgain, btnHard, btnGood, btnEasy};
        for (MaterialButton btn : allButtons) {
            if (btn != null) {
                btn.setAlpha(1f);     // sáng bình thường
                btn.setScaleX(1f);    // kích thước gốc
                btn.setScaleY(1f);
            }
        }
    }



    // ==================== 2 HÀM ANIMATION VỚI HARDWARE LAYER ====================



    private void showSentenceWithSlideTransition(boolean isPrev) {
        ImageView imgSentence = findViewById(R.id.imageView);
        if (imgSentence == null || imgSentence.getVisibility() != View.VISIBLE) {
            isImageAnimating = false;
            showSentence(isPrev);

            // ⭐ Câu được show ngay không qua anim → refocus sau khi show
            refocusFloatingNoteIfVisible();
            return;
        }

        float slideDistance = imgSentence.getWidth();
        float slideOut = isPrev ? slideDistance : -slideDistance;

        imgSentence.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        imgSentence.animate()
                .translationX(slideOut)
                .alpha(0f)
                .setDuration(120)
                .withEndAction(() -> {
                    imgSentence.setVisibility(View.GONE);
                    imgSentence.setTranslationX(0f);

                    imgSentence.setLayerType(View.LAYER_TYPE_NONE, null);

                    isImageAnimating = false;

                    // ⭐ Show câu mới (update toàn bộ UI)
                    showSentence(isPrev);

                    // ⭐ SAU KHI CÂU MỚI ĐÃ ĐƯỢC BIND UI → PC mode mới refocus Note
                    refocusFloatingNoteIfVisible();
                })
                .start();
    }




    private void revealFullAnswerAndStartLoop() {
        if (currentSentence == null) return;

        // Hiện full đáp án (EN + VI + example inline)
        handleTapOrEnterShowAnswer();   // hoặc showEnglishHideVietnamese() + showInlineExampleIfAny();

        // Bật loop example audio nếu có
        if (!TextUtils.isEmpty(currentSentence.example_audio)) {
            stopAudioLoop();
            isExampleLooping = true;
            isAudioLooping = true;
            startExampleAudioLoop();
        }
    }



    private void showSentenceWithScaleTransition(boolean isPrev) {
        ImageView imgSentence = findViewById(R.id.imageView);
        if (imgSentence == null || imgSentence.getVisibility() != View.VISIBLE) {
            isImageAnimating = false;
            showSentence(isPrev);

            // ❌ BỎ: auto reveal + loop khi Prev
            // if (isPrev) {
            //     revealFullAnswerAndStartLoop();
            // }

            refocusFloatingNoteIfVisible();
            return;
        }

        imgSentence.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        imgSentence.animate()
                .scaleX(0.8f)
                .scaleY(0.8f)
                .alpha(0f)
                .setDuration(120)
                .withEndAction(() -> {
                    imgSentence.setVisibility(View.GONE);
                    imgSentence.setScaleX(1f);
                    imgSentence.setScaleY(1f);

                    imgSentence.setLayerType(View.LAYER_TYPE_NONE, null);

                    isImageAnimating = false;

                    showSentence(isPrev);

                    // ❌ BỎ: auto reveal + loop khi Prev
                    // if (isPrev) {
                    //     revealFullAnswerAndStartLoop();
                    // }

                    refocusFloatingNoteIfVisible();
                })
                .start();
    }



// ==================== HẾT 2 HÀM ====================


    private void showInlineExampleIfAny() {
        if (currentSentence == null) return;

        if (TextUtils.isEmpty(currentSentence.example)) {
            // Không có ví dụ → ẩn container nếu đang hiện, nhưng KHÔNG toast nữa
            if (exampleContainer != null) {
                exampleContainer.setVisibility(View.GONE);
            }
            return;
        }

        // Chuẩn hóa chuỗi: "\\n" trong JSON → xuống dòng thật
        String ex = currentSentence.example.replace("\\\\n", "\n");
        String enPart = ex;
        String viPart = "";

        int idx = ex.indexOf('\n');
        if (idx >= 0) {
            enPart = ex.substring(0, idx).trim();
            viPart = ex.substring(idx + 1).trim();
        }

        if (exampleContainer != null) {
            if (tvExampleEnInline != null) {
                tvExampleEnInline.setText(enPart);
            }
            if (tvExampleViInline != null) {
                if (!TextUtils.isEmpty(viPart)) {
                    tvExampleViInline.setText(viPart);
                    tvExampleViInline.setVisibility(View.VISIBLE);
                } else {
                    tvExampleViInline.setText("");
                    tvExampleViInline.setVisibility(View.GONE);
                }
            }
            exampleContainer.setVisibility(View.VISIBLE);
        }
    }

    private void stopAllLoopsAndTimers() {
        isAudioLooping = false;
        isLoopingReplay = false;
        isExampleLooping = false;

        // 🔄 TẮT VOICE LOOP FEMALE/MALE
        stopVoiceLoop("female");
        stopVoiceLoop("male");
        isFemaleLooping = false;
        isMaleLooping = false;
        updateVoiceLoopButtonsUi();   // ✅ reset hiệu ứng 2 nút F/M

        stopAudioLoop();
        stopLoopVisualEffect();
        stopExampleLoopVisualEffect();
        stopEnglishBlowEffect();

        // ĐÃ BỎ: autoNextRunnable, handler.removeCallbacks(...)
    }


    private void startAudioLoopFromTap() {
        // Nếu đang loop rồi thì không làm gì (tap không tắt loop)
        if (isAudioLooping) return;
        if (currentSentence == null || TextUtils.isEmpty(currentSentence.audio)) return;

        ImageButton btnSpeaker = findViewById(R.id.btnSpeaker);

        isAudioLooping = true;
        if (btnSpeaker != null) {
            btnSpeaker.setImageResource(R.drawable.ic_pause);
        }



        startAudioLoop();
        startLoopVisualEffect();      // hiệu ứng loop EN
        startEnglishBlowEffect();     // thêm dòng này để text blow vàng khi bắt đầu loop
    }


    private void resetReplayFabState() {
        isAudioLooping = false;

        // Dừng mọi loop audio hiện tại (câu chính hoặc example)
        stopAudioLoop();          // hủy handler + dừng MediaPlayer
        resetExampleLoopState();  // tắt hiệu ứng + màu nút example

        if (fabReplay != null) {
            fabReplay.setImageResource(R.drawable.ic_replay_once); // icon trạng thái mặc định
            fabReplay.setAlpha(1.0f);
            fabReplay.setColorFilter(null);
            // hoặc nếu bạn có tint riêng thì set lại ở đây
        }

        // ⭐ THÊM: trả ảnh về trạng thái chuẩn trước khi sang câu mới
        resetScaleForNewSentence();
    }

    private void resetExampleLoopState() {
        isExampleLooping = false;
        // dừng cờ loop chung, nhưng KHÔNG stopAudioLoop() ở đây
        // vì hàm này có thể được gọi ở những nơi đã stop audio rồi
        stopExampleLoopVisualEffect();   // dừng animation nhún liên tục


    }



    // Mở panel
    private void expandView(View v) {
        v.measure(
                View.MeasureSpec.makeMeasureSpec(((View)v.getParent()).getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        int targetHeight = v.getMeasuredHeight();

        v.getLayoutParams().height = 0;
        v.setVisibility(View.VISIBLE);

        ValueAnimator animator = ValueAnimator.ofInt(0, targetHeight);
        animator.setDuration(250);
        animator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            v.getLayoutParams().height = value;
            v.requestLayout();
        });
        animator.start();
    }

    // Đóng panel
    private void collapseView(View v) {
        int initialHeight = v.getHeight();

        ValueAnimator animator = ValueAnimator.ofInt(initialHeight, 0);
        animator.setDuration(250);
        animator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            v.getLayoutParams().height = value;
            v.requestLayout();
        });

        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                v.setVisibility(View.GONE);

                ViewGroup.LayoutParams lp = v.getLayoutParams();
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                v.setLayoutParams(lp);
            }
        });

        animator.start();
    }

    private void showThreeSrsButtons() {
        MaterialButton btnAgain = findViewById(R.id.btnAgain);
        MaterialButton btnHard  = findViewById(R.id.btnHard);
        MaterialButton btnGood  = findViewById(R.id.btnGood);
        MaterialButton btnEasy  = findViewById(R.id.btnEasy);
        LinearLayout srsContainer = findViewById(R.id.srsButtonsContainer);

        // Nếu đang Listening theo thứ tự → ẩn luôn container + nút
        if (listeningSequentialMode) {
            if (srsContainer != null) srsContainer.setVisibility(View.GONE);
            if (btnAgain != null) btnAgain.setVisibility(View.GONE);
            if (btnHard  != null) btnHard.setVisibility(View.GONE);
            if (btnEasy  != null) btnEasy.setVisibility(View.GONE);
            if (btnGood  != null) btnGood.setVisibility(View.GONE);
            return;
        }

        // Bình thường (SRS mode)
        if (srsContainer != null) srsContainer.setVisibility(View.VISIBLE);
        if (btnAgain != null) btnAgain.setVisibility(View.VISIBLE);
        if (btnHard  != null) btnHard.setVisibility(View.VISIBLE);
        if (btnEasy  != null) btnEasy.setVisibility(View.VISIBLE);
        if (btnGood  != null) btnGood.setVisibility(View.GONE); // ẩn Good
    }

    private void showAllSrsButtons() {
        MaterialButton btnAgain = findViewById(R.id.btnAgain);
        MaterialButton btnHard  = findViewById(R.id.btnHard);
        MaterialButton btnGood  = findViewById(R.id.btnGood);
        MaterialButton btnEasy  = findViewById(R.id.btnEasy);
        LinearLayout srsContainer = findViewById(R.id.srsButtonsContainer);

        // Nếu đang Listening theo thứ tự → ẩn luôn container + nút
        if (listeningSequentialMode) {
            if (srsContainer != null) srsContainer.setVisibility(View.GONE);
            if (btnAgain != null) btnAgain.setVisibility(View.GONE);
            if (btnHard  != null) btnHard.setVisibility(View.GONE);
            if (btnEasy  != null) btnEasy.setVisibility(View.GONE);
            if (btnGood  != null) btnGood.setVisibility(View.GONE);
            return;
        }

        // Bình thường (SRS mode)
        if (srsContainer != null) srsContainer.setVisibility(View.VISIBLE);
        if (btnAgain != null) btnAgain.setVisibility(View.VISIBLE);
        if (btnHard  != null) btnHard.setVisibility(View.VISIBLE);
        if (btnEasy  != null) btnEasy.setVisibility(View.VISIBLE);
        if (btnGood  != null) btnGood.setVisibility(View.VISIBLE); // hiện lại Good
    }



    // ⭐ Hàm dùng chung: mô phỏng hành vi tap 1 lần để xem đáp án
    private void showAnswerForCurrentSentenceFromTap() {
        if (currentSentence == null) return;

        // 1) Show đáp án
        showEnglishHideVietnamese();

        // 2) Show luôn example inline (nếu có)
        showInlineExampleIfAny();

        // 3) Dừng mọi loop voice/EN hiện có trước khi play audio mới
        stopAudioLoop();        // nếu bạn có hàm này để dừng loop example/en
        stopAllVoiceLoops();    // dừng female + male loop
        updateVoiceLoopButtonsUi(); // reset hiệu ứng nút Female/Male về trạng thái không loop

        // 4) Audio + hiệu ứng như tap
        if (!isMuted) {
            startAudioLoopFromTap();
        }
        startEnglishBlowEffect();

        // 5) Hiệu ứng card + thu nhỏ image
        animateCardViewPress(cardSentence, handler, originalColor, pressedColor);
        shrinkImageForAnswer();
    }




    private void applySrsRating(Sentence s, int rating) {
        long now = System.currentTimeMillis();

        if (s == null) return;

        // Chuẩn hóa giá trị ban đầu
        if (s.srsEase <= 0) s.srsEase = 250;
        if (s.srsIntervalDays < 0) s.srsIntervalDays = 0;
        if (s.srsReps < 0) s.srsReps = 0;

        Log.d("SRS_DEBUG", "📝 applySrsRating: " + s.en);
        Log.d("SRS_DEBUG", "  rating=" + rating
                + ", before phase=" + s.srsPhase
                + ", phaseStep=" + s.phaseStep
                + ", reps=" + s.srsReps
                + ", intervalDays=" + s.srsIntervalDays);

        switch (rating) {
            case 0: { // Again: luôn reset về Again step 1
                s.srsReps = 0;
                s.srsEase = Math.max(130, s.srsEase - 20);

                s.againCount++;
                s.hardCount = 0;

                s.phaseStep = 1;
                scheduleAgainStep(s);
                break;
            }

            case 1: { // Hard: luôn reset về Hard step 1
                s.srsEase = Math.max(130, s.srsEase - 15);

                s.hardCount++;
                s.againCount = 0;

                s.phaseStep = 1;
                scheduleHardStep(s);
                break;
            }

            case 2: { // Good: để thuật toán kiểu Anki tự tính
                s.srsReps++;
                s.hardCount = 0;
                s.againCount = 0;

                s.swipePhase = SWIPE_PHASE_NORMAL;
                s.swipeStep = 0;

                s.srsPhase = "GOOD";
                s.phaseStep = 0;

                s.inLearning = false;

                double easeFactor = s.srsEase / 100.0;

                if (s.srsIntervalDays < 1) {
                    // Lần đầu Good: đưa sang review ngày mai
                    s.srsIntervalDays = 1;
                } else {
                    s.srsIntervalDays = Math.max(
                            1,
                            (int) Math.round(s.srsIntervalDays * easeFactor)
                    );
                }

                s.srsDueTime = now + s.srsIntervalDays * DAY;
                s.lastRating = 2;
                break;
            }

            case 3: { // Easy: Anki-style nhưng nhảy xa hơn Good
                s.srsReps = Math.max(1, s.srsReps + 1);
                s.srsEase = Math.min(300, s.srsEase + 15);

                s.hardCount = 0;
                s.againCount = 0;

                s.swipePhase = SWIPE_PHASE_NORMAL;
                s.swipeStep = 0;

                s.srsPhase = "EASY";
                s.phaseStep = 0;

                s.inLearning = false;

                double easeFactor = s.srsEase / 100.0;

                if (s.srsIntervalDays < 1) {
                    s.srsIntervalDays = 2;
                } else {
                    s.srsIntervalDays = Math.max(
                            2,
                            (int) Math.round(s.srsIntervalDays * easeFactor * 1.3)
                    );
                }

                s.srsDueTime = now + s.srsIntervalDays * DAY;
                s.lastRating = 3;
                break;
            }
        }

        // Giới hạn an toàn
        if (s.srsIntervalDays < 0) s.srsIntervalDays = 0;
        if (s.srsEase < 130) s.srsEase = 130;
        if (s.srsEase > 300) s.srsEase = 300;

        Log.d("SRS_DEBUG", "✅ after applySrsRating:"
                + " phase=" + s.srsPhase
                + ", phaseStep=" + s.phaseStep
                + ", lastRating=" + s.lastRating
                + ", reps=" + s.srsReps
                + ", intervalDays=" + s.srsIntervalDays
                + ", due=" + s.srsDueTime
                + ", inLearning=" + s.inLearning);
    }


    /**
     * Quyết định rating thực sự cho thao tác VUỐT (next nhanh),
     * dựa trên trạng thái swipePhase + swipeStep của câu.
     *
     * Trả về 0=Again, 1=Hard, 2=Good, 3=Easy
     */
    private int getRatingForSwipe(Sentence s) {
        if (s == null) return 2; // fallback Good

        // Câu mới hoàn toàn: chưa có srsReps, lastRating -1
        boolean isNewCard = (s.srsReps == 0 && (s.lastRating < 0 || s.lastRating > 3));

        if (isNewCard && s.swipePhase == 0) {
            // NEW: vuốt = Good "thật", đưa vào luồng Good (6h/1d tuỳ applySrsRating)
            s.swipePhase = 0;
            s.swipeStep = 0;
            return 2; // Good
        }

        // Đang ở vòng Again (từ khó)
        if (s.swipePhase == 1) {
            s.swipeStep++;

            if (s.swipeStep == 1) {
                // Again: vuốt lần 1 -> Again, 1 phút
                return 0; // Again
            } else if (s.swipeStep == 2) {
                // Again: vuốt lần 2 -> chuyển sang Hard-phase
                s.swipePhase = 2;  // chuyển sang vòng Hard
                s.swipeStep = 0;   // reset đếm cho Hard
                return 1; // Hard
            } else {
                // Các lần sau nữa: coi như đang ở Hard luôn
                s.swipePhase = 2;
                s.swipeStep = 0;
                return 1;
            }
        }

        // Đang ở vòng Hard
        if (s.swipePhase == 2) {
            s.swipeStep++;

            if (s.swipeStep == 1) {
                // Hard: vuốt lần 1 -> Hard bước 1 (5 phút)
                return 1;
            } else if (s.swipeStep == 2) {
                // Hard: vuốt lần 2 -> Hard bước 2 (5 phút lần nữa)
                return 1;
            } else if (s.swipeStep == 3) {
                // Hard: vuốt lần 3 -> Hard bước 3 (7 phút)
                return 1;
            } else if (s.swipeStep == 4) {
                // Hard: vuốt lần 4 -> Hard bước 4 (10 phút)
                return 1;
            } else {
                // Từ lần thứ 5 trở đi: chuyển qua Good (6h) và thoát vòng Hard
                s.swipePhase = 0;  // trở về trạng thái bình thường / Good
                s.swipeStep = 0;
                return 2; // Good
            }
        }

        // Mặc định (không ở vòng Again/Hard):
        // - Nếu lần trước user đánh Easy (3) -> vuốt = Easy
        // - Ngược lại -> vuốt = Good
        s.swipePhase = 0;
        s.swipeStep = 0;

        if (s.lastRating == 3) {
            return 3; // Easy
        } else {
            return 2; // Good
        }
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // 0) Global double-tap để hiện lại bubble khi đang lock
        if (globalLockGestureDetector != null) {
            globalLockGestureDetector.onTouchEvent(ev);
        }

        // 1) Floating Note
        if (ev.getAction() == MotionEvent.ACTION_DOWN
                && isFloatingNoteVisible
                && floatingNoteView != null
                && edtFloatingNote != null) {

            int[] loc = new int[2];
            floatingNoteView.getLocationOnScreen(loc);
            float x = ev.getRawX();
            float y = ev.getRawY();
            float left = loc[0];
            float top = loc[1];
            float right = left + floatingNoteView.getWidth();
            float bottom = top + floatingNoteView.getHeight();

            boolean insideNote = !(x < left || x > right || y < top || y > bottom);

            if (!insideNote) {
                checkFloatingNoteAnswer();
                hideKeyboard(edtFloatingNote);
                edtFloatingNote.clearFocus();
                moveFloatingNoteToCenter();
            } else {
                edtFloatingNote.requestFocus();
                showKeyboard(edtFloatingNote);
            }
        }

        // 2) Lock touch
        if (isTouchLocked) {
            if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {

                View barTop        = findViewById(R.id.swipeVerticalBarTop);
                View barBottom     = findViewById(R.id.swipeVerticalBarBottom);
                View barHorizontal = findViewById(R.id.swipeHorizontalBar);
                View centerTap     = findViewById(R.id.swipeCenterTap);
                View bubble        = lockTouchBubble;

                boolean allowed =
                        isTouchInsideView(barTop, ev)
                                || isTouchInsideView(barBottom, ev)
                                || isTouchInsideView(barHorizontal, ev)
                                || isTouchInsideView(centerTap, ev)
                                || isTouchInsideView(bubble, ev);

                if (!allowed) {
                    return true; // chặn mọi tap khác
                }
            }
        }

        return super.dispatchTouchEvent(ev);
    }



    @Override
    protected void onStop() {
        super.onStop();
        stopAllLoopsAndTimers();
        try { unregisterReceiver(topicChangedReceiver); } catch (Exception ignored) {}
    }

    private boolean isPointInsideFab(float rawX, float rawY) {
        if (fabReplay == null) return false;

        int[] loc = new int[2];
        fabReplay.getLocationOnScreen(loc);
        float fabX = loc[0];
        float fabY = loc[1];
        float fabRight = fabX + fabReplay.getWidth();
        float fabBottom = fabY + fabReplay.getHeight();

        return rawX >= fabX && rawX <= fabRight
                && rawY >= fabY && rawY <= fabBottom;
    }


    private BroadcastReceiver topicChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.OPD2nd.popup.TOPIC_CHANGED".equals(intent.getAction())) {
                String topicId  = intent.getStringExtra("topic_id");
                String topicFile = intent.getStringExtra("topic_file");
                if (topicId != null && topicFile != null) {
                    TopicInfo t = TopicManager.getTopicById(context, topicId);
                    if (t != null) {
                        loadSentencesForTopicOnLockScreen(t);  // đã có updateSrsStatsBar() bên trong (Bước 3)
                        shownHistoryKeys.clear();
                        shownHistoryIdx = -1;
                        currentSentence = null;
                        showSentence(false);
                        // updateSrsStatsBar();  // ⚠️ Không cần vì loadSentences... đã gọi rồi
                    }
                }
            }
        }
    };






    private void animateFabSingleTap() {
        if (fabReplay == null) return;

        fabReplay.animate()
                .scaleX(0.85f).scaleY(0.85f).alpha(0.8f)
                .setDuration(80)
                .withEndAction(() ->
                        fabReplay.animate()
                                .scaleX(1f).scaleY(1f).alpha(1f)
                                .setDuration(120)
                                .start()
                )
                .start();
    }


    private void openTypingMode() {
        // Nếu bạn muốn vẫn chặn khi chưa có câu:
        if (currentSentence == null) return;

        Intent intent = new Intent(LockScreenActivity.this, TypingActivity.class);
        // Không cần putExtra EN/VI/IPA/IMAGE/AUDIO nữa
        startActivity(intent);
    }

    private void updateSrsStatsBar() {
        Log.d("UpdateStats", "=== BẮT ĐẦU updateSrsStatsBar() ===");

        TextView tvQuota = findViewById(R.id.tvSrsStatsQuota);
        TextView tvRating = findViewById(R.id.tvSrsStatsRating);

        if (tvQuota == null || tvRating == null) {
            Log.e("UpdateStats", "❌ TextView NULL!");
            return;
        }

        long now = System.currentTimeMillis();

        int totalDue = 0;
        int countAgain = 0;
        int countHard = 0;
        int countGood = 0;
        int countEasy = 0;

        if (sentences != null) {
            for (Sentence s : sentences) {
                if (s == null) continue;

                if (s.srsReps > 0 && s.srsDueTime > 0 && s.srsDueTime <= now) {
                    totalDue++;
                }

                String phase = s.srsPhase != null ? s.srsPhase.trim().toUpperCase() : "";

                boolean hasSrsState = !phase.isEmpty() || s.srsReps > 0 || s.srsDueTime > 0;

                if (!hasSrsState) continue;

                // Again vẫn phải đếm dù srsReps = 0
                if ("AGAIN".equals(phase) || s.lastRating == 0) {
                    countAgain++;
                } else if ("HARD".equals(phase) || s.lastRating == 1) {
                    countHard++;
                } else if ("GOOD".equals(phase) || s.lastRating == 2) {
                    countGood++;
                } else if ("EASY".equals(phase) || s.lastRating == 3) {
                    countEasy++;
                }
            }
        }

        int totalLearned = countAgain + countHard + countGood + countEasy;

        int dailyNewLimit = currentTopicNewLimit;
        int dailyReviewLimit = currentTopicReviewLimit;

        int todayNewDone = currentTopicNewDone;
        int todayReviewDone = currentTopicReviewDone;

        Log.d("UpdateStats", "Total LEARNED: " + totalLearned);
        Log.d("UpdateStats", "Again=" + countAgain + " Hard=" + countHard
                + " Good=" + countGood + " Easy=" + countEasy);
        Log.d("UpdateStats", "Total DUE: " + totalDue);

        float newPercent = dailyNewLimit > 0 ? (todayNewDone * 100f / dailyNewLimit) : 0;
        float reviewPercent = dailyReviewLimit > 0 ? (todayReviewDone * 100f / dailyReviewLimit) : 0;

        int newColor = newPercent >= 100 ? Color.parseColor("#4CAF50")
                : newPercent >= 70 ? Color.parseColor("#FFC107")
                : Color.parseColor("#FF9800");

        int reviewColor = reviewPercent >= 100 ? Color.parseColor("#4CAF50")
                : reviewPercent >= 70 ? Color.parseColor("#FFC107")
                : Color.parseColor("#FF9800");

        String quotaText = "Total: " + totalLearned
                + " | New: " + todayNewDone + "/" + dailyNewLimit
                + " | Review: " + todayReviewDone + "/" + dailyReviewLimit;

        SpannableString quotaSpan = new SpannableString(quotaText);

        int totalStart = quotaText.indexOf("Total: ") + 7;
        int totalEnd = totalStart + String.valueOf(totalLearned).length();
        if (totalStart >= 7 && totalEnd <= quotaSpan.length()) {
            quotaSpan.setSpan(new ForegroundColorSpan(Color.parseColor("#4CAF50")),
                    totalStart, totalEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        int newStart = quotaText.indexOf("New: ") + 5;
        int newEnd = newStart + String.valueOf(todayNewDone).length();
        if (newStart >= 5 && newEnd <= quotaSpan.length()) {
            quotaSpan.setSpan(new ForegroundColorSpan(newColor),
                    newStart, newEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        int reviewStart = quotaText.indexOf("Review: ") + 8;
        int reviewEnd = reviewStart + String.valueOf(todayReviewDone).length();
        if (reviewStart >= 8 && reviewEnd <= quotaSpan.length()) {
            quotaSpan.setSpan(new ForegroundColorSpan(reviewColor),
                    reviewStart, reviewEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        tvQuota.setText(quotaSpan);

        String ratingText = "Cần Ôn: " + totalDue
                + " | Again: " + countAgain
                + " | Hard: " + countHard
                + " | Good: " + countGood
                + " | Easy: " + countEasy;

        SpannableString ratingSpan = new SpannableString(ratingText);

        int dueColor;
        if (totalDue == 0) {
            dueColor = Color.parseColor("#4CAF50");
        } else if (dailyReviewLimit > 0 && totalDue > dailyReviewLimit) {
            dueColor = Color.parseColor("#F44336");
        } else if (dailyReviewLimit > 0 && totalDue > dailyReviewLimit * 0.7f) {
            dueColor = Color.parseColor("#FF9800");
        } else {
            dueColor = Color.parseColor("#FFC107");
        }

        int dueStart = ratingText.indexOf("Cần Ôn: ") + 8;
        int dueEnd = dueStart + String.valueOf(totalDue).length();
        if (dueStart >= 8 && dueEnd <= ratingSpan.length()) {
            ratingSpan.setSpan(new ForegroundColorSpan(dueColor),
                    dueStart, dueEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        highlightNumberAfterSymbol(ratingSpan, ratingText, "Again: ", String.valueOf(countAgain),
                Color.parseColor("#F44336"));
        highlightNumberAfterSymbol(ratingSpan, ratingText, "Hard: ", String.valueOf(countHard),
                Color.parseColor("#FF9800"));
        highlightNumberAfterSymbol(ratingSpan, ratingText, "Good: ", String.valueOf(countGood),
                Color.parseColor("#4CAF50"));
        highlightNumberAfterSymbol(ratingSpan, ratingText, "Easy: ", String.valueOf(countEasy),
                Color.parseColor("#2196F3"));

        tvRating.setText(ratingSpan);

        Log.d("UpdateStats", "=== KẾT THÚC updateSrsStatsBar() ===");
    }



    private void fixMissingRatings() {
        if (sentences == null) return;

        boolean fixed = false;

        for (Sentence s : sentences) {
            if (s == null) continue;

            // ✅ NẾU ĐÃ HỌC NHƯNG CHƯA CÓ RATING → GÁN MẶC ĐỊNH
            if (s.srsReps > 0 && s.lastRating < 0) {
                s.lastRating = 2;  // ⭐ MẶC ĐỊNH = Good
                fixed = true;
                Log.d("FixData", "✅ Fixed missing rating for: " + s.en);
            }
        }

        if (fixed) {
            saveSentencesToFile();
            Log.d("FixData", "✅ Đã sửa xong dữ liệu thiếu rating");

            // ✅ LƯU FLAG ĐÃ FIX CHO TOPIC NÀY
            if (currentTopicOnLockscreen != null) {
                SharedPreferences prefs = getSharedPreferences("TOPIC_STATS", MODE_PRIVATE);
                prefs.edit()
                        .putBoolean("topic_" + currentTopicOnLockscreen.id + "_rating_fixed", true)
                        .apply();
            }
        }
    }





    private void playViAudioOnceForCurrentSentence() {
        if (currentSentence == null) {
            return;
        }

        Log.d(
                "AUDIO_VI",
                "playViAudioOnce"
                        + " | hasPlayedExample="
                        + hasPlayedExampleForCurrentSentence
                        + " | example_audio_vi="
                        + currentSentence.example_audio_vi
                        + " | audio_vi="
                        + currentSentence.audio_vi
                        + " | volume="
                        + audioViVolume
        );

        // =========================================================
        // CHỌN FILE AUDIO VI CẦN PHÁT
        // =========================================================
        String targetFile;

        /*
         * Nếu trước đó đã phát Example EN và câu hiện tại có
         * example_audio_vi thì ưu tiên phát Example VI.
         */
        if (hasPlayedExampleForCurrentSentence
                && !TextUtils.isEmpty(
                currentSentence.example_audio_vi
        )) {

            targetFile =
                    currentSentence.example_audio_vi.trim();

            Log.d(
                    "AUDIO_VI",
                    "Use example_audio_vi: "
                            + targetFile
            );

        } else {
            targetFile =
                    currentSentence.audio_vi;

            if (TextUtils.isEmpty(targetFile)) {
                Toast.makeText(
                        this,
                        "Không có audio VI cho câu này.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            targetFile =
                    targetFile.trim();

            Log.d(
                    "AUDIO_VI",
                    "Use audio_vi: "
                            + targetFile
            );
        }

        // =========================================================
        // DỪNG AUDIO VI CŨ NẾU ĐANG PHÁT
        // =========================================================
        if (isViAudioPlaying) {
            Log.d(
                    "AUDIO_VI",
                    "VI audio was playing -> stop and restart"
            );

            stopAudioLoop();
            stopAllVoiceLoops();
            stopCurrentMediaPlayerSafely();

            isViAudioPlaying =
                    false;
        }

        // Dừng mọi nguồn audio khác trước khi phát VI
        stopAudioLoop();
        stopAllVoiceLoops();
        stopCurrentMediaPlayerSafely();

        isViAudioPlaying =
                true;

        // =========================================================
        // PHÁT AUDIO VI VỚI VOLUME RIÊNG
        // =========================================================
        playAudio(
                targetFile,
                false,
                () -> {
                    Log.d(
                            "AUDIO_VI",
                            "audio_vi/example_audio_vi completed"
                    );

                    isViAudioPlaying =
                            false;
                },
                audioViVolume
        );
    }



    private void stopCurrentMediaPlayerSafely() {
        if (currentMediaPlayer != null) {
            try {
                currentMediaPlayer.stop();
            } catch (Exception ignore) {}
            try {
                currentMediaPlayer.reset();
            } catch (Exception ignore) {}
            try {
                currentMediaPlayer.release();
            } catch (Exception ignore) {}
            currentMediaPlayer = null;
        }
    }



    private void playReplayOnce() {
        if (isMuted) return;

        if (currentAudioEnFile != null && !currentAudioEnFile.isEmpty()) {
            playAudio(currentAudioEnFile, false, null);
        } else if (currentAudioViFile != null && !currentAudioViFile.isEmpty()) {
            playAudio(currentAudioViFile, false, null);
        } else {
            Toast.makeText(this, "Không có audio để phát lại", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleReplayLoop() {
        isLoopingReplay = !isLoopingReplay;

        if (!isLoopingReplay) {
            // Tắt loop
            isAudioLooping = false;
            if (currentMediaPlayer != null) {
                try {
                    currentMediaPlayer.stop();
                    currentMediaPlayer.release();
                } catch (Exception ignored) {}
                currentMediaPlayer = null;
            }
            if (fabReplay != null) {
                fabReplay.setImageResource(R.drawable.ic_replay_once);
            }
            Toast.makeText(this, "Loop OFF", Toast.LENGTH_SHORT).show();
            stopLoopVisualEffect();
            return;
        }

        if (isMuted) {
            isLoopingReplay = false;
            return;
        }

        String audioToLoop = null;
        if (currentAudioEnFile != null && !currentAudioEnFile.isEmpty()) {
            audioToLoop = currentAudioEnFile;
        } else if (currentAudioViFile != null && !currentAudioViFile.isEmpty()) {
            audioToLoop = currentAudioViFile;
        }

        if (audioToLoop == null) {
            isLoopingReplay = false;
            Toast.makeText(this, "Không có audio để loop", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Loop ON", Toast.LENGTH_SHORT).show();

        if (fabReplay != null) {
            fabReplay.setImageResource(R.drawable.ic_replay_loop);
        }

        // 👉 Dùng cùng cơ chế loop với nút loa: delayBetweenAudioLoopMs
        isAudioLooping = true;
        startAudioLoop();
        startLoopVisualEffect();
    }









    private void shrinkImageForAnswer() {
        ShapeableImageView imgSentence = findViewById(R.id.imageView);
        if (imgSentence == null) return;

        float targetScale = 0.6f;

        if (Math.abs(imgSentence.getScaleX() - targetScale) < 0.01f &&
                Math.abs(imgSentence.getScaleY() - targetScale) < 0.01f) {
            return;
        }

        imgSentence.animate().cancel();

        imgSentence.animate()
                .scaleX(targetScale)
                .scaleY(targetScale)
                .setDuration(150)
                .start();
    }


    private void adjustImageContainerForAnswer(boolean showAnswer) {
        RelativeLayout imageContainer = findViewById(R.id.imageContainer);
        if (imageContainer == null) return;

        ViewGroup.MarginLayoutParams lp =
                (ViewGroup.MarginLayoutParams) imageContainer.getLayoutParams();

        float density = getResources().getDisplayMetrics().density;

        if (showAnswer) {
            // Khi SHOW đáp án: kéo block hình lên một chút để giảm gap
            lp.bottomMargin = (int) (-12 * density); // thử -12dp, không thích thì chỉnh số này
        } else {
            // Khi CÂU MỚI: trả margin về như XML (4dp)
            lp.bottomMargin = (int) (4 * density);
        }

        imageContainer.setLayoutParams(lp);
    }



    private void resetScaleForNewSentence() {
        ShapeableImageView imgSentence = findViewById(R.id.imageView);
        if (imgSentence == null) return;

        // Dừng mọi animation đang chạy trên ImageView (rất quan trọng)
        imgSentence.animate().cancel();

        // Trả ảnh về đúng scale gốc (không làm thay đổi layout params)
        imgSentence.setScaleX(1f);
        imgSentence.setScaleY(1f);

        // Reset translationY về 0 để ảnh trở về vị trí trung tâm (không bị dịch lên)
        imgSentence.setTranslationY(0f);

        // Bảo đảm alpha và visibility về trạng thái bình thường
        imgSentence.setAlpha(1f);
    }



    private void applyImageVisibility(ShapeableImageView imgSentence) {
        if (imgSentence == null) return;

        // 1. Theo cài đặt chung trong code (showImageInPopup)
        boolean visible = showImageInPopup;

        // 2. Kết hợp với trạng thái của switchShowImage trên UI
        SwitchMaterial switchShowImage = findViewById(R.id.switchShowImage);
        if (switchShowImage != null) {
            visible = visible && switchShowImage.isChecked();
        }

        // 3. Quyết định cuối cùng
        imgSentence.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void loadSentencesForTopicOnLockScreen(TopicInfo topic) {
        if (topic == null) return;

        if (currentTopicOnLockscreen != null
                && currentTopicOnLockscreen.id != null
                && !TextUtils.equals(currentTopicOnLockscreen.id, topic.id)) {
            saveCurrentTopicStats();
        }

        stopAudioLoop();
        stopExampleLoopVisualEffect();
        stopAllVoiceLoops();
        stopCurrentMediaPlayerSafely();

        if (topicPackageManager != null) {
            try {
                topicPackageManager.close();
            } catch (Exception ignored) {}
            topicPackageManager = null;
        }

        currentTopicOnLockscreen = topic;

        try {
            String packageTopicName = getTopicPackageNameFromJsonFile(topic.fileName);
            topicPackageManager = new TopicPackageManager(this, packageTopicName);
            Log.d("LOCKSCREEN_TOPIC", "Package manager reloaded: " + packageTopicName);
        } catch (Exception e) {
            topicPackageManager = null;
            Log.e("LOCKSCREEN_TOPIC", "Cannot create TopicPackageManager for: " + topic.fileName, e);
        }

        currentSentence = null;
        hasShownAnswer = false;
        hasShownAnswerForCurrentSentence = false;

        currentAudioEnFile = null;
        currentAudioViFile = null;

        isAudioLooping = false;
        isExampleLooping = false;
        isFemaleLooping = false;
        isMaleLooping = false;

        if (shownHistoryKeys != null) shownHistoryKeys.clear();
        if (historyCache != null) historyCache.clear();
        shownHistoryIdx = -1;

        if (inSessionLearningQueue != null) inSessionLearningQueue.clear();
        consecutiveLearningShown = 0;

        if (todayNewPool != null) todayNewPool.clear();
        todayNewIndex = 0;

        typingCheckSentence = null;
        typingFailedAndShowingAnswer = false;
        pendingRatingAfterTyping = -1;

        getSharedPreferences("locks_settings", MODE_PRIVATE)
                .edit()
                .putString("current_topic_id", topic.id)
                .apply();

        getSharedPreferences("lockscreen_prefs", MODE_PRIVATE)
                .edit()
                .putString("current_topic_id", topic.id)
                .putString("current_topic_file", topic.fileName)
                .apply();

        Log.d("LOCKSCREEN_TOPIC", "Saved current_topic_id: " + topic.id
                + " file=" + topic.fileName);

        try {
            InputStream is = openTopicInputStreamSafe(topic.fileName);

            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder json = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                json.append(line);
            }

            br.close();

            sentences = new Gson().fromJson(
                    json.toString(),
                    new TypeToken<List<Sentence>>() {}.getType()
            );

            if (sentences == null) {
                sentences = new ArrayList<>();
            }

            for (int i = 0; i < sentences.size(); i++) {
                Sentence s = sentences.get(i);
                if (s != null && s.id == -1) {
                    s.id = i;
                }
            }

            Log.d("LOAD_SENTENCES", "Assigned IDs to " + sentences.size()
                    + " sentences topic=" + topic.name);

            recalcRatingCounters();

            SharedPreferences prefs = getSharedPreferences("TOPIC_STATS", MODE_PRIVATE);
            boolean alreadyFixed = prefs.getBoolean("topic_" + topic.id + "_rating_fixed", false);
            if (!alreadyFixed) {
                fixMissingRatings();
            }

            restoreTopicStats(topic.id);

            updateStats();
            updateSuggestionList();
            updateSrsStatsBar();
            rebuildTodayNewPool();

            // ⭐ Nếu đang có lesson đã chọn thì KHÔNG pick/show ở đây.
            // onCreate/onResume sẽ gọi restoreSelectedLessonIfNeeded()
            // rồi restoreCurrentLockscreenPosition() để về đúng câu cũ, ví dụ 05/15.
            if (lockCurrentLessonKeys != null && !lockCurrentLessonKeys.isEmpty()) {
                Log.d("LOCKSCREEN_TOPIC",
                        "Lesson selection exists, skip showSentence here. keys="
                                + lockCurrentLessonKeys);
                return;
            }

            showSentence(false);

            if (currentSentence != null) {
                updateCardViewWithSentence(currentSentence);
                updateNowPlayingHeader();
                updateNowPlayingInfo(currentSentence);
                syncSequentialIndexWithCurrentSentence();
            }

        } catch (Exception e) {
            Log.e("LOCKSCREEN_TOPIC", "Lỗi đọc dữ liệu chủ đề. fileName="
                    + (topic != null ? topic.fileName : "null")
                    + ", topicName="
                    + (topic != null ? topic.name : "null"), e);

            Toast.makeText(this,
                    "Lỗi đọc dữ liệu chủ đề: "
                            + (topic != null ? topic.fileName : "null"),
                    Toast.LENGTH_LONG).show();
        }
    }

    private InputStream openTopicInputStreamSafe(String topicFileName) throws Exception {
        if (TextUtils.isEmpty(topicFileName)) {
            throw new Exception("topicFileName is empty");
        }

        String name = topicFileName.trim();
        String baseName = name;

        int slash = Math.max(baseName.lastIndexOf('/'), baseName.lastIndexOf('\\'));
        if (slash >= 0) {
            baseName = baseName.substring(slash + 1);
        }

        // 1) internal exact
        File f1 = new File(getFilesDir(), name);
        if (f1.exists() && f1.length() > 0) {
            return new FileInputStream(f1);
        }

        // 2) internal basename
        File f2 = new File(getFilesDir(), baseName);
        if (f2.exists() && f2.length() > 0) {
            return new FileInputStream(f2);
        }

        // 3) assets exact
        try {
            return getAssets().open(name);
        } catch (Exception ignored) {}

        // 4) assets/topics/basename
        try {
            return getAssets().open("topics/" + baseName);
        } catch (Exception ignored) {}

        // 5) assets basename
        try {
            return getAssets().open(baseName);
        } catch (Exception ignored) {}

        throw new Exception("Không tìm thấy topic file: " + topicFileName
                + " | tried: " + f1.getAbsolutePath()
                + " ; " + f2.getAbsolutePath()
                + " ; assets/" + name
                + " ; assets/topics/" + baseName
                + " ; assets/" + baseName);
    }

    private void saveCurrentTopicStats() {
        if (currentTopicOnLockscreen == null) return;

        // 🔒 KHÔNG cho save nếu chưa restore quota lần nào trong phiên
        if (!hasRestoredTopicStats) {
            Log.d("TopicStats", "⏭️ Skip saveCurrentTopicStats (hasRestoredTopicStats = false)");
            return;
        }

        String topicId = currentTopicOnLockscreen.id;

        SharedPreferences prefs = getSharedPreferences("TOPIC_STATS", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        String prefix = "topic_" + topicId + "_";

        editor.putString(prefix + "name", currentTopicOnLockscreen.name);
        editor.putString(prefix + "fileName", currentTopicOnLockscreen.fileName);

        editor.putInt(prefix + "newLimit", currentTopicNewLimit);
        editor.putInt(prefix + "reviewLimit", currentTopicReviewLimit);

        editor.putInt(prefix + "newDone", currentTopicNewDone);
        editor.putInt(prefix + "reviewDone", currentTopicReviewDone);

        int total = (sentences != null) ? sentences.size() : 0;
        int totalLearned = 0;
        int totalDue = 0;
        int countAgain = 0;
        int countHard = 0;
        int countGood = 0;
        int countEasy = 0;

        long now = System.currentTimeMillis();

        if (sentences != null) {
            for (Sentence s : sentences) {
                if (s == null) continue;
                if (s.srsReps > 0) {
                    totalLearned++;
                    if (s.srsDueTime > 0 && s.srsDueTime <= now) totalDue++;

                    switch (s.lastRating) {
                        case 0: countAgain++; break;
                        case 1: countHard++; break;
                        case 2: countGood++; break;
                        case 3: countEasy++; break;
                    }
                }
            }
        }

        editor.putInt(prefix + "total", total);
        editor.putInt(prefix + "totalLearned", totalLearned);
        editor.putInt(prefix + "totalDue", totalDue);
        editor.putInt(prefix + "countAgain", countAgain);
        editor.putInt(prefix + "countHard", countHard);
        editor.putInt(prefix + "countGood", countGood);
        editor.putInt(prefix + "countEasy", countEasy);

        long todayDay = getTodayLocalDayIndex();
        editor.putLong(prefix + "lastDay", todayDay);

        editor.commit();

        Log.d("TopicStats", "✅ Saved stats for topic: " + topicId
                + " (" + currentTopicOnLockscreen.name + ")"
                + " | FileName=" + currentTopicOnLockscreen.fileName
                + " | NewLimit=" + currentTopicNewLimit
                + " | ReviewLimit=" + currentTopicReviewLimit
                + " | NewDone=" + currentTopicNewDone
                + " | ReviewDone=" + currentTopicReviewDone
                + " | Total=" + total
                + " | Learned=" + totalLearned
                + " | Again=" + countAgain
                + " | Hard=" + countHard
                + " | Good=" + countGood
                + " | Easy=" + countEasy
                + " | lastDay=" + todayDay);
    }

    // Biến global cho topic hiện tại
    private int currentTopicNewLimit = 50;
    private int currentTopicReviewLimit = 100;
    private int currentTopicNewDone = 0;
    private int currentTopicReviewDone = 0;

    private void restoreTopicStats(String topicId) {
        if (topicId == null || topicId.isEmpty()) return;

        SharedPreferences prefs = getSharedPreferences("TOPIC_STATS", MODE_PRIVATE);
        String prefix = "topic_" + topicId + "_";

        int newLimit = prefs.getInt(prefix + "newLimit", 50);
        int reviewLimit = prefs.getInt(prefix + "reviewLimit", 100);
        int newDone = prefs.getInt(prefix + "newDone", 0);
        int reviewDone = prefs.getInt(prefix + "reviewDone", 0);

        currentTopicNewLimit = newLimit;
        currentTopicReviewLimit = reviewLimit;
        currentTopicNewDone = newDone;
        currentTopicReviewDone = reviewDone;

        hasRestoredTopicStats = true; // ✅ Quan trọng

        Log.d("TopicStats", "✅ Restored stats for topic: " + topicId
                + " | NewLimit=" + newLimit
                + " | ReviewLimit=" + reviewLimit
                + " | NewDone=" + newDone
                + " | ReviewDone=" + reviewDone);
    }


    private void resetCurrentTopicDailyCountersIfNeeded() {
        if (currentTopicOnLockscreen == null || currentTopicOnLockscreen.id == null) {
            Log.d("SRS_DEBUG", "resetCurrentTopicDailyCountersIfNeeded: no current topic, skip");
            return;
        }

        String topicId = currentTopicOnLockscreen.id;
        SharedPreferences prefs = getSharedPreferences("TOPIC_STATS", MODE_PRIVATE);
        String prefix = "topic_" + topicId + "_";

        long todayDay = getTodayLocalDayIndex(); // ví dụ: số ngày local từ epoch
        long lastDay  = prefs.getLong(prefix + "lastDay", -1L);

        Log.d("SRS_DEBUG", "resetCurrentTopicDailyCountersIfNeeded: topicId=" + topicId
                + ", lastDay=" + lastDay + ", todayDay=" + todayDay);

        if (lastDay == todayDay) {
            Log.d("SRS_DEBUG", "Same day for topic " + topicId + " → keep daily counters");
            return;
        }

        Log.d("SRS_DEBUG", "New day for topic " + topicId + " → reset daily counters (newDone/reviewDone=0)");

        SharedPreferences.Editor ed = prefs.edit();
        ed.putInt(prefix + "newDone", 0);
        ed.putInt(prefix + "reviewDone", 0);
        ed.putLong(prefix + "lastDay", todayDay);
        ed.commit(); // dùng commit cho chắc khi reset theo ngày [web:314][web:319]

        // Cập nhật biến runtime cho topic hiện tại
        currentTopicNewDone = 0;
        currentTopicReviewDone = 0;
    }











    private void loadGlobalSentences() {
        globalSentences.clear();
        try {
            File file = new File(getFilesDir(), "all_sentences_global.json");
            if (!file.exists()) return;

            InputStream is = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) json.append(line);
            br.close();

            List<Sentence> loaded = new Gson().fromJson(
                    json.toString(),
                    new TypeToken<List<Sentence>>(){}.getType()
            );
            if (loaded != null) globalSentences.addAll(loaded);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ⭐ THÊM: Show next sentence trong auto mode
     */






    // 1. Hàm cập nhật suggestionList và adapter (dùng globalSentences)
    private void updateSuggestionList() {
        Set<String> tempSet = new LinkedHashSet<>();
        for (Sentence s : globalSentences) {
            if (!TextUtils.isEmpty(s.en)) tempSet.add(s.en.trim());
            if (!TextUtils.isEmpty(s.vi)) tempSet.add(s.vi.trim());
        }
        suggestionList.clear();
        suggestionList.addAll(tempSet);

        Log.d("DEBUG_SUGGEST", "suggestionList size=" + suggestionList.size());

        // CHỈ update adapter nếu đã được tạo
        if (autoAdapter != null) {
            isUpdatingSuggestions = true;
            autoAdapter.clear();
            autoAdapter.addAll(suggestionList);
            autoAdapter.notifyDataSetChanged();
            isUpdatingSuggestions = false;
        }
    }




    // Dialog chọn delay cho loop audio
    private void showDelayPickerDialog() {
        final String[] items = {
                "0.2 giây",
                "0.35 giây",
                "0.5 giây",
                "0.75 giây",
                "1 giây",
                "1.5 giây",
                "2 giây",
                "3 giây",
                "4 giây",
                "5 giây"
        };

        final int[] values = {
                200,
                350,
                500,
                750,
                1000,
                1500,
                2000,
                3000,
                4000,
                5000
        };

        int checkedItem = 4;

        for (int i = 0; i < values.length; i++) {
            if (delayBetweenAudioLoopMs == values[i]) {
                checkedItem = i;
                break;
            }
        }

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle("Chọn thời gian giữa các lần lặp")
                        .setSingleChoiceItems(
                                items,
                                checkedItem,
                                null
                        )
                        .setPositiveButton(
                                "Lưu",
                                null
                        )
                        .setNegativeButton(
                                "Hủy",
                                null
                        )
                        .create();

        dialog.setOnShowListener(unused -> {
            dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener(v -> {
                int selectedPosition =
                        dialog.getListView()
                                .getCheckedItemPosition();

                if (selectedPosition < 0
                        || selectedPosition >= values.length) {

                    return;
                }

                delayBetweenAudioLoopMs =
                        values[selectedPosition];

                getSharedPreferences(
                        PREFS_AUDIO_LOOP,
                        MODE_PRIVATE
                )
                        .edit()
                        .putInt(
                                KEY_LOOP_DELAY_MS,
                                delayBetweenAudioLoopMs
                        )
                        .apply();

                Log.d(
                        "AUDIO_LOOP_DELAY",
                        "Saved loop delay="
                                + delayBetweenAudioLoopMs
                                + " ms"
                );

                Toast.makeText(
                        this,
                        "Thời gian giữa các lần lặp: "
                                + items[selectedPosition],
                        Toast.LENGTH_SHORT
                ).show();

                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void showSentenceByContent(String content) {
        if (TextUtils.isEmpty(content)) return;
        String keyword = content.trim().toLowerCase();

        // luôn search trong globalSentences
        Sentence found = null;
        for (Sentence s : globalSentences) {
            String en = s.en != null ? s.en.trim().toLowerCase() : "";
            String vi = s.vi != null ? s.vi.trim().toLowerCase() : "";
            if (en.contains(keyword) || vi.contains(keyword)) {
                found = s;
                break;
            }
        }

        if (found == null) {
            Toast.makeText(this, "Không tìm thấy câu trong dữ liệu tổng!", Toast.LENGTH_SHORT).show();
            return;
        }

        currentSentence = found;
        updateCardViewWithSentence(found);
        searchView.dismissDropDown();
    }






    // Helper to get file from internal images or cache, or null
    private File getImageFile(String imageName) {
        if (TextUtils.isEmpty(imageName)) return null;

        imageName = imageName.trim();

        // 1. Ảnh user tự chọn/lưu trong app
        File userFile = new File(getFilesDir(), "images/" + imageName);
        if (userFile.exists() && userFile.length() > 0) {
            return userFile;
        }

        // 2. Ảnh từ package/cache/assets/images
        File pkgFile = ImageUtils.getImageFile(this, imageName, topicPackageManager);
        if (pkgFile != null && pkgFile.exists() && pkgFile.length() > 0) {
            return pkgFile;
        }

        return null;
    }

    private void showEditDialog(Sentence sentence) {
        if (sentence == null) return;
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_sentence, null);
        EditText etEnglish = dialogView.findViewById(R.id.etEnglish);
        EditText etVietnamese = dialogView.findViewById(R.id.etVietnamese);
        EditText etIpa = dialogView.findViewById(R.id.etIPA);

        ImageView ivImage = dialogView.findViewById(R.id.ivImage);
        Button btnPickAudio = dialogView.findViewById(R.id.btnPickAudio);
        TextView tvAudioName = dialogView.findViewById(R.id.tvAudioName);
        currentAudioTextView = tvAudioName;
        currentEditImageView = ivImage;
        selectedImageUri = null;

        etEnglish.setText(sentence.en);
        etVietnamese.setText(sentence.vi);
        etIpa.setText(sentence.ipa != null ? sentence.ipa : "");
        tvAudioName.setText(sentence.audio != null ? sentence.audio : "Chưa chọn file");

        // Load hình ảnh: ưu tiên file, nếu không thì thử assets, nếu không thì placeholder
        File imgFile = getImageFile(sentence.image);
        if (imgFile != null && imgFile.exists()) {
            loadImageWithRoundedCorners(ivImage, imgFile);
        } else if (!TextUtils.isEmpty(sentence.image)) {
            try {
                InputStream is = getAssets().open("images/" + sentence.image);
                Bitmap bm = BitmapFactory.decodeStream(is);
                loadImageWithRoundedCorners(ivImage, bm);
                is.close();
            } catch (Exception e) {
                loadImageWithRoundedCorners(ivImage, R.drawable.no_image);
            }
        } else {
            loadImageWithRoundedCorners(ivImage, R.drawable.no_image);
        }
        btnPickAudio.setOnClickListener(v -> {
            audioPickerLauncher.launch("audio/*");
        });

        ivImage.setOnClickListener(v -> {
            currentEditImageView = ivImage;
            imagePickerLauncher.launch("image/*");
        });

        if (editDialog != null && editDialog.isShowing()) {
            editDialog.dismiss();
        }
        editDialog = new AlertDialog.Builder(this)
                .setTitle("Sửa câu")
                .setView(dialogView)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    sentence.en = etEnglish.getText().toString();
                    sentence.vi = etVietnamese.getText().toString();
                    sentence.ipa = etIpa.getText().toString();
                    if (selectedAudioUri != null && selectedAudioFileName != null) {
                        File audioDir = new File(getFilesDir(), "audio");
                        if (!audioDir.exists()) audioDir.mkdirs();
                        File destFile = new File(audioDir, System.currentTimeMillis() + "_" + selectedAudioFileName);
                        try (InputStream in = getContentResolver().openInputStream(selectedAudioUri);
                             OutputStream out = new FileOutputStream(destFile)) {
                            byte[] buf = new byte[4096];
                            int len;
                            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                            sentence.audio = destFile.getName();
                        } catch (Exception e) {
                            Toast.makeText(this, "Lỗi lưu file audio", Toast.LENGTH_SHORT).show();
                            sentence.audio = "";
                        }
                        selectedAudioUri = null;
                    }
                    if (selectedImageUri != null) {
                        try {
                            String fileName = "user_" + System.currentTimeMillis() + ".jpg";
                            File destDir = new File(getFilesDir(), "images");
                            if (!destDir.exists()) destDir.mkdirs();
                            File destFile = new File(destDir, fileName);
                            try (InputStream in = getContentResolver().openInputStream(selectedImageUri);
                                 OutputStream out = new FileOutputStream(destFile)) {
                                byte[] buf = new byte[4096];
                                int len;
                                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                            }
                            sentence.image = fileName;
                        } catch (Exception e) {
                            Toast.makeText(this, "Lỗi lưu ảnh mới", Toast.LENGTH_SHORT).show();
                        }
                        selectedImageUri = null;
                    }
                    Toast.makeText(this, "Đã lưu thay đổi!", Toast.LENGTH_SHORT).show();
                    updateCardViewWithSentence(sentence);
                    saveSentencesToFile();
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }


    private final BroadcastReceiver sentencesUpdatedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SharedPreferences lsPrefs = getSharedPreferences("lockscreen_prefs", MODE_PRIVATE);
            String topicId = lsPrefs.getString("current_topic_id", null);

            Log.d("TOPIC_RECEIVER", "RECEIVE: current_topic_id=" + topicId);
            Log.d("TOPIC_RECEIVER", "RECEIVE: currentTopicOnLockscreen.id="
                    + (currentTopicOnLockscreen == null ? "null" : currentTopicOnLockscreen.id));

            if (!TextUtils.isEmpty(topicId)) {
                TopicInfo t = TopicManager.getTopicById(LockScreenActivity.this, topicId);
                if (t != null) {
                    loadSentencesForTopicOnLockScreen(t);
                } else {
                    loadSentencesFromAssets();
                }
            } else {
                loadSentencesFromAssets();
            }

            updateStats();
            updateSrsStatsBar();

            if (currentSentence != null) {
                updateCardViewWithSentence(currentSentence);
                updateNowPlayingHeader();
            }
        }
    };










    private void loadImageWithRoundedCorners(ImageView imageView, Object imageSource) {
        float density = getResources().getDisplayMetrics().density;
        int radiusPx = (int) (IMAGE_CORNER_RADIUS_DP * density);
        Glide.with(this)
                .load(imageSource)
                .centerCrop()
                .transform(new com.bumptech.glide.load.resource.bitmap.RoundedCorners(radiusPx))
                .into(imageView);
    }

    private void loadSentencesFromAssets() {
        currentTopicOnLockscreen = null;
        try {
            File file = new File(getFilesDir(), "sentences.json");
            InputStream is;
            if (file.exists()) {
                is = new FileInputStream(file);
            } else {
                is = getAssets().open("sentences.json");
            }
            StringBuilder json = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) json.append(line);
            br.close();

            sentences = new Gson().fromJson(
                    json.toString(),
                    new TypeToken<List<Sentence>>() {}.getType()
            );

            // ✅✅✅ GÁN ID CHO TỪNG CÂU ✅✅✅
            if (sentences != null) {
                for (int i = 0; i < sentences.size(); i++) {
                    Sentence s = sentences.get(i);
                    if (s.id == -1) {  // chỉ gán nếu chưa có ID
                        s.id = i;
                    }
                }
                Log.d("LOAD_SENTENCES", "✅ Assigned IDs to " + sentences.size() + " sentences");
            }

            Log.d("DEBUG_LIST",
                    "Gán lại sentences, size mới: " + (sentences == null ? -1 : sentences.size()));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi đọc file dữ liệu", Toast.LENGTH_LONG).show();
        }

        // ⭐ TÍNH LẠI COUNTER SRS DỰA TRÊN lastRating CỦA TOÀN BỘ FILE MẶC ĐỊNH
        recalcRatingCounters();

        // ⭐ Cập nhật thống kê + gợi ý như cũ
        updateStats();
        updateSuggestionList();
        updateSrsStatsBar();
        rebuildTodayNewPool();
        if (autoAdapter != null) {
            autoAdapter.clear();
            autoAdapter.addAll(suggestionList);
            autoAdapter.notifyDataSetChanged();
        }
    }





    private void showExampleDialog() {
        if (currentSentence == null || TextUtils.isEmpty(currentSentence.example)) {
            // Không có ví dụ → im lặng
            return;
        }

        if (currentExampleDialog != null && currentExampleDialog.isShowing()) {
            currentExampleDialog.dismiss();
        }

        // Tách EN / VI
        String ex = currentSentence.example.replace("\\n", "\n");
        String enPart = ex;
        String viPart = "";

        int idx = ex.indexOf('\n');
        if (idx >= 0) {
            enPart = ex.substring(0, idx).trim();
            viPart = ex.substring(idx + 1).trim();
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_example, null);
        TextView tvEn = dialogView.findViewById(R.id.tvExampleEn);
        TextView tvVi = dialogView.findViewById(R.id.tvExampleVi);

        tvEn.setText(enPart);
        tvVi.setText(viPart);

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyExampleDialogTheme);
        builder.setView(dialogView);

        currentExampleDialog = builder.create();
        currentExampleDialog.setCanceledOnTouchOutside(true);
        currentExampleDialog.setOnDismissListener(d -> stopAudioLoop());

        currentExampleDialog.show();
    }



    private void smoothStopCurrentAudio() {
        if (currentMediaPlayer == null) return;

        try {
            if (!currentMediaPlayer.isPlaying()) {
                // Không đang play thì chỉ cần release
                try {
                    currentMediaPlayer.release();
                } catch (Exception ignored) {}
                currentMediaPlayer = null;
                return;
            }

            final MediaPlayer mp = currentMediaPlayer;
            final int steps = 5;          // 5 bước
            final long stepDelay = 40L;   // mỗi bước 40ms → tổng ~200ms
            final float[] vol = {1.0f};

            Handler h = new Handler(Looper.getMainLooper());

            Runnable fade = new Runnable() {
                int i = 0;
                @Override
                public void run() {
                    if (i < steps) {
                        vol[0] -= 1.0f / steps;
                        if (vol[0] < 0f) vol[0] = 0f;
                        try {
                            mp.setVolume(vol[0], vol[0]);
                        } catch (Exception ignored) {}
                        i++;
                        h.postDelayed(this, stepDelay);
                    } else {
                        try {
                            if (mp.isPlaying()) {
                                mp.pause();
                            }
                            mp.seekTo(0);
                        } catch (Exception ignored) {}
                        try {
                            mp.release();
                        } catch (Exception ignored) {}
                        if (mp == currentMediaPlayer) {
                            currentMediaPlayer = null;
                        }
                    }
                }
            };

            h.post(fade);
        } catch (Exception ignored) {}
    }






    private Sentence pickAnyLearnedReviewLoopSentence() {
        if (sentences == null || sentences.isEmpty()) {
            return null;
        }

        List<Sentence> list = new ArrayList<>();

        for (Sentence s : sentences) {
            if (s == null) continue;

            String phase = s.srsPhase != null ? s.srsPhase.trim().toUpperCase() : "";

            boolean hasSrsState = !phase.isEmpty()
                    || s.srsDueTime > 0
                    || s.srsReps > 0;

            if (!hasSrsState) continue;

            list.add(s);
        }

        if (list.isEmpty()) {
            return null;
        }

        // Ưu tiên AGAIN → HARD → GOOD → EASY
        list.sort((a, b) -> {
            int pa = reviewLoopPriority(a);
            int pb = reviewLoopPriority(b);

            if (pa != pb) {
                return Integer.compare(pa, pb);
            }

            // Nếu cùng phase thì ưu tiên câu due sớm hơn
            if (a.srsDueTime != b.srsDueTime) {
                return Long.compare(a.srsDueTime, b.srsDueTime);
            }

            // Cuối cùng sắp theo EN để thứ tự ổn định
            String ea = a.en != null ? a.en : "";
            String eb = b.en != null ? b.en : "";
            return ea.compareToIgnoreCase(eb);
        });

        // Nếu chưa có currentSentence thì lấy câu đầu tiên
        if (currentSentence == null) {
            Sentence picked = list.get(0);

            Log.d("REVIEW_LOOP",
                    "pick(first)=" + picked.en
                            + " phase=" + picked.srsPhase
                            + " rating=" + picked.lastRating);

            return picked;
        }

        String currentKey = makeSentenceKey(currentSentence);

        int currentPos = -1;
        for (int i = 0; i < list.size(); i++) {
            if (safeEquals(makeSentenceKey(list.get(i)), currentKey)) {
                currentPos = i;
                break;
            }
        }

        int nextPos = currentPos + 1;

        if (nextPos >= list.size() || nextPos < 0) {
            nextPos = 0;
        }

        Sentence picked = list.get(nextPos);

        Log.d("REVIEW_LOOP",
                "pick=" + picked.en
                        + " phase=" + picked.srsPhase
                        + " rating=" + picked.lastRating
                        + " pos=" + nextPos
                        + "/" + list.size());

        return picked;
    }

    private int reviewLoopPriority(Sentence s) {
        if (s == null) return 99;

        String phase = s.srsPhase != null ? s.srsPhase : "";

        if ("AGAIN".equals(phase)) return 0;
        if ("HARD".equals(phase)) return 1;
        if ("GOOD".equals(phase)) return 2;

        if (s.lastRating == 0) return 3;
        if (s.lastRating == 1) return 4;
        if (s.lastRating == 2) return 5;
        if (s.lastRating == 3) return 6;

        return 7;
    }



    private Sentence pickNextSentenceForSrs(long now,
                                            int dailyNewLimit,
                                            int dailyReviewLimit,
                                            int todayNewDone,
                                            int todayReviewDone) {
        Sentence next = null;

        // 1) LEARNING QUEUE
        if (inSessionLearningQueue != null
                && !inSessionLearningQueue.isEmpty()
                && consecutiveLearningShown < MAX_CONSECUTIVE_LEARNING) {

            for (Sentence q : inSessionLearningQueue) {
                if (q == null) continue;
                if (q == currentSentence) continue;

                if (q.srsDueTime > 0 && q.srsDueTime <= now) {
                    next = q;
                    consecutiveLearningShown++;

                    currentCardFromReviewLoop = false;

                    Log.d("SRS_DEBUG", "[PICK_FN] from LEARNING_QUEUE: " + next.en);
                    return next;
                }
            }
        }

        // 2) DUE REVIEW theo quota
        if (dailyReviewLimit > 0 && todayReviewDone < dailyReviewLimit) {
            List<Sentence> hardFirst = new ArrayList<>();
            List<Sentence> goodLater = new ArrayList<>();

            for (Sentence ss : sentences) {
                if (ss == null) continue;
                if (ss == currentSentence) continue;

                if (ss.srsReps > 0 && ss.srsDueTime > 0 && ss.srsDueTime <= now) {
                    if (ss.lastRating == 0 || ss.lastRating == 1) {
                        hardFirst.add(ss);
                    } else {
                        goodLater.add(ss);
                    }
                }
            }

            if (!hardFirst.isEmpty()) {
                hardFirst.sort((a, b) -> Long.compare(a.srsDueTime, b.srsDueTime));
                next = hardFirst.get(0);

                currentCardFromReviewLoop = false;

                Log.d("SRS_DEBUG", "[PICK_FN] from DUE hard/again: " + next.en);
                return next;
            }

            if (!goodLater.isEmpty()) {
                goodLater.sort((a, b) -> Long.compare(a.srsDueTime, b.srsDueTime));
                next = goodLater.get(0);

                currentCardFromReviewLoop = false;

                Log.d("SRS_DEBUG", "[PICK_FN] from DUE good/easy: " + next.en);
                return next;
            }
        }

        // 3) NEW CARD theo quota
        if (dailyNewLimit > 0 && todayNewDone < dailyNewLimit) {
            if (todayNewPool.isEmpty()) {
                Log.d("SRS_DEBUG", "[PICK_FN] todayNewPool empty, rebuild");
                rebuildTodayNewPool();
            }

            while (todayNewIndex < todayNewPool.size()
                    && todayNewPool.get(todayNewIndex).srsReps > 0) {
                todayNewIndex++;
            }

            if (todayNewIndex < todayNewPool.size()) {
                next = todayNewPool.get(todayNewIndex);
                todayNewIndex++;

                currentCardFromReviewLoop = false;

                Log.d("SRS_DEBUG", "[PICK_FN] from NEW: " + next.en);
                return next;
            }

            rebuildTodayNewPool();

            if (!todayNewPool.isEmpty()) {
                next = todayNewPool.get(0);
                todayNewIndex = 1;

                currentCardFromReviewLoop = false;

                Log.d("SRS_DEBUG", "[PICK_FN] from NEW after rebuild: " + next.en);
                return next;
            }
        }

        // 4) REVIEW LOOP: hết due/new thì vẫn cho ôn câu đã học
        next = pickAnyLearnedReviewLoopSentence();
        if (next != null) {
            currentCardFromReviewLoop = true;

            Log.d("SRS_DEBUG", "[PICK_FN] from REVIEW_LOOP: " + next.en);
            return next;
        }

        // 5) EARLIEST FUTURE cuối cùng
        Sentence earliestFuture = null;

        for (Sentence ss : sentences) {
            if (ss == null) continue;
            if (ss == currentSentence) continue;

            if (ss.srsDueTime > now) {
                if (earliestFuture == null || ss.srsDueTime < earliestFuture.srsDueTime) {
                    earliestFuture = ss;
                }
            }
        }

        if (earliestFuture != null) {
            currentCardFromReviewLoop = false;

            Log.d("SRS_DEBUG", "[PICK_FN] from EARLIEST_FUTURE: " + earliestFuture.en);
            return earliestFuture;
        }

        currentCardFromReviewLoop = false;
        return null;
    }


    private void showSentence(boolean isHistory) {
        isSwitchingSentence = true; // ⭐ bắt đầu chuyển câu

        resetDailyCountersIfNeeded();
        resetFloatingNoteBackground();
        stopAllVoiceLoops();

        // ✅ RESET FLOATING NOTE SỚM NHẤT MỖI KHI ĐỔI CÂU
        if (isFloatingNoteVisible && floatingNoteView != null) {
            View header = floatingNoteView.findViewById(R.id.floatingNoteHeader);
            if (header != null) header.setVisibility(View.VISIBLE);

            if (edtFloatingNote != null) {
                edtFloatingNote.setGravity(Gravity.TOP | Gravity.START);
                Editable editable = edtFloatingNote.getText();
                if (editable != null) editable.clear(); else edtFloatingNote.setText("");
                edtFloatingNote.clearFocus();

                ViewGroup.LayoutParams lp = edtFloatingNote.getLayoutParams();
                lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                edtFloatingNote.setLayoutParams(lp);
            }

            floatingNoteView.setScaleX(1f);
            floatingNoteView.setScaleY(1f);

            if (noteLastX >= 0 && noteLastY >= 0) {
                floatingNoteView.setX(noteLastX);
                floatingNoteView.setY(noteLastY);
            }
        }

        if (exampleContainer != null) {
            exampleContainer.setVisibility(View.GONE);
            if (tvExampleEnInline != null) tvExampleEnInline.setText("");
            if (tvExampleViInline != null) tvExampleViInline.setText("");
        }

        stopLoopVisualEffect();
        isAudioLooping = false;
        isLoopingReplay = false;
        stopEnglishBlowEffect();
        resetExampleLoopState();
        resetSrsButtonsState();
        hasShownAnswer = false;

        // ✅ Dừng audio mượt khi next câu
        if (currentMediaPlayer != null) {
            smoothStopCurrentAudio();
        }

        Log.d("SRS_DEBUG", "==== showSentence(isHistory=" + isHistory + ") ====");

        if (!isHistory) {
            SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
            boolean favoritesOnly = prefs.getBoolean("locks_favorites_only", false);

            Sentence next = null;

            if (favoritesOnly) {
                // ⭐ Chế độ chỉ câu yêu thích (GIỮ NGUYÊN)
                List<Sentence> favList = new ArrayList<>();
                for (Sentence s : sentences) {
                    if (s.favorite) favList.add(s);
                }
                Log.d("SRS_DEBUG", "[PICK] favoritesOnly=true, favList size=" + favList.size());

                if (favList.isEmpty()) {
                    Toast.makeText(this, "Không có câu yêu thích nào!", Toast.LENGTH_SHORT).show();
                    isSwitchingSentence = false;
                    return;
                }

                List<Sentence> favUnshown = new ArrayList<>();
                for (Sentence s : favList) {
                    if (!s.favoriteShown) favUnshown.add(s);
                }
                Log.d("SRS_DEBUG", "[PICK] favUnshown size=" + favUnshown.size());

                if (favUnshown.isEmpty()) {
                    for (Sentence s : favList) s.favoriteShown = false;
                    saveSentencesToFile();
                    favUnshown.addAll(favList);
                    Log.d("SRS_DEBUG", "[PICK] reset favoriteShown, favUnshown size=" + favUnshown.size());
                }

                int idx = (int) (Math.random() * favUnshown.size());
                next = favUnshown.get(idx);
                Log.d("SRS_DEBUG", "[PICK] from favorites: en=" + next.en);

            } else {
                // ⭐ Chế độ SRS bình thường (ANKI-style)
                if (sentences == null || sentences.isEmpty()) {
                    Toast.makeText(this, "Không có câu nào trong chủ đề!", Toast.LENGTH_SHORT).show();
                    isSwitchingSentence = false;
                    return;
                }
                if (currentTopicOnLockscreen == null || currentTopicOnLockscreen.id == null) {
                    Toast.makeText(this, "Chưa chọn chủ đề cho LockScreen!", Toast.LENGTH_SHORT).show();
                    isSwitchingSentence = false;
                    return;
                }

                long now = System.currentTimeMillis();

                final int dailyNewLimit    = currentTopicNewLimit;
                final int dailyReviewLimit = currentTopicReviewLimit;
                final int todayNewDone     = currentTopicNewDone;
                final int todayReviewDone  = currentTopicReviewDone;

                Log.d("SRS_DEBUG", "[PICK] topic quotas: todayNewDone=" + todayNewDone
                        + "/" + dailyNewLimit + ", todayReviewDone=" + todayReviewDone
                        + "/" + dailyReviewLimit);

                next = pickNextSentenceForSrs(
                        now,
                        dailyNewLimit,
                        dailyReviewLimit,
                        todayNewDone,
                        todayReviewDone
                );

                if (next == null) {
                    Log.d("SRS_DEBUG", "[PICK] pickNextSentenceForSrs returned null");
                    Toast.makeText(this, "Hôm nay đã hết câu để học!", Toast.LENGTH_SHORT).show();
                    isSwitchingSentence = false;
                    return;
                }
            }

            // ⭐ Reset đếm nếu next không còn là learning short-term
            if (next != null && next != currentSentence) {
                if (!next.inLearning || next.srsIntervalDays > 0) {
                    Log.d("SRS_DEBUG", "[PICK] next is not short-learning, reset consecutiveLearningShown");
                    consecutiveLearningShown = 0;
                }
            }

            if (next == null) {
                Log.d(
                        "SRS_DEBUG",
                        "[PICK] next=null, abort"
                );

                Toast.makeText(
                        this,
                        "Không còn câu nào để hiển thị!",
                        Toast.LENGTH_SHORT
                ).show();

                isSwitchingSentence = false;
                return;
            }

            /*
             * =========================================================
             * MAP CÂU ĐƯỢC CHỌN VỀ OBJECT CHÍNH TRONG sentences
             * =========================================================
             *
             * todayNewPool / learning queue có thể còn giữ object thuộc
             * danh sách cũ sau khi đổi topic, restore lesson hoặc filter.
             *
             * Rating phải luôn được áp dụng lên chính object hiện đang
             * nằm trong sentences để:
             * - recalcRatingCounters() thấy thay đổi;
             * - saveSentencesToFile() lưu đúng;
             * - Again/Hard/Good/Easy cập nhật ngay.
             */
            Sentence canonicalNext =
                    findSentenceByKey(
                            makeSentenceKey(next)
                    );

            if (canonicalNext != null) {
                if (canonicalNext != next) {
                    Log.w(
                            "SRS_DEBUG",
                            "[PICK] Re-mapped stale object to canonical sentence"
                                    + " | en=" + next.en
                                    + " | oldHash="
                                    + System.identityHashCode(next)
                                    + " | newHash="
                                    + System.identityHashCode(canonicalNext)
                    );
                }

                next =
                        canonicalNext;

            } else {
                Log.e(
                        "SRS_DEBUG",
                        "[PICK] Cannot map selected sentence to current list"
                                + " | en=" + next.en
                );

                Toast.makeText(
                        this,
                        "Câu được chọn không còn thuộc danh sách hiện tại.",
                        Toast.LENGTH_SHORT
                ).show();

                /*
                 * Pool có thể đã cũ, rebuild lại từ sentences hiện tại.
                 */
                rebuildTodayNewPool();

                isSwitchingSentence = false;
                return;
            }

            Log.d(
                    "SRS_DEBUG",
                    "[PICK] FINAL next: en=" + next.en
                            + " | inLearning=" + next.inLearning
                            + ", intDays=" + next.srsIntervalDays
                            + ", lastRating=" + next.lastRating
                            + ", laps=" + next.sessionLearningLaps
                            + ", hash="
                            + System.identityHashCode(next)
            );

            currentSentence =
                    next;

            for (Sentence ss : sentences) {
                if (safeEquals(ss.en, currentSentence.en)
                        && safeEquals(ss.vi, currentSentence.vi)
                        && safeEquals(ss.ipa, currentSentence.ipa)) {
                    ss.shown = true;
                    if (favoritesOnly) ss.favoriteShown = true;
                    break;
                }
            }

            shownHistoryKeys.add(makeSentenceKey(currentSentence));
            shownHistoryIdx = shownHistoryKeys.size() - 1;
            historyCache.add(currentSentence);
            saveSentencesToFile();

        } else {
            // ⭐ Load từ history cache
            if (shownHistoryIdx >= 0 && shownHistoryIdx < historyCache.size()) {
                currentSentence = historyCache.get(shownHistoryIdx);
            } else if (shownHistoryIdx >= 0 && shownHistoryIdx < shownHistoryKeys.size()) {
                String key = shownHistoryKeys.get(shownHistoryIdx);
                currentSentence = findSentenceByKey(key);
                if (currentSentence == null) {
                    Toast.makeText(this, "Câu này đã bị xóa!", Toast.LENGTH_SHORT).show();
                    isSwitchingSentence = false;
                    return;
                }
            } else {
                isSwitchingSentence = false;
                return;
            }
            Log.d("SRS_DEBUG", "[PICK] from HISTORY: en=" + currentSentence.en);
        }

        if (currentSentence == null) {
            Toast.makeText(this, "Không tìm được câu để hiển thị!", Toast.LENGTH_SHORT).show();
            isSwitchingSentence = false;
            return;
        }

        // ⭐ Typing "tốt nghiệp" ...
        if (!isHistory
                && isTypingCheckEnabledGlobally()
                && shouldForceTypingOnShow(currentSentence)) {

            if (currentSentence.lastRating == 0) {
                pendingRatingAfterTyping = 1;
            } else if (currentSentence.lastRating == 1) {
                pendingRatingAfterTyping = 2;
            } else {
                pendingRatingAfterTyping = -1;
            }

            typingCheckSentence = currentSentence;
            currentSentence.needsTypingCheck = true;

            Log.d("SRS_DEBUG", "[SRS] force typing on show for en=" + currentSentence.en
                    + " | pendingRatingAfterTyping=" + pendingRatingAfterTyping);

            isSwitchingSentence = false;
            showTypingCheckDialog(currentSentence);
            return;
        }

        // ⭐ Gạch đích nút SRS theo lastRating
        MaterialButton btnAgain = findViewById(R.id.btnAgain);
        MaterialButton btnHard  = findViewById(R.id.btnHard);
        MaterialButton btnGood  = findViewById(R.id.btnGood);
        MaterialButton btnEasy  = findViewById(R.id.btnEasy);

        MaterialButton toUnderline = null;
        if (currentSentence.lastRating == 0) {
            toUnderline = btnAgain;
        } else if (currentSentence.lastRating == 1) {
            toUnderline = btnHard;
        } else if (currentSentence.lastRating == 2) {
            toUnderline = btnGood;
        } else if (currentSentence.lastRating == 3) {
            toUnderline = btnEasy;
        }

        underlineRatingButton(toUnderline, btnAgain, btnHard, btnGood, btnEasy);

        // ⭐⭐⭐ TYPING CHECK ƯU TIÊN CHO CARD ĐANG DÍNH AGAIN/HARD ⭐⭐⭐
        if (currentSentence.needsTypingCheck) {
            if (currentSentence.typingStage == TYPING_STAGE_NONE) {
                currentSentence.typingStage = TYPING_STAGE_AGAIN_TO_HARD;
            }
            typingCheckSentence = currentSentence;
            Log.d("SRS_DEBUG", "[PICK] needsTypingCheck=true on show for en=" + currentSentence.en);
            isSwitchingSentence = false;
            showTypingCheckDialog(currentSentence);
            return;
        }

        // ⭐ Reset kích thước/scale ảnh + card
        resetScaleForNewSentence();

        // Hiện/ẩn 4 nút SRS mỗi lần show câu mới, tôn trọng Listening mode
        View srsContainer = findViewById(R.id.srsButtonsContainer);
        if (srsContainer != null) {
            srsContainer.setVisibility(
                    listeningSequentialMode ? View.GONE : View.VISIBLE
            );
        }

        Log.d("DEBUG_SHOW",
                "Hiển thị: EN=" + (currentSentence.en != null ? currentSentence.en : "")
                        + ", VI=" + (currentSentence.vi != null ? currentSentence.vi : "")
                        + ", IMAGE=" + (currentSentence.image != null ? currentSentence.image : ""));

        currentAudioEnFile = currentSentence.audio;
        currentAudioViFile = currentSentence.audio_vi;

        updateCardViewWithSentence(currentSentence);
        updateNowPlayingHeader();
        updateNowPlayingInfo(currentSentence);
        saveCurrentLockscreenPosition();

        maybeFocusFloatingNoteOnNext();

        getSharedPreferences(PREFS_APP_STATE, MODE_PRIVATE)
                .edit()
                .putInt(KEY_LAST_SHOWN_SENTENCE_ID, currentSentence.id)
                .apply();

        updateStats();
        preloadNextImage();

        isSwitchingSentence = false; // ⭐ kết thúc chuyển câu

        // ⭐ Manual: play EN/VI khi show câu mới (không còn auto-next trong Activity)
        SharedPreferences appPrefs = getSharedPreferences(PREFS_APP_SETTINGS, MODE_PRIVATE);
        boolean playEnOnNext = appPrefs.getBoolean(KEY_PLAY_EN_AUDIO_ON_NEXT, false);
        boolean playViOnNext = appPrefs.getBoolean(KEY_PLAY_VI_AUDIO, false);

        if (!isMuted && !isSwitchingSentence) {
            if (playEnOnNext
                    && !TextUtils.isEmpty(currentAudioEnFile)) {

                // Audio EN vẫn phát 100%
                playAudio(
                        currentAudioEnFile,
                        false,
                        null
                );

            } else if (playViOnNext
                    && !TextUtils.isEmpty(currentAudioViFile)) {

                // Audio VI dùng mức volume riêng đã chọn
                playAudio(
                        currentAudioViFile,
                        false,
                        null,
                        audioViVolume
                );
            }
        }
    }











    private long getTodayLocalDayIndex() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        long now = System.currentTimeMillis();
        cal.setTimeInMillis(now);
        int year = cal.get(java.util.Calendar.YEAR);
        int dayOfYear = cal.get(java.util.Calendar.DAY_OF_YEAR);
        return year * 1000L + dayOfYear;
    }

    private void resetDailyCountersIfNeeded() {
        SharedPreferences locksPrefs = getSharedPreferences("locks_settings", MODE_PRIVATE);
        long storedDay = locksPrefs.getLong("today_day", -1L);
        long todayDay  = getTodayLocalDayIndex();

        Log.d("SRS_DEBUG", "resetDailyCountersIfNeeded: storedDay=" + storedDay
                + ", todayDay=" + todayDay);

        if (storedDay == todayDay) {
            Log.d("SRS_DEBUG", "SAME DAY → keep counters");
            return;
        }

        Log.d("SRS_DEBUG", "NEW DAY → reset global & per-topic counters");

        // 1) Ghi lại today_day
        locksPrefs.edit()
                .putLong("today_day", todayDay)
                .apply();

        // 2) Reset global daily counters (nếu còn dùng)
        SharedPreferences counterPrefs = getSharedPreferences(PREFS_DAILY_COUNTERS, MODE_PRIVATE);
        counterPrefs.edit()
                .putInt(KEY_NEW_COUNT, 0)
                .putInt(KEY_REVIEW_COUNT, 0)
                .apply();

        Log.d("SRS_DEBUG", "resetDailyCountersIfNeeded: reset KEY_NEW_COUNT/KEY_REVIEW_COUNT");

        // 3) Reset per-topic daily counters cho TẤT CẢ topic
        SharedPreferences topicPrefs = getSharedPreferences("TOPIC_STATS", MODE_PRIVATE);
        Map<String, ?> all = topicPrefs.getAll();
        SharedPreferences.Editor topicEditor = topicPrefs.edit();

        for (String key : all.keySet()) {
            if (key.endsWith("newDone") || key.endsWith("reviewDone")) {
                topicEditor.putInt(key, 0);
                Log.d("SRS_DEBUG", "resetDailyCountersIfNeeded: set " + key + " = 0");
            }
        }
        topicEditor.apply();

        // 4) Đồng bộ biến in-memory cho topic hiện tại (nếu đã restore)
        currentTopicNewDone = 0;
        currentTopicReviewDone = 0;

        if (currentTopicOnLockscreen != null && currentTopicOnLockscreen.id != null) {
            Log.d("SRS_DEBUG", "resetDailyCountersIfNeeded: reset per-topic counters for ALL topics, current="
                    + currentTopicOnLockscreen.id);
        }

        // 5) Rebuild pool + cập nhật UI nếu đã có sentences
        rebuildTodayNewPool();
        updateSrsStatsBar();
    }

    /**
     * ✅ KHÔI PHỤC CÂU HỌC GẦN NHẤT
     */
    private void restoreLastShownSentence() {
        SharedPreferences statePrefs = getSharedPreferences(PREFS_APP_STATE, MODE_PRIVATE);
        int lastShownId = statePrefs.getInt(KEY_LAST_SHOWN_SENTENCE_ID, -1);

        if (lastShownId == -1) {
            Log.d("SRS_STATE", "❌ Chưa có câu nào được lưu");
            return;
        }

        // ✅ TÌM CÂU THEO ID
        for (Sentence s : sentences) {
            if (s.id == lastShownId) {
                currentSentence = s;

                // ✅ HIỂN THỊ NGAY (KHÔNG ĐỢI showSentence)
                updateCardViewWithSentence(s);

                Log.d("SRS_STATE", "✅ Restored lastShownSentence: id=" + s.id +
                        ", en=" + s.en);
                return;
            }
        }

        Log.d("SRS_STATE", "⚠️ Không tìm thấy câu id=" + lastShownId);
    }


    private void incrementDailyCountersFor(Sentence s) {
        if (s == null) return;
        boolean isNew = (s.srsReps == 0);

        Log.d("INCREMENT_DAILY", "=== incrementDailyCountersFor ===");
        Log.d("INCREMENT_DAILY", "EN: " + s.en);
        Log.d("INCREMENT_DAILY", "srsReps: " + s.srsReps);
        Log.d("INCREMENT_DAILY", "isNew: " + isNew);

        if (isNew) {
            Log.d("INCREMENT_DAILY", "→ Calling incrementNewCount()");
            incrementNewCount();
        } else {
            Log.d("INCREMENT_DAILY", "→ Calling incrementReviewCount()");
            incrementReviewCount();
        }
    }


    private void incrementNewCount() {
        Log.d("INCREMENT_COUNT", "=== incrementNewCount() CALLED ===");
        Log.d("INCREMENT_COUNT", "Current New (topic): " + currentTopicNewDone + "/" + currentTopicNewLimit);

        // 1) Tăng per-topic trong RAM
        if (currentTopicNewDone < currentTopicNewLimit) {
            currentTopicNewDone++;
            Log.d("INCREMENT_COUNT", "✅ Topic New count increased to: " + currentTopicNewDone);
        } else {
            Log.d("INCREMENT_COUNT", "❌ Topic New quota FULL: " + currentTopicNewDone + "/" + currentTopicNewLimit);
        }

        // 2) Ghi lại vào TOPIC_STATS (để nhớ theo chủ đề trong ngày)
        if (currentTopicOnLockscreen != null && currentTopicOnLockscreen.id != null) {
            SharedPreferences topicPrefs = getSharedPreferences("TOPIC_STATS", MODE_PRIVATE);
            String prefix = "topic_" + currentTopicOnLockscreen.id + "_";

            topicPrefs.edit()
                    .putInt(prefix + "newDone", currentTopicNewDone)
                    .apply();

            Log.d("INCREMENT_COUNT", "Saved newDone=" + currentTopicNewDone + " to " + prefix + "newDone");
        }

        // 3) Lưu luôn stats đầy đủ (kèm lastDay)
        saveCurrentTopicStats();

        // ❌ Không gọi updateSrsStatsBar() ở đây, vì đã gọi trong performNextSentence()
    }

    private void incrementReviewCount() {
        Log.d("INCREMENT_COUNT", "=== incrementReviewCount() CALLED ===");
        Log.d("INCREMENT_COUNT", "Current Review (topic): "
                + currentTopicReviewDone + "/" + currentTopicReviewLimit
                + " | fromReviewLoop=" + currentCardFromReviewLoop);

        // ⭐ Review loop: không tính vào quota Review per day
        if (currentCardFromReviewLoop) {
            Log.d("INCREMENT_COUNT", "⏭ Skip review count: card from REVIEW_LOOP");
            updateSrsStatsBar();
            return;
        }

        // 1) Tăng per-topic trong RAM
        if (currentTopicReviewDone < currentTopicReviewLimit) {
            currentTopicReviewDone++;
            Log.d("INCREMENT_COUNT", "✅ Topic Review count increased to: " + currentTopicReviewDone);
        } else {
            Log.d("INCREMENT_COUNT", "❌ Topic Review quota FULL: "
                    + currentTopicReviewDone + "/" + currentTopicReviewLimit);
        }

        // 2) Ghi lại vào TOPIC_STATS
        if (currentTopicOnLockscreen != null && currentTopicOnLockscreen.id != null) {
            SharedPreferences topicPrefs = getSharedPreferences("TOPIC_STATS", MODE_PRIVATE);
            String prefix = "topic_" + currentTopicOnLockscreen.id + "_";

            topicPrefs.edit()
                    .putInt(prefix + "reviewDone", currentTopicReviewDone)
                    .apply();

            Log.d("INCREMENT_COUNT", "Saved reviewDone="
                    + currentTopicReviewDone + " to " + prefix + "reviewDone");
        }

        // 3) Lưu luôn stats đầy đủ
        saveCurrentTopicStats();
    }




    // ⭐ So sánh từng ký tự và tô màu phần sai
    private void setTypingCompareText(TextView tvYour, TextView tvCorrect,
                                      String userAnswer, String correctAnswer) {
        if (userAnswer == null) userAnswer = "";
        if (correctAnswer == null) correctAnswer = "";

        // Normalize như isTypingCorrect (bỏ hoa/thường, dấu câu, space dư)
        String normUser    = normalizeForTypingCompare(userAnswer);
        String normCorrect = normalizeForTypingCompare(correctAnswer);

        // Tách token hiển thị (raw) theo space
        String[] userTokensRaw    = userAnswer.isEmpty()    ? new String[0] : userAnswer.split("\\s+");
        String[] correctTokensRaw = correctAnswer.isEmpty() ? new String[0] : correctAnswer.split("\\s+");

        // Tách token normalize để so sánh
        String[] userTokensNorm    = normUser.isEmpty()    ? new String[0] : normUser.split(" ");
        String[] correctTokensNorm = normCorrect.isEmpty() ? new String[0] : normCorrect.split(" ");

        int maxTokens = Math.max(userTokensNorm.length, correctTokensNorm.length);

        SpannableStringBuilder userSpan = new SpannableStringBuilder();
        SpannableStringBuilder correctSpan = new SpannableStringBuilder();

        int colorRed   = Color.parseColor("#D32F2F");
        int colorGreen = Color.parseColor("#388E3C");
        int colorGrey  = Color.parseColor("#9E9E9E");

        for (int i = 0; i < maxTokens; i++) {
            String uNorm = (i < userTokensNorm.length)    ? userTokensNorm[i]    : null;
            String cNorm = (i < correctTokensNorm.length) ? correctTokensNorm[i] : null;

            String uRaw = (i < userTokensRaw.length)    ? userTokensRaw[i]    : "";
            String cRaw = (i < correctTokensRaw.length) ? correctTokensRaw[i] : "";

            boolean userHasToken    = (uNorm != null);
            boolean correctHasToken = (cNorm != null);

            if (!userHasToken && correctHasToken) {
                // User thiếu 1 từ
                if (userSpan.length() > 0) userSpan.append(" ");
                int su = userSpan.length();
                userSpan.append("▢");
                userSpan.setSpan(new ForegroundColorSpan(colorGrey),
                        su, su + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                if (correctSpan.length() > 0) correctSpan.append(" ");
                int sc = correctSpan.length();
                correctSpan.append(cRaw);
                correctSpan.setSpan(new ForegroundColorSpan(colorGreen),
                        sc, sc + cRaw.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            } else if (userHasToken && !correctHasToken) {
                // User có từ dư
                if (userSpan.length() > 0) userSpan.append(" ");
                int su = userSpan.length();
                userSpan.append(uRaw);
                userSpan.setSpan(new ForegroundColorSpan(colorRed),
                        su, su + uRaw.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                if (correctSpan.length() > 0) correctSpan.append(" ");
                int sc = correctSpan.length();
                correctSpan.append("▢");
                correctSpan.setSpan(new ForegroundColorSpan(colorGrey),
                        sc, sc + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            } else if (userHasToken && correctHasToken) {
                // Cả 2 đều có từ tương ứng
                if (userSpan.length() > 0) userSpan.append(" ");
                if (correctSpan.length() > 0) correctSpan.append(" ");

                if (uNorm.equals(cNorm)) {
                    // Từ đúng hoàn toàn (theo normalize) -> giữ nguyên
                    userSpan.append(uRaw);
                    correctSpan.append(cRaw);
                } else {
                    // Từ sai -> so sánh từng ký tự bên trong
                    appendDiffWord(userSpan, correctSpan, uRaw, cRaw,
                            colorRed, colorGreen, colorGrey);
                }
            }
        }

        tvYour.setText(userSpan);
        tvCorrect.setText(correctSpan);
    }




    private void showTypingCompareDialog(String userAnswer, String correctAnswer, Runnable onOk) {
        View view = getLayoutInflater().inflate(R.layout.dialog_typing_compare, null);
        TextView tvYour = view.findViewById(R.id.tvYourAnswer);
        TextView tvCorrect = view.findViewById(R.id.tvCorrectAnswer);

        setTypingCompareText(tvYour, tvCorrect, userAnswer, correctAnswer);

        new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .setPositiveButton("OK", (d, which) -> {
                    if (onOk != null) {
                        onOk.run();
                    }
                })
                .create()
                .show();
    }



    private void showNextSentence() {
        if (currentSentence == null) return;

        if (listeningSequentialMode) {
            goNextSequentialInTopic();
            return;
        }

        lastRatingHandledTime = System.currentTimeMillis();

        stopVoiceLoop("female");
        stopVoiceLoop("male");
        isFemaleLooping = false;
        isMaleLooping = false;
        updateVoiceLoopButtonsUi();

        int dailyNewLimit = currentTopicNewLimit;
        int dailyReviewLimit = currentTopicReviewLimit;
        int todayNewDone = currentTopicNewDone;
        int todayReviewDone = currentTopicReviewDone;

        if (dailyNewLimit > 0 && dailyReviewLimit > 0
                && todayNewDone >= dailyNewLimit
                && todayReviewDone >= dailyReviewLimit) {

            String topicName = currentTopicOnLockscreen != null && currentTopicOnLockscreen.name != null
                    ? currentTopicOnLockscreen.name
                    : "chủ đề hiện tại";

            new AlertDialog.Builder(this)
                    .setTitle("Hoàn thành quota")
                    .setMessage("🎉 Đã đủ quota hôm nay cho \"" + topicName + "\"!\n"
                            + "New: " + todayNewDone + "/" + dailyNewLimit + " | "
                            + "Review: " + todayReviewDone + "/" + dailyReviewLimit)
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        Sentence s = currentSentence;

        if (isTypingCheckEnabledGlobally() && s.needsTypingCheck) {
            if (s.typingStage == TYPING_STAGE_NONE) {
                s.typingStage = TYPING_STAGE_AGAIN_TO_HARD;
            }

            if (pendingRatingAfterTyping < 0) {
                pendingRatingAfterTyping = s.lastRating >= 0 && s.lastRating <= 3 ? s.lastRating : 2;
            }

            typingCheckSentence = s;
            showTypingCheckDialog(s);
            return;
        }

        if (typingFailedAndShowingAnswer) {
            typingFailedAndShowingAnswer = false;

            MaterialButton btnAgain = findViewById(R.id.btnAgain);
            MaterialButton btnHard = findViewById(R.id.btnHard);
            MaterialButton btnGood = findViewById(R.id.btnGood);
            MaterialButton btnEasy = findViewById(R.id.btnEasy);

            if (btnAgain != null) {
                highlightSelectedButton(btnAgain, btnAgain, btnHard, btnGood, btnEasy);
            }

            goNextWithRating(0);
            return;
        }

        int swipeRating;

        if (s.swipePhase == SWIPE_PHASE_AGAIN) {
            swipeRating = handleSwipeInAgainFlow(s);
        } else if (s.swipePhase == SWIPE_PHASE_HARD) {
            swipeRating = handleSwipeInHardFlow(s);
        } else {
            swipeRating = getRatingForSwipe(s);
        }

        if (!hasShownAnswer) {
            showEnglishHideVietnamese();
            hasShownAnswer = true;
        }

        MaterialButton btnAgain = findViewById(R.id.btnAgain);
        MaterialButton btnHard = findViewById(R.id.btnHard);
        MaterialButton btnGood = findViewById(R.id.btnGood);
        MaterialButton btnEasy = findViewById(R.id.btnEasy);

        MaterialButton btnToHighlight = null;
        if (swipeRating == 0) btnToHighlight = btnAgain;
        else if (swipeRating == 1) btnToHighlight = btnHard;
        else if (swipeRating == 2) btnToHighlight = btnGood;
        else if (swipeRating == 3) btnToHighlight = btnEasy;

        if (btnToHighlight != null) {
            highlightSelectedButton(btnToHighlight, btnAgain, btnHard, btnGood, btnEasy);
        }

        goNextWithRating(swipeRating);
        nextFReservedForImage = false;
    }



    // ✳️ TẠM THỜI: Next tuần tự đơn giản, lát nữa sẽ chỉnh theo list câu của topic




    private void goNextSequentialInTopic() {
        if (sentences == null || sentences.isEmpty()) return;

        int currentIndex = -1;

        if (currentSentence != null) {
            String curKey = makeSentenceKey(currentSentence);

            for (int i = 0; i < sentences.size(); i++) {
                Sentence s = sentences.get(i);
                if (s == currentSentence || (s != null && makeSentenceKey(s).equals(curKey))) {
                    currentIndex = i;
                    break;
                }
            }
        }

        int nextIndex = currentIndex + 1;
        if (nextIndex >= sentences.size()) {
            nextIndex = 0;
        }

        listeningSequentialIndex = nextIndex;
        lessonAutoLoopIndex = nextIndex;

        currentSentence = sentences.get(nextIndex);

        currentAudioEnFile = currentSentence.audio;
        currentAudioViFile = currentSentence.audio_vi;

        hasShownAnswer = false;
        hasShownAnswerForCurrentSentence = false;
        hasPlayedExampleForCurrentSentence = false;
        isViAudioPlaying = false;

        saveCurrentLockscreenPosition();

        updateCardViewWithSentence(currentSentence);
        updateNowPlayingHeader();
        updateNowPlayingInfo(currentSentence);

        if (autoPlayOnNext && !TextUtils.isEmpty(currentAudioEnFile)) {
            playAudio(currentAudioEnFile, false, null);
        }
    }


    private void initListeningSequentialIndex() {
        if (currentSentence == null) return;
        if (sentences == null || sentences.isEmpty()) return;

        // Tìm vị trí câu hiện tại trong list sentences (theo topic đang load)
        for (int i = 0; i < sentences.size(); i++) {
            Sentence s = sentences.get(i);
            if (s == currentSentence) {
                listeningSequentialIndex = i;
                Log.d("LISTENING_SEQ", "init index = " + listeningSequentialIndex);
                return;
            }
        }

        // Nếu không tìm thấy (ví dụ currentSentence là object khác nhưng nội dung giống),
        // fallback: so theo key (en+vi+ipa), rồi về 0 nếu vẫn không tìm thấy
        String curKey = makeSentenceKey(currentSentence); // bạn đã có hàm này
        for (int i = 0; i < sentences.size(); i++) {
            Sentence s = sentences.get(i);
            if (makeSentenceKey(s).equals(curKey)) {
                listeningSequentialIndex = i;
                Log.d("LISTENING_SEQ", "init index by key = " + listeningSequentialIndex);
                return;
            }
        }

        listeningSequentialIndex = 0;
        Log.d("LISTENING_SEQ", "init index fallback to 0");
    }


    private void scheduleAgainStep(Sentence s) {
        if (s == null) return;

        long now = System.currentTimeMillis();

        if (s.phaseStep < 1) {
            s.phaseStep = 1;
        }

        if (s.phaseStep > AGAIN_STEPS_MS.length) {
            s.phaseStep = AGAIN_STEPS_MS.length;
        }

        int idx = s.phaseStep - 1;

        s.srsPhase = "AGAIN";
        s.lastRating = 0;

        s.inLearning = true;
        s.srsIntervalDays = 0;

        s.swipePhase = SWIPE_PHASE_AGAIN;
        s.swipeStep = idx;

        s.srsDueTime = now + AGAIN_STEPS_MS[idx];
    }

    private void scheduleHardStep(Sentence s) {
        if (s == null) return;

        long now = System.currentTimeMillis();

        if (s.phaseStep < 1) {
            s.phaseStep = 1;
        }

        if (s.phaseStep > HARD_STEPS_MS.length) {
            s.phaseStep = HARD_STEPS_MS.length;
        }

        int idx = s.phaseStep - 1;

        s.srsPhase = "HARD";
        s.lastRating = 1;

        s.inLearning = true;
        s.srsIntervalDays = 0;

        s.swipePhase = SWIPE_PHASE_HARD;
        s.swipeStep = idx;

        s.srsDueTime = now + HARD_STEPS_MS[idx];
    }

    private int handleSwipeInAgainFlow(Sentence s) {
        if (s == null) return 0;

        int currentStep = s.phaseStep;

        if (currentStep < 1) {
            currentStep = 1;
        }

        if (currentStep < AGAIN_STEPS_MS.length) {
            s.phaseStep = currentStep + 1;
            scheduleAgainStep(s);

            Log.d("SRS_DEBUG", "Again flow next step: phaseStep="
                    + s.phaseStep
                    + ", due=" + s.srsDueTime);

            return 0; // vẫn là Again
        }

        // Hết chuỗi Again -> chuyển sang Hard step 1
        s.phaseStep = 1;
        scheduleHardStep(s);

        Log.d("SRS_DEBUG", "Again flow finished -> Hard step 1, due="
                + s.srsDueTime);

        return 1; // Hard
    }

    private int handleSwipeInHardFlow(Sentence s) {
        if (s == null) return 1;

        int currentStep = s.phaseStep;

        if (currentStep < 1) {
            currentStep = 1;
        }

        if (currentStep < HARD_STEPS_MS.length) {
            s.phaseStep = currentStep + 1;
            scheduleHardStep(s);

            Log.d("SRS_DEBUG", "Hard flow next step: phaseStep="
                    + s.phaseStep
                    + ", due=" + s.srsDueTime);

            return 1; // vẫn là Hard
        }

        // Hết chuỗi Hard -> chuyển sang Good
        s.swipePhase = SWIPE_PHASE_NORMAL;
        s.swipeStep = 0;

        s.srsPhase = "GOOD";
        s.phaseStep = 0;

        s.inLearning = false;
        s.lastRating = 2;

        Log.d("SRS_DEBUG", "Hard flow finished -> Good");

        return 2; // Good
    }

    // ⭐ Typing tốt nghiệp: Again -> Hard, Hard -> Good
    private boolean shouldForceTypingOnShow(Sentence s) {
        if (s == null) return false;

        // Chỉ quan tâm câu đang ở Again hoặc Hard
        if (s.lastRating != 0 && s.lastRating != 1) {
            return false;
        }

        // "Độ dày" bạn tự chỉnh: ví dụ
        boolean appearedOften = (s.srsReps >= 3);        // đã lặp ít nhất 3 lần
        boolean oldEnough     = (s.srsIntervalDays >= 1); // đã qua ít nhất 1 ngày

        return appearedOften && oldEnough;
    }


    // ⭐ Hiệu ứng fade + scale cho 1 TextView (reveal)
    private void revealTextWithEffect(final TextView tv) {
        if (tv == null) return;
        tv.setVisibility(View.VISIBLE);
        tv.setAlpha(0f);
        tv.setScaleX(0.9f);
        tv.setScaleY(0.9f);
        tv.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(180)
                .start();
    }

    // ⭐ Hiệu ứng ẩn (fade out) cho 1 TextView
    private void hideTextWithEffect(final TextView tv) {
        if (tv == null) return;
        if (tv.getVisibility() != View.VISIBLE) {
            tv.setVisibility(View.GONE);
            tv.setAlpha(1f);
            tv.setScaleX(1f);
            tv.setScaleY(1f);
            return;
        }
        tv.animate()
                .alpha(0f)
                .setDuration(150)
                .withEndAction(() -> {
                    tv.setVisibility(View.GONE);
                    tv.setAlpha(1f);
                })
                .start();
    }

    // ⭐ Reveal cả EN + VI với dạng “cascade”
    private void revealBothWithCascade(TextView en, TextView vi) {
        if (en != null) {
            en.setVisibility(View.VISIBLE);
            en.setAlpha(0f);
            en.setScaleX(0.9f);
            en.setScaleY(0.9f);
            en.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(180)
                    .start();
        }
        if (vi != null) {
            vi.setVisibility(View.VISIBLE);
            vi.setAlpha(0f);
            vi.setScaleX(0.9f);
            vi.setScaleY(0.9f);
            vi.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setStartDelay(80)  // cascade: VI trễ hơn EN 80ms
                    .setDuration(180)
                    .start();
        }
    }













    /**
     * ⭐ PLAY AUDIO VỚI HIỆU ỨNG GLOW ALPHA
     */
    private void playAudioWithBounceEffect(String audioFile, final Runnable onComplete) {
        ImageView imgSentence = findViewById(R.id.imageView);

        if (imgSentence == null) {
            playAudio(audioFile, false, onComplete);
            return;
        }

        imgSentence.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // ⭐ HÀM GLOW LẶP LẠI
        final Runnable[] glowRunnable = new Runnable[1];
        glowRunnable[0] = new Runnable() {
            @Override
            public void run() {
                // ⭐ MỜ ĐI
                imgSentence.animate()
                        .alpha(0.6f)
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(300)
                        .withEndAction(() -> {
                            // ⭐ SÁNG LẠI
                            imgSentence.animate()
                                    .alpha(1.0f)
                                    .scaleX(1.0f)
                                    .scaleY(1.0f)
                                    .setDuration(300)
                                    .withEndAction(() -> {
                                        imgSentence.postDelayed(glowRunnable[0], 50);
                                    })
                                    .start();
                        })
                        .start();
            }
        };

        glowRunnable[0].run();

        playAudio(audioFile, false, new Runnable() {
            @Override
            public void run() {
                imgSentence.removeCallbacks(glowRunnable[0]);
                imgSentence.animate().cancel();

                imgSentence.animate()
                        .alpha(1.0f)
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(200)
                        .withEndAction(() -> {
                            imgSentence.setLayerType(View.LAYER_TYPE_NONE, null);
                            if (onComplete != null) {
                                onComplete.run();
                            }
                        })
                        .start();
            }
        });
    }




    private static class LockDisplayNode {
        int type; // 0 group, 1 section, 2 lesson/topic, 3 chọn lesson, 4 học toàn bộ section

        String text;
        String groupName;
        String sectionName;
        String sectionKey;

        TopicInfo topic;
        LessonInfo lesson;

        List<TopicInfo> sectionTopics;
        boolean expandable;

        LockDisplayNode(
                int type,
                String text,
                String groupName,
                String sectionName,
                String sectionKey,
                TopicInfo topic,
                LessonInfo lesson,
                List<TopicInfo> sectionTopics,
                boolean expandable
        ) {
            this.type = type;
            this.text = text;
            this.groupName = groupName;
            this.sectionName = sectionName;
            this.sectionKey = sectionKey;
            this.topic = topic;
            this.lesson = lesson;
            this.sectionTopics = sectionTopics;
            this.expandable = expandable;
        }
    }

    private String buildLockSectionKey(String group, String section) {
        return group + "||" + section;
    }


    private List<LockDisplayNode> buildLockscreenTopicRows(List<TopicInfo> topicsLocal) {
        List<LockDisplayNode> rows = new ArrayList<>();

        LinkedHashMap<String, LinkedHashMap<String, List<LockDisplayNode>>> tree = new LinkedHashMap<>();

        for (TopicInfo topic : topicsLocal) {
            String defaultGroup = topic.group != null && !topic.group.trim().isEmpty()
                    ? topic.group.trim()
                    : "Khác";

            String defaultSection = topic.section != null
                    ? topic.section.trim()
                    : "";

            String defaultSectionKey = buildLockSectionKey(defaultGroup, defaultSection);

            if (topic.lessons != null && !topic.lessons.isEmpty()) {
                for (LessonInfo lesson : topic.lessons) {
                    String group = lesson.group != null && !lesson.group.trim().isEmpty()
                            ? lesson.group.trim()
                            : defaultGroup;

                    String section = lesson.section != null
                            ? lesson.section.trim()
                            : defaultSection;

                    String sectionKey = buildLockSectionKey(group, section);

                    if (!tree.containsKey(group)) {
                        tree.put(group, new LinkedHashMap<>());
                    }

                    if (!tree.get(group).containsKey(section)) {
                        tree.get(group).put(section, new ArrayList<>());
                    }

                    tree.get(group).get(section).add(
                            new LockDisplayNode(
                                    2,
                                    "       • " + lesson.name + "  (" + lesson.count + " câu)",
                                    group,
                                    section,
                                    sectionKey,
                                    topic,
                                    lesson,
                                    null,
                                    false
                            )
                    );
                }
            } else {
                if (!tree.containsKey(defaultGroup)) {
                    tree.put(defaultGroup, new LinkedHashMap<>());
                }

                if (!tree.get(defaultGroup).containsKey(defaultSection)) {
                    tree.get(defaultGroup).put(defaultSection, new ArrayList<>());
                }

                tree.get(defaultGroup).get(defaultSection).add(
                        new LockDisplayNode(
                                2,
                                "       • " + topic.name + "  (" + topic.sentenceCount + " câu)",
                                defaultGroup,
                                defaultSection,
                                defaultSectionKey,
                                topic,
                                null,
                                null,
                                false
                        )
                );
            }
        }

        for (String group : tree.keySet()) {
            boolean groupExpanded = lockExpandedGroupKeys.contains(group);

            String groupLabel = groupExpanded
                    ? "▼ 📚 " + group
                    : "▶ 📚 " + group;

            if (lockCurrentSelectionLabel != null
                    && !lockCurrentSelectionLabel.isEmpty()
                    && lockCurrentSelectionLabel.startsWith(group)) {
                groupLabel += "  ✅";
            }

            rows.add(new LockDisplayNode(
                    0,
                    groupLabel,
                    group,
                    null,
                    null,
                    null,
                    null,
                    null,
                    true
            ));

            if (!groupExpanded) continue;

            LinkedHashMap<String, List<LockDisplayNode>> sections = tree.get(group);

            for (String section : sections.keySet()) {
                String sectionKey = buildLockSectionKey(group, section);
                List<LockDisplayNode> childNodes = sections.get(section);

                List<TopicInfo> sectionTopics = new ArrayList<>();
                boolean hasLessons = false;

                for (LockDisplayNode node : childNodes) {
                    if (node.topic != null && !sectionTopics.contains(node.topic)) {
                        sectionTopics.add(node.topic);
                    }

                    if (node.lesson != null) {
                        hasLessons = true;
                    }
                }

                boolean hasRealSection = section != null && !section.trim().isEmpty();

                if (!hasRealSection) {
                    rows.addAll(childNodes);
                    continue;
                }

                boolean expanded = lockExpandedSectionKeys.contains(sectionKey);
                String arrow = expanded ? "   ▼ " : "   ▶ ";
                String sectionLabel = arrow + "📂 " + section;

                if (hasLessons) {
                    sectionLabel += "  (" + childNodes.size() + " bài)";
                }

                if (lockCurrentSelectionLabel != null
                        && !lockCurrentSelectionLabel.isEmpty()
                        && lockCurrentSelectionLabel.contains(group + " / " + section)) {
                    sectionLabel += "  ✅";
                }

                rows.add(new LockDisplayNode(
                        1,
                        sectionLabel,
                        group,
                        section,
                        sectionKey,
                        null,
                        null,
                        sectionTopics,
                        true
                ));

                if (expanded) {
                    rows.add(new LockDisplayNode(
                            4,
                            "       ▶ Học toàn bộ Section",
                            group,
                            section,
                            sectionKey,
                            null,
                            null,
                            sectionTopics,
                            false
                    ));

                    if (hasLessons) {
                        rows.add(new LockDisplayNode(
                                3,
                                "       ☑ Chọn Lesson...",
                                group,
                                section,
                                sectionKey,
                                null,
                                null,
                                sectionTopics,
                                false
                        ));

                        rows.addAll(childNodes);
                    } else {
                        rows.addAll(childNodes);
                    }
                }
            }
        }

        return rows;
    }


    private void expandCurrentLockscreenTopicPath(List<TopicInfo> topics) {
        if (topics == null || topics.isEmpty()) return;

        lockExpandedGroupKeys.clear();
        lockExpandedSectionKeys.clear();

        String currentLessonKey = getCurrentSingleLessonKey();

        for (TopicInfo topic : topics) {
            if (topic == null) continue;

            String topicGroup = topic.group != null && !topic.group.trim().isEmpty()
                    ? topic.group.trim()
                    : "Khác";

            String topicSection = topic.section != null ? topic.section.trim() : "";

            boolean isCurrentTopic = currentTopicOnLockscreen != null
                    && currentTopicOnLockscreen.id != null
                    && topic.id != null
                    && currentTopicOnLockscreen.id.equals(topic.id);

            if (topic.lessons != null && !topic.lessons.isEmpty()) {
                for (LessonInfo lesson : topic.lessons) {
                    if (lesson == null) continue;

                    boolean isCurrentLesson = !TextUtils.isEmpty(currentLessonKey)
                            && lesson.key != null
                            && lesson.key.equals(currentLessonKey);

                    if (isCurrentLesson || isCurrentTopic) {
                        String group = lesson.group != null && !lesson.group.trim().isEmpty()
                                ? lesson.group.trim()
                                : topicGroup;

                        String section = lesson.section != null
                                ? lesson.section.trim()
                                : topicSection;

                        lockExpandedGroupKeys.add(group);

                        if (!TextUtils.isEmpty(section)) {
                            lockExpandedSectionKeys.add(buildLockSectionKey(group, section));
                        }

                        return;
                    }
                }
            }

            if (isCurrentTopic) {
                lockExpandedGroupKeys.add(topicGroup);

                if (!TextUtils.isEmpty(topicSection)) {
                    lockExpandedSectionKeys.add(buildLockSectionKey(topicGroup, topicSection));
                }

                return;
            }
        }
    }

    private int getApproxListRowHeightPx() {
        float density = getResources().getDisplayMetrics().density;
        return (int) (48 * density);
    }


    private void showLockscreenTopicTreeDialog() {
        List<TopicInfo> topics = TopicManager.getTopics(this);

        if (topics == null || topics.isEmpty()) {
            Toast.makeText(this,
                    "Chưa có chủ đề nào. Hãy mở app chính để import file JSON và tạo chủ đề trước.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // ⭐ Mỗi lần bấm Topic: chỉ expand đúng group/section đang học,
        // các group khác tự thu gọn.
        expandCurrentLockscreenTopicPath(topics);

        final List<LockDisplayNode> rows = new ArrayList<>();
        rows.addAll(buildLockscreenTopicRows(topics));

        ArrayAdapter<LockDisplayNode> adapter = new ArrayAdapter<LockDisplayNode>(
                this,
                android.R.layout.simple_list_item_1,
                rows
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = view.findViewById(android.R.id.text1);

                LockDisplayNode row = getItem(position);
                if (row == null) return view;

                tv.setText(row.text);

                if (row.type == 0) {
                    tv.setTextColor(Color.parseColor("#1976D2"));
                    tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
                    tv.setTextSize(18);

                } else if (row.type == 1) {
                    boolean isCurrentSection = lockCurrentSelectionLabel != null
                            && row.groupName != null
                            && row.sectionName != null
                            && lockCurrentSelectionLabel.contains(row.groupName + " / " + row.sectionName);

                    tv.setTextColor(isCurrentSection ? Color.parseColor("#FF5722") : Color.parseColor("#FF9800"));
                    tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
                    tv.setTextSize(16);

                } else if (row.type == 3) {
                    tv.setTextColor(Color.parseColor("#4CAF50"));
                    tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
                    tv.setTextSize(15);

                } else if (row.type == 4) {
                    tv.setTextColor(Color.parseColor("#1976D2"));
                    tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
                    tv.setTextSize(15);

                } else {
                    boolean isCurrentTopic = currentTopicOnLockscreen != null
                            && currentTopicOnLockscreen.id != null
                            && row.topic != null
                            && currentTopicOnLockscreen.id.equals(row.topic.id);

                    boolean isSelectedLesson = row.lesson != null
                            && lockCurrentLessonKeys != null
                            && lockCurrentLessonKeys.contains(row.lesson.key);

                    boolean selected = isCurrentTopic && (row.lesson == null || isSelectedLesson);

                    tv.setTextColor(selected ? Color.parseColor("#FF5722") : Color.BLACK);
                    tv.setTypeface(
                            tv.getTypeface(),
                            selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL
                    );
                    tv.setTextSize(15);
                }

                return view;
            }

            @Override
            public boolean isEnabled(int position) {
                LockDisplayNode row = getItem(position);

                return row != null
                        && (
                        (row.type == 0 && row.expandable)
                                || (row.type == 1 && row.expandable)
                                || (row.type == 2 && row.topic != null)
                                || (row.type == 3 && row.sectionTopics != null && !row.sectionTopics.isEmpty())
                                || (row.type == 4 && row.sectionTopics != null && !row.sectionTopics.isEmpty())
                );
            }
        };

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(
                        lockCurrentSelectionLabel == null || lockCurrentSelectionLabel.isEmpty()
                                ? "Chọn bài nghe"
                                : "Đang học: " + lockCurrentSelectionLabel
                )
                .setAdapter(adapter, null)
                .setNegativeButton("Đóng", null)
                .create();

        dialog.setOnShowListener(d -> {
            android.widget.ListView listView = dialog.getListView();

            // ⭐ Tự scroll tới lesson đang chọn và cố gắng đưa vào giữa dialog
            listView.post(() -> {
                int selectedPos = -1;

                for (int i = 0; i < rows.size(); i++) {
                    LockDisplayNode row = rows.get(i);
                    if (row == null) continue;

                    boolean isSelectedLesson = row.lesson != null
                            && lockCurrentLessonKeys != null
                            && lockCurrentLessonKeys.contains(row.lesson.key);

                    boolean isSelectedTopic = row.lesson == null
                            && row.topic != null
                            && currentTopicOnLockscreen != null
                            && currentTopicOnLockscreen.id != null
                            && currentTopicOnLockscreen.id.equals(row.topic.id);

                    if (isSelectedLesson || isSelectedTopic) {
                        selectedPos = i;
                        break;
                    }
                }

                if (selectedPos >= 0) {
                    int visibleCount = Math.max(
                            1,
                            listView.getHeight() / getApproxListRowHeightPx()
                    );

                    int targetTop = Math.max(0, selectedPos - visibleCount / 2);

                    listView.setSelection(targetTop);

                    Log.d("LOCK_TOPIC_DIALOG",
                            "Center selectedPos=" + selectedPos
                                    + " visibleCount=" + visibleCount
                                    + " targetTop=" + targetTop);
                }
            });

            listView.setOnItemClickListener((parent, view, position, id) -> {
                LockDisplayNode row = rows.get(position);
                if (row == null) return;

                if (row.type == 0 && row.expandable) {
                    if (lockExpandedGroupKeys.contains(row.groupName)) {
                        lockExpandedGroupKeys.remove(row.groupName);
                    } else {
                        // ⭐ Khi mở group mới, thu gọn group khác cho gọn
                        lockExpandedGroupKeys.clear();
                        lockExpandedSectionKeys.clear();
                        lockExpandedGroupKeys.add(row.groupName);
                    }

                    rows.clear();
                    rows.addAll(buildLockscreenTopicRows(topics));
                    adapter.notifyDataSetChanged();

                } else if (row.type == 1 && row.expandable) {
                    if (lockExpandedSectionKeys.contains(row.sectionKey)) {
                        lockExpandedSectionKeys.remove(row.sectionKey);
                    } else {
                        // ⭐ Chỉ mở 1 section trong group hiện tại
                        lockExpandedSectionKeys.clear();
                        lockExpandedSectionKeys.add(row.sectionKey);
                    }

                    rows.clear();
                    rows.addAll(buildLockscreenTopicRows(topics));
                    adapter.notifyDataSetChanged();

                } else if (row.type == 4) {
                    applyLockscreenWholeSectionFilter(row, dialog);

                } else if (row.type == 3) {
                    showLockscreenMultiLessonDialog(row, dialog);

                } else if (row.type == 2 && row.topic != null) {
                    selectLockscreenNode(row, dialog);
                }
            });
        });

        dialog.show();
    }

    private List<Sentence> loadSentencesForTopicPreviewOnLockScreen(TopicInfo topic) {
        List<Sentence> result = new ArrayList<>();
        if (topic == null || topic.fileName == null) return result;

        int expectedMin = 0;
        if (topic.lessons != null && !topic.lessons.isEmpty()) {
            for (LessonInfo l : topic.lessons) {
                if (l != null && l.count > 0) {
                    expectedMin += l.count;
                }
            }
        } else {
            expectedMin = topic.sentenceCount;
        }

        // 1) Thử đọc internal trước
        List<Sentence> internalList = loadTopicListFromInternalOrEmpty(topic.fileName);

        if (internalList != null && !internalList.isEmpty()) {
            // Nếu topic có nhiều lesson mà internal chỉ còn ít câu,
            // khả năng cao file đã bị lưu đè bởi lesson filter -> bỏ qua internal
            if (expectedMin <= 0 || internalList.size() >= expectedMin) {
                Log.d("LOCK_TOPIC_LOAD",
                        "Use INTERNAL topic=" + topic.fileName
                                + ", size=" + internalList.size()
                                + ", expectedMin=" + expectedMin);
                return internalList;
            } else {
                Log.w("LOCK_TOPIC_LOAD",
                        "Internal topic looks truncated/corrupted: "
                                + topic.fileName
                                + ", size=" + internalList.size()
                                + ", expectedMin=" + expectedMin
                                + " -> fallback ASSETS");
            }
        }

        // 2) Fallback đọc asset gốc
        List<Sentence> assetList = loadTopicListFromAssetsOrEmpty(topic.fileName);

        if (assetList != null && !assetList.isEmpty()) {
            Log.d("LOCK_TOPIC_LOAD",
                    "Use ASSET topic=" + topic.fileName
                            + ", size=" + assetList.size()
                            + ", expectedMin=" + expectedMin);
            return assetList;
        }

        if (internalList != null) {
            return internalList;
        }

        return result;
    }


    private List<Sentence> loadTopicListFromInternalOrEmpty(String fileName) {
        List<Sentence> result = new ArrayList<>();
        if (TextUtils.isEmpty(fileName)) return result;

        try {
            String name = fileName.trim();
            String baseName = name;

            int slash = Math.max(baseName.lastIndexOf('/'), baseName.lastIndexOf('\\'));
            if (slash >= 0) {
                baseName = baseName.substring(slash + 1);
            }

            File f = new File(getFilesDir(), name);
            if (!f.exists()) {
                f = new File(getFilesDir(), baseName);
            }

            if (!f.exists() || f.length() <= 0) {
                return result;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
            StringBuilder json = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                json.append(line);
            }

            br.close();

            List<Sentence> list = new Gson().fromJson(
                    json.toString(),
                    new TypeToken<List<Sentence>>() {}.getType()
            );

            if (list != null) result.addAll(list);

        } catch (Exception e) {
            Log.e("LOCK_TOPIC_LOAD", "Internal load error: " + fileName, e);
        }

        return result;
    }

    private List<Sentence> loadTopicListFromAssetsOrEmpty(String fileName) {
        List<Sentence> result = new ArrayList<>();
        if (TextUtils.isEmpty(fileName)) return result;

        try {
            String name = fileName.trim();
            String baseName = name;

            int slash = Math.max(baseName.lastIndexOf('/'), baseName.lastIndexOf('\\'));
            if (slash >= 0) {
                baseName = baseName.substring(slash + 1);
            }

            InputStream is = null;

            try {
                is = getAssets().open(name);
            } catch (Exception ignored) {}

            if (is == null) {
                try {
                    is = getAssets().open("topics/" + baseName);
                } catch (Exception ignored) {}
            }

            if (is == null) {
                try {
                    is = getAssets().open(baseName);
                } catch (Exception ignored) {}
            }

            if (is == null) {
                return result;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder json = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                json.append(line);
            }

            br.close();

            List<Sentence> list = new Gson().fromJson(
                    json.toString(),
                    new TypeToken<List<Sentence>>() {}.getType()
            );

            if (list != null) result.addAll(list);

        } catch (Exception e) {
            Log.e("LOCK_TOPIC_LOAD", "Asset load error: " + fileName, e);
        }

        return result;
    }

    private void applyLockscreenWholeSectionFilter(LockDisplayNode sectionNode, AlertDialog parentDialog) {
        if (sectionNode == null || sectionNode.sectionTopics == null || sectionNode.sectionTopics.isEmpty()) {
            return;
        }

        List<LessonInfo> allLessons = new ArrayList<>();
        LinkedHashMap<String, TopicInfo> lessonTopicMap = new LinkedHashMap<>();

        String nodeGroup = sectionNode.groupName != null ? sectionNode.groupName.trim() : "";
        String nodeSection = sectionNode.sectionName != null ? sectionNode.sectionName.trim() : "";

        for (TopicInfo topic : sectionNode.sectionTopics) {
            if (topic == null) continue;

            String topicGroup = topic.group != null ? topic.group.trim() : "";
            String topicSection = topic.section != null ? topic.section.trim() : "";

            if (topic.lessons != null && !topic.lessons.isEmpty()) {
                for (LessonInfo lesson : topic.lessons) {
                    if (lesson == null) continue;

                    String lessonGroup = lesson.group != null ? lesson.group.trim() : "";
                    String lessonSection = lesson.section != null ? lesson.section.trim() : "";

                    if (lessonGroup.isEmpty()) lessonGroup = topicGroup;
                    if (lessonSection.isEmpty()) lessonSection = topicSection;

                    if (!lessonGroup.equals(nodeGroup) || !lessonSection.equals(nodeSection)) {
                        continue;
                    }

                    allLessons.add(lesson);
                    lessonTopicMap.put(lesson.key, topic);
                }
            } else {
                if (!topicGroup.equals(nodeGroup) || !topicSection.equals(nodeSection)) {
                    continue;
                }

                LessonInfo fakeLesson = new LessonInfo(
                        "topic_" + topic.id,
                        topic.name,
                        0,
                        topic.sentenceCount
                );

                allLessons.add(fakeLesson);
                lessonTopicMap.put(fakeLesson.key, topic);
            }
        }

        if (allLessons.isEmpty()) {
            Toast.makeText(this, "Section này chưa có bài!", Toast.LENGTH_SHORT).show();
            return;
        }

        applyLockscreenMultiLessonFilter(
                sectionNode.groupName,
                sectionNode.sectionName,
                allLessons,
                lessonTopicMap
        );

        if (parentDialog != null) parentDialog.dismiss();
    }

    private void selectLockscreenNode(
            LockDisplayNode node,
            AlertDialog dialog
    ) {
        if (node == null || node.topic == null) {
            return;
        }

        stopAllLoopsAndTimers();

        TopicInfo topic =
                node.topic;

        String selectedGroup =
                node.groupName != null
                        ? node.groupName.trim()
                        : "";

        String selectedSection =
                node.sectionName != null
                        ? node.sectionName.trim()
                        : "";

        lockCurrentGroupName =
                selectedGroup;

        lockCurrentSectionName =
                selectedSection;

        currentTopicOnLockscreen =
                topic;

        // =========================================================
        // 1. ĐÓNG PACKAGE CŨ VÀ TẠO PACKAGE MỚI
        // =========================================================
        if (topicPackageManager != null) {
            try {
                topicPackageManager.close();
            } catch (Exception ignored) {
            }

            topicPackageManager =
                    null;
        }

        try {
            String packageTopicName =
                    getTopicPackageNameFromJsonFile(
                            topic.fileName
                    );

            topicPackageManager =
                    new TopicPackageManager(
                            this,
                            packageTopicName
                    );

            Log.d(
                    "LOCK_SINGLE_LESSON",
                    "Package manager set: "
                            + packageTopicName
            );

        } catch (Exception e) {
            Log.e(
                    "LOCK_SINGLE_LESSON",
                    "Cannot create package manager",
                    e
            );
        }

        // =========================================================
        // 2. LƯU TOPIC GỐC HIỆN TẠI
        // =========================================================
        SharedPreferences lockscreenPrefs =
                getSharedPreferences(
                        "lockscreen_prefs",
                        MODE_PRIVATE
                );

        lockscreenPrefs.edit()
                .putString(
                        "current_topic_id",
                        topic.id != null
                                ? topic.id
                                : ""
                )
                .putString(
                        "current_topic_file",
                        topic.fileName != null
                                ? topic.fileName
                                : ""
                )
                .apply();

        getSharedPreferences(
                "locks_settings",
                MODE_PRIVATE
        )
                .edit()
                .putString(
                        "current_topic_id",
                        topic.id
                )
                .apply();

        // =========================================================
        // 3. LOAD TOÀN BỘ CÂU CỦA TOPIC
        // =========================================================
        List<Sentence> loaded =
                loadSentencesForTopicPreviewOnLockScreen(
                        topic
                );

        Log.d(
                "LOCK_SINGLE_LESSON",
                "CLICK lesson="
                        + (
                        node.lesson != null
                                ? node.lesson.name
                                : "null"
                )
                        + " | lessonKey="
                        + (
                        node.lesson != null
                                ? node.lesson.key
                                : "null"
                )
                        + " | topicId="
                        + topic.id
                        + " | topicName="
                        + topic.name
                        + " | topicFile="
                        + topic.fileName
                        + " | loadedSize="
                        + (
                        loaded == null
                                ? -1
                                : loaded.size()
                )
        );

        if (loaded == null || loaded.isEmpty()) {
            Toast.makeText(
                    this,
                    "Topic này chưa có câu nào!",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (sentences == null) {
            sentences =
                    new ArrayList<>();
        }

        lockCurrentLessonKeys.clear();

        String selectedLessonKey =
                "";

        String selectedLessonName =
                "";

        String selectionMode;

        // =========================================================
        // 4. CHỌN MỘT LESSON CỤ THỂ
        // =========================================================
        if (node.lesson != null) {
            List<Sentence> filtered =
                    filterSentencesByLesson(
                            loaded,
                            node.lesson
                    );

            if (filtered == null || filtered.isEmpty()) {
                Toast.makeText(
                        this,
                        "Không tìm thấy câu cho lesson: "
                                + node.lesson.name,
                        Toast.LENGTH_LONG
                ).show();

                Log.e(
                        "LOCK_SINGLE_LESSON",
                        "Empty lesson."
                                + " lessonKey="
                                + node.lesson.key
                                + ", lessonName="
                                + node.lesson.name
                                + ", topic="
                                + topic.name
                                + ", topicFile="
                                + topic.fileName
                                + ", loadedSize="
                                + loaded.size()
                );

                return;
            }

            sentences.clear();
            sentences.addAll(
                    filtered
            );

            if (node.lesson.key != null
                    && !node.lesson.key.trim().isEmpty()) {

                selectedLessonKey =
                        node.lesson.key.trim();

                lockCurrentLessonKeys.add(
                        selectedLessonKey
                );
            }

            selectedLessonName =
                    node.lesson.name != null
                            ? node.lesson.name.trim()
                            : "";

            selectionMode =
                    "lesson";

            Log.d(
                    "LOCK_SINGLE_LESSON",
                    "Use FILTERED lesson: "
                            + selectedLessonName
                            + ", filteredSize="
                            + filtered.size()
                            + ", loadedSize="
                            + loaded.size()
                            + ", topicFile="
                            + topic.fileName
            );

            if (selectedSection.isEmpty()) {
                lockCurrentSelectionLabel =
                        selectedGroup
                                + " / "
                                + selectedLessonName;

            } else {
                lockCurrentSelectionLabel =
                        selectedGroup
                                + " / "
                                + selectedSection
                                + " / "
                                + selectedLessonName;
            }

        } else {
            // =====================================================
            // 5. CHỌN TOPIC / SECTION KHÔNG CÓ LESSON
            // =====================================================
            sentences.clear();
            sentences.addAll(
                    loaded
            );

            selectionMode =
                    "topic";

            if (!selectedSection.isEmpty()) {
                lockCurrentSelectionLabel =
                        selectedGroup
                                + " / "
                                + selectedSection;

            } else {
                lockCurrentSelectionLabel =
                        selectedGroup
                                + " / "
                                + topic.name;
            }

            Log.d(
                    "LOCK_SINGLE_LESSON",
                    "Use FULL topic: "
                            + topic.name
                            + ", group="
                            + selectedGroup
                            + ", section="
                            + selectedSection
                            + ", size="
                            + loaded.size()
                            + ", topicFile="
                            + topic.fileName
            );
        }

        // =========================================================
        // 6. GÁN ID NẾU CÂU CHƯA CÓ
        // =========================================================
        for (int i = 0; i < sentences.size(); i++) {
            Sentence sentence =
                    sentences.get(i);

            if (sentence != null
                    && sentence.id == -1) {

                sentence.id =
                        i;
            }
        }

        // =========================================================
        // 7. LƯU STATE CỦA LOCKSCREEN
        // =========================================================
        saveLockscreenSelectionState(
                lockCurrentSelectionLabel,
                lockCurrentLessonKeys,
                selectedGroup,
                selectedSection
        );

        // =========================================================
        // 8. LƯU ĐẦY ĐỦ LỰA CHỌN DÙNG CHUNG CHO MAINACTIVITY
        // =========================================================
        SharedPreferences.Editor sharedEditor =
                lockscreenPrefs.edit();

        sharedEditor
                .putString(
                        "current_topic_id",
                        topic.id != null
                                ? topic.id
                                : ""
                )
                .putString(
                        "current_topic_file",
                        topic.fileName != null
                                ? topic.fileName
                                : ""
                )
                .putString(
                        "selected_group",
                        selectedGroup
                )
                .putString(
                        "selected_section",
                        selectedSection
                )
                .putString(
                        "selected_lesson_key",
                        selectedLessonKey
                )
                .putString(
                        "selected_lesson_name",
                        selectedLessonName
                )
                .putStringSet(
                        "selected_lesson_keys",
                        new java.util.HashSet<>(
                                lockCurrentLessonKeys
                        )
                )
                .putString(
                        "selection_label",
                        lockCurrentSelectionLabel != null
                                ? lockCurrentSelectionLabel
                                : ""
                )
                .putBoolean(
                        "is_virtual_selection",
                        false
                )
                .putString(
                        "selection_mode",
                        selectionMode
                )
                .apply();

        Log.d(
                "SharedSelection",
                "LockScreen saved shared selection:"
                        + " topicId="
                        + topic.id
                        + ", topicFile="
                        + topic.fileName
                        + ", group="
                        + selectedGroup
                        + ", section="
                        + selectedSection
                        + ", lessonKey="
                        + selectedLessonKey
                        + ", lessonName="
                        + selectedLessonName
                        + ", lessonKeys="
                        + lockCurrentLessonKeys
                        + ", mode="
                        + selectionMode
                        + ", label="
                        + lockCurrentSelectionLabel
        );

        // =========================================================
        // 9. KHÔI PHỤC THỐNG KÊ VÀ RESET TRẠNG THÁI CÂU
        // =========================================================
        restoreTopicStats(
                topic.id
        );

        currentSentence =
                null;

        hasShownAnswer =
                false;

        hasShownAnswerForCurrentSentence =
                false;

        hasPlayedExampleForCurrentSentence =
                false;

        currentAudioEnFile =
                null;

        currentAudioViFile =
                null;

        shownHistoryKeys.clear();

        shownHistoryIdx =
                -1;

        historyCache.clear();

        if (inSessionLearningQueue != null) {
            inSessionLearningQueue.clear();
        }

        consecutiveLearningShown =
                0;

        rebuildTodayNewPool();
        recalcRatingCounters();

        updateStats();
        updateSuggestionList();
        updateSrsStatsBar();

        lessonAutoLoopIndex =
                0;

        listeningSequentialIndex =
                0;

        // =========================================================
        // 10. NHẬN DIỆN MODE CỦA TOPIC / LESSON VỪA CHỌN
        // =========================================================
        StudyMode detectedMode =
                detectStudyModeFromSentences(
                        sentences
                );

        detectAndApplyStudyModeForCurrentSelection(
                "selectLockscreenNode"
        );

        // =========================================================
        // 11. CHỌN CÂU HIỂN THỊ THEO MODE
        // =========================================================
        if (detectedMode == StudyMode.LISTENING) {
            /*
             * Luyện nghe:
             * luôn bắt đầu từ track đầu tiên.
             */
            showFirstTrackOfCurrentLesson(
                    true
            );

            Log.d(
                    "LOCK_POSITION",
                    "selectLockscreenNode"
                            + " | LISTENING"
                            + " -> show first track"
            );

        } else {
            /*
             * Luyện từ vựng:
             * khôi phục câu đang học gần nhất của topic.
             */
            restoreCurrentLockscreenPosition();

            if (currentSentence == null
                    && sentences != null
                    && !sentences.isEmpty()) {

                currentSentence =
                        sentences.get(0);

                listeningSequentialIndex =
                        0;

                lessonAutoLoopIndex =
                        0;
            }

            if (currentSentence != null) {
                currentAudioEnFile =
                        currentSentence.audio;

                currentAudioViFile =
                        currentSentence.audio_vi;

                updateCardViewWithSentence(
                        currentSentence
                );

                updateNowPlayingHeader();

                updateNowPlayingInfo(
                        currentSentence
                );

                syncSequentialIndexWithCurrentSentence();

                /*
                 * Không gọi saveCurrentLockscreenPosition() ở đây trước
                 * khi restore hoàn tất. Sau restore thì lưu lại là an toàn.
                 */
                saveCurrentLockscreenPosition();
            }

            Log.d(
                    "LOCK_POSITION",
                    "selectLockscreenNode"
                            + " | VOCABULARY"
                            + " -> restore saved position"
                            + " | en="
                            + (
                            currentSentence != null
                                    ? currentSentence.en
                                    : "null"
                    )
            );
        }

        // =========================================================
        // 12. ĐÁNH DẤU TOPIC ĐÃ LOAD
        // =========================================================
        lastLoadedLockTopicId =
                topic.id;

        lastLoadedLockTopicFile =
                topic.fileName;

        // =========================================================
        // 13. BÁO CHO MAINACTIVITY TOPIC/SELECTION ĐÃ ĐỔI
        // =========================================================
        Intent changedIntent =
                new Intent(
                        "com.OPD2nd.popup.TOPIC_CHANGED"
                );

        changedIntent.putExtra(
                "topic_id",
                topic.id
        );

        changedIntent.putExtra(
                "topic_file",
                topic.fileName
        );

        changedIntent.putExtra(
                "selected_group",
                selectedGroup
        );

        changedIntent.putExtra(
                "selected_section",
                selectedSection
        );

        changedIntent.putExtra(
                "selected_lesson_key",
                selectedLessonKey
        );

        changedIntent.putExtra(
                "selection_mode",
                selectionMode
        );

        sendBroadcast(
                changedIntent
        );

        if (dialog != null) {
            dialog.dismiss();
        }
    }




    private void applyLockscreenLessonFilter(LessonInfo lesson) {
        if (lesson == null || sentences == null || sentences.isEmpty()) return;

        List<Sentence> filtered = new ArrayList<>();

        for (int i = 0; i < sentences.size(); i++) {
            Sentence s = sentences.get(i);
            LessonInfo parsed = TopicTreeManager.parseLessonInfoFromSentence(s, i);

            if (parsed != null && lesson.key != null && lesson.key.equals(parsed.key)) {
                filtered.add(s);
            }
        }

        if (filtered.isEmpty()) {
            Toast.makeText(this,
                    "Không tìm thấy câu cho lesson: " + lesson.name,
                    Toast.LENGTH_LONG).show();
            Log.e("LESSON_FILTER", "Empty lesson filter: key=" + lesson.key + ", name=" + lesson.name);
            return;
        }

        sentences.clear();
        sentences.addAll(filtered);

        for (int i = 0; i < sentences.size(); i++) {
            Sentence s = sentences.get(i);
            if (s != null && s.id == -1) {
                s.id = i;
            }
        }

        updateStats();
        updateSuggestionList();
        updateSrsStatsBar();
    }



    private List<Sentence> filterSentencesByLesson(List<Sentence> source, LessonInfo targetLesson) {
        List<Sentence> result = new ArrayList<>();

        if (source == null || source.isEmpty() || targetLesson == null) {
            return result;
        }

        String targetKey = targetLesson.key != null ? targetLesson.key.trim() : "";

        Log.d("LOCK_LESSON_FILTER", "TARGET key=" + targetKey
                + " | name=" + targetLesson.name);

        for (int i = 0; i < source.size(); i++) {
            Sentence s = source.get(i);
            if (s == null) continue;

            LessonInfo parsed = TopicTreeManager.parseLessonInfoFromSentence(s, i);
            if (parsed == null) continue;

            String parsedKey = parsed.key != null ? parsed.key.trim() : "";

            // ✅ CHỈ MATCH KEY CHÍNH XÁC
            if (!targetKey.isEmpty() && targetKey.equals(parsedKey)) {
                result.add(s);
            }
        }

        Log.d("LOCK_LESSON_FILTER",
                "RESULT exactKey=" + targetKey
                        + " -> " + result.size()
                        + "/" + source.size());

        return result;
    }



    private String normalizeLessonCompareText(String text) {
        if (text == null) return "";

        String s = text.trim().toLowerCase(Locale.US);

        // bỏ đuôi file
        s = s.replace(".mp3", "")
                .replace(".wav", "")
                .replace(".m4a", "")
                .replace(".json", "");

        // bỏ segment số
        s = s.replaceAll("_segment_\\d+", "");
        s = s.replaceAll("segment_\\d+", "");

        // chuẩn hóa ký tự phân cách
        s = s.replace("_", " ");
        s = s.replace("-", " ");

        // bỏ ký tự lạ, giữ chữ/số
        s = s.replaceAll("[^a-z0-9]+", " ");

        // gom space
        s = s.replaceAll("\\s+", " ").trim();

        return s;
    }


    private void showLockscreenMultiLessonDialog(
            LockDisplayNode sectionNode,
            AlertDialog parentDialog
    ) {
        if (sectionNode == null
                || sectionNode.sectionTopics == null
                || sectionNode.sectionTopics.isEmpty()) {
            return;
        }

        List<LessonInfo> allLessons =
                new ArrayList<>();

        LinkedHashMap<String, TopicInfo> lessonTopicMap =
                new LinkedHashMap<>();

        String nodeGroup =
                sectionNode.groupName != null
                        ? sectionNode.groupName.trim()
                        : "";

        String nodeSection =
                sectionNode.sectionName != null
                        ? sectionNode.sectionName.trim()
                        : "";

        // =========================================================
        // 1. THU THẬP LESSON THUỘC ĐÚNG GROUP / SECTION
        // =========================================================
        for (TopicInfo topic : sectionNode.sectionTopics) {
            if (topic == null) {
                continue;
            }

            String topicGroup =
                    topic.group != null
                            ? topic.group.trim()
                            : "";

            String topicSection =
                    topic.section != null
                            ? topic.section.trim()
                            : "";

            if (topic.lessons != null
                    && !topic.lessons.isEmpty()) {

                for (LessonInfo lesson : topic.lessons) {
                    if (lesson == null) {
                        continue;
                    }

                    String lessonGroup =
                            lesson.group != null
                                    ? lesson.group.trim()
                                    : "";

                    String lessonSection =
                            lesson.section != null
                                    ? lesson.section.trim()
                                    : "";

                    // Fallback nếu lesson thiếu group/section
                    if (lessonGroup.isEmpty()) {
                        lessonGroup = topicGroup;
                    }

                    if (lessonSection.isEmpty()) {
                        lessonSection = topicSection;
                    }

                    // Chỉ lấy lesson đúng section đang bấm
                    if (!lessonGroup.equals(nodeGroup)
                            || !lessonSection.equals(nodeSection)) {
                        continue;
                    }

                    if (lesson.key == null
                            || lesson.key.trim().isEmpty()) {
                        continue;
                    }

                    allLessons.add(lesson);

                    lessonTopicMap.put(
                            lesson.key,
                            topic
                    );
                }

            } else {
                /*
                 * Topic không có lesson con:
                 * tạo fake lesson để có thể chọn chung trong Section.
                 */
                if (!topicGroup.equals(nodeGroup)
                        || !topicSection.equals(nodeSection)) {
                    continue;
                }

                LessonInfo fakeLesson =
                        new LessonInfo(
                                "topic_" + topic.id,
                                topic.name,
                                0,
                                topic.sentenceCount
                        );

                /*
                 * Gán group/section nếu LessonInfo cho phép truy cập field.
                 */
                fakeLesson.group = nodeGroup;
                fakeLesson.section = nodeSection;

                allLessons.add(fakeLesson);

                lessonTopicMap.put(
                        fakeLesson.key,
                        topic
                );
            }
        }

        if (allLessons.isEmpty()) {
            Toast.makeText(
                    this,
                    "Section này chưa có bài!",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // =========================================================
        // 2. KHÔI PHỤC LESSON ĐANG ĐƯỢC CHỌN
        // =========================================================
        boolean[] checked =
                new boolean[allLessons.size()];

        boolean hasChecked = false;

        for (int i = 0; i < allLessons.size(); i++) {
            LessonInfo lesson =
                    allLessons.get(i);

            checked[i] =
                    lockCurrentLessonKeys != null
                            && lesson.key != null
                            && lockCurrentLessonKeys.contains(
                            lesson.key
                    );

            if (checked[i]) {
                hasChecked = true;
            }
        }

        /*
         * Nếu trước đó chưa chọn lesson nào trong section,
         * mặc định chọn toàn bộ.
         */
        if (!hasChecked) {
            for (int i = 0; i < checked.length; i++) {
                checked[i] = true;
            }
        }

        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        int pad =
                (int) (12 * density);

        // =========================================================
        // 3. DỰNG GIAO DIỆN DIALOG
        // =========================================================
        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setPadding(
                pad,
                pad,
                pad,
                pad
        );

        TextView title =
                new TextView(this);

        title.setText(
                nodeGroup
                        + " / "
                        + nodeSection
        );

        title.setTextSize(18);

        title.setTypeface(
                title.getTypeface(),
                android.graphics.Typeface.BOLD
        );

        title.setPadding(
                0,
                0,
                0,
                pad
        );

        root.addView(title);

        TextView countText =
                new TextView(this);

        countText.setTextSize(15);

        countText.setPadding(
                0,
                0,
                0,
                pad
        );

        root.addView(countText);

        LinearLayout topButtons =
                new LinearLayout(this);

        topButtons.setOrientation(
                LinearLayout.HORIZONTAL
        );

        Button btnSelectAll =
                new Button(this);

        btnSelectAll.setText(
                "Chọn tất cả"
        );

        Button btnClearAll =
                new Button(this);

        btnClearAll.setText(
                "Bỏ chọn tất cả"
        );

        topButtons.addView(
                btnSelectAll,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        topButtons.addView(
                btnClearAll,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        root.addView(topButtons);

        String[] names =
                new String[allLessons.size()];

        for (int i = 0; i < allLessons.size(); i++) {
            LessonInfo lesson =
                    allLessons.get(i);

            names[i] =
                    lesson.name
                            + "  ("
                            + lesson.count
                            + " câu)";
        }

        ListView listView =
                new ListView(this);

        listView.setChoiceMode(
                ListView.CHOICE_MODE_MULTIPLE
        );

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_list_item_multiple_choice,
                        names
                );

        listView.setAdapter(adapter);

        int listHeight =
                (int) (360 * density);

        root.addView(
                listView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        listHeight
                )
        );

        LinearLayout bottomButtons =
                new LinearLayout(this);

        bottomButtons.setOrientation(
                LinearLayout.HORIZONTAL
        );

        bottomButtons.setPadding(
                0,
                pad,
                0,
                0
        );

        Button btnCancel =
                new Button(this);

        btnCancel.setText("Hủy");

        Button btnStudy =
                new Button(this);

        btnStudy.setText(
                "Học đã chọn"
        );

        bottomButtons.addView(
                btnCancel,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        bottomButtons.addView(
                btnStudy,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        root.addView(bottomButtons);

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setView(root)
                        .create();

        // =========================================================
        // 4. CẬP NHẬT SỐ LESSON ĐÃ CHỌN
        // =========================================================
        Runnable updateCount = () -> {
            int selected = 0;

            for (boolean value : checked) {
                if (value) {
                    selected++;
                }
            }

            countText.setText(
                    "Đã chọn: "
                            + selected
                            + " / "
                            + checked.length
                            + " bài"
            );
        };

        dialog.setOnShowListener(d -> {
            for (int i = 0; i < checked.length; i++) {
                listView.setItemChecked(
                        i,
                        checked[i]
                );
            }

            updateCount.run();
        });

        listView.setOnItemClickListener(
                (parent, view, position, id) -> {
                    checked[position] =
                            listView.isItemChecked(
                                    position
                            );

                    updateCount.run();
                }
        );

        btnSelectAll.setOnClickListener(v -> {
            for (int i = 0; i < checked.length; i++) {
                checked[i] = true;

                listView.setItemChecked(
                        i,
                        true
                );
            }

            updateCount.run();
        });

        btnClearAll.setOnClickListener(v -> {
            for (int i = 0; i < checked.length; i++) {
                checked[i] = false;

                listView.setItemChecked(
                        i,
                        false
                );
            }

            updateCount.run();
        });

        btnCancel.setOnClickListener(
                v -> dialog.dismiss()
        );

        // =========================================================
        // 5. HỌC CÁC LESSON ĐÃ CHỌN
        // =========================================================
        btnStudy.setOnClickListener(v -> {
            List<LessonInfo> selectedLessons =
                    new ArrayList<>();

            java.util.HashSet<String> selectedLessonKeys =
                    new java.util.HashSet<>();

            for (int i = 0; i < allLessons.size(); i++) {
                if (!checked[i]) {
                    continue;
                }

                LessonInfo selectedLesson =
                        allLessons.get(i);

                selectedLessons.add(
                        selectedLesson
                );

                if (selectedLesson != null
                        && selectedLesson.key != null
                        && !selectedLesson.key.trim().isEmpty()) {

                    selectedLessonKeys.add(
                            selectedLesson.key.trim()
                    );
                }
            }

            if (selectedLessons.isEmpty()) {
                Toast.makeText(
                        this,
                        "Bạn chưa chọn bài nào!",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            /*
             * Áp dụng bộ lọc và load dữ liệu trước.
             */
            applyLockscreenMultiLessonFilter(
                    nodeGroup,
                    nodeSection,
                    selectedLessons,
                    lessonTopicMap
            );

            /*
             * Đồng bộ state hiển thị hiện tại của LockScreen.
             */
            lockCurrentGroupName =
                    nodeGroup;

            lockCurrentSectionName =
                    nodeSection;

            lockCurrentSelectionLabel =
                    nodeSection.isEmpty()
                            ? nodeGroup
                            : nodeGroup
                            + " / "
                            + nodeSection;

            lockCurrentLessonKeys.clear();
            lockCurrentLessonKeys.addAll(
                    selectedLessonKeys
            );

            /*
             * Xác định một topic đại diện để lưu current_topic_id/file.
             *
             * Với multi-selection, phần tô đỏ chính xác sẽ dựa vào:
             * - selected_group
             * - selected_section
             * - selected_lesson_keys
             *
             * Topic đại diện chủ yếu dùng để giữ package/context hiện tại.
             */
            TopicInfo representativeTopic =
                    currentTopicOnLockscreen;

            if (representativeTopic == null
                    && !selectedLessons.isEmpty()) {

                LessonInfo firstLesson =
                        selectedLessons.get(0);

                if (firstLesson != null
                        && firstLesson.key != null) {

                    representativeTopic =
                            lessonTopicMap.get(
                                    firstLesson.key
                            );
                }
            }

            if (representativeTopic != null) {
                currentTopicOnLockscreen =
                        representativeTopic;
            }

            // =====================================================
            // 6. LƯU LỰA CHỌN SECTION/MULTI DÙNG CHUNG
            // =====================================================
            SharedPreferences.Editor editor =
                    getSharedPreferences(
                            "lockscreen_prefs",
                            MODE_PRIVATE
                    ).edit();

            if (representativeTopic != null) {
                editor.putString(
                        "current_topic_id",
                        representativeTopic.id != null
                                ? representativeTopic.id
                                : ""
                );

                editor.putString(
                        "current_topic_file",
                        representativeTopic.fileName != null
                                ? representativeTopic.fileName
                                : ""
                );
            }

            editor.putString(
                    "selected_group",
                    nodeGroup
            );

            editor.putString(
                    "selected_section",
                    nodeSection
            );

            /*
             * Multi-selection không có một lesson duy nhất.
             */
            editor.putString(
                    "selected_lesson_key",
                    ""
            );

            editor.putString(
                    "selected_lesson_name",
                    ""
            );

            editor.putStringSet(
                    "selected_lesson_keys",
                    new java.util.HashSet<>(
                            selectedLessonKeys
                    )
            );

            editor.putString(
                    "selection_label",
                    lockCurrentSelectionLabel
            );

            editor.putBoolean(
                    "is_virtual_selection",
                    true
            );

            editor.putString(
                    "selection_mode",
                    "section"
            );

            editor.apply();

            /*
             * Tiếp tục lưu theo cơ chế cũ của LockScreenActivity.
             */
            saveLockscreenSelectionState(
                    lockCurrentSelectionLabel,
                    lockCurrentLessonKeys,
                    nodeGroup,
                    nodeSection
            );

            Log.d(
                    "SharedSelection",
                    "LockScreen saved multi selection:"
                            + " group="
                            + nodeGroup
                            + ", section="
                            + nodeSection
                            + ", lessonKeys="
                            + selectedLessonKeys
                            + ", representativeTopic="
                            + (
                            representativeTopic != null
                                    ? representativeTopic.id
                                    : "null"
                    )
            );

            // =====================================================
            // 7. BROADCAST CHO MAINACTIVITY
            // =====================================================
            Intent changedIntent =
                    new Intent(
                            "com.OPD2nd.popup.TOPIC_CHANGED"
                    );

            changedIntent.putExtra(
                    "topic_id",
                    representativeTopic != null
                            && representativeTopic.id != null
                            ? representativeTopic.id
                            : ""
            );

            changedIntent.putExtra(
                    "topic_file",
                    representativeTopic != null
                            && representativeTopic.fileName != null
                            ? representativeTopic.fileName
                            : ""
            );

            changedIntent.putExtra(
                    "selected_group",
                    nodeGroup
            );

            changedIntent.putExtra(
                    "selected_section",
                    nodeSection
            );

            changedIntent.putExtra(
                    "selection_mode",
                    "section"
            );

            sendBroadcast(
                    changedIntent
            );

            if (parentDialog != null) {
                parentDialog.dismiss();
            }

            dialog.dismiss();
        });

        dialog.show();
    }


    private void applyLockscreenMultiLessonFilter(
            String groupName,
            String sectionName,
            List<LessonInfo> selectedLessons,
            LinkedHashMap<String, TopicInfo> lessonTopicMap
    ) {
        if (selectedLessons == null || selectedLessons.isEmpty()) {
            return;
        }

        stopAllLoopsAndTimers();

        List<Sentence> selectedSentences = new ArrayList<>();
        lockCurrentLessonKeys.clear();

        TopicInfo firstTopic = null;

        // =========================================================
        // 1. GOM TOÀN BỘ CÂU CỦA CÁC LESSON ĐÃ CHỌN
        // =========================================================
        for (LessonInfo selectedLesson : selectedLessons) {
            if (selectedLesson == null
                    || selectedLesson.key == null
                    || selectedLesson.key.trim().isEmpty()) {
                continue;
            }

            TopicInfo topic =
                    lessonTopicMap.get(selectedLesson.key);

            if (topic == null) {
                continue;
            }

            if (firstTopic == null) {
                firstTopic = topic;
            }

            List<Sentence> topicSentences =
                    loadSentencesForTopicPreviewOnLockScreen(topic);

            if (topicSentences == null || topicSentences.isEmpty()) {
                Log.w(
                        "MULTI_LESSON",
                        "Topic has no sentences"
                                + " | topic=" + topic.name
                                + " | lessonKey=" + selectedLesson.key
                );

                continue;
            }

            /*
             * Fake lesson dạng topic_xxx:
             * lấy toàn bộ câu của topic đó.
             */
            if (selectedLesson.key.startsWith("topic_")) {
                selectedSentences.addAll(topicSentences);
                lockCurrentLessonKeys.add(selectedLesson.key);
                continue;
            }

            /*
             * Lesson thật:
             * chỉ lấy những câu có lesson key khớp chính xác.
             */
            for (int i = 0; i < topicSentences.size(); i++) {
                Sentence sentence =
                        topicSentences.get(i);

                if (sentence == null) {
                    continue;
                }

                LessonInfo parsed =
                        TopicTreeManager.parseLessonInfoFromSentence(
                                sentence,
                                i
                        );

                if (parsed != null
                        && parsed.key != null
                        && selectedLesson.key.equals(parsed.key)) {

                    selectedSentences.add(sentence);
                }
            }

            lockCurrentLessonKeys.add(
                    selectedLesson.key
            );
        }

        // =========================================================
        // 2. KIỂM TRA KẾT QUẢ
        // =========================================================
        if (selectedSentences.isEmpty()) {
            Toast.makeText(
                    this,
                    "Không có câu nào trong lựa chọn này!",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (sentences == null) {
            sentences = new ArrayList<>();
        }

        sentences.clear();
        sentences.addAll(selectedSentences);

        // Gán ID nếu câu chưa có ID
        for (int i = 0; i < sentences.size(); i++) {
            Sentence sentence =
                    sentences.get(i);

            if (sentence != null
                    && sentence.id == -1) {

                sentence.id = i;
            }
        }

        // =========================================================
        // 3. THIẾT LẬP TOPIC ĐẠI DIỆN VÀ PACKAGE MANAGER
        // =========================================================
        if (firstTopic != null) {
            currentTopicOnLockscreen =
                    firstTopic;

            if (topicPackageManager != null) {
                try {
                    topicPackageManager.close();
                } catch (Exception ignored) {
                }

                topicPackageManager = null;
            }

            try {
                String packageTopicName =
                        getTopicPackageNameFromJsonFile(
                                firstTopic.fileName
                        );

                topicPackageManager =
                        new TopicPackageManager(
                                this,
                                packageTopicName
                        );

                Log.d(
                        "MULTI_LESSON",
                        "Package manager set: "
                                + packageTopicName
                );

            } catch (Exception e) {
                Log.e(
                        "MULTI_LESSON",
                        "Cannot create package manager",
                        e
                );
            }

            SharedPreferences lsp =
                    getSharedPreferences(
                            "lockscreen_prefs",
                            MODE_PRIVATE
                    );

            lsp.edit()
                    .putString(
                            "current_topic_id",
                            firstTopic.id != null
                                    ? firstTopic.id
                                    : ""
                    )
                    .putString(
                            "current_topic_file",
                            firstTopic.fileName != null
                                    ? firstTopic.fileName
                                    : ""
                    )
                    .apply();

            getSharedPreferences(
                    "locks_settings",
                    MODE_PRIVATE
            )
                    .edit()
                    .putString(
                            "current_topic_id",
                            firstTopic.id != null
                                    ? firstTopic.id
                                    : ""
                    )
                    .apply();

            restoreTopicStats(
                    firstTopic.id
            );
        }

        // =========================================================
        // 4. LƯU LABEL VÀ SELECTION HIỆN TẠI
        // =========================================================
        String safeGroupName =
                groupName != null
                        ? groupName.trim()
                        : "";

        String safeSectionName =
                sectionName != null
                        ? sectionName.trim()
                        : "";

        lockCurrentGroupName =
                safeGroupName;

        lockCurrentSectionName =
                safeSectionName;

        if (safeSectionName.isEmpty()) {
            lockCurrentSelectionLabel =
                    safeGroupName
                            + " / "
                            + selectedLessons.size()
                            + " bài đã chọn";
        } else {
            lockCurrentSelectionLabel =
                    safeGroupName
                            + " / "
                            + safeSectionName
                            + " / "
                            + selectedLessons.size()
                            + " bài đã chọn";
        }

        saveLockscreenSelectionState(
                lockCurrentSelectionLabel,
                lockCurrentLessonKeys,
                safeGroupName,
                safeSectionName
        );

        // =========================================================
        // 5. RESET TRẠNG THÁI CÂU
        // =========================================================
        currentSentence = null;

        currentAudioEnFile = null;
        currentAudioViFile = null;

        hasShownAnswer = false;
        hasShownAnswerForCurrentSentence = false;
        hasPlayedExampleForCurrentSentence = false;
        isViAudioPlaying = false;

        shownHistoryKeys.clear();
        shownHistoryIdx = -1;
        historyCache.clear();

        lessonAutoLoopIndex = 0;
        listeningSequentialIndex = 0;

        rebuildTodayNewPool();
        recalcRatingCounters();

        updateStats();
        updateSuggestionList();
        updateSrsStatsBar();

        // =========================================================
        // 6. TỰ NHẬN DIỆN MODE CỦA MULTI-LESSON / TOÀN SECTION
        // =========================================================
        /*
         * sentences lúc này đã chứa toàn bộ câu
         * của các lesson vừa được chọn.
         */
        detectAndApplyStudyModeForCurrentSelection(
                "applyLockscreenMultiLessonFilter"
        );

        // =========================================================
        // 7. HIỂN THỊ TRACK ĐẦU TIÊN
        // =========================================================
        showFirstTrackOfCurrentLesson(true);

        Toast.makeText(
                this,
                "Đã chọn "
                        + selectedLessons.size()
                        + " bài",
                Toast.LENGTH_SHORT
        ).show();
    }



    /**
     * ⭐ HÀM THỰC HIỆN NEXT CÂU VỚI RATING TÙY Ý
     */
    // Giới hạn số vòng learning cho mỗi câu trong 1 phiên
    private static final int MAX_LAPS_PER_SESSION = 3;

    private void performNextSentence(int rating) {
        if (currentSentence != null) {
            Sentence s = currentSentence;

            // ✅ KIỂM TRA TRƯỚC KHI applySrsRating()
            boolean wasNewBeforeRating = (s.srsReps == 0);

            Log.d("SRS_DEBUG", "performNextSentence: en=" + s.en
                    + " | rating=" + rating
                    + " | srsReps BEFORE: " + s.srsReps
                    + " | wasNewBeforeRating: " + wasNewBeforeRating
                    + " | BEFORE: inLearning=" + s.inLearning
                    + ", intervalDays=" + s.srsIntervalDays
                    + ", lastRating=" + s.lastRating
                    + ", again=" + s.againCount
                    + ", hard=" + s.hardCount
                    + ", laps=" + s.sessionLearningLaps);

            applySrsRating(s, rating);

            Log.d("SRS_DEBUG", "performNextSentence: en=" + s.en
                    + " | srsReps AFTER: " + s.srsReps
                    + " | AFTER applySrsRating: inLearning=" + s.inLearning
                    + ", intervalDays=" + s.srsIntervalDays
                    + ", lastRating=" + s.lastRating
                    + ", again=" + s.againCount
                    + ", hard=" + s.hardCount
                    + ", laps=" + s.sessionLearningLaps);

            // ⭐ CHỈ TĂNG COUNTER NẾU LÀ LẦN ĐẦU RATING
            if (wasNewBeforeRating) {
                Log.d("SRS_DEBUG", "→ Was NEW before rating, increment New count ONCE");
                incrementNewCount();
            } else {
                Log.d("SRS_DEBUG", "→ Was already rated before, increment Review count");
                incrementReviewCount();
            }

            // ✅✅✅ THÊM 2 DÒNG NÀY NGAY SAU incrementNewCount/incrementReviewCount ✅✅✅
            recalcRatingCounters();  // ⭐ TÍNH LẠI RATING COUNTERS
            updateSrsStatsBar();     // ⭐ HIỂN THỊ THỐNG KÊ MỚI

            // ... code learning queue giữ nguyên ...
            boolean isShortLearning = s.inLearning && s.srsIntervalDays == 0;

            if (isShortLearning) {
                if (s.sessionLearningLaps < MAX_LAPS_PER_SESSION) {
                    if (!inSessionLearningQueue.contains(s)) {
                        inSessionLearningQueue.add(s);
                        Log.d("SRS_DEBUG", "  -> Added to queue: " + s.en);
                    } else {
                        Log.d("SRS_DEBUG", "  -> Already in queue: " + s.en);
                    }
                    s.sessionLearningLaps++;
                    Log.d("SRS_DEBUG", "  -> laps++ => " + s.sessionLearningLaps);
                } else {
                    inSessionLearningQueue.remove(s);
                    Log.d("SRS_DEBUG", "  -> MAX_LAPS_PER_SESSION reached, remove from queue: " + s.en);
                }
            } else {
                if (inSessionLearningQueue.remove(s)) {
                    Log.d("SRS_DEBUG", "  -> Graduate/remove from queue: " + s.en);
                }
                s.sessionLearningLaps = 0;
            }

            if (inSessionLearningQueue != null && !inSessionLearningQueue.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (Sentence q : inSessionLearningQueue) {
                    sb.append("[")
                            .append(q.en)
                            .append(" laps=").append(q.sessionLearningLaps)
                            .append(" inLearning=").append(q.inLearning)
                            .append(" int=").append(q.srsIntervalDays)
                            .append(" last=").append(q.lastRating)
                            .append("] ");
                }
                Log.d("SRS_DEBUG", "  Queue now: " + sb);
            } else {
                Log.d("SRS_DEBUG", "  Queue now: EMPTY");
            }

            saveSentencesToFile();
            // ❌ XÓA updateSrsStatsBar() Ở ĐÂY (đã gọi ở trên rồi)
            // updateSrsStatsBar();
        }

        Log.d("SRS_DEBUG", "  -> showSentenceWithSlideTransition(false)");
        showSentenceWithSlideTransition(false);
// ⭐ Chỉ PC mode mới refocus Floating Note
        // refocusFloatingNoteIfVisible();

    }




    /**
     * ⭐ XỬ LÝ KHI BẤM NÚT ANKI (Again/Hard/Good/Easy)
     */
    private void handleSrsButton(int rating) {
        if (currentSentence == null) {
            return;
        }

        if (isTypingDialogShowing) {
            return;
        }

        if (rating < 0 || rating > 3) {
            return;
        }

        /*
         * Rating thủ công sẽ hủy trạng thái tự next sau typing fail.
         */
        typingFailedAndShowingAnswer =
                false;

        Sentence s =
                currentSentence;

        Log.d(
                "SRS_DEBUG",
                "handleSrsButton"
                        + " | en=" + s.en
                        + " | manualRating=" + rating
                        + " | oldSwipePhase=" + s.swipePhase
                        + " | oldSwipeStep=" + s.swipeStep
                        + " | oldSrsPhase=" + s.srsPhase
                        + " | oldLastRating=" + s.lastRating
                        + " | oldAgain=" + s.againCount
                        + " | oldHard=" + s.hardCount
        );

        // =========================================================
        // 1. NÚT THỦ CÔNG GHI ĐÈ FLOW NGAY LẬP TỨC
        // =========================================================
        if (rating == 0) {
            /*
             * AGAIN:
             * Dù đang Normal, Hard hay Again,
             * đều quay về đầu flow Again.
             */
            s.swipePhase =
                    SWIPE_PHASE_AGAIN;

            s.swipeStep =
                    0;

            /*
             * Chỉ set cấu trúc flow để lần vuốt kế tiếp
             * tiếp tục đúng nhánh Again.
             *
             * Không tăng againCount và không set lastRating tại đây.
             * applySrsRating(0) sẽ làm việc đó đúng một lần.
             */
            s.phaseStep =
                    0;

        } else if (rating == 1) {
            /*
             * HARD:
             * Dù đang Normal hay Again,
             * chuyển sang đầu flow Hard.
             */
            s.swipePhase =
                    SWIPE_PHASE_HARD;

            s.swipeStep =
                    0;

            s.phaseStep =
                    0;

        } else {
            /*
             * GOOD hoặc EASY:
             * Thoát flow Again/Hard và trở về Normal.
             */
            s.swipePhase =
                    SWIPE_PHASE_NORMAL;

            s.swipeStep =
                    0;

            s.phaseStep =
                    0;
        }

        Log.d(
                "SRS_DEBUG",
                "Manual flow override"
                        + " | en=" + s.en
                        + " | rating=" + rating
                        + " | newSwipePhase=" + s.swipePhase
                        + " | newSwipeStep=" + s.swipeStep
        );

        // =========================================================
        // 2. KIỂM TRA TYPING TRƯỚC KHI ÁP RATING
        // =========================================================
        boolean requireTyping =
                isTypingCheckEnabledGlobally()
                        && shouldRequireTypingBeforeRating(
                        s,
                        rating
                );

        if (requireTyping) {
            s.needsTypingCheck =
                    true;

            if (rating == 1) {
                /*
                 * Chuẩn bị chuyển từ Again lên Hard.
                 */
                s.typingStage =
                        TYPING_STAGE_AGAIN_TO_HARD;

            } else if (rating == 2) {
                /*
                 * Chuẩn bị chuyển từ Hard lên Good.
                 */
                s.typingStage =
                        TYPING_STAGE_HARD_TO_GOOD;

            } else {
                s.typingStage =
                        TYPING_STAGE_NONE;
            }

            pendingRatingAfterTyping =
                    rating;

            typingCheckSentence =
                    s;

            Log.d(
                    "SRS_DEBUG",
                    "Manual rating requires typing"
                            + " | en=" + s.en
                            + " | rating=" + rating
                            + " | typingStage=" + s.typingStage
            );

            showTypingCheckDialog(
                    s
            );

            return;
        }

        // =========================================================
        // 3. KHÔNG CẦN TYPING → ÁP RATING QUA PIPELINE CHUẨN
        // =========================================================
        s.needsTypingCheck =
                false;

        s.typingStage =
                TYPING_STAGE_NONE;

        pendingRatingAfterTyping =
                -1;

        typingCheckSentence =
                null;

        /*
         * Không gán tại đây:
         *
         * s.lastRating
         * s.srsPhase
         * s.inLearning
         * s.srsIntervalDays
         * s.srsDueTime
         * s.againCount
         * s.hardCount
         *
         * applySrsRating() sẽ cập nhật đúng một lần.
         */
        goNextWithRating(
                rating
        );
    }



    // Quyết định khi nào cần typing trước khi nâng bậc
// - Không bắt typing ngay sau Again
// - Chỉ khi card đã được Anki cho lặp đủ (Reps + Interval) và đang chuẩn bị nâng bậc
    private boolean shouldRequireTypingBeforeRating(Sentence s, int pressedRating) {
        // Không yêu cầu typing cho Easy
        if (pressedRating == 3) return false;

        // Chỉ xét typing khi card đã ra khỏi learning,
        // tức là interval >= 1 ngày (đúng ý Good lần 2 trở đi)
        if (s.srsIntervalDays < 1) {
            return false;
        }

        // Nếu user vừa bấm Hard ở phase review
        if (pressedRating == 1) {
            // Bật typing khi:
            // - Card đã từng Again ít nhất 1 lần (thực sự khó)
            // - Và đã có ít nhất 2 lần ôn (reps >= 2)
            return (s.againCount > 0 && s.srsReps >= 2);
        }

        // Nếu user vừa bấm Good ở phase review
        if (pressedRating == 2) {
            // Bật typing khi:
            // - Card từng Hard nhiều lần
            //   HOẶC reps khá lớn (đã sống lâu)
            return (s.hardCount >= 2 || s.srsReps >= 4);
        }

        return false;
    }






    private void showTypingCheckDialog(Sentence sentence) {
        if (sentence == null) return;

        // ⭐ Mỗi lần vào typing check: tắt mọi audio loop đang chạy
        stopAudioLoop();
        isAudioLooping = false;
        isLoopingReplay = false;
        isExampleLooping = false;
        resetReplayFabState();
        stopLoopVisualEffect();
        stopExampleLoopVisualEffect();

        View view = getLayoutInflater().inflate(R.layout.dialog_typing_check, null);
        EditText edtAnswer = view.findViewById(R.id.edtAnswer);
        ImageView imgSentenceDialog = view.findViewById(R.id.imgSentenceDialog);
        TextView tvViDialog = view.findViewById(R.id.tvViDialog);

        // 1) Hiện nghĩa tiếng Việt
        if (tvViDialog != null) {
            if (sentence.vi != null && !sentence.vi.isEmpty()) {
                tvViDialog.setText(sentence.vi);
            } else {
                tvViDialog.setText("");
            }
        }

        // 2) Hiện hình ảnh (nếu có)
        if (imgSentenceDialog != null) {
            if (sentence.image != null && !sentence.image.isEmpty()) {
                imgSentenceDialog.setVisibility(View.VISIBLE);
                loadSentenceImageIntoView(sentence, imgSentenceDialog);
            } else {
                imgSentenceDialog.setVisibility(View.GONE);
            }
        }

        // Câu chuẩn để so sánh
        String correct = sentence.en != null ? sentence.en : "";

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("")
                .setView(view)
                .setCancelable(false)
                .setPositiveButton("OK", null)
                .setNegativeButton("Huỷ", (d, which) -> {
                    // ❌ Không còn coi cancel là Again nữa

                    // 1) Clear trạng thái typing đang chờ (nếu có)
                    typingCheckSentence = null;
                    pendingRatingAfterTyping = -1;

                    // 2) Có thể xem cancel là “bỏ qua typing, vẫn xử lý câu hiện tại như Good”
                    if (sentence != null) {
                        // Nếu bạn có hàm reset counters Again/Hard, đặt ở đây
                        // clearAgainHardCounters(sentence);
                    }

                    // 3) Áp dụng rating mặc định: Good (2) qua pipeline chuẩn
                    performNextSentence(2);
                })
                .create();

        isTypingDialogShowing = true;

        dialog.setOnDismissListener(d -> {
            isTypingDialogShowing = false;
        });

        dialog.show();

        // Gán listener cho nút OK sau khi dialog.show()

        Button btnOk = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (btnOk != null) {
            btnOk.setOnClickListener(v -> {
                String user = edtAnswer.getText().toString().trim();

                // 1) Check đúng/sai
                boolean correctTyping = isTypingCorrect(correct, user);

                // 2) Nếu đúng → PLAY AUDIO NGAY BÂY GIỜ
                if (correctTyping) {
                    if (!isMuted && sentence.audio != null && !sentence.audio.isEmpty()) {
                        playAudio(sentence.audio, false, null);
                    }
                }

                // 3) Định nghĩa onDone: chỉ gọi onTypingPassed / onTypingFailed
                Runnable onDone = () -> {
                    if (correctTyping) {
                        // ===== GÕ ĐÚNG =====
                        onTypingPassed(sentence);
                    } else {
                        // ===== GÕ SAI =====
                        Toast.makeText(this,
                                "Sai rồi, thẻ này sẽ cần học lại.",
                                Toast.LENGTH_SHORT).show();
                        onTypingFailed(sentence);
                    }

                    dialog.dismiss();
                };

                // 4) Tuỳ đúng/sai mà chọn dialog:
                if (correctTyping) {
                    // ✅ GÕ ĐÚNG: dialog chúc mừng, auto close sau 2s hoặc bấm OK
                    showTypingCorrectDialog(correct, onDone);
                } else {
                    // ❌ GÕ SAI: dialog so sánh cũ
                    showTypingCompareDialog(user, correct, onDone);
                }
            });
        }

        // ⭐ Focus sẵn vào ô nhập + bật keyboard
        edtAnswer.requestFocus();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                            | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
            );
        }
    }





    // Gọi khi user gõ ĐÚNG trong typing check
    private void onTypingPassed(Sentence s) {
        if (s == null) return;

        // Xoá trạng thái typing cho câu này
        s.needsTypingCheck = false;
        s.typingStage = TYPING_STAGE_NONE;
        s.againCount = 0;
        s.hardCount = 0;

        // Lấy rating đã “pending” sau khi typing
        int rating;
        if (pendingRatingAfterTyping >= 0) {
            rating = pendingRatingAfterTyping;
        } else {
            rating = 2; // default Good nếu vì lý do gì không có pending
        }

        pendingRatingAfterTyping = -1;
        typingCheckSentence = null;
        isTypingDialogShowing = false;

        // ⭐ Dùng chung luồng với bấm nút SRS:
        // applySrsRating + update queue + showSentence(...) đều nằm trong goNextWithRating/performNextSentence
        goNextWithRating(rating);
    }


    // Gọi khi user gõ SAI trong typing check
    private void onTypingFailed(Sentence s) {
        if (s == null) return;

        // 1) Hạ trạng thái typing trên câu
        s.needsTypingCheck = false;
        s.typingStage = TYPING_STAGE_NONE;
        s.againCount = 0;
        s.hardCount = 0;

        // 2) Clear trạng thái typing global
        pendingRatingAfterTyping = -1;
        typingCheckSentence = null;
        isTypingDialogShowing = false;

        // 3) ❌ KHÔNG áp dụng rating Again ở đây nữa
        //    applySrsRating(s, 0);  // BỎ DÒNG NÀY

        // 4) Queue câu vào short-term learning nếu cần
        boolean isShortLearning =
                s.inLearning
                        && s.srsIntervalDays == 0;

        if (isShortLearning) {
            if (!inSessionLearningQueue.contains(s)) {
                inSessionLearningQueue.add(s);
            }
        } else {
            inSessionLearningQueue.remove(s);
        }

        // 5) Lưu & cập nhật bar (tuỳ bạn, có thể giữ nguyên)
        saveSentencesToFile();
        updateSrsStatsBar();

        // 6) Bind lại câu hiện tại (giữ nguyên UX cũ)
        currentSentence = s;
        currentAudioEnFile = s.audio;
        currentAudioViFile = s.audio_vi;
        updateCardViewWithSentence(s);

        // 7) Show full đáp án, đánh dấu trạng thái "vừa fail typing"
        showAnswerForCurrentSentenceFromTap();
        typingFailedAndShowingAnswer = true;
    }







    private void loadSentenceImageIntoView(Sentence s, ImageView target) {
        if (s == null || target == null) return;

        if (s.image == null || s.image.isEmpty()) {
            target.setVisibility(View.GONE);
            return;
        }

        target.setVisibility(View.VISIBLE);

        Object imageSource;
        File imgFile = getImageFile(s.image);
        if (imgFile != null && imgFile.exists()) {
            imageSource = imgFile;
        } else {
            // Thử asset
            imageSource = Uri.parse("file:///android_asset/images/" + s.image);
        }

        float density = getResources().getDisplayMetrics().density;
        int radiusPx = (int) (IMAGE_CORNER_RADIUS_DP * density);

        Glide.with(target.getContext())
                .load(imageSource)
                .centerCrop()
                .transform(new com.bumptech.glide.load.resource.bitmap.RoundedCorners(radiusPx))
                .into(target);
    }



    private boolean isTypingCorrect(String correct, String user) {
        if (correct == null) correct = "";
        if (user == null) user = "";

        // Chuẩn hóa: trim + lowercase
        String c = correct.trim().toLowerCase(Locale.US);
        String u = user.trim().toLowerCase(Locale.US);

        // Bỏ dấu câu và ký tự không cần: . , ? ! ; : " ' ( ) / \
        String punctuationRegex = "[\\.,\\?\\!;:\\\"'()\\\\/]";

        c = c.replaceAll(punctuationRegex, "");
        u = u.replaceAll(punctuationRegex, "");

        // Bỏ bớt khoảng trắng dư (nhiều space thành 1, và bỏ đầu/cuối)
        c = c.replaceAll("\\s+", " ").trim();
        u = u.replaceAll("\\s+", " ").trim();

        return c.equals(u);
    }



    private String normalizeForTypingCompare(String s) {
        if (s == null) return "";

        String result = s.trim().toLowerCase(Locale.US);

        String punctuationRegex = "[\\.,\\?\\!;:\\\"'()\\\\/]";
        result = result.replaceAll(punctuationRegex, "");

        result = result.replaceAll("\\s+", " ").trim();

        return result;
    }






    private void appendDiffWord(SpannableStringBuilder userSpan,
                                SpannableStringBuilder correctSpan,
                                String uRaw, String cRaw,
                                int colorRed, int colorGreen, int colorGrey) {

        // So sánh theo lowercase nhưng hiển thị theo raw
        String uNorm = uRaw.toLowerCase(Locale.US);
        String cNorm = cRaw.toLowerCase(Locale.US);

        int maxLen = Math.max(uNorm.length(), cNorm.length());

        for (int i = 0; i < maxLen; i++) {
            char ucNorm = (i < uNorm.length()) ? uNorm.charAt(i) : 0;
            char ccNorm = (i < cNorm.length()) ? cNorm.charAt(i) : 0;

            char ucRaw = (i < uRaw.length()) ? uRaw.charAt(i) : ' ';
            char ccRaw = (i < cRaw.length()) ? cRaw.charAt(i) : ' ';

            boolean userHasChar    = (i < uNorm.length());
            boolean correctHasChar = (i < cNorm.length());

            if (!userHasChar && correctHasChar) {
                // User thiếu ký tự
                int su = userSpan.length();
                userSpan.append(" "); // hoặc '▢'
                userSpan.setSpan(new ForegroundColorSpan(colorGrey),
                        su, su + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                int sc = correctSpan.length();
                correctSpan.append(ccRaw);
                correctSpan.setSpan(new ForegroundColorSpan(colorGreen),
                        sc, sc + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (userHasChar && !correctHasChar) {
                // User có ký tự dư
                int su = userSpan.length();
                userSpan.append(ucRaw);
                userSpan.setSpan(new ForegroundColorSpan(colorRed),
                        su, su + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                int sc = correctSpan.length();
                correctSpan.append(" ");
                correctSpan.setSpan(new ForegroundColorSpan(colorGrey),
                        sc, sc + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                // Cả 2 đều có ký tự ở vị trí này
                if (ucNorm == ccNorm) {
                    // Ký tự đúng
                    userSpan.append(ucRaw);
                    correctSpan.append(ccRaw);
                } else {
                    // Ký tự sai
                    int su = userSpan.length();
                    userSpan.append(ucRaw);
                    userSpan.setSpan(new ForegroundColorSpan(colorRed),
                            su, su + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                    int sc = correctSpan.length();
                    correctSpan.append(ccRaw);
                    correctSpan.setSpan(new ForegroundColorSpan(colorGreen),
                            sc, sc + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
    }






    private void showTypingCorrectDialog(String correctAnswer, Runnable onDone) {
        View view = getLayoutInflater().inflate(R.layout.dialog_typing_correct, null);
        TextView tvCorrectPreview = view.findViewById(R.id.tvCorrectAnswerPreview);

        if (tvCorrectPreview != null) {
            tvCorrectPreview.setText(correctAnswer != null ? correctAnswer : "");
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .setPositiveButton("OK", (d, which) -> {
                    if (onDone != null) onDone.run();
                })
                .create();

        dialog.show();

        // Auto dismiss + gọi onDone sau 2 giây
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (dialog.isShowing()) {
                dialog.dismiss();
                if (onDone != null) onDone.run();
            }
        }, 5000);
    }



    /**
     * ⭐ RESET TẤT CẢ NÚT VỀ TRẠNG THÁI BÌNH THƯỜNG
     */
    private void resetAllSrsButtons() {
        MaterialButton btnAgain = findViewById(R.id.btnAgain);
        MaterialButton btnHard  = findViewById(R.id.btnHard);
        MaterialButton btnGood  = findViewById(R.id.btnGood);
        MaterialButton btnEasy  = findViewById(R.id.btnEasy);

        MaterialButton[] allButtons = {btnAgain, btnHard, btnGood, btnEasy};
        for (MaterialButton btn : allButtons) {
            if (btn != null) {
                btn.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)  // ⭐ SÁNG LẠI HOÀN TOÀN
                        .setDuration(150)
                        .start();

                btn.setElevation(2f);  // ⭐ RESET SHADOW
            }
        }
    }






    private void showPrevSentence() {
        // ⭐ LISTENING MODE: nếu đang bật nghe tuần tự → prev theo thứ tự
        if (listeningSequentialMode) {
            goPrevSequentialInTopic();
            return;
        }

        if (shownHistoryIdx > 0) {
            shownHistoryIdx--;
            Log.d("HISTORY_DEBUG", "Prev → shownHistoryIdx=" + shownHistoryIdx
                    + " / size=" + shownHistoryKeys.size());

            nextFReservedForImage = false;

            boolean answerHidden = (enText != null && enText.getVisibility() == View.GONE);

            if (answerHidden) {
                showSentenceWithScaleTransition(true);
            } else {
                showSentenceWithLayoutScaleTransition(true);
            }

            updateSrsStatsBar();
        } else {
            Toast.makeText(this, "Không có câu trước!", Toast.LENGTH_SHORT).show();
        }
    }


    private void goPrevSequentialInTopic() {
        if (sentences == null || sentences.isEmpty()) {
            Log.w("LISTENING_SEQ", "No sentences in current topic (prev)");
            return;
        }

        int currentIndex = -1;

        if (currentSentence != null) {
            String curKey = makeSentenceKey(currentSentence);

            for (int i = 0; i < sentences.size(); i++) {
                Sentence s = sentences.get(i);
                if (s == currentSentence || (s != null && makeSentenceKey(s).equals(curKey))) {
                    currentIndex = i;
                    break;
                }
            }
        }

        int prevIndex = currentIndex - 1;
        if (prevIndex < 0) {
            prevIndex = sentences.size() - 1;
        }

        listeningSequentialIndex = prevIndex;
        lessonAutoLoopIndex = prevIndex;

        currentSentence = sentences.get(prevIndex);

        currentAudioEnFile = currentSentence.audio;
        currentAudioViFile = currentSentence.audio_vi;

        hasShownAnswer = false;
        hasShownAnswerForCurrentSentence = false;
        isViAudioPlaying = false;
        hasPlayedExampleForCurrentSentence = false;

        saveCurrentLockscreenPosition();

        updateCardViewWithSentence(currentSentence);
        updateNowPlayingHeader();
        updateNowPlayingInfo(currentSentence);

        maybeFocusFloatingNoteOnNext();

        SharedPreferences appPrefs = getSharedPreferences(PREFS_APP_SETTINGS, MODE_PRIVATE);
        boolean playEnOnNext = appPrefs.getBoolean(KEY_PLAY_EN_AUDIO_ON_NEXT, false);
        boolean playViOnNext = appPrefs.getBoolean(KEY_PLAY_VI_AUDIO, false);

        if (!isMuted) {
            if (playEnOnNext
                    && !TextUtils.isEmpty(currentAudioEnFile)) {

                playAudio(
                        currentAudioEnFile,
                        false,
                        null
                );

            } else if (playViOnNext
                    && !TextUtils.isEmpty(currentAudioViFile)) {

                playAudio(
                        currentAudioViFile,
                        false,
                        null,
                        audioViVolume
                );
            }
        }
    }

    private void updateImageAndFavoriteOnly(Sentence s) {
        // Dùng đúng kiểu của view trong layout (ShapeableImageView)
        ShapeableImageView imgSentence = findViewById(R.id.imageView);

        if (imgSentence != null) {
            String uniqueKey = makeSentenceKey(s);
            imgSentence.setTag(uniqueKey);

            if (showImageInPopup) {
                Object imageSource;
                File imgFile = getImageFile(s.image);
                if (imgFile != null && imgFile.exists()) {
                    imageSource = imgFile;
                } else if (!TextUtils.isEmpty(s.image)) {
                    try {
                        InputStream is = getAssets().open("images/" + s.image);
                        Bitmap bitmap = BitmapFactory.decodeStream(is);
                        imageSource = bitmap;
                        is.close();
                    } catch (Exception e) {
                        imageSource = R.drawable.no_image;
                    }
                } else {
                    imageSource = R.drawable.no_image;
                }

                float density = getResources().getDisplayMetrics().density;
                int radiusPx = (int) (IMAGE_CORNER_RADIUS_DP * density);

                Glide.with(imgSentence.getContext())
                        .load(imageSource)
                        .centerCrop()
                        .transform(new com.bumptech.glide.load.resource.bitmap.RoundedCorners(radiusPx))
                        .into(new com.bumptech.glide.request.target.ImageViewTarget<Drawable>(imgSentence) {
                            @Override
                            protected void setResource(@Nullable Drawable resource) {
                                if (imgSentence.getTag() != null && imgSentence.getTag().equals(uniqueKey)) {
                                    imgSentence.setImageDrawable(resource);

                                    imgSentence.invalidate();

                                    // Quyết định ẩn/hiện sau khi load xong
                                    applyImageVisibility(imgSentence);
                                }
                            }
                        });
            } else {
                // Không load ảnh, nhưng vẫn áp logic ẩn/hiện chung
                applyImageVisibility(imgSentence);
            }
        }

        ImageButton btnFavorite = findViewById(R.id.btnFavorite);
        if (btnFavorite != null) {
            btnFavorite.setImageResource(
                    s.favorite ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline
            );
        }
    }





    /**
     * Lấy phần từ loại được đặt ở đầu trường IPA.
     *
     * Ví dụ:
     * "(n) /kʌzn/"                 -> "(n)"
     * "(noun) /ˈkɒn.tekst/"        -> "(noun)"
     * "(v phr) /breɪk ʌp/"         -> "(v phr)"
     * "(phrasal verb) /lʊk ʌp/"    -> "(phrasal verb)"
     * "/kʌzn/"                     -> ""
     */
    private String extractPartOfSpeechFromIpa(String ipaValue) {
        return SentenceTextFormatter.extractPartOfSpeechFromIpa(ipaValue, false);
    }

    private void renderVietnameseQuestionAndAnswer(
            String vietnamese,
            String partOfSpeech
    ) {
        if (tvVietnameseTop != null) {
            String safeVi = vietnamese != null ? vietnamese.trim() : "";

            if (!TextUtils.isEmpty(safeVi)
                    && !TextUtils.isEmpty(partOfSpeech)) {
                String fullText = safeVi + " " + partOfSpeech;
                SpannableString spannable = new SpannableString(fullText);

                spannable.setSpan(
                        new ForegroundColorSpan(Color.parseColor("#2979FF")),
                        0,
                        safeVi.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                spannable.setSpan(
                        new ForegroundColorSpan(Color.BLACK),
                        safeVi.length() + 1,
                        fullText.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                tvVietnameseTop.setText(spannable);
            } else if (!TextUtils.isEmpty(safeVi)) {
                tvVietnameseTop.setText(safeVi);
            } else {
                tvVietnameseTop.setText(partOfSpeech);
            }
        }

        if (tvVietnamese != null) {
            tvVietnamese.setText(vietnamese);
        }
    }

    private void releaseCurrentMediaPlayerForPlayback() {
        if (currentMediaPlayer == null) return;

        try {
            if (currentMediaPlayer.isPlaying()) {
                currentMediaPlayer.pause();
            }
            currentMediaPlayer.seekTo(0);
        } catch (Exception ignored) {
        }

        try {
            currentMediaPlayer.release();
        } catch (Exception ignored) {
        }

        currentMediaPlayer = null;
    }







    private void updateCardViewWithSentence(Sentence s) {
        if (s == null) return;

        Log.d("SRS_DEBUG", "[BIND] updateCardViewWithSentence: en=" + s.en
                + " | inLearning=" + s.inLearning
                + ", intDays=" + s.srsIntervalDays
                + ", lastRating=" + s.lastRating
                + ", laps=" + s.sessionLearningLaps);

        resetScaleForNewSentence();
        adjustImageContainerForAnswer(false);

        Log.d("UI_UPDATE", "updateCardViewWithSentence → EN=" + s.en
                + " | VI=" + s.vi
                + " | IMAGE_FIELD=" + s.image);

        stopLoopVisualEffect();
        isAudioLooping = false;
        isLoopingReplay = false;
        resetExampleLoopState();

        hasPlayedExampleForCurrentSentence = false;

        if (tvVietnameseTop == null) {
            tvVietnameseTop = findViewById(R.id.tvVietnameseTop);
        }
        if (tvVietnamese == null) {
            tvVietnamese = findViewById(R.id.tvVietnamese);
        }
        if (ipaText == null) {
            ipaText = findViewById(R.id.ipaText);
        }

        ShapeableImageView imgSentence = findViewById(R.id.imageView);

        if (exampleContainer != null) {
            exampleContainer.setVisibility(View.GONE);
            if (tvExampleEnInline != null) tvExampleEnInline.setText("");
            if (tvExampleViInline != null) tvExampleViInline.setText("");
        }

        String en =
                s.en != null
                        ? s.en
                        : "";

        String vi =
                s.vi != null
                        ? s.vi
                        : "";

        String ipa =
                s.ipa != null
                        ? s.ipa
                        : "";

// Lấy từ loại ở đầu trường IPA, ví dụ: (n), (noun), (v phr)...
        String partOfSpeech =
                extractPartOfSpeechFromIpa(ipa);

        applySmartEnglishTextSize(
                enText,
                en
        );

        if (ipaText != null) {
            ipaText.setText(
                    ipa
            );
        }

        renderVietnameseQuestionAndAnswer(vi, partOfSpeech);

        if (switchShowEnglish != null) {
            boolean showEn = switchShowEnglish.isChecked();
            enText.setVisibility(showEn ? View.VISIBLE : View.GONE);
            if (ipaText != null) ipaText.setVisibility(showEn ? View.VISIBLE : View.GONE);
        } else {
            enText.setVisibility(View.VISIBLE);
            if (ipaText != null) ipaText.setVisibility(View.VISIBLE);
        }

        boolean showVi = switchShowVietnamese == null || switchShowVietnamese.isChecked();

        if (showVi) {
            if (tvVietnameseTop != null) tvVietnameseTop.setVisibility(View.VISIBLE);
            if (tvVietnamese != null) tvVietnamese.setVisibility(View.GONE);
        } else {
            if (tvVietnameseTop != null) tvVietnameseTop.setVisibility(View.GONE);
            if (tvVietnamese != null) tvVietnamese.setVisibility(View.GONE);
        }

        // =========================
        // HÌNH ẢNH: user file → package/cache → no_image
        // =========================
        if (imgSentence != null) {
            String uniqueKey = makeSentenceKey(s);
            imgSentence.setTag(TAG_IMAGE_UNIQUE_KEY, uniqueKey);

            if (showImageInPopup) {
                Object imageSource = R.drawable.no_image;

                if (!TextUtils.isEmpty(s.image)) {
                    File imgFile = getImageFile(s.image);

                    if (imgFile != null && imgFile.exists() && imgFile.length() > 0) {
                        imageSource = imgFile;
                        Log.d("UI_IMAGE", "Ảnh dùng FILE/PACKAGE: " + imgFile.getAbsolutePath());
                    } else {
                        Log.e("UI_IMAGE", "Không load được ảnh, dùng no_image: " + s.image);
                    }
                } else {
                    Log.d("UI_IMAGE", "Không có image field, dùng no_image");
                }

                float density = getResources().getDisplayMetrics().density;
                int radiusPx = (int) (IMAGE_CORNER_RADIUS_DP * density);

                Glide.with(imgSentence.getContext()).clear(imgSentence);
                imgSentence.setImageDrawable(null);
                imgSentence.setImageResource(R.drawable.no_image);

                Glide.with(imgSentence.getContext())
                        .load(imageSource)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .signature(new ObjectKey(uniqueKey))
                        .placeholder(R.drawable.no_image)
                        .centerCrop()
                        .transform(new com.bumptech.glide.load.resource.bitmap.RoundedCorners(radiusPx))
                        .into(imgSentence);

                applyImageVisibility(imgSentence);

            } else {
                applyImageVisibility(imgSentence);
            }
        }

        ImageButton btnFavorite = findViewById(R.id.btnFavorite);
        if (btnFavorite != null) {
            btnFavorite.setImageResource(
                    s.favorite ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline
            );
        }

        MaterialButton btnLoopFemale = findViewById(R.id.btnLoopFemale);
        MaterialButton btnLoopMale = findViewById(R.id.btnLoopMale);

        boolean hasFemaleVoice = s.audio_female != null && !s.audio_female.isEmpty();
        boolean hasMaleVoice = s.audio_male != null && !s.audio_male.isEmpty();
        boolean hasExampleVi = s.example_audio_vi != null && !s.example_audio_vi.isEmpty();

        if (btnLoopFemale != null) {
            if (hasFemaleVoice) {
                btnLoopFemale.setText("Female");
            } else if (hasExampleVi) {
                btnLoopFemale.setText("Example VI");
            } else {
                btnLoopFemale.setText("Audio EN");
            }
        }

        if (btnLoopMale != null) {
            if (hasMaleVoice) {
                btnLoopMale.setText("Male");
            } else {
                btnLoopMale.setText("Audio Example");
            }
        }

        hasShownAnswerForCurrentSentence = false;
        imageSingleTapCountForCurrentSentence = 0;

        if (playEnMaleOnNext) {

            if (suppressPresetAutoAudioOnce) {
                Log.d(
                        "STUDY_MODE",
                        "Skip auto Male audio during preset refresh"
                );

                suppressPresetAutoAudioOnce = false;

            } else {
                tempPlayMaleOnce = true;
                startVoiceLoop("male");
            }

        } else {
            suppressPresetAutoAudioOnce = false;
        }



        updateNowPlayingInfo(s);
        preloadNextImage();
    }

    private void toggleFavoriteCurrentSentence() {
        if (currentSentence == null || sentences == null) return;

        ImageButton btnFavorite = findViewById(R.id.btnFavorite);

        for (Sentence s : sentences) {
            if (safeEquals(s.en, currentSentence.en) && safeEquals(s.vi, currentSentence.vi)) {
                s.favorite = !s.favorite;

                if (btnFavorite != null) {
                    btnFavorite.setImageResource(
                            s.favorite ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline
                    );
                }

                saveSentencesToFile();
                updateStats();
                break;
            }
        }
    }





    private void applySmartEnglishTextSize(TextView tv, String text) {
        if (tv == null) return;
        if (text == null) text = "";

        tv.setSingleLine(false);
        tv.setMaxLines(6);
        tv.setEllipsize(null);

        final String finalText = text;

        tv.post(() -> {
            int width = tv.getWidth() - tv.getPaddingLeft() - tv.getPaddingRight();

            if (width <= 0) {
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 34);
                tv.setText(finalText);
                return;
            }

            float[] sizesSp = {34f, 30f, 26f, 20f};
            float chosenSize = 20f;

            for (float sizeSp : sizesSp) {
                TextPaint paint = new TextPaint(tv.getPaint());
                float sizePx = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_SP,
                        sizeSp,
                        tv.getResources().getDisplayMetrics()
                );
                paint.setTextSize(sizePx);

                StaticLayout layout = StaticLayout.Builder
                        .obtain(finalText, 0, finalText.length(), paint, width)
                        .setAlignment(Layout.Alignment.ALIGN_CENTER)
                        .setLineSpacing(tv.getLineSpacingExtra(), tv.getLineSpacingMultiplier())
                        .setIncludePad(tv.getIncludeFontPadding())
                        .build();

                if (layout.getLineCount() <= 6) {
                    chosenSize = sizeSp;
                    break;
                }
            }

            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, chosenSize);
            tv.setText(finalText);
            tv.requestLayout();
        });
    }



    private void showEnglishHideVietnamese() {
        TextView viText = findViewById(R.id.tvVietnamese);
        TextView ipaText = findViewById(R.id.ipaText);

        if (currentSentence != null) {
            enText.setText(currentSentence.en != null ? currentSentence.en : "");
            if (ipaText != null) {
                ipaText.setText(currentSentence.ipa != null ? currentSentence.ipa : "");
            }
        }

        // Đảm bảo EN/IPA/VI được layout trước khi fit
        enText.setVisibility(View.VISIBLE);
        if (ipaText != null) ipaText.setVisibility(View.VISIBLE);
        if (viText != null) viText.setVisibility(View.VISIBLE);

        // ✅ Chỉ scale khi thật sự cần, đảm bảo hiển thị trọn vẹn trong 2 dòng, không "..."
        enText.post(this::fitEnglishIntoTwoLines);

        // ⭐ Chỉ hiện 4 nút SRS khi KHÔNG ở Listening mode
        View srsContainer = findViewById(R.id.srsButtonsContainer);
        if (srsContainer != null) {
            srsContainer.setVisibility(
                    listeningSequentialMode ? View.GONE : View.VISIBLE
            );
        }

        hasShownAnswer = true;
    }




    private void fitEnglishIntoTwoLines() {
        if (enText == null) return;

        // Cỡ chữ mặc định & min (sp)
        float maxSp = 34f;
        float minSp = 18f;

        // Bắt đầu từ 34sp
        enText.setTextSize(TypedValue.COMPLEX_UNIT_SP, maxSp);
        enText.setMaxLines(Integer.MAX_VALUE); // tạm bỏ giới hạn để đo
        enText.setEllipsize(null);

        // Force đo layout hiện tại
        enText.measure(
                View.MeasureSpec.makeMeasureSpec(enText.getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );

        // Nếu đã <= 2 dòng thì thôi
        if (enText.getLayout() != null && enText.getLayout().getLineCount() <= 2) {
            enText.setMaxLines(2);
            return;
        }

        // Nếu > 2 dòng thì bắt đầu giảm size
        float currentSp = maxSp;
        while (currentSp > minSp) {
            currentSp -= 1f;
            enText.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentSp);

            // Đo lại
            enText.measure(
                    View.MeasureSpec.makeMeasureSpec(enText.getWidth(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            );

            Layout layout = enText.getLayout();
            if (layout != null && layout.getLineCount() <= 2) {
                break;
            }
        }

        // Sau khi tìm được size phù hợp, giới hạn 2 dòng, không ellipsize
        enText.setMaxLines(2);
        enText.setEllipsize(null);
    }

    private void playAudio(
            String audioFile,
            boolean loop,
            Runnable onComplete,
            float volumeScale
    ) {
        final String finalAudioFile =
                audioFile != null
                        ? audioFile.trim()
                        : "";

        try {
            releaseCurrentMediaPlayerForPlayback();

            if (finalAudioFile.isEmpty()) {
                Toast.makeText(
                        this,
                        "Không tìm thấy file audio",
                        Toast.LENGTH_SHORT
                ).show();

                if (onComplete != null) {
                    onComplete.run();
                }

                return;
            }

            MediaPlayer mediaPlayer =
                    new MediaPlayer();

            boolean dataSourceReady =
                    AudioDataSourceHelper.setDataSource(
                            this,
                            mediaPlayer,
                            finalAudioFile,
                            topicPackageManager
                    );

            if (!dataSourceReady) {
                try {
                    mediaPlayer.release();
                } catch (Exception ignored) {
                }

                Toast.makeText(
                        this,
                        "Không tìm thấy file audio",
                        Toast.LENGTH_SHORT
                ).show();

                if (onComplete != null) {
                    onComplete.run();
                }

                return;
            }

            mediaPlayer.setLooping(loop);

            float safeVolumeScale =
                    Math.max(
                            0f,
                            Math.min(
                                    1f,
                                    volumeScale
                            )
                    );

            float finalVolume =
                    isMuted
                            ? 0f
                            : safeVolumeScale;

            mediaPlayer.setVolume(
                    finalVolume,
                    finalVolume
            );

            Log.d(
                    "PLAY_AUDIO",
                    "Start audio"
                            + " | file=" + finalAudioFile
                            + " | loop=" + loop
                            + " | volumeScale=" + safeVolumeScale
                            + " | finalVolume=" + finalVolume
                            + " | muted=" + isMuted
            );

            currentMediaPlayer =
                    mediaPlayer;

            mediaPlayer.setOnCompletionListener(
                    completedPlayer -> {
                        if (!loop) {
                            try {
                                completedPlayer.release();
                            } catch (Exception ignored) {
                            }

                            if (currentMediaPlayer == completedPlayer) {
                                currentMediaPlayer = null;
                            }

                            if (onComplete != null) {
                                onComplete.run();
                            }
                        }
                    }
            );

            mediaPlayer.setOnErrorListener(
                    (errorPlayer, what, extra) -> {
                        Log.e(
                                "PLAY_AUDIO",
                                "MediaPlayer error"
                                        + " | what=" + what
                                        + " | extra=" + extra
                                        + " | file=" + finalAudioFile
                        );

                        try {
                            errorPlayer.release();
                        } catch (Exception ignored) {
                        }

                        if (currentMediaPlayer == errorPlayer) {
                            currentMediaPlayer = null;
                        }

                        if (onComplete != null) {
                            onComplete.run();
                        }

                        return true;
                    }
            );

            mediaPlayer.prepare();
            mediaPlayer.start();

        } catch (Exception e) {
            Log.e(
                    "PLAY_AUDIO",
                    "playAudio error"
                            + " | file=" + finalAudioFile
                            + " | loop=" + loop
                            + " | volumeScale=" + volumeScale,
                    e
            );

            Toast.makeText(
                    this,
                    "Lỗi phát âm thanh",
                    Toast.LENGTH_SHORT
            ).show();

            if (currentMediaPlayer != null) {
                try {
                    currentMediaPlayer.release();
                } catch (Exception ignored) {
                }

                currentMediaPlayer = null;
            }

            if (onComplete != null) {
                onComplete.run();
            }
        }
    }

    private void showAudioSettingsDialog() {
        final String[] delayItems = {
                "0.2 giây",
                "0.35 giây",
                "0.5 giây",
                "0.75 giây",
                "1 giây",
                "1.5 giây",
                "2 giây",
                "3 giây",
                "4 giây",
                "5 giây"
        };

        final int[] delayValues = {
                200,
                350,
                500,
                750,
                1000,
                1500,
                2000,
                3000,
                4000,
                5000
        };

        int checkedDelayIndex = 4;

        for (int i = 0; i < delayValues.length; i++) {
            if (delayBetweenAudioLoopMs == delayValues[i]) {
                checkedDelayIndex = i;
                break;
            }
        }

        // =========================================================
        // ROOT LAYOUT
        // =========================================================
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);

        int paddingDp = 20;
        int paddingPx = (int) (
                paddingDp * getResources().getDisplayMetrics().density
        );

        rootLayout.setPadding(
                paddingPx,
                paddingPx / 2,
                paddingPx,
                paddingPx / 2
        );

        // =========================================================
        // PHẦN CHỌN DELAY LOOP
        // =========================================================
        TextView tvDelayTitle = new TextView(this);
        tvDelayTitle.setText("Thời gian giữa các lần lặp");
        tvDelayTitle.setTextSize(16f);
        tvDelayTitle.setTextColor(Color.parseColor("#222222"));
        tvDelayTitle.setTypeface(
                tvDelayTitle.getTypeface(),
                Typeface.BOLD
        );

        rootLayout.addView(tvDelayTitle);

        RadioGroup delayRadioGroup = new RadioGroup(this);
        delayRadioGroup.setOrientation(RadioGroup.VERTICAL);

        for (int i = 0; i < delayItems.length; i++) {
            RadioButton radioButton = new RadioButton(this);

            radioButton.setId(
                    View.generateViewId()
            );

            radioButton.setText(
                    delayItems[i]
            );

            radioButton.setTag(
                    delayValues[i]
            );

            radioButton.setTextSize(14f);

            delayRadioGroup.addView(
                    radioButton
            );

            if (i == checkedDelayIndex) {
                radioButton.setChecked(true);
            }
        }

        rootLayout.addView(delayRadioGroup);

        // =========================================================
        // KHOẢNG CÁCH
        // =========================================================
        Space spacer = new Space(this);

        spacer.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        paddingPx / 2
                )
        );

        rootLayout.addView(spacer);

        // =========================================================
        // PHẦN CHỈNH VOLUME AUDIO VI
        // =========================================================
        TextView tvVolumeTitle = new TextView(this);

        int initialVolumePercent =
                Math.round(audioViVolume * 100f);

        tvVolumeTitle.setText(
                "Âm lượng Audio VI: "
                        + initialVolumePercent
                        + "%"
        );

        tvVolumeTitle.setTextSize(16f);
        tvVolumeTitle.setTextColor(Color.parseColor("#222222"));

        tvVolumeTitle.setTypeface(
                tvVolumeTitle.getTypeface(),
                Typeface.BOLD
        );

        rootLayout.addView(tvVolumeTitle);

        SeekBar volumeSeekBar = new SeekBar(this);
        volumeSeekBar.setMax(100);
        volumeSeekBar.setProgress(initialVolumePercent);

        rootLayout.addView(volumeSeekBar);

        TextView tvVolumeHint = new TextView(this);
        tvVolumeHint.setText(
                "Chỉ áp dụng cho audio_vi và example_audio_vi"
        );
        tvVolumeHint.setTextSize(12f);
        tvVolumeHint.setTextColor(Color.parseColor("#777777"));

        rootLayout.addView(tvVolumeHint);

        volumeSeekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser
                    ) {
                        tvVolumeTitle.setText(
                                "Âm lượng Audio VI: "
                                        + progress
                                        + "%"
                        );
                    }

                    @Override
                    public void onStartTrackingTouch(
                            SeekBar seekBar
                    ) {
                    }

                    @Override
                    public void onStopTrackingTouch(
                            SeekBar seekBar
                    ) {
                    }
                }
        );

        // =========================================================
        // TẠO DIALOG
        // =========================================================
        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle("Cài đặt âm thanh")
                        .setView(rootLayout)
                        .setPositiveButton(
                                "Lưu",
                                null
                        )
                        .setNegativeButton(
                                "Hủy",
                                null
                        )
                        .create();

        dialog.setOnShowListener(unused -> {
            dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener(v -> {

                // =================================================
                // LẤY DELAY ĐÃ CHỌN
                // =================================================
                int selectedRadioId =
                        delayRadioGroup.getCheckedRadioButtonId();

                int selectedDelay =
                        delayBetweenAudioLoopMs;

                String selectedDelayText =
                        delayBetweenAudioLoopMs
                                + " ms";

                if (selectedRadioId != -1) {
                    RadioButton selectedRadioButton =
                            delayRadioGroup.findViewById(
                                    selectedRadioId
                            );

                    if (selectedRadioButton != null
                            && selectedRadioButton.getTag()
                            instanceof Integer) {

                        selectedDelay =
                                (Integer) selectedRadioButton.getTag();

                        selectedDelayText =
                                selectedRadioButton
                                        .getText()
                                        .toString();
                    }
                }

                // =================================================
                // LẤY VOLUME ĐÃ CHỌN
                // =================================================
                int volumePercent =
                        volumeSeekBar.getProgress();

                float selectedAudioViVolume =
                        volumePercent / 100f;

                delayBetweenAudioLoopMs =
                        selectedDelay;

                audioViVolume =
                        selectedAudioViVolume;

                // =================================================
                // LƯU SHARED PREFERENCES
                // =================================================
                getSharedPreferences(
                        PREFS_AUDIO_LOOP,
                        MODE_PRIVATE
                )
                        .edit()
                        .putInt(
                                KEY_LOOP_DELAY_MS,
                                delayBetweenAudioLoopMs
                        )
                        .putFloat(
                                KEY_AUDIO_VI_VOLUME,
                                audioViVolume
                        )
                        .apply();

                Log.d(
                        "AUDIO_SETTINGS",
                        "Saved"
                                + " | loopDelay="
                                + delayBetweenAudioLoopMs
                                + " ms"
                                + " | audioViVolume="
                                + audioViVolume
                );

                Toast.makeText(
                        this,
                        "Đã lưu: "
                                + selectedDelayText
                                + " · Audio VI "
                                + volumePercent
                                + "%",
                        Toast.LENGTH_SHORT
                ).show();

                dialog.dismiss();
            });
        });

        dialog.show();
    }


    private void playAudio(String audioFile, boolean loop, Runnable onComplete) {
        try {
            releaseCurrentMediaPlayerForPlayback();

            if (audioFile == null || audioFile.trim().isEmpty()) {
                Toast.makeText(this, "Không tìm thấy file audio", Toast.LENGTH_SHORT).show();
                if (onComplete != null) onComplete.run();
                return;
            }

            audioFile = audioFile.trim();

            MediaPlayer mp = new MediaPlayer();

            boolean ok = AudioDataSourceHelper.setDataSource(
                    this,
                    mp,
                    audioFile,
                    topicPackageManager
            );

            if (!ok) {
                mp.release();
                Toast.makeText(this, "Không tìm thấy file audio", Toast.LENGTH_SHORT).show();
                if (onComplete != null) onComplete.run();
                return;
            }

            mp.setLooping(loop);

            float volume = isMuted ? 0f : 1.0f;
            mp.setVolume(volume, volume);

            currentMediaPlayer = mp;

            mp.setOnCompletionListener(mediaPlayer -> {
                if (!loop) {
                    mediaPlayer.release();
                    if (currentMediaPlayer == mediaPlayer) {
                        currentMediaPlayer = null;
                    }
                    if (onComplete != null) onComplete.run();
                }
            });

            mp.prepare();
            mp.start();

        } catch (Exception e) {
            Toast.makeText(this, "Lỗi phát âm thanh", Toast.LENGTH_SHORT).show();

            if (currentMediaPlayer != null) {
                try {
                    currentMediaPlayer.release();
                } catch (Exception ignored) {}
                currentMediaPlayer = null;
            }

            if (onComplete != null) onComplete.run();
        }
    }

    private void playAudio(String audioFile, boolean loop) {
        playAudio(audioFile, loop, null);
    }

    private List<Sentence> getUnshownSentences() {
        boolean onlyFavorites = false;
        if (switchShowFavorites != null) {
            onlyFavorites = switchShowFavorites.isChecked();
        }
        List<Sentence> unshown = new ArrayList<>();
        for (Sentence s : sentences) {
            if (!s.shown && (!onlyFavorites || s.favorite)) {
                unshown.add(s);
            }
        }
        return unshown;
    }


    private void saveSentencesToFile() {
        if (sentences == null) return;

        try {
            String fileName = currentTopicOnLockscreen != null
                    ? currentTopicOnLockscreen.fileName
                    : null;

            // Nếu không có topic thì lưu fallback
            if (TextUtils.isEmpty(fileName)) {
                FileOutputStream fos = openFileOutput("sentences.json", MODE_PRIVATE);
                fos.write(new Gson().toJson(sentences).getBytes("UTF-8"));
                fos.close();
                return;
            }

            // ✅ Load full topic trước, tránh ghi đè topic bằng list lesson đã filter
            List<Sentence> fullList = loadSentencesForTopicPreviewOnLockScreen(currentTopicOnLockscreen);

            if (fullList == null || fullList.isEmpty()) {
                fullList = new ArrayList<>(sentences);
            } else {
                Map<String, Sentence> editedMap = new LinkedHashMap<>();

                for (Sentence s : sentences) {
                    if (s != null) {
                        editedMap.put(makeSentenceKey(s), s);
                    }
                }

                for (int i = 0; i < fullList.size(); i++) {
                    Sentence old = fullList.get(i);
                    if (old == null) continue;

                    Sentence edited = editedMap.get(makeSentenceKey(old));
                    if (edited != null) {
                        fullList.set(i, edited);
                    }
                }
            }

            File outFile = getTopicWritableFile(fileName);
            File parent = outFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            FileOutputStream fos = new FileOutputStream(outFile, false);
            fos.write(new Gson().toJson(fullList).getBytes("UTF-8"));
            fos.close();

            Log.d("DEBUG_SAVE",
                    "Saved FULL topic safely"
                            + " | file=" + outFile.getAbsolutePath()
                            + " | currentFilteredSize=" + sentences.size()
                            + " | fullSize=" + fullList.size());

        } catch (Exception e) {
            Log.e("DEBUG_SAVE", "saveSentencesToFile error", e);
            Toast.makeText(this, "Lỗi lưu file dữ liệu!", Toast.LENGTH_SHORT).show();
        }
    }

    private File getTopicWritableFile(String fileName) {
        String name = fileName != null ? fileName.trim() : "sentences.json";

        if (TextUtils.isEmpty(name)) {
            name = "sentences.json";
        }

        // Nếu fileName có dạng topics/abc.json thì lưu đúng thư mục trong internal files
        return new File(getFilesDir(), name);
    }


    private boolean safeEquals(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.trim().equals(b.trim());
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_TYPING && resultCode == Activity.RESULT_OK && data != null) {
            String typingResult = data.getStringExtra(TypingActivity.EXTRA_TYPING_RESULT);
            if ("correct".equals(typingResult) && currentSentence != null) {
                // Ví dụ: coi như user bấm Good
                applySrsRating(currentSentence, 2); // Good
                saveSentencesToFile();
                updateSrsStatsBar();

                stopAudioLoop();
                showNextSentence();
                if (currentSentence != null) {
                    updateCardViewWithSentence(currentSentence);
                    updateNowPlayingHeader();
                }
            }
        }
    }



    @Override
    protected void onDestroy() {

        // ⭐ Lesson Auto Loop chỉ tồn tại trong phiên chạy
        lessonAutoLoopEnabled = false;
        lessonAutoLoopRunning = false;

        getSharedPreferences(PREFS_APP_SETTINGS, MODE_PRIVATE)
                .edit()
                .putBoolean("lesson_auto_loop_enabled", false)
                .apply();

        MaterialButton btn = findViewById(R.id.btnLessonAutoLoop);
        if (btn != null) {
            btn.setText("🔁 Loop Lesson");
        }

        saveCurrentTopicStats();

        stopAudioLoop();
        stopExampleLoopVisualEffect();
        stopAllVoiceLoops();

        if (currentMediaPlayer != null) {
            try {
                currentMediaPlayer.stop();
            } catch (Exception ignored) {
            }

            try {
                currentMediaPlayer.release();
            } catch (Exception ignored) {
            }

            currentMediaPlayer = null;
        }

        if (topicPackageManager != null) {
            try {
                topicPackageManager.close();
            } catch (Exception ignored) {
            }
            topicPackageManager = null;
        }

        handler.removeCallbacksAndMessages(null);

        if (prefListener != null) {
            SharedPreferences prefs =
                    getSharedPreferences("settings", MODE_PRIVATE);

            prefs.unregisterOnSharedPreferenceChangeListener(prefListener);
        }

        if (isSentencesReceiverRegistered) {
            try {
                unregisterReceiver(sentencesUpdatedReceiver);
            } catch (Exception ignored) {
            }

            isSentencesReceiverRegistered = false;
        }

        saveCurrentLockscreenPosition();

        remoteLongPressHandler.removeCallbacksAndMessages(null);

        super.onDestroy();
    }


    private void updateStats() {
        // Dùng list sentences hiện tại (đã load theo topic) để tính
        List<Sentence> statsSentences = (sentences != null) ? sentences : new ArrayList<>();

        int total = statsSentences.size();
        int shown = 0, fav = 0;
        for (Sentence s : statsSentences) {
            if (s.shown) shown++;
            if (s.favorite) fav++;
        }
        int notShown = total - shown;

        float percentShown = total == 0 ? 0 : (shown * 100f / total);
        float percentNotShown = total == 0 ? 0 : (notShown * 100f / total);

        int shownColor;
        if (percentShown < 40) shownColor = Color.parseColor("#FF9800");
        else if (percentShown < 70) shownColor = Color.parseColor("#FFC107");
        else shownColor = Color.parseColor("#4CAF50");

        int notShownColor;
        if (percentNotShown < 40) notShownColor = Color.parseColor("#4CAF50");
        else if (percentNotShown < 70) notShownColor = Color.parseColor("#FFC107");
        else notShownColor = Color.parseColor("#F44336");

        int favColor = Color.parseColor("#2196F3");

        String stats = "💛: " + total + "   🔔: " + shown + "   ⏳: " + notShown + "   ⭐: " + fav;
        SpannableString spannable = new SpannableString(stats);

        highlightNumberAfterSymbol(spannable, stats, "💛: ", String.valueOf(total), Color.YELLOW);
        highlightNumberAfterSymbol(spannable, stats, "🔔: ", String.valueOf(shown), shownColor);
        highlightNumberAfterSymbol(spannable, stats, "⏳: ", String.valueOf(notShown), notShownColor);
        highlightNumberAfterSymbol(spannable, stats, "⭐: ", String.valueOf(fav), favColor);

        TextView tvStats = findViewById(R.id.tvStats);
        tvStats.setText(spannable);
    }





    private void highlightNumberAfterSymbol(SpannableString spannable, String fullText,
                                            String symbol, String number, int color) {
        int start = fullText.indexOf(symbol);
        if (start >= 0) {
            start += symbol.length();
            int end = start + number.length();
            if (end <= spannable.length()) {
                spannable.setSpan(new ForegroundColorSpan(color),
                        start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private void animateCardViewPress(androidx.cardview.widget.CardView cardView, Handler handler, int originalColor, int pressedColor) {
        cardView.setCardBackgroundColor(pressedColor);
        cardView.animate().scaleX(0.96f).scaleY(0.96f).alpha(0.8f)
                .setDuration(70)
                .withEndAction(() -> {
                    cardView.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(120).start();
                    handler.postDelayed(() -> cardView.setCardBackgroundColor(originalColor), 120);
                }).start();
    }







    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
        }
        // KHÔNG clear focus ở đây
        // searchView.clearFocus();
    }


    private void clearRipple(androidx.cardview.widget.CardView cardView) {
        // Hủy trạng thái ripple pressed/focused nếu có
        cardView.setPressed(false);
        cardView.setHovered(false);
        cardView.setActivated(false);
        cardView.clearFocus();
        //cardView.requestFocus();
        // Nếu foreground là RippleDrawable thì force state reset
        Drawable fg = cardView.getForeground();
        if (fg != null) fg.setState(new int[]{});
    }

    private void startAudioLoop() {
        if (currentSentence == null || TextUtils.isEmpty(currentSentence.audio)) {
            Toast.makeText(this, "Không có audio để lặp.", Toast.LENGTH_SHORT).show();
            isAudioLooping = false;
            return;
        }
        playAudio(currentSentence.audio, false, () -> {
            if (isAudioLooping) {
                audioLoopRunnable = () -> startAudioLoop();
                audioLoopHandler.postDelayed(audioLoopRunnable, delayBetweenAudioLoopMs);
            }
        });
    }

    private void startExampleAudioLoop() {
        if (currentSentence == null
                || TextUtils.isEmpty(currentSentence.example_audio)) {

            Toast.makeText(
                    this,
                    "Không có example audio để lặp.",
                    Toast.LENGTH_SHORT
            ).show();

            isExampleLooping = false;
            isAudioLooping = false;
            return;
        }

        // Nếu loop đã bị tắt trong lúc chờ thì không phát tiếp
        if (!isExampleLooping || !isAudioLooping) {
            return;
        }

        final String exampleFile =
                currentSentence.example_audio;

        hasPlayedExampleForCurrentSentence = true;

        Log.d(
                "EXAMPLE_LOOP",
                "Play example"
                        + " | file=" + exampleFile
                        + " | exampleLooping=" + isExampleLooping
                        + " | audioLooping=" + isAudioLooping
        );

        playAudio(exampleFile, false, () -> {
            if (!isExampleLooping || !isAudioLooping) {
                Log.d(
                        "EXAMPLE_LOOP",
                        "Loop stopped after completion"
                );
                return;
            }

            if (audioLoopRunnable != null) {
                audioLoopHandler.removeCallbacks(audioLoopRunnable);
            }

            audioLoopRunnable =
                    this::startExampleAudioLoop;

            audioLoopHandler.postDelayed(
                    audioLoopRunnable,
                    delayBetweenAudioLoopMs
            );
        });
    }

    private void toggleExampleAudioLoopFromDoubleTap() {
        if (currentSentence == null) {
            return;
        }

        if (TextUtils.isEmpty(currentSentence.example_audio)) {
            Toast.makeText(
                    this,
                    "Câu này không có example audio.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        // =========================================================
        // ĐANG LOOP EXAMPLE -> DOUBLE TAP LẦN NỮA ĐỂ TẮT
        // =========================================================
        if (isExampleLooping && isAudioLooping) {
            Log.d(
                    "EXAMPLE_DOUBLE_TAP",
                    "Example loop OFF"
            );

            isExampleLooping = false;
            isAudioLooping = false;
            isLoopingReplay = false;

            stopAudioLoop();
            stopCurrentMediaPlayerSafely();

            // Chỉ reset state, không chạy hiệu ứng màu/animation
            resetExampleLoopState();

            Toast.makeText(
                    this,
                    "Example Loop OFF",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // =========================================================
        // BẮT ĐẦU LOOP EXAMPLE
        // =========================================================
        Log.d(
                "EXAMPLE_DOUBLE_TAP",
                "Example loop ON"
                        + " | file=" + currentSentence.example_audio
        );

        // Dừng tất cả nguồn audio/loop trước đó
        isAudioLooping = false;
        isExampleLooping = false;
        isLoopingReplay = false;

        stopAudioLoop();
        stopAllVoiceLoops();
        stopCurrentMediaPlayerSafely();

        isFemaleLooping = false;
        isMaleLooping = false;
        updateVoiceLoopButtonsUi();

        // Bật cờ trước khi gọi startExampleAudioLoop()
        isExampleLooping = true;
        isAudioLooping = true;
        isLoopingReplay = false;

        hasPlayedExampleForCurrentSentence = true;

        // Hàm này tự phát lại sau khi audio kết thúc
        startExampleAudioLoop();

        Toast.makeText(
                this,
                "Example Loop ON",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void toggleAudioLoopFromUI() {
        ImageButton btnSpeaker = findViewById(R.id.btnSpeaker);

        if (!isAudioLooping) {
            // Bật loop
            isAudioLooping = true;
            if (btnSpeaker != null) {
                btnSpeaker.setImageResource(R.drawable.ic_pause);
            }
            startAudioLoop();          // loop audio chính
            startLoopVisualEffect();   // hiệu ứng card
            startEnglishBlowEffect();  // viền vàng
        } else {
            // Tắt loop
            isAudioLooping = false;
            stopAudioLoop();
            if (btnSpeaker != null) {
                btnSpeaker.setImageResource(R.drawable.ic_speaker);
                stopLoopVisualEffect();
            }
            stopEnglishBlowEffect();
            // Không auto-next nữa, chỉ dừng loop
        }
    }



    private void showSentenceByContentGlobal(String content) {
        if (TextUtils.isEmpty(content)) return;
        String keyword = content.trim().toLowerCase();

        Sentence found = null;
        for (Sentence s : globalSentences) {
            if ((s.en != null && s.en.trim().toLowerCase().contains(keyword))
                    || (s.vi != null && s.vi.trim().toLowerCase().contains(keyword))) {
                found = s;
                break;
            }
        }

        if (found == null) {
            Toast.makeText(this, "Không tìm thấy câu trong dữ liệu tổng!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Chỉ dùng để hiển thị nhanh, không động vào shown/favorite của file topic hiện tại
        currentSentence = found;
        updateCardViewWithSentence(found);
        searchView.dismissDropDown();
    }
    private void stopAudioLoop() {
        isAudioLooping = false;
        audioLoopHandler.removeCallbacksAndMessages(null);

        releaseCurrentMediaPlayerForPlayback();

        ImageButton btnSpeaker = findViewById(R.id.btnSpeaker);
        btnSpeaker.setImageResource(R.drawable.ic_speaker);
        // ⭐ Tắt luôn hiệu ứng blow EN khi dừng loop
        stopEnglishBlowEffect();
    }







    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent == null) {
            return;
        }

        // Cập nhật Intent hiện tại của Activity
        setIntent(intent);

        boolean fromNotification =
                intent.getBooleanExtra(
                        "from_notification_click",
                        false
                );

        Log.d(
                "NOTI_LOCK",
                "onNewIntent"
                        + " | fromNotification="
                        + fromNotification
                        + " | en="
                        + intent.getStringExtra("en")
                        + " | topicFile="
                        + intent.getStringExtra("topic_file")
        );

        if (fromNotification) {
            openedFromNotification = true;

            // Dừng audio/loop đang chạy của câu cũ
            stopAllLoopsAndTimers();
            stopCurrentMediaPlayerSafely();

            handleNotificationClickIntent(
                    intent
            );
        }
    }





    @Override
    protected void onResume() {
        super.onResume();

        Intent resumeIntent =
                getIntent();

        boolean resumingFromNotification =
                resumeIntent != null
                        && resumeIntent.getBooleanExtra(
                        "from_notification_click",
                        false
                );

        Log.d(
                "NOTI_LOCK",
                "onResume"
                        + " | resumingFromNotification="
                        + resumingFromNotification
                        + " | en="
                        + (
                        resumeIntent != null
                                ? resumeIntent.getStringExtra("en")
                                : null
                )
        );

        // =========================================================
        // 1. ĐÁNH DẤU APP ĐANG Ở FOREGROUND
        // =========================================================
        getSharedPreferences(
                "popup_flags",
                MODE_PRIVATE
        )
                .edit()
                .putBoolean(
                        "app_in_foreground",
                        true
                )
                .putString(
                        "current_activity",
                        getClass().getSimpleName()
                )
                .apply();

        // =========================================================
        // 2. ĐĂNG KÝ RECEIVER NẾU CHƯA ĐĂNG KÝ
        // =========================================================
        if (!isSentencesReceiverRegistered) {
            IntentFilter filter =
                    new IntentFilter(
                            "com.OPD2nd.popup.SENTENCES_UPDATED"
                    );

            if (Build.VERSION.SDK_INT
                    >= Build.VERSION_CODES.TIRAMISU) {

                registerReceiver(
                        sentencesUpdatedReceiver,
                        filter,
                        Context.RECEIVER_NOT_EXPORTED
                );

            } else {
                registerReceiver(
                        sentencesUpdatedReceiver,
                        filter
                );
            }

            isSentencesReceiverRegistered =
                    true;
        }

        // =========================================================
        // 3. ĐÓNG LEARNING MEDIA SERVICE NẾU ĐANG CHẠY
        // =========================================================
        SharedPreferences mediaPrefs =
                getSharedPreferences(
                        "learning_media_prefs",
                        MODE_PRIVATE
                );

        boolean wasMediaRunning =
                mediaPrefs.getBoolean(
                        "running",
                        false
                );

        if (wasMediaRunning) {
            LearningMediaService.requestClose(
                    this
            );

            mediaPrefs.edit()
                    .putBoolean(
                            "running",
                            false
                    )
                    .apply();
        }

        // =========================================================
        // 4. KHÔI PHỤC TOPIC / LESSON
        // =========================================================
        /*
         * Khi mở từ notification:
         * không restore topic/câu đang học cũ vì sẽ ghi đè câu
         * notification vừa được handle trong onCreate/onNewIntent.
         */
        if (!resumingFromNotification) {
            restoreLockscreenSelectionState();

            /*
             * Nếu topic thật sự thay đổi, hàm này sẽ:
             * - load topic mới;
             * - restore lesson/câu;
             * - tự nhận diện mode;
             * - áp preset.
             */
            reloadLockscreenTopicIfChanged();

            /*
             * Chặn Play Male tự phát khi Activity resume.
             */
            suppressPresetAutoAudioOnce =
                    true;

            restoreSelectedLessonIfNeeded();
            restoreCurrentLockscreenPosition();

            if (currentSentence == null
                    && sentences != null
                    && !sentences.isEmpty()) {

                currentSentence =
                        sentences.get(0);
            }

            ensureCurrentLessonState();

        } else {
            /*
             * Trong trường hợp Activity được tạo mới từ notification,
             * bảo đảm Intent được xử lý nếu currentSentence chưa được set.
             */
            if (currentSentence == null
                    && resumeIntent != null) {

                handleNotificationClickIntent(
                        resumeIntent
                );
            }
        }

        // =========================================================
        // 5. HIỂN THỊ CÂU HIỆN TẠI
        // =========================================================
        if (currentSentence != null) {
            currentAudioEnFile =
                    currentSentence.audio;

            currentAudioViFile =
                    currentSentence.audio_vi;

            /*
             * Khi mở từ notification, không tự phát Male hoặc audio
             * bất ngờ trong lúc refresh UI.
             */
            if (resumingFromNotification) {
                suppressPresetAutoAudioOnce =
                        true;
            }

            updateCardViewWithSentence(
                    currentSentence
            );

            updateNowPlayingHeader();

            updateNowPlayingInfo(
                    currentSentence
            );

            syncSequentialIndexWithCurrentSentence();

            /*
             * Luôn lưu vị trí hiện tại.
             *
             * Khi mở từ notification, handleNotificationClickIntent()
             * đã chuyển hẳn ngữ cảnh học sang topic và câu notification,
             * vì vậy câu này phải được xem là vị trí học thật.
             */
            saveCurrentLockscreenPosition();
        }

        updateStats();
        updateSrsStatsBar();

        // =========================================================
        // 6. ĐỒNG BỘ PLAY EN
        // =========================================================
        SharedPreferences appPrefs =
                getSharedPreferences(
                        PREFS_APP_SETTINGS,
                        MODE_PRIVATE
                );

        boolean currentEn =
                appPrefs.getBoolean(
                        KEY_PLAY_EN_AUDIO_ON_NEXT,
                        false
                );

        autoPlayOnNext =
                currentEn;

        if (switchAutoPlayOnNext != null
                && switchAutoPlayOnNext.isChecked() != currentEn) {

            switchAutoPlayOnNext.setOnCheckedChangeListener(
                    null
            );

            switchAutoPlayOnNext.setChecked(
                    currentEn
            );

            switchAutoPlayOnNext.setOnCheckedChangeListener(
                    (buttonView, isChecked) -> {
                        autoPlayOnNext =
                                isChecked;

                        appPrefs.edit()
                                .putBoolean(
                                        KEY_PLAY_EN_AUDIO_ON_NEXT,
                                        isChecked
                                )
                                .apply();
                    }
            );
        }

        // =========================================================
        // 7. ĐỒNG BỘ PLAY VI
        // =========================================================
        boolean currentVi =
                appPrefs.getBoolean(
                        KEY_PLAY_VI_AUDIO,
                        true
                );

        if (switchPlayViOnNext != null
                && switchPlayViOnNext.isChecked() != currentVi) {

            switchPlayViOnNext.setOnCheckedChangeListener(
                    null
            );

            switchPlayViOnNext.setChecked(
                    currentVi
            );

            switchPlayViOnNext.setOnCheckedChangeListener(
                    (buttonView, isChecked) -> {
                        appPrefs.edit()
                                .putBoolean(
                                        KEY_PLAY_VI_AUDIO,
                                        isChecked
                                )
                                .apply();

                        Intent syncIntent =
                                new Intent(
                                        "com.OPD2nd.popup.SETTINGS_CHANGED"
                                );

                        syncIntent.putExtra(
                                "key",
                                KEY_PLAY_VI_AUDIO
                        );

                        syncIntent.putExtra(
                                "value",
                                isChecked
                        );

                        sendBroadcast(
                                syncIntent
                        );
                    }
            );
        }

        // =========================================================
        // 8. RESET / MIGRATE DỮ LIỆU HÀNG NGÀY
        // =========================================================
        resetDailyCountersIfNeeded();

        migrateOldQuotaData();
        migrateOldCounterData();

        // =========================================================
        // 9. XÓA FLAG NOTIFICATION SAU KHI ĐÃ HIỂN THỊ
        // =========================================================
        /*
         * Xóa flag để các lần onPause/onResume tiếp theo không tiếp tục
         * bị xem là một lần mở mới từ notification.
         */
        if (resumingFromNotification
                && resumeIntent != null) {

            resumeIntent.removeExtra(
                    "from_notification_click"
            );

            setIntent(
                    resumeIntent
            );

            openedFromNotification =
                    false;

            Log.d(
                    "NOTI_LOCK",
                    "Notification resume flag cleared"
            );
        }
    }


    private void syncSequentialIndexWithCurrentSentence() {
        if (sentences == null || sentences.isEmpty()) {
            listeningSequentialIndex = 0;
            return;
        }

        if (currentSentence == null) {
            listeningSequentialIndex = 0;
            currentSentence = sentences.get(0);
            return;
        }

        String curKey = makeSentenceKey(currentSentence);

        for (int i = 0; i < sentences.size(); i++) {
            Sentence s = sentences.get(i);

            if (s == currentSentence
                    || (s != null && makeSentenceKey(s).equals(curKey))) {
                listeningSequentialIndex = i;
                Log.d("LISTENING_SEQ", "sync index after resume = " + i);
                return;
            }
        }

        listeningSequentialIndex = 0;
    }


    /**
     * ✅ Migrate daily counters từ "locks_settings" sang PREFS_DAILY_COUNTERS
     * Chỉ chạy 1 lần duy nhất
     */

    private void migrateOldCounterData() {
        SharedPreferences newCounterPrefs = getSharedPreferences(PREFS_DAILY_COUNTERS, MODE_PRIVATE);

        // ✅ NẾU ĐÃ MIGRATE RỒI THÌ BỎ QUA
        if (newCounterPrefs.getBoolean("migrated_counters", false)) {
            return;
        }

        // ✅ ĐỌC DỮ LIỆU CŨ TỪ "locks_settings"
        SharedPreferences oldPrefs = getSharedPreferences("locks_settings", MODE_PRIVATE);
        int oldNewDone = oldPrefs.getInt("today_new_done", 0);
        int oldReviewDone = oldPrefs.getInt("today_review_done", 0);

        // ✅ COPY SANG PREFS MỚI
        newCounterPrefs.edit()
                .putInt(KEY_NEW_COUNT, oldNewDone)
                .putInt(KEY_REVIEW_COUNT, oldReviewDone)
                .putBoolean("migrated_counters", true)
                .apply();

        Log.d("MIGRATE", "Migrated counters: New=" + oldNewDone + " Review=" + oldReviewDone);

        // ✅ THÔNG BÁO (TÙY CHỌN)
        if (oldNewDone > 0 || oldReviewDone > 0) {
            Toast.makeText(this,
                    "✅ Đã đồng bộ tiến độ hôm nay\nNew: " + oldNewDone + " | Review: " + oldReviewDone,
                    Toast.LENGTH_SHORT).show();
        }
    }




    private void playQuotaReachedSound() {
        try {
            MediaPlayer mp = MediaPlayer.create(this, R.raw.quota_done);
            if (mp == null) {
                Log.w("QUOTA_SOUND", "MediaPlayer.create trả về null");
                return;
            }

            // ⭐ Giảm volume: 0.3f = 30% so với mức volume media hiện tại
            float volume = 0.3f;
            mp.setVolume(volume, volume);

            mp.setOnCompletionListener(player -> player.release());
            mp.setOnErrorListener((player, what, extra) -> {
                player.release();
                return true;
            });

            mp.start();
            Log.d("QUOTA_SOUND", "playQuotaReachedSound: started với volume=" + volume);
        } catch (Exception e) {
            Log.e("QUOTA_SOUND", "playQuotaReachedSound: error", e);
        }
    }

    private void migrateOldQuotaData() {
        SharedPreferences oldPrefs = getSharedPreferences("locks_settings", MODE_PRIVATE);
        SharedPreferences newPrefs = getSharedPreferences(PREFS_APP_SETTINGS, MODE_PRIVATE);

        // ✅ NẾU ĐÃ MIGRATE RỒI THÌ BỎ QUA
        if (newPrefs.getBoolean("migrated_quota", false)) {
            return;
        }

        // ✅ COPY DỮ LIỆU CŨ SANG MỚI
        int oldNewLimit = oldPrefs.getInt("daily_new_limit", -1);
        int oldReviewLimit = oldPrefs.getInt("daily_review_limit", -1);

        SharedPreferences.Editor editor = newPrefs.edit();

        if (oldNewLimit >= 0) {
            editor.putInt(KEY_QUOTA_NEW, oldNewLimit);
            Log.d("MIGRATE", "Migrated daily_new_limit: " + oldNewLimit);
        } else {
            // ✅ KHÔNG CÓ DỮ LIỆU CŨ → SET DEFAULT
            editor.putInt(KEY_QUOTA_NEW, 50);
            Log.d("MIGRATE", "No old new_limit, set default: 50");
        }

        if (oldReviewLimit >= 0) {
            editor.putInt(KEY_QUOTA_REVIEW, oldReviewLimit);
            Log.d("MIGRATE", "Migrated daily_review_limit: " + oldReviewLimit);
        } else {
            // ✅ KHÔNG CÓ DỮ LIỆU CŨ → SET DEFAULT
            editor.putInt(KEY_QUOTA_REVIEW, 100);
            Log.d("MIGRATE", "No old review_limit, set default: 100");
        }

        editor.putBoolean("migrated_quota", true);
        editor.apply();

        Log.d("MIGRATE", "✅ Quota migration completed!");
    }




    /**
     * Hiển thị hint hướng dẫn long press stats bar (chỉ 1 lần đầu)
     */
    private void showQuotaHintIfNeeded() {
        SharedPreferences prefs = getSharedPreferences("locks_settings", MODE_PRIVATE);
        boolean hasShownHint = prefs.getBoolean("quota_hint_shown", false);

        if (!hasShownHint) {
            // Delay 2 giây để user kịp nhìn UI
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                TextView tvSrsStatsQuota = findViewById(R.id.tvSrsStatsQuota);
                if (tvSrsStatsQuota != null && tvSrsStatsQuota.isAttachedToWindow()) {
                    // Tạo hiệu ứng nhấp nháy
                    tvSrsStatsQuota.animate()
                            .alpha(0.3f)
                            .setDuration(500)
                            .withEndAction(() -> {
                                tvSrsStatsQuota.animate()
                                        .alpha(1.0f)
                                        .setDuration(500)
                                        .start();
                            })
                            .start();

                    // Hiển thị toast hint
                    Toast.makeText(this,
                            "💡 Tip: Giữ lâu hàng New/Review để cài đặt quota",
                            Toast.LENGTH_LONG).show();
                }

                // Lưu đã hiển thị hint
                prefs.edit().putBoolean("quota_hint_shown", true).apply();

            }, 2000);
        }
    }

    private void showQuotaSettingsDialog() {
        // ✅ LƯU currentSentence TRƯỚC KHI SHOW DIALOG
        final Sentence lastSentence = currentSentence;

        if (currentTopicOnLockscreen == null) {
            Toast.makeText(this, "❌ Không có topic nào đang mở!", Toast.LENGTH_SHORT).show();
            return;
        }

        String topicId = currentTopicOnLockscreen.id;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_quota_settings, null);

        EditText etNewLimit = dialogView.findViewById(R.id.etDailyNewLimit);
        EditText etReviewLimit = dialogView.findViewById(R.id.etDailyReviewLimit);
        TextView tvCurrentNew = dialogView.findViewById(R.id.tvCurrentNewDone);
        TextView tvCurrentReview = dialogView.findViewById(R.id.tvCurrentReviewDone);

        // ✅✅✅ ĐỌC TỪ BIẾN PER-TOPIC (ĐÃ RESTORE TỪ TOPIC_STATS) ✅✅✅
        int dailyNewLimit = currentTopicNewLimit;
        int dailyReviewLimit = currentTopicReviewLimit;
        int todayNewDone = currentTopicNewDone;
        int todayReviewDone = currentTopicReviewDone;

        etNewLimit.setText(String.valueOf(dailyNewLimit));
        etReviewLimit.setText(String.valueOf(dailyReviewLimit));

        float newPercent = dailyNewLimit > 0 ? (todayNewDone * 100f / dailyNewLimit) : 0;
        float reviewPercent = dailyReviewLimit > 0 ? (todayReviewDone * 100f / dailyReviewLimit) : 0;

        int newColor = newPercent >= 100 ? Color.parseColor("#4CAF50") :
                newPercent >= 70 ? Color.parseColor("#FFC107") :
                        Color.parseColor("#FF9800");
        int reviewColor = reviewPercent >= 100 ? Color.parseColor("#4CAF50") :
                reviewPercent >= 70 ? Color.parseColor("#FFC107") :
                        Color.parseColor("#FF9800");

        String newText = "Đã học hôm nay: " + todayNewDone + "/" + dailyNewLimit
                + " (" + Math.round(newPercent) + "%)";
        String reviewText = "Đã ôn hôm nay: " + todayReviewDone + "/" + dailyReviewLimit
                + " (" + Math.round(reviewPercent) + "%)";

        SpannableString newSpan = new SpannableString(newText);
        newSpan.setSpan(new ForegroundColorSpan(newColor),
                15, 15 + String.valueOf(todayNewDone).length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        SpannableString reviewSpan = new SpannableString(reviewText);
        reviewSpan.setSpan(new ForegroundColorSpan(reviewColor),
                13, 13 + String.valueOf(todayReviewDone).length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvCurrentNew.setText(newSpan);
        tvCurrentReview.setText(reviewSpan);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("⚙️ Cài đặt quota học tập - " + currentTopicOnLockscreen.name)
                .setView(dialogView)
                .setPositiveButton("💾 Lưu", null)
                .setNegativeButton("✖️ Hủy", null)
                .create();

        dialog.show();

        Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        btnSave.setOnClickListener(v -> {
            try {
                String newStr = etNewLimit.getText().toString().trim();
                String reviewStr = etReviewLimit.getText().toString().trim();

                if (newStr.isEmpty() || reviewStr.isEmpty()) {
                    Toast.makeText(this, "⚠️ Vui lòng nhập đầy đủ cả 2 giá trị!",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                int newLimit = Integer.parseInt(newStr);
                int reviewLimit = Integer.parseInt(reviewStr);

                if (newLimit < 0) {
                    etNewLimit.setError("Không được âm!");
                    etNewLimit.requestFocus();
                    return;
                }
                if (reviewLimit < 0) {
                    etReviewLimit.setError("Không được âm!");
                    etReviewLimit.requestFocus();
                    return;
                }
                if (newLimit > 9999) {
                    etNewLimit.setError("Tối đa 9999!");
                    etNewLimit.requestFocus();
                    return;
                }
                if (reviewLimit > 9999) {
                    etReviewLimit.setError("Tối đa 9999!");
                    etReviewLimit.requestFocus();
                    return;
                }

                // ✅✅✅ LƯU QUOTA MỚI VÀO TOPIC_STATS ✅✅✅
                SharedPreferences prefs = getSharedPreferences("TOPIC_STATS", MODE_PRIVATE);
                String prefix = "topic_" + topicId + "_";

                prefs.edit()
                        .putInt(prefix + "newLimit", newLimit)
                        .putInt(prefix + "reviewLimit", reviewLimit)
                        .apply();

                Log.d("QUOTA_SAVE", "Saved to TOPIC_STATS for topic " + topicId + ": " +
                        "newLimit=" + newLimit + ", reviewLimit=" + reviewLimit);

                // ✅✅✅ CẬP NHẬT BIẾN PER-TOPIC NGAY LẬP TỨC ✅✅✅
                currentTopicNewLimit = newLimit;
                currentTopicReviewLimit = reviewLimit;

                // ✅ CẬP NHẬT STATS BAR (VẼ LẠI UI NGAY)
                updateSrsStatsBar();

                // ✅ THÔNG BÁO
                String message = "✅ Đã cập nhật quota cho topic \"" + currentTopicOnLockscreen.name + "\":\n" +
                        "• New: " + newLimit + " câu/ngày\n" +
                        "• Review: " + reviewLimit + " câu/ngày";

                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

                // ✅ ĐÓNG DIALOG
                dialog.dismiss();

                // ✅ NẾU CẦN RELOAD CÂU (TUỲ CHỌN, CÓ THỂ BỎ NẾU CHỈ CẬP NHẬT STATS)
                // showSentence(false);

            } catch (NumberFormatException e) {
                Toast.makeText(this, "⚠️ Vui lòng nhập số nguyên hợp lệ!",
                        Toast.LENGTH_SHORT).show();
            }
        });

        Button btnCancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        btnCancel.setOnClickListener(v -> {
            // ✅ LOAD LẠI IMAGE KHI BẤM "HỦY"
            if (lastSentence != null) {
                currentSentence = lastSentence;
                updateCardViewWithSentence(lastSentence);
                Log.d("QUOTA_DIALOG", "✅ Reloaded image after cancel quota settings");
            }
            dialog.dismiss();
        });
    }




    @Override
    protected void onPause() {
        super.onPause();

        // Dừng timer không tương tác
        cancelInactivityTimer();

        // Lưu tiến độ và vị trí hiện tại
        saveCurrentTopicStats();
        saveCurrentLockscreenPosition();

        // Lưu đúng topic/lesson đang chọn
        saveLockscreenSelectionState(
                lockCurrentSelectionLabel,
                lockCurrentLessonKeys,
                lockCurrentGroupName,
                lockCurrentSectionName
        );

        // Hủy receiver và đồng bộ lại cờ đăng ký
        if (isSentencesReceiverRegistered) {
            try {
                unregisterReceiver(sentencesUpdatedReceiver);
            } catch (IllegalArgumentException e) {
                Log.w(
                        "LOCK_RECEIVER",
                        "sentencesUpdatedReceiver chưa được đăng ký hoặc đã bị hủy trước đó",
                        e
                );
            } catch (Exception e) {
                Log.e(
                        "LOCK_RECEIVER",
                        "Lỗi unregister sentencesUpdatedReceiver",
                        e
                );
            } finally {
                isSentencesReceiverRegistered = false;
            }
        }

        // Đánh dấu app không còn ở foreground
        getSharedPreferences("popup_flags", MODE_PRIVATE)
                .edit()
                .putBoolean("app_in_foreground", false)
                .putString("current_activity", "")
                .apply();

        Log.d(
                "LOCK_PAUSE",
                "onPause saved state"
                        + " | topic="
                        + (currentTopicOnLockscreen != null
                        ? currentTopicOnLockscreen.id
                        : "null")
                        + " | label=" + lockCurrentSelectionLabel
                        + " | group=" + lockCurrentGroupName
                        + " | section=" + lockCurrentSectionName
                        + " | lessonKeys=" + lockCurrentLessonKeys
                        + " | receiverRegistered="
                        + isSentencesReceiverRegistered
        );
    }

    private String makeSentenceKey(Sentence s) {
        if (s == null) return "";

        String en = s.en == null ? "" : s.en.trim().toLowerCase();
        String vi = s.vi == null ? "" : s.vi.trim().toLowerCase();
        String ipa = s.ipa == null ? "" : s.ipa.trim().toLowerCase();
        String img = s.image == null ? "" : s.image.trim().toLowerCase();

        // Kết hợp 4 trường để giảm tối đa trùng key
        return en + "|" + vi + "|" + ipa + "|" + img;
    }

    // Helper tìm Sentence từ key
    private Sentence findSentenceByKey(String key) {
        for (Sentence s : sentences) {
            if (makeSentenceKey(s).equals(key)) return s;
        }
        return null;
    }

    private void importFromAssets(String filename) {
        try (InputStream is = getAssets().open(filename)) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[4096];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            byte[] fileBytes = buffer.toByteArray();
            importSentences(fileBytes, filename, null);
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi import file từ assets", Toast.LENGTH_SHORT).show();
        }
    }



    private boolean isJsonFile(Uri uri) {
        String name = "";
        if ("content".equals(uri.getScheme())) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = cursor.getString(idx);
                cursor.close();
            }
        } else if ("file".equals(uri.getScheme())) {
            name = new File(uri.getPath()).getName();
        }
        return name.endsWith(".json");
    }

    // Đặt cuối class LockScreenActivity
    public static String doubleDecrypt(byte[] fileBytes, String password) throws Exception {
        // Tách salt, iv2, iv1
        byte[] salt = java.util.Arrays.copyOfRange(fileBytes, 0, 16);
        byte[] iv2 = java.util.Arrays.copyOfRange(fileBytes, 16, 32);
        byte[] iv1 = java.util.Arrays.copyOfRange(fileBytes, 32, 48);
        byte[] enc2 = java.util.Arrays.copyOfRange(fileBytes, 48, fileBytes.length);

        // Giải mã lớp 2 (mật khẩu người dùng nhập)
        javax.crypto.SecretKeyFactory factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        java.security.spec.KeySpec spec = new javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, 100_000, 256);
        byte[] key2 = factory.generateSecret(spec).getEncoded();
        javax.crypto.spec.SecretKeySpec key2spec = new javax.crypto.spec.SecretKeySpec(key2, "AES");
        javax.crypto.Cipher cipher2 = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher2.init(javax.crypto.Cipher.DECRYPT_MODE, key2spec, new javax.crypto.spec.IvParameterSpec(iv2));
        byte[] enc1 = cipher2.doFinal(enc2);

        // Giải mã lớp 1 (key hardcode)
        byte[] key1 = "MyOPD2ndSuperHardKey1011".getBytes("UTF-8");
        javax.crypto.spec.SecretKeySpec key1spec = new javax.crypto.spec.SecretKeySpec(key1, "AES");
        javax.crypto.Cipher cipher1 = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher1.init(javax.crypto.Cipher.DECRYPT_MODE, key1spec, new javax.crypto.spec.IvParameterSpec(iv1));
        byte[] plain = cipher1.doFinal(enc1);

        return new String(plain, "UTF-8");
    }

    private void importSentences(byte[] fileBytes, String fileName, @Nullable Runnable afterImport) {
        boolean isJson = fileName.endsWith(".json");
        if (isJson) {
            importJsonDirectly(fileBytes, afterImport);
        } else {
            String savedPassword = getDecryptedPassword(fileName);
            if (savedPassword != null) {
                try {
                    String json = doubleDecrypt(fileBytes, savedPassword);
                    List<Sentence> importList = new Gson().fromJson(json, new TypeToken<List<Sentence>>() {}.getType());
                    Log.d("DEBUG_LIST", "Load file, size loaded: " + (importList == null ? -1 : importList.size()));
                    if (importList != null && !importList.isEmpty()) {
                        sentences.clear();
                        sentences.addAll(importList);
                        Log.d("DEBUG_IMPORT", "Import xong, sentences.size=" + sentences.size());
                        saveSentencesToFile();
                        updateUIAfterImport();
                        Toast.makeText(this, "Đã import thành công!", Toast.LENGTH_SHORT).show();
                        if (afterImport != null) afterImport.run();
                        return;
                    } else {
                        Toast.makeText(this, "Sai mật khẩu hoặc file rỗng!", Toast.LENGTH_SHORT).show();
                        // Nếu sai thì xóa password đã lưu để hỏi lại lần sau
                        SharedPreferences prefs = getSharedPreferences("password_cache", MODE_PRIVATE);
                        prefs.edit().remove(fileName).apply();
                    }
                } catch (Exception e) {
                    SharedPreferences prefs = getSharedPreferences("password_cache", MODE_PRIVATE);
                    prefs.edit().remove(fileName).apply();
                    askPasswordAndImportEnc(fileBytes, fileName, afterImport);
                    return;
                }
            }
            // Nếu chưa có hoặc sai thì hỏi mật khẩu
            askPasswordAndImportEnc(fileBytes, fileName, afterImport);
        }
    }

    private void importJsonDirectly(byte[] fileBytes, @Nullable Runnable afterImport) {
        try {
            String json = new String(fileBytes, "UTF-8");
            List<Sentence> importList = new Gson().fromJson(json, new TypeToken<List<Sentence>>() {}.getType());
            Log.d("DEBUG_LIST", "Load file, size loaded: " + (importList == null ? -1 : importList.size()));
            if (importList != null && !importList.isEmpty()) {
                sentences.clear();
                sentences.addAll(importList);
                saveSentencesToFile();
                updateUIAfterImport();
                Toast.makeText(this, "Đã import thành công!", Toast.LENGTH_SHORT).show();
                if (afterImport != null) afterImport.run();
            } else {
                Toast.makeText(this, "File không hợp lệ hoặc rỗng.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi đọc file JSON!", Toast.LENGTH_SHORT).show();
        }
    }

    private void askPasswordAndImportEnc(byte[] fileBytes, String fileName, @Nullable Runnable afterImport) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_password_input, null);
        EditText etPassword = dialogView.findViewById(R.id.editTextPassword);

        final MediaPlayer[] mediaPlayer = new MediaPlayer[1];

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Nhập mật khẩu giải mã")
                .setView(dialogView)
                .setPositiveButton("OK", (d, which) -> {
                    String password = etPassword.getText().toString();
                    try {
                        String json = doubleDecrypt(fileBytes, password);
                        List<Sentence> importList = new Gson().fromJson(json, new TypeToken<List<Sentence>>() {}.getType());
                        Log.d("DEBUG_LIST", "Load file, size loaded: " + (importList == null ? -1 : importList.size()));
                        if (importList != null && !importList.isEmpty()) {
                            sentences.clear();
                            sentences.addAll(importList);
                            saveSentencesToFile();
                            updateUIAfterImport();
                            saveEncryptedPassword(fileName, password); // Lưu mật khẩu đã nhập (mã hóa)
                            Toast.makeText(this, "Đã import thành công!", Toast.LENGTH_SHORT).show();
                            if (afterImport != null) afterImport.run();
                        } else {
                            Toast.makeText(this, "Sai mật khẩu hoặc file rỗng!", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Sai mật khẩu hoặc file lỗi!", Toast.LENGTH_SHORT).show();
                    }
                    // Giải phóng audio khi bấm OK
                    if (mediaPlayer[0] != null) {
                        mediaPlayer[0].stop();
                        mediaPlayer[0].release();
                        mediaPlayer[0] = null;
                    }
                })
                .setNegativeButton("Huỷ", (d, which) -> {
                    if (mediaPlayer[0] != null) {
                        mediaPlayer[0].stop();
                        mediaPlayer[0].release();
                        mediaPlayer[0] = null;
                    }
                })
                .create();

        dialog.setOnShowListener(d -> {
            mediaPlayer[0] = MediaPlayer.create(this, R.raw.contact);
            if (mediaPlayer[0] != null) mediaPlayer[0].start();
        });
        dialog.setOnDismissListener(d -> {
            if (mediaPlayer[0] != null) {
                if (mediaPlayer[0].isPlaying()) mediaPlayer[0].stop();
                mediaPlayer[0].release();
                mediaPlayer[0] = null;
            }
        });

        dialog.show();
    }

    private void updateUIAfterImport() {
        shownHistoryKeys.clear();
        shownHistoryIdx = -1;

        // ⭐ Rebuild pool new dựa trên sentences mới import
        rebuildTodayNewPool();

        // Sau đó mới chọn câu tiếp theo theo SRS
        showSentence(false);

        updateStats();
        updateSuggestionList();
        if (autoAdapter != null) autoAdapter.notifyDataSetChanged();
    }

    private String getFileNameFromUri(Uri uri) {
        String name = "";
        if ("content".equals(uri.getScheme())) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = cursor.getString(idx);
                cursor.close();
            }
        } else if ("file".equals(uri.getScheme())) {
            name = new File(uri.getPath()).getName();
        }
        return name;
    }

    private static final String SECRET_KEY = "SuperSecretKey11"; // Nên để độ dài >=16 ký tự

    private String encrypt(String plainText) {
        try {
            javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(SECRET_KEY.getBytes("UTF-8"), "AES");
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES");
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));
            return Base64.encodeToString(encrypted, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String decrypt(String encryptedText) {
        try {
            javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(SECRET_KEY.getBytes("UTF-8"), "AES");
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES");
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec);
            byte[] decoded = Base64.decode(encryptedText, Base64.DEFAULT);
            byte[] original = cipher.doFinal(decoded);
            return new String(original, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void saveEncryptedPassword(String fileName, String password) {
        Log.d("LockScreen", "Save password for fileName: " + fileName);
        if (fileName == null) return;
        fileName = fileName.toLowerCase();
        SharedPreferences prefs = getSharedPreferences("password_cache", MODE_PRIVATE);
        String encryptedPwd = encrypt(password);
        prefs.edit().putString(fileName, encryptedPwd).apply();
    }

    private String getDecryptedPassword(String fileName) {
        Log.d("LockScreen", "Save password for fileName: " + fileName);
        if (fileName == null) return null;
        fileName = fileName.toLowerCase();
        SharedPreferences prefs = getSharedPreferences("password_cache", MODE_PRIVATE);
        String encryptedPwd = prefs.getString(fileName, null);
        if (encryptedPwd == null) return null;
        return decrypt(encryptedPwd);
    }


    /**
     * ⭐ TẢI TRƯỚC ẢNH CÂU TIẾP THEO VÀO CACHE
     */
    private void preloadNextImage() {
        try {
            int nextIdx = shownHistoryIdx + 1;

            Sentence next = null;

            if (nextIdx < historyCache.size()) {
                next = historyCache.get(nextIdx);
            } else if (nextIdx < shownHistoryKeys.size()) {
                String key = shownHistoryKeys.get(nextIdx);
                next = findSentenceByKey(key);
            }

            if (next == null) {
                SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
                boolean favoritesOnly = prefs.getBoolean("locks_favorites_only", false);

                if (favoritesOnly) {
                    List<Sentence> favList = new ArrayList<>();
                    for (Sentence s : sentences) {
                        if (s.favorite && !s.favoriteShown) {
                            favList.add(s);
                        }
                    }

                    if (!favList.isEmpty()) {
                        next = favList.get(0);
                    }

                } else {
                    long now = System.currentTimeMillis();
                    List<Sentence> dueList = new ArrayList<>();

                    for (Sentence s : sentences) {
                        if (s.srsDueTime <= now) {
                            dueList.add(s);
                        }
                    }

                    if (!dueList.isEmpty()) {
                        dueList.sort((a, b) -> {
                            if (a.srsReps == 0 && b.srsReps > 0) return -1;
                            if (b.srsReps == 0 && a.srsReps > 0) return 1;
                            return Long.compare(a.srsDueTime, b.srsDueTime);
                        });

                        if (dueList.size() > 1) {
                            next = dueList.get(1);
                        } else {
                            next = dueList.get(0);
                        }
                    }
                }
            }

            if (next != null && next.image != null && !next.image.trim().isEmpty()) {
                File imageFile = getImageFile(next.image);

                if (imageFile != null && imageFile.exists() && imageFile.length() > 0) {
                    Glide.with(this)
                            .load(imageFile)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .preload(800, 800);

                    Log.d("PRELOAD", "✅ Preloaded image from file/package: " + next.image);
                } else {
                    Log.w("PRELOAD", "⚠ Failed to preload image: " + next.image);
                }
            } else {
                Log.d("PRELOAD", "⏭ No next image to preload");
            }

        } catch (Exception e) {
            Log.e("PRELOAD", "❌ Error preloading image", e);
        }
    }

    /**
     * ⭐ KHUYẾN NGHỊ - CÂN BẰNG HOÀN HẢO
     */
    private void showSentenceWithLayoutScaleTransition(boolean isPrev) {
        View cardSentence = findViewById(R.id.cardSentence);

        if (cardSentence == null) {
            isImageAnimating = false;
            showSentence(isPrev);

            // ❌ BỎ
            // if (isPrev) {
            //     revealFullAnswerAndStartLoop();
            // }

            return;
        }

        cardSentence.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        cardSentence.setPivotX(cardSentence.getWidth() / 2f);
        cardSentence.setPivotY(cardSentence.getHeight() / 2f);

        cardSentence.animate()
                .scaleX(0.85f)
                .scaleY(0.85f)
                .setDuration(120)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .withEndAction(() -> {

                    isImageAnimating = false;
                    showSentence(isPrev);

                    // ❌ BỎ
                    // if (isPrev) {
                    //     revealFullAnswerAndStartLoop();
                    // }

                    cardSentence.setScaleX(0.85f);
                    cardSentence.setScaleY(0.85f);

                    cardSentence.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(150)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f))
                            .withEndAction(() -> {
                                cardSentence.setLayerType(View.LAYER_TYPE_NONE, null);
                            })
                            .start();
                })
                .start();
    }




    /**
     * Lưu quota từ EditText vào SharedPreferences và update UI
     */
    void saveQuotaFromEditText(EditText editText, String key, int defaultValue) {
        try {
            String text = editText.getText().toString().trim();

            Log.d("QuotaEdit", "=== saveQuotaFromEditText ===");
            Log.d("QuotaEdit", "Key: " + key);
            Log.d("QuotaEdit", "Text nhập: " + text);

            if (text.isEmpty()) {
                editText.setText(String.valueOf(defaultValue));
                return;
            }

            int value = Integer.parseInt(text);

            // ✅ VALIDATE
            if (value < 0) {
                Toast.makeText(this, "❌ Quota không được âm!", Toast.LENGTH_SHORT).show();
                editText.setText(String.valueOf(defaultValue));
                return;
            }
            if (value > 9999) {
                Toast.makeText(this, "❌ Quota tối đa 9999!", Toast.LENGTH_SHORT).show();
                editText.setText("9999");
                value = 9999;
            }



            // ✅ LƯU VÀO ĐÚNG PREFS (DÙNG PREFS_APP_SETTINGS)
            SharedPreferences prefs = getSharedPreferences(PREFS_APP_SETTINGS, MODE_PRIVATE);
            int oldValue = prefs.getInt(key, defaultValue);

            Log.d("QuotaEdit", "Giá trị cũ: " + oldValue);
            Log.d("QuotaEdit", "Giá trị mới: " + value);

            if (value == oldValue) {
                Log.d("QuotaEdit", "Giá trị không đổi, bỏ qua");
                return;
            }

            prefs.edit().putInt(key, value).apply();

            Log.d("QuotaEdit", "Đã lưu vào " + PREFS_APP_SETTINGS + " key=" + key + " value=" + value);

            // ✅ UPDATE STATS BAR
            updateSrsStatsBar();

            // ✅ THÔNG BÁO
            String name = key.equals(KEY_QUOTA_NEW) ? "New" : "Review";
            Toast.makeText(this, "✅ " + name + ": " + value + " câu/ngày", Toast.LENGTH_SHORT).show();

        } catch (NumberFormatException e) {
            Log.e("QuotaEdit", "Lỗi parse số: " + e.getMessage());
            Toast.makeText(this, "❌ Vui lòng nhập số!", Toast.LENGTH_SHORT).show();
            editText.setText(String.valueOf(defaultValue));
        }
    }


    private void scheduleDailyReminder() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        // ✅ TEST: ALARM 1 SAU 1 PHÚT
        Calendar cal1 = Calendar.getInstance();
        cal1.add(Calendar.MINUTE, 1);
        scheduleAlarmAt(alarmManager, cal1, 1001);

        // ✅ TEST: ALARM 2 SAU 2 PHÚT
        Calendar cal2 = Calendar.getInstance();
        cal2.add(Calendar.MINUTE, 2);
        scheduleAlarmAt(alarmManager, cal2, 1002);

        Log.d("REMINDER", "✅ Test alarms scheduled at +1 min and +2 min");
    }

    private void scheduleAlarmAt(AlarmManager alarmManager, Calendar calendar, int requestCode) {
        Intent intent = new Intent(this, DailyReminderReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );

        Log.d("REMINDER", "✅ Alarm scheduled at " + calendar.getTime() + " (requestCode=" + requestCode + ")");
    }


    private List<Sentence> loadSentencesFromFile(String fileName) {
        try {
            File f = new File(getFilesDir(), fileName);
            InputStream is;
            if (f.exists()) {
                is = openFileInput(fileName);
            } else {
                // Fallback asset
                is = getAssets().open(fileName);
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                json.append(line);
            }
            br.close();
            is.close();

            List<Sentence> list = new Gson().fromJson(
                    json.toString(),
                    new TypeToken<List<Sentence>>() {}.getType()
            );
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }


    private void handleNotificationClickIntent(
            Intent intent
    ) {
        if (intent == null) {
            return;
        }

        // =========================================================
        // 1. ĐỌC DỮ LIỆU TỪ NOTIFICATION
        // =========================================================
        String en =
                intent.getStringExtra(
                        "en"
                );

        String vi =
                intent.getStringExtra(
                        "vi"
                );

        String ipa =
                intent.getStringExtra(
                        "ipa"
                );

        String image =
                intent.getStringExtra(
                        "image_name"
                );

        String audio =
                intent.getStringExtra(
                        "audio_file"
                );

        String topicFile =
                intent.getStringExtra(
                        "topic_file"
                );

        if (TextUtils.isEmpty(en)) {
            Log.w(
                    "NOTI_LOCK",
                    "Notification intent has no EN text"
            );

            return;
        }

        if (TextUtils.isEmpty(topicFile)) {
            topicFile =
                    "sentences.json";
        }

        final String finalTopicFile =
                topicFile.trim();

        Log.d(
                "NOTI_LOCK",
                "handleNotificationClickIntent START"
                        + " | topicFile=" + finalTopicFile
                        + " | en=" + en
                        + " | vi=" + vi
                        + " | ipa=" + ipa
                        + " | image=" + image
                        + " | audio=" + audio
        );

        // =========================================================
        // 2. DỪNG AUDIO, LOOP VÀ TIMER CỦA CÂU CŨ
        // =========================================================
        stopAllLoopsAndTimers();
        stopCurrentMediaPlayerSafely();

        isAudioLooping =
                false;

        isExampleLooping =
                false;

        isLoopingReplay =
                false;

        isViAudioPlaying =
                false;

        isFemaleLooping =
                false;

        isMaleLooping =
                false;

        updateVoiceLoopButtonsUi();

        // =========================================================
        // 3. TÌM TOPIC THẬT TƯƠNG ỨNG VỚI NOTIFICATION
        // =========================================================
        TopicInfo notificationTopic =
                findTopicByFileName(
                        finalTopicFile
                );

        /*
         * Trường hợp sentences.json:
         * thử lấy Default Topic nếu không tìm được trực tiếp.
         */
        if (notificationTopic == null
                && "sentences.json".equalsIgnoreCase(
                finalTopicFile
        )) {

            notificationTopic =
                    TopicManager.getDefaultTopic(
                            this
                    );
        }

        // =========================================================
        // 4. CHUYỂN HẲN NGỮ CẢNH SANG TOPIC NOTIFICATION
        // =========================================================
        if (notificationTopic != null) {

            /*
             * Xóa toàn bộ lesson/section filter cũ trước khi load topic.
             * sentences sau khi load sẽ là danh sách đầy đủ của topic
             * notification, không còn là playlist lesson cũ.
             */
            clearLockscreenSelectionForNotification();

            currentTopicOnLockscreen =
                    notificationTopic;

            SharedPreferences lockPrefs =
                    getSharedPreferences(
                            "lockscreen_prefs",
                            MODE_PRIVATE
                    );

            boolean prefsSaved =
                    lockPrefs.edit()
                            .putString(
                                    "current_topic_id",
                                    !TextUtils.isEmpty(notificationTopic.id)
                                            ? notificationTopic.id
                                            : ""
                            )
                            .putString(
                                    "current_topic_file",
                                    !TextUtils.isEmpty(notificationTopic.fileName)
                                            ? notificationTopic.fileName
                                            : finalTopicFile
                            )
                            .putBoolean(
                                    "use_lock_filtered_selection",
                                    false
                            )
                            .commit();

            getSharedPreferences(
                    "locks_settings",
                    MODE_PRIVATE
            )
                    .edit()
                    .putString(
                            "current_topic_id",
                            !TextUtils.isEmpty(notificationTopic.id)
                                    ? notificationTopic.id
                                    : ""
                    )
                    .apply();

            Log.d(
                    "NOTI_LOCK",
                    "Notification topic saved to prefs"
                            + " | saved=" + prefsSaved
                            + " | id=" + notificationTopic.id
                            + " | name=" + notificationTopic.name
                            + " | file=" + notificationTopic.fileName
            );

            /*
             * Đóng package manager cũ.
             */
            if (topicPackageManager != null) {
                try {
                    topicPackageManager.close();

                } catch (Exception e) {
                    Log.w(
                            "NOTI_LOCK",
                            "Cannot close old TopicPackageManager",
                            e
                    );
                }

                topicPackageManager =
                        null;
            }

            /*
             * Tạo package manager đúng topic notification.
             */
            try {
                String packageTopicName =
                        getTopicPackageNameFromJsonFile(
                                notificationTopic.fileName
                        );

                if (!TextUtils.isEmpty(packageTopicName)) {
                    topicPackageManager =
                            new TopicPackageManager(
                                    this,
                                    packageTopicName
                            );
                }

            } catch (Exception e) {
                topicPackageManager =
                        null;

                Log.e(
                        "NOTI_LOCK",
                        "Cannot create notification TopicPackageManager"
                                + " | file="
                                + notificationTopic.fileName,
                        e
                );
            }

            /*
             * Load bằng flow chuẩn của LockScreenActivity.
             */
            loadSentencesForTopicOnLockScreen(
                    notificationTopic
            );

            restoreTopicStats(
                    notificationTopic.id
            );

            /*
             * Reset toàn bộ state của topic/lesson cũ trong RAM.
             * Sau bước này, vuốt next sẽ chạy như học bình thường.
             */
            shownHistoryKeys.clear();
            shownHistoryIdx =
                    -1;

            historyCache.clear();

            inSessionLearningQueue.clear();
            consecutiveLearningShown =
                    0;

            todayNewPool.clear();
            todayNewIndex =
                    0;

            currentCardFromReviewLoop =
                    false;

            typingCheckSentence =
                    null;

            typingFailedAndShowingAnswer =
                    false;

            pendingRatingAfterTyping =
                    -1;

            rebuildTodayNewPool();
            recalcRatingCounters();

            updateStats();
            updateSuggestionList();
            updateSrsStatsBar();

            lastLoadedLockTopicId =
                    notificationTopic.id;

            lastLoadedLockTopicFile =
                    notificationTopic.fileName;

            Log.d(
                    "NOTI_LOCK",
                    "Notification topic loaded through normal flow"
                            + " | size="
                            + (
                            sentences != null
                                    ? sentences.size()
                                    : -1
                    )
                            + " | packageManager="
                            + (topicPackageManager != null)
            );

        } else {
            // =====================================================
            // FALLBACK: KHÔNG TÌM ĐƯỢC TOPICINFO
            // =====================================================
            Log.e(
                    "NOTI_LOCK",
                    "No TopicInfo found"
                            + " | file=" + finalTopicFile
                            + " -> fallback direct loading"
            );

            clearLockscreenSelectionForNotification();

            List<Sentence> fallbackSentences =
                    loadSentencesFromFile(
                            finalTopicFile
                    );

            if (fallbackSentences == null
                    || fallbackSentences.isEmpty()) {

                Log.e(
                        "NOTI_LOCK",
                        "Cannot load notification topic"
                                + " | file=" + finalTopicFile
                );

                return;
            }

            this.sentences =
                    fallbackSentences;

            /*
             * Reset state cũ ngay cả trong nhánh fallback.
             */
            shownHistoryKeys.clear();
            shownHistoryIdx =
                    -1;

            historyCache.clear();

            inSessionLearningQueue.clear();
            consecutiveLearningShown =
                    0;

            todayNewPool.clear();
            todayNewIndex =
                    0;

            currentCardFromReviewLoop =
                    false;

            typingCheckSentence =
                    null;

            typingFailedAndShowingAnswer =
                    false;

            pendingRatingAfterTyping =
                    -1;

            rebuildTodayNewPool();
            recalcRatingCounters();

            updateStats();
            updateSuggestionList();
            updateSrsStatsBar();
        }

        if (sentences == null
                || sentences.isEmpty()) {

            Log.e(
                    "NOTI_LOCK",
                    "Notification topic playlist is empty"
                            + " | file=" + finalTopicFile
            );

            return;
        }

        // =========================================================
        // 5. BẢO ĐẢM ID CÁC CÂU HỢP LỆ
        // =========================================================
        for (int i = 0;
             i < sentences.size();
             i++) {

            Sentence sentence =
                    sentences.get(i);

            if (sentence != null
                    && sentence.id == -1) {

                sentence.id =
                        i;
            }
        }

        // =========================================================
        // 6. TÌM ĐÚNG OBJECT SENTENCE TRONG TOPIC THẬT
        // =========================================================
        Sentence target =
                null;

        int targetIndex =
                -1;

        /*
         * Ưu tiên so khớp chính xác EN + VI + IPA.
         */
        for (int i = 0;
             i < sentences.size();
             i++) {

            Sentence sentence =
                    sentences.get(i);

            if (sentence == null) {
                continue;
            }

            if (safeEquals(
                    sentence.en,
                    en
            )
                    && safeEquals(
                    sentence.vi,
                    vi
            )
                    && safeEquals(
                    sentence.ipa,
                    ipa
            )) {

                target =
                        sentence;

                targetIndex =
                        i;

                break;
            }
        }

        /*
         * Fallback theo EN + VI.
         */
        if (target == null) {
            for (int i = 0;
                 i < sentences.size();
                 i++) {

                Sentence sentence =
                        sentences.get(i);

                if (sentence == null) {
                    continue;
                }

                if (safeEquals(
                        sentence.en,
                        en
                )
                        && safeEquals(
                        sentence.vi,
                        vi
                )) {

                    target =
                            sentence;

                    targetIndex =
                            i;

                    Log.w(
                            "NOTI_LOCK",
                            "Exact EN+VI+IPA not found"
                                    + " -> matched by EN+VI"
                    );

                    break;
                }
            }
        }

        /*
         * Fallback cuối theo EN.
         */
        if (target == null) {
            for (int i = 0;
                 i < sentences.size();
                 i++) {

                Sentence sentence =
                        sentences.get(i);

                if (sentence == null) {
                    continue;
                }

                if (safeEquals(
                        sentence.en,
                        en
                )) {

                    target =
                            sentence;

                    targetIndex =
                            i;

                    Log.w(
                            "NOTI_LOCK",
                            "Exact notification sentence not found"
                                    + " -> matched by EN"
                    );

                    break;
                }
            }
        }

        /*
         * Không thêm Sentence giả vào topic thật.
         * Rating phải áp dụng trực tiếp lên object trong sentences.
         */
        if (target == null) {
            Log.e(
                    "NOTI_LOCK",
                    "Notification sentence not found in real topic"
                            + " | topicFile=" + finalTopicFile
                            + " | en=" + en
            );

            return;
        }

        // =========================================================
        // 7. BỔ SUNG FIELD CÒN THIẾU TỪ INTENT
        // =========================================================
        if (TextUtils.isEmpty(target.en)) {
            target.en =
                    en;
        }

        if (TextUtils.isEmpty(target.vi)) {
            target.vi =
                    vi;
        }

        if (TextUtils.isEmpty(target.ipa)) {
            target.ipa =
                    ipa;
        }

        if (TextUtils.isEmpty(target.image)) {
            target.image =
                    image;
        }

        if (TextUtils.isEmpty(target.audio)) {
            target.audio =
                    audio;
        }

        // =========================================================
        // 8. SET CÂU HIỆN TẠI VÀ INDEX THẬT
        // =========================================================
        currentSentence =
                target;

        currentAudioEnFile =
                target.audio;

        currentAudioViFile =
                target.audio_vi;

        listeningSequentialIndex =
                targetIndex;

        lessonAutoLoopIndex =
                targetIndex;

        hasShownAnswer =
                false;

        hasShownAnswerForCurrentSentence =
                false;

        hasPlayedExampleForCurrentSentence =
                false;

        isViAudioPlaying =
                false;

        isSwitchingSentence =
                false;

        suppressPresetAutoAudioOnce =
                true;

        // =========================================================
        // 9. TẠO HISTORY BẮT ĐẦU TỪ CÂU NOTIFICATION
        // =========================================================
        shownHistoryKeys.clear();
        historyCache.clear();

        String targetKey =
                makeSentenceKey(
                        target
                );

        shownHistoryKeys.add(
                targetKey
        );

        historyCache.add(
                target
        );

        shownHistoryIdx =
                0;

        // =========================================================
        // 10. ĐỒNG BỘ INDEX VÀ STATE HIỆN TẠI
        // =========================================================
        syncSequentialIndexWithCurrentSentence();

        /*
         * Tự nhận diện Vocabulary/Listening theo toàn topic notification.
         * suppressPresetAutoAudioOnce đã bật nên không phát audio bất ngờ.
         */
        detectAndApplyStudyModeForCurrentSelection(
                "notification click"
        );

        // =========================================================
        // 11. CẬP NHẬT UI ĐÚNG CÂU NOTIFICATION
        // =========================================================
        updateCardViewWithSentence(
                target
        );

        updateNowPlayingHeader();

        updateNowPlayingInfo(
                target
        );

        updateStats();
        updateSrsStatsBar();

        // =========================================================
        // 12. LƯU VỊ TRÍ VÀ NGỮ CẢNH HIỆN TẠI
        // =========================================================
        saveCurrentLockscreenPosition();

        if (currentTopicOnLockscreen != null) {
            saveLockscreenSelectionState(
                    lockCurrentSelectionLabel,
                    lockCurrentLessonKeys,
                    lockCurrentGroupName,
                    lockCurrentSectionName
            );
        }

        Log.d(
                "NOTI_LOCK",
                "Notification learning context ready"
                        + " | index=" + targetIndex
                        + "/" + sentences.size()
                        + " | historyIdx=" + shownHistoryIdx
                        + " | historySize=" + historyCache.size()
                        + " | queueSize="
                        + (
                        inSessionLearningQueue != null
                                ? inSessionLearningQueue.size()
                                : -1
                )
                        + " | topicId="
                        + (
                        currentTopicOnLockscreen != null
                                ? currentTopicOnLockscreen.id
                                : "null"
                )
                        + " | topicFile="
                        + (
                        currentTopicOnLockscreen != null
                                ? currentTopicOnLockscreen.fileName
                                : finalTopicFile
                )
                        + " | en=" + target.en
                        + " | image=" + target.image
                        + " | audio=" + target.audio
        );
    }



    private void clearLockscreenSelectionForNotification() {
        /*
         * Notification mở theo topic thật, không tiếp tục sử dụng
         * lesson/section filter cũ của LockScreenActivity.
         */
        lockCurrentLessonKeys.clear();

        lockCurrentGroupName = "";
        lockCurrentSectionName = "";
        lockCurrentSelectionLabel = "";

        SharedPreferences lockPrefs =
                getSharedPreferences(
                        "lockscreen_prefs",
                        MODE_PRIVATE
                );

        lockPrefs.edit()
                .putStringSet(
                        "lock_current_lesson_keys",
                        new HashSet<>()
                )
                .putString(
                        "lock_current_group_name",
                        ""
                )
                .putString(
                        "lock_current_section_name",
                        ""
                )
                .putString(
                        "lock_current_selection_label",
                        ""
                )

                /*
                 * Xóa cả bộ key selection dùng chung với MainActivity.
                 */
                .putString(
                        "selected_group",
                        ""
                )
                .putString(
                        "selected_section",
                        ""
                )
                .putString(
                        "selected_lesson_key",
                        ""
                )
                .putString(
                        "selected_lesson_name",
                        ""
                )
                .putStringSet(
                        "selected_lesson_keys",
                        new HashSet<>()
                )
                .putString(
                        "selection_label",
                        ""
                )
                .putBoolean(
                        "is_virtual_selection",
                        false
                )
                .putString(
                        "selection_mode",
                        "topic"
                )
                .putBoolean(
                        "use_lock_filtered_selection",
                        false
                )
                .commit();

        Log.d(
                "NOTI_LOCK",
                "Old lockscreen lesson/section selection cleared"
        );
    }


    private TopicInfo findTopicByFileName(
            String topicFile
    ) {
        if (TextUtils.isEmpty(topicFile)) {
            return null;
        }

        String wanted =
                topicFile.trim()
                        .replace("\\", "/");

        String wantedBase =
                wanted;

        int wantedSlash =
                wanted.lastIndexOf('/');

        if (wantedSlash >= 0
                && wantedSlash < wanted.length() - 1) {

            wantedBase =
                    wanted.substring(
                            wantedSlash + 1
                    );
        }

        try {
            List<TopicInfo> topics =
                    TopicManager.getTopics(
                            this
                    );

            if (topics == null
                    || topics.isEmpty()) {

                Log.w(
                        "NOTI_LOCK",
                        "findTopicByFileName: topic list is empty"
                );

                return null;
            }

            for (TopicInfo topic : topics) {
                if (topic == null
                        || TextUtils.isEmpty(topic.fileName)) {

                    continue;
                }

                String candidate =
                        topic.fileName.trim()
                                .replace("\\", "/");

                String candidateBase =
                        candidate;

                int candidateSlash =
                        candidate.lastIndexOf('/');

                if (candidateSlash >= 0
                        && candidateSlash < candidate.length() - 1) {

                    candidateBase =
                            candidate.substring(
                                    candidateSlash + 1
                            );
                }

                // So khớp đường dẫn đầy đủ
                if (TextUtils.equals(
                        candidate,
                        wanted
                )) {

                    Log.d(
                            "NOTI_LOCK",
                            "Topic matched by full file name"
                                    + " | id=" + topic.id
                                    + " | file=" + topic.fileName
                    );

                    return topic;
                }

                // Fallback theo basename
                if (TextUtils.equals(
                        candidateBase,
                        wantedBase
                )) {

                    Log.d(
                            "NOTI_LOCK",
                            "Topic matched by basename"
                                    + " | id=" + topic.id
                                    + " | file=" + topic.fileName
                    );

                    return topic;
                }
            }

        } catch (Exception e) {
            Log.e(
                    "NOTI_LOCK",
                    "findTopicByFileName error"
                            + " | topicFile=" + topicFile,
                    e
            );
        }

        return null;
    }
}
