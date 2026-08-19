package vn.loi.learning.android.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId

class AndroidLockScreenWallpaperRendererTest {

    @Test
    fun renderModelMapsCandidateFieldsAccuratelyWithoutMutatingDomainState() {
        val candidate = AndroidVocabularyCandidate(
            contentId = ContentId("content-1"),
            packageId = InstalledPackageId("pkg-1"),
            packageName = "English Vocabulary",
            primaryText = "sustainable",
            translation = "bền vững",
            answer = "bền vững",
            ipa = "səˈsteɪ.nə.bəl",
            partOfSpeech = "adjective",
            imageReference = "media/sustainable.png",
            primaryAudioReference = "media/sustainable.mp3"
        )

        val model = LockWallpaperVocabularyRenderModel(
            headword = candidate.primaryText,
            ipa = candidate.ipa,
            partOfSpeech = candidate.partOfSpeech,
            meaning = candidate.translation ?: "",
            imageBitmap = null,
            hasPrimaryAudio = !candidate.primaryAudioReference.isNullOrBlank(),
            backgroundBitmap = null,
            wordSize = LockWallpaperWordSize.HUGE,
            vietnameseSize = LockWallpaperVietnameseSize.LARGE,
            imageSize = LockWallpaperImageSize.MAXIMUM,
            cardBackgroundOpacity = 0.80f
        )

        assertEquals("sustainable", model.headword)
        assertEquals("səˈsteɪ.nə.bəl", model.ipa)
        assertEquals("adjective", model.partOfSpeech)
        assertEquals("bền vững", model.meaning)
        assertNull(model.imageBitmap)
        assertNull(model.backgroundBitmap)
        assertTrue(model.hasPrimaryAudio)
        assertEquals(LockWallpaperWordSize.HUGE, model.wordSize)
        assertEquals(LockWallpaperVietnameseSize.LARGE, model.vietnameseSize)
        assertEquals(LockWallpaperImageSize.MAXIMUM, model.imageSize)
        assertEquals(0.80f, model.cardBackgroundOpacity)
    }

    @Test
    fun renderModelHandlesMissingAudioAndMissingIpaPosCleanly() {
        val model = LockWallpaperVocabularyRenderModel(
            headword = "omnipresent",
            ipa = null,
            partOfSpeech = null,
            meaning = "có mặt ở khắp mọi nơi",
            imageBitmap = null,
            hasPrimaryAudio = false,
            backgroundBitmap = null
        )

        assertEquals("omnipresent", model.headword)
        assertNull(model.ipa)
        assertNull(model.partOfSpeech)
        assertNull(model.backgroundBitmap)
        assertFalse(model.hasPrimaryAudio)
        assertEquals(LockWallpaperWordSize.EXTRA_LARGE, model.wordSize)
        assertEquals(LockWallpaperVietnameseSize.MEDIUM, model.vietnameseSize)
        assertEquals(LockWallpaperImageSize.EXTRA_LARGE, model.imageSize)
        assertEquals(0.72f, model.cardBackgroundOpacity)
    }

    @Test
    fun safeZoneProfilePreservesReservedBoundsOnDevice() {
        val profile = LockWallpaperSafeZoneProfile()
        assertEquals(1220, profile.screenWidth)
        assertEquals(2712, profile.screenHeight)

        assertTrue(profile.safeZoneB.top >= profile.weatherReserved.bottom)
        assertTrue(profile.safeZoneB.bottom <= profile.fingerprintReserved.top)
        assertEquals(1120, profile.safeZoneB.right - profile.safeZoneB.left)
        assertEquals(830, profile.safeZoneB.bottom - profile.safeZoneB.top)
    }

    @Test
    fun presentationSnapshotIntegrityGuaranteesSingleCandidateOrigin() {
        val candidate = AndroidVocabularyCandidate(
            contentId = ContentId("content-2"),
            packageId = InstalledPackageId("pkg-2"),
            packageName = "IELTS Core",
            primaryText = "breakthrough",
            translation = "bước đột phá",
            answer = "bước đột phá",
            ipa = "ˈbreɪk.θruː",
            partOfSpeech = "noun",
            imageReference = "media/breakthrough.webp",
            primaryAudioReference = "media/breakthrough.mp3"
        )

        val presentation = PreparedLockWallpaperPresentation(
            sessionToken = 42L,
            candidate = candidate,
            audioSourcePath = "/sdcard/media/breakthrough.mp3"
        )

        assertEquals(42L, presentation.sessionToken)
        assertEquals("breakthrough", presentation.candidate.primaryText)
        assertEquals("bước đột phá", presentation.candidate.translation)
        assertEquals("/sdcard/media/breakthrough.mp3", presentation.audioSourcePath)
        assertFalse(presentation.isAudioPlayed.get())

        // Test exactly-once audio play guard
        assertTrue(presentation.isAudioPlayed.compareAndSet(false, true))
        assertFalse(presentation.isAudioPlayed.compareAndSet(false, true))
    }

    @Test
    fun customBackgroundAndAllSizeSettingsCanBePersistedAndPassedToModel() {
        val settings = AndroidLockScreenVocabularySettings(
            enabled = true,
            selectedPackageId = "pkg-1",
            selectionMode = AndroidLockScreenVocabularyMode.AGAIN_HARD,
            autoPlayPronunciation = true,
            customBackgroundPath = "/data/user/0/vn.loi.learning.android/files/lockscreen_custom_bg.png",
            wordSize = LockWallpaperWordSize.HUGE,
            vietnameseSize = LockWallpaperVietnameseSize.EXTRA_LARGE,
            imageSize = LockWallpaperImageSize.MAXIMUM,
            cardBackgroundOpacity = 0.45f
        )

        assertEquals("/data/user/0/vn.loi.learning.android/files/lockscreen_custom_bg.png", settings.customBackgroundPath)
        assertEquals(LockWallpaperWordSize.HUGE, settings.wordSize)
        assertEquals(LockWallpaperVietnameseSize.EXTRA_LARGE, settings.vietnameseSize)
        assertEquals(LockWallpaperImageSize.MAXIMUM, settings.imageSize)
        assertEquals(0.45f, settings.cardBackgroundOpacity)

        val draft = AndroidLockScreenVocabularyDraft.from(settings)
        assertEquals(settings.customBackgroundPath, draft.customBackgroundPath)
        assertEquals(settings.wordSize, draft.wordSize)
        assertEquals(settings.vietnameseSize, draft.vietnameseSize)
        assertEquals(settings.imageSize, draft.imageSize)
        assertEquals(settings.cardBackgroundOpacity, draft.cardBackgroundOpacity)
        assertEquals(settings, draft.toSettings())
    }

    @Test
    fun imageAndAudioResolutionFailuresGracefullyProduceSafeRenderModel() {
        val candidate = AndroidVocabularyCandidate(
            contentId = ContentId("content-3"),
            packageId = InstalledPackageId("pkg-3"),
            packageName = "Basic English",
            primaryText = "hello",
            translation = "xin chào",
            answer = "xin chào",
            ipa = null,
            partOfSpeech = null,
            imageReference = "nonexistent/image.png",
            primaryAudioReference = null
        )

        val renderModel = LockWallpaperVocabularyRenderModel(
            headword = candidate.primaryText,
            ipa = candidate.ipa,
            partOfSpeech = candidate.partOfSpeech,
            meaning = candidate.translation ?: "",
            imageBitmap = null,
            hasPrimaryAudio = false,
            backgroundBitmap = null
        )

        assertEquals("hello", renderModel.headword)
        assertEquals("xin chào", renderModel.meaning)
        assertNull(renderModel.imageBitmap)
        assertNull(renderModel.backgroundBitmap)
        assertFalse(renderModel.hasPrimaryAudio)
    }

    @Test
    fun contentDrivenLayoutBoundsRemainStrictlyWithinSafeZoneB() {
        val profile = LockWallpaperSafeZoneProfile()
        val safe = profile.safeZoneB

        val model = LockWallpaperVocabularyRenderModel(
            headword = "willing",
            ipa = "ˈwɪlɪŋ",
            partOfSpeech = "adjective",
            meaning = "Sẵn lòng",
            imageBitmap = null,
            hasPrimaryAudio = true,
            backgroundBitmap = null,
            wordSize = LockWallpaperWordSize.HUGE,
            vietnameseSize = LockWallpaperVietnameseSize.HUGE,
            imageSize = LockWallpaperImageSize.MAXIMUM,
            cardBackgroundOpacity = 0.50f
        )

        val safeTop = safe.top
        val safeBottom = safe.bottom
        val safeHeight = safeBottom - safeTop

        assertTrue(safeHeight >= 800)
        assertEquals(1120, safe.width)
    }
}
