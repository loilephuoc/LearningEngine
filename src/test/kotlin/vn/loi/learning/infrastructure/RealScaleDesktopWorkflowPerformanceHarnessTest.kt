package vn.loi.learning.infrastructure

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import vn.loi.learning.application.session.StartStudySessionCommand
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy

class RealScaleDesktopWorkflowPerformanceHarnessTest {
    @Test
    fun `synthetic real scale import library query and study preparation use production boundaries`() {
        val root = Files.createTempDirectory("desktop-real-scale-harness")
        val data = root.resolve("data")
        val packages = root.resolve("packages").also(Files::createDirectories)
        try {
            val records = (0 until 2_425).joinToString(",") { index ->
                """{"group":"Scale","section":"Section ${index / 100}","lesson":"Lesson $index","en":"Sentence $index","vi":"Cau $index","audio":"audio-$index.mp3"}"""
            }
            Files.writeString(packages.resolve("scale.json"), "[$records]")
            Files.write(
                packages.resolve("scale.pkg"),
                ByteBuffer.allocate(12).order(ByteOrder.BIG_ENDIAN)
                    .put("OPD3".toByteArray(Charsets.US_ASCII))
                    .putInt(1)
                    .putInt(0)
                    .array()
            )

            val context = LearningApplicationFactory.createPersisted(data)
            var importedItems = 0
            val importMillis = measureTimeMillis {
                val result = context.packageImporter(packages)
                    .importAllDetailed(PackageCatalogId("scale-catalog"))
                assertEquals(1, result.successfulImports.size, result.failures.toString())
                importedItems = result.successfulImports.single().importedLearningItemCount
            }
            assertEquals(12_125, importedItems)

            var libraryItems = 0
            val libraryMillis = measureTimeMillis {
                val library = context.contentLibraries.query().single()
                libraryItems = context.libraryContents
                    .query(vn.loi.learning.domain.content.library.model.ContentLibraryId(library.id))
                    .size
            }
            assertEquals(2_425, libraryItems)

            val studyMillis = measureTimeMillis {
                val session = context.engine.startSession(
                    StartStudySessionCommand(
                        SessionId("scale-session"),
                        LearnerId("scale-learner"),
                        Moment(1_000L),
                        SessionPolicy(newItemLimit = 20, reviewItemLimit = 20)
                    )
                )
                assertNotNull(context.engine.getNextSessionItem(session.id, Moment(1_001L)))
            }
            println(
                "real-scale-import-ms=$importMillis library-query-ms=$libraryMillis " +
                    "study-preparation-ms=$studyMillis contents=$libraryItems learningItems=$importedItems"
            )
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}
