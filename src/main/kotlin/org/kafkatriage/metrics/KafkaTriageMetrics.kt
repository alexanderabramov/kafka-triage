package org.kafkatriage.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.binder.MeterBinder
import org.apache.logging.log4j.LogManager
import org.kafkatriage.records.RecordRepository
import org.kafkatriage.topics.TopicPartition
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.stream.Collectors
import kotlin.concurrent.schedule

/**
 * Errors awaiting triage for all partitions available on startup.
 *
 * TODO: add partitions with errors posted later
 *
 */
@Component
class KafkaTriageMetrics(
        val recordRepository: RecordRepository,
        val clock: Clock
) : MeterBinder {

    private val logger = LogManager.getLogger()
    private lateinit var registry: MeterRegistry

    private var triageLags = listOf<TopicPartition>()
    private var lastUpdated: Instant = Instant.MIN
    private var updateTimer: Timer? = null

    override fun bindTo(registry: MeterRegistry) {
        this.registry = registry
        update()
    }

    fun getLag(topic: String, partition: Int): Double {
        if (updateRequired()) {
            update()
        }
        val lag = triageLags.find { it.topic == topic && it.partition == partition }?.errors
        return lag?.toDouble() ?: 0.0
    }

    private fun updateRequired() = clock.instant() >= lastUpdated + updateAtMostEvery


    @Synchronized
    private fun update() {
        if (!updateRequired()) {
            return
        }

        val allPartitions = recordRepository.listPartitions()
        val newPartitions = allPartitions.filter { p -> triageLags.find { it.topic == p.topic && it.partition == p.partition } == null }
        for (topicPartition in newPartitions) {
            val tags = Tags.of(TOPIC, topicPartition.topic).and(PARTITION, topicPartition.partition.toString())
            if (logger.isDebugEnabled) {
                logger.debug("Registering gauge " + METRIC_LAG + " " + tags.stream().map { it.toString() }.collect(Collectors.joining(",")))
            }
            registry.gauge(METRIC_LAG, tags, this) { getLag(topicPartition.topic, topicPartition.partition) }
        }

        if (allPartitions.isEmpty() && updateTimer == null) {
            updateTimer = Timer()
            updateTimer?.schedule(noMetricsPollEveryMs) { update() }

        } else if (!allPartitions.isEmpty() && updateTimer != null) {
            updateTimer?.cancel()
            updateTimer = null
        }

        triageLags = allPartitions
        lastUpdated = clock.instant()
    }

    companion object {
        private const val TOPIC = "topic"
        private const val PARTITION = "partition"
        private const val METRIC_LAG = "kafka.triage.lag"

        private const val noMetricsPollEveryMs = 15000L
        private val updateAtMostEvery = Duration.ofSeconds(5)
    }
}
