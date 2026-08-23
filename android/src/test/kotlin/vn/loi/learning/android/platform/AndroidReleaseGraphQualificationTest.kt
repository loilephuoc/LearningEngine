package vn.loi.learning.android.platform

import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.test.*
import org.junit.Test
import vn.loi.learning.infrastructure.LearningApplicationFactory

class AndroidReleaseGraphQualificationTest {
    @Test fun `persisted startup graph exposes release critical services`() {
        val root=createTempDirectory("android-release-graph")
        try { val context=LearningApplicationFactory.createPersisted(root)
            assertNotNull(context.libraryQuery);assertNotNull(context.libraryCommand);assertNotNull(context.packageBrowserQuery)
            assertNotNull(context.contentBrowserEdit);assertNotNull(context.exportContentPackage);assertNotNull(context.packageVerifier)
            assertNotNull(context.upgradeContentPackage);assertNotNull(context.uninstallContentPackage);assertNotNull(context.scopedStudy);assertNotNull(context.lessonBrowser)
        } finally { Files.walk(root).use { it.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) } }
    }
    @Test fun `release manifest contains no broad permissions or unsafe flags`() {
        val manifest=java.io.File("src/main/AndroidManifest.xml").readText()
        listOf("MANAGE_EXTERNAL_STORAGE","READ_EXTERNAL_STORAGE","WRITE_EXTERNAL_STORAGE","CAMERA","largeHeap","usesCleartextTraffic=\"true\"").forEach { assertFalse(manifest.contains(it),it) }
        assertTrue(manifest.contains("android.permission.INTERNET"))
        assertTrue(manifest.contains("android:allowBackup=\"false\""));assertTrue(manifest.contains("android:exported=\"true\""))
    }
    @Test fun `release build declares minification shrinking and no signing secret`() {
        val build=java.io.File("build.gradle.kts").readText();assertTrue(build.contains("isMinifyEnabled = true"));assertTrue(build.contains("isShrinkResources = true"))
        assertFalse(build.contains("storePassword"));assertFalse(build.contains("keyPassword"));assertFalse(build.contains("signingConfig = signingConfigs.getByName(\"debug\")"))
    }
}
