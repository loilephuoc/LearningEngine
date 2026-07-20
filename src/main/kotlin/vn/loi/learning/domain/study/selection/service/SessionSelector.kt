package vn.loi.learning.domain.study.selection.service

import vn.loi.learning.domain.study.selection.model.SessionSelectionRequest
import vn.loi.learning.domain.study.selection.model.SessionSelectionResult
import vn.loi.learning.domain.study.selection.pipeline.SelectionPipeline

/**
 * Facade của Session Selection Engine.
 *
 * SessionSelector không chứa rule selection.
 * Toàn bộ pipeline được điều phối bởi SelectionPipeline.
 */
class SessionSelector(
    private val pipeline: SelectionPipeline = SelectionPipeline()
) {

    fun select(
        request: SessionSelectionRequest
    ): SessionSelectionResult =
        pipeline.execute(request)
}