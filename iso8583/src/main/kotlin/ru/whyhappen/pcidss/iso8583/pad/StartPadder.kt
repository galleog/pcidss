package ru.whyhappen.pcidss.iso8583.pad

/**
 * [Padder] that pads bytes at the beginning.
 */
class StartPadder(padChar: Char) : AbstractPadder(padChar) {
    override fun pad(bytes: ByteArray, length: Int): ByteArray =
        if (bytes.size >= length) bytes
        else fillArray(length - bytes.size) + bytes

    override fun unpad(bytes: ByteArray): ByteArray {
        val paddingLength = findPaddingBoundary(bytes, fromEnd = false)
        return bytes.sliceArray(paddingLength until bytes.size)
    }
}