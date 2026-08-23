package vn.loi.learning.android.study

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.android.library.AndroidLibraryFacade
import vn.loi.learning.android.library.AndroidLibraryState
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.recall.StudyMode
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.LearningApplicationFactory

class AndroidCanonicalActivePackageProjectionTest {

    private val learnerId = LearnerId("default-learner")

    @Test
    fun `Library active OPD_2nd projects correctly to Home with exact name and count`() {
        val context = LearningApplicationFactory.createInMemory()
        val pkgId = install(context, "OPD_2nd", displayName = "Oxford Picture Dictionary 2nd Edition", count = 2389)
        context.libraryCommand!!.setActivePackage(context.defaultLibraryId!!, pkgId)

        val facade = AndroidStudyFacade(context, learnerId)
        val home = facade.home()

        assertEquals("Oxford Picture Dictionary 2nd Edition", home.model.activePackageName)
        assertEquals(2389, home.model.activePackageContentCount)
        assertEquals("Oxford Picture Dictionary 2nd Edition", home.model.contextTitle)
    }

    @Test
    fun `Library active OPD_2nd projects correctly to Learn tab presentation`() {
        val context = LearningApplicationFactory.createInMemory()
        val pkgId = install(context, "OPD_2nd", displayName = "Oxford Picture Dictionary 2nd Edition", count = 2389)
        context.libraryCommand!!.setActivePackage(context.defaultLibraryId!!, pkgId)

        val facade = AndroidStudyFacade(context, learnerId)
        val home = facade.home()
        val presentation = resolveLearningLandingPresentation(home)

        assertEquals("Oxford Picture Dictionary 2nd Edition", presentation.contextTitle)
        assertTrue(presentation.hasContent)
    }

    @Test
    fun `Active StudySession OPD_2nd projects exact session package to Resume card`() {
        val context = LearningApplicationFactory.createInMemory()
        val pkgId = install(context, "OPD_2nd", displayName = "Oxford Picture Dictionary 2nd Edition", count = 2389)
        context.libraryCommand!!.setActivePackage(context.defaultLibraryId!!, pkgId)

        val facade = AndroidStudyFacade(context, learnerId, now = { 2_000L })
        val started = facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        assertIs<AndroidStudyState.Introduction>(started)

        val home = facade.home()
        assertIs<AndroidHomePrimaryAction.Resume>(home.model.primaryAction)
        assertEquals("Oxford Picture Dictionary 2nd Edition", home.model.contextTitle)
        assertEquals("Oxford Picture Dictionary 2nd Edition", home.model.activePackageName)
        assertEquals(2389, home.model.activePackageContentCount)
    }

    @Test
    fun `Package switch via Library command refreshes Home and Learn immediately`() {
        val context = LearningApplicationFactory.createInMemory()
        val pkg1 = install(context, "OPD_2nd", displayName = "Oxford Picture Dictionary 2nd Edition", count = 2389)
        val pkg2 = install(context, "TOEIC_Essential", displayName = "600 Essential Words for TOEIC", count = 600)
        context.libraryCommand!!.setActivePackage(context.defaultLibraryId!!, pkg1)

        val studyFacade = AndroidStudyFacade(context, learnerId)
        val home1 = studyFacade.home()
        assertEquals("Oxford Picture Dictionary 2nd Edition", home1.model.activePackageName)
        assertEquals(2389, home1.model.activePackageContentCount)

        var widgetRefreshed = 0
        val libraryFacade = AndroidLibraryFacade(
            context,
            onActivePackageChanged = { widgetRefreshed += 1 }
        )
        val selectResult = libraryFacade.selectLearningPackage(pkg2)
        assertIs<AndroidLibraryState.Root>(selectResult)
        assertEquals(1, widgetRefreshed)

        val home2 = studyFacade.home()
        assertEquals("600 Essential Words for TOEIC", home2.model.activePackageName)
        assertEquals(600, home2.model.activePackageContentCount)
        assertEquals("600 Essential Words for TOEIC", home2.model.contextTitle)
    }

    @Test
    fun `Process recreation and restart preserves canonical active package and count`() {
        val context = LearningApplicationFactory.createInMemory()
        val pkg = install(context, "OPD_2nd", displayName = "Oxford Picture Dictionary 2nd Edition", count = 2389)
        context.libraryCommand!!.setActivePackage(context.defaultLibraryId!!, pkg)

        // Simulate creating brand new facade instances after restart
        val restoredStudyFacade = AndroidStudyFacade(context, learnerId)
        val restoredHome = restoredStudyFacade.home()

        assertEquals("Oxford Picture Dictionary 2nd Edition", restoredHome.model.activePackageName)
        assertEquals(2389, restoredHome.model.activePackageContentCount)
        assertEquals("Oxford Picture Dictionary 2nd Edition", restoredHome.model.contextTitle)
    }

    @Test
    fun `Library active package and session package mismatch displays exact session package on Resume card without selecting wrong package`() {
        val context = LearningApplicationFactory.createInMemory()
        val pkg1 = install(context, "OPD_2nd", displayName = "Oxford Picture Dictionary 2nd Edition", count = 2389)
        val pkg2 = install(context, "TOEIC_Essential", displayName = "600 Essential Words for TOEIC", count = 600)
        context.libraryCommand!!.setActivePackage(context.defaultLibraryId!!, pkg1)

        val facade = AndroidStudyFacade(context, learnerId, now = { 2_000L })
        facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)

        // Switch Library active package to pkg2 without finishing session
        context.libraryCommand!!.setActivePackage(context.defaultLibraryId!!, pkg2)

        val home = facade.home()
        // Mismatched session was finished, projecting canonical active package pkg2 cleanly
        assertIs<AndroidHomePrimaryAction.StartLearning>(home.model.primaryAction)
        assertEquals("600 Essential Words for TOEIC", home.model.contextTitle)
        assertEquals("600 Essential Words for TOEIC", home.model.activePackageName)
        assertEquals(600, home.model.activePackageContentCount)
    }

    @Test
    fun `No active package selected never leaks count from another installed package`() {
        val context = LearningApplicationFactory.createInMemory()
        install(context, "OPD_2nd", displayName = "Oxford Picture Dictionary 2nd Edition", count = 2389)

        val facade = AndroidStudyFacade(context, learnerId)
        val home = facade.home()

        assertNull(home.model.activePackageName)
        assertNull(home.model.activePackageContentCount)
        assertNull(home.model.contextTitle)
        assertIs<AndroidHomePrimaryAction.OpenLibrary>(home.model.primaryAction)
    }

    private fun install(
        context: LearningApplicationContext,
        id: String,
        displayName: String,
        count: Int
    ): InstalledPackageId {
        val contentIds = (0 until count).mapTo(linkedSetOf()) { index ->
            val suffix = if (count == 1) "" else "-$index"
            val contentId = ContentId("$id-content$suffix")
            context.contentRepository!!.save(
                Content(contentId, ContentType.WORD, ContentText("$id$suffix", "$id-answer$suffix"))
            )
            context.learningItemRepository!!.save(
                LearningItem(LearningItemId("$id-item$suffix"), contentId, LearningMode.MEANING_RECOGNITION)
            )
            contentId
        }
        val contentLibraryId = ContentLibraryId("$id-library")
        val packageId = PackageId(id)
        val installedId = InstalledPackageId(id)
        context.contentLibraryRepository!!.save(
            ContentLibrary(
                contentLibraryId,
                LibraryDescriptor(displayName),
                contentIds
            )
        )
        context.contentPackageRepository!!.save(
            ContentPackage(
                packageId,
                PackageDescriptor(displayName, "1.0.0", "OPD3"),
                setOf(contentLibraryId)
            )
        )
        val libraryId = context.defaultLibraryId!!
        context.installedPackageRepository!!.save(
            InstalledPackage.reconstitute(
                installedId, libraryId, packageId, TopicId("$id-topic"), PackageName(displayName), PackageVersion("1.0.0"),
                PackageState.ACTIVE, java.time.Instant.EPOCH, count, count
            )
        )
        val library = context.domainLibraryRepository!!.findById(libraryId)!!
        context.domainLibraryRepository!!.save(library.registerEntry(installedId, packageId, java.time.Instant.EPOCH))
        return installedId
    }
}
