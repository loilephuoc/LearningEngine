package vn.loi.learning.adapter.jvm

import java.nio.file.Path
import vn.loi.learning.application.importing.ImportContentResult
import vn.loi.learning.application.importing.LegacyJsonImportService

/**
 * Adapter JVM nối việc đọc file với LegacyJsonImportService.
 */
class LegacyJsonFileImportService(
    private val importService: LegacyJsonImportService,
    private val fileReader: JvmJsonFileReader =
        JvmJsonFileReader()
) {

    fun import(path: Path): ImportContentResult {
        val jsonText = fileReader.read(path)

        return importService.import(
            sourceName = path.fileName.toString(),
            jsonText = jsonText
        )
    }
}