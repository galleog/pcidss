package ru.whyhappen.pcidss.iso8583.encode

/**
 * [Encoder] for binary data.
 */
class BinaryEncoder : Encoder {
    override fun encode(data: ByteArray): ByteArray = data.copyOf()

    override fun decode(data: ByteArray, length: Int): Pair<ByteArray, Int> {
        checkEncoder(length >= 0) { "Length $length can't be negative" }
        checkEncoder(length <= data.size) { "Not enough data ${data.size} to decode $length bytes" }

        return data.sliceArray(0 until length) to length
    }
}