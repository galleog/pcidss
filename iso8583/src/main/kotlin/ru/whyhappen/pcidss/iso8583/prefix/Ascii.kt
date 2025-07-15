package ru.whyhappen.pcidss.iso8583.prefix

/**
 * ASCII prefixers.
 */
object Ascii {
    val fixed = AsciiFixedPrefixer()
    val L = AsciiVarPrefixer(1)
    val LL = AsciiVarPrefixer(2)
    val LLL = AsciiVarPrefixer(3)
    val LLLL = AsciiVarPrefixer(4)
    val LLLLL = AsciiVarPrefixer(5)
    val LLLLLL = AsciiVarPrefixer(6)
}

/**
 * [Prefixer] for ASCII string fields of fixed length.
 */
class AsciiFixedPrefixer : FixedPrefixer()

/**
 * [Prefixer] that encodes field length as ASCII digits.
 */
class AsciiVarPrefixer(override val digits: Int) : Prefixer {
    override fun encodeLength(maxLen: Int, dataLen: Int): ByteArray {
        checkPrefixer(maxLen >= dataLen) { "Field length $dataLen is greater than maximum $maxLen" }
        checkPrefixer(dataLen.toString().length <= digits) { "Number of digits in length $dataLen exceeds $digits" }

        val result = "%0${digits}d".format(dataLen)
        return result.toByteArray()
    }

    override fun decodeLength(maxLen: Int, data: ByteArray): Pair<Int, Int> {
        checkPrefixer(data.size >= digits) { "Not enough data ${data.size} to read $digits bytes" }

        val result = data.sliceArray(0 until digits)
            .toString(Charsets.US_ASCII)
            .toInt()
        checkPrefixer(maxLen >= result) { "Data length $result is greater than maximum $maxLen" }
        return result to digits
    }

    override fun toString(): String = "AsciiVarPrefixer($digits)"
}