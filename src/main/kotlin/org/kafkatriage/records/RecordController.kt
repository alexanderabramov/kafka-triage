package org.kafkatriage.records

import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import kotlin.text.Charsets.UTF_8

@RestController
class RecordController(
        val kafkaConsumer: KafkaConsumer<String, String>,
        val kafkaProducer: KafkaProducer<String, String>
) {
    @GetMapping("/topics/{topic}/records")
    fun list(@PathVariable topic: String): List<Record> {
        val partitions = kafkaConsumer.assignment().filter { it.topic() == topic }
        for (partition in partitions) {
            kafkaConsumer.seek(partition, kafkaConsumer.committed(partition).offset())
        }
        val records = kafkaConsumer.poll(Duration.ofMillis(1000)).filter { it.topic() == topic }
        return records.map { r -> Record(r.partition(), r.offset(), r.key(), r.value(), r.headers().map { h -> Pair<String, String>(h.key(), h.value().toString(UTF_8)) }) }
    }

    /**
     * discard records up to and including the specified offset
     */
    @PostMapping("/topics/{topic}/records/{partition}/{offset}/discard")
    fun discard(@PathVariable topic: String, @PathVariable partition: Int, @PathVariable offset: Long): Boolean {
        val topicPartition = kafkaConsumer.assignment().firstOrNull { it.topic() == topic && it.partition() == partition }
        if (topicPartition == null || kafkaConsumer.committed(topicPartition).offset() > offset) {
            return false
        }
        kafkaConsumer.commitSync(mapOf(Pair(topicPartition, OffsetAndMetadata(offset + 1))))
        return true
    }

    /**
     * move records up to and including specified offset to the corresponding retry topic
     */
    @PostMapping("/topics/{topic}/records/{partition}/{offset}/retry")
    fun retry(@PathVariable topic: String, @PathVariable partition: Int, @PathVariable offset: Long): Boolean {
        val topicPartition = kafkaConsumer.assignment().firstOrNull { it.topic() == topic && it.partition() == partition }
        if (topicPartition == null || kafkaConsumer.committed(topicPartition).offset() > offset) {
            return false
        }

        kafkaConsumer.seek(topicPartition, kafkaConsumer.committed(topicPartition).offset())
        // poll might return less records if you are asking for too many. In the typical case should be ok.
        val records = kafkaConsumer.poll(Duration.ofMillis(1000)).filter { it.offset() <= offset }
        if (records.isEmpty()) {
            throw Exception("could not get any records up to offset $offset")
        }
        val lastOffset = records.maxBy { it.offset() }!!.offset()

        val retryTopic = topic.replace("error-", "retry-")
        records.forEach {
            kafkaProducer.send(ProducerRecord(retryTopic, 0, it.timestamp(), it.key(), it.value(), it.headers()))
        }

        kafkaConsumer.commitSync(mapOf(Pair(topicPartition, OffsetAndMetadata(lastOffset + 1))))

        return true
    }
}
