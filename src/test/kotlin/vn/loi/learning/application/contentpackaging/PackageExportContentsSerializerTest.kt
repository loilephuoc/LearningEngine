package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class PackageExportContentsSerializerTest {

    @Test
    fun `serializes empty contents to json`() {
        val text = PackageExportContentsSerializer().serialize(emptyList())
        val json = Json.parseToJsonElement(text).jsonObject

        assertEquals(0, json.getValue("contents").jsonArray.size)
    }
}
