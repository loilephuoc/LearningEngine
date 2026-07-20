package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor

class PackageExportMetadataSerializerTest {

    @Test
    fun `serializes metadata to json`() {
        val descriptor = PackageDescriptor(
            name = "demo-package",
            version = "1.0.0",
            format = "OPD3"
        )

        val text = PackageExportMetadataSerializer().serialize(descriptor)
        val json = Json.parseToJsonElement(text).jsonObject

        assertEquals("demo-package", json.getValue("name").jsonPrimitive.content)
        assertEquals("1.0.0", json.getValue("version").jsonPrimitive.content)
        assertEquals("OPD3", json.getValue("format").jsonPrimitive.content)
    }
}
