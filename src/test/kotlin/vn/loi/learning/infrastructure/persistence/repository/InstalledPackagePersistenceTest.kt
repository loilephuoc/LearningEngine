package vn.loi.learning.infrastructure.persistence.repository

import java.io.File
import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion
import vn.loi.learning.infrastructure.persistence.json.JsonInstalledPackageStore

/**
 * AC-2, AC-3 (7–12): Persistence round-trip tests for InstalledPackage / contentChecksum.
 *
 * Uses the real JsonInstalledPackageStore → StoreBackedInstalledPackageRepository stack,
 * simulating restart by creating new repository instances over the same file.
 */
class InstalledPackagePersistenceTest {

    private val libId = LibraryId("lib-persist")

    private fun makePkg(
        instId: String,
        pkgId: String,
        topicId: String,
        version: String = "1.0.0",
        checksum: String? = null
    ) = InstalledPackage.reconstitute(
        id = InstalledPackageId(instId),
        libraryId = libId,
        packageId = PackageId(pkgId),
        topicId = TopicId(topicId),
        name = PackageName("Package $pkgId"),
        version = PackageVersion(version),
        state = PackageState.ACTIVE,
        installedAt = Instant.parse("2026-01-01T00:00:00Z"),
        contentCount = 10,
        learningItemCount = 30,
        contentChecksum = checksum
    )

    private fun repoFor(file: File) =
        StoreBackedInstalledPackageRepository(JsonInstalledPackageStore(file.toPath()))

    // ---------------------------------------------------------------------------
    // AC-3.7: Checksum saved and loaded via real persistence
    // ---------------------------------------------------------------------------

    @Test
    fun `checksum round-trips exactly through save and load`() {
        val tmpDir = Files.createTempDirectory("persist-rt").toFile()
        try {
            val storeFile = File(tmpDir, "installed-packages.json")
            val checksum = "sha256:exact-round-trip-value"
            val instId = InstalledPackageId("inst-rt")

            repoFor(storeFile).save(makePkg("inst-rt", "pkg-rt", "topic-rt", checksum = checksum))

            val loaded = repoFor(storeFile).findById(instId)
            assertNotNull(loaded)
            assertEquals(checksum, loaded.contentChecksum)
        } finally { tmpDir.deleteRecursively() }
    }

    // ---------------------------------------------------------------------------
    // AC-3.8: Restart preserves checksum
    // ---------------------------------------------------------------------------

    @Test
    fun `contentChecksum survives application restart (repo re-creation)`() {
        val tmpDir = Files.createTempDirectory("persist-restart").toFile()
        try {
            val storeFile = File(tmpDir, "installed-packages.json")
            val checksum = "sha256:survives-restart"

            // Session 1 — save
            repoFor(storeFile).save(makePkg("inst-restart", "pkg-restart", "topic-restart", checksum = checksum))

            // Session 2 — reload via new repo instance
            val loaded = repoFor(storeFile).findById(InstalledPackageId("inst-restart"))
            assertNotNull(loaded)
            assertEquals(checksum, loaded.contentChecksum)

            // Session 3 — another reload
            val loaded2 = repoFor(storeFile).findByPackageId(PackageId("pkg-restart"))
            assertNotNull(loaded2)
            assertEquals(checksum, loaded2.contentChecksum)
        } finally { tmpDir.deleteRecursively() }
    }

    // ---------------------------------------------------------------------------
    // AC-3.9: Legacy record (no contentChecksum field) loads as null
    // ---------------------------------------------------------------------------

    @Test
    fun `legacy record without contentChecksum field loads as null`() {
        val tmpDir = Files.createTempDirectory("persist-legacy").toFile()
        try {
            val storeFile = File(tmpDir, "installed-packages.json")
            // Write legacy-style envelope JSON (no contentChecksum key at all)
            storeFile.writeText(
                """
                {
                  "schemaVersion": 1,
                  "records": [
                    {
                      "id": "inst-legacy",
                      "libraryId": "lib-persist",
                      "packageId": "pkg-legacy",
                      "topicId": "topic-legacy",
                      "name": "Legacy Package",
                      "version": "1.0.0",
                      "state": "ACTIVE",
                      "installedAt": "2026-01-01T00:00:00Z",
                      "contentCount": 5,
                      "learningItemCount": 15
                    }
                  ]
                }
                """.trimIndent()
            )

            val loaded = repoFor(storeFile).findById(InstalledPackageId("inst-legacy"))
            assertNotNull(loaded)
            assertNull(loaded.contentChecksum, "Legacy record must load with null contentChecksum")
            assertEquals("1.0.0", loaded.version.value)
            assertEquals(5, loaded.contentCount)
        } finally { tmpDir.deleteRecursively() }
    }

    // ---------------------------------------------------------------------------
    // AC-3.10: Upgrade of existing package updates persisted checksum
    // ---------------------------------------------------------------------------

    @Test
    fun `replacement overwrites old checksum with new checksum in persistence`() {
        val tmpDir = Files.createTempDirectory("persist-replace").toFile()
        try {
            val storeFile = File(tmpDir, "installed-packages.json")
            val instId = InstalledPackageId("inst-replace")

            // Save original
            repoFor(storeFile).save(makePkg("inst-replace", "pkg-replace", "topic-replace",
                version = "1.0.0", checksum = "sha256-v1"))

            // Save updated (replacement)
            val updated = InstalledPackage.reconstitute(
                id = instId,
                libraryId = libId,
                packageId = PackageId("pkg-replace"),
                topicId = TopicId("topic-replace"),
                name = PackageName("Package pkg-replace"),
                version = PackageVersion("2.0.0"),
                state = PackageState.ACTIVE,
                installedAt = Instant.parse("2026-06-01T00:00:00Z"),
                contentCount = 15,
                learningItemCount = 45,
                contentChecksum = "sha256-v2"
            )
            repoFor(storeFile).save(updated)

            val loaded = repoFor(storeFile).findById(instId)
            assertNotNull(loaded)
            assertEquals("sha256-v2", loaded.contentChecksum)
            assertEquals("2.0.0", loaded.version.value)
        } finally { tmpDir.deleteRecursively() }
    }

    // ---------------------------------------------------------------------------
    // AC-3.11: findAllByLibraryId after restart
    // ---------------------------------------------------------------------------

    @Test
    fun `findAllByLibraryId returns all persisted records with correct checksums after restart`() {
        val tmpDir = Files.createTempDirectory("persist-multi").toFile()
        try {
            val storeFile = File(tmpDir, "installed-packages.json")

            repoFor(storeFile).save(makePkg("inst-1", "pkg-1", "topic-1", checksum = "sha256-1"))
            repoFor(storeFile).save(makePkg("inst-2", "pkg-2", "topic-2", checksum = "sha256-2"))
            repoFor(storeFile).save(makePkg("inst-3", "pkg-3", "topic-3", checksum = null))

            val all = repoFor(storeFile).findAllByLibraryId(libId)
            assertEquals(3, all.size)

            val pkg1 = all.find { it.packageId == PackageId("pkg-1") }
            val pkg2 = all.find { it.packageId == PackageId("pkg-2") }
            val pkg3 = all.find { it.packageId == PackageId("pkg-3") }

            assertNotNull(pkg1)
            assertNotNull(pkg2)
            assertNotNull(pkg3)
            assertEquals("sha256-1", pkg1.contentChecksum)
            assertEquals("sha256-2", pkg2.contentChecksum)
            assertNull(pkg3.contentChecksum)
        } finally { tmpDir.deleteRecursively() }
    }

    // ---------------------------------------------------------------------------
    // AC-3.12: delete removes from persistence
    // ---------------------------------------------------------------------------

    @Test
    fun `delete removes record from persistent store across restarts`() {
        val tmpDir = Files.createTempDirectory("persist-delete").toFile()
        try {
            val storeFile = File(tmpDir, "installed-packages.json")
            val instId = InstalledPackageId("inst-del")

            repoFor(storeFile).save(makePkg("inst-del", "pkg-del", "topic-del"))
            repoFor(storeFile).delete(instId)

            val loaded = repoFor(storeFile).findById(instId)
            assertNull(loaded, "Deleted record must not be found after restart")
        } finally { tmpDir.deleteRecursively() }
    }
}
