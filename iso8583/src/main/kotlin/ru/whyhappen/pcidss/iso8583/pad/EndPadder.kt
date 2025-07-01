package ru.whyhappen.pcidss.iso8583.pad

/**
 * [Padder] that pads a byte at the end.
 */
class EndPadder(padChar: Char) : AbstractPadder(padChar) {
    override fun pad(bytes: ByteArray, length: Int): ByteArray =
        if (bytes.size >= length) bytes
        else bytes + fillArray(length - bytes.size)

    override fun unpad(bytes: ByteArray): ByteArray {
        val paddingLength = findPaddingBoundary(bytes, fromEnd = true)
        val originalLength = bytes.size - paddingLength
        return bytes.copyOf(originalLength)
    }
}