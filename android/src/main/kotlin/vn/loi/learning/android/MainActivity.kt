package vn.loi.learning.android

import android.content.ComponentName
import android.os.Bundle
import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material3.Scaffold
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import vn.loi.learning.android.reminder.ReminderReviewScreen
import vn.loi.learning.android.reminder.ReminderSettingsScreen
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
        val app = application as? LearningEngineAndroidApplication
        val shouldStartService = app?.reminderPreferencesController?.current()?.enabled == true ||
            app?.reminderPreferencesController?.currentLockScreen()?.enabled == true
        if (shouldStartService) {
            vn.loi.learning.android.reminder.AndroidLockScreenVocabularyService.start(this)
        }
    }

    override fun onResume() {
        super.onResume()
        ControllerDiagnosticsHolder.setLifecycleState("RESUMED")
        ControllerDiagnosticsHolder.setForeground(true)
        ControllerDiagnosticsHolder.refreshDevices(this)
        vn.loi.learning.android.controller.StudyControllerBridge.onActivityForegroundChanged(true)
    }

    override fun onPause() {
        super.onPause()
        ControllerDiagnosticsHolder.setLifecycleState("PAUSED")
    }

    override fun onStop() {
        super.onStop()
        ControllerDiagnosticsHolder.setLifecycleState("STOPPED")
        ControllerDiagnosticsHolder.setForeground(false)
        vn.loi.learning.android.controller.StudyControllerBridge.onActivityForegroundChanged(false)
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
            onQuickPause = { minutes ->
                app.reminderPreferencesController.pauseUnlocked(java.time.Duration.ofMinutes(minutes))
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
                "RESUME" -> app.reminderPreferencesController.resumeUnlocked()
            }
        }

        app.lockScreenVocabularyCoordinator.reRenderCurrentPresentation("SETTINGS_INTENT")
    }

    private fun handleReminderIntent(intent: Intent?) {
        val action = intent?.action
        val packageId = intent?.getStringExtra(AndroidVocabularyReminderNotificationHelper.EXTRA_PACKAGE_ID)
        val contentId = intent?.getStringExtra(AndroidVocabularyReminderNotificationHelper.EXTRA_CONTENT_ID)
        val mode = intent?.getStringExtra(AndroidVocabularyReminderNotificationHelper.EXTRA_REMINDER_MODE) ?: "AGAIN_HARD"
        if (action == AndroidVocabularyReminderNotificationHelper.ACTION_REMINDER_REVIEW && packageId != null && contentId != null) {
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
            val studyLimits by app.studyPreferencesController.limits.collectAsStateWithLifecycle()
            val continuousSkim by app.studyPreferencesController.continuousSkim.collectAsStateWithLifecycle()
            LearningEngineTheme(mode = themeMode) {
                LaunchedEffect(Unit){AndroidStartupTrace.mark("first_composition_reached");withFrameNanos{AndroidStartupTrace.mark("first_frame_committed")}}
                var graphRetry by rememberSaveable { mutableIntStateOf(0) }
                val rootState by produceState<AndroidRootState>(AndroidRootState.Bootstrapping, graphRetry) {
                    value=withContext(Dispatchers.IO) {
                        runCatching { AndroidRootState.Ready(app.graph) }
                            .getOrElse { error ->
                                AndroidStartupTrace.write(true,"phase=root_shell_state state=FAILURE type=${error.javaClass.simpleName}")
                                AndroidRootState.Failed("Your learning data could not be opened safely.")
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
                        is AndroidRootState.Ready -> AndroidFeatureLoading("Opening Learning Engine")
                        is AndroidRootState.Failed -> AndroidRootFailure(root) { graphRetry += 1 }
                    }
                } else {
                val studyViewModel = viewModel<AndroidStudyViewModel> {
                    AndroidStudyViewModel(
                        AndroidStudyFacade(graph.engine, resolveMedia = { reference ->
                            graph.media.resolve(reference)?.toString()
                        }, dailyLimits = app.studyPreferencesController::current,
                            continuousSkimEnabled = app.studyPreferencesController::continuousSkimEnabled),
                        createSavedStateHandle(),
                        typingViMutedInitially = app.studyPreferencesController.typingViMuted(),
                        onTypingViMutedChanged = app.studyPreferencesController::updateTypingViMuted
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
                val libraryViewModel = viewModel<AndroidLibraryViewModel> {
                    AndroidLibraryViewModel(AndroidLibraryFacade(
                        graph.engine, app.studyPreferencesController::current
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
                    currentRoute == "reminder_settings" -> false
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
                                AndroidStudyState.Loading -> AndroidFeatureLoading("Preparing your learning overview")
                                is AndroidStudyState.Failed -> AndroidFeatureFailure("Learning overview unavailable",state.message,
                                    if (state.retryable) ({ studyViewModel.onEvent(AndroidStudyEvent.Retry) }) else null)
                                else -> AndroidFeatureLoading("Opening Study")
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
                                graph.engine, app.studyPreferencesController::current
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
                            onDismissOperation = packageViewModel::dismissOperation
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
                            }
                        ) }
                    }
                    composable("review", enterTransition={fadeIn()},exitTransition={fadeOut()}) {
                        val home=state as? AndroidStudyState.Home
                        if(home==null) {
                            when(state) {
                                AndroidStudyState.Loading -> AndroidFeatureLoading("Preparing Review")
                                is AndroidStudyState.Failed -> AndroidFeatureFailure("Review unavailable",state.message,
                                    if (state.retryable) ({ studyViewModel.onEvent(AndroidStudyEvent.Retry) }) else null)
                                else -> AndroidFeatureLoading("Opening Study")
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
                        SettingsScreen(
                            themeMode, app.themeController::setMode, studyLimits,
                            app.studyPreferencesController::updateNew,
                            app.studyPreferencesController::updateReview,
                            continuousSkim,
                            app.studyPreferencesController::updateContinuousSkim,
                            onControllerSettings = { navController.navigate("controller_settings") { launchSingleTop = true } },
                            onControllerDiagnostics = { navController.navigate("controller_diagnostics") { launchSingleTop = true } },
                            onVoiceRecordings = { navController.navigate("voice_recordings") { launchSingleTop = true } },
                            onReminderSettings = { navController.navigate("reminder_settings") { launchSingleTop = true } }
                        ) { kind->contentViewModel.begin(kind);when(kind){AndroidOperationKind.IMPORT->importLauncher.launch(arrayOf("application/zip","application/octet-stream","application/json"));AndroidOperationKind.BACKUP->backupLauncher.launch("learning-engine-backup.lebak");AndroidOperationKind.RESTORE->restoreLauncher.launch(arrayOf("application/zip","application/octet-stream"))} }
                    }
                    composable("reminder_settings", enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
                        ReminderSettingsScreen(
                            controller = app.reminderPreferencesController,
                            runtime = app.reminderRuntime,
                            selector = app.reminderCandidateSelector,
                            notificationHelper = app.reminderNotificationHelper,
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
                                runtime = app.reminderRuntime
                            )
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Vocabulary item is unavailable.", style = MaterialTheme.typography.titleMedium)
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
                } } }
            }
        }
    }
}

internal fun opensStudyFromExplicitEvent(event: AndroidStudyEvent): Boolean =
    event is AndroidStudyEvent.Start || event is AndroidStudyEvent.OpenSession || event == AndroidStudyEvent.Resume

private fun AndroidPackageOperationResult.message()=when(this){is AndroidPackageOperationResult.Success->message;is AndroidPackageOperationResult.Failed->message;is AndroidPackageOperationResult.Verification->if(valid)"Package verification passed." else "Package verification failed: ${errors.joinToString()}"}
