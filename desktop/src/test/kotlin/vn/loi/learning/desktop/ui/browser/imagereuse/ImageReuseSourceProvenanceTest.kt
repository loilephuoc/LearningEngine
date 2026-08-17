package vn.loi.learning.desktop.ui.browser.imagereuse

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage

class ImageReuseSourceProvenanceTest {

    private fun createBrowserItem(
        contentId: String,
        question: String,
        packageName: String,
        imageRef: String? = null
    ): PackageContentBrowserItem = PackageContentBrowserItem(
        index = 1,
        contentId = ContentId(contentId),
        questionText = question,
        answerText = "nghĩa $question",
        pronunciation = "",
        partOfSpeech = "noun",
        group = null,
        section = null,
        lesson = "Unit 1",
        packageName = packageName,
        hasImage = imageRef != null,
        hasAudio = false,
        imageRef = imageRef,
        audioRef = null,
        exampleText = "Example for $question",
        exampleTranslation = "Ví dụ cho $question",
        learningItemCount = 1,
        learningItemIds = emptyList(),
        learningModes = emptyList(),
        tags = emptySet(),
        searchableText = question
    )

    @Test
    fun `discoverCandidates strictly enforces source provenance across 6+ targets and rejects target package self-matching`() {
        val tempDir = Files.createTempDirectory("provenance_test")
        val storage = JvmContentMediaStorage(tempDir)

        val targetPkgName = "Vocabulary_In_Use_Upper_Intermediate"
        val targetPkgId = "pkg_upper_intermediate"

        val intermediatePkgName = "Vocabulary_in_Use_Intermediate"
        val intermediatePkgId = "pkg_intermediate"

        val elementaryPkgName = "Vocabulary_in_Use_Elementary"
        val elementaryPkgId = "pkg_elementary"

        val words = listOf("a bit", "a good friend", "a la carte", "about", "absent-minded", "abstract")

        // Store images in storage for intermediate
        words.forEachIndexed { idx, word ->
            val filename = "img_inter_$idx.jpg"
            storage.store(intermediatePkgName, filename, byteArrayOf(idx.toByte(), 1, 2))
        }
        // Store images for target and elementary
        words.forEachIndexed { idx, word ->
            storage.store(targetPkgName, "img_upper_$idx.jpg", byteArrayOf(99, idx.toByte()))
            storage.store(elementaryPkgName, "img_elem_$idx.jpg", byteArrayOf(55, idx.toByte()))
        }

        // Target items (Upper Intermediate)
        val targetItems = words.mapIndexed { idx, word ->
            createBrowserItem(
                contentId = "target_$idx",
                question = word,
                packageName = targetPkgName,
                imageRef = null // Missing image
            )
        }

        // Selected Source: Intermediate
        val intermediateOption = ImageReusePackageOption(intermediatePkgId, intermediatePkgName, "1.0.0")
        val intermediateItems = words.mapIndexed { idx, word ->
            createBrowserItem(
                contentId = "inter_$idx",
                question = word,
                packageName = intermediatePkgName,
                imageRef = "img_inter_$idx.jpg"
            )
        }

        // Target package items mistakenly passed as a source
        val upperOption = ImageReusePackageOption(targetPkgId, targetPkgName, "1.0.0")
        val upperItems = words.mapIndexed { idx, word ->
            createBrowserItem(
                contentId = "upper_src_$idx",
                question = word,
                packageName = targetPkgName,
                imageRef = "img_upper_$idx.jpg"
            )
        }

        val sourceMap = mapOf(
            intermediateOption to intermediateItems,
            upperOption to upperItems // Even if target is present in source map, discovery engine must ignore it
        )

        val discovered = ImageReuseDiscoveryEngine.discoverCandidates(
            targetItems = targetItems,
            sourcePackagesWithItems = sourceMap,
            scope = ImageReuseScope.MISSING_IMAGES_ONLY,
            mediaStorage = storage
        )

        assertEquals(6, discovered.size, "All 6 targets must produce matches")

        // Invariant check: EVERY candidate on EVERY target must originate strictly from Intermediate
        discovered.forEachIndexed { targetIdx, targetItem ->
            assertEquals(targetItems[targetIdx].questionText, targetItem.question)
            assertTrue(targetItem.candidates.isNotEmpty(), "Target $targetIdx must have candidates")
            targetItem.candidates.forEach { candidate ->
                assertEquals(
                    intermediatePkgId,
                    candidate.sourcePackageId,
                    "Candidate sourcePackageId for target $targetIdx ('${targetItem.question}') must be Intermediate"
                )
                assertEquals(
                    intermediatePkgName,
                    candidate.sourcePackageName,
                    "Candidate sourcePackageName for target $targetIdx ('${targetItem.question}') must be Intermediate"
                )
            }
        }
    }

    @Test
    fun `ImageReuseReviewStage Review enforces allowedSourcePackageIds invariant and throws on breach`() {
        val candidateValid = ImageReuseSourceCandidate(
            sourcePackageId = "pkg_intermediate",
            sourcePackageName = "Vocabulary_in_Use_Intermediate",
            sourceContentId = "s1",
            question = "agree",
            answer = "đồng ý",
            translation = "đồng ý",
            exampleText = "I agree",
            partOfSpeech = "verb",
            imageRef = "agree.jpg"
        )

        val candidateInvalid = ImageReuseSourceCandidate(
            sourcePackageId = "pkg_upper_intermediate", // Leak
            sourcePackageName = "Vocabulary_In_Use_Upper_Intermediate",
            sourceContentId = "s2",
            question = "break",
            answer = "làm vỡ",
            translation = "làm vỡ",
            exampleText = "break it",
            partOfSpeech = "verb",
            imageRef = "break.jpg"
        )

        val targetItemValid = ImageReuseTargetItem(
            targetContentId = "t1",
            targetLesson = "Unit 1",
            question = "agree",
            answer = "đồng ý",
            translation = "đồng ý",
            exampleText = "I agree",
            partOfSpeech = "verb",
            currentImageRef = null,
            candidates = listOf(candidateValid)
        )

        val targetItemWithLeak = ImageReuseTargetItem(
            targetContentId = "t2",
            targetLesson = "Unit 2",
            question = "break",
            answer = "làm vỡ",
            translation = "làm vỡ",
            exampleText = "break it",
            partOfSpeech = "verb",
            currentImageRef = null,
            candidates = listOf(candidateInvalid)
        )

        val allowed = setOf("pkg_intermediate")

        // Valid setup passes without error
        val validStage = ImageReuseReviewStage.Review(
            targetItems = listOf(targetItemValid),
            allowedSourcePackageIds = allowed
        )
        assertEquals(1, validStage.targetItems.size)

        // Invalid setup containing candidate with leaked package ID fails fast
        assertFailsWith<IllegalArgumentException> {
            ImageReuseReviewStage.Review(
                targetItems = listOf(targetItemValid, targetItemWithLeak),
                allowedSourcePackageIds = allowed
            )
        }
    }
}
