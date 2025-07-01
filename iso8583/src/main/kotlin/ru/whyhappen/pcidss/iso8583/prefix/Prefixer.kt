package ru.whyhappen.pcidss.iso8583.prefix

import ru.whyhappen.pcidss.iso8583.IsoException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Determines how to get the length of variable-length fields, such as LLVAR, LLLVAR, etc.
 */
interface Prefixer {
    /**
     * Number of bytes needed to encode field length.
     */
    val bytesSize: Int
    /**
     * Returns field length encoded into a byte array.
     *
     * @param maxLen the maximal field length
     * @param dataLen the field length
     */
    fun encodeLength(maxLen: Int, dataLen: Int): ByteArray

    /**
     * Returns the size of a field (number of characters, hexadecimal digits, bytes)
     * as well as the number of bytes read to decode field length.
     *
     * @param maxLen the maximal field length
     * @param data the field length followed by the field itself
     */
    fun decodeLength(maxLen: Int, data: ByteArray): Pair<Int, Int>
}

/**
 * Exception thrown by [Prefixer].
 */
class PrefixerException(message: String) : IsoException(message)

@OptIn(ExperimentalContracts::class)
inline fun checkPrefixer(value: Boolean, lazyMessage: () -> String) {
    contract {
        returns() implies value
    }

    if (!value) throw PrefixerException(lazyMessage())
}