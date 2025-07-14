package ru.whyhappen.pcidss.iso8583.pad

/**
 * Base class for implementations of [Padder].
 */
abstract class AbstractPadder(protected val padChar: Char) : Padder {
    internal val padBytes = padChar.toString().toByteArray(Charsets.UTF_8)

    /**
     * Creates an array of the specified length filled with [padBytes].
     */
    protected fun fillArray(length: Int): ByteArray {
        checkPadder(length % padBytes.size == 0) {
            "Data length $length must divide evenly by padding size ${padBytes.size}"
        }

        return ByteArray(length).apply {
            var offset = 0
            repeat(length / padBytes.size) {
                padBytes.copyInto(this, offset, 0)
                offset += padBytes.size
            }
        }
    }

    /**
     * Finds the length of padding in a byte array.
     *
     * The function only considers padding lengths that are multiples of padBytes size to ensure
     * correct handling of multibyte characters.
     */
    protected fun findPaddingBoundary(bytes: ByteArray, fromEnd: Boolean): Int {
        if (bytes.isEmpty() || padBytes.isEmpty()) return 0

        val maxPadChars = bytes.size / padBytes.size

        // find the maximum number of consecutive pad characters
        for (numPadChars in 1..maxPadChars) {
            val paddingLength = numPadChars * padBytes.size
            val startIndex = if (fromEnd) bytes.size - paddingLength else 0

            if (!isPaddingPattern(bytes, startIndex, paddingLength)) {
                return (numPadChars - 1) * padBytes.size
            }
        }

        return maxPadChars * padBytes.size
    }

    /**
     * Checks if the specified section of the byte array matches the padding pattern.
     */
    private fun isPaddingPattern(bytes: ByteArray, startIndex: Int, length: Int): Boolean {
        for (i in 0 until length) {
            val paddingIndex = i % padBytes.size
            val byteIndex = startIndex + i

            if (bytes[byteIndex] != padBytes[paddingIndex]) return false
        }

        return true
    }

    override fun toString(): String = "${this::class.simpleName!!}($padChar)"
}
