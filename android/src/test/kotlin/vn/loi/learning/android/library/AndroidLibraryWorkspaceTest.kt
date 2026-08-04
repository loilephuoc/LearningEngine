package vn.loi.learning.android.library

import kotlin.test.*
import org.junit.Test
import vn.loi.learning.application.contentpackaging.browser.*
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.LearningApplicationFactory

class AndroidLibraryWorkspaceTest {
    @Test fun `default criteria preserves Application original ordering`() {
        val criteria=AndroidLibraryCriteria(); assertEquals(BrowserSortOption.ORIGINAL_ORDER,criteria.sort); assertEquals(BrowserMediaFilter.ALL,criteria.media)
    }
    @Test fun `search delegates unicode matching to Application policy`() {
        val visible=PackageContentBrowserProjectionPolicy.filterAndSort(listOf(item(1,"Xin Chào",true,false)),"xin chào",null,BrowserMediaFilter.ALL,BrowserSortOption.ORIGINAL_ORDER)
        assertEquals(listOf("c1"),visible.map{it.contentId.value})
    }
    @Test fun `media filter uses typed Application criterion`() {
        val all=listOf(item(1,"one",true,false),item(2,"two",false,true)); val visible=PackageContentBrowserProjectionPolicy.filterAndSort(all,"",null,BrowserMediaFilter.HAS_AUDIO,BrowserSortOption.ORIGINAL_ORDER)
        assertEquals("c2",visible.single().contentId.value)
    }
    @Test fun `stable content identity is independent from displayed index`() { assertEquals("c9",item(42,"word",false,false,"c9").contentId.value) }
    @Test fun `browser state retains lightweight criteria and selection`() {
        val criteria=AndroidLibraryCriteria(query="word"); assertEquals("word",criteria.query); assertNull(criteria.lesson)
    }
    @Test fun `editor draft preserves invalid user input for presentation recovery`() {
        val draft=AndroidItemDraft("","meaning","ipa","noun","example","translation")
        assertTrue(draft.question.isBlank()); assertEquals("meaning",draft.answer)
    }
    @Test fun `global search result carries only stable package and projected item identity`() {
        val result=AndroidLibrarySearchResult("pkg-1","Package",item(1,"word",false,false))
        assertEquals("pkg-1",result.packageId); assertEquals("c1",result.item.contentId.value)
    }
    @Test fun `canonical imported package projection drives root search and browser`() {
        val context=LearningApplicationFactory.createInMemory()
        val contentId=ContentId("imported-bed")
        val libraryId=ContentLibraryId("imported-library")
        context.contentRepository!!.save(Content(contentId,ContentType.WORD,ContentText("bed","cái giường")))
        context.contentLibraryRepository!!.save(ContentLibrary(libraryId,LibraryDescriptor("Imported"),setOf(contentId)))
        context.contentPackageRepository!!.save(ContentPackage(PackageId("imported-package"),PackageDescriptor("Imported Package","1.0.0","OPD3"),setOf(libraryId)))

        val facade=AndroidLibraryFacade(context)
        val root=assertIs<AndroidLibraryState.Root>(facade.loadRoot())
        assertEquals(listOf("imported-package"),root.packages.map{it.id})
        val search=assertIs<AndroidLibraryState.Root>(facade.searchGlobal("cái giường"))
        assertEquals(contentId,search.results.single().item.contentId)
        val browser=assertIs<AndroidLibraryState.PackageBrowser>(facade.openPackage(vn.loi.learning.domain.library.model.InstalledPackageId("imported-package")))
        assertEquals(contentId,browser.visibleItems.single().contentId)
    }
    private fun item(index:Int,text:String,image:Boolean,audio:Boolean,id:String="c$index")=PackageContentBrowserItem(index,ContentId(id),text,"answer","","noun",null,null,"Lesson","Package",image,audio,null,null,exampleText=null,exampleTranslation=null,learningItemCount=0,learningItemIds=emptyList(),learningModes=emptyList(),tags=emptySet(),searchableText=text.lowercase())
}
