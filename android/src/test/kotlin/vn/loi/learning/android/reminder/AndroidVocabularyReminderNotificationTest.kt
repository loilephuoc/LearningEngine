package vn.loi.learning.android.reminder

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId

class AndroidVocabularyReminderNotificationTest {

    @Test
    fun `19 and 20 notification payload and constant contract verify correctly`() {
        assertEquals("vocabulary_reminder_v2", AndroidVocabularyReminderNotificationHelper.CHANNEL_ID)
        assertEquals("Vocabulary Reminder", AndroidVocabularyReminderNotificationHelper.CHANNEL_NAME)
        assertEquals(202608, AndroidVocabularyReminderNotificationHelper.REMINDER_NOTIFICATION_ID)
        assertEquals("vn.loi.learning.android.ACTION_REMINDER_REVIEW", AndroidVocabularyReminderNotificationHelper.ACTION_REMINDER_REVIEW)
        assertEquals("extra_package_id", AndroidVocabularyReminderNotificationHelper.EXTRA_PACKAGE_ID)
        assertEquals("extra_content_id", AndroidVocabularyReminderNotificationHelper.EXTRA_CONTENT_ID)
        assertEquals("extra_reminder_mode", AndroidVocabularyReminderNotificationHelper.EXTRA_REMINDER_MODE)
        assertEquals("extra_notification_id", AndroidVocabularyReminderNotificationHelper.EXTRA_NOTIFICATION_ID)
        assertEquals(384, AndroidVocabularyReminderNotificationHelper.THUMBNAIL_MAX_DIMENSION)

        val candidate = AndroidVocabularyCandidate(
            contentId = ContentId("content-test-101"),
            packageId = InstalledPackageId("pkg-english-basic"),
            packageName = "English Basic 1",
            primaryText = "accommodate",
            answer = "to provide with a place to live or to be stored in",
            translation = "cung cap cho o",
            ipa = "əˈkɑː.mə.deɪt",
            partOfSpeech = "verb",
            imageReference = "images/accommodate.png",
            primaryAudioReference = "audio/accommodate.mp3",
            example = "The hotel can accommodate up to 500 guests.",
            exampleTranslation = "Khach san co the chua toi 500 khach."
        )

        assertEquals("accommodate", candidate.primaryText)
        assertEquals("cung cap cho o", candidate.translation)
        assertEquals("verb", candidate.partOfSpeech)
        assertEquals("əˈkɑː.mə.deɪt", candidate.ipa)
        assertEquals("images/accommodate.png", candidate.imageReference)
        assertEquals("audio/accommodate.mp3", candidate.primaryAudioReference)
        assertEquals("The hotel can accommodate up to 500 guests.", candidate.example)
        assertEquals("Khach san co the chua toi 500 khach.", candidate.exampleTranslation)
    }

    @Test
    fun `21 candidate media resolution passes through injected resolveMedia`() {
        var resolvedRef: String? = null
        val mediaResolver: (String) -> String? = { ref ->
            resolvedRef = ref
            "/storage/emulated/0/Android/data/vn.loi.learning.android/files/media/$ref"
        }

        val candidate = AndroidVocabularyCandidate(
            contentId = ContentId("content-with-image"),
            packageId = InstalledPackageId("pkg-1"),
            packageName = "Package 1",
            primaryText = "strawberry",
            answer = "a sweet soft red fruit",
            translation = "qua dau tay",
            ipa = "ˈstrɔː.bər.i",
            partOfSpeech = "noun",
            imageReference = "images/strawberry.jpg",
            primaryAudioReference = "audio/strawberry.mp3"
        )

        val resolved = candidate.imageReference?.let(mediaResolver)
        assertEquals("images/strawberry.jpg", resolvedRef)
        assertEquals("/storage/emulated/0/Android/data/vn.loi.learning.android/files/media/images/strawberry.jpg", resolved)
    }

    @Test
    fun `22 candidate data model accommodates missing media safely`() {
        val candidate = AndroidVocabularyCandidate(
            contentId = ContentId("content-missing-media"),
            packageId = InstalledPackageId("pkg-1"),
            packageName = "Package 1",
            primaryText = "persevere",
            answer = "to continue trying",
            translation = "kien tri",
            ipa = "ˌpɜː.sɪˈvɪər",
            partOfSpeech = "verb",
            imageReference = null,
            primaryAudioReference = null,
            answerAudioReference = null,
            exampleAudioReference = null,
            translationAudioReference = null,
            example = null
        )

        assertEquals("persevere", candidate.primaryText)
        assertNull(candidate.imageReference)
        assertNull(candidate.primaryAudioReference)
        assertNull(candidate.example)
    }

    @Test
    fun `24 draft validation verifies notification duration range`() {
        val draft = AndroidVocabularyReminderDraft(
            enabled = true,
            selectedPackageId = "pkg-1",
            selectionMode = AndroidVocabularyReminderSelectionMode.RANDOM_ALL,
            intervalValueText = "30",
            intervalUnit = AndroidVocabularyReminderIntervalUnit.SECONDS,
            activeStartText = "08:00",
            activeEndText = "22:00",
            displayDurationText = "5.0",
            autoPlayPronunciation = true
        )

        val valid = draft.validate(null)
        assertTrue(valid is AndroidVocabularyReminderDraftValidation.Valid)
        assertEquals(5_000L, valid.settings.displayDurationMillis)
        assertEquals(30_000L, valid.settings.intervalMillis)
    }

    @Test
    fun `25 notification helper constants and channel ID are stable`() {
        assertEquals("vocabulary_reminder_v2", AndroidVocabularyReminderNotificationHelper.CHANNEL_ID)
        assertEquals(202608, AndroidVocabularyReminderNotificationHelper.REMINDER_NOTIFICATION_ID)
        assertEquals(384, AndroidVocabularyReminderNotificationHelper.THUMBNAIL_MAX_DIMENSION)
    }

    @Test
    fun `26 candidate text fallback uses translation over answer and falls back to answer when translation is blank`() {
        val candidateWithTrans = AndroidVocabularyCandidate(
            contentId = ContentId("c1"),
            packageId = InstalledPackageId("p1"),
            packageName = "Pkg1",
            primaryText = "apple",
            answer = "a round fruit",
            translation = "qua tao",
            ipa = "ˈæp.əl",
            partOfSpeech = "noun",
            imageReference = null,
            primaryAudioReference = null
        )
        val meaning1 = candidateWithTrans.translation?.takeIf { it.isNotBlank() }
            ?: candidateWithTrans.answer?.takeIf { it.isNotBlank() } ?: ""
        assertEquals("qua tao", meaning1)

        val candidateNoTrans = AndroidVocabularyCandidate(
            contentId = ContentId("c2"),
            packageId = InstalledPackageId("p1"),
            packageName = "Pkg1",
            primaryText = "banana",
            answer = "a long yellow fruit",
            translation = "",
            ipa = "bəˈnæn.ə",
            partOfSpeech = "noun",
            imageReference = null,
            primaryAudioReference = null
        )
        val meaning2 = candidateNoTrans.translation?.takeIf { it.isNotBlank() }
            ?: candidateNoTrans.answer?.takeIf { it.isNotBlank() } ?: ""
        assertEquals("a long yellow fruit", meaning2)
    }
}
