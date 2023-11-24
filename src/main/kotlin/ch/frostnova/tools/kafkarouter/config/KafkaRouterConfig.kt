package ch.frostnova.tools.kafkarouter.config

import ch.frostnova.tools.kafkarouter.config.converter.DoubleListDeserializer
import ch.frostnova.tools.kafkarouter.config.converter.StringListDeserializer
import ch.frostnova.tools.kafkarouter.config.validator.RegularExpression
import ch.frostnova.tools.kafkarouter.util.validate
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import jakarta.validation.Valid
import jakarta.validation.ValidationException
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern

class KafkaRouterConfig {

    @Valid
    var backoffStrategy: BackoffStrategyConfig = BackoffStrategyConfig()

    @Valid
    var kafka: Map<@Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*\$") String, KafkaConfig> = emptyMap()

    @Valid
    var routes: Map<@Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*\$") String, RouteConfig> = emptyMap()

    fun validate() {
        var errors = mutableListOf<String>()

        // bean validation first
        try {
            validate(this)
        } catch (ex: ValidationException) {
            ex.message?.split("\n")?.let { errors.addAll(it) }
        }

        // validate backoff times
        backoffStrategy.backoffTimeSeconds.forEach { seconds ->
            if (seconds < 0) errors.add("backoff-strategy.backoff-time-seconds: $seconds must not be negative")
        }

        // validate that the source and target of each route is available
        validateKafkaConfigured(routes.values.map { it.source }, "sources")?.let { errors.add(it) }
        validateKafkaConfigured(routes.values.map { it.target }, "targets")?.let { errors.add(it) }

        if (errors.isNotEmpty()) throw ValidationException(errors.joinToString("\n"))
    }

    private fun validateKafkaConfigured(keys: List<String?>, typesName: String): String? {
        keys.filterNotNull().filter { !kafka.containsKey(it) }.also { key ->
            return if (key.isNotEmpty()) "unconfigured Kafka $typesName: ${key.joinToString(", ") { "'$it'" }}"
            else null
        }
    }
}

class BackoffStrategyConfig {

    @NotEmpty
    @JsonDeserialize(using = DoubleListDeserializer::class)
    var backoffTimeSeconds: @Valid List<@Min(0) Double> = listOf(0.5, 1.0, 2.0, 3.0, 5.0, 10.0)
}

class KafkaConfig {

    @NotEmpty
    @JsonDeserialize(using = StringListDeserializer::class)
    var bootstrapServers: List<String> = emptyList()

    var truststorePath: String? = null

    var truststorePassword: String? = null

    var keystorePath: String? = null

    var keystorePassword: String? = null

    var properties: Map<String, String>? = emptyMap()
}

class RouteConfig {

    @NotBlank
    var source: String? = null

    @NotEmpty
    @RegularExpression
    var sourceTopic: String? = null

    @NotBlank
    var target: String? = null

    var targetTopic: String? = null

    override fun toString() = "$source ($sourceTopic) -> $target (${targetTopic ?: "default"})"
}
