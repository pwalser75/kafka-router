package ch.frostnova.tools.kafkarouter.config.converter

import ch.frostnova.tools.kafkarouter.util.ObjectMappers
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class DoubleListDeserializerTest {

    @Test
    fun `should deserialize double list from comma-separated list`() {
        val json = "{ \"values\": \"-2.718, 0, 1.000, 123.45,   -500\"}"
        val deserialized = ObjectMappers.json().readValue(json, DoubleListDeserializerExample::class.java)
        assertThat(deserialized.values).containsExactly(-2.718, 0.0, 1.0, 123.45, -500.0)
    }

    @Test
    fun `should deserialize double list from array`() {
        val json = "{ \"values\": [-2.718, 0, 1.000, 123.45,   -500]}"
        val deserialized = ObjectMappers.json().readValue(json, DoubleListDeserializerExample::class.java)
        assertThat(deserialized.values).containsExactly(-2.718, 0.0, 1.0, 123.45, -500.0)
    }


    @Test
    fun `should deserialize double list from integer`() {
        val json = "{ \"values\": -500}"
        val deserialized = ObjectMappers.json().readValue(json, DoubleListDeserializerExample::class.java)
        assertThat(deserialized.values).containsExactly(-500.0)
    }

    @Test
    fun `should deserialize double list from float`() {
        val json = "{ \"values\": -2.718}"
        val deserialized = ObjectMappers.json().readValue(json, DoubleListDeserializerExample::class.java)
        assertThat(deserialized.values).containsExactly(-2.718)
    }

    @Test
    fun `should deserialize double list from null`() {
        val json = "{ \"values\": null}"
        val deserialized = ObjectMappers.json().readValue(json, DoubleListDeserializerExample::class.java)
        assertThat(deserialized.values).isNull()
    }

    @Test
    fun `should not deserialize double list from object`() {
        val json = "{ \"values\": {}}}"

        assertThatThrownBy {
            ObjectMappers.json().readValue(json, DoubleListDeserializerExample::class.java)
        }.isInstanceOf(
            JsonMappingException::class.java
        )
    }
}

private class DoubleListDeserializerExample {
    @JsonDeserialize(using = DoubleListDeserializer::class)
    var values: List<Double>? = null
}