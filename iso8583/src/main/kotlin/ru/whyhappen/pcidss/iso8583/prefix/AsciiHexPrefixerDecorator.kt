package ru.whyhappen.pcidss.iso8583.prefix

/**
 * Decorator for an [Prefixer] uses the number of ASCII characters in the hex representation of a byte array
 * to generate a length prefix.
 */
class AsciiHexPrefixerDecorator(private val prefixer: Prefixer) : Prefixer {
    override val digits: Int = prefixer.digits

    override fun encodeLength(maxLen: Int, dataLen: Int): ByteArray {
        // two ASCII characters is one hexadecimal digit
        return prefixer.encodeLength(maxLen * 2, dataLen * 2)
    }

    override fun decodeLength(maxLen: Int, data: ByteArray): Pair<Int, Int> {
        val (hexDataLen, prefBytes) = prefixer.decodeLength(maxLen * 2, data)
        return (hexDataLen / 2) to prefBytes
    }
}