package vn.loi.learning.application.session

import kotlin.test.*
import org.junit.jupiter.api.Test
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.*
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.study.session.model.*

class ScopedStudySessionServiceTest {
    @Test fun `package scope delegates all canonical content to existing session command`() {
        val f=fixture(); f.service.execute(request(StudyContentScope.Package(f.pkg)))
        assertEquals(setOf(ContentId("a"),ContentId("b")),f.command!!.includedContentIds); assertEquals(f.pkg,f.command!!.installedPackageId)
    }
    @Test fun `lesson scope delegates only exact canonical lesson membership`() {
        val f=fixture(); f.service.execute(request(StudyContentScope.Lesson(f.pkg,"L2")))
        assertEquals(setOf(ContentId("b")),f.command!!.includedContentIds)
    }
    @Test fun `selection rejects content outside package before session creation`() {
        val f=fixture(); assertFailsWith<IllegalArgumentException>{f.service.execute(request(StudyContentScope.Selection(f.pkg,setOf(ContentId("foreign")))))}; assertNull(f.command)
    }
    @Test fun `collection scope unions canonical package content without platform queue logic`() {
        val f=fixture(); f.service.execute(request(StudyContentScope.Collection(CollectionId("c")))); assertEquals(setOf(ContentId("a"),ContentId("b")),f.command!!.includedContentIds); assertNull(f.command!!.installedPackageId)
    }
    private fun request(scope:StudyContentScope)=StartScopedStudyRequest(SessionId("s"),LearnerId("l"),Moment(1),scope)
    private fun fixture():Fixture { val pkg=InstalledPackageId("p"); lateinit var holder:Fixture; val service=ScopedStudySessionService(
        packageItems={listOf(item("a","L1"),item("b","L2"))}, collectionPackages={listOf(pkg)}, startSession={cmd->holder.command=cmd;StudySession.start(cmd.sessionId,cmd.learnerId,cmd.startedAt,policy=cmd.policy,includedContentIds=cmd.includedContentIds,installedPackageId=cmd.installedPackageId)})
        return Fixture(pkg,service).also{holder=it} }
    private fun item(id:String,lesson:String)=PackageContentBrowserItem(1,ContentId(id),id,"answer","","noun",null,null,lesson,"pkg",false,false,null,null,exampleText=null,exampleTranslation=null,learningItemCount=0,learningItemIds=emptyList(),learningModes=emptyList(),tags=emptySet(),searchableText=id)
    private data class Fixture(val pkg:InstalledPackageId,val service:ScopedStudySessionService,var command:StartStudySessionCommand?=null)
}
