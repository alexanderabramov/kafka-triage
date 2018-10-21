package org.kafkatriage.topics

import org.kafkatriage.records.RecordRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/topics")
class TopicController(
        val recordRepository: RecordRepository
) {

    @GetMapping("/")
    fun list(): List<Topic> {
        val topics = recordRepository.listTopics()
        return topics.map {
            Topic(it.name, it.lag)
        }
    }
}
