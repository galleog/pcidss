package ru.whyhappen.pcidss.iso8583.spec

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import ru.whyhappen.pcidss.iso8583.fields.IsoField

/**
 * Specification for ISO messages.
 */
@JsonDeserialize(using = MessageSpecDeserializer::class)
data class MessageSpec(
    /**
     * Fields the message can have.
     */
    val fields: Map<Int, IsoField>
) {
    operator fun plus(other: MessageSpec): MessageSpec = MessageSpec(this.fields + other.fields)
}

/**
 * Custom deserializer for [MessageSpec] that creates it from number-object pairs.
 */
class MessageSpecDeserializer : StdDeserializer<MessageSpec>(MessageSpec::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): MessageSpec {
        val node = p.readValueAsTree<JsonNode>()

        // create fields
        val fields = node.properties()
            .mapNotNull { (key, value) ->
                key.toIntOrNull()?.let { it to p.codec.treeToValue(value, IsoField::class.java) }
            }.toMap()

        return MessageSpec(fields)
    }
}
