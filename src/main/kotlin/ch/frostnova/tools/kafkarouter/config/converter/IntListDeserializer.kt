package ch.frostnova.tools.kafkarouter.config.converter

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer

class IntListDeserializer : StdDeserializer<List<Int>>(List::class.java) {
    override fun deserialize(p: JsonParser, context: DeserializationContext?): List<Int> {
        if (p.currentToken() == JsonToken.START_ARRAY) {
            val result = mutableListOf<Int>()
            while (p.nextToken() != JsonToken.END_ARRAY) {
                result.add(p.valueAsInt)
            }
            return result
        }
        if (p.currentToken() == JsonToken.VALUE_STRING) {
            return p.valueAsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }.map { it.toInt() }
        }
        if (p.currentToken() == JsonToken.VALUE_NUMBER_INT) {
            return listOf(p.valueAsInt)
        }
        if (p.currentToken() == JsonToken.VALUE_NUMBER_FLOAT) {
            return listOf(p.valueAsInt)
        }
        throw IllegalArgumentException("cannot deserialize string list from value ${p.currentToken()}")
    }
}
