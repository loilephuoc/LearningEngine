package vn.loi.learning.android.family

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class FamilyLegacyImportPreviewPresentationTest {

    @Test
    fun `People menu uses SAF CSV and preview exposes controlled import flow`() {
        val screen = Files.readString(
            Path.of(
                "src/main/kotlin/vn/loi/learning/android/family/FamilyScreen.kt"
            )
        )

        val preview = Files.readString(
            Path.of(
                "src/main/kotlin/vn/loi/learning/android/family/FamilyLegacyImportPreview.kt"
            )
        )

        assertTrue(
            screen.contains(
                "ActivityResultContracts.OpenDocument()"
            )
        )

        assertTrue(
            screen.contains(
                "Xem trước nhập AppSheet CSV"
            )
        )

        assertTrue(
            screen.contains(
                "FamilyLegacyImportPreviewService.preview(csv, snapshot)"
            )
        )

        assertTrue(
            screen.contains(
                "repository = repository"
            )
        )

        assertTrue(
            screen.contains(
                "onImportFinished"
            )
        )

        assertTrue(
            preview.contains(
                "Xem trước dữ liệu AppSheet"
            )
        )

        assertTrue(
            preview.contains(
                "FamilyLegacyImportCandidateDetailScreen"
            )
        )

        assertTrue(
            preview.contains(
                "FamilyLegacyImportPlanner.selectableRowIndexes"
            )
        )

        assertTrue(
            preview.contains(
                "FamilyLegacyImportPlanner.build"
            )
        )

        assertTrue(
            preview.contains(
                "Chọn tất cả an toàn"
            )
        )

        assertTrue(
            preview.contains(
                "Bỏ chọn tất cả"
            )
        )

        assertTrue(
            preview.contains(
                "Nhập ${'$'}{currentPlan.peopleToCreate} người"
            )
        )

        assertTrue(
            preview.contains(
                "Sắp nhập dữ liệu"
            )
        )

        assertTrue(
            preview.contains(
                "NHẬP DỮ LIỆU"
            )
        )

        assertTrue(
            preview.contains(
                "Dữ liệu trùng và dữ liệu cần xem lại sẽ không được tự động hợp nhất hoặc ghi đè."
            )
        )

        assertTrue(
            preview.contains(
                "Các trường nhạy cảm bị chặn sẽ không được ghi."
            )
        )

        assertTrue(
            preview.contains(
                "FamilyLegacyImportRepositoryAdapter"
            )
        )

        assertTrue(
            preview.contains(
                ".commit("
            )
        )

        assertTrue(
            preview.contains(
                "isImporting"
            )
        )

        assertTrue(
            preview.contains(
                "Nhập dữ liệu hoàn tất"
            )
        )

        assertTrue(
            preview.contains(
                "XEM DANH BẠ"
            )
        )

        /*
         * The preview UI must never bypass the controlled importer
         * by mutating FAMILY entities directly.
         */
        assertFalse(
            preview.contains(
                "repository.upsertPerson("
            )
        )

        assertFalse(
            preview.contains(
                "repository.upsertPersonContactField("
            )
        )

        assertFalse(
            preview.contains(
                "repository.deletePerson("
            )
        )

        assertFalse(
            preview.contains(
                "repository.replaceSnapshot("
            )
        )
    }
}