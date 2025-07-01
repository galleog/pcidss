package ru.whyhappen.pcidss.iso8583.encode

/**
 * [Encoder] for ASCII strings.
 */
@OptIn(ExperimentalStdlibApi::class)
class AsciiEncoder : Encoder {
    override fun encode(data: ByteArray): ByteArray {
        for (byte in data) {
            checkEncoder(byte.toUByte() <= 127u) { "Invalid ASCII character 0x${byte.toHexString()}" }
        }

        return data.copyOf()
    }

    override fun decode(data: ByteArray, length: Int): Pair<ByteArray, Int> {
        checkEncoder(length >= 0) { "Length $length can't be negative" }
        checkEncoder(length <= data.size) { "Not enough data ${data.size} to decode $length bytes" }

        val result = data.sliceArray(0 until length)
        for (byte in result) {
            checkEncoder(byte.toUByte() <= 127u) { "Invalid ASCII character 0x${byte.toHexString()}" }
        }

        return result to length
    }
}