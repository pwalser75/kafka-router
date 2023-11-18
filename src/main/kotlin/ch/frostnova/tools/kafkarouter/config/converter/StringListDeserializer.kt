package ch.frostnova.tools.kafkarouter.config.converter

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer

class StringListDeserializer : StdDeserializer<List<String>>(List::class.java) {
    override fun deserialize(p: JsonParser, context: DeserializationContext?): List<String> {
        if (p.currentToken() == JsonToken.START_ARRAY) {
            val result = mutableListOf<String>()
            while (p.nextToken() != JsonToken.END_ARRAY) {
                result.add(p.valueAsString)
            }
            return result
        }
        if (p.currentToken() == JsonToken.VALUE_STRING) {
            return p.valueAsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
        if (p.currentToken() == JsonToken.VALUE_NUMBER_INT) {
            return listOf(p.valueAsString)
        }
        if (p.currentToken() == JsonToken.VALUE_NUMBER_FLOAT) {
            return listOf(p.valueAsString)
        }
        throw IllegalArgumentException("cannot deserialize string list from value ${p.currentToken()}")
    }
}
