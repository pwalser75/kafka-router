package ch.frostnova.tools.kafkarouter

import ch.frostnova.tools.kafkarouter.util.BackoffStrategy
import ch.frostnova.tools.kafkarouter.util.join
import ch.frostnova.tools.kafkarouter.util.logger
import java.util.concurrent.Executors.newFixedThreadPool

fun main(args: Array<String>) {
    showBanner()
    KafkaRouterMain().run(args)
}

private fun showBanner() {
    val bannerResource = KafkaRouterMain::class.java.getResource("/banner.txt")
    bannerResource?.let { println(it.readText()) }
}

class KafkaRouterMain {

    private val logger = logger(KafkaRouterMain::class)

    fun run(args: Array<String>): Int {
        if (args.isNotEmpty()) {
            System.setProperty(configPathEnvVariable, args[0])
        }
        try {
            start()
        } catch (ex: IllegalArgumentException) {
            logger.error(ex.message)
            return 2
        } catch (ex: Exception) {
            logger.error("${ex.javaClass.simpleName}: ${ex.message}", ex)
            return 2
        }
        return 0
    }

    private fun start() {
        val kafkaRouterConfig = ResourceLoader.loadConfig()
        val consumerGroup = kafkaRouterConfig.consumerGroup ?: "kafka-router".also {
            logger.info("using default consumer group '$it'")
        }
        val backoffStrategy = BackoffStrategy(Int.MAX_VALUE, kafkaRouterConfig.backoffStrategy.backoffTimeSeconds)

        if (kafkaRouterConfig.routes.isEmpty()) {
            logger.warn("No routes configured, idle.")
        } else {
            val kafkaClientFactory = KafkaClientFactory(kafkaRouterConfig.kafka, consumerGroup)

            logger.info("Configuring routes:")
            val routers = kafkaRouterConfig.routes.mapIndexed { idx, route ->
                logger.info("- {}", route)
                KafkaRouter("Route #${idx + 1}", kafkaClientFactory, backoffStrategy, route)
            }
            logger.info("Starting routes:")

            // one thread per route
            val executorService = newFixedThreadPool(routers.size)
            val backgroundJobs = routers.map { it.start(executorService) }

            logger.info("Startup complete, ready to route messages...")

            backgroundJobs.forEach { it.join() }
        }
    }
}
