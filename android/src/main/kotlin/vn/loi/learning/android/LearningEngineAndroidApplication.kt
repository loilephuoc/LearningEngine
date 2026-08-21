package vn.loi.learning.android

import android.app.Application
import vn.loi.learning.android.platform.AndroidApplicationGraph
import vn.loi.learning.android.ui.AndroidThemeController
import vn.loi.learning.android.ui.SharedPreferencesThemeStore
import vn.loi.learning.android.study.AndroidStudyPreferencesController
import vn.loi.learning.android.study.SharedPreferencesStudyPreferenceStore
import vn.loi.learning.android.platform.AndroidStartupTrace
import vn.loi.learning.android.reminder.AndroidLockScreenVocabularyService
import vn.loi.learning.infrastructure.persistence.json.JsonPersistenceTrace

class LearningEngineAndroidApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidStartupTrace.enabled = BuildConfig.DEBUG
        JsonPersistenceTrace.enabled = BuildConfig.DEBUG
        vn.loi.learning.android.platform.AppLanguageManager.init(this)
        vn.loi.learning.android.media.LearningEngineAudioPolicy.init(this)
        vn.loi.learning.android.controller.ControllerDiagnosticsHolder.registerInputDeviceListener(this)
        reminderNotificationHelper.createNotificationChannel()
        AndroidLockScreenVocabularyService.reconcile(this, "APPLICATION_ON_CREATE")
        homeVocabularyWidgetCoordinator.start()
    }

    val themeController: AndroidThemeController by lazy {
        AndroidThemeController(SharedPreferencesThemeStore(this))
    }
    val studyPreferencesController: AndroidStudyPreferencesController by lazy {
        AndroidStudyPreferencesController(SharedPreferencesStudyPreferenceStore(this))
    }
    val controllerPreferencesController: vn.loi.learning.android.controller.ControllerPreferencesController by lazy {
        vn.loi.learning.android.controller.ControllerPreferencesController(
            vn.loi.learning.android.controller.SharedPreferencesControllerPreferenceStore(this)
        )
    }

    val reminderDifficultStore: vn.loi.learning.android.reminder.AndroidVocabularyReminderDifficultMarkers by lazy {
        vn.loi.learning.android.reminder.SharedPreferencesVocabularyReminderDifficultStore(this)
    }
    val reminderPreferencesController: vn.loi.learning.android.reminder.AndroidVocabularyReminderPreferencesController by lazy {
        vn.loi.learning.android.reminder.AndroidVocabularyReminderPreferencesController(
            vn.loi.learning.android.reminder.SharedPreferencesVocabularyReminderPreferenceStore(this)
        )
    }
    val reminderNotificationHelper: vn.loi.learning.android.reminder.AndroidVocabularyReminderNotificationHelper by lazy {
        vn.loi.learning.android.reminder.AndroidVocabularyReminderNotificationHelper(this)
    }
    val reminderCandidateSelector: vn.loi.learning.android.reminder.AndroidVocabularyReminderCandidateSelector by lazy {
        vn.loi.learning.android.reminder.AndroidVocabularyReminderCandidateSelector(
            context = graph.engine,
            difficultMarkers = reminderDifficultStore,
            shuffleBagStore = vn.loi.learning.android.reminder.SharedPreferencesLockScreenShuffleBagStore(this)
        )
    }
    val reminderOverlayController: vn.loi.learning.android.reminder.AndroidVocabularyReminderOverlayPresenter by lazy {
        vn.loi.learning.android.reminder.AndroidVocabularyReminderOverlayController(
            this,
            resolveMedia = { ref -> graph.media.resolve(ref)?.toString() },
            defaultQuickPauseHandler = { minutes ->
                reminderPreferencesController.pauseUnlocked(java.time.Duration.ofMinutes(minutes))
            }
        )
    }
    val reminderDeviceStateProvider: vn.loi.learning.android.reminder.AndroidVocabularyReminderDeviceStateProvider by lazy {
        vn.loi.learning.android.reminder.DefaultAndroidVocabularyReminderDeviceStateProvider(this)
    }
    val reminderRuntime: vn.loi.learning.android.reminder.AndroidVocabularyReminderRuntime by lazy {
        vn.loi.learning.android.reminder.AndroidVocabularyReminderRuntime(
            preferencesController = reminderPreferencesController,
            selector = reminderCandidateSelector,
            notificationHelper = reminderNotificationHelper,
            audioController = vn.loi.learning.android.media.AndroidAudioController(this),
            overlayPresenter = reminderOverlayController,
            deviceStateProvider = reminderDeviceStateProvider,
            resolveMedia = { ref -> graph.media.resolve(ref)?.toString() }
        )
    }
    val lockScreenVocabularyCoordinator: vn.loi.learning.android.reminder.AndroidLockScreenVocabularyCoordinator by lazy {
        vn.loi.learning.android.reminder.AndroidLockScreenVocabularyCoordinator(
            context = this,
            preferencesController = reminderPreferencesController,
            selector = reminderCandidateSelector,
            resolveMedia = { ref -> graph.media.resolve(ref)?.toString() },
            overlayPresenter = reminderOverlayController,
            notificationHelper = reminderNotificationHelper
        )
    }
    val homeVocabularyWidgetCoordinator: vn.loi.learning.android.reminder.AndroidHomeVocabularyWidgetCoordinator by lazy {
        vn.loi.learning.android.reminder.AndroidHomeVocabularyWidgetCoordinator(
            context = this,
            preferencesController = reminderPreferencesController,
            selector = reminderCandidateSelector,
            difficultMarkers = reminderDifficultStore,
            resolveMedia = { ref -> graph.media.resolve(ref)?.toString() }
        )
    }
    val reminderRatingBridge: vn.loi.learning.android.reminder.AndroidReminderReviewRatingBridge by lazy {
        vn.loi.learning.android.reminder.AndroidReminderReviewRatingBridge(
            context = graph.engine
        )
    }
    val reminderFsrsInspectorQuery: vn.loi.learning.android.reminder.AndroidReminderReviewFsrsInspectorQuery by lazy {
        vn.loi.learning.android.reminder.AndroidReminderReviewFsrsInspectorQuery(context = graph.engine)
    }

    @Volatile
    private var graphOwner = SingleInstanceOwner { AndroidApplicationGraph.create(this) }

    val graph: AndroidApplicationGraph
        get() = graphOwner.value

    fun reloadApplicationGraph(): AndroidApplicationGraph {
        graphOwner = SingleInstanceOwner { AndroidApplicationGraph.create(this) }
        return graphOwner.value
    }
}

internal class SingleInstanceOwner<T>(
    create: () -> T
) {
    val value: T by lazy(LazyThreadSafetyMode.SYNCHRONIZED, create)
}
