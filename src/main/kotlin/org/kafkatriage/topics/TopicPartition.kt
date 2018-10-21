package org.kafkatriage.topics

data class TopicPartition(
        val topic: String,
        val partition: Int,
        val errors: Long
)
