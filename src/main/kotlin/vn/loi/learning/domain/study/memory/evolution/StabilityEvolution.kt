package vn.loi.learning.domain.study.memory.evolution

import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.Stability

/**
 * Quy luật tiến hóa Stability sau một lần review.
 *
 * Contract này chỉ mô hình hóa:
 *
 * Stability hiện tại + ReviewRating
 *                 ↓
 * Stability tiếp theo
 *
 * Nó không biết về:
 * - MemoryState;
 * - Scheduler;
 * - persistence;
 * - repository;
 * - thời điểm review;
 * - due time hoặc interval.
 *
 * Các thuật toán Simple, FSRS hoặc adaptive có thể
 * cung cấp implementation riêng.
 */
interface StabilityEvolution {

    fun evolve(
        current: Stability,
        rating: ReviewRating
    ): Stability
}