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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

class LearningEngineAndroidApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        AndroidStartupTrace.enabled = BuildConfig.DEBUG
        JsonPersistenceTrace.enabled = BuildConfig.DEBUG
        vn.loi.learning.android.platform.AppLanguageManager.init(this)
        vn.loi.learning.android.media.LearningEngineAudioPolicy.init(this)
        vn.loi.learning.android.controller.ControllerDiagnosticsHolder.registerInputDeviceListener(this)
        reminderNotificationHelper.createNotificationChannel()
        dailyNotificationHelper.createNotificationChannels()
        dailyNotificationScheduler.reconcile()
        familyNotificationPublisher.createChannel()
        familyReminderReconciler.reconcile(forceReschedule = true)
        familyCloudSyncController.enqueueBackground()
        applicationScope.launch {
            familyRepository.snapshot.drop(1).collect {
                familyReminderReconciler.reconcile()
            }
        }
        AndroidLockScreenVocabularyService.reconcile(this, "APPLICATION_ON_CREATE")
        homeVocabularyWidgetCoordinator.start()
    }

    val dailyNotificationPreferencesController: vn.loi.learning.android.notification.DailyLearningNotificationPreferencesController by lazy {
        vn.loi.learning.android.notification.DailyLearningNotificationPreferencesController(
            vn.loi.learning.android.notification.SharedPreferencesDailyLearningNotificationStore(this)
        )
    }
    val dailyNotificationHelper: vn.loi.learning.android.notification.DailyLearningNotificationHelper by lazy {
        vn.loi.learning.android.notification.DailyLearningNotificationHelper(this)
    }
    val dailyNotificationScheduler: vn.loi.learning.android.notification.DailyLearningNotificationScheduler by lazy {
        vn.loi.learning.android.notification.DailyLearningNotificationScheduler(this, dailyNotificationPreferencesController)
    }

    val themeController: AndroidThemeController by lazy {
        AndroidThemeController(SharedPreferencesThemeStore(this))
    }
    val studyPreferencesController: AndroidStudyPreferencesController by lazy {
        AndroidStudyPreferencesController(SharedPreferencesStudyPreferenceStore(this))
    }
    val familySyncMetadataStore: vn.loi.learning.android.family.FamilySyncMetadataStore by lazy {
        vn.loi.learning.android.family.FamilySyncMetadataStore(
            filesDir.toPath().resolve("learning-engine/family/family-sync-v1.json")
        )
    }
    private val rawFamilyRepository: vn.loi.learning.android.family.FamilyRepository by lazy {
        vn.loi.learning.android.family.JsonFamilyRepository(filesDir.toPath().resolve("learning-engine/family/family-v1.json"))
    }
    val familyRepository: vn.loi.learning.android.family.FamilyRepository by lazy {
        vn.loi.learning.android.family.SyncAwareFamilyRepository(rawFamilyRepository, familySyncMetadataStore) {
            familyCloudSyncController.enqueueBackground()
            vn.loi.learning.android.family.widget.FamilyAppWidgetProvider.updateAll(this)
            vn.loi.learning.android.family.widget.FamilyMonthAppWidgetProvider.updateAll(this)
        }
    }
    val familyCloudSyncController: vn.loi.learning.android.family.FamilyCloudSyncController by lazy {
        val configuration = if (BuildConfig.FAMILY_SUPABASE_URL.isBlank() || BuildConfig.FAMILY_SUPABASE_PUBLISHABLE_KEY.isBlank()) null
        else runCatching {
            vn.loi.learning.infrastructure.sync.supabase.SupabaseConfiguration(
                BuildConfig.FAMILY_SUPABASE_URL,
                BuildConfig.FAMILY_SUPABASE_PUBLISHABLE_KEY
            )
        }.getOrNull()
        vn.loi.learning.android.family.FamilyCloudSyncController(
            this,
            applicationScope,
            configuration,
            familyRepository,
            familySyncMetadataStore,
            afterMerge = {
                familyReminderReconciler.reconcile()
                vn.loi.learning.android.family.widget.FamilyAppWidgetProvider.updateAll(this)
                vn.loi.learning.android.family.widget.FamilyMonthAppWidgetProvider.updateAll(this)
            }
        )
    }
    val familyReminderScheduler: vn.loi.learning.android.family.AndroidFamilyReminderScheduler by lazy {
        vn.loi.learning.android.family.AndroidFamilyReminderScheduler(this)
    }
    val familyNotificationPublisher: vn.loi.learning.android.family.FamilyNotificationPublisher by lazy {
        vn.loi.learning.android.family.FamilyNotificationPublisher(this)
    }
    val familyReminderReconciler: vn.loi.learning.android.family.ReminderScheduleReconciler by lazy {
        vn.loi.learning.android.family.ReminderScheduleReconciler(
            snapshot = { familyRepository.snapshot.value },
            projector = vn.loi.learning.android.family.FamilyReminderScheduleProjector(
                vn.loi.learning.android.family.CalendarProjectionService(
                    vn.loi.learning.android.family.AstronomicalVietnameseLunarCalendar()
                )
            ),
            scheduler = familyReminderScheduler
        )
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
            defaultQuickPauseHandler = { action ->
                reminderPreferencesController.handleQuickPause(action)
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
    val quickReviewWidgetCoordinator: vn.loi.learning.android.reminder.AndroidQuickReviewWidgetCoordinator by lazy {
        vn.loi.learning.android.reminder.AndroidQuickReviewWidgetCoordinator(
            context = this,
            selector = reminderCandidateSelector,
            ratingBridge = reminderRatingBridge
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
