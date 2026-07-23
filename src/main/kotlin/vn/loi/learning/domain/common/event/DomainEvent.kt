package vn.loi.learning.domain.common.event

import java.time.Instant

/**
 * Interface đánh dấu cho tất cả Domain Events trong hệ thống.
 */
interface DomainEvent {
    val occurredAt: Instant
}
