package org.kafkatriage.metrics

import com.nhaarman.mockito_kotlin.whenever
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.extension.ExtendWith
import org.kafkatriage.records.RecordRepository
import org.kafkatriage.topics.TopicPartition
import org.mockito.Mock
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.Clock
import kotlin.test.Test
import kotlin.test.assertEquals


private const val TOPIC = "error-test"
private const val PARTITION = 0

@ExtendWith(SpringExtension::class)
class KafkaTriageMetricsTest {

    @Mock
    lateinit var recordRepositoryMock: RecordRepository

    @Test
    fun `per partition lag metric is registered in micrometer with at least one message in repository`() {

        val partitions = listOf(TopicPartition(TOPIC, PARTITION, 2))
        whenever(recordRepositoryMock.listPartitions()).thenReturn(partitions)

        val registry = SimpleMeterRegistry()
        val kafkaTriageMetrics = KafkaTriageMetrics(recordRepositoryMock, Clock.systemUTC())
        kafkaTriageMetrics.bindTo(registry)

        val gauge = registry.get("kafka.triage.lag").tag("topic", TOPIC).tag("partition", PARTITION.toString()).gauge()
        assertEquals(2.0, gauge.value())
    }
}
