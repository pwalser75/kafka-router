package ch.frostnova.tools.kafkarouter

import ch.frostnova.tools.kafkarouter.config.RouteConfig
import ch.frostnova.tools.kafkarouter.util.BackoffStrategy
import ch.frostnova.tools.kafkarouter.util.join
import ch.frostnova.tools.kafkarouter.util.logger
import org.apache.kafka.clients.producer.Callback
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.regex.Pattern


/**
 * A KafkaRouter is responsible for a single route,
 * and routes the messages from the selected source topics to the target topic.
 */
class KafkaRouter(
    private val name: String,
    kafkaClientFactory: KafkaClientFactory,
    private val backoffStrategy: BackoffStrategy,
    private val route: RouteConfig
) {
    private val logger = logger(KafkaRouter::class)

    private val source = kafkaClientFactory.createConsumer(route.source!!)
    private val target = kafkaClientFactory.createProducer(route.target!!)

    private val callback =
        Callback { recordMetadata, ex ->
            ex?.let {
                logger.error(
                    "Failed to send record to ${recordMetadata.topic()} ${ex.javaClass.simpleName}: ${ex.message}", ex
                )
                ex.printStackTrace()
            }
        }

    fun start(executorService: ExecutorService): Future<*> {

        logger.info("Joining consumer group...")
        source.groupMetadata()

        logger.info("Subscribing to initial topics: ")
        val regex = Regex(route.sourceTopic!!)
        source.listTopics().map { it.key }.filter { regex.matches(it) }.forEach { logger.info("- $it") }

        source.subscribe(Pattern.compile(route.sourceTopic!!))

        return executorService.submit {
            Thread.currentThread().name = name
            while (true) {
                source.poll(Duration.ofSeconds(1))?.let { consumerRecords ->
                    if (!consumerRecords.isEmpty) {
                        val producerRecords = consumerRecords.map { consumerRecord ->
                            val producerRecord = ProducerRecord(
                                route.targetTopic ?: consumerRecord.topic(),
                                null,
                                consumerRecord.timestamp(),
                                consumerRecord.key(),
                                consumerRecord.value(),
                                consumerRecord.headers().apply {
                                    add(
                                        RecordHeader(
                                            "X-Kafka-Router-Source",
                                            ("${route.source} " +
                                                    "| topic: ${consumerRecord.topic()} " +
                                                    "| partition: ${consumerRecord.partition()} " +
                                                    "| offset: ${consumerRecord.offset()}").toByteArray()
                                        )
                                    )
                                }
                            )
                            logger.info(
                                "routing [{}] {} ({}:{}) to [{}] {}",
                                route.source,
                                consumerRecord.topic(),
                                consumerRecord.partition(),
                                consumerRecord.offset(),
                                route.target,
                                producerRecord.topic()
                            )
                            producerRecord
                        }
                        val futures = producerRecords.map {
                            backoffStrategy.runRetryable {
                                target.send(it, callback)
                            }
                        }

                        val exceptions = futures.map { it.join() }.mapNotNull { it.second }
                        if (exceptions.isEmpty()) {
                            logger.debug("no exceptions in batch, committing read")
                            source.commitSync()
                        } else {
                            logger.warn("{} exceptions in batch -> retry", exceptions.size)
                        }
                    }
                }
            }
        }
    }
}