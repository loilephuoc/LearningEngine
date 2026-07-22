package vn.loi.learning.desktop.runtime

import java.io.InputStream
import java.util.Properties

data class DesktopDistributionMetadata(
    val packageName: String,
    val packageVersion: String,
    val windowsFormats: List<String>
) {
    init {
        require(packageName.isNotBlank())
        require(packageVersion.matches(Regex("\\d+\\.\\d+\\.\\d+")))
        require(windowsFormats.isNotEmpty())
        require(windowsFormats.all { it in SUPPORTED_WINDOWS_FORMATS })
    }

    companion object {
        val SUPPORTED_WINDOWS_FORMATS: Set<String> = setOf("msi", "exe")
    }
}

object DesktopDistributionMetadataLoader {
    private const val RESOURCE_NAME = "/learning-engine-build.properties"

    fun load(): DesktopDistributionMetadata =
        checkNotNull(
            DesktopDistributionMetadataLoader::class.java.getResourceAsStream(RESOURCE_NAME)
        ) { "Missing desktop distribution metadata resource: $RESOURCE_NAME" }
            .use(::load)

    internal fun load(input: InputStream): DesktopDistributionMetadata {
        val properties = Properties().apply { load(input.reader(Charsets.UTF_8)) }
        return DesktopDistributionMetadata(
            packageName = properties.required("distribution.package.name"),
            packageVersion = properties.required("distribution.package.version"),
            windowsFormats =
                properties.required("distribution.windows.formats")
                    .split(',')
                    .map(String::trim)
                    .filter(String::isNotEmpty)
        )
    }

    private fun Properties.required(key: String): String =
        getProperty(key)?.trim()?.takeIf(String::isNotEmpty)
            ?: throw IllegalStateException(
                "Missing desktop distribution metadata property: $key"
            )
}
