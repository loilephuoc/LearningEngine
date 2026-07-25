package vn.loi.learning.application.contentpackaging.export

fun interface ExportContentPackageUseCase {
    fun execute(command: ExportContentPackageCommand): ExportContentPackageResult
}
