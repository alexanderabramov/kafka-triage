package org.kafkatriage.records

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test
import kotlin.test.assertEquals

class RecordTest {

    @Test
    @DisplayName("deserialize null in key and value")
    fun nullValue() {
        val kafkaRecord = ConsumerRecord<String, ByteArray>("error-topic-group", 0, 0, null, null)

        val record = Record.fromConsumerRecord(kafkaRecord)

        assertEquals(null, record.key)
        assertEquals(null, record.value)
    }
}
