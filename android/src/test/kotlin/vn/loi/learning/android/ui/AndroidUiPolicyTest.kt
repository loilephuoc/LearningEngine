package vn.loi.learning.android.ui

import kotlin.test.*
import org.junit.Test

class AndroidUiPolicyTest {
    @Test fun `compact width selects compact bounded presentation`() { val p=androidLayoutPolicy(360,800); assertEquals(AndroidWindowWidth.COMPACT,p.width); assertEquals(600,p.maxContentWidthDp) }
    @Test fun `medium width selects bounded presentation`() { val p=androidLayoutPolicy(700,1000); assertEquals(AndroidWindowWidth.MEDIUM,p.width); assertEquals(720,p.maxContentWidthDp) }
    @Test fun `expanded width does not stretch content indefinitely`() { assertEquals(840,androidLayoutPolicy(1400,1000).maxContentWidthDp) }
    @Test fun `landscape bounds media height`() { assertEquals(180,androidLayoutPolicy(800,420).maxMediaHeightDp) }
    @Test fun `landscape compacts vertical rhythm without reducing touch targets`() { val p=androidLayoutPolicy(800,420); assertEquals(8,p.verticalPaddingDp); assertEquals(12,p.runtimeSpacingDp); assertEquals(48,p.minimumTouchTargetDp) }
    @Test fun `portrait permits useful media height`() { assertEquals(320,androidLayoutPolicy(400,800).maxMediaHeightDp) }
    @Test fun `vi semantics are localized`() { assertEquals("Câu trả lời",androidAccessibilityStrings("vi").answer); assertTrue(androidAccessibilityStrings("vi").option(2,4,true).contains("đã chọn")) }
    @Test fun `en option announces position and selection`() { assertEquals("Option 2 of 4, selected",androidAccessibilityStrings("en").option(2,4,true)) }
    @Test fun `image description never includes canonical answer`() { assertFalse(androidAccessibilityStrings("en").imagePrompt.contains("expected",true)) }
    @Test fun `example blank semantics are explicit`() { assertEquals("blank to complete",androidAccessibilityStrings("en").blank) }
    @Test fun `package title is human readable without changing identity`() { assertEquals("Vocabulary In Use Elementary", androidDisplayTitle("Vocabulary_In_Use_Elementary")) }
}
