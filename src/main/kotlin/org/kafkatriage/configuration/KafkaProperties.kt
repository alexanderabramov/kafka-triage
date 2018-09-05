package org.kafkatriage.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "kafka")
class KafkaProperties {
    lateinit var brokers: String
    lateinit var group: String
}
