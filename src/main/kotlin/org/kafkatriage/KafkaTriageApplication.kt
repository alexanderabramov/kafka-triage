package org.kafkatriage

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication


@SpringBootApplication
class KafkaTriageApplication

fun main(args: Array<String>) {
    runApplication<KafkaTriageApplication>(*args)
}
