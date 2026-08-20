package vn.loi.learning.desktop.notification

import java.nio.file.Path
import java.time.Clock
import java.time.ZoneId
import vn.loi.learning.application.port.MemoryStateQuery
import vn.loi.learning.application.study.ContentLearningStateQueryService
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.application.port.ContentMediaStorage

object DesktopVocabularyReminderRuntimeFactory {
    fun create(
        configDirectory: Path,
        applicationContext: LearningApplicationContext,
        contentMediaStorage: ContentMediaStorage,
        onFailure: (Throwable) -> Unit = {}
    ): DesktopVocabularyReminderComponents? =
        runCatching { createOrNull(configDirectory, applicationContext, contentMediaStorage, onFailure) }
            .onFailure(onFailure)
            .getOrNull()

    private fun createOrNull(
        configDirectory: Path,
        applicationContext: LearningApplicationContext,
        contentMediaStorage: ContentMediaStorage,
        onFailure: (Throwable) -> Unit
    ): DesktopVocabularyReminderComponents? {
        val packageContents = applicationContext.packageContentQuery ?: return null
        val contents = applicationContext.contentRepository ?: return null
        val learningItems = applicationContext.learningItemRepository ?: return null
        val memoryStates = applicationContext.memoryStateRepository as? MemoryStateQuery ?: return null
        val reviewEvents = applicationContext.reviewEventRepository ?: return null
        val readSources = DesktopVocabularyReminderReadSources(
            installedPackages = applicationContext.installedPackages,
            packageContents = packageContents,
            contents = contents,
            learningItems = learningItems,
            memoryStates = memoryStates,
            contentLearningStates = ContentLearningStateQueryService(learningItems, reviewEvents)
        )
        val clock = Clock.systemUTC()
        val difficultMarkers = DesktopVocabularyReminderDifficultStore(
            configDirectory.resolve(DesktopVocabularyReminderDifficultStore.FILE_NAME)
        )
        fun selector() = DesktopVocabularyReminderCandidateSelector(
            installedPackages = readSources.installedPackage,
            packageContents = readSources.packageContent,
            contents = readSources.content,
            learningItems = readSources.learningItem,
            memoryStates = readSources.memoryState,
            contentLearningStates = readSources.contentLearningState,
            learnerId = LearnerId(DEFAULT_LEARNER_ID),
            clock = clock,
            markedContent = difficultMarkers
        )
        val popupTimer = CoroutineDesktopVocabularyReminderPopupTimer()
        lateinit var runtime: DesktopVocabularyReminderRuntime
        val popupController = DesktopVocabularyReminderPopupController(
            uiDispatcher = SwingDesktopVocabularyReminderUiDispatcher,
            clock = DesktopVocabularyReminderMonotonicClock { System.nanoTime() / 1_000_000L },
            timer = popupTimer,
            audio = DefaultDesktopVocabularyReminderAudioLifecycle(contentMediaStorage),
            difficultMarkers = difficultMarkers,
            autoPlayAuthority = object : DesktopVocabularyReminderAutoPlayAuthority {
                override fun current() = runtime.settings.autoPlayPronunciation
                override fun update(enabled: Boolean) =
                    runtime.updateSettings(runtime.settings.copy(autoPlayPronunciation = enabled))
            },
            layoutAuthority = DesktopVocabularyReminderLayoutAuthority { layout ->
                runtime.updateSettings(runtime.settings.copy(popupLayout = layout))
            },
            snoozeAuthority = DesktopVocabularyReminderSnoozeAuthority { durationMinutes ->
                runtime.snooze(durationMinutes)
            }
        )
        runtime = DesktopVocabularyReminderRuntime(
            settingsRepository = DesktopVocabularyReminderSettingsStore(
                configDirectory.resolve(DesktopVocabularyReminderSettingsStore.FILE_NAME)
            ),
            selector = selector(),
            sink = popupController,
            delayScheduler = CoroutineDesktopVocabularyReminderDelayScheduler(),
            clock = clock,
            zoneId = ZoneId::systemDefault,
            onFailure = onFailure
        )
        return runCatching {
            runtime.setBackgroundMode(false)
            runtime.start()
            DesktopVocabularyReminderComponents(
                runtime,
                popupController,
                DesktopVocabularyReminderSettingsController(
                    runtime = runtime,
                    previewSelector = selector(),
                    popupController = popupController,
                    installedPackages = applicationContext.installedPackages
                )
            )
        }.getOrElse { failure ->
            popupController.close()
            throw failure
        }
    }

    private const val DEFAULT_LEARNER_ID = "default-learner"
}

data class DesktopVocabularyReminderComponents(
    val runtime: DesktopVocabularyReminderRuntime,
    val popupController: DesktopVocabularyReminderPopupController,
    val settingsController: DesktopVocabularyReminderSettingsController
)

private class CoroutineDesktopVocabularyReminderPopupTimer :
    DesktopVocabularyReminderPopupTimer,
    AutoCloseable {
    private val scheduler = CoroutineDesktopVocabularyReminderDelayScheduler()

    override fun schedule(delayMillis: Long, action: () -> Unit) =
        scheduler.schedule(delayMillis, action)

    override fun close() = scheduler.close()
}
