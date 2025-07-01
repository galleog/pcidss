package ru.whyhappen.pcidss.iso8583.prefix

/**
 * [Prefixer] that encodes field length as ASCII digits.
 */
class AsciiVarPrefixer(override val bytesSize: Int) : Prefixer {
    override fun encodeLength(maxLen: Int, dataLen: Int): ByteArray {
        checkPrefixer(maxLen >= dataLen) { "Field length $dataLen is greater than maximum $maxLen" }
        checkPrefixer(dataLen.toString().length <= bytesSize) { "Number of digits in length $dataLen exceeds $bytesSize" }

        val result = "%0${bytesSize}d".format(dataLen)
        return result.toByteArray()
    }

    override fun decodeLength(maxLen: Int, data: ByteArray): Pair<Int, Int> {
        checkPrefixer(data.size >= bytesSize) { "Not enough data ${data.size} to read $bytesSize byte digits" }

        val result = data.sliceArray(0 until bytesSize)
            .toString(Charsets.US_ASCII)
            .toInt()
        checkPrefixer(maxLen >= result) { "Data length $result is greater than maximum $maxLen" }
        return result to bytesSize
    }
}