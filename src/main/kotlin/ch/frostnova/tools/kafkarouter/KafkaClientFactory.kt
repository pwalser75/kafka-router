package ch.frostnova.tools.kafkarouter

import ch.frostnova.tools.kafkarouter.config.KafkaConfig
import ch.frostnova.tools.kafkarouter.util.logger
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.BytesDeserializer
import org.apache.kafka.common.serialization.BytesSerializer
import org.apache.kafka.common.utils.Bytes
import java.util.Properties

/**
 * The KafkaClientFactory creates Kafka clients for sources (consumers) and targets (producers).
 */
class KafkaClientFactory(
    private val kafkaConfigs: Map<String, KafkaConfig>
) {

    private val logger = logger(KafkaClientFactory::class)

    fun createConsumer(id: String, consumerGroup: String): KafkaConsumer<Bytes, Bytes> {
        val kafkaConfig = getConfig(id)

        // overridable properties
        val consumerProperties = Properties()
        consumerProperties[ConsumerConfig.GROUP_ID_CONFIG] = consumerGroup
        consumerProperties[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = BytesDeserializer::class.java
        consumerProperties[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = BytesDeserializer::class.java
        consumerProperties[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        configureAdditionalProperties(kafkaConfig, consumerProperties)

        // non-overridable properties
        consumerProperties[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaConfig.bootstrapServers.joinToString(",")
        configureTLS(kafkaConfig, consumerProperties)

        logProperties(consumerProperties)

        return KafkaConsumer<Bytes, Bytes>(consumerProperties)
    }

    fun createProducer(id: String): KafkaProducer<Bytes, Bytes> {
        val kafkaConfig = getConfig(id)

        // overridable properties
        val producerProperties = Properties()
        producerProperties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, BytesSerializer::class.java.name)
        producerProperties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, BytesSerializer::class.java.name)
        producerProperties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4")
        producerProperties.setProperty(ProducerConfig.LINGER_MS_CONFIG, "50")
        producerProperties.setProperty(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, "5242880")
        configureAdditionalProperties(kafkaConfig, producerProperties)

        // non-overridable properties
        producerProperties.setProperty(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
            kafkaConfig.bootstrapServers.joinToString(",")
        )
        configureTLS(kafkaConfig, producerProperties)

        logProperties(producerProperties)

        return KafkaProducer<Bytes, Bytes>(producerProperties)
    }

    private fun logProperties(properties: Properties) {
        if (logger.isDebugEnabled) {
            logger.debug("Kafka properties:")
            properties.forEach { key, value -> logger.debug("- {} = {}", key, value) }
        }
    }

    private fun configureTLS(kafkaConfig: KafkaConfig, kafkaProperties: Properties) {
        kafkaConfig.truststorePath?.let {
            kafkaProperties[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SSL"
            kafkaProperties[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG] =
                kafkaConfig.truststorePath?.let { ResourceLoader.getResource(it).path }
            kafkaProperties[SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG] = kafkaConfig.truststorePassword

            kafkaConfig.keystorePath?.let {
                kafkaProperties[SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG] =
                    kafkaConfig.keystorePath?.let { ResourceLoader.getResource(it).path }
                kafkaProperties[SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG] = kafkaConfig.keystorePassword
                kafkaProperties[SslConfigs.SSL_KEY_PASSWORD_CONFIG] = kafkaConfig.keystorePassword
            }
        }
    }

    private fun configureAdditionalProperties(kafkaConfig: KafkaConfig, kafkaProperties: Properties) {
        kafkaConfig.properties?.let { properties ->
            properties.forEach { kafkaProperties[it.key] = it.value }
        }
    }

    private fun getConfig(key: String) =
        kafkaConfigs[key] ?: throw NoSuchElementException("no Kafka configuration found for $key")
}