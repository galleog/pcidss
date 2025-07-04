package ru.whyhappen.pcidss.iso8583.fields

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import ru.whyhappen.pcidss.iso8583.encode.AsciiEncoder
import ru.whyhappen.pcidss.iso8583.encode.BinaryEncoder
import ru.whyhappen.pcidss.iso8583.encode.BytesToAsciiHexEncoder
import ru.whyhappen.pcidss.iso8583.encode.Encoder
import ru.whyhappen.pcidss.iso8583.pad.EndPadder
import ru.whyhappen.pcidss.iso8583.pad.NoOpPadder
import ru.whyhappen.pcidss.iso8583.pad.Padder
import ru.whyhappen.pcidss.iso8583.pad.StartPadder
import ru.whyhappen.pcidss.iso8583.prefix.*
import ru.whyhappen.pcidss.iso8583.spec.Spec
import java.math.BigInteger
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Field of an ISO8583 message.
 */
@JsonDeserialize(using = IsoFieldDeserializer::class)
interface IsoField {
    /**
     * Field specification.
     */
    val spec: Spec

    /**
     * Binary representation of the field value.
     */
    var bytes: ByteArray

    /**
     * Sets the field value.
     * At least two types are supported for all fields: [ByteArray] and [String].
     * Some types allow setting values of other types as well. For example,
     * [NumericField] additionally supports [BigInteger], [Int], and [Long].
     *
     * @param T the type of the value
     */
    fun <T : Any> setValue(value: T)
    /**
     * Gets the field value converted to the specified type.
     */
    fun <T> getValue(cls: Class<T>): T?

    /**
     * Serializes the field into its binary representation.
     */
    fun pack(): ByteArray
    /**
     * Deserializes the field from its binary representation.
     *
     * @return the number of read bytes
     */
    fun unpack(bytes: ByteArray): Int

    /**
     * Creates a copy of the field.
     */
    fun copyOf(): IsoField

    /**
     * Resets the field to its initial state.
     */
    fun reset()
}

class IsoFieldDeserializer : StdDeserializer<IsoField>(IsoField::class.java) {
    companion object {
        private val fields: Map<String, (Spec) -> IsoField> = mapOf(
            "String" to { spec -> StringField(spec = spec) },
            "Binary" to { spec -> BinaryField(spec = spec) },
            "Numeric" to { spec -> NumericField(spec = spec) },
            "Bitmap" to { spec -> Bitmap(spec) }
        )

        private val encoders: Map<String, Encoder> = mapOf(
            "ASCII" to AsciiEncoder(),
            "Binary" to BinaryEncoder(),
            "HexToASCII" to BytesToAsciiHexEncoder(),
        )

        private val prefixers: Map<String, Prefixer> = mapOf(
            "NoOp.Fixed" to NoOpPrefixer(),
            "ASCII.Fixed" to AsciiFixedPrefixer(),
            "ASCII.L" to AsciiVarPrefixer(1),
            "ASCII.LL" to AsciiVarPrefixer(2),
            "ASCII.LLL" to AsciiVarPrefixer(3),
            "ASCII.LLLL" to AsciiVarPrefixer(4),
            "Binary.Fixed" to BinaryFixedPrefixer(),
            "Binary.L" to BinaryVarPrefixer(1),
            "Binary.LL" to BinaryVarPrefixer(2),
            "Binary.LLL" to BinaryVarPrefixer(3),
            "Binary.LLLL" to BinaryVarPrefixer(4),
            "Hex.Fixed" to HexFixedPrefixer(),
            "BCD.Fixed" to BcdFixedPrefixer(),
            "BCD.L" to BcdVarPrefixer(1),
            "BCD.LL" to BcdVarPrefixer(2),
            "BCD.LLL" to BcdVarPrefixer(3),
            "BCD.LLLL" to BcdVarPrefixer(4)
        )

        private val padders: Map<String, (Char?) -> Padder> = mapOf(
            "Start" to { ch ->
                check(ch != null) { "Padding character must not be null" }
                StartPadder(ch)
            },
            "End" to { ch ->
                check(ch != null) { "Padding character must not be null" }
                EndPadder(ch)
            },
            "NoOp" to { NoOpPadder() }
        )
    }

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): IsoField {
        val node = p.readValueAsTree<JsonNode>()

        val fieldCreator = toInstance(p, node.get("type"), fields, "type")
        val length = length(p, node.get("length"))
        val description = description(p, node.get("description"))
        val encoder = toInstance(p, node.get("enc"), encoders, "enc")
        val prefixer = toInstance(p, node.get("prefix"), prefixers, "prefix")
        val padder = padder(p, node.get("padding"))

        return fieldCreator(Spec(length, description, encoder, prefixer, padder))
    }

    private fun <T : Any> toInstance(p: JsonParser, node: JsonNode?, map: Map<String, T>, field: String): T {
        checkNode(p, node, String::class.java, node != null && node.isTextual) { "Field '$field' must be a string" }
        return map[node.textValue()] ?: throw InvalidFormatException.from(
            p,
            "Invalid $field type: ${node.textValue()}",
            node,
            String::class.java
        )
    }

    private fun length(p: JsonParser, node: JsonNode?): Int {
        checkNode(p, node, Int::class.java, node != null && node.isInt) { "Field 'length' must be an integer" }
        return node.intValue()
    }

    private fun description(p: JsonParser, node: JsonNode?): String {
        checkNode(
            p,
            node,
            String::class.java,
            node != null && node.isTextual
        ) { "Field 'description' must be a string" }
        return node.textValue()
    }

    private fun padder(p: JsonParser, node: JsonNode?): Padder? {
        if (node == null) return null

        checkNode(p, node, Any::class.java, node.isObject) { "Field 'padding' must be an object" }

        val padderCreator = toInstance(p, node.get("type"), padders, "type")
        val pad = pad(p, node.get("pad"))
        return padderCreator(pad)
    }

    private fun pad(p: JsonParser, node: JsonNode?): Char? {
        if (node == null) return null

        checkNode(p, node, String::class.java, node.isInt) { "Field 'pad' must be an integer" }
        return node.intValue().toChar()
    }
}

@OptIn(ExperimentalContracts::class)
private fun checkNode(p: JsonParser, node: JsonNode?, cls: Class<*>, value: Boolean, lazyMessage: () -> Any) {
    contract {
        returns() implies value
    }

    if (!value) {
        throw InvalidFormatException.from(p, lazyMessage().toString(), node, cls)
    }
}