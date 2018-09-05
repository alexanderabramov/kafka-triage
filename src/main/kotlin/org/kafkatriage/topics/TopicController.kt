package org.kafkatriage.topics

import org.apache.kafka.clients.admin.AdminClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/topics")
class TopicController(
        val kafkaAdminClient: AdminClient
) {

    @GetMapping("/")
    fun list(): List<Topic> {
        val topics = kafkaAdminClient.listTopics().listings().get()
        return topics.map { tl -> Topic(tl.name()) }
    }
}
