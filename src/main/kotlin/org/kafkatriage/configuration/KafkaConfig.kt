package org.kafkatriage.configuration

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration
import java.util.*
import java.util.regex.Pattern

@Configuration
class KafkaConfig(
        val kafkaProperties: KafkaProperties
) {

    @Bean
    fun adminClient(): AdminClient {
        val configs = HashMap<String, Any>()
        configs[AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaProperties.brokers
        return AdminClient.create(configs)
    }

    // this is a single-user oversimplification, as KafkaConsumer is not thread safe
    @Bean
    fun kafkaConsumer(): KafkaConsumer<String, String> {
        val configs = HashMap<String, Any>()
        configs[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        configs[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaProperties.brokers
        configs[ConsumerConfig.CLIENT_ID_CONFIG] = kafkaProperties.group
        configs[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false
        configs[ConsumerConfig.GROUP_ID_CONFIG] = kafkaProperties.group
        configs[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = "org.apache.kafka.common.serialization.StringDeserializer"
        configs[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = "org.apache.kafka.common.serialization.StringDeserializer"

        val consumer = KafkaConsumer<String, String>(configs)
        consumer.subscribe(Pattern.compile("^error-.*"))
        consumer.poll(Duration.ofMillis(5000)) // necessary to trigger subscription
        return consumer
    }

    @Bean
    fun kafkaProducer(): KafkaProducer<String, String> {
        val configs = HashMap<String, Any>()
        configs[ProducerConfig.ACKS_CONFIG] = "1"
        configs[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaProperties.brokers
        configs[ProducerConfig.CLIENT_ID_CONFIG] = kafkaProperties.group
        configs[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = "org.apache.kafka.common.serialization.StringSerializer"
        configs[ProducerConfig.LINGER_MS_CONFIG] = 1
        configs[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = "org.apache.kafka.common.serialization.StringSerializer"

        return KafkaProducer(configs)
    }
}
