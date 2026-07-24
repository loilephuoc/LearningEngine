package vn.loi.learning.application.packageprogress

import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment

data class PackageLearningProgressQuery(
    val installedPackageId: InstalledPackageId,
    val learnerId: LearnerId,
    val at: Moment
)
