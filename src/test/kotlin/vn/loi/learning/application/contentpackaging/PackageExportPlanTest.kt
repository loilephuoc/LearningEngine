package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class PackageExportPlanTest {

    @Test
    fun `rejects duplicate relative paths`() {
        assertFailsWith<IllegalArgumentException> {
            PackageExportPlan(
                files = listOf(
                    PackageExportFile("manifest.json", "{}"),
                    PackageExportFile("manifest.json", "{}")
                )
            )
        }
    }

    @Test
    fun `provides ordered file access`() {
        val metadata = PackageExportFile("metadata.json", "{}")
        val manifest = PackageExportFile("manifest.json", "{}")
        val plan = PackageExportPlan(listOf(metadata, manifest))

        assertEquals(2, plan.size)
        assertEquals(false, plan.isEmpty())
        assertEquals(true, plan.isNotEmpty())
        assertEquals(setOf("metadata.json", "manifest.json"), plan.relativePaths)
        assertEquals(listOf("metadata.json", "manifest.json"), plan.relativePathSequence().toList())
        assertEquals(listOf(metadata, manifest), plan.fileSequence().toList())
        assertEquals(listOf(metadata, manifest), plan.toList())
        assertSame(metadata, plan["metadata.json"])
        assertSame(metadata, plan.requireFile("metadata.json"))
        assertEquals(true, plan.contains("metadata.json"))
        assertEquals(false, plan.contains("contents.json"))
        assertFailsWith<IllegalArgumentException> { plan.requireFile("contents.json") }

        val iterator = plan.iterator()
        assertEquals(true, iterator.hasNext())
        assertSame(metadata, iterator.next())
        assertEquals(true, iterator.hasNext())
        assertSame(manifest, iterator.next())
        assertEquals(false, iterator.hasNext())
    }
}
