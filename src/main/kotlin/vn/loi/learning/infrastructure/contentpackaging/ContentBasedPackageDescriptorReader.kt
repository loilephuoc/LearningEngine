package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Path
import vn.loi.learning.application.contentpackaging.PackageDescriptorReader
import vn.loi.learning.application.contentpackaging.PackageScanCandidate
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor

class ContentBasedPackageDescriptorReader(
    private val archiveReader: PackageDescriptorReader,
    private val formatDetector: JvmPackageFormatDetector = JvmPackageFormatDetector(),
    private val pairResolver: JvmOpd3PairResolver = JvmOpd3PairResolver()
) : PackageDescriptorReader {

    override fun read(candidate: PackageScanCandidate): PackageDescriptor {
        val source = Path.of(candidate.source)
        return when (formatDetector.detect(source)) {
            JvmPackageFormat.ZIP_ARCHIVE -> archiveReader.read(candidate)
            JvmPackageFormat.OPD3_BINARY_PAIR -> {
                val pair = pairResolver.resolve(source)
                PackageDescriptor(
                    name = Path.of(pair.jsonSource).fileName.toString().substringBeforeLast('.'),
                    version = "1.0.0",
                    format = "OPD3"
                )
            }
        }
    }
}
