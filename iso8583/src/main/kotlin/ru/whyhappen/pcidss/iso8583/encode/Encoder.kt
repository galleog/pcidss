package ru.whyhappen.pcidss.iso8583.encode

import ru.whyhappen.pcidss.iso8583.IsoException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Encodes and decodes data in ISO8583 messages.
 */
interface Encoder {
    /**
     * Encodes source data (ASCII, characters, digits, etc.) into destination bytes,
     */
    fun encode(data: ByteArray): ByteArray

    /**
     * Decodes data into bytes (ASCII, characters, digits, etc.).
     *
     * @param data the byte array whose part should be decoded
     * @param length the length of the part that should be decoded
     * @return the bytes representing the decoded data and the number of bytes read from the input
     */
    fun decode(data: ByteArray, length: Int): Pair<ByteArray, Int>
}

/**
 * Exception thrown by [Encoder].
 */
class EncoderException(message: String, cause: Throwable? = null) : IsoException(message, cause)

@OptIn(ExperimentalContracts::class)
inline fun checkEncoder(value: Boolean, lazyMessage: () -> String) {
    contract {
        returns() implies value
    }

    if (!value) throw EncoderException(lazyMessage())
}

/**
 * All encoders.
 */
object Encoders {
    val ascii = AsciiEncoder()
    val binary = BinaryEncoder()
    val hexToAscii = BytesToAsciiHexEncoder()
    val asciiToHex = AsciiHexToBytesEncoder()
    val bcd = BcdEncoder()
}