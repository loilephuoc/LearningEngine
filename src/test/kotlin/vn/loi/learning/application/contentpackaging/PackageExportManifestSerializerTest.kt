package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import vn.loi.learning.domain.content.packaging.model.PackageDependency

class PackageExportManifestSerializerTest {

    @Test
    fun `serializes manifest to opd3 json`() {
        val manifest =
            PackageExportManifest(
                name =
                    "demo-package",
                version =
                    "1.0.0",
                format =
                    "OPD3",
                contentCount =
                    12,
                learningItemCount =
                    48,
                dependencies =
                    setOf(
                        PackageDependency(
                            packageName =
                                "core-package",
                            minimumVersion =
                                "1.0.0",
                            maximumVersion =
                                "2.0.0"
                        )
                    )
            )

        val text =
            PackageExportManifestSerializer()
                .serialize(
                    manifest
                )

        val json =
            Json.parseToJsonElement(
                text
            ).jsonObject

        assertEquals(
            "demo-package",
            json.getValue(
                "name"
            ).jsonPrimitive.content
        )

        assertEquals(
            "1.0.0",
            json.getValue(
                "version"
            ).jsonPrimitive.content
        )

        assertEquals(
            "OPD3",
            json.getValue(
                "format"
            ).jsonPrimitive.content
        )

        assertEquals(
            12,
            json.getValue(
                "contentCount"
            ).jsonPrimitive.int
        )

        assertEquals(
            48,
            json.getValue(
                "learningItemCount"
            ).jsonPrimitive.int
        )

        val dependency =
            json.getValue(
                "dependencies"
            ).jsonArray
                .single()
                .jsonObject

        assertEquals(
            "core-package",
            dependency.getValue(
                "packageName"
            ).jsonPrimitive.content
        )

        assertEquals(
            "1.0.0",
            dependency.getValue(
                "minimumVersion"
            ).jsonPrimitive.content
        )

        assertEquals(
            "2.0.0",
            dependency.getValue(
                "maximumVersion"
            ).jsonPrimitive.content
        )
    }

    @Test
    fun `deserializes legacy manifest without dependencies`() {
        val content =
            """
            {
              "name": "legacy-package",
              "version": "1.0.0",
              "format": "OPD3",
              "contentCount": 3,
              "learningItemCount": 6
            }
            """.trimIndent()

        val manifest =
            PackageExportManifestSerializer()
                .deserialize(
                    content
                )

        assertEquals(
            emptySet(),
            manifest.dependencies
        )
    }
}