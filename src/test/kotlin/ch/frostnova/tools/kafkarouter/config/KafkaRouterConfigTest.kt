package ch.frostnova.tools.kafkarouter.config

import ch.frostnova.tools.kafkarouter.util.ObjectMappers
import ch.frostnova.tools.kafkarouter.util.validator
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class KafkaRouterConfigTest {

    @Test
    fun `should validate config OK`() {

        val resource = "/test-config.yaml"

        val kafkaRouterConfig = ObjectMappers.forResource(resource)
            .readValue(javaClass.getResource(resource), KafkaRouterConfig::class.java)

        kafkaRouterConfig.validate()
    }

    @Test
    fun `should validate config NOT OK`() {

        val resource = "/test-config-invalid.yaml"

        val kafkaRouterConfig = ObjectMappers.forResource(resource)
            .readValue(javaClass.getResource(resource), KafkaRouterConfig::class.java)

        validator.validate(kafkaRouterConfig)

        val expectedValidationErrors = setOf(
            "kafka[internal].bootstrapServers: ([]) must not be empty",
            "routes[Example 123].source: (null) must not be blank",
            "routes[Example 123].sourceTopic: (null) must not be empty",
            "routes[example-123].sourceTopic: ((123) must be a valid regular expression",
            "routes[example-123].target: (null) must not be blank",
            "backoff-strategy.backoff-time-seconds: -1.0 must not be negative",
            "unconfigured Kafka sources: 'nope'",
            "unconfigured Kafka targets: 'yodel'"
        )

        assertThatThrownBy { kafkaRouterConfig.validate() }
            .isInstanceOfSatisfying(ValidationException::class.java) {
                assertThat(it.message).isNotEmpty
                assertThat(it.message?.split("\n"))
                    .containsExactlyInAnyOrderElementsOf(expectedValidationErrors)
            }
    }
}