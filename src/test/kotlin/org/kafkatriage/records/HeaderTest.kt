package org.kafkatriage.records

import org.junit.jupiter.api.DisplayName
import kotlin.test.Test
import kotlin.test.assertEquals

class HeaderTest {

    @Test
    @DisplayName("deserialize int in header value")
    fun intValue() {
        val kafkaHeader = org.apache.kafka.common.header.internals.RecordHeader("original-partition", byteArrayOf(0, 0, 0, 1))

        val header = Header.fromKafkaHeader(kafkaHeader)

        assertEquals("1", header.value)
    }

    @Test
    @DisplayName("deserialize long in header value")
    fun longValue() {
        val kafkaHeader = org.apache.kafka.common.header.internals.RecordHeader("original-offset", byteArrayOf(0, 0, 0, 0, 0, 0, 0, 1))

        val header = Header.fromKafkaHeader(kafkaHeader)

        assertEquals("1", header.value)
    }

    @Test
    @DisplayName("deserialize UTF-8 string in header value")
    fun stringValue() {
        val kafkaHeader = org.apache.kafka.common.header.internals.RecordHeader("original-offset", "0".toByteArray())

        val header = Header.fromKafkaHeader(kafkaHeader)

        assertEquals("0", header.value)
    }
}
