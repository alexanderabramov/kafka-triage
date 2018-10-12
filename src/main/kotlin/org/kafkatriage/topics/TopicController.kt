package org.kafkatriage.topics

import org.apache.kafka.clients.consumer.KafkaConsumer
import org.kafkatriage.committedOrBeginning
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/topics")
class TopicController(
        val kafkaConsumer: KafkaConsumer<String, String>
) {

    @GetMapping("/")
    fun list(): List<Topic> {
        val assignedPartitions = kafkaConsumer.assignment()
        val endOffsets = kafkaConsumer.endOffsets(assignedPartitions)
        return assignedPartitions.map {
            Topic(it.topic(), (endOffsets[it] ?: 0) - kafkaConsumer.committedOrBeginning(it))
        }
    }

}

