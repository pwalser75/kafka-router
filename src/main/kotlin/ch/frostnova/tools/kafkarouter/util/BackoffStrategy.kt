package ch.frostnova.tools.kafkarouter.util

import java.lang.Thread.sleep

class BackoffStrategy(
    val retryCount: Int = Int.MAX_VALUE,
    val backoffTimeSeconds: List<Int> = listOf(1, 2, 3, 5, 10, 30)
) {

    val logger = logger(BackoffStrategy::class)

    fun <T : Any> runRetryable(producer: () -> T): T {

        var count = 0
        var lastException: Exception? = null
        while (count < retryCount) {
            try {
                val result = producer()
                if (count > 1)
                    logger.info("retry #$count was successful")
                return result
            } catch (ex: Exception) {
                lastException = ex
                val nextRetryOffset =
                    if (count < backoffTimeSeconds.size) backoffTimeSeconds[count] else backoffTimeSeconds.lastOrNull()
                        ?: 0
                count++
                if (count < retryCount) {
                    logger.warn("caught ${ex.javaClass.simpleName}: ${ex.message} from ${ex.stackTrace[1]}, retry #$count in $nextRetryOffset seconds...")
                }
                sleep(nextRetryOffset * 1000L)
            }
        }
        throw lastException ?: IllegalStateException()
    }
}
