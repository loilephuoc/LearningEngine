package vn.loi.learning.desktop.notification

import java.nio.file.Path
import java.time.Clock
import java.time.ZoneId
import vn.loi.learning.application.port.MemoryStateQuery
import vn.loi.learning.application.study.ContentLearningStateQueryService
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.infrastructure.LearningApplicationContext

object DesktopVocabularyReminderRuntimeFactory {
    fun create(
        configDirectory: Path,
        applicationContext: LearningApplicationContext,
        onFailure: (Throwable) -> Unit = {}
    ): DesktopVocabularyReminderRuntime? =
        runCatching { createOrNull(configDirectory, applicationContext, onFailure) }
            .onFailure(onFailure)
            .getOrNull()

    private fun createOrNull(
        configDirectory: Path,
        applicationContext: LearningApplicationContext,
        onFailure: (Throwable) -> Unit
    ): DesktopVocabularyReminderRuntime? {
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
        val selector = DesktopVocabularyReminderCandidateSelector(
            installedPackages = readSources.installedPackage,
            packageContents = readSources.packageContent,
            contents = readSources.content,
            learningItems = readSources.learningItem,
            memoryStates = readSources.memoryState,
            contentLearningStates = readSources.contentLearningState,
            learnerId = LearnerId(DEFAULT_LEARNER_ID),
            clock = clock
        )
        return DesktopVocabularyReminderRuntime(
            settingsRepository = DesktopVocabularyReminderSettingsStore(
                configDirectory.resolve(DesktopVocabularyReminderSettingsStore.FILE_NAME)
            ),
            selector = selector,
            sink = NoOpDesktopVocabularyReminderSink,
            delayScheduler = CoroutineDesktopVocabularyReminderDelayScheduler(),
            clock = clock,
            zoneId = ZoneId::systemDefault,
            onFailure = onFailure
        ).also(DesktopVocabularyReminderRuntime::start)
    }

    private const val DEFAULT_LEARNER_ID = "default-learner"
}
