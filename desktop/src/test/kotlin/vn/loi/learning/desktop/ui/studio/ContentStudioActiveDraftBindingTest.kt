package vn.loi.learning.desktop.ui.studio

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.browser.LegacyExampleTranslationProjection
import vn.loi.learning.desktop.ui.browser.PackageContentBrowserFacade
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryFacade
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryViewModel
import vn.loi.learning.desktop.ui.contentlibrary.LessonBrowserFacade
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentMetadata
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.infrastructure.LearningApplicationFactory

class ContentStudioActiveDraftBindingTest {

    private fun setupViewModel(): Pair<ContentLibraryViewModel, InstalledPackageId> {
        val appContext = LearningApplicationFactory.createInMemory()
        val instId = InstalledPackageId("inst-binding-test")
        val pkgId = PackageId("pkg-binding-test")
        val libId = LibraryId("lib-binding-test")
        val contentLibId = ContentLibraryId("lib-binding-test")

        appContext.installedPackageRepository!!.save(
            InstalledPackage.reconstitute(
                id = instId,
                libraryId = libId,
                packageId = pkgId,
                topicId = TopicId.deriveForLegacyPackage("Binding Package", "OPD3"),
                name = PackageName("Binding Package"),
                version = PackageVersion("1.0.0"),
                state = PackageState.ACTIVE,
                installedAt = Instant.now(),
                contentCount = 1,
                learningItemCount = 1
            )
        )

        appContext.contentPackageRepository!!.save(
            ContentPackage(
                id = pkgId,
                descriptor = PackageDescriptor(name = "Binding Package", version = "1.0.0", format = "OPD3"),
                libraryIds = setOf(contentLibId)
            )
        )

        appContext.contentLibraryRepository!!.save(
            ContentLibrary(
                id = contentLibId,
                descriptor = LibraryDescriptor(name = "Binding Library"),
                contentIds = setOf(ContentId("cnt-binding-1"))
            )
        )

        appContext.contentRepository!!.save(
            Content(
                id = ContentId("cnt-binding-1"),
                type = ContentType.WORD,
                text = ContentText(
                    primaryText = "account",
                    translatedText = "tài khoản",
                    pronunciation = "əˈkaʊnt",
                    exampleText = "Action films have a lot of fighting.\nPhim hành động có nhiều cảnh đánh nhau."
                ),
                media = ContentMedia(
                    image = "account.jpg",
                    primaryAudio = "account_q.mp3",
                    translatedAudio = "account_a.mp3",
                    exampleAudio = "account_ex.mp3",
                    exampleTranslatedAudio = "account_tr.mp3"
                ),
                metadata = ContentMetadata(lesson = "General")
            )
        )

        appContext.learningItemRepository!!.save(
            LearningItem(
                id = LearningItemId("item-binding-1"),
                contentId = ContentId("cnt-binding-1"),
                mode = LearningMode.MEANING_RECOGNITION
            )
        )

        val editService = vn.loi.learning.application.contentpackaging.browser.ContentBrowserEditService(
            contentRepository = appContext.contentRepository!!,
            contentLibraryRepository = appContext.contentLibraryRepository,
            installedPackageRepository = appContext.installedPackageRepository
        )
        val facade = ContentLibraryFacade(appContext)
        val lessonBrowserFacade = LessonBrowserFacade(appContext)
        val packageBrowserFacade = PackageContentBrowserFacade(
            queryService = appContext.packageBrowserQuery,
            editService = editService,
            learningItemRepository = appContext.learningItemRepository
        )

        val vm = ContentLibraryViewModel(
            facade = facade,
            lessonBrowserFacade = lessonBrowserFacade,
            packageBrowserFacade = packageBrowserFacade
        )

        vm.browsePackageLessons(instId, "Binding Package")
        return vm to instId
    }

    @Test
    fun `TC01 - startNewItem creates completely empty draft without reusing selected item fields or media`() {
        val (vm, _) = setupViewModel()
        vm.selectPackageBrowserRow("cnt-binding-1")
        vm.startNewItem()

        val state = vm.packageBrowserUiState!!
        assertTrue(state.isCreatingNewItem)
        assertEquals("new_item_draft", state.editingContentId)

        val draft = state.draftEdits
        assertNotNull(draft)
        assertEquals("", draft.questionText)
        assertEquals("", draft.answerText)
        assertEquals("", draft.pronunciation)
        assertEquals("WORD", draft.partOfSpeech)
        assertEquals("", draft.exampleText)
        assertEquals("", draft.exampleTranslation)
        assertNull(draft.imageRef)
        assertNull(draft.questionAudioRef)
        assertNull(draft.answerAudioRef)
        assertNull(draft.exampleAudioRef)
        assertNull(draft.translationAudioRef)
    }

    @Test
    fun `TC02 - Edit mode initializes draft media from persisted item and updates immediately when draft changes`() {
        val (vm, _) = setupViewModel()
        vm.selectPackageBrowserRow("cnt-binding-1")
        vm.startEditContent()

        val draft = vm.packageBrowserUiState?.draftEdits!!
        assertEquals("account.jpg", draft.imageRef)
        assertEquals("account_q.mp3", draft.questionAudioRef)
        assertEquals("account_a.mp3", draft.answerAudioRef)
        assertEquals("account_ex.mp3", draft.exampleAudioRef)
        assertEquals("account_tr.mp3", draft.translationAudioRef)

        // Replace Image immediately updates active draft ref
        vm.updateDraftImageRef("bau.png")
        assertEquals("bau.png", vm.packageBrowserUiState?.draftEdits?.imageRef)

        // Replace Question Audio immediately updates active draft ref
        vm.updateDraftQuestionAudioRef("bau_q.mp3")
        assertEquals("bau_q.mp3", vm.packageBrowserUiState?.draftEdits?.questionAudioRef)
    }

    @Test
    fun `TC03 - Draft null media explicitly overrides persisted media without falling back`() {
        val (vm, _) = setupViewModel()
        vm.selectPackageBrowserRow("cnt-binding-1")
        vm.startEditContent()

        vm.updateDraftImageRef(null)
        assertNull(vm.packageBrowserUiState?.draftEdits?.imageRef)

        vm.updateDraftQuestionAudioRef(null)
        assertNull(vm.packageBrowserUiState?.draftEdits?.questionAudioRef)
    }

    @Test
    fun `TC04 - Legacy bilingual example splits into English and Vietnamese correctly`() {
        val (vm, _) = setupViewModel()
        val item = vm.packageBrowserUiState?.selectedItemAnywhere!!

        assertEquals("Action films have a lot of fighting.", item.exampleText)
        assertEquals("Phim hành động có nhiều cảnh đánh nhau.", item.exampleTranslation)
    }

    @Test
    fun `TC05 - LegacyExampleTranslationProjection unit rules`() {
        // Rule 1: Explicit translation has precedence
        val split1 = LegacyExampleTranslationProjection.project("Eng text", "Vi text")
        assertEquals("Eng text", split1.exampleText)
        assertEquals("Vi text", split1.exampleTranslation)

        // Rule 2: Parentheses split
        val split2 = LegacyExampleTranslationProjection.project("Action films (Phim hành động)", null)
        assertEquals("Action films", split2.exampleText)
        assertEquals("Phim hành động", split2.exampleTranslation)

        // Rule 3: Dash split
        val split3 = LegacyExampleTranslationProjection.project("Good morning - Chào buổi sáng", "")
        assertEquals("Good morning", split3.exampleText)
        assertEquals("Chào buổi sáng", split3.exampleTranslation)

        // Rule 4: English-only unsplit
        val split4 = LegacyExampleTranslationProjection.project("Just an english sentence.", "")
        assertEquals("Just an english sentence.", split4.exampleText)
        assertNull(split4.exampleTranslation)
    }
}
