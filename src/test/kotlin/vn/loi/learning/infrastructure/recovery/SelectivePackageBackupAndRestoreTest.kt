package vn.loi.learning.infrastructure.recovery

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.zip.ZipFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion
import vn.loi.learning.domain.sync.model.PackageCompatibilityStatus
import vn.loi.learning.infrastructure.LearningApplicationFactory

class SelectivePackageBackupAndRestoreTest {

    @Test
    fun `selective package graph filters canonical records and restores a resolving queue`() {
        val root = Files.createTempDirectory("selective-graph-test-")
        try {
            val source = root.resolve("source/data")
            val media = root.resolve("source/media")
            Files.createDirectories(media.resolve("Named_A"))
            Files.createDirectories(media.resolve("Named_B"))
            Files.writeString(media.resolve("Named_A/a.mp3"), "a")
            Files.writeString(media.resolve("Named_B/b.mp3"), "b")
            fun write(name: String, records: String) = Files.writeString(
                source.resolve(name), """{"schemaVersion":1,"records":$records}"""
            )
            Files.createDirectories(source)
            write("installed-packages.json", """[
              {"id":"installed-a","libraryId":"default","packageId":"package-a","topicId":"topic-a","name":"Named_A","version":"1","state":"ACTIVE","installedAt":"2026-01-01T00:00:00Z"},
              {"id":"installed-b","libraryId":"default","packageId":"package-b","topicId":"topic-b","name":"Named_B","version":"1","state":"ACTIVE","installedAt":"2026-01-01T00:00:00Z"}
            ]""")
            write("content-packages.json", """[
              {"id":"package-a","name":"Named_A","version":"1","format":"OPD3","libraryIds":["library-a"]},
              {"id":"package-b","name":"Named_B","version":"1","format":"OPD3","libraryIds":["library-b"]}
            ]""")
            write("content-libraries.json", """[
              {"id":"library-a","name":"A","contentIds":["content-a"]},
              {"id":"library-b","name":"B","contentIds":["content-b"]}
            ]""")
            write("contents.json", """[
              {"id":"content-a","primaryAudio":"Named_A/a.mp3"},
              {"id":"content-b","primaryAudio":"Named_B/b.mp3"}
            ]""")
            write("learning-items.json", """[
              {"id":"item-a","contentId":"content-a"},{"id":"item-b","contentId":"content-b"}
            ]""")
            write("memory-states.json", """[
              {"learnerId":"learner","learningItemId":"item-a"},{"learnerId":"learner","learningItemId":"item-b"}
            ]""")
            write("review-events.json", """[
              {"id":"review-a","stateBefore":{"learningItemId":"item-a"},"stateAfter":{"learningItemId":"item-a"}},
              {"id":"review-b","stateBefore":{"learningItemId":"item-b"},"stateAfter":{"learningItemId":"item-b"}}
            ]""")
            write("learning-trajectories.json", """[
              {"learnerId":"learner","contentId":"content-a","chains":[]},
              {"learnerId":"learner","contentId":"content-b","chains":[]}
            ]""")
            write("study-sessions.json", """[
              {"id":"session-a","installedPackageId":"installed-a","reviewedItemIds":["item-a"],"reviewedContentIds":["content-a"]},
              {"id":"session-b","installedPackageId":"installed-b","reviewedItemIds":["item-b"],"reviewedContentIds":["content-b"]}
            ]""")
            write("study-queues.json", """[
              {"sessionId":"session-a","learningItemIds":["item-a"],"itemContentIds":{"item-a":"content-a"},"fixedPracticeMembership":["item-a"],"practiceReinforcementStates":{"item-a":{}},"coverageReinforcementStates":{"item-a":{}},"practiceMembershipUndo":{"learningItemId":"item-a","previousMembership":["item-a"],"previousQueue":["item-a"]},"coverageReinforcementUndo":{"learningItemId":"item-a","discardedTail":["item-a"]}},
              {"sessionId":"session-b","learningItemIds":["item-b"],"itemContentIds":{"item-b":"content-b"}}
            ]""")

            val manager = JvmLearningDataRecoveryManager(
                roots = mapOf("data" to source, "media" to media), safetyDirectory = root.resolve("safety")
            )
            val descriptor = PortableBackupV2Descriptor("2.0", sourcePlatform = "desktop", specificPackageIds = setOf("package-a"))
            val preview = manager.previewPortableBackupCreation(descriptor)
            val archive = root.resolve("a.lebak")
            manager.createPortableBackupV2(archive, descriptor)
            val manifest = manager.validatePortableBackupV2(archive)

            ZipFile(archive.toFile()).use { zip ->
                fun records(name: String): List<JsonObject> {
                    val element = Json.parseToJsonElement(zip.getInputStream(zip.getEntry("portable/data/$name"))
                        .bufferedReader().use { it.readText() }) as JsonObject
                    assertFalse("data" in element)
                    return (element.getValue("records") as JsonArray).filterIsInstance<JsonObject>()
                }
                assertEquals(listOf("package-a"), records("installed-packages.json").map { it["packageId"]!!.jsonPrimitive.content })
                assertEquals(listOf("session-a"), records("study-sessions.json").map { it["id"]!!.jsonPrimitive.content })
                val queue = records("study-queues.json").single()
                assertEquals("session-a", queue["sessionId"]!!.jsonPrimitive.content)
                assertEquals(listOf("item-a"), (queue["learningItemIds"] as JsonArray).map { it.jsonPrimitive.content })
                assertEquals(listOf("review-a"), records("review-events.json").map { it["id"]!!.jsonPrimitive.content })
                assertEquals(listOf("content-a"), records("learning-trajectories.json").map { it["contentId"]!!.jsonPrimitive.content })
            }
            assertEquals(preview.counts, manifest.counts)
            assertEquals(1, manifest.counts.studySessions)
            assertEquals(1, manifest.counts.studyQueues)
            assertEquals(1, manifest.counts.learningTrajectories)

            val mergeResult = assertIs<PortableBackupV2RestoreResult.Success>(
                manager.restorePortableBackupV2(archive, selectedPackageIds = setOf("package-a"))
            )
            assertEquals(1, mergeResult.restoredCounts.packages)
            assertEquals(1, mergeResult.restoredCounts.contents)
            assertEquals(1, mergeResult.restoredCounts.learningItems)
            assertEquals(1, mergeResult.restoredCounts.mediaFiles)
            assertEquals(1, mergeResult.restoredEntriesCount)
            assertEquals(
                setOf("package-a", "package-b"),
                Json.parseToJsonElement(Files.readString(source.resolve("installed-packages.json")))
                    .let { it as JsonObject }.getValue("records").let { it as JsonArray }
                    .map { (it as JsonObject)["packageId"]!!.jsonPrimitive.content }.toSet()
            )

            val noProgressArchive = root.resolve("a-no-progress.lebak")
            manager.createPortableBackupV2(noProgressArchive, descriptor.copy(includeLearningProgress = false))
            ZipFile(noProgressArchive.toFile()).use { zip ->
                PROGRESS_FILE_NAMES.forEach { name -> assertTrue(zip.getEntry("portable/data/$name") == null) }
            }

            val targetData = root.resolve("target/data")
            val targetMedia = root.resolve("target/media")
            val target = JvmLearningDataRecoveryManager(
                roots = mapOf("data" to targetData, "media" to targetMedia), safetyDirectory = root.resolve("target/safety")
            )
            val restoreResult = assertIs<PortableBackupV2RestoreResult.Success>(
                target.restorePortableBackupV2(archive, selectedPackageIds = setOf("package-a"))
            )
            assertEquals(manifest.counts, restoreResult.restoredCounts)
            assertEquals(listOf("item-a"), Json.parseToJsonElement(Files.readString(targetData.resolve("learning-items.json")))
                .let { it as JsonObject }.getValue("records").let { it as JsonArray }
                .map { (it as JsonObject)["id"]!!.jsonPrimitive.content })
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    private companion object {
        val PROGRESS_FILE_NAMES = listOf(
            "memory-states.json", "review-events.json", "learning-trajectories.json",
            "study-sessions.json", "study-queues.json"
        )
    }

    @Test
    fun `selected queue with a dangling item fails before archive publication`() {
        val root = Files.createTempDirectory("selective-dangling-queue-test-")
        try {
            val data = root.resolve("data")
            Files.createDirectories(data)
            val files = mapOf(
                "installed-packages.json" to """[{"id":"installed-a","packageId":"package-a","name":"A"}]""",
                "content-packages.json" to """[{"id":"package-a","libraryIds":["library-a"]}]""",
                "content-libraries.json" to """[{"id":"library-a","contentIds":["content-a"]}]""",
                "contents.json" to """[{"id":"content-a"}]""",
                "learning-items.json" to """[{"id":"item-a","contentId":"content-a"}]""",
                "study-sessions.json" to """[{"id":"session-a","installedPackageId":"installed-a","reviewedItemIds":[],"reviewedContentIds":[]}]""",
                "study-queues.json" to """[{"sessionId":"session-a","learningItemIds":["missing-item"]}]"""
            )
            files.forEach { (name, records) -> Files.writeString(data.resolve(name), """{"schemaVersion":1,"records":$records}""") }
            val archive = root.resolve("invalid.lebak")
            val failure = runCatching {
                JvmLearningDataRecoveryManager(mapOf("data" to data), root.resolve("safety")).createPortableBackupV2(
                    archive, PortableBackupV2Descriptor("2.0", sourcePlatform = "desktop", specificPackageIds = setOf("package-a"))
                )
            }.exceptionOrNull()
            assertNotNull(failure)
            assertTrue(failure.cause?.message.orEmpty().contains("StudyQueue"))
            assertTrue(Files.notExists(archive))
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `selective backup resolves canonical package identity to library content and named media folder`() {
        val root = Files.createTempDirectory("selective-real-schema-test-")
        try {
            val data = root.resolve("data")
            val media = data.resolve("media")
            Files.createDirectories(media.resolve("Vocabulary_In_Use_Upper_Intermediate"))
            Files.createDirectories(media.resolve("Other_Package"))
            Files.writeString(media.resolve("Vocabulary_In_Use_Upper_Intermediate/word.mp3"), "selected-audio")
            Files.writeString(media.resolve("Other_Package/other.mp3"), "unrelated-audio")
            Files.writeString(data.resolve("installed-packages.json"), """
                {"schemaVersion":1,"records":[
                  {"id":"installed-upper","packageId":"package-upper-hash","name":"Vocabulary_In_Use_Upper_Intermediate","version":"1.0.0","contentCount":1,"learningItemCount":1},
                  {"id":"installed-other","packageId":"package-other-hash","name":"Other_Package","version":"1.0.0","contentCount":1,"learningItemCount":1}
                ]}
            """.trimIndent())
            Files.writeString(data.resolve("content-packages.json"), """
                {"schemaVersion":1,"records":[
                  {"id":"package-upper-hash","name":"Vocabulary_In_Use_Upper_Intermediate","libraryIds":["library-upper"]},
                  {"id":"package-other-hash","name":"Other_Package","libraryIds":["library-other"]}
                ]}
            """.trimIndent())
            Files.writeString(data.resolve("content-libraries.json"), """
                {"schemaVersion":1,"records":[
                  {"id":"library-upper","name":"Upper","contentIds":["content-upper"]},
                  {"id":"library-other","name":"Other","contentIds":["content-other"]}
                ]}
            """.trimIndent())
            Files.writeString(data.resolve("contents.json"), """
                {"schemaVersion":1,"records":[
                  {"id":"content-upper","primaryAudio":"Vocabulary_In_Use_Upper_Intermediate/word.mp3"},
                  {"id":"content-other","primaryAudio":"Other_Package/other.mp3"}
                ]}
            """.trimIndent())
            Files.writeString(data.resolve("learning-items.json"), """
                {"schemaVersion":1,"records":[
                  {"id":"item-upper","contentId":"content-upper"},
                  {"id":"item-other","contentId":"content-other"}
                ]}
            """.trimIndent())

            val backup = root.resolve("upper.lebak")
            val recovery = JvmLearningDataRecoveryManager(
                roots = mapOf("data" to data, "media" to media),
                safetyDirectory = root.resolve("safety")
            )
            val plan = recovery.previewPortableBackupCreation(
                PortableBackupV2Descriptor(
                    appVersion = "2.0.0",
                    sourcePlatform = "desktop",
                    specificPackageIds = setOf("package-upper-hash")
                )
            )
            assertEquals(1, plan.counts.packages)
            assertEquals(1, plan.counts.contents)
            assertEquals(1, plan.counts.learningItems)
            assertEquals(1, plan.counts.mediaFiles)
            assertEquals("selected-audio".toByteArray().size.toLong(), plan.mediaBytes)
            assertTrue(plan.estimatedDataBytes > 0)
            assertEquals(plan.estimatedDataBytes + plan.mediaBytes, plan.estimatedTotalBytes)
            val phases = mutableListOf<PortableBackupPhaseV2>()
            recovery.createPortableBackupV2(
                backup,
                PortableBackupV2Descriptor(
                    appVersion = "2.0.0",
                    sourcePlatform = "desktop",
                    specificPackageIds = setOf("package-upper-hash")
                ),
                onProgress = { phases += it.phase }
            )
            assertEquals(PortableBackupPhaseV2.PREPARING, phases.first())
            assertTrue(PortableBackupPhaseV2.WRITING_MEDIA in phases)
            assertTrue(PortableBackupPhaseV2.VERIFYING_BACKUP in phases)
            assertEquals(PortableBackupPhaseV2.COMPLETED, phases.last())

            val manifest = recovery.validatePortableBackupV2(backup)
            assertEquals(1, manifest.packages.single().contentCount)
            assertEquals(1, manifest.packages.single().learningItemCount)
            assertEquals(1, manifest.packages.single().mediaCount)
            assertEquals("selected-audio".toByteArray().size.toLong(), manifest.bytes.mediaBytes)
            ZipFile(backup.toFile()).use { zip ->
                val names = zip.entries().toList().map { it.name }
                assertTrue("portable/media/Vocabulary_In_Use_Upper_Intermediate/word.mp3" in names)
                assertTrue(names.none { "Other_Package" in it })
            }
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `selective backup rejects a selected package with a missing referenced media file`() {
        val root = Files.createTempDirectory("selective-missing-media-test-")
        try {
            val data = root.resolve("data")
            Files.createDirectories(data.resolve("media/Named_Package"))
            Files.writeString(data.resolve("installed-packages.json"),
                """{"records":[{"id":"installed","packageId":"package-id","name":"Named_Package","contentCount":1,"learningItemCount":0}]}""")
            Files.writeString(data.resolve("content-packages.json"),
                """{"records":[{"id":"package-id","name":"Named_Package","version":"1","format":"OPD3","libraryIds":["library-id"]}]}""")
            Files.writeString(data.resolve("content-libraries.json"),
                """{"records":[{"id":"library-id","name":"Named_Package","contentIds":["content-id"]}]}""")
            Files.writeString(data.resolve("contents.json"),
                """{"records":[{"id":"content-id","primaryAudio":"Named_Package/missing.mp3"}]}""")

            val recovery = JvmLearningDataRecoveryManager(
                roots = mapOf("data" to data, "media" to data.resolve("media")),
                safetyDirectory = root.resolve("safety")
            )
            val failure = kotlin.runCatching {
                recovery.createPortableBackupV2(
                    root.resolve("missing.lebak"),
                    PortableBackupV2Descriptor(
                        appVersion = "2.0.0",
                        sourcePlatform = "desktop",
                        specificPackageIds = setOf("package-id")
                    )
                )
            }.exceptionOrNull()

            assertNotNull(failure)
            assertTrue(failure.cause?.message.orEmpty().contains("media is incomplete"))
            assertTrue(Files.notExists(root.resolve("missing.lebak")))
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `backup cancellation removes temporary work and never publishes target`() {
        val root = Files.createTempDirectory("portable-cancel-test-")
        try {
            val data = root.resolve("data")
            Files.createDirectories(data)
            Files.writeString(data.resolve("installed-packages.json"), "{\"records\":[]}")
            val target = root.resolve("cancelled.lebak")
            val recovery = JvmLearningDataRecoveryManager(
                roots = mapOf("data" to data),
                safetyDirectory = root.resolve("safety")
            )

            val failure = runCatching {
                recovery.createPortableBackupV2(
                    target,
                    PortableBackupV2Descriptor(appVersion = "2.0.0", sourcePlatform = "desktop"),
                    shouldCancel = { true }
                )
            }.exceptionOrNull()

            assertNotNull(failure)
            assertTrue(failure.cause is PortableBackupCancelledException)
            assertTrue(Files.notExists(target))
            assertTrue(Files.list(root).use { paths ->
                paths.noneMatch { it.fileName.toString().startsWith(".learning-engine-") }
            })
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `selective backup includes only selected package data and manifest package list`() {
        val root = Files.createTempDirectory("selective-backup-test-")
        try {
            val appA = LearningApplicationFactory.createPersisted(root.resolve("node-a/data"), false)
            val libraryId = LibraryId("lib-default")
            val pkgA = InstalledPackage.reconstitute(
                id = InstalledPackageId("pkg-alpha-id"),
                libraryId = libraryId,
                packageId = PackageId("pkg-alpha"),
                topicId = TopicId("topic-alpha"),
                name = PackageName("Alpha Package"),
                version = PackageVersion("1.0.0"),
                state = PackageState.ACTIVE,
                installedAt = Instant.now(),
                contentCount = 1,
                learningItemCount = 1
            )
            val pkgB = InstalledPackage.reconstitute(
                id = InstalledPackageId("pkg-beta-id"),
                libraryId = libraryId,
                packageId = PackageId("pkg-beta"),
                topicId = TopicId("topic-beta"),
                name = PackageName("Beta Package"),
                version = PackageVersion("1.0.0"),
                state = PackageState.ACTIVE,
                installedAt = Instant.now(),
                contentCount = 1,
                learningItemCount = 1
            )
            appA.installedPackageRepository!!.save(pkgA)
            appA.installedPackageRepository!!.save(pkgB)

            val contentA = Content(
                id = ContentId("content-alpha-1"),
                type = ContentType.WORD,
                text = ContentText("Apple"),
                media = ContentMedia(primaryAudio = "pkg-alpha/apple.mp3")
            )
            val contentB = Content(
                id = ContentId("content-beta-1"),
                type = ContentType.WORD,
                text = ContentText("Banana"),
                media = ContentMedia(primaryAudio = "pkg-beta/banana.mp3")
            )
            appA.contentRepository!!.save(contentA)
            appA.contentRepository!!.save(contentB)
            Files.writeString(root.resolve("node-a/data/content-packages.json"), """
                {"schemaVersion":1,"records":[
                  {"id":"pkg-alpha","name":"Alpha Package","version":"1.0.0","format":"OPD3","libraryIds":["lib-alpha"]},
                  {"id":"pkg-beta","name":"Beta Package","version":"1.0.0","format":"OPD3","libraryIds":["lib-beta"]}
                ]}
            """.trimIndent())
            Files.writeString(root.resolve("node-a/data/content-libraries.json"), """
                {"schemaVersion":1,"records":[
                  {"id":"lib-alpha","name":"Alpha","contentIds":["content-alpha-1"]},
                  {"id":"lib-beta","name":"Beta","contentIds":["content-beta-1"]}
                ]}
            """.trimIndent())

            // Create media files
            val mediaRoot = root.resolve("node-a/media")
            Files.createDirectories(mediaRoot.resolve("pkg-alpha"))
            Files.createDirectories(mediaRoot.resolve("pkg-beta"))
            Files.writeString(mediaRoot.resolve("pkg-alpha/apple.mp3"), "alpha-audio")
            Files.writeString(mediaRoot.resolve("pkg-beta/banana.mp3"), "beta-audio")

            val backupPath = root.resolve("selective-backup.lebak")
            val recovery = JvmLearningDataRecoveryManager(
                roots = mapOf(
                    "data" to root.resolve("node-a/data"),
                    "media" to mediaRoot
                ),
                safetyDirectory = root.resolve("safety")
            )

            // Create selective backup of pkg-alpha only
            recovery.createPortableBackupV2(
                target = backupPath,
                descriptor = PortableBackupV2Descriptor(
                    appVersion = "2.0.0",
                    versionCode = 1,
                    sourcePlatform = "desktop",
                    learnerIds = listOf("learner-1"),
                    specificPackageIds = setOf("pkg-alpha")
                )
            )

            assertTrue(Files.exists(backupPath))

            // Inspect manifest
            val manifest = recovery.validatePortableBackupV2(backupPath)
            assertEquals(1, manifest.packages.size)
            assertEquals("pkg-alpha", manifest.packages.first().packageId)
            assertEquals("Alpha Package", manifest.packages.first().packageName)

            // Verify Zip entries contain only pkg-alpha media
            ZipFile(backupPath.toFile()).use { zip ->
                val entries = zip.entries().toList().map { it.name }
                assertTrue(entries.any { it.contains("pkg-alpha/apple.mp3") })
                assertTrue(entries.none { it.contains("pkg-beta/banana.mp3") })
            }
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `preview correctly identifies NEW, PRESENT, and CONFLICT package statuses`() {
        val root = Files.createTempDirectory("selective-preview-test-")
        try {
            val app = LearningApplicationFactory.createPersisted(root.resolve("local-node/data"), false)
            val libraryId = LibraryId("lib-default")
            val pkgPresent = InstalledPackage.reconstitute(
                id = InstalledPackageId("pkg-present-id"),
                libraryId = libraryId,
                packageId = PackageId("pkg-present"),
                topicId = TopicId("topic-1"),
                name = PackageName("Present Package"),
                version = PackageVersion("1.0.0"),
                state = PackageState.ACTIVE,
                installedAt = Instant.now(),
                contentCount = 2,
                learningItemCount = 2
            )
            val pkgConflict = InstalledPackage.reconstitute(
                id = InstalledPackageId("pkg-conflict-id"),
                libraryId = libraryId,
                packageId = PackageId("pkg-conflict"),
                topicId = TopicId("topic-2"),
                name = PackageName("Conflict Package"),
                version = PackageVersion("1.0.0"),
                state = PackageState.ACTIVE,
                installedAt = Instant.now(),
                contentCount = 5, // Local has 5 items
                learningItemCount = 5
            )
            app.installedPackageRepository!!.save(pkgPresent)
            app.installedPackageRepository!!.save(pkgConflict)

            // Create a backup on a remote node containing pkg-present, pkg-conflict (with 2 items), and pkg-new
            val remoteData = root.resolve("remote-node/data")
            val remoteApp = LearningApplicationFactory.createPersisted(remoteData, false)
            val remotePkgPresent = InstalledPackage.reconstitute(
                id = InstalledPackageId("pkg-present-id"),
                libraryId = libraryId,
                packageId = PackageId("pkg-present"),
                topicId = TopicId("topic-1"),
                name = PackageName("Present Package"),
                version = PackageVersion("1.0.0"),
                state = PackageState.ACTIVE,
                installedAt = Instant.now(),
                contentCount = 2,
                learningItemCount = 2
            )
            val remotePkgConflict = InstalledPackage.reconstitute(
                id = InstalledPackageId("pkg-conflict-id"),
                libraryId = libraryId,
                packageId = PackageId("pkg-conflict"),
                topicId = TopicId("topic-2"),
                name = PackageName("Conflict Package"),
                version = PackageVersion("1.0.0"),
                state = PackageState.ACTIVE,
                installedAt = Instant.now(),
                contentCount = 2, // Remote backup has 2 items vs local 5
                learningItemCount = 2
            )
            val remotePkgNew = InstalledPackage.reconstitute(
                id = InstalledPackageId("pkg-new-id"),
                libraryId = libraryId,
                packageId = PackageId("pkg-new"),
                topicId = TopicId("topic-3"),
                name = PackageName("Brand New Package"),
                version = PackageVersion("1.0.0"),
                state = PackageState.ACTIVE,
                installedAt = Instant.now(),
                contentCount = 10,
                learningItemCount = 10
            )
            remoteApp.installedPackageRepository!!.save(remotePkgPresent)
            remoteApp.installedPackageRepository!!.save(remotePkgConflict)
            remoteApp.installedPackageRepository!!.save(remotePkgNew)

            val backupPath = root.resolve("remote-backup.lebak")
            val remoteRecovery = JvmLearningDataRecoveryManager(
                roots = mapOf("data" to remoteData),
                safetyDirectory = root.resolve("safety")
            )
            remoteRecovery.createPortableBackupV2(
                target = backupPath,
                descriptor = PortableBackupV2Descriptor(
                    appVersion = "2.0.0",
                    versionCode = 1,
                    sourcePlatform = "desktop",
                    learnerIds = listOf("default-learner")
                )
            )

            // Preview backup on local node
            val localRecovery = JvmLearningDataRecoveryManager(
                roots = mapOf("data" to root.resolve("local-node/data")),
                safetyDirectory = root.resolve("safety")
            )
            val preview = localRecovery.previewPortableBackupV2(backupPath)

            assertEquals(3, preview.packagePreviews.size)
            val presentPreview = preview.packagePreviews.first { it.packageId == "pkg-present" }
            val conflictPreview = preview.packagePreviews.first { it.packageId == "pkg-conflict" }
            val newPreview = preview.packagePreviews.first { it.packageId == "pkg-new" }

            assertEquals(PackageCompatibilityStatus.PRESENT, presentPreview.status)
            assertEquals(PackageCompatibilityStatus.CONFLICT, conflictPreview.status)
            assertEquals(PackageCompatibilityStatus.NEW, newPreview.status)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `selective restore restores only target package and preserves other existing packages and media`() {
        val root = Files.createTempDirectory("selective-restore-test-")
        try {
            val libraryId = LibraryId("lib-default")
            // Local state on Device B: has Package Beta
            val localData = root.resolve("local/data")
            val localMedia = root.resolve("local/media")
            val localApp = LearningApplicationFactory.createPersisted(localData, false)
            val localPkgBeta = InstalledPackage.reconstitute(
                id = InstalledPackageId("pkg-beta-id"),
                libraryId = libraryId,
                packageId = PackageId("pkg-beta"),
                topicId = TopicId("topic-beta"),
                name = PackageName("Beta Local"),
                version = PackageVersion("1.0.0"),
                state = PackageState.ACTIVE,
                installedAt = Instant.now(),
                contentCount = 1,
                learningItemCount = 1
            )
            localApp.installedPackageRepository!!.save(localPkgBeta)
            localApp.contentRepository!!.save(
                Content(
                    id = ContentId("content-beta-1"),
                    type = ContentType.WORD,
                    text = ContentText("Beta Existing"),
                    media = ContentMedia(primaryAudio = "pkg-beta/beta.mp3")
                )
            )
            Files.writeString(localData.resolve("content-packages.json"),
                """{"schemaVersion":1,"records":[{"id":"pkg-beta","name":"Beta Local","version":"1.0.0","format":"OPD3","libraryIds":["lib-beta"]}]}""")
            Files.writeString(localData.resolve("content-libraries.json"),
                """{"schemaVersion":1,"records":[{"id":"lib-beta","name":"Beta","contentIds":["content-beta-1"]}]}""")
            Files.createDirectories(localMedia.resolve("pkg-beta"))
            Files.writeString(localMedia.resolve("pkg-beta/beta.mp3"), "local-beta-sound")

            // Backup from Device A contains Package Alpha and Package Gamma
            val remoteData = root.resolve("remote/data")
            val remoteMedia = root.resolve("remote/media")
            val remoteApp = LearningApplicationFactory.createPersisted(remoteData, false)
            val pkgAlpha = InstalledPackage.reconstitute(
                id = InstalledPackageId("pkg-alpha-id"),
                libraryId = libraryId,
                packageId = PackageId("pkg-alpha"),
                topicId = TopicId("topic-alpha"),
                name = PackageName("Alpha Package"),
                version = PackageVersion("1.0.0"),
                state = PackageState.ACTIVE,
                installedAt = Instant.now(),
                contentCount = 1,
                learningItemCount = 1
            )
            val pkgGamma = InstalledPackage.reconstitute(
                id = InstalledPackageId("pkg-gamma-id"),
                libraryId = libraryId,
                packageId = PackageId("pkg-gamma"),
                topicId = TopicId("topic-gamma"),
                name = PackageName("Gamma Package"),
                version = PackageVersion("1.0.0"),
                state = PackageState.ACTIVE,
                installedAt = Instant.now(),
                contentCount = 1,
                learningItemCount = 1
            )
            remoteApp.installedPackageRepository!!.save(pkgAlpha)
            remoteApp.installedPackageRepository!!.save(pkgGamma)
            remoteApp.contentRepository!!.save(
                Content(
                    id = ContentId("content-alpha-1"),
                    type = ContentType.WORD,
                    text = ContentText("Alpha New"),
                    media = ContentMedia(primaryAudio = "pkg-alpha/alpha.mp3")
                )
            )
            Files.writeString(remoteData.resolve("content-packages.json"), """
                {"schemaVersion":1,"records":[
                  {"id":"pkg-alpha","name":"Alpha Package","version":"1.0.0","format":"OPD3","libraryIds":["lib-alpha"]},
                  {"id":"pkg-gamma","name":"Gamma Package","version":"1.0.0","format":"OPD3","libraryIds":["lib-gamma"]}
                ]}
            """.trimIndent())
            Files.writeString(remoteData.resolve("content-libraries.json"), """
                {"schemaVersion":1,"records":[
                  {"id":"lib-alpha","name":"Alpha","contentIds":["content-alpha-1"]},
                  {"id":"lib-gamma","name":"Gamma","contentIds":["content-gamma-1"]}
                ]}
            """.trimIndent())
            remoteApp.contentRepository!!.save(
                Content(
                    id = ContentId("content-gamma-1"),
                    type = ContentType.WORD,
                    text = ContentText("Gamma Remote"),
                    media = ContentMedia(primaryAudio = "pkg-gamma/gamma.mp3")
                )
            )
            Files.createDirectories(remoteMedia.resolve("pkg-alpha"))
            Files.createDirectories(remoteMedia.resolve("pkg-gamma"))
            Files.writeString(remoteMedia.resolve("pkg-alpha/alpha.mp3"), "remote-alpha-sound")
            Files.writeString(remoteMedia.resolve("pkg-gamma/gamma.mp3"), "remote-gamma-sound")

            val backupFile = root.resolve("device-a-backup.lebak")
            val remoteRecovery = JvmLearningDataRecoveryManager(
                roots = mapOf("data" to remoteData, "media" to remoteMedia),
                safetyDirectory = root.resolve("safety")
            )
            remoteRecovery.createPortableBackupV2(
                target = backupFile,
                descriptor = PortableBackupV2Descriptor(
                    appVersion = "2.0.0",
                    versionCode = 1,
                    sourcePlatform = "desktop",
                    learnerIds = listOf("default-learner")
                )
            )

            // Selectively restore ONLY Package Alpha to Device B
            val localRecovery = JvmLearningDataRecoveryManager(
                roots = mapOf("data" to localData, "media" to localMedia),
                safetyDirectory = root.resolve("safety")
            )

            val restoreResult = localRecovery.restorePortableBackupV2(
                source = backupFile,
                selectedPackageIds = setOf("pkg-alpha")
            )

            assertTrue(restoreResult is PortableBackupV2RestoreResult.Success)

            // Reload local app and verify isolation
            val reloadedLocalApp = LearningApplicationFactory.createPersisted(localData, false)
            val installed = reloadedLocalApp.installedPackageRepository!!.findAll()

            // Both pkg-beta (existing) and pkg-alpha (restored) must exist, but NOT pkg-gamma
            assertEquals(2, installed.size)
            assertTrue(installed.any { it.packageId.value == "pkg-beta" })
            assertTrue(installed.any { it.packageId.value == "pkg-alpha" })
            assertTrue(installed.none { it.packageId.value == "pkg-gamma" })

            // Check content isolation
            val allContents = reloadedLocalApp.contentRepository!!.findAll()
            assertEquals(2, allContents.size)
            assertTrue(allContents.any { it.id.value == "content-beta-1" })
            assertTrue(allContents.any { it.id.value == "content-alpha-1" })
            assertTrue(allContents.none { it.id.value == "content-gamma-1" })

            // Check media isolation
            assertTrue(Files.exists(localMedia.resolve("pkg-beta/beta.mp3")))
            assertEquals("local-beta-sound", Files.readString(localMedia.resolve("pkg-beta/beta.mp3")))
            assertTrue(Files.exists(localMedia.resolve("pkg-alpha/alpha.mp3")))
            assertEquals("remote-alpha-sound", Files.readString(localMedia.resolve("pkg-alpha/alpha.mp3")))
            assertTrue(!Files.exists(localMedia.resolve("pkg-gamma")))
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}
