package ch.frostnova.tools.kafkarouter.config.converter

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer

class DoubleListDeserializer : StdDeserializer<List<Double>>(List::class.java) {
    override fun deserialize(p: JsonParser, context: DeserializationContext?): List<Double> {
        if (p.currentToken() == JsonToken.START_ARRAY) {
            val result = mutableListOf<Double>()
            while (p.nextToken() != JsonToken.END_ARRAY) {
                result.add(p.valueAsDouble)
            }
            return result
        }
        if (p.currentToken() == JsonToken.VALUE_STRING) {
            return p.valueAsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }.map { it.toDouble() }
        }
        if (p.currentToken() == JsonToken.VALUE_NUMBER_INT) {
            return listOf(p.valueAsDouble)
        }
        if (p.currentToken() == JsonToken.VALUE_NUMBER_FLOAT) {
            return listOf(p.valueAsDouble)
        }
        throw IllegalArgumentException("cannot deserialize string list from value ${p.currentToken()}")
    }
}
