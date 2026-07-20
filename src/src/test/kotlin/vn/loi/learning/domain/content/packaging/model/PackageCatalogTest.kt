package vn.loi.learning.domain.content.packaging.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PackageCatalogTest {

    private val catalogId =
        PackageCatalogId(
            "catalog-1"
        )

    @Test
    fun `catalog ID rejects blank value`() {
        assertFailsWith<IllegalArgumentException> {
            PackageCatalogId(" ")
        }
    }

    @Test
    fun `new catalog is empty`() {
        val catalog =
            PackageCatalog(
                id = catalogId
            )

        assertTrue(
            catalog.isEmpty
        )

        assertEquals(
            0,
            catalog.packageCount
        )
    }

    @Test
    fun `register adds package immutably`() {
        val packageId =
            PackageId(
                "package-1"
            )

        val original =
            PackageCatalog(
                id = catalogId
            )

        val updated =
            original.register(
                packageId
            )

        assertTrue(
            original.isEmpty
        )

        assertFalse(
            updated.isEmpty
        )

        assertTrue(
            updated.contains(
                packageId
            )
        )

        assertEquals(
            1,
            updated.packageCount
        )
    }

    @Test
    fun `register existing package returns same instance`() {
        val packageId =
            PackageId(
                "package-1"
            )

        val catalog =
            PackageCatalog(
                id = catalogId,
                packageIds = setOf(packageId)
            )

        assertSame(
            catalog,
            catalog.register(packageId)
        )
    }

    @Test
    fun `registerAll adds only missing packages`() {
        val first =
            PackageId(
                "package-1"
            )

        val second =
            PackageId(
                "package-2"
            )

        val catalog =
            PackageCatalog(
                id = catalogId,
                packageIds = setOf(first)
            )

        val updated =
            catalog.registerAll(
                setOf(first, second)
            )

        assertEquals(
            setOf(first, second),
            updated.packageIds
        )

        assertEquals(
            2,
            updated.packageCount
        )
    }

    @Test
    fun `registerAll without changes returns same instance`() {
        val packageId =
            PackageId(
                "package-1"
            )

        val catalog =
            PackageCatalog(
                id = catalogId,
                packageIds = setOf(packageId)
            )

        assertSame(
            catalog,
            catalog.registerAll(
                setOf(packageId)
            )
        )
    }

    @Test
    fun `remove deletes package immutably`() {
        val packageId =
            PackageId(
                "package-1"
            )

        val original =
            PackageCatalog(
                id = catalogId,
                packageIds = setOf(packageId)
            )

        val updated =
            original.remove(
                packageId
            )

        assertTrue(
            original.contains(packageId)
        )

        assertFalse(
            updated.contains(packageId)
        )

        assertTrue(
            updated.isEmpty
        )
    }

    @Test
    fun `remove missing package returns same instance`() {
        val catalog =
            PackageCatalog(
                id = catalogId
            )

        assertSame(
            catalog,
            catalog.remove(
                PackageId(
                    "missing-package"
                )
            )
        )
    }
}
