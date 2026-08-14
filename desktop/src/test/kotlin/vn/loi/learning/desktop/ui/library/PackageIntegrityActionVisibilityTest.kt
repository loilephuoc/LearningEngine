package vn.loi.learning.desktop.ui.library

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.application.library.query.InstalledPackageSummary
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.*

@OptIn(ExperimentalTestApi::class)
class PackageIntegrityActionVisibilityTest {
    @Test
    fun `expanded production package card exposes package-specific Check Integrity action`() {
        var invoked = 0
        runComposeUiTest {
            setContent {
                PackageCard(
                    pkg = fixturePackage(),
                    onCheckIntegrity = { invoked++ }
                )
            }
            onNodeWithText("Check Integrity").assertIsDisplayed().performClick()
            waitForIdle()
        }
        assertEquals(1, invoked)
    }

    @Test
    fun `expanded production package card disables integrity action while scanning`() = runComposeUiTest {
        setContent {
            PackageCard(
                pkg = fixturePackage(),
                onCheckIntegrity = {},
                integrityBusy = true
            )
        }
        onNodeWithText("Checking…").assertIsDisplayed().assertIsNotEnabled()
    }

    private fun fixturePackage() = InstalledPackageSummary(
        InstalledPackageId("installed"), LibraryId("library"), PackageId("package"), TopicId("topic"),
        "Package", "1.0", PackageState.ACTIVE, Instant.EPOCH, 1, 1
    )
}
