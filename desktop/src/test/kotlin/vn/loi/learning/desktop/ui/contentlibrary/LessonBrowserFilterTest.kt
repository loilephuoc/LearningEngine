package vn.loi.learning.desktop.ui.contentlibrary
import kotlin.test.*
class LessonBrowserFilterTest{@Test fun translations(){val a=LessonBrowserItem("1","A","t",null,null,null,"A",null,1);assertTrue(LessonBrowserFilter.WITHOUT_TRANSLATION.matches(a));assertFalse(LessonBrowserFilter.WITH_TRANSLATION.matches(a))}}
