package org.kafkatriage.metrics

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.rule.KafkaEmbedded
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*
import java.util.concurrent.CompletableFuture


private const val TOPIC = "error-test"
private const val CONSUMER1 = "consumer-1"
private const val PRODUCER1 = "producer-1"
private const val PARTITION = 0

@ExtendWith(SpringExtension::class)
@DirtiesContext
@EmbeddedKafka(partitions = 1, topics = [TOPIC])
@Tag("integration")
class KafkaTriageMetricsTest {

    @Autowired
    private lateinit var embeddedKafka: KafkaEmbedded

    @Test
    fun `per partition lag metric is registered in micrometer when read at least one message`() {

        val (consumer, onPartitionsAssigned) = createConsumer(CONSUMER1)
        // trigger partition assignment so it's done faster
        consumer.poll(1)
        // there's something about includeMetadataInTimeout=true that makes this test hang forever with kafka 2.0
        //consumer.poll(Duration.ofMillis(1))

        val producer = createProducer(PRODUCER1)
        producer.send(ProducerRecord(TOPIC, PARTITION, null, "1"))
        val sendFuture = producer.send(ProducerRecord(TOPIC, PARTITION, null, "2"))
        sendFuture.get()
        producer.close()

        onPartitionsAssigned.get()

        val registry = SimpleMeterRegistry()
        val kafkaTriageMetrics = KafkaTriageMetrics(consumer)
        kafkaTriageMetrics.bindTo(registry)

        consumer.commitSync(mapOf(Pair(TopicPartition(TOPIC, PARTITION), OffsetAndMetadata(1))))

        val gauge = registry.get("kafka.triage.lag").tag("topic", TOPIC).tag("partition", PARTITION.toString()).gauge()
        assertEquals(1.0, gauge.value())

        consumer.close()
    }

    private fun createConsumer(name: String): Pair<KafkaConsumer<String, String>, CompletableFuture<MutableCollection<TopicPartition>>> {
        val props = Properties()
        props[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = 1
        props[ConsumerConfig.CLIENT_ID_CONFIG] = name
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = embeddedKafka.brokersAsString
        props[ConsumerConfig.GROUP_ID_CONFIG] = "MicrometerTestConsumer"
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name

        val consumer = KafkaConsumer<String, String>(props)
        val future: CompletableFuture<MutableCollection<TopicPartition>> = CompletableFuture()
        val listener: ConsumerRebalanceListener = object : ConsumerRebalanceListener {
            override fun onPartitionsRevoked(partitions: MutableCollection<TopicPartition>) {}
            override fun onPartitionsAssigned(partitions: MutableCollection<TopicPartition>) {
                future.complete(partitions)
            }
        }
        consumer.subscribe(listOf(TOPIC), listener)
        return Pair(consumer, future)
    }

    private fun createProducer(name: String): KafkaProducer<String, String> {
        val props = Properties()
        props[ProducerConfig.CLIENT_ID_CONFIG] = name
        props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = embeddedKafka.brokersAsString
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name

        return KafkaProducer(props)
    }
}
