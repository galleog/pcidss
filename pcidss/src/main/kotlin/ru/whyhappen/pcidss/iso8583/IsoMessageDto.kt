package ru.whyhappen.pcidss.iso8583

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer

/**
 * DTO for [IsoMessage] with flattened JSON representation.
 * Serializes to: { "0": "0210", "2": "value2", "3": "value3" }.
 */
@JsonSerialize(using = IsoMessageDtoSerializer::class)
@JsonDeserialize(using = IsoMessageDtoDeserializer::class)
data class IsoMessageDto(
    val mti: Int?,
    val fields: Map<Int, String> = emptyMap()
)

/**
 * Custom serializer for [IsoMessageDto] that flattens it to field number-value pairs.
 */
class IsoMessageDtoSerializer : StdSerializer<IsoMessageDto>(IsoMessageDto::class.java) {
    override fun serialize(value: IsoMessageDto, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeStartObject()

        // write MTI as the field "0" with a formatted hex string
        value.mti?.let { mti ->
            gen.writeStringField("0", "%04x".format(mti))
        }

        // write all fields as direct properties
        value.fields.forEach { (key, value) ->
            gen.writeStringField(key.toString(), value)
        }

        gen.writeEndObject()
    }
}

/**
 * Custom deserializer for [IsoMessageDto] that creates it from flat field number-value pairs.
 */
class IsoMessageDtoDeserializer : StdDeserializer<IsoMessageDto>(IsoMessageDto::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): IsoMessageDto {
        val node = p.readValueAsTree<JsonNode>()

        // parse MTI from field "0" if present
        val mti = node.get("0")?.textValue()?.toInt(16)

        // collect all non-0 fields
        val fields = node.properties().filter { (key, _) -> key != "0" }
            .mapNotNull { (key, value) -> key.toIntOrNull()?.let { it to value.textValue() } }
            .toMap()

        return IsoMessageDto(mti, fields)
    }
}