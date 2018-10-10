package org.kafkatriage.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.core.lang.NonNullApi
import io.micrometer.core.lang.NonNullFields
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.logging.log4j.LogManager
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.stream.Collectors

/**
 * Binds consumer group offset for each partition assigned on startup.
 *
 * TODO: add partitions assigned later
 *
 */
@NonNullApi
@NonNullFields
@Component
@ConditionalOnProperty(value = ["management.metrics.kafka.consumer.enabled"], matchIfMissing = true)
class KafkaTriageMetrics(
        val kafkaConsumer: KafkaConsumer<String, String>
) : MeterBinder {

    private val logger = LogManager.getLogger()

    override fun bindTo(registry: MeterRegistry) {
        val assignedPartitions = kafkaConsumer.assignment()
        for (topicPartition in assignedPartitions) {
            val name = "kafka.triage.lag"
            val tags = Tags.of(TOPIC, topicPartition.topic()).and(PARTITION, topicPartition.partition().toString())
            if (logger.isDebugEnabled) {
                logger.debug("Registering gauge " + name + " " + tags.stream().map { it.toString() }.collect(Collectors.joining(",")))
            }
            registry.gauge(name, tags, kafkaConsumer) { kafkaConsumer.endOffsets(listOf(topicPartition))[topicPartition]!! - kafkaConsumer.committed(topicPartition).offset().toDouble() }
        }
    }

    companion object {
        private const val TOPIC = "topic"
        private const val PARTITION = "partition"
    }
}
