package vn.loi.learning.android

import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import vn.loi.learning.android.study.*
import vn.loi.learning.android.platform.*
import vn.loi.learning.android.ui.LearningEngineTheme
import vn.loi.learning.android.ui.*
import vn.loi.learning.android.library.*
import vn.loi.learning.android.packageexperience.*
import vn.loi.learning.android.autoplay.*
import vn.loi.learning.domain.library.model.InstalledPackageId
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.mutableIntStateOf
import android.view.KeyEvent
import android.view.MotionEvent
import vn.loi.learning.android.controller.ControllerDiagnosticsHolder
import vn.loi.learning.android.controller.ControllerDiagnosticsScreen
import vn.loi.learning.android.controller.ControllerSettingsScreen
import vn.loi.learning.android.controller.ControllerInputRouter
import vn.loi.learning.android.controller.EventOrigin
import android.Manifest
import vn.loi.learning.android.recording.QuickVoiceRecorderController
import vn.loi.learning.android.recording.QuickVoicePermissionBridge
import vn.loi.learning.android.recording.QuickVoiceRecordingsScreen
import vn.loi.learning.android.reminder.AndroidVocabularyReminderNotificationHelper
import vn.loi.learning.android.reminder.AndroidVocabularyReminderSelectionMode
import vn.loi.learning.android.recovery.BackupRestoreScreen
import vn.loi.learning.android.recovery.BackupRestoreViewModel
import vn.loi.learning.android.sync.AndroidSyncSettingsScreen
import vn.loi.learning.android.sync.AndroidSyncViewModel
import vn.loi.learning.android.reminder.HomeWidgetSettingsScreen
import vn.loi.learning.android.reminder.LockScreenSettingsScreen
import vn.loi.learning.android.reminder.ReminderReviewScreen
import vn.loi.learning.android.reminder.ReminderSettingsScreen
import vn.loi.learning.android.reminder.VocabularyRemindersHubScreen
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import kotlinx.coroutines.flow.MutableStateFlow

data class PendingReminderReviewTarget(
    val packageId: String,
    val contentId: String,
    val mode: String
)

class MainActivity : ComponentActivity() {

    private val pendingReminderTarget = MutableStateFlow<PendingReminderReviewTarget?>(null)

    private val recordAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        QuickVoiceRecorderController.onPermissionResult(isGranted)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val dev = event.device ?: if (event.deviceId > 0) android.view.InputDevice.getDevice(event.deviceId) else null
        if (vn.loi.learning.android.controller.ControllerInputDiagnostic.isCandidateControllerDevice(dev)) {
            val router = ControllerInputRouter.getInstance(this)
            if (router.isControllerEnabled()) {
                router.onKeyEvent(event, origin = EventOrigin.ACTIVITY)
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        ControllerDiagnosticsHolder.recordMotionEvent(event)
        return super.dispatchGenericMotionEvent(event)
    }

    override fun onStart() {
        super.onStart()
        ControllerDiagnosticsHolder.setLifecycleState("STARTED")
        vn.loi.learning.android.reminder.AndroidLockScreenVocabularyService.reconcile(this, "MAIN_ACTIVITY_ON_START")
    }

    override fun onResume() {
        super.onResume()
        ControllerDiagnosticsHolder.setLifecycleState("RESUMED")
        ControllerDiagnosticsHolder.setForeground(true)
        ControllerDiagnosticsHolder.refreshDevices(this)
        vn.loi.learning.android.controller.StudyControllerBridge.onActivityForegroundChanged(true)
        val app = application as? LearningEngineAndroidApplication
        app?.homeVocabularyWidgetCoordinator?.transitionDeviceState(
            vn.loi.learning.android.reminder.VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON,
            "MAIN_ACTIVITY_RESUMED"
        )
        app?.homeVocabularyWidgetCoordinator?.setHomeForegroundState(
            vn.loi.learning.android.reminder.HomeForegroundState.OTHER_APP,
            "LEARNING_ENGINE_FOREGROUND"
        )
    }

    override fun onPause() {
        super.onPause()
        ControllerDiagnosticsHolder.setLifecycleState("PAUSED")
        ControllerDiagnosticsHolder.setForeground(false)
        vn.loi.learning.android.controller.StudyControllerBridge.onActivityForegroundChanged(false)
    }

    override fun onStop() {
        super.onStop()
        ControllerDiagnosticsHolder.setLifecycleState("STOPPED")
        ControllerDiagnosticsHolder.setForeground(false)
        vn.loi.learning.android.controller.StudyControllerBridge.onActivityForegroundChanged(false)
        val app = application as? LearningEngineAndroidApplication
        app?.homeVocabularyWidgetCoordinator?.refreshForegroundState("LEARNING_ENGINE_STOPPED")
        app?.homeVocabularyWidgetCoordinator?.reconcileRuntimeClock("LEARNING_ENGINE_STOPPED")
    }

    override fun onDestroy() {
        super.onDestroy()
        QuickVoicePermissionBridge.unregister()
        ControllerDiagnosticsHolder.setLifecycleState("DESTROYED")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleReminderIntent(intent)
        if (intent.action == "vn.loi.learning.android.ACTION_TEST_WALLPAPER_DEBUG") {
            vn.loi.learning.android.reminder.AndroidLockScreenWallpaperRenderer.renderDebugSafeZones()
        } else if (intent.action == "vn.loi.learning.android.ACTION_TEST_WALLPAPER_APPLY") {
            val caseId = intent.getStringExtra("CASE_ID") ?: "DEFAULT"
            vn.loi.learning.android.reminder.AndroidLockScreenWallpaperAudit.applyTestCase(this, caseId)
        } else if (intent.action == "vn.loi.learning.android.ACTION_UPDATE_LOCKSCREEN_SETTINGS") {
            handleUpdateLockScreenSettingsIntent(intent)
        } else if (intent.action == "vn.loi.learning.android.ACTION_SHOW_TEST_OVERLAY") {
            handleShowTestOverlayIntent(intent)
        } else if (intent.action == "vn.loi.learning.android.ACTION_RENDER_OVERLAY_AUDIT") {
            handleRenderOverlayAuditIntent(intent)
        } else if (intent.action == "vn.loi.learning.android.ACTION_RESUME_UNLOCKED_NOW") {
            val app = application as? LearningEngineAndroidApplication
            app?.lockScreenVocabularyCoordinator?.resumeUnlockedNow()
        }
    }

    private fun handleRenderOverlayAuditIntent(intent: Intent?) {
        if (intent == null) return
        android.util.Log.i("OverlayAudit", "handleRenderOverlayAuditIntent extras=${intent.extras?.keySet()?.map { "$it=${intent.extras?.get(it)}" }}")
        val headword = intent.getStringExtra("HEADWORD") ?: "inform"
        val ipa = intent.getStringExtra("IPA")
        val pos = intent.getStringExtra("POS")
        val meaning = intent.getStringExtra("MEANING") ?: "Thông báo"
        val fileName = (intent.getStringExtra("OUTPUT_PATH") ?: intent.getStringExtra("OUTPUT_FILE") ?: "overlay_audit.png").substringAfterLast('/')
        val targetFile = java.io.File(filesDir, fileName)
        val sampleImage = vn.loi.learning.android.reminder.AndroidLockScreenWallpaperAudit.createSampleImage(400, 400, headword)
        vn.loi.learning.android.reminder.AndroidVocabularyReminderOverlayAudit.renderAndSaveOverlay(
            this, headword, ipa, pos, meaning, sampleImage, targetFile.absolutePath
        )
    }

    private fun handleShowTestOverlayIntent(intent: Intent?) {
        if (intent == null) return
        val app = application as? LearningEngineAndroidApplication ?: return
        val headword = intent.getStringExtra("HEADWORD") ?: "inform"
        val ipa = intent.getStringExtra("IPA") ?: "ɪnˈfɔːm"
        val pos = intent.getStringExtra("POS") ?: "verb"
        val meaning = intent.getStringExtra("MEANING") ?: "Thông báo"
        val duration = intent.getLongExtra("DURATION", 15000L)
        val imageRef = intent.getStringExtra("IMAGE_REF")

        val candidate = vn.loi.learning.android.reminder.AndroidVocabularyCandidate(
            contentId = vn.loi.learning.domain.content.model.ContentId("test-overlay-candidate"),
            packageId = vn.loi.learning.domain.library.model.InstalledPackageId("test-pkg"),
            packageName = "Test Package",
            primaryText = headword,
            answer = meaning,
            translation = meaning,
            ipa = ipa,
            partOfSpeech = pos,
            imageReference = imageRef,
            primaryAudioReference = null
        )

        app.reminderOverlayController.show(
            candidate = candidate,
            mode = vn.loi.learning.android.reminder.AndroidVocabularyReminderSelectionMode.RANDOM_ALL,
            displayDurationMillis = duration,
            onQuickPause = { action ->
                app.reminderPreferencesController.handleQuickPause(action)
            }
        )
    }

    private fun handleUpdateLockScreenSettingsIntent(intent: Intent?) {
        if (intent == null) return
        val app = application as? LearningEngineAndroidApplication ?: return
        android.util.Log.i("MainActivity", "handleUpdateLockScreenSettingsIntent action=${intent.action} extras=${intent.extras?.keySet()?.joinToString()} QUICK_REVIEW_INTERVAL=${intent.getLongExtra("QUICK_REVIEW_INTERVAL", -1)}")
        val current = app.reminderPreferencesController.currentLockScreen()
        val opacity = if (intent.hasExtra("OPACITY")) intent.getFloatExtra("OPACITY", current.cardBackgroundOpacity) else current.cardBackgroundOpacity
        val wordSizeStr = intent.getStringExtra("WORD_SIZE")
        val vnSizeStr = intent.getStringExtra("VN_SIZE")
        val imgSizeStr = intent.getStringExtra("IMG_SIZE")
        val wordSize = runCatching { wordSizeStr?.let { vn.loi.learning.android.reminder.LockWallpaperWordSize.valueOf(it) } }.getOrNull() ?: current.wordSize
        val vnSize = runCatching { vnSizeStr?.let { vn.loi.learning.android.reminder.LockWallpaperVietnameseSize.valueOf(it) } }.getOrNull() ?: current.vietnameseSize
        val imgSize = runCatching { imgSizeStr?.let { vn.loi.learning.android.reminder.LockWallpaperImageSize.valueOf(it) } }.getOrNull() ?: current.imageSize
        val quickReviewInterval = if (intent.hasExtra("QUICK_REVIEW_INTERVAL")) intent.getLongExtra("QUICK_REVIEW_INTERVAL", current.quickReviewIntervalMillis) else current.quickReviewIntervalMillis
        val screenOffPrepEnabled = if (intent.hasExtra("SCREEN_OFF_PREP_ENABLED")) intent.getBooleanExtra("SCREEN_OFF_PREP_ENABLED", current.screenOffPreparationEnabled) else current.screenOffPreparationEnabled
        val screenOffPrepareDelay = if (intent.hasExtra("SCREEN_OFF_PREPARE_DELAY")) intent.getLongExtra("SCREEN_OFF_PREPARE_DELAY", current.screenOffPrepareDelayMillis) else current.screenOffPrepareDelayMillis
        val autoPlay = if (intent.hasExtra("AUTOPLAY")) intent.getBooleanExtra("AUTOPLAY", current.autoPlayPronunciation) else current.autoPlayPronunciation

        val updated = current.copy(
            cardBackgroundOpacity = opacity,
            wordSize = wordSize,
            vietnameseSize = vnSize,
            imageSize = imgSize,
            quickReviewIntervalMillis = quickReviewInterval,
            screenOffPreparationEnabled = screenOffPrepEnabled,
            screenOffPrepareDelayMillis = screenOffPrepareDelay,
            autoPlayPronunciation = autoPlay
        )
        app.reminderPreferencesController.updateLockScreenSettings(updated)

        if (intent.hasExtra("UNLOCKED_INTERVAL") || intent.hasExtra("UNLOCKED_ENABLED")) {
            val curReminder = app.reminderPreferencesController.current()
            val unlockedInterval = if (intent.hasExtra("UNLOCKED_INTERVAL")) intent.getLongExtra("UNLOCKED_INTERVAL", curReminder.intervalMillis) else curReminder.intervalMillis
            val unlockedEnabled = if (intent.hasExtra("UNLOCKED_ENABLED")) intent.getBooleanExtra("UNLOCKED_ENABLED", curReminder.enabled) else curReminder.enabled
            app.reminderPreferencesController.updateSettings(
                curReminder.copy(
                    intervalMillis = unlockedInterval,
                    enabled = unlockedEnabled
                )
            )
        }

        val pauseAction = intent.getStringExtra("PAUSE_ACTION")
        if (pauseAction != null) {
            when (pauseAction) {
                "PAUSE_5M" -> app.reminderPreferencesController.pauseUnlocked5Minutes()
                "PAUSE_30M" -> app.reminderPreferencesController.pauseUnlocked30Minutes()
                "PAUSE_1H" -> app.reminderPreferencesController.pauseUnlockedOneHour()
                "PAUSE_INDEFINITE" -> app.reminderPreferencesController.pauseUnlockedIndefinitely()
                "RESUME" -> app.reminderPreferencesController.resumeUnlocked()
            }
        }

        app.lockScreenVocabularyCoordinator.reRenderCurrentPresentation("SETTINGS_INTENT")
        vn.loi.learning.android.reminder.AndroidLockScreenVocabularyService.reconcile(this, "SETTINGS_INTENT")
    }

    private fun handleReminderIntent(intent: Intent?) {
        val action = intent?.action
        val packageId = intent?.getStringExtra(AndroidVocabularyReminderNotificationHelper.EXTRA_PACKAGE_ID)
        val contentId = intent?.getStringExtra(AndroidVocabularyReminderNotificationHelper.EXTRA_CONTENT_ID)
        val mode = intent?.getStringExtra(AndroidVocabularyReminderNotificationHelper.EXTRA_REMINDER_MODE) ?: "AGAIN_HARD"
        if (action == AndroidVocabularyReminderNotificationHelper.ACTION_REMINDER_REVIEW && packageId != null && contentId != null) {
            android.util.Log.i("HomeWidgetReview", "[HomeWidgetReview] candidateId=$contentId resolved=true action=OPEN")
            pendingReminderTarget.value = PendingReminderReviewTarget(packageId, contentId, mode)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleReminderIntent(intent)
        if (intent?.action == "vn.loi.learning.android.ACTION_TEST_WALLPAPER_DEBUG") {
            vn.loi.learning.android.reminder.AndroidLockScreenWallpaperRenderer.renderDebugSafeZones()
        } else if (intent?.action == "vn.loi.learning.android.ACTION_TEST_WALLPAPER_APPLY") {
            val caseId = intent?.getStringExtra("CASE_ID") ?: "DEFAULT"
            vn.loi.learning.android.reminder.AndroidLockScreenWallpaperAudit.applyTestCase(this, caseId)
        } else if (intent?.action == "vn.loi.learning.android.ACTION_UPDATE_LOCKSCREEN_SETTINGS") {
            handleUpdateLockScreenSettingsIntent(intent)
        } else if (intent?.action == "vn.loi.learning.android.ACTION_SHOW_TEST_OVERLAY") {
            handleShowTestOverlayIntent(intent)
        } else if (intent?.action == "vn.loi.learning.android.ACTION_RENDER_OVERLAY_AUDIT") {
            handleRenderOverlayAuditIntent(intent)
        }
        QuickVoiceRecorderController.initialize(this)
        QuickVoicePermissionBridge.register {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        enableEdgeToEdge()
        val app = application as LearningEngineAndroidApplication
        AndroidStartupTrace.mark("set_content_reached")
        setContent {
            val themeMode by app.themeController.mode.collectAsStateWithLifecycle()
            val currentLanguage by vn.loi.learning.android.platform.AppLanguageManager.currentLanguage.collectAsStateWithLifecycle()
            val studyLimits by app.studyPreferencesController.limits.collectAsStateWithLifecycle()
            val continuousSkim by app.studyPreferencesController.continuousSkim.collectAsStateWithLifecycle()
            val dailyNotificationSettings by app.dailyNotificationPreferencesController.settings.collectAsStateWithLifecycle()

            val locale = java.util.Locale(currentLanguage.code)
            val currentConfig = LocalConfiguration.current
            val localizedConfig = remember(currentLanguage, currentConfig) {
                android.content.res.Configuration(currentConfig).apply {
                    setLocale(locale)
                    setLayoutDirection(locale)
                }
            }
            val baseContext = LocalContext.current
            val localizedContext = remember(currentLanguage, baseContext, localizedConfig) {
                val configContext = baseContext.createConfigurationContext(localizedConfig)
                LocalizedActivityContextWrapper(baseContext, configContext)
            }

            CompositionLocalProvider(
                LocalConfiguration provides localizedConfig,
                LocalContext provides localizedContext,
                LocalActivityResultRegistryOwner provides this@MainActivity
            ) {
                LearningEngineTheme(mode = themeMode) {
                    LaunchedEffect(Unit){AndroidStartupTrace.mark("first_composition_reached");withFrameNanos{AndroidStartupTrace.mark("first_frame_committed")}}
                    var graphRetry by rememberSaveable { mutableIntStateOf(0) }
                val rootState by produceState<AndroidRootState>(AndroidRootState.Bootstrapping, graphRetry) {
                    value=withContext(Dispatchers.IO) {
                        runCatching { AndroidRootState.Ready(app.graph) }
                            .getOrElse { error ->
                                AndroidStartupTrace.write(true,"phase=root_shell_state state=FAILURE type=${error.javaClass.simpleName}")
                                AndroidRootState.Failed("Không thể mở dữ liệu học của bạn một cách an toàn.")
                            }
                    }
                }
                val graph=(rootState as? AndroidRootState.Ready)?.graph
                LaunchedEffect(rootState::class) {
                    AndroidStartupTrace.write(false,"phase=root_shell_state state=${rootState.javaClass.simpleName} thread=${Thread.currentThread().name}")
                }
                if(graph==null) {
                    when(val root=rootState) {
                        AndroidRootState.Bootstrapping -> AndroidStartupShell()
                        is AndroidRootState.Ready -> AndroidFeatureLoading("Đang mở Learning Engine")
                        is AndroidRootState.Failed -> AndroidRootFailure(root) { graphRetry += 1 }
                    }
                } else {
                val studyViewModel = viewModel<AndroidStudyViewModel> {
                    AndroidStudyViewModel(
                        AndroidStudyFacade(
                            graph.engine,
                            resolveMedia = { reference -> graph.media.resolve(reference)?.toString() },
                            dailyLimits = app.studyPreferencesController::current,
                            continuousSkimEnabled = app.studyPreferencesController::continuousSkimEnabled,
                            getInsightsScopePackageId = app.studyPreferencesController::insightsScopePackageId,
                            onInsightsScopeChanged = app.studyPreferencesController::updateInsightsScopePackageId,
                            difficultMarkers = app.reminderDifficultStore
                        ),
                        createSavedStateHandle(),
                        typingViMutedInitially = app.studyPreferencesController.typingViMuted(),
                        onTypingViMutedChanged = app.studyPreferencesController::updateTypingViMuted,
                        onDailyLimitsChanged = app.studyPreferencesController::updateLimits
                    )
                }
                val studyBackgroundAudioController = remember(app) {
                    vn.loi.learning.android.media.AndroidAudioController(app).also {
                        vn.loi.learning.android.controller.StudyControllerBridge.registerBackgroundAudioController(it)
                    }
                }
                DisposableEffect(studyBackgroundAudioController) {
                    onDispose {
                        vn.loi.learning.android.controller.StudyControllerBridge.unregisterBackgroundAudioController(studyBackgroundAudioController)
                        studyBackgroundAudioController.close()
                    }
                }
                val state = studyViewModel.state.collectAsStateWithLifecycle().value
                val quickReviewSummary = studyViewModel.quickReviewSummary.collectAsStateWithLifecycle().value
                val contentViewModel = viewModel<AndroidContentViewModel> {
                    AndroidContentViewModel(AndroidContentOperations(graph), createSavedStateHandle())
                }
                val contentState = contentViewModel.state.collectAsStateWithLifecycle().value
                val syncViewModel = viewModel<AndroidSyncViewModel> {
                    AndroidSyncViewModel.production(this@MainActivity, graph.engine, graph.media)
                }
                val libraryViewModel = viewModel<AndroidLibraryViewModel>(key = "library-graph-$graphRetry") {
                    AndroidLibraryViewModel(AndroidLibraryFacade(
                        graph.engine,
                        app.studyPreferencesController::current,
                        onActivePackageChanged = {
                            app.quickReviewWidgetCoordinator.update()
                            studyViewModel.onEvent(AndroidStudyEvent.RefreshHomeIfIdle)
                        }
                    ), createSavedStateHandle())
                }
                val libraryState = libraryViewModel.state.collectAsStateWithLifecycle().value
                val autoPlayViewModel = viewModel<AutoPlayViewModel> {
                    val selector = AutoPlayContentSelector(
                        context = graph.engine,
                        resolveMedia = { reference -> graph.media.resolve(reference)?.toString() }
                    )
                    val prefStore = SharedPreferencesAutoPlayPreferenceStore(app)
                    val prefController = AutoPlayPreferencesController(prefStore)
                    val coordinator = AutoPlayRuntimeCoordinator.getInstance(app)
                    AutoPlayViewModel(selector, prefController, coordinator, app)
                }
                val packageOperations=remember { AndroidPackageOperations(graph) }
                val operationScope=rememberCoroutineScope()
                var packageActionId by rememberSaveable { mutableStateOf<String?>(null) }
                var packageOperationMessage by remember { mutableStateOf<String?>(null) }
                val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    if (uri == null) contentViewModel.cancel() else {
                        runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                        val name = runCatching {
                            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                                if (cursor.moveToFirst()) cursor.getString(0) else null
                            }
                        }.getOrNull() ?: "package.opd3"
                        contentViewModel.importDocument(name) { contentResolver.openInputStream(uri) }
                    }
                }
                val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
                    if (uri == null) contentViewModel.cancel() else contentViewModel.createBackup { contentResolver.openOutputStream(uri, "wt") }
                }
                val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    if (uri == null) contentViewModel.cancel() else contentViewModel.restoreBackup { contentResolver.openInputStream(uri) }
                }
                val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
                    val id=packageActionId;packageActionId=null
                    if(uri!=null&&id!=null) operationScope.launch { packageOperationMessage=packageOperations.export(InstalledPackageId(id)){contentResolver.openOutputStream(uri,"wt")}.message() }
                }
                val verifyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    if(uri!=null) operationScope.launch { packageOperationMessage=packageOperations.verify{contentResolver.openInputStream(uri)}.message() }
                }
                LaunchedEffect(contentState) {
                    if (contentState is AndroidContentOperationState.Succeeded) {
                        studyViewModel.onEvent(AndroidStudyEvent.Home)
                        libraryViewModel.invalidate()
                    }
                }
                val navController = rememberNavController()
                val openStudyFromExplicitEvent: (AndroidStudyEvent) -> Unit = { event ->
                    studyViewModel.onEvent(event)
                    if (opensStudyFromExplicitEvent(event)) {
                        navController.navigate("study") { launchSingleTop = true }
                    }
                }
                LaunchedEffect(libraryState) {
                    if (libraryState is AndroidLibraryState.StudyStarted) {
                        studyViewModel.onEvent(AndroidStudyEvent.OpenSession(libraryState.sessionId))
                        libraryViewModel.consumeStudyStarted(libraryState.sessionId)
                        navController.navigate("study") { launchSingleTop = true }
                    }
                }
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                val rootDestination = AndroidRootDestination.fromRoute(currentRoute)
                LaunchedEffect(currentRoute) {
                    if (currentRoute != null) {
                        AndroidStartupTrace.write(false, "phase=destination_changed destination=$currentRoute thread=${Thread.currentThread().name}")
                        when (currentRoute) {
                            "home" -> studyViewModel.onEvent(AndroidStudyEvent.EnsureHome)
                            "review" -> studyViewModel.onEvent(AndroidStudyEvent.ProjectHome)
                        }
                    }
                }
                val pendingReminder by pendingReminderTarget.collectAsStateWithLifecycle()
                LaunchedEffect(pendingReminder) {
                    val target = pendingReminder
                    if (target != null) {
                        pendingReminderTarget.value = null
                        navController.navigate("reminder_review/${target.packageId}/${target.contentId}/${target.mode}") {
                            launchSingleTop = true
                        }
                    }
                }
                val showRootNavigation = when {
                    currentRoute == "autoplay" -> false
                    currentRoute == "controller_diagnostics" -> false
                    currentRoute == "controller_settings" -> false
                    currentRoute == "voice_recordings" -> false
                    currentRoute == "vocabulary_reminders" -> false
                    currentRoute == "lock_screen_settings" -> false
                    currentRoute == "unlocked_reminder_settings" -> false
                    currentRoute == "home_widget_settings" -> false
                    currentRoute == "reminder_settings" -> false
                    currentRoute == "sync_settings" -> false
                    currentRoute?.startsWith("reminder_review") == true -> false
                    currentRoute?.startsWith("package/") == true -> false
                    currentRoute == "library" -> libraryState is AndroidLibraryState.Root
                    currentRoute == "study" -> state is AndroidStudyState.Home
                    currentRoute in listOf("home", "library", "study", "review", "settings") -> true
                    else -> false
                }
                Scaffold(bottomBar={if(showRootNavigation)AndroidRootNavigation(rootDestination.route){destination->navController.navigate(destination.route){popUpTo("home"){saveState=true};launchSingleTop=true;restoreState=true}}}) { innerPadding -> NavHost(
                    navController,
                    startDestination = "home",
                    modifier = androidx.compose.ui.Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)
                ) {
                    composable("home", enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
                        val home = state as? AndroidStudyState.Home
                        if(home==null){
                            when(state) {
                                AndroidStudyState.Loading -> AndroidFeatureLoading("Đang chuẩn bị tổng quan học tập")
                                is AndroidStudyState.Failed -> AndroidFeatureFailure("Không thể hiển thị tổng quan học tập",state.message,
                                    if (state.retryable) ({ studyViewModel.onEvent(AndroidStudyEvent.Retry) }) else null)
                                else -> AndroidFeatureLoading("Đang mở mục Học")
                            }
                            return@composable
                        }
                        BackHandler(enabled = contentState is AndroidContentOperationState.Running) {
                            contentViewModel.cancel()
                        }
                        HomeScreen(home, contentState, onEvent = openStudyFromExplicitEvent,
                            onLibrary = { navController.navigate("library") }, onReview = { navController.navigate("review") },
                            onStudyLauncher = { navController.navigate("study") { launchSingleTop = true } },
                            onAutoPlay = { navController.navigate("autoplay") { launchSingleTop = true } },
                            onContentDismiss = contentViewModel::cancel, onContentAction = { kind ->
                                contentViewModel.begin(kind)
                                when (kind) {
                                    AndroidOperationKind.IMPORT -> importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "application/json"))
                                    AndroidOperationKind.BACKUP -> backupLauncher.launch("learning-engine-backup.lebak")
                                    AndroidOperationKind.RESTORE -> restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                                }
                            }
                        )
                    }
                    composable("autoplay", enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
                        AutoPlayScreen(
                            viewModel = autoPlayViewModel,
                            onBack = {
                                autoPlayViewModel.stop()
                                navController.popBackStack()
                            }
                        )
                    }
                    composable("library", enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
                        LaunchedEffect(Unit) { libraryViewModel.ensureLoaded() }
                        LibraryScreen(
                            libraryState,
                            onOpenPackage = { packageId -> navController.navigate("package/$packageId") { launchSingleTop = true } },
                            libraryViewModel::search,
                            libraryViewModel::globalSearch, libraryViewModel::openSearchResult, libraryViewModel::select, libraryViewModel::beginEdit,
                            libraryViewModel::updateDraft, libraryViewModel::saveEdit, libraryViewModel::openLessons,
                            libraryViewModel::startPackage, libraryViewModel::startLesson, libraryViewModel::startSelected,
                            libraryViewModel::back, libraryViewModel::reload, resolveMedia={ graph.media.resolve(it)?.toString() },
                            operationMessage=packageOperationMessage,onExport={id->packageActionId=id;exportLauncher.launch("${id}.opd3")},
                            onVerify={verifyLauncher.launch(arrayOf("application/zip","application/octet-stream"))},onUninstall={id->operationScope.launch { packageOperationMessage=packageOperations.uninstall(InstalledPackageId(id)).message();libraryViewModel.back() }},
                            contentState=contentState,onImport={contentViewModel.begin(AndroidOperationKind.IMPORT);importLauncher.launch(arrayOf("application/zip","application/octet-stream","application/json"))},
                            onFilter=libraryViewModel::filter,onOpenCollection=libraryViewModel::openCollection,
                            onSelectLearningPackage=libraryViewModel::selectLearningPackage)
                    }
                    composable("package/{packageId}", enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) { backEntry ->
                        val packageId = backEntry.arguments?.getString("packageId") ?: return@composable
                        val packageViewModel = viewModel<AndroidPackageViewModel>(backEntry) {
                            AndroidPackageViewModel(AndroidPackageFacade(
                                context = graph.engine,
                                dailyLimits = app.studyPreferencesController::current,
                                difficultMarkers = app.reminderDifficultStore,
                                onActivePackageChanged = {
                                    app.quickReviewWidgetCoordinator.update()
                                    studyViewModel.onEvent(AndroidStudyEvent.RefreshHomeIfIdle)
                                }
                            ), createSavedStateHandle())
                        }
                        LaunchedEffect(packageId) { packageViewModel.open(packageId) }
                        val packageState by packageViewModel.state.collectAsStateWithLifecycle()
                        val packageOpState by packageViewModel.operationState.collectAsStateWithLifecycle()
                        PackageScreen(
                            state = packageState,
                            operationState = packageOpState,
                            onBack = { navController.popBackStack() },
                            onSearch = packageViewModel::search,
                            onClearSearch = packageViewModel::clearSearch,
                            onStudyPackage = {
                                packageViewModel.startStudy { sessionId ->
                                    studyViewModel.onEvent(AndroidStudyEvent.OpenSession(sessionId))
                                    navController.navigate("study") { launchSingleTop = true }
                                }
                            },
                            onContinueLearning = {
                                packageViewModel.startStudy { sessionId ->
                                    studyViewModel.onEvent(AndroidStudyEvent.OpenSession(sessionId))
                                    navController.navigate("study") { launchSingleTop = true }
                                }
                            },
                            onSelectLearningPackage = packageViewModel::selectLearningPackage,
                            onOpenContent = { contentId ->
                                navController.navigate("reminder_review/$packageId/$contentId/RANDOM_ALL")
                            },
                            onSaveQuickEdit = packageViewModel::saveQuickEdit,
                            onExport = { packageActionId = packageId; exportLauncher.launch("${packageId}.opd3") },
                            onVerify = { verifyLauncher.launch(arrayOf("application/zip","application/octet-stream")) },
                            onUninstall = {
                                operationScope.launch {
                                    val msg = packageOperations.uninstall(InstalledPackageId(packageId)).message()
                                    packageViewModel.setOperationResult(msg, true)
                                    libraryViewModel.reload()
                                    navController.popBackStack()
                                }
                            },
                            resolveMedia = { reference -> graph.media.resolve(reference)?.toString() },
                            onDismissOperation = packageViewModel::dismissOperation,
                            onSetFsrsFilter = packageViewModel::setFsrsFilter,
                            onToggleDifficultFilter = packageViewModel::toggleDifficultFilter,
                            onSetLessonFilter = packageViewModel::setLessonFilter,
                            onSetMediaFilter = packageViewModel::setMediaFilter,
                            onClearFilters = packageViewModel::clearFilters,
                            onToggleDifficult = packageViewModel::toggleDifficult,
                            onRefresh = packageViewModel::refresh
                        )
                    }
                    composable("study", enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
                        if(state is AndroidStudyState.Home) StudyHub(
                            state,
                            openStudyFromExplicitEvent,
                            onLibrary = { navController.navigate("library") },
                            onReview = { navController.navigate("review") }
                        )
                        else {
                        BackHandler {
                            studyViewModel.onEvent(AndroidStudyEvent.Home)
                            navController.navigate("home") { popUpTo("study") { inclusive = true } }
                        }
                        StudyScreen(
                            state,
                            onEvent = { event ->
                                studyViewModel.onEvent(event)
                                if (event == AndroidStudyEvent.Home) {
                                    navController.navigate("home") { popUpTo("study") { inclusive = true } }
                                }
                            },
                            onAutoPlay = {
                                val contentIds = studyViewModel.activeStudySessionAutoPlayContentIds()
                                if (contentIds.isNotEmpty()) {
                                    autoPlayViewModel.startAutoPlayForContentIds(contentIds)
                                    navController.navigate("autoplay") { launchSingleTop = true }
                                }
                            },
                            isDifficult = studyViewModel::isDifficult,
                            onToggleDifficult = studyViewModel::toggleDifficult,
                            onSaveQuickEdit = studyViewModel::saveQuickEdit
                        ) }
                    }
                    composable("review", enterTransition={fadeIn()},exitTransition={fadeOut()}) {
                        val home=state as? AndroidStudyState.Home
                        if(home==null) {
                            when(state) {
                                AndroidStudyState.Loading -> AndroidFeatureLoading("Đang chuẩn bị ôn tập")
                                is AndroidStudyState.Failed -> AndroidFeatureFailure("Không thể mở ôn tập",state.message,
                                    if (state.retryable) ({ studyViewModel.onEvent(AndroidStudyEvent.Retry) }) else null)
                                else -> AndroidFeatureLoading("Đang mở mục Học")
                            }
                        } else ReviewHub(
                            home,
                            quickReviewSummary,
                            openStudyFromExplicitEvent,
                            onAutoPlayEntry = { entry ->
                                val contentIds = studyViewModel.resolveReviewEntryAutoPlayContentIds(entry)
                                if (contentIds.isNotEmpty()) {
                                    autoPlayViewModel.startAutoPlayForContentIds(contentIds)
                                    navController.navigate("autoplay") { launchSingleTop = true }
                                }
                            }
                        )
                    }
                    composable("settings", enterTransition={fadeIn()},exitTransition={fadeOut()}) {
                        val activePackageName = remember(graph) {
                            val libId = graph.engine.defaultLibraryId
                            val actPkgId = libId?.let { graph.engine.domainLibraryRepository?.findById(it)?.activePackageId }
                            actPkgId?.let { graph.engine.installedPackageRepository?.findById(it)?.name?.value }
                        }
                        SettingsScreen(
                            themeMode, app.themeController::setMode, studyLimits,
                            { newLimit ->
                                val res = app.studyPreferencesController.updateNew(newLimit)
                                studyViewModel.onEvent(AndroidStudyEvent.Home)
                                res
                            },
                            { reviewLimit ->
                                val res = app.studyPreferencesController.updateReview(reviewLimit)
                                studyViewModel.onEvent(AndroidStudyEvent.Home)
                                res
                            },
                            continuousSkim,
                            app.studyPreferencesController::updateContinuousSkim,
                            onControllerSettings = { navController.navigate("controller_settings") { launchSingleTop = true } },
                            onControllerDiagnostics = { navController.navigate("controller_diagnostics") { launchSingleTop = true } },
                            onVoiceRecordings = { navController.navigate("voice_recordings") { launchSingleTop = true } },
                            onVocabularyReminders = { navController.navigate("vocabulary_reminders") { launchSingleTop = true } },
                            onReminderSettings = { navController.navigate("vocabulary_reminders") { launchSingleTop = true } },
                            onHomeWidgetSettings = { navController.navigate("home_widget_settings") { launchSingleTop = true } },
                            onBackupRestore = { navController.navigate("backup_restore") { launchSingleTop = true } },
                            onSyncSettings = { navController.navigate("sync_settings") { launchSingleTop = true } },
                            onAdaptiveStudyUiLab = { navController.navigate("adaptive_study_ui_lab") { launchSingleTop = true } },
                            currentLanguage = currentLanguage,
                            onLanguage = { vn.loi.learning.android.platform.AppLanguageManager.setLanguage(this@MainActivity, it) },
                            dailyNotificationSettings = dailyNotificationSettings,
                            activePackageName = activePackageName,
                            onUpdateDueReview = { enabled, hour, minute ->
                                app.dailyNotificationPreferencesController.updateDueReview(enabled, hour, minute)
                                app.dailyNotificationScheduler.reconcile()
                            },
                            onUpdateInactivity = { enabled, threshold, hour, minute ->
                                app.dailyNotificationPreferencesController.updateInactivity(enabled, threshold, hour, minute)
                                app.dailyNotificationScheduler.reconcile()
                            }
                        ) { kind->contentViewModel.begin(kind);when(kind){AndroidOperationKind.IMPORT->importLauncher.launch(arrayOf("application/zip","application/octet-stream","application/json"));AndroidOperationKind.BACKUP->backupLauncher.launch("learning-engine-backup.lebak");AndroidOperationKind.RESTORE->restoreLauncher.launch(arrayOf("application/zip","application/octet-stream"))} }
                    }
                    composable("sync_settings", enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
                        AndroidSyncSettingsScreen(syncViewModel) { navController.popBackStack() }
                    }
                    composable("backup_restore", enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
                        val backupRestoreViewModel = viewModel<BackupRestoreViewModel> {
                            BackupRestoreViewModel(
                                graphProvider = { app.graph },
                                savedState = createSavedStateHandle()
                            )
                        }
                        val coroutineScope = rememberCoroutineScope()
                        BackupRestoreScreen(
                            viewModel = backupRestoreViewModel,
                            onBack = { navController.popBackStack() },
                            onReload = {
                                coroutineScope.launch {
                                    app.reloadApplicationGraph()
                                    vn.loi.learning.android.recording.QuickVoiceRecordingRepository.getInstance(this@MainActivity).reconcile()
                                    app.homeVocabularyWidgetCoordinator.start()
                                    vn.loi.learning.android.reminder.AndroidLockScreenVocabularyService.reconcile(this@MainActivity, "POST_RESTORE")
                                    graphRetry += 1
                                    navController.navigate("home") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }
                    composable("vocabulary_reminders", enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
                        VocabularyRemindersHubScreen(
                            controller = app.reminderPreferencesController,
                            onLockScreenSettings = { navController.navigate("lock_screen_settings") { launchSingleTop = true } },
                            onUnlockedReminderSettings = { navController.navigate("unlocked_reminder_settings") { launchSingleTop = true } },
                            onHomeWidgetSettings = { navController.navigate("home_widget_settings") { launchSingleTop = true } },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("lock_screen_settings", enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
                        LockScreenSettingsScreen(
                            controller = app.reminderPreferencesController,
                            selector = app.reminderCandidateSelector,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("unlocked_reminder_settings", enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
                        ReminderSettingsScreen(
                            controller = app.reminderPreferencesController,
                            runtime = app.reminderRuntime,
                            selector = app.reminderCandidateSelector,
                            notificationHelper = app.reminderNotificationHelper,
                            onLockScreenSettings = { navController.navigate("lock_screen_settings") { launchSingleTop = true } },
                            onHomeWidgetSettings = { navController.navigate("home_widget_settings") { launchSingleTop = true } },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("home_widget_settings", enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
                        HomeWidgetSettingsScreen(
                            controller = app.reminderPreferencesController,
                            selector = app.reminderCandidateSelector,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("reminder_settings", enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
                        VocabularyRemindersHubScreen(
                            controller = app.reminderPreferencesController,
                            onLockScreenSettings = { navController.navigate("lock_screen_settings") { launchSingleTop = true } },
                            onUnlockedReminderSettings = { navController.navigate("unlocked_reminder_settings") { launchSingleTop = true } },
                            onHomeWidgetSettings = { navController.navigate("home_widget_settings") { launchSingleTop = true } },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        "reminder_review/{packageId}/{contentId}/{mode}",
                        enterTransition = { fadeIn() },
                        exitTransition = { fadeOut() }
                    ) { backStackEntry ->
                        val packageId = backStackEntry.arguments?.getString("packageId") ?: return@composable
                        val contentId = backStackEntry.arguments?.getString("contentId") ?: return@composable
                        val modeName = backStackEntry.arguments?.getString("mode") ?: "AGAIN_HARD"

                        val session = remember(packageId, contentId, modeName) {
                            app.reminderCandidateSelector.getReviewQueue(packageId, modeName, contentId)
                        }

                        if (session != null) {
                            ReminderReviewScreen(
                                session = session,
                                difficultMarkers = app.reminderDifficultStore,
                                resolveMedia = { ref -> graph.media.resolve(ref)?.toString() },
                                onBack = { navController.popBackStack() },
                                runtime = app.reminderRuntime,
                                ratingBridge = app.reminderRatingBridge,
                                fsrsInspectorQuery = app.reminderFsrsInspectorQuery
                            )
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Mục từ vựng không khả dụng.", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                    composable("controller_settings", enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
                        ControllerSettingsScreen(
                            onBack = { navController.popBackStack() },
                            onOpenDiagnostics = { navController.navigate("controller_diagnostics") { launchSingleTop = true } }
                        )
                    }
                    composable("voice_recordings", enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
                        QuickVoiceRecordingsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("controller_diagnostics", enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
                        ControllerDiagnosticsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    if (BuildConfig.DEBUG) {
                        composable("adaptive_study_ui_lab", enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
                            vn.loi.learning.android.study.debug.AdaptiveStudyUiLabScreen(
                                engine = app.graph.engine,
                                onBack = { navController.popBackStack() },
                                onOpenPreview = { packageId, itemIndex, mode ->
                                    navController.navigate("adaptive_study_ui_preview/$packageId/$itemIndex/${mode.name}")
                                }
                            )
                        }
                        composable(
                            "adaptive_study_ui_preview/{packageId}/{itemIndex}/{mode}",
                            enterTransition = { fadeIn() },
                            exitTransition = { fadeOut() }
                        ) { backStackEntry ->
                            val packageId = backStackEntry.arguments?.getString("packageId") ?: "demo-package"
                            val itemIndex = backStackEntry.arguments?.getString("itemIndex")?.toIntOrNull() ?: 0
                            val modeName = backStackEntry.arguments?.getString("mode") ?: vn.loi.learning.android.study.debug.LabStudyMode.TYPING.name
                            val mode = runCatching { vn.loi.learning.android.study.debug.LabStudyMode.valueOf(modeName) }
                                .getOrDefault(vn.loi.learning.android.study.debug.LabStudyMode.TYPING)

                            vn.loi.learning.android.study.debug.AdaptiveStudyUiPreviewScreen(
                                engine = app.graph.engine,
                                initialPackageId = packageId,
                                initialItemIndex = itemIndex,
                                initialMode = mode,
                                resolveMedia = { ref -> app.graph.media.resolve(ref)?.toString() },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                } } }
            }
            }
        }
    }
}

internal fun opensStudyFromExplicitEvent(event: AndroidStudyEvent): Boolean =
    event is AndroidStudyEvent.Start || event is AndroidStudyEvent.OpenSession || event == AndroidStudyEvent.Resume

private fun AndroidPackageOperationResult.message()=when(this){is AndroidPackageOperationResult.Success->message;is AndroidPackageOperationResult.Failed->message;is AndroidPackageOperationResult.Verification->if(valid)"Xác minh gói thành công." else "Xác minh gói thất bại: ${errors.joinToString()}"}

private class LocalizedActivityContextWrapper(
    base: Context,
    private val localizedConfigurationContext: Context
) : ContextWrapper(base) {
    override fun getResources(): Resources = localizedConfigurationContext.resources
}
