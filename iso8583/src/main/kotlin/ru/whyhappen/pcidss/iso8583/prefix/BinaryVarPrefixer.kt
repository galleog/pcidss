package ru.whyhappen.pcidss.iso8583.prefix

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * [Prefixer] that encodes field length as an integer.
 */
class BinaryVarPrefixer(override val bytesSize: Int) : Prefixer {
    override fun encodeLength(maxLen: Int, dataLen: Int): ByteArray {
        checkPrefixer(maxLen >= dataLen) { "Field length $dataLen is greater than maximum $maxLen" }

        val bytes = intToBytes(dataLen)
        return adjustBytesLength(bytes, bytesSize)
    }

    override fun decodeLength(maxLen: Int, data: ByteArray): Pair<Int, Int> {
        checkPrefixer(data.size >= bytesSize) { "Not enough data ${data.size} to read $bytesSize byte digits" }

        val bytes = data.sliceArray(0 until bytesSize)
        val result = bytesToInt(bytes)
        checkPrefixer(result <= maxLen) { "Data length $result is greater than maximum $maxLen" }
        return result to bytesSize
    }

    private fun intToBytes(n: Int): ByteArray {
        checkPrefixer(n >= 0) { "Value must be non-negative" }

        val buffer = ByteBuffer.allocate(Int.SIZE_BYTES)
            .order(ByteOrder.BIG_ENDIAN)
            .putInt(n)
        return buffer.array()
    }

    private fun bytesToInt(bytes: ByteArray): Int {
        val intBytes = adjustBytesLength(bytes, Int.SIZE_BYTES)
        val n = ByteBuffer.wrap(intBytes).order(ByteOrder.BIG_ENDIAN).int
        checkPrefixer(n >= 0) { "Value must be non-negative" }
        return n
    }

    /**
     * Prepends a byte array with leading 0x00.
     */
    private fun prependWithZeros(bytes: ByteArray, length: Int): ByteArray {
        return if (bytes.size == length) bytes else ByteArray(length)
            .apply {
                repeat(length - bytes.size) {
                    this[it] = 0x00
                }
                bytes.copyInto(this, length - bytes.size, 0, bytes.size)
            }
    }

    /**
     * Remove leading 0x00 from a byte array.
     */
    private fun removeLeadingZeros(bytes: ByteArray, length: Int): ByteArray {
        if (bytes.size == length) return bytes

        var offset = 0
        while (offset < bytes.size - length && bytes[offset] == 0x00.toByte()) {
            offset++
        }
        return bytes.copyOfRange(offset, bytes.size)
    }

    private fun adjustBytesLength(bytes: ByteArray, length: Int): ByteArray {
        val result = if (bytes.size <= length) prependWithZeros(bytes, length) else removeLeadingZeros(bytes, length)
        checkPrefixer(result.size == length) {
            "Bytes of size ${bytes.size} cannot be adjusted to the required length $length"
        }
        return result
    }
}