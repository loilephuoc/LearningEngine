package vn.loi.learning.desktop.ui.contentlibrary
import kotlin.test.*
class LessonBrowserSearchPresentationTest{@Test fun noMatch(){val s=LessonBrowserUiState(lessons=listOf(LessonBrowserItem("1","A","t",null,null,null,"A",null,1)),query="z");assertTrue(lessonBrowserEmptySearchMessage(s).contains("z"));assertEquals(0,lessonBrowserSearchSummary(s).visibleCount)}}
