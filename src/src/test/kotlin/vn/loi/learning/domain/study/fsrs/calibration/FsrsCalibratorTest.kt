package vn.loi.learning.domain.study.fsrs.calibration

import kotlin.test.Test
import kotlin.test.assertSame
import vn.loi.learning.domain.study.fsrs.calibration.model.ReviewHistoryDataset
import vn.loi.learning.domain.study.fsrs.model.FsrsParameters
import vn.loi.learning.domain.study.memory.model.LearnerId

class FsrsCalibratorTest {

    @Test
    fun `calibrator contract returns calibrated parameters`() {
        val expected =
            FsrsParameters.DEFAULT

        val calibrator =
            FsrsCalibrator {
                expected
            }

        val dataset =
            ReviewHistoryDataset.empty(
                LearnerId("learner-1")
            )

        assertSame(
            expected,
            calibrator.calibrate(dataset)
        )
    }
}