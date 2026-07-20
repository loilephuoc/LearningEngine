package vn.loi.learning.domain.study.memory.evolution

import vn.loi.learning.domain.study.memory.model.Difficulty
import vn.loi.learning.domain.study.memory.model.ReviewRating

/**
 * Quy luật tiến hóa Difficulty sau một lần review.
 *
 * Contract này chỉ mô hình hóa:
 *
 * Difficulty hiện tại + ReviewRating
 *                  ↓
 * Difficulty tiếp theo
 *
 * Nó không biết về:
 * - MemoryState;
 * - Scheduler;
 * - persistence;
 * - repository;
 * - thời điểm review;
 * - interval hoặc stability.
 *
 * Các thuật toán khác nhau có thể cung cấp implementation riêng,
 * ví dụ Simple, FSRS hoặc adaptive evolution.
 */
interface DifficultyEvolution {

    fun evolve(
        current: Difficulty,
        rating: ReviewRating
    ): Difficulty
}