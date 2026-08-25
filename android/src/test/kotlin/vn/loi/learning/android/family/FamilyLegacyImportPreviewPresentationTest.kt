package vn.loi.learning.android.family

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class FamilyLegacyImportPreviewPresentationTest {
    @Test fun `People menu uses SAF CSV and preview offers no import mutation action`() {
        val screen = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/android/family/FamilyScreen.kt"))
        val preview = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/android/family/FamilyLegacyImportPreview.kt"))
        assertTrue(screen.contains("ActivityResultContracts.OpenDocument()"))
        assertTrue(screen.contains("Xem trước nhập AppSheet CSV"))
        assertTrue(screen.contains("FamilyLegacyImportPreviewService.preview(csv, snapshot)"))
        assertTrue(preview.contains("Chỉ xem trước — không ghi dữ liệu"))
        assertTrue(preview.contains("Xem trước dữ liệu AppSheet"))
        assertTrue(preview.contains("FamilyLegacyImportCandidateDetailScreen"))
        assertTrue(preview.contains("BackHandler { selected = null }"))
        assertTrue(preview.contains("BỊ CHẶN"))
        assertFalse(preview.contains("Import now"))
        assertFalse(preview.contains("repository."))
    }
}
