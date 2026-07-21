package vn.loi.learning.application.study

import vn.loi.learning.domain.study.selection.model.SelectionCandidate

/**
 * Chiến lược xác định thứ tự các candidate đã đủ điều kiện
 * tham gia StudyQueue.
 *
 * Strategy chỉ chịu trách nhiệm ordering.
 *
 * Strategy không:
 * - đọc repository;
 * - lọc eligibility;
 * - áp dụng SessionPolicy;
 * - tạo StudyQueuePlan;
 * - thay đổi SelectionCandidate;
 * - ghi dữ liệu vào persistence.
 */
fun interface StudyQueueStrategy {

    fun order(
        candidates: List<SelectionCandidate>
    ): List<SelectionCandidate>
}