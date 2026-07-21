package vn.loi.learning.infrastructure.persistence.mapper

import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.infrastructure.persistence.record.LearningItemRecord

object LearningItemRecordMapper {

    fun toRecord(
        item: LearningItem
    ): LearningItemRecord =
        LearningItemRecord(
            id = item.id.toString(),
            contentId = item.contentId.toString(),
            mode = item.mode.name,
            isEnabled = item.isEnabled
        )

    fun toDomain(
        record: LearningItemRecord
    ): LearningItem =
        mapPersistedRecord(
            recordType = "learning-item",
            recordId = record.id
        ) {
            LearningItem(
                id = LearningItemId(record.id),
                contentId = ContentId(record.contentId),
                mode = LearningMode.valueOf(record.mode),
                isEnabled = record.isEnabled
            )
        }
}
