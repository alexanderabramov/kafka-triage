package org.kafkatriage

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.transaction.annotation.EnableTransactionManagement


@SpringBootApplication
@EnableTransactionManagement
class KafkaTriageApplication

fun main(args: Array<String>) {
    runApplication<KafkaTriageApplication>(*args)
}
