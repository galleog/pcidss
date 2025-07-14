package ru.whyhappen.pcidss.iso8583.prefix

/**
 * HEX prefixers.
 */
object Hex {
    val fixed = HexFixedPrefixer()
}

/**
 * [Prefixer] for hex-encoded fields of fixed length.
 */
class HexFixedPrefixer : FixedPrefixer() {
    override fun encodeLength(maxLen: Int, dataLen: Int): ByteArray {
        checkPrefixer(maxLen * 2 == dataLen) { "Field length $dataLen should be ${maxLen * 2}" }
        return byteArrayOf()
    }
}