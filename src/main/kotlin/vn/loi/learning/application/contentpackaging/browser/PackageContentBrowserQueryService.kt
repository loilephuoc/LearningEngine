package vn.loi.learning.application.contentpackaging.browser

import java.time.Instant
import vn.loi.learning.application.contentpackaging.InstalledPackageQueryService
import vn.loi.learning.application.port.ContentLibraryRepository
import vn.loi.learning.application.port.ContentPackageRepository
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.partofspeech.PartOfSpeechExtractor
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion
import vn.loi.learning.domain.library.repository.InstalledPackageRepository

/**
 * Application service chịu trách nhiệm duy nhất: Truy vấn và dựng dữ liệu read-only cho [PackageContentBrowserItem].
 *
 * Nguyên tắc:
 * 1. [InstalledPackageRepository] là lifecycle authority: Chỉ mở browser cho package ACTIVE hoặc ARCHIVED.
 * 2. Canonical package ownership: Chỉ load Content thuộc về package được truy vấn.
 * 3. Contract representation: ONE ROW PER CONTENT (Option A).
 */
class PackageContentBrowserQueryService(
    private val installedPackageRepository: InstalledPackageRepository? = null,
    private val installedPackages: InstalledPackageQueryService? = null,
    private val contentPackageRepository: ContentPackageRepository,
    private val contentLibraryRepository: ContentLibraryRepository,
    private val contentRepository: ContentRepository,
    private val learningItemRepository: LearningItemRepository
) {

    fun getBrowserItemsForPackage(installedPackageId: InstalledPackageId): List<PackageContentBrowserItem> {
        val installedPkg: InstalledPackage? = installedPackageRepository?.findById(installedPackageId)
            ?: installedPackageRepository?.findAll()?.firstOrNull { it.id == installedPackageId || it.packageId.value == installedPackageId.value }
            ?: findByQueryService(installedPackageId)

        if (installedPkg == null) {
            throw IllegalArgumentException("Package with id '${installedPackageId.value}' is not available in library.")
        }

        // Enforce lifecycle authority: only ACTIVE or ARCHIVED packages are browsable
        val state = installedPkg.state
        if (state != PackageState.ACTIVE && state != PackageState.ARCHIVED) {
            throw IllegalArgumentException("Package with id '${installedPackageId.value}' is in state '$state' and cannot be browsed.")
        }

        // Canonical package ownership resolution
        val targetLibraryIds = mutableSetOf<ContentLibraryId>()
        targetLibraryIds.add(ContentLibraryId(installedPackageId.value))
        targetLibraryIds.add(ContentLibraryId(installedPkg.id.value))
        targetLibraryIds.add(ContentLibraryId(installedPkg.packageId.value))

        val contentPackage = contentPackageRepository.findById(installedPkg.packageId)
        if (contentPackage != null) {
            targetLibraryIds.addAll(contentPackage.libraryIds)
        }

        val targetContentIds = targetLibraryIds
            .mapNotNull { contentLibraryRepository.findById(it) }
            .flatMap { library -> library.contentIds }
            .toSet()

        if (targetContentIds.isEmpty()) {
            return emptyList()
        }

        // Batch query learning items to group by contentId
        val learningItemsByContentId = learningItemRepository.findAllEnabled()
            .filter { item -> item.contentId in targetContentIds }
            .groupBy { item -> item.contentId }

        val packageName = installedPkg.name.value

        // Query content repository and strictly filter by targetContentIds
        val matchingContents = contentRepository.findAll()
            .filter { content -> content.id in targetContentIds }
            .sortedWith(
                compareBy<Content>(
                    { it.metadata.group.orEmpty().lowercase() },
                    { it.metadata.section.orEmpty().lowercase() },
                    { it.metadata.lesson.orEmpty().lowercase() },
                    { it.displayName.lowercase() },
                    { it.id.value }
                )
            )

        return matchingContents.mapIndexed { indexZero, content ->
            val index = indexZero + 1
            val itemsForContent = learningItemsByContentId[content.id] ?: emptyList()
            val learningItemIds = itemsForContent.map { it.id }
            val learningModes = itemsForContent.map { it.mode }

            val questionText = content.text.primaryText
            val answerText = content.text.translatedText.orEmpty()
            val pronunciation = content.text.pronunciation.orEmpty()
            val partOfSpeech = extractPartOfSpeech(content)
            val group = content.metadata.group
            val section = content.metadata.section
            val lesson = content.metadata.lesson ?: "General"

            val hasImage = !content.media.image.isNullOrBlank()
            val primaryAudio = content.media.primaryAudio
            val translatedAudio = content.media.translatedAudio
            val exampleAudio = content.media.exampleAudio
            val audioRef = primaryAudio?.takeIf { it.isNotBlank() }
                ?: translatedAudio?.takeIf { it.isNotBlank() }
                ?: exampleAudio?.takeIf { it.isNotBlank() }
            val hasAudio = !audioRef.isNullOrBlank()

            val searchableText = PackageContentBrowserProjectionPolicy.buildSearchableText(
                questionText = questionText,
                answerText = answerText,
                pronunciation = pronunciation,
                partOfSpeech = partOfSpeech,
                lesson = lesson,
                group = group,
                section = section,
                tags = content.metadata.tags
            )

            val projectedExample = LegacyExampleTranslationProjection.project(
                rawExampleText = content.text.exampleText,
                rawExampleTranslation = content.text.exampleTranslation
            )

            PackageContentBrowserItem(
                index = index,
                contentId = content.id,
                questionText = questionText,
                answerText = answerText,
                pronunciation = pronunciation,
                partOfSpeech = partOfSpeech,
                group = group,
                section = section,
                lesson = lesson,
                packageName = packageName,
                hasImage = hasImage,
                hasAudio = hasAudio,
                imageRef = content.media.image?.takeIf { it.isNotBlank() },
                audioRef = audioRef,
                questionAudioRef = primaryAudio?.takeIf { it.isNotBlank() },
                answerAudioRef = translatedAudio?.takeIf { it.isNotBlank() },
                exampleAudioRef = exampleAudio?.takeIf { it.isNotBlank() },
                translationAudioRef = content.media.exampleTranslatedAudio?.takeIf { it.isNotBlank() },
                exampleText = projectedExample.exampleText,
                exampleTranslation = projectedExample.exampleTranslation,
                learningItemCount = itemsForContent.size,
                learningItemIds = learningItemIds,
                learningModes = learningModes,
                tags = content.metadata.tags,
                searchableText = searchableText,
                partOfSpeechReviewStatus = content.customFields[vn.loi.learning.domain.content.model.ContentFieldId("partOfSpeechReviewStatus")]?.value
            )
        }
    }

    private fun findByQueryService(installedPackageId: InstalledPackageId): InstalledPackage? {
        val query = installedPackages ?: return null
        val item = query.findById(installedPackageId.value)
            ?: query.query().firstOrNull { it.id == installedPackageId.value } ?: return null

        val pkgId = PackageId(item.id)
        val topicId = TopicId.deriveForLegacyPackage(item.name, item.format)

        return InstalledPackage.reconstitute(
            id = installedPackageId,
            libraryId = LibraryId("default-library"),
            packageId = pkgId,
            topicId = topicId,
            name = PackageName(item.name),
            version = PackageVersion(item.version),
            state = PackageState.ACTIVE,
            installedAt = Instant.now(),
            contentCount = 0,
            learningItemCount = 0
        )
    }

    private fun extractPartOfSpeech(content: Content): String {
        return PartOfSpeechExtractor.extract(content)
            .sortedBy { it.source.ordinal }
            .firstNotNullOfOrNull { it.trimmedValue?.takeIf(String::isNotBlank) }
            ?: content.type.name
    }
}
