package vn.loi.learning.infrastructure.persistence.memory.fsrs

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.fsrs.calibration.model.FsrsParameterProfile
import vn.loi.learning.domain.study.fsrs.calibration.model.FsrsParameterProfileId
import vn.loi.learning.domain.study.fsrs.calibration.model.FsrsParameterProfileSource
import vn.loi.learning.domain.study.fsrs.model.DesiredRetention
import vn.loi.learning.domain.study.fsrs.model.FsrsConfiguration
import vn.loi.learning.domain.study.fsrs.model.FsrsParameters

class InMemoryFsrsParameterProfileRepositoryTest {

    @Test
    fun `repository contains default profile initially`() {
        val repository =
            InMemoryFsrsParameterProfileRepository()

        assertTrue(
            repository.exists(
                FsrsParameterProfile.DEFAULT.id
            )
        )

        assertSame(
            FsrsParameterProfile.DEFAULT,
            repository.findById(
                FsrsParameterProfile.DEFAULT.id
            )
        )
    }

    @Test
    fun `repository can be created without initial profiles`() {
        val repository =
            InMemoryFsrsParameterProfileRepository(
                initialProfiles = emptyList()
            )

        assertTrue(
            repository.findAll().isEmpty()
        )

        assertFalse(
            repository.exists(
                FsrsParameterProfile.DEFAULT.id
            )
        )
    }

    @Test
    fun `save stores profile`() {
        val repository =
            InMemoryFsrsParameterProfileRepository(
                initialProfiles = emptyList()
            )

        val profile =
            customProfile()

        repository.save(profile)

        assertSame(
            profile,
            repository.findById(profile.id)
        )

        assertTrue(
            repository.exists(profile.id)
        )
    }

    @Test
    fun `save replaces profile with same ID`() {
        val repository =
            InMemoryFsrsParameterProfileRepository(
                initialProfiles = emptyList()
            )

        val original =
            customProfile(
                name = "Original Profile",
                desiredRetention = 0.90
            )

        val replacement =
            customProfile(
                name = "Updated Profile",
                desiredRetention = 0.95
            )

        repository.save(original)
        repository.save(replacement)

        assertEquals(
            1,
            repository.findAll().size
        )

        assertSame(
            replacement,
            repository.findById(original.id)
        )
    }

    @Test
    fun `findById returns null for unknown profile`() {
        val repository =
            InMemoryFsrsParameterProfileRepository(
                initialProfiles = emptyList()
            )

        assertNull(
            repository.findById(
                FsrsParameterProfileId(
                    "unknown-profile"
                )
            )
        )
    }

    @Test
    fun `findAll preserves insertion order`() {
        val repository =
            InMemoryFsrsParameterProfileRepository(
                initialProfiles = emptyList()
            )

        val first =
            customProfile(
                id = "first-profile",
                name = "First Profile"
            )

        val second =
            customProfile(
                id = "second-profile",
                name = "Second Profile"
            )

        repository.save(first)
        repository.save(second)

        assertEquals(
            listOf(first, second),
            repository.findAll()
        )
    }

    private fun customProfile(
        id: String = "custom-profile",
        name: String = "Custom Profile",
        desiredRetention: Double = 0.90
    ): FsrsParameterProfile =
        FsrsParameterProfile(
            id =
                FsrsParameterProfileId(id),
            name = name,
            source =
                FsrsParameterProfileSource.CALIBRATED,
            configuration =
                FsrsConfiguration(
                    parameters =
                        FsrsParameters.DEFAULT,
                    desiredRetention =
                        DesiredRetention(
                            desiredRetention
                        )
                )
        )
}