package ch.frostnova.tools.kafkarouter.config.util

import ch.frostnova.tools.kafkarouter.util.BackoffStrategy
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class BackoffStrategyTest {

    @Test
    fun `should return immediately on success`() {
        val backoffStrategy = BackoffStrategy()
        val start = System.nanoTime()

        backoffStrategy.runRetryable { Math.random() }
        val durationNs = System.nanoTime() - start

        assertThat(durationNs).isLessThan(1000000000L)
    }

    @Test
    fun `should eventually throw exception`() {
        val backoffStrategy = BackoffStrategy(3, listOf(0.1, 0.2, 0.3, 5.0, 10.0))
        val start = System.nanoTime()

        val sequence = AtomicInteger()
        assertThatThrownBy {
            backoffStrategy.runRetryable {
                sequence.incrementAndGet()
                println(1 / 0)
            }
        }.isInstanceOf(ArithmeticException::class.java)
        val durationNs = System.nanoTime() - start

        assertThat(sequence.get()).isEqualTo(3)
        assertThat(durationNs).isLessThan(5000000000L)
    }

    @Test
    fun `should retry until successful`() {
        val backoffStrategy = BackoffStrategy(100, listOf(0.1, 0.2, 0.3))
        val start = System.nanoTime()

        val sequence = AtomicInteger()
        backoffStrategy.runRetryable {
            sequence.incrementAndGet()
            if (sequence.get() < 5) throw IOException()
        }
        val durationNs = System.nanoTime() - start

        assertThat(sequence.get()).isEqualTo(5)
        assertThat(durationNs).isLessThan(5000000000L)
    }
}