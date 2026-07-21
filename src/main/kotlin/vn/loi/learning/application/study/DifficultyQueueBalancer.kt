package vn.loi.learning.application.study

import vn.loi.learning.domain.study.selection.model.SelectionCandidate

/**
 * Giảm việc các candidate thuộc cùng nhóm độ khó xuất hiện liên tiếp.
 *
 * Thuật toán:
 *
 * 1. Ưu tiên candidate đầu tiên còn lại.
 * 2. Nếu candidate đó khác nhóm với candidate trước thì chọn nó.
 * 3. Nếu cùng nhóm, chọn candidate xuất hiện sớm nhất thuộc nhóm khác.
 * 4. Nếu không còn nhóm khác, giữ candidate đầu tiên.
 *
 * Candidate được phân phối vào ba queue EASY, MEDIUM và HARD.
 * Mỗi vòng chỉ cần kiểm tra phần tử đầu của tối đa ba queue.
 *
 * Độ phức tạp:
 *
 * - thời gian: O(n);
 * - bộ nhớ phụ: O(n).
 *
 * Thuật toán bảo toàn toàn bộ candidate và deterministic.
 */
class DifficultyQueueBalancer :
    QueueBalancer {

    override fun balance(
        orderedCandidates:
        List<SelectionCandidate>
    ): List<SelectionCandidate> {
        if (orderedCandidates.size < 2) {
            return orderedCandidates.toList()
        }

        val queues =
            createQueues(
                orderedCandidates
            )

        val balanced =
            ArrayList<SelectionCandidate>(
                orderedCandidates.size
            )

        var previousBand:
                DifficultyBand? = null

        while (
            balanced.size <
            orderedCandidates.size
        ) {
            val selectedQueue =
                selectQueue(
                    queues =
                        queues,
                    previousBand =
                        previousBand
                )

            val selected =
                selectedQueue.removeFirst()

            balanced.add(
                selected.candidate
            )

            previousBand =
                selected.band
        }

        return balanced
    }

    private fun createQueues(
        orderedCandidates:
        List<SelectionCandidate>
    ): Map<
            DifficultyBand,
            ArrayDeque<IndexedCandidate>
            > {
        val queues =
            DifficultyBand.entries
                .associateWith {
                    ArrayDeque<IndexedCandidate>()
                }

        orderedCandidates.forEachIndexed {
                index,
                candidate ->

            val band =
                candidate.difficultyBand()

            queues
                .getValue(
                    band
                )
                .addLast(
                    IndexedCandidate(
                        originalIndex =
                            index,
                        candidate =
                            candidate,
                        band =
                            band
                    )
                )
        }

        return queues
    }

    private fun selectQueue(
        queues:
        Map<
                DifficultyBand,
                ArrayDeque<IndexedCandidate>
                >,
        previousBand:
        DifficultyBand?
    ): ArrayDeque<IndexedCandidate> {
        val firstQueue =
            checkNotNull(
                earliestNonEmptyQueue(
                    queues =
                        queues,
                    excludedBand =
                        null
                )
            ) {
                "At least one difficulty queue must contain a candidate."
            }

        val firstCandidate =
            firstQueue.first()

        if (
            previousBand == null ||
            firstCandidate.band !=
            previousBand
        ) {
            return firstQueue
        }

        return earliestNonEmptyQueue(
            queues =
                queues,
            excludedBand =
                previousBand
        ) ?: firstQueue
    }

    private fun earliestNonEmptyQueue(
        queues:
        Map<
                DifficultyBand,
                ArrayDeque<IndexedCandidate>
                >,
        excludedBand:
        DifficultyBand?
    ): ArrayDeque<IndexedCandidate>? {
        var earliestQueue:
                ArrayDeque<IndexedCandidate>? =
            null

        var earliestIndex =
            Int.MAX_VALUE

        DifficultyBand.entries.forEach { band ->
            if (band == excludedBand) {
                return@forEach
            }

            val queue =
                queues.getValue(
                    band
                )

            if (queue.isEmpty()) {
                return@forEach
            }

            val candidateIndex =
                queue.first()
                    .originalIndex

            if (
                candidateIndex <
                earliestIndex
            ) {
                earliestIndex =
                    candidateIndex

                earliestQueue =
                    queue
            }
        }

        return earliestQueue
    }

    private fun SelectionCandidate
            .difficultyBand(): DifficultyBand =
        DifficultyBand.from(
            memoryState
                .difficultyValue
                .value
        )

    private data class IndexedCandidate(
        val originalIndex: Int,
        val candidate: SelectionCandidate,
        val band: DifficultyBand
    )
}