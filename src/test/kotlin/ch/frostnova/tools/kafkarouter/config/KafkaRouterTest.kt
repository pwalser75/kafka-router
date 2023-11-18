package ch.frostnova.tools.kafkarouter.config

import ch.frostnova.tools.kafkarouter.KafkaClientFactory
import ch.frostnova.tools.kafkarouter.KafkaRouter
import ch.frostnova.tools.kafkarouter.util.BackoffStrategy
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.PartitionInfo
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.header.internals.RecordHeaders
import org.apache.kafka.common.record.TimestampType
import org.apache.kafka.common.utils.Bytes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration
import java.time.Instant
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.regex.Pattern

@ExtendWith(MockKExtension::class)
class KafkaRouterTest {

    private val routeConfig = RouteConfig().apply {
        source = "local"
        sourceTopic = "sales-.+"
        target = "global"
        targetTopic = "all-sales"
    }
    private val backoffStrategy = BackoffStrategy(5, listOf(0.0, 0.1, 0.2, 0.5, 1.0))

    @MockK
    private lateinit var kafkaClientFactory: KafkaClientFactory

    @MockK
    private lateinit var consumer: KafkaConsumer<Bytes, Bytes>

    @MockK
    private lateinit var producer: KafkaProducer<Bytes, Bytes>

    @MockK
    private lateinit var partitionInfo: PartitionInfo

    @MockK
    private lateinit var recordMetadata: RecordMetadata


    private var capturedRecords = mutableListOf<ProducerRecord<Bytes, Bytes>>()

    @BeforeEach
    fun setup() {
        every { kafkaClientFactory.createConsumer(any()) } returns consumer
        every { kafkaClientFactory.createProducer(any()) } returns producer

        every { consumer.subscribe(any<Pattern>()) } just runs
        every { consumer.listTopics() } returns mapOf(
            "sales-one" to listOf(partitionInfo),
            "sales-two" to listOf(partitionInfo),
            "failures" to listOf(partitionInfo)
        )

        val consumerRecords = ConsumerRecords(
            mapOf(
                TopicPartition("sales-one", 1) to listOf(consumerRecord("sales-one", "Sale 1")),
                TopicPartition("sales-two", 1) to listOf(
                    consumerRecord("sales-two", "Sale 2"),
                    consumerRecord("sales-two", "Sale 3")
                )
            )
        )
        every { consumer.poll(any<Duration>()) } returns consumerRecords

        capturedRecords.clear()
        every { producer.send(capture(capturedRecords), any()) } returns CompletableFuture.completedFuture(
            recordMetadata
        )
    }

    @Test
    fun `should route messages`() {


        val kafkaRouter = KafkaRouter("test", kafkaClientFactory, backoffStrategy, routeConfig)
        val future = kafkaRouter.start(Executors.newFixedThreadPool(2))
        Thread.sleep(1000)
        future.cancel(false)

        assertThat(capturedRecords.isNotEmpty())
        assertThat(capturedRecords.map { it.topic() }).containsOnly("all-sales")
        assertThat(capturedRecords.map { String(it.value().get()) }).containsExactlyInAnyOrder(
            "Sale 1",
            "Sale 2",
            "Sale 3"
        )
    }

    private fun consumerRecord(topic: String, message: String) = ConsumerRecord<Bytes, Bytes>(
        topic, 1, 1,
        Instant.now().epochSecond,
        TimestampType.CREATE_TIME,
        0, 0,
        null,
        Bytes.wrap(message.toByteArray()),
        RecordHeaders(), Optional.empty()
    )
}