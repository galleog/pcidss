package ru.whyhappen.pcidss.iso8583.pad

import ru.whyhappen.pcidss.iso8583.IsoException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Adds padding to a byte array or, on the contrary, removes it.
 */
interface Padder {
    /**
     * Adds padding to the specified bytes.
     *
     * @param bytes the byte array to add padding to
     * @param length the length of the byte array after the padding
     * @return the byte array after adding the padding
     */
    fun pad(bytes: ByteArray, length: Int): ByteArray

    /**
     * Removes padding from the specified bytes.
     *
     * @param bytes the byte array with the padding
     * @return the byte array after removal of the padding
     */
    fun unpad(bytes: ByteArray): ByteArray
}

/**
 * Exception thrown by [Padder].
 */
class PadderException(message: String) : IsoException(message)

@OptIn(ExperimentalContracts::class)
inline fun checkPadder(value: Boolean, lazyMessage: () -> String) {
    contract {
        returns() implies value
    }

    if (!value) throw PadderException(lazyMessage())
}