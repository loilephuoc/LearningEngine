package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class PackageExportLearningItemsSerializerTest {

    @Test
    fun `serializes empty learning items to json`() {
        val text = PackageExportLearningItemsSerializer().serialize(emptyList())
        val json = Json.parseToJsonElement(text).jsonObject

        assertEquals(0, json.getValue("learningItems").jsonArray.size)
    }
}
