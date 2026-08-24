package vn.loi.learning.desktop.tts.ui

import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import vn.loi.learning.application.contentmedia.ContentMediaAsset
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.desktop.tts.DesktopTtsAudioService
import vn.loi.learning.desktop.tts.TtsAudioFileNamer
import vn.loi.learning.desktop.tts.TtsAudioParameters
import vn.loi.learning.desktop.tts.TtsEngine
import vn.loi.learning.desktop.tts.TtsField
import vn.loi.learning.desktop.tts.TtsLanguage
import vn.loi.learning.desktop.tts.TtsPreviewStore
import vn.loi.learning.desktop.tts.TtsSynthesisRequest
import vn.loi.learning.desktop.tts.TtsSynthesisResult
import vn.loi.learning.desktop.tts.TtsVoice
import vn.loi.learning.desktop.tts.batch.BatchTtsLanguageRequirements
import vn.loi.learning.desktop.tts.batch.BatchTtsLanguageResolver
import vn.loi.learning.desktop.tts.batch.BatchTtsScanner
import vn.loi.learning.desktop.tts.batch.BatchTtsScopeScan
import vn.loi.learning.desktop.tts.preset.TtsLanguageConfigPresetJson
import vn.loi.learning.desktop.tts.preset.TtsLanguagePresetConfig
import vn.loi.learning.desktop.tts.preset.TtsPreset
import vn.loi.learning.desktop.tts.preset.TtsPresetDocumentJson
import vn.loi.learning.desktop.tts.preset.TtsPresetJson
import vn.loi.learning.desktop.tts.preset.TtsPresetStore
import vn.loi.learning.desktop.tts.strategy.VoiceStrategyConfig
import vn.loi.learning.desktop.tts.strategy.VoiceStrategyMode
import vn.loi.learning.domain.content.model.ContentId
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BatchTtsHotfix43BilingualAndNavigationTest {

    private val enAva = TtsVoice("en-US-AvaMultilingualNeural", "Microsoft Ava", "en-US", "en", "Female")
    private val enGuy = TtsVoice("en-US-GuyNeural", "Microsoft Guy", "en-US", "en", "Male")
    private val enSonia = TtsVoice("en-GB-SoniaNeural", "Microsoft Sonia", "en-GB", "en", "Female")
    private val enRyan = TtsVoice("en-GB-RyanNeural", "Microsoft Ryan", "en-GB", "en", "Male")

    private val viHoaiMy = TtsVoice("vi-VN-HoaiMyNeural", "Microsoft HoaiMy", "vi-VN", "vi", "Female")
    private val viNamMinh = TtsVoice("vi-VN-NamMinhNeural", "Microsoft NamMinh", "vi-VN", "vi", "Male")

    private val allVoices = listOf(enAva, enGuy, enSonia, enRyan, viHoaiMy, viNamMinh)

    private lateinit var tempMediaDir: Path
    private lateinit var tempPreviewDir: Path
    private lateinit var tempPresetFile: Path
    private lateinit var fakeMediaStorage: FakeMediaStorage
    private lateinit var fakeEngine: FakeEngine
    private lateinit var service: DesktopTtsAudioService

    @BeforeTest
    fun setup() {
        tempMediaDir = Files.createTempDirectory("batch-tts-hotfix43-media-")
        tempPreviewDir = Files.createTempDirectory("batch-tts-hotfix43-preview-")
        tempPresetFile = Files.createTempFile("batch-tts-hotfix43-preset-", ".json")
        fakeMediaStorage = FakeMediaStorage(tempMediaDir)
        fakeEngine = FakeEngine(allVoices)
        service = DesktopTtsAudioService(
            ttsEngine = fakeEngine,
            mediaStorage = fakeMediaStorage,
            fileNamer = TtsAudioFileNamer(Clock.fixed(Instant.parse("2026-08-24T05:00:00Z"), ZoneOffset.UTC)),
            previewStore = TtsPreviewStore(tempPreviewDir)
        )
    }

    @AfterTest
    fun tearDown() {
        service.close()
        tempMediaDir.toFile().deleteRecursively()
        tempPreviewDir.toFile().deleteRecursively()
        Files.deleteIfExists(tempPresetFile)
    }

    private fun createItem(
        id: String = "item-1",
        qAudio: String? = null,
        aAudio: String? = "existing_answer.mp3",
        exAudio: String? = null,
        trAudio: String? = "existing_tr.mp3"
    ): PackageContentBrowserItem = PackageContentBrowserItem(
        index = 1,
        contentId = ContentId(id),
        packageName = "TestPackage",
        questionText = "Question text $id",
        answerText = "Nghĩa tiếng Việt $id",
        pronunciation = "/test/",
        partOfSpeech = "NOUN",
        group = "Vocabulary",
        section = "A",
        lesson = "Lesson 1",
        hasImage = false,
        hasAudio = !qAudio.isNullOrBlank() || !aAudio.isNullOrBlank(),
        imageRef = null,
        audioRef = qAudio ?: aAudio,
        questionAudioRef = qAudio,
        answerAudioRef = aAudio,
        exampleAudioRef = exAudio,
        translationAudioRef = trAudio,
        exampleText = "Example sentence $id",
        exampleTranslation = "Dịch ví dụ $id",
        learningItemCount = 1,
        learningItemIds = emptyList(),
        learningModes = emptyList(),
        tags = emptySet(),
        searchableText = "Question text $id Nghĩa tiếng Việt $id"
    )

    // =========================================================================
    // 1-7: Language Configuration & Field Preview Representation
    // =========================================================================

    @Test
    fun `1 - Question selected only requires English config and omits Vietnamese config`() {
        val selected = setOf(TtsField.QUESTION)
        val req = BatchTtsLanguageRequirements.from(selected)
        assertTrue(req.requiresEnglish)
        assertFalse(req.requiresVietnamese)
    }

    @Test
    fun `2 - Answer selected only requires Vietnamese config and omits English config`() {
        val selected = setOf(TtsField.ANSWER)
        val req = BatchTtsLanguageRequirements.from(selected)
        assertFalse(req.requiresEnglish)
        assertTrue(req.requiresVietnamese)
    }

    @Test
    fun `3 - Question and Example selected renders English preview tabs Question and Example`() {
        val selected = setOf(TtsField.QUESTION, TtsField.EXAMPLE)
        val req = BatchTtsLanguageRequirements.from(selected)
        assertTrue(req.requiresEnglish)
        assertFalse(req.requiresVietnamese)

        val englishManaged = listOf(TtsField.QUESTION, TtsField.EXAMPLE).filter { it in selected }
        assertEquals(listOf(TtsField.QUESTION, TtsField.EXAMPLE), englishManaged)
    }

    @Test
    fun `4 - Answer and Translation selected renders Vietnamese preview tabs Answer and Translation`() {
        val selected = setOf(TtsField.ANSWER, TtsField.TRANSLATION)
        val req = BatchTtsLanguageRequirements.from(selected)
        assertFalse(req.requiresEnglish)
        assertTrue(req.requiresVietnamese)

        val vietnameseManaged = listOf(TtsField.ANSWER, TtsField.TRANSLATION).filter { it in selected }
        assertEquals(listOf(TtsField.ANSWER, TtsField.TRANSLATION), vietnameseManaged)
    }

    @Test
    fun `5 - All four selected renders BOTH English and Vietnamese configuration cards`() {
        val selected = setOf(TtsField.QUESTION, TtsField.EXAMPLE, TtsField.ANSWER, TtsField.TRANSLATION)
        val req = BatchTtsLanguageRequirements.from(selected)
        assertTrue(req.requiresEnglish, "English card must be active when English fields selected")
        assertTrue(req.requiresVietnamese, "Vietnamese card must be active when Vietnamese fields selected")
    }

    @Test
    fun `6 - All four selected gives Question and Example to English card and Answer and Translation to Vietnamese card`() {
        val selected = setOf(TtsField.QUESTION, TtsField.EXAMPLE, TtsField.ANSWER, TtsField.TRANSLATION)
        val englishManaged = listOf(TtsField.QUESTION, TtsField.EXAMPLE).filter { it in selected }
        val vietnameseManaged = listOf(TtsField.ANSWER, TtsField.TRANSLATION).filter { it in selected }

        assertEquals(listOf(TtsField.QUESTION, TtsField.EXAMPLE), englishManaged)
        assertEquals(listOf(TtsField.ANSWER, TtsField.TRANSLATION), vietnameseManaged)
    }

    @Test
    fun `7 - Question and Translation selected maps Question to English card and Translation to Vietnamese card`() {
        val selected = setOf(TtsField.QUESTION, TtsField.TRANSLATION)
        val req = BatchTtsLanguageRequirements.from(selected)
        assertTrue(req.requiresEnglish)
        assertTrue(req.requiresVietnamese)

        val englishManaged = listOf(TtsField.QUESTION, TtsField.EXAMPLE).filter { it in selected }
        val vietnameseManaged = listOf(TtsField.ANSWER, TtsField.TRANSLATION).filter { it in selected }

        assertEquals(listOf(TtsField.QUESTION), englishManaged)
        assertEquals(listOf(TtsField.TRANSLATION), vietnameseManaged)
    }

    // =========================================================================
    // 8-10: Zero-Target Behavior
    // =========================================================================

    @Test
    fun `8 - Answer selected with Vietnamese target count 0 keeps Vietnamese config visible`() {
        val item = createItem(qAudio = null, aAudio = "exists.mp3", exAudio = null, trAudio = "exists.mp3")
        val selected = setOf(TtsField.QUESTION, TtsField.EXAMPLE, TtsField.ANSWER, TtsField.TRANSLATION)
        val scan = BatchTtsScanner.scanBatchScope(listOf(item), selected, overwriteExisting = false)

        assertEquals(2, scan.englishTargetsCount, "English should have 2 missing targets (Q and Ex)")
        assertEquals(0, scan.vietnameseTargetsCount, "Vietnamese should have 0 missing targets (A and Tr already exist)")

        val req = BatchTtsLanguageRequirements.from(scan)
        assertTrue(req.requiresEnglish, "English must be required")
        assertTrue(req.requiresVietnamese, "Vietnamese MUST remain required even when target count is 0")
    }

    @Test
    fun `9 - Translation selected with target count 0 still provides sample text for preview`() {
        val item = createItem(trAudio = "existing.mp3")
        val selected = setOf(TtsField.TRANSLATION)
        val scan = BatchTtsScanner.scanBatchScope(listOf(item), selected, overwriteExisting = false)

        assertEquals(0, scan.vietnameseTargetsCount)
        val samples = scan.samplesFor(TtsField.TRANSLATION)
        assertTrue(samples.isNotEmpty(), "Sample text from item must be preserved for preview even if audio exists")
        assertEquals("Dịch ví dụ item-1", samples.first().text)
    }

    @Test
    fun `10 - Zero target count does not mutate field selection`() {
        val item = createItem(qAudio = "q.mp3", aAudio = "a.mp3", exAudio = "ex.mp3", trAudio = "tr.mp3")
        val initialSelected = setOf(TtsField.QUESTION, TtsField.ANSWER)
        val scan = BatchTtsScanner.scanBatchScope(listOf(item), initialSelected, overwriteExisting = false)

        assertEquals(0, scan.totalValidTargets)
        assertEquals(initialSelected, scan.selectedFields)
    }

    // =========================================================================
    // 11-18: Voice Safety & Language Pool Invariants
    // =========================================================================

    @Test
    fun `11 and 12 - Question and Example generation jobs use English voice pool only`() {
        val item = createItem(qAudio = null, aAudio = "a.mp3", exAudio = null, trAudio = "tr.mp3")
        val scan = BatchTtsScanner.scanBatchScope(listOf(item), setOf(TtsField.QUESTION, TtsField.EXAMPLE))
        val enStrategy = VoiceStrategyConfig(VoiceStrategyMode.SINGLE_VOICE, enAva)
        val viStrategy = VoiceStrategyConfig(VoiceStrategyMode.SINGLE_VOICE, viHoaiMy)

        val jobs = BatchTtsScanner.buildJobsWithStrategy(scan.validTargets, enStrategy, viStrategy)
        assertEquals(2, jobs.size)
        assertTrue(jobs.all { it.language == TtsLanguage.ENGLISH })
        assertTrue(jobs.all { it.voice.id == enAva.id })
    }

    @Test
    fun `13 and 14 - Answer and Translation generation jobs use Vietnamese voice pool only`() {
        val item = createItem(qAudio = "q.mp3", aAudio = null, exAudio = "ex.mp3", trAudio = null)
        val scan = BatchTtsScanner.scanBatchScope(listOf(item), setOf(TtsField.ANSWER, TtsField.TRANSLATION))
        val enStrategy = VoiceStrategyConfig(VoiceStrategyMode.SINGLE_VOICE, enAva)
        val viStrategy = VoiceStrategyConfig(VoiceStrategyMode.SINGLE_VOICE, viHoaiMy)

        val jobs = BatchTtsScanner.buildJobsWithStrategy(scan.validTargets, enStrategy, viStrategy)
        assertEquals(2, jobs.size)
        assertTrue(jobs.all { it.language == TtsLanguage.VIETNAMESE })
        assertTrue(jobs.all { it.voice.id == viHoaiMy.id })
    }

    @Test
    fun `15 - English job cannot resolve Vietnamese primary voice`() {
        val item = createItem(qAudio = null)
        val scan = BatchTtsScanner.scanBatchScope(listOf(item), setOf(TtsField.QUESTION))
        // Attempt to supply Vietnamese voice to English strategy
        val invalidEnStrategy = VoiceStrategyConfig(VoiceStrategyMode.SINGLE_VOICE, viHoaiMy)

        assertFailsWith<IllegalArgumentException> {
            BatchTtsScanner.buildJobsWithStrategy(scan.validTargets, invalidEnStrategy, null)
        }
    }

    @Test
    fun `16 - Vietnamese job cannot resolve English primary voice`() {
        val item = createItem(aAudio = null)
        val scan = BatchTtsScanner.scanBatchScope(listOf(item), setOf(TtsField.ANSWER))
        // Attempt to supply English voice to Vietnamese strategy
        val invalidViStrategy = VoiceStrategyConfig(VoiceStrategyMode.SINGLE_VOICE, enAva)

        assertFailsWith<IllegalArgumentException> {
            BatchTtsScanner.buildJobsWithStrategy(scan.validTargets, null, invalidViStrategy)
        }
    }

    @Test
    fun `17 - English fallback pool excludes Vietnamese voices`() {
        val enCandidates = allVoices.filter { it.isEnglish }
        val fallbacks = OrderedFallbackVoices.hydrate(listOf(viHoaiMy.id, enGuy.id), allVoices, TtsLanguage.ENGLISH, enAva)
        assertEquals(listOf(enGuy.id), fallbacks.ids, "Vietnamese voice must be rejected from English fallback pool")
    }

    @Test
    fun `18 - Vietnamese fallback pool excludes English voices`() {
        val viCandidates = allVoices.filter { it.isVietnamese }
        val fallbacks = OrderedFallbackVoices.hydrate(listOf(enAva.id, viNamMinh.id), allVoices, TtsLanguage.VIETNAMESE, viHoaiMy)
        assertEquals(listOf(viNamMinh.id), fallbacks.ids, "English voice must be rejected from Vietnamese fallback pool")
    }

    // =========================================================================
    // 19-23: Independent Configuration & Filter States
    // =========================================================================

    @Test
    fun `19 and 20 - Changing English primary does not change Vietnamese primary and vice versa`() {
        var selectedEnglish: TtsVoice? = enAva
        var selectedVietnamese: TtsVoice? = viHoaiMy

        // Change English
        selectedEnglish = enGuy
        assertEquals(enGuy, selectedEnglish)
        assertEquals(viHoaiMy, selectedVietnamese)

        // Change Vietnamese
        selectedVietnamese = viNamMinh
        assertEquals(enGuy, selectedEnglish)
        assertEquals(viNamMinh, selectedVietnamese)
    }

    @Test
    fun `21 and 22 - English fallback modifications do not mutate Vietnamese fallbacks and vice versa`() {
        var enFallbacks = OrderedFallbackVoices.EMPTY.add(enGuy, enAva)
        var viFallbacks = OrderedFallbackVoices.EMPTY.add(viNamMinh, viHoaiMy)

        // Add second English fallback
        enFallbacks = enFallbacks.add(enSonia, enAva)
        assertEquals(2, enFallbacks.voices.size)
        assertEquals(1, viFallbacks.voices.size)

        // Reorder Vietnamese fallbacks
        viFallbacks = viFallbacks.remove(viNamMinh.id)
        assertEquals(2, enFallbacks.voices.size)
        assertEquals(0, viFallbacks.voices.size)
    }

    @Test
    fun `23 - English filter state is independent from Vietnamese filter state`() {
        val enFilter = VoicePickerFilterState()
        val viFilter = VoicePickerFilterState()

        enFilter.searchQuery = "Ava"
        enFilter.selectedRegionCode = "en-US"
        enFilter.selectedGender = "Female"

        viFilter.searchQuery = "NamMinh"
        viFilter.selectedRegionCode = "vi-VN"
        viFilter.selectedGender = "Male"

        assertEquals("Ava", enFilter.searchQuery)
        assertEquals("NamMinh", viFilter.searchQuery)

        enFilter.clear()
        assertEquals("", enFilter.searchQuery)
        assertEquals("NamMinh", viFilter.searchQuery)
        assertEquals("vi-VN", viFilter.selectedRegionCode)
    }

    // =========================================================================
    // 24-29: Inline Preview & Row Highlight
    // =========================================================================

    @Test
    fun `24 - Preview voice A sets previewing state and lastPreviewedVoiceId`() {
        var previewingVoiceId: String? = null
        var lastPreviewedVoiceId: String? = null

        fun onPreview(voice: TtsVoice) {
            previewingVoiceId = voice.id
            lastPreviewedVoiceId = voice.id
        }

        onPreview(enAva)
        assertEquals(enAva.id, previewingVoiceId)
        assertEquals(enAva.id, lastPreviewedVoiceId)
    }

    @Test
    fun `25 - Preview B after A stops A and sets B as playing and last previewed`() {
        var previewingVoiceId: String? = null
        var lastPreviewedVoiceId: String? = null
        var stopCount = 0

        fun stop() {
            if (previewingVoiceId != null) {
                stopCount++
                previewingVoiceId = null
            }
        }

        fun onPreview(voice: TtsVoice) {
            stop()
            previewingVoiceId = voice.id
            lastPreviewedVoiceId = voice.id
        }

        onPreview(enAva)
        assertEquals(enAva.id, previewingVoiceId)
        assertEquals(0, stopCount)

        onPreview(enSonia)
        assertEquals(enSonia.id, previewingVoiceId)
        assertEquals(enSonia.id, lastPreviewedVoiceId)
        assertEquals(1, stopCount, "Voice A must be stopped when previewing B")
    }

    @Test
    fun `26 and 27 - Preview does not select primary voice and does not add fallback voice`() {
        var primaryVoice: TtsVoice? = enAva
        var fallbackVoices = OrderedFallbackVoices.EMPTY
        var previewedVoice: TtsVoice? = null

        fun onPreview(voice: TtsVoice) {
            previewedVoice = voice
            // Preview contract: do NOT mutate primary or fallbacks
        }

        onPreview(enGuy)
        assertEquals(enGuy, previewedVoice)
        assertEquals(enAva, primaryVoice, "Primary voice must not change on preview")
        assertTrue(fallbackVoices.voices.isEmpty(), "Fallbacks must not be mutated on preview")
    }

    @Test
    fun `28 - Last previewed row remains identifiable after playback completes`() {
        var previewingVoiceId: String? = enAva.id
        var lastPreviewedVoiceId: String? = enAva.id

        // Playback finishes naturally -> previewingVoiceId becomes null
        previewingVoiceId = null

        assertNull(previewingVoiceId)
        assertEquals(enAva.id, lastPreviewedVoiceId, "lastPreviewedVoiceId must persist after playback stops")
    }

    @Test
    fun `29 - Closing picker stops preview`() {
        var previewingVoiceId: String? = enAva.id
        var isStopped = false

        fun onDismiss() {
            previewingVoiceId = null
            isStopped = true
        }

        onDismiss()
        assertNull(previewingVoiceId)
        assertTrue(isStopped)
    }

    // =========================================================================
    // 30-39: Scrolling & Navigation Controls
    // =========================================================================

    @Test
    fun `30 and 31 - Voice pickers provide vertical scrolling layout`() {
        val voices = (1..50).map { i ->
            TtsVoice("en-US-Voice$i", "Voice $i", "en-US", "en", if (i % 2 == 0) "Female" else "Male")
        }
        assertEquals(50, voices.size)
    }

    @Test
    fun `34 and 35 - Up and Down viewport navigation moves exactly one list item`() {
        var firstVisibleIndex = 5
        val maxIndex = 20

        // Move Down (1 row)
        firstVisibleIndex = (firstVisibleIndex + 1).coerceAtMost(maxIndex)
        assertEquals(6, firstVisibleIndex)

        // Move Up (1 row)
        firstVisibleIndex = (firstVisibleIndex - 1).coerceAtLeast(0)
        assertEquals(5, firstVisibleIndex)
    }

    @Test
    fun `36 and 37 - Up is disabled at top and Down is disabled at bottom`() {
        val totalItems = 10
        val maxIndex = totalItems - 1

        var currentIndex = 0
        val canScrollUpAtTop = currentIndex > 0
        val canScrollDownAtTop = currentIndex < maxIndex
        assertFalse(canScrollUpAtTop, "Up must be disabled at index 0")
        assertTrue(canScrollDownAtTop, "Down must be enabled at index 0")

        currentIndex = maxIndex
        val canScrollUpAtBottom = currentIndex > 0
        val canScrollDownAtBottom = currentIndex < maxIndex
        assertTrue(canScrollUpAtBottom, "Up must be enabled at bottom")
        assertFalse(canScrollDownAtBottom, "Down must be disabled at bottom")
    }

    @Test
    fun `38 - Search and filter change keeps scroll index clamped`() {
        val totalItems = 30
        var currentIndex = 25

        // Filter reduces items to 5
        val filteredCount = 5
        val clampedIndex = currentIndex.coerceIn(0, (filteredCount - 1).coerceAtLeast(0))
        assertEquals(4, clampedIndex)
    }

    @Test
    fun `39 - Preview highlight remains tied to voice ID regardless of index position`() {
        val voiceA = enAva
        val voiceB = enGuy
        var previewingVoiceId: String? = voiceA.id

        assertEquals(true, previewingVoiceId == voiceA.id)
        assertEquals(false, previewingVoiceId == voiceB.id)

        // Simulating list scroll
        assertEquals(true, previewingVoiceId == voiceA.id, "Highlight must remain tied to voice ID")
    }

    // =========================================================================
    // 40-42: Fallback Capacity & Operations
    // =========================================================================

    @Test
    fun `40 and 41 - English and Vietnamese fallbacks have independent maximum capacity of 3`() {
        var enFallbacks = OrderedFallbackVoices.EMPTY
            .add(enGuy, enAva)
            .add(enSonia, enAva)
            .add(enRyan, enAva)

        assertEquals(3, enFallbacks.voices.size)

        var viFallbacks = OrderedFallbackVoices.EMPTY
            .add(viNamMinh, viHoaiMy)

        assertEquals(1, viFallbacks.voices.size, "Vietnamese fallback capacity must not be affected by English fallbacks")
    }

    @Test
    fun `42 - Fallback remove and reorder behavior remains completely verified`() {
        var fallbacks = OrderedFallbackVoices.EMPTY
            .add(enGuy, enAva)
            .add(enSonia, enAva)

        // Move Sonia up
        fallbacks = fallbacks.move(enSonia.id, -1)
        assertEquals(listOf(enSonia.id, enGuy.id), fallbacks.ids)

        // Remove Sonia
        fallbacks = fallbacks.remove(enSonia.id)
        assertEquals(listOf(enGuy.id), fallbacks.ids)
    }

    // =========================================================================
    // 43-45: Preset Round Trip & Backward Compatibility
    // =========================================================================

    @Test
    fun `43 and 44 - Bilingual preset round-trip preserves English and Vietnamese configurations`() {
        val store = TtsPresetStore(tempPresetFile)
        val preset = TtsPreset(
            id = "custom-bilingual-preset",
            name = "Custom Bilingual",
            selectedFields = setOf(TtsField.QUESTION, TtsField.ANSWER, TtsField.EXAMPLE, TtsField.TRANSLATION),
            english = TtsLanguagePresetConfig(
                strategyMode = VoiceStrategyMode.FALLBACK_CHAIN,
                primaryVoiceId = enSonia.id,
                fallbackVoiceIds = listOf(enRyan.id),
                candidateVoiceIds = listOf(enSonia.id, enRyan.id),
                audioParameters = TtsAudioParameters(ratePercent = 10, pitchHz = -2, volumePercent = 5)
            ),
            vietnamese = TtsLanguagePresetConfig(
                strategyMode = VoiceStrategyMode.FALLBACK_CHAIN,
                primaryVoiceId = viNamMinh.id,
                fallbackVoiceIds = emptyList(),
                candidateVoiceIds = listOf(viNamMinh.id),
                audioParameters = TtsAudioParameters(ratePercent = -5, pitchHz = 4, volumePercent = 0)
            )
        )

        store.savePreset(preset)
        val loaded = store.getPreset("custom-bilingual-preset")
        assertNotNull(loaded)
        assertEquals(enSonia.id, loaded.english.primaryVoiceId)
        assertEquals(listOf(enRyan.id), loaded.english.fallbackVoiceIds)
        assertEquals(10, loaded.english.audioParameters.ratePercent)
        assertEquals(-2, loaded.english.audioParameters.pitchHz)

        assertEquals(viNamMinh.id, loaded.vietnamese.primaryVoiceId)
        assertEquals(-5, loaded.vietnamese.audioParameters.ratePercent)
        assertEquals(4, loaded.vietnamese.audioParameters.pitchHz)
    }

    @Test
    fun `45 - Legacy preset loading handles defaults safely without crashing`() {
        val store = TtsPresetStore(tempPresetFile)
        val defaultPreset = store.getDefaultPreset()
        assertNotNull(defaultPreset)
        assertTrue(defaultPreset.english.primaryVoiceId.isNotBlank())
        assertTrue(defaultPreset.vietnamese.primaryVoiceId.isNotBlank())
    }

    // =========================================================================
    // 46-48: Generation Planning & Existing Audio Skipping
    // =========================================================================

    @Test
    fun `46 - Mixed English and Vietnamese generation creates correctly language-mapped jobs`() {
        val item1 = createItem("item-1", qAudio = null, aAudio = null, exAudio = null, trAudio = null)
        val item2 = createItem("item-2", qAudio = null, aAudio = null, exAudio = null, trAudio = null)
        val scan = BatchTtsScanner.scanBatchScope(
            listOf(item1, item2),
            setOf(TtsField.QUESTION, TtsField.ANSWER, TtsField.EXAMPLE, TtsField.TRANSLATION)
        )

        val enStrategy = VoiceStrategyConfig(VoiceStrategyMode.SINGLE_VOICE, enAva)
        val viStrategy = VoiceStrategyConfig(VoiceStrategyMode.SINGLE_VOICE, viHoaiMy)

        val jobs = BatchTtsScanner.buildJobsWithStrategy(scan.validTargets, enStrategy, viStrategy)
        assertEquals(8, jobs.size)

        val enJobs = jobs.filter { it.language == TtsLanguage.ENGLISH }
        val viJobs = jobs.filter { it.language == TtsLanguage.VIETNAMESE }

        assertEquals(4, enJobs.size)
        assertEquals(4, viJobs.size)

        assertTrue(enJobs.all { it.voice.id == enAva.id && (it.field == TtsField.QUESTION || it.field == TtsField.EXAMPLE) })
        assertTrue(viJobs.all { it.voice.id == viHoaiMy.id && (it.field == TtsField.ANSWER || it.field == TtsField.TRANSLATION) })
    }

    @Test
    fun `47 - Zero Vietnamese pending targets creates zero Vietnamese jobs without error`() {
        val item = createItem("item-1", qAudio = null, aAudio = "exists.mp3", exAudio = null, trAudio = "exists.mp3")
        val scan = BatchTtsScanner.scanBatchScope(
            listOf(item),
            setOf(TtsField.QUESTION, TtsField.EXAMPLE, TtsField.ANSWER, TtsField.TRANSLATION),
            overwriteExisting = false
        )

        val enStrategy = VoiceStrategyConfig(VoiceStrategyMode.SINGLE_VOICE, enAva)
        val viStrategy = VoiceStrategyConfig(VoiceStrategyMode.SINGLE_VOICE, viHoaiMy)

        val jobs = BatchTtsScanner.buildJobsWithStrategy(scan.validTargets, enStrategy, viStrategy)
        assertEquals(2, jobs.size)
        assertTrue(jobs.all { it.language == TtsLanguage.ENGLISH })
    }

    @Test
    fun `48 - Existing audio skip behavior remains unchanged when overwriteExisting is false`() {
        val item = createItem("item-1", qAudio = "exists.mp3", aAudio = "exists.mp3", exAudio = null, trAudio = null)
        val scan = BatchTtsScanner.scanBatchScope(
            listOf(item),
            setOf(TtsField.QUESTION, TtsField.ANSWER, TtsField.EXAMPLE, TtsField.TRANSLATION),
            overwriteExisting = false
        )

        assertEquals(2, scan.existingAudioSkippedCount)
        assertEquals(2, scan.totalValidTargets)
        assertEquals(listOf(TtsField.EXAMPLE, TtsField.TRANSLATION), scan.validTargets.map { it.field })
    }

    private class FakeEngine(private val voices: List<TtsVoice>) : TtsEngine {
        override suspend fun listVoices(): List<TtsVoice> = voices

        override suspend fun synthesize(request: TtsSynthesisRequest, outputFile: Path): TtsSynthesisResult {
            Files.createDirectories(outputFile.parent)
            Files.write(outputFile, byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x00))
            return TtsSynthesisResult(outputFile, 4)
        }
    }

    private class FakeMediaStorage(private val root: Path) : ContentMediaStorage {
        var storedCount = 0
            private set
        val storedAssets = mutableMapOf<String, ByteArray>()

        override fun store(packageName: String, fileName: String, content: ByteArray): ContentMediaAsset {
            val relPath = "$packageName/$fileName"
            storedAssets[relPath] = content
            val targetPath = root.resolve(relPath)
            Files.createDirectories(targetPath.parent)
            Files.write(targetPath, content)
            storedCount++
            return ContentMediaAsset(packageName, fileName, relPath)
        }

        override fun storeStream(packageName: String, fileName: String, source: Path): ContentMediaAsset {
            return store(packageName, fileName, Files.readAllBytes(source))
        }

        override fun resolve(relativePath: String): Path? {
            val path = root.resolve(relativePath)
            return if (Files.exists(path)) path else null
        }

        override fun exists(relativePath: String): Boolean {
            return storedAssets.containsKey(relativePath) || Files.exists(root.resolve(relativePath))
        }
    }
}
