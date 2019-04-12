package org.kafkatriage

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.logging.log4j.LogManager
import org.hibernate.exception.LockAcquisitionException
import org.kafkatriage.records.Record
import org.kafkatriage.records.RecordRepository
import org.kafkatriage.records.ReplayResult
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertTrue

@DirtiesContext
class NonTransactionalTests @Autowired constructor(
        private val recordRepository: RecordRepository,
        private val mockMvc: MockMvc,
        private val objectMapper: ObjectMapper
) : FullContextTest() {

    private val logger = LogManager.getLogger()

    @Test
    fun `concurrent replay does not duplicate`() {

        val testRecord = Record(topic = "error-test", partition = 0, offset = 0, key = "", value = "")
        recordRepository.save(testRecord)
        recordRepository.flush()

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
