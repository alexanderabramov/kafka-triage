package org.kafkatriage.records

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.springframework.cloud.stream.binder.EmbeddedHeaderUtils
import org.springframework.cloud.stream.binder.MessageValues
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.support.GenericMessage
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.Future

@RestController
@Transactional(isolation = Isolation.SERIALIZABLE)
class RecordController(
        val recordRepository: RecordRepository,
        private val kafkaProducer: KafkaProducer<String, ByteArray>
) {
    @GetMapping("/topics/{topic}/records")
    fun list(@PathVariable topic: String): List<Record> {
        return recordRepository.findByTopicAndTriaged(topic)
    }

    /**
     * discard records up to and including the specified offset
     */
    @PostMapping("/topics/{topic}/records/{partition}/{offset}/discard")
    fun discard(@PathVariable topic: String, @PathVariable partition: Int, @PathVariable offset: Long): Boolean {
        recordRepository.markTriagedTo(topic, partition, offset)
        return true
    }

    /**
     * Move records up to and including specified offset to the corresponding retry topic.
     *
     * Potential outcomes:
     * - records produced on retry topic, DB updated with triaged=true and replayedOffset; all good
     * - for some DB records triaged=true && replayedOffset=null; need to review exceptions
     *  and verify if the records were in fact replayed
     */
    @PostMapping("/topics/{topic}/records/{partition}/{offset}/replay")
    fun replay(@PathVariable topic: String, @PathVariable partition: Int, @PathVariable offset: Long): ReplayResult {
        val retryTopic = topic.replaceFirst("error-", "retry-")
        val headersToIgnore = listOf("x-original-topic", "x-exception-message", "x-exception-stacktrace")
        val headersToIgnoreRegex = Regex("^kafka_dlt-.*")

        val recordsToReplay = recordRepository.findUnTriagedTo(topic, partition, offset)
        recordRepository.markTriagedTo(topic, partition, offset)

        val sendFutures = ArrayList<Future<RecordMetadata>>(recordsToReplay.count())
        for (i in recordsToReplay.indices) {
            val record = recordsToReplay[i]
            val headers = record.headers
                    .filterNot { headersToIgnore.contains(it.key) }
                    .filterNot { headersToIgnoreRegex.containsMatchIn(it.key) }

            val value: ByteArray?
            val nativeHeaders: List<org.apache.kafka.common.header.Header>
            if (headers.isNotEmpty() && headers.all { !it.native }) {
                // this is awkward to use
                val messageValues = MessageValues(GenericMessage(record.value?.toByteArray() ?: ByteArray(0),
                        MessageHeaders(headers.associateBy({ it.key }, { it.value }))))
                value = EmbeddedHeaderUtils.embedHeaders(messageValues, *headers.map { it.key }.toTypedArray())
                nativeHeaders = listOf()
            } else {
                value = record.value?.toByteArray()
                nativeHeaders = headers.map(Header::toKafkaHeader)
            }
            val producerRecord = ProducerRecord(retryTopic, 0, record.timestamp, record.key, value,
                    nativeHeaders)
            sendFutures.add(i, kafkaProducer.send(producerRecord))
        }

        val replayed = mutableListOf<ReplayResult.ReplayedRecord>()
        val errors = mutableListOf<ReplayResult.RecordWithError>()
        for (i in sendFutures.indices) {
            val record = recordsToReplay[i]
            try {
                val metadata = sendFutures[i].get()
                if (metadata.hasOffset()) {
                    val replayedOffset = metadata.offset()
                    recordRepository.setReplayedOffset(record.id!!, replayedOffset)
                    replayed.add(ReplayResult.ReplayedRecord(record.offset, replayedOffset))
                }

            } catch (ex: Exception) {
                errors.add(ReplayResult.RecordWithError(record.offset, ex))
            }
        }
        recordRepository.flush()
        // return 202 if there are some errors?
        return ReplayResult(replayed, errors)
    }
}
