package org.kafkatriage

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.logging.log4j.LogManager
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.exception.LockAcquisitionException
import org.kafkatriage.records.Record
import org.kafkatriage.records.RecordController
import org.kafkatriage.records.RecordRepository
import org.kafkatriage.records.ReplayResult
import org.kafkatriage.topics.TopicController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import java.lang.Thread.sleep
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertTrue


class IntegrationTest @Autowired constructor(
        private val kafkaTemplate: KafkaTemplate<String, ByteArray>,
        private val recordRepository: RecordRepository,
        private val kafkaAdminClient: AdminClient,
        private val topicController: TopicController,
        private val recordController: RecordController,
        private val mockMvc: MockMvc,
        private val objectMapper: ObjectMapper
) : FullContextTest() {

    private val logger = LogManager.getLogger()

    @Test
    fun `kafka record is read into db and offset committed`() {
        val value = "value1"
        val valueAsByteArray = value.toByteArray()
        val producerRecord = ProducerRecord("error-test", 0, "key1", valueAsByteArray)
        kafkaTemplate.send(producerRecord).get()

        sleep(1000)

        val allRecordsInDb = recordRepository.findAll()
        assertThat(allRecordsInDb).hasOnlyOneElementSatisfying { it.topic == "error-test" && it.partition == 0 && it.offset == 0L && it.key == "key1" && it.value == value }

        val offsets = kafkaAdminClient.listConsumerGroupOffsets("triage").partitionsToOffsetAndMetadata().get()
        val offset = offsets[TopicPartition("error-test", 0)]!!.offset()
        assertThat(offset).isEqualTo(1)

        val topics = topicController.list()
        assertThat(topics).hasOnlyOneElementSatisfying { it.name == "error-test" && it.lag == 1L }

        val records = recordController.list("error-test")
        assertThat(records).hasOnlyOneElementSatisfying { it.topic == "error-test" && it.partition == 0 && it.offset == 0L && it.key == "key1" && it.value == value }
    }

    @Test
    @Ignore("embedded H2 does not support serializable transactions, but PostgreSQL does")
    fun `concurrent replay does not duplicate`() {

        val testRecord = Record(topic = "error-test", partition = 0, offset = 0, key = "", value = "")
        recordRepository.save(testRecord)

        val numThreads = 10
        val maxTimeoutSeconds = 2L
        val threadPool = Executors.newFixedThreadPool(numThreads)
        val allThreadsReadyLatch = CountDownLatch(numThreads)
        val allDoneLatch = CountDownLatch(numThreads)

        val results = Collections.synchronizedList(ArrayList<ReplayResult>())
        val exceptions = Collections.synchronizedList(ArrayList<Throwable>())

        try {
            for (i in 0 until numThreads) {
                threadPool.submit {
                    try {
                        allThreadsReadyLatch.countDown()
                        val resultString = mockMvc.perform(post("/topics/{topic}/records/{partition}/{offset}/replay", "error-test", 0, 0))
                                .andReturn().response.contentAsString
                        val result = objectMapper.readValue<ReplayResult>(resultString)
                        results.add(result)
                    } catch (ex: Throwable) {
                        exceptions.add(ex)
                    } finally {
                        allDoneLatch.countDown()
                    }
                }
            }

            val allDone = allDoneLatch.await(maxTimeoutSeconds, TimeUnit.SECONDS)

            val replayed = results.stream().flatMap { it.replayed.stream() }.count()
            val lockExceptions = exceptions.stream().filter { it.cause?.cause is LockAcquisitionException }.toArray()
            val unexpectedExceptions = exceptions.stream().filter { it.cause?.cause !is LockAcquisitionException }.toArray()
            val successful = replayed == 1L && lockExceptions.size == 9
            unexpectedExceptions.forEach { ex -> logger.error("", ex) }

            assertTrue(allDone && successful && unexpectedExceptions.isEmpty(),
                    String.format("threads completed: %s  / %s, unexpected exceptions: %s, replayed: %s",
                            numThreads - allDoneLatch.count,
                            numThreads,
                            unexpectedExceptions.size,
                            replayed))
        } finally {
            threadPool.shutdownNow()
        }
    }
}
