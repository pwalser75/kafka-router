package ch.frostnova.tools.kafkarouter.util

import org.slf4j.LoggerFactory
import java.util.concurrent.Future
import kotlin.reflect.KClass


/**
 * Joins a future and returns the result as pair of result and exception.
 */
fun <T> Future<T>.join(): Pair<T?, Exception?> {
    var result: T? = null
    var exception: Exception? = null
    try {
        result = get()
    } catch (ex: Exception) {
        exception = ex
    }
    return result to exception
}

inline fun logger(kClass: KClass<*>) = LoggerFactory.getLogger(kClass.java)