package org.kafkatriage.records

data class ReplayResult(
        val replayed: List<ReplayedRecord>,
        val errors: List<RecordWithError> = listOf()
) {
    data class ReplayedRecord(
            val originalOffset: Long,
            val replayedOffset: Long
    )

    data class RecordWithError(
            val originalOffset: Long,
            val exception: Exception
    )
}
