package ru.whyhappen.pcidss.iso8583.fields

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import ru.whyhappen.pcidss.iso8583.encode.Encoder
import ru.whyhappen.pcidss.iso8583.encode.Encoders
import ru.whyhappen.pcidss.iso8583.pad.EndPadder
import ru.whyhappen.pcidss.iso8583.pad.NoOpPadder
import ru.whyhappen.pcidss.iso8583.pad.Padder
import ru.whyhappen.pcidss.iso8583.pad.StartPadder
import ru.whyhappen.pcidss.iso8583.prefix.*
import ru.whyhappen.pcidss.iso8583.spec.DefaultPacker
import ru.whyhappen.pcidss.iso8583.spec.HexPacker
import ru.whyhappen.pcidss.iso8583.spec.Packer
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
            "ASCII" to Encoders.ascii,
            "Binary" to Encoders.binary,
            "HexToASCII" to Encoders.hexToAscii,
            "ASCIIToHex" to Encoders.asciiToHex,
            "BCD" to Encoders.bcd
        )

        private val prefixers: Map<String, Prefixer> = mapOf(
            "NoOp.fixed" to NoOp.fixed,
            "ASCII.fixed" to Ascii.fixed,
            "ASCII.L" to Ascii.L,
            "ASCII.LL" to Ascii.LL,
            "ASCII.LLL" to Ascii.LLL,
            "ASCII.LLLL" to Ascii.LLLL,
            "Binary.fixed" to Binary.fixed,
            "Binary.L" to Binary.L,
            "Binary.LL" to Binary.LL,
            "Binary.LLL" to Binary.LLL,
            "Binary.LLLL" to Binary.LLLL,
            "Hex.fixed" to Hex.fixed,
            "BCD.fixed" to Bcd.fixed,
            "BCD.L" to Bcd.L,
            "BCD.LL" to Bcd.LL,
            "BCD.LLL" to Bcd.LLL,
            "BCD.LLLL" to Bcd.LLLL
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

        private val packers: Map<String, (Int, Encoder, Prefixer, Padder?) -> Packer> = mapOf(
            "Default" to { length, encoder, prefixer, padder -> DefaultPacker(length, encoder, prefixer, padder) },
            "Hex" to { length, encoder, prefixer, padder -> HexPacker(length, encoder, prefixer, padder) }
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
        val packer = packer(p, node.get("packer"), length, encoder, prefixer, padder)

        return fieldCreator(Spec(length, description, encoder, prefixer, padder, packer))
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

    private fun packer(
        p: JsonParser,
        node: JsonNode?,
        length: Int,
        encoder: Encoder,
        prefixer: Prefixer,
        padder: Padder?
    ): Packer {
        if (node == null) return DefaultPacker(length, encoder, prefixer, padder)

        checkNode(p, node, String::class.java, node.isTextual) { "Field 'packer' must be a string" }

        val packerCreator = packers[node.textValue()] ?: throw InvalidFormatException.from(
            p,
            "Invalid packer type: ${node.textValue()}",
            node,
            String::class.java
        )
        return packerCreator(length, encoder, prefixer, padder)
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