package org.kafkatriage

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
import org.assertj.core.api.Assertions.assertThat
import org.kafkatriage.records.RecordController
import org.kafkatriage.records.RecordRepository
import org.kafkatriage.topics.TopicController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import java.lang.Thread.sleep
import kotlin.test.Test

class IntegrationTest @Autowired constructor(
        private val kafkaTemplate: KafkaTemplate<String, String>,
        private val recordRepository: RecordRepository,
        private val kafkaAdminClient: AdminClient,
        private val topicController: TopicController,
        private val recordController: RecordController
) : FullContextTest() {

    @Test
    fun `kafka record is read into db and offset committed`() {
        val producerRecord = ProducerRecord("error-test", 0, "key1", "value1")
        kafkaTemplate.send(producerRecord).get()

        sleep(1000)

        val allRecordsInDb = recordRepository.findAll()
        assertThat(allRecordsInDb).hasOnlyOneElementSatisfying { it.topic == "error-test" && it.partition == 0 && it.offset == 0L && it.key == "key1" && it.value == "value1" }

        val offsets = kafkaAdminClient.listConsumerGroupOffsets("triage").partitionsToOffsetAndMetadata().get()
        val offset = offsets[TopicPartition("error-test", 0)]!!.offset()
        assertThat(offset).isEqualTo(1)

        val topics = topicController.list()
        assertThat(topics).hasOnlyOneElementSatisfying { it.name == "error-test" && it.lag == 1L }

        val records = recordController.list("error-test")
        assertThat(records).hasOnlyOneElementSatisfying { it.topic == "error-test" && it.partition == 0 && it.offset == 0L && it.key == "key1" && it.value == "value1" }
    }
}
