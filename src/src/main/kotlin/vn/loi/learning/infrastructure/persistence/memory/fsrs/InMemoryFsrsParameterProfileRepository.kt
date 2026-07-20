package vn.loi.learning.infrastructure.persistence.memory.fsrs

import java.util.LinkedHashMap
import vn.loi.learning.domain.study.fsrs.calibration.model.FsrsParameterProfile
import vn.loi.learning.domain.study.fsrs.calibration.model.FsrsParameterProfileId
import vn.loi.learning.domain.study.fsrs.calibration.repository.FsrsParameterProfileRepository

/**
 * In-memory implementation của FSRS Parameter Profile repository.
 *
 * Dùng cho:
 * - unit test;
 * - local development;
 * - composition root trước khi có persistence chính thức.
 */
class InMemoryFsrsParameterProfileRepository(
    initialProfiles: Iterable<FsrsParameterProfile> =
        listOf(FsrsParameterProfile.DEFAULT)
) : FsrsParameterProfileRepository {

    private val profiles =
        LinkedHashMap<
                FsrsParameterProfileId,
                FsrsParameterProfile
                >()

    init {
        initialProfiles.forEach(::save)
    }

    override fun save(
        profile: FsrsParameterProfile
    ) {
        profiles[profile.id] = profile
    }

    override fun findById(
        id: FsrsParameterProfileId
    ): FsrsParameterProfile? =
        profiles[id]

    override fun findAll():
            List<FsrsParameterProfile> =
        profiles.values.toList()
}