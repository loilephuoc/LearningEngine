package vn.loi.learning.android.library

import kotlin.test.*
import org.junit.Test
import vn.loi.learning.application.contentpackaging.browser.*
import vn.loi.learning.domain.content.model.ContentId

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
    private fun item(index:Int,text:String,image:Boolean,audio:Boolean,id:String="c$index")=PackageContentBrowserItem(index,ContentId(id),text,"answer","","noun",null,null,"Lesson","Package",image,audio,null,null,exampleText=null,exampleTranslation=null,learningItemCount=0,learningItemIds=emptyList(),learningModes=emptyList(),tags=emptySet(),searchableText=text.lowercase())
}
