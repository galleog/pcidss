package ru.whyhappen.pcidss.iso8583.spec

import ru.whyhappen.pcidss.iso8583.encode.Encoder
import ru.whyhappen.pcidss.iso8583.pad.Padder
import ru.whyhappen.pcidss.iso8583.prefix.Prefixer

/**
 * [Packer] uses the number of ASCII characters in the hex representation of a byte array
 * instead of the number of bytes.
 */
data class HexPacker(
    private val length: Int,
    private val encoder: Encoder,
    private val prefixer: Prefixer,
    private val padder: Padder? = null
) : Packer {
    override fun pack(bytes: ByteArray): ByteArray {
        val paddedValue = padder?.run { pad(bytes, length) } ?: bytes
        val encodedValue = encoder.encode(paddedValue)

        // the number of hex characters is twice the number of bytes
        val maxLen = length * 2
        val dataLen = paddedValue.size * 2

        // encode the length
        val lengthPrefix = prefixer.encodeLength(maxLen, dataLen)
        return lengthPrefix + encodedValue
    }

    override fun unpack(bytes: ByteArray): Pair<ByteArray, Int> {
        val (hexDataLen, prefBytes) = prefixer.decodeLength(length * 2, bytes)

        // the number of bytes is half as many as hex characters
        val dataLen = hexDataLen / 2

        val (decodedValue, read) = encoder.decode(
            bytes.sliceArray(prefBytes until bytes.size),
            dataLen
        )

        val unpaddedValue = padder?.run { unpad(decodedValue) } ?: decodedValue
        return unpaddedValue to (prefBytes + read)
    }
}