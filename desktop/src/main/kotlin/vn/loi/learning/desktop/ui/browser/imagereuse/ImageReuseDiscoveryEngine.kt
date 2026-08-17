package vn.loi.learning.desktop.ui.browser.imagereuse

import java.nio.file.Files
import java.util.UUID
import vn.loi.learning.application.contentmedia.MediaReferencePolicy
import vn.loi.learning.application.contentpackaging.browser.ContentBrowserEditService
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.domain.content.model.ContentId

object ImageReuseDiscoveryEngine {

    /**
     * Conservatively normalizes English question text for candidate discovery:
     * trims leading/trailing whitespace, converts to lowercase, and collapses multiple spaces.
     */
    fun normalizeQuestion(question: String): String =
        question.trim().lowercase().replace(Regex("\\s+"), " ")

    /**
     * Discovers reusable image candidates from selected source packages for the given target items.
     *
     * Invariants:
     * - Question equality means "this is a candidate worth showing", never automatic equality.
     * - Duplicate source candidates with the same question are never collapsed.
     * - Missing/sentinel/unresolvable source images are excluded.
     * - Source packages are strictly read-only during candidate discovery.
     */
    fun discoverCandidates(
        targetItems: List<PackageContentBrowserItem>,
        sourcePackagesWithItems: Map<ImageReusePackageOption, List<PackageContentBrowserItem>>,
        scope: ImageReuseScope,
        mediaStorage: ContentMediaStorage?
    ): List<ImageReuseTargetItem> {
        val targetPackageNames = targetItems.map { it.packageName.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
        // Step 1: Build in-memory multi-candidate lookup index: NormalizedQuestion -> List<ImageReuseSourceCandidate>
        val sourceCandidatesByQuestion = mutableMapOf<String, MutableList<ImageReuseSourceCandidate>>()

        for ((sourcePkg, items) in sourcePackagesWithItems) {
            if (sourcePkg.name.trim().lowercase() in targetPackageNames) {
                // Strong invariant: Target package must never be treated as a source package
                continue
            }
            for (sourceItem in items) {
                val imageRef = sourceItem.imageRef
                if (imageRef.isNullOrBlank() || MediaReferencePolicy.isNoImageSentinel(imageRef)) {
                    continue
                }
                if (mediaStorage != null) {
                    val resolved = PackageMediaResolver.resolve(sourcePkg.name, imageRef, mediaStorage)
                    if (resolved == null || !Files.exists(resolved) || !Files.isReadable(resolved)) {
                        continue
                    }
                }

                val normalizedKey = normalizeQuestion(sourceItem.questionText)
                if (normalizedKey.isBlank()) continue

                val candidate = ImageReuseSourceCandidate(
                    sourcePackageId = sourcePkg.id,
                    sourcePackageName = sourcePkg.name,
                    sourceContentId = sourceItem.contentId.value,
                    question = sourceItem.questionText,
                    answer = sourceItem.answerText,
                    translation = sourceItem.exampleTranslation.orEmpty().ifBlank { sourceItem.answerText },
                    exampleText = sourceItem.exampleText.orEmpty(),
                    partOfSpeech = sourceItem.partOfSpeech,
                    imageRef = imageRef
                )

                sourceCandidatesByQuestion.getOrPut(normalizedKey) { mutableListOf() }.add(candidate)
            }
        }

        // Step 2: Match against target items according to scope
        val result = mutableListOf<ImageReuseTargetItem>()

        for (targetItem in targetItems) {
            val targetImageRef = targetItem.imageRef
            if (scope == ImageReuseScope.MISSING_IMAGES_ONLY) {
                val hasMissingImage = targetImageRef.isNullOrBlank() ||
                    MediaReferencePolicy.isNoImageSentinel(targetImageRef) ||
                    (mediaStorage != null && PackageMediaResolver.resolve(targetItem.packageName, targetImageRef, mediaStorage) == null)
                if (!hasMissingImage) continue
            }

            val normalizedKey = normalizeQuestion(targetItem.questionText)
            val matchingCandidates = sourceCandidatesByQuestion[normalizedKey].orEmpty()
            if (matchingCandidates.isNotEmpty()) {
                result.add(
                    ImageReuseTargetItem(
                        targetContentId = targetItem.contentId.value,
                        targetLesson = targetItem.lesson,
                        question = targetItem.questionText,
                        answer = targetItem.answerText,
                        translation = targetItem.exampleTranslation.orEmpty().ifBlank { targetItem.answerText },
                        exampleText = targetItem.exampleText.orEmpty(),
                        partOfSpeech = targetItem.partOfSpeech,
                        currentImageRef = targetItem.imageRef,
                        candidates = matchingCandidates
                    )
                )
            }
        }

        return result
    }

    /**
     * Executes the explicit human-approved image reuse operation:
     * 1. Resolves and reads source image file via canonical media storage with package context.
     * 2. Copies image into target package media storage with collision-safe unique naming.
     * 3. Updates only the target Content's image reference.
     * 4. Source package remains 100% untouched.
     */
    fun applyImageReuse(
        targetPackageName: String,
        targetContentId: String,
        sourceCandidate: ImageReuseSourceCandidate,
        mediaStorage: ContentMediaStorage,
        editService: ContentBrowserEditService
    ): String {
        val sourcePath = PackageMediaResolver.resolve(sourceCandidate.sourcePackageName, sourceCandidate.imageRef, mediaStorage)
            ?: throw IllegalStateException("Source image could not be resolved: ${sourceCandidate.imageRef}")

        require(Files.exists(sourcePath) && Files.isReadable(sourcePath)) {
            "Source image file is not readable: $sourcePath"
        }

        val bytes = Files.readAllBytes(sourcePath)
        val sourceFileName = sourcePath.fileName.toString()
        val cleanName = sourceFileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val uniqueName = "${UUID.randomUUID().toString().take(8)}_$cleanName"

        val asset = mediaStorage.store(targetPackageName, uniqueName, bytes)
        editService.replaceContentImage(ContentId(targetContentId), asset.relativePath)
        return asset.relativePath
    }
}
