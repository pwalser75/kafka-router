package ch.frostnova.tools.kafkarouter.util

import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY
import com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT
import com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT
import com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
import com.fasterxml.jackson.databind.SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector
import com.fasterxml.jackson.databind.util.StdDateFormat
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import java.util.concurrent.ConcurrentHashMap

/**
 * Lazy-created object mappers for various serialization formats.
 *
 * @author pwalser
 * @since 2021-05-09
 */
object ObjectMappers {

    private val objectMappers = ConcurrentHashMap<Type, ObjectMapper>()

    fun forResource(name: String): ObjectMapper {
        val nameLowerCase = name.lowercase()
        if (nameLowerCase.endsWith(".json")) return json()
        if (nameLowerCase.endsWith(".yaml") || nameLowerCase.endsWith(".yml")) return yaml()
        throw IllegalArgumentException("$name: unsupported file format, only json and yaml|yml are supported")
    }

    fun json(): ObjectMapper {
        return objectMappers.computeIfAbsent(Type.JSON) {
            configure(ObjectMapper())
        }
    }

    fun yaml(): ObjectMapper {
        return objectMappers.computeIfAbsent(Type.YAML) {
            configure(
                ObjectMapper(
                    YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                )
            ).apply {
                propertyNamingStrategy = PropertyNamingStrategies.KEBAB_CASE
            }
        }
    }

    private fun configure(mapper: ObjectMapper): ObjectMapper {
        return mapper
            .setAnnotationIntrospector(JacksonAnnotationIntrospector())
            .registerModule(JavaTimeModule())
            .setDateFormat(StdDateFormat())
            .enable(INDENT_OUTPUT)
            .enable(ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .enable(ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
            .disable(WRITE_DATES_AS_TIMESTAMPS)
            .disable(WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
            .enable(FAIL_ON_UNKNOWN_PROPERTIES)
            .setSerializationInclusion(NON_EMPTY)
    }

    internal enum class Type {
        JSON, //  JavaScript Object Notation
        YAML //  Yet Another Markup Language
    }
}
