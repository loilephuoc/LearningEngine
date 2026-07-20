package vn.loi.learning.domain.study.fsrs.calibration.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import vn.loi.learning.domain.study.fsrs.model.DesiredRetention
import vn.loi.learning.domain.study.fsrs.model.FsrsConfiguration
import vn.loi.learning.domain.study.fsrs.model.FsrsParameters

class FsrsParameterProfileTest {

    @Test
    fun `default profile uses default FSRS configuration`() {
        val profile =
            FsrsParameterProfile.DEFAULT

        assertEquals(
            FsrsParameterProfileId(
                "fsrs-default-v6"
            ),
            profile.id
        )

        assertEquals(
            "FSRS Default v6",
            profile.name
        )

        assertEquals(
            FsrsParameterProfileSource.DEFAULT,
            profile.source
        )

        assertSame(
            FsrsConfiguration.DEFAULT,
            profile.configuration
        )
    }

    @Test
    fun `profile preserves supplied values`() {
        val configuration =
            FsrsConfiguration(
                parameters =
                    FsrsParameters.DEFAULT,
                desiredRetention =
                    DesiredRetention(0.95)
            )

        val profile =
            FsrsParameterProfile(
                id =
                    FsrsParameterProfileId(
                        "personal-profile"
                    ),
                name =
                    "Personal Profile",
                source =
                    FsrsParameterProfileSource.CALIBRATED,
                configuration =
                    configuration
            )

        assertEquals(
            FsrsParameterProfileId(
                "personal-profile"
            ),
            profile.id
        )

        assertEquals(
            "Personal Profile",
            profile.name
        )

        assertEquals(
            FsrsParameterProfileSource.CALIBRATED,
            profile.source
        )

        assertSame(
            configuration,
            profile.configuration
        )
    }

    @Test
    fun `profile ID rejects blank value`() {
        assertFailsWith<IllegalArgumentException> {
            FsrsParameterProfileId(" ")
        }
    }

    @Test
    fun `profile ID rejects surrounding whitespace`() {
        assertFailsWith<IllegalArgumentException> {
            FsrsParameterProfileId(
                " personal-profile "
            )
        }
    }

    @Test
    fun `profile rejects blank name`() {
        assertFailsWith<IllegalArgumentException> {
            FsrsParameterProfile(
                id =
                    FsrsParameterProfileId(
                        "invalid-name"
                    ),
                name = " ",
                source =
                    FsrsParameterProfileSource.IMPORTED,
                configuration =
                    FsrsConfiguration.DEFAULT
            )
        }
    }

    @Test
    fun `profile rejects name with surrounding whitespace`() {
        assertFailsWith<IllegalArgumentException> {
            FsrsParameterProfile(
                id =
                    FsrsParameterProfileId(
                        "invalid-name"
                    ),
                name =
                    " Imported Profile ",
                source =
                    FsrsParameterProfileSource.IMPORTED,
                configuration =
                    FsrsConfiguration.DEFAULT
            )
        }
    }
}