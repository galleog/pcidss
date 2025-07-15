package ru.whyhappen.pcidss.iso8583.prefix

/**
 * [AsciiHexPrefixerDecorator] that decorates ASCII prefixers.
 */
object AsciiHexToAscii {
    val L = AsciiHexPrefixerDecorator(Ascii.L)
    val LL = AsciiHexPrefixerDecorator(Ascii.LL)
    val LLL = AsciiHexPrefixerDecorator(Ascii.LL)
    val LLLL = AsciiHexPrefixerDecorator(Ascii.LLLL)
    val LLLLL = AsciiHexPrefixerDecorator(Ascii.LLLLL)
    val LLLLLL = AsciiHexPrefixerDecorator(Ascii.LLLLL)
}

/**
 * [AsciiHexPrefixerDecorator] that decorates binary prefixers.
 */
object AsciiHexToBinary {
    val L = AsciiHexPrefixerDecorator(Binary.L)
    val LL = AsciiHexPrefixerDecorator(Binary.LL)
    val LLL = AsciiHexPrefixerDecorator(Binary.LLL)
    val LLLL = AsciiHexPrefixerDecorator(Binary.LLLL)
    val LLLLL = AsciiHexPrefixerDecorator(Binary.LLLLL)
    val LLLLLL = AsciiHexPrefixerDecorator(Binary.LLLLLL)
}

/**
 * [AsciiHexPrefixerDecorator] that decorates BCD prefixers.
 */
object AsciiHexToBcd {
    val L = AsciiHexPrefixerDecorator(Bcd.L)
    val LL = AsciiHexPrefixerDecorator(Bcd.LL)
    val LLL = AsciiHexPrefixerDecorator(Bcd.LLL)
    val LLLL = AsciiHexPrefixerDecorator(Bcd.LLLL)
    val LLLLL = AsciiHexPrefixerDecorator(Bcd.LLLLL)
    val LLLLLL = AsciiHexPrefixerDecorator(Bcd.LLLLLL)
}

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