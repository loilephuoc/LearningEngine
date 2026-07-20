package vn.loi.learning.application.contentpackaging

import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.domain.content.packaging.model.PackageCatalog

class UpgradeContentPackageUseCase(
    private val upgradeOperation:
    PackageUpgradeOperation,
    private val transactionRunner:
    TransactionRunner
) {

    fun execute(
        command: UpgradeContentPackageCommand
    ): PackageCatalog =
        transactionRunner.runInTransaction {
            upgradeOperation.execute(
                command
            )
        }
}