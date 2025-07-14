package ru.whyhappen.pcidss.iso8583.spec

import ru.whyhappen.pcidss.iso8583.encode.Encoder
import ru.whyhappen.pcidss.iso8583.pad.Padder
import ru.whyhappen.pcidss.iso8583.prefix.Prefixer

/**
 * Default realization of [Packer].
 *
 * It uses the field value to calculate the length prefix not the data after encoding.
 * This default behavior may not meet your requirements. For instance, you might need the length prefix to represent
 * the length of the encoded data instead. This is often necessary when using BCD or HEX encoding,
 * where the field value's length differs from the encoded field value's length.
 */
data class DefaultPacker(
    private val length: Int,
    private val encoder: Encoder,
    private val prefixer: Prefixer,
    private val padder: Padder?
) : Packer {
    override fun pack(bytes: ByteArray): ByteArray {
        // pad the value if needed
        val paddedValue = padder?.run { pad(bytes, length) } ?: bytes

        // encode the value
        val encodedValue = encoder.encode(paddedValue)

        // encode the length
        val lengthPrefix = prefixer.encodeLength(length, paddedValue.size)
        return lengthPrefix + encodedValue
    }

    override fun unpack(bytes: ByteArray): Pair<ByteArray, Int> {
        // get the field length
        val (dataLen, prefBytes) = prefixer.decodeLength(length, bytes)

        // decode the byte array
        val (decodedValue, read) = encoder.decode(
            bytes.sliceArray(prefixer.digits until bytes.size),
            dataLen
        )

        // unpad the value if needed
        val unpaddedValue = padder?.run { unpad(decodedValue) } ?: decodedValue
        return unpaddedValue to (prefBytes + read)
    }
}