package org.kafkatriage.records

import com.fasterxml.jackson.annotation.JsonIgnore
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.kafkatriage.records.Header.Companion.fromEmbeddedHeader
import org.kafkatriage.records.Header.Companion.fromKafkaHeader
import org.springframework.cloud.stream.binder.EmbeddedHeaderUtils
import org.springframework.messaging.support.GenericMessage
import java.nio.charset.StandardCharsets
import javax.persistence.*

@Entity
data class Record(
        val topic: String,
        val partition: Int,
        @Column(name = "\"offset\"") val offset: Long,
        val timestamp: Long? = null,
        val key: String?,
        val value: String?,
        @OneToMany(mappedBy = "record", cascade = [CascadeType.ALL]) val headers: List<Header> = listOf(),
        var triaged: Boolean = false,
        var replayedOffset: Long? = null,
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @JsonIgnore val id: Int? = null
) {

    companion object {
        fun <K, V> fromConsumerRecord(cr: ConsumerRecord<K, V>): Record {
            val value = cr.value() as ByteArray
            var embeddedHeaders: List<Header> = listOf()
            var embeddedPayload: ByteArray? = null
            if (EmbeddedHeaderUtils.mayHaveEmbeddedHeaders(value)) {
                try {
                    // why does it have to be private?
                    val springMessage = GenericMessage<ByteArray>(value)
                    val messageValues = EmbeddedHeaderUtils.extractHeaders(springMessage, false)
                    embeddedHeaders = messageValues.headers.map(::fromEmbeddedHeader)
                    embeddedPayload = messageValues.payload as? ByteArray
                } catch (_: Exception) {
                    // it's ok if we cannot decode them, maybe there aren't any
                }
            }
            val nativeHeaders = cr.headers().map(::fromKafkaHeader)
            val valueAsString = if (embeddedPayload != null) {
                String(embeddedPayload)
            } else {
                String(value)
            }
            val allHeaders = nativeHeaders.plus(embeddedHeaders)
            val record = Record(topic = cr.topic(), partition = cr.partition(), offset = cr.offset(),
                    key = cr.key()?.toString(),
                    value = valueAsString,
                    headers = allHeaders)
            allHeaders.forEach { it.record = record }
            return record
        }
    }
}
