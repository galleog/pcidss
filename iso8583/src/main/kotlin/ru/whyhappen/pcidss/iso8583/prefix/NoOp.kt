package ru.whyhappen.pcidss.iso8583.prefix

object NoOp {
    val fixed = NoOpPrefixer()
}

/**
 * [Prefixer] that does nothing.
 */
class NoOpPrefixer : Prefixer {
    override val digits: Int = 0

    override fun encodeLength(maxLen: Int, dataLen: Int): ByteArray = byteArrayOf()

    override fun decodeLength(maxLen: Int, data: ByteArray): Pair<Int, Int> = data.size to 0

    override fun toString(): String = "NoOpPrefixer"
}