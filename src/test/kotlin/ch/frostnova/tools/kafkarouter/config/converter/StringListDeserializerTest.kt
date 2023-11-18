package ch.frostnova.tools.kafkarouter.config.converter

import ch.frostnova.tools.kafkarouter.util.ObjectMappers
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StringListDeserializerTest {

    @Test
    fun `should deserialize string list`() {
        val json = "{ \"values\": \"This is, a  , test\"}"
        val deserialized = ObjectMappers.json().readValue(json, StringListDeserializerExample::class.java)

        assertThat(deserialized.values).containsExactly("This is", "a", "test")
    }

    @Test
    fun `should deserialize string list from array`() {
        val json = "{ \"values\": [ \"This is\", \"a\"  , \"test\" ]}"
        val deserialized = ObjectMappers.json().readValue(json, StringListDeserializerExample::class.java)

        assertThat(deserialized.values).containsExactly("This is", "a", "test")
    }


    @Test
    fun `should deserialize string list from integer`() {
        val json = "{ \"values\": 987}"
        val deserialized = ObjectMappers.json().readValue(json, StringListDeserializerExample::class.java)

        assertThat(deserialized.values).containsExactly("987")
    }

    @Test
    fun `should deserialize string list from float`() {
        val json = "{ \"values\": -123.45}"
        val deserialized = ObjectMappers.json().readValue(json, StringListDeserializerExample::class.java)

        assertThat(deserialized.values).containsExactly("-123.45")
    }

    @Test
    fun `should deserialize string list from null`() {
        val json = "{ \"values\": null}"
        val deserialized = ObjectMappers.json().readValue(json, StringListDeserializerExample::class.java)
        assertThat(deserialized.values).isNull()
    }

    @Test
    fun `should not deserialize string list from object`() {
        val json = "{ \"values\": {}}}"

        Assertions.assertThatThrownBy {
            ObjectMappers.json().readValue(json, StringListDeserializerExample::class.java)
        }.isInstanceOf(
            JsonMappingException::class.java
        )
    }
}

private class StringListDeserializerExample {
    @JsonDeserialize(using = StringListDeserializer::class)
    var values: List<String>? = null
}