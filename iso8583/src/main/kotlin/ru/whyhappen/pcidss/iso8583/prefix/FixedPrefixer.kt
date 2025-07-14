package ru.whyhappen.pcidss.iso8583.prefix

/**
 * Base [Prefixer] for fields of fixed length
 */
abstract class FixedPrefixer : Prefixer {
    override val digits: Int = 0

    override fun encodeLength(maxLen: Int, dataLen: Int): ByteArray {
        checkPrefixer(maxLen == dataLen) { "Field length $dataLen should be $maxLen" }
        return byteArrayOf()
    }

    override fun decodeLength(maxLen: Int, data: ByteArray): Pair<Int, Int> = maxLen to 0

    override fun toString(): String = this::class.simpleName!!
}