package org.kafkatriage.records

import com.fasterxml.jackson.annotation.JsonIgnore
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.kafkatriage.records.Header.Companion.fromKafkaHeader
import javax.persistence.*

@Entity
data class Record(
        val topic: String,
        val partition: Int,
        @Column(name = "\"offset\"") val offset: Long,
        val timestamp: Long? = null,
        val key: String?,
        val value: String?,
        @OneToMany(mappedBy = "record") val headers: List<Header> = listOf(),
        var triaged: Boolean = false,
        var replayedOffset: Long? = null,
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @JsonIgnore val id: Int? = null
) {

    companion object {
        fun <K, V> fromConsumerRecord(cr: ConsumerRecord<K, V>): Record {
            return Record(topic = cr.topic(), partition = cr.partition(), offset = cr.offset(), key = cr.key().toString(), value = cr.value().toString(),
                    headers = cr.headers().map(::fromKafkaHeader))
        }
    }
}
