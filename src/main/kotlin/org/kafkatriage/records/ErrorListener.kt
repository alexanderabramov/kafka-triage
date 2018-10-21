package org.kafkatriage.records

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.kafkatriage.records.Record.Companion.fromConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class ErrorListener(
        private val recordRepository: RecordRepository
) {
    @KafkaListener(topicPattern = "^error-.*")
    fun <K, V> listen(cr: ConsumerRecord<K, V>) {
        val record = fromConsumerRecord(cr)
        recordRepository.save(record)
    }
}
