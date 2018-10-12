package org.kafkatriage

import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition

fun <K, V> KafkaConsumer<K, V>.committedOrBeginning(partition: TopicPartition?): Long {
    val committed = this.committed(partition)
    return committed?.offset() ?: (this.beginningOffsets(listOf(partition))[partition] ?: 0)
}

fun <K, V> KafkaConsumer<K, V>.seekToCommittedOrBeginning(partition: TopicPartition?) {
    val committed = this.committed(partition)
    if (committed != null) {
        this.seek(partition, committed.offset())
    } else {
        this.seekToBeginning(listOf(partition))
    }
}
