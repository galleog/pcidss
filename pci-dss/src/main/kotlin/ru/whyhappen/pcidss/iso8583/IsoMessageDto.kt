package ru.whyhappen.pcidss.iso8583

import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer
import com.fasterxml.jackson.databind.ser.std.ToStringSerializerBase
import com.solab.iso8583.IsoMessage

/**
 * DTO for [IsoMessage].
 */
data class IsoMessageDto(
    @JsonSerialize(using = MtiSerializer::class)
    @JsonDeserialize(using = MtiDeserializer::class)
    val mti: Int?,
    val fields: Map<Int, String>
)

class MtiSerializer : ToStringSerializerBase(Int::class.java) {
    override fun valueToString(value: Any): String =
        if (value is Int) "%04x".format(value)
        else throw IllegalArgumentException("Unsupported value: $value")
}

class MtiDeserializer : FromStringDeserializer<Int>(Int::class.java) {
    override fun _deserialize(value: String, ctxt: DeserializationContext): Int = value.toInt(16)
}