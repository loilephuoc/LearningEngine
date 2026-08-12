package vn.loi.learning.application.packageprogress

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.application.LearningEngine
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.infrastructure.LearningApplicationFactory

class PackageLearningProgressQueryPerformanceTest {

    @Test
    fun `multi-package progress and ratings share one learning-item snapshot`() {
        val tempDir = Files.createTempDirectory("batch-progress-packages")
        val persistenceDir = Files.createTempDirectory("batch-progress-db")
        try {
            val first = tempDir.resolve("First.opd3")
            val second = tempDir.resolve("Second.opd3")
            createLargeOpd3ZipPackage(first, lessonCount = 3, idPrefix = "first")
            createLargeOpd3ZipPackage(second, lessonCount = 2, idPrefix = "second")

            val context = LearningApplicationFactory.createPersisted(persistenceDir)
            context.packageImporter(first).importAllDetailed(vn.loi.learning.domain.content.packaging.model.PackageCatalogId("first-catalog"))
            context.packageImporter(second).importAllDetailed(vn.loi.learning.domain.content.packaging.model.PackageCatalogId("second-catalog"))
            val packageIds = context.installedPackages.query().map { InstalledPackageId(it.id) }

            val engine = context.engine
            val repoField = LearningEngine::class.java.getDeclaredField("learningItemRepository")
            repoField.isAccessible = true
            val delegate = repoField.get(engine) as LearningItemRepository
            var findByContentIdsCalls = 0
            var findAllEnabledCalls = 0
            repoField.set(engine, object : LearningItemRepository by delegate {
                override fun findByContentIds(contentIds: Set<ContentId>): List<LearningItem> {
                    findByContentIdsCalls++
                    return delegate.findByContentIds(contentIds)
                }

                override fun findAllEnabled(): List<LearningItem> {
                    findAllEnabledCalls++
                    return delegate.findAllEnabled()
                }
            })

            val result = requireNotNull(context.packageProgress).executeAllWithLatestRatings(
                packageIds,
                LearnerId("batch-learner"),
                Moment(System.currentTimeMillis()),
                requireNotNull(context.packageLatestRatings)
            )

            assertEquals(setOf(2, 3), result.progress.values.map { it.getOrThrow().totalLearningItemCount }.toSet())
            assertTrue(result.latestRatings.values.all { it.getOrThrow() == PackageLatestRatingDistribution.EMPTY })
            assertEquals(1, findByContentIdsCalls)
            assertEquals(0, findAllEnabledCalls)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `1 progress calculation for 400 lessons executes single findByContentIds batch query without per-content loop`() {
        val tempDir = Files.createTempDirectory("perf-test-dir")
        val persistenceDir = Files.createTempDirectory("perf-test-db")
        try {
            val opd3File = tempDir.resolve("LargePkg.opd3")
            createLargeOpd3ZipPackage(opd3File, lessonCount = 400)

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val importer = appContext.packageImporter(opd3File)
            val result = importer.importAllDetailed(vn.loi.learning.domain.content.packaging.model.PackageCatalogId("test-catalog"))

            assertEquals(1, result.successfulImports.size)

            val installedPkg = appContext.installedPackages.query().first()
            val pkgId = InstalledPackageId(installedPkg.id)

            var findByContentIdsCallCount = 0
            var findByContentIdCallCount = 0

            val realEngine = appContext.engine
            val repoField = LearningEngine::class.java.getDeclaredField("learningItemRepository")
            repoField.isAccessible = true
            val realItemRepo = repoField.get(realEngine) as LearningItemRepository

            val countingItemRepo = object : LearningItemRepository {
                override fun findById(learningItemId: LearningItemId): LearningItem? = realItemRepo.findById(learningItemId)
                override fun findByContentId(contentId: ContentId): List<LearningItem> {
                    findByContentIdCallCount++
                    return realItemRepo.findByContentId(contentId)
                }
                override fun findByContentIds(contentIds: Set<ContentId>): List<LearningItem> {
                    findByContentIdsCallCount++
                    return realItemRepo.findByContentIds(contentIds)
                }
                override fun findAllEnabled(): List<LearningItem> = realItemRepo.findAllEnabled()
                override fun save(learningItem: LearningItem) = realItemRepo.save(learningItem)
                override fun deleteById(learningItemId: LearningItemId) = realItemRepo.deleteById(learningItemId)
            }

            repoField.set(realEngine, countingItemRepo)

            val progressService = appContext.packageProgress!!

            val startTime = System.currentTimeMillis()
            val progress = progressService.execute(
                PackageLearningProgressQuery(
                    installedPackageId = pkgId,
                    learnerId = LearnerId("perf-learner"),
                    at = Moment(System.currentTimeMillis())
                )
            )
            val duration = System.currentTimeMillis() - startTime

            assertEquals(400, progress.totalLessonCount)
            assertEquals(400, progress.totalLearningItemCount)

            assertEquals(1, findByContentIdsCallCount, "findByContentIds must be called exactly once for batch operation")
            assertEquals(0, findByContentIdCallCount, "findByContentId must NOT be called in a loop for individual items")

            assertTrue(duration < 2_000, "Progress calculation for 400 lessons executed safely in ${duration}ms")
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    private fun createLargeOpd3ZipPackage(file: Path, lessonCount: Int, idPrefix: String = "large") {
        ZipOutputStream(Files.newOutputStream(file)).use { zip ->
            writeZipEntry(
                zip,
                "manifest.json",
                """{ "name": "Large Pkg $idPrefix", "version": "1.0.0", "format": "OPD3", "schemaVersion": 1, "contentCount": $lessonCount, "learningItemCount": $lessonCount }"""
            )
            writeZipEntry(zip, "metadata.json", """{ "name": "Large Pkg $idPrefix", "version": "1.0.0", "format": "OPD3" }""")

            val contentsJson = buildString {
                append("""{ "contents": [""")
                (1..lessonCount).forEach { i ->
                    if (i > 1) append(",")
                    append("""{ "id": "cnt-$idPrefix-$i", "type": "SENTENCE", "primaryText": "Sentence $i", "translatedText": "Cau $i", "title": "Sentence $i", "group": "Group ${i / 20}", "section": "Section ${i / 5}", "lesson": "Lesson $i" }""")
                }
                append("""] }""")
            }
            writeZipEntry(zip, "contents.json", contentsJson)

            val itemsJson = buildString {
                append("""{ "learningItems": [""")
                (1..lessonCount).forEach { i ->
                    if (i > 1) append(",")
                    append("""{ "id": "cnt-$idPrefix-$i-item", "contentId": "cnt-$idPrefix-$i", "mode": "MEANING_RECOGNITION", "isEnabled": true }""")
                }
                append("""] }""")
            }
            writeZipEntry(zip, "learning-items.json", itemsJson)
        }
    }

    private fun writeZipEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()
    }
}
