package ru.whyhappen.pcidss.iso8583.encode

/**
 * [Encoder] that converts ASCII hex digits into a byte slice.
 */
@OptIn(ExperimentalStdlibApi::class)
class AsciiHexToBytesEncoder : Encoder {
    /**
     * Converts ASCII hex digits into a byte slice.
     *
     * @return the actual bytes of the ASCII hex representation ("AABBCC" would be converted into [0xAA, 0xBB, 0xCC])
     */
    override fun encode(data: ByteArray): ByteArray = data.toString(Charsets.US_ASCII)
        .hexToByteArray()

    /**
     * Converts bytes into their ASCII representation.
     *
     * @param length the number of hexadecimal digits (two ASCII characters is one hexadecimal digit)
     * @return the ASCII representation bytes ([0x5F, 0x2A] would be converted to "5F2A")
     */
    override fun decode(data: ByteArray, length: Int): Pair<ByteArray, Int> {
        checkEncoder(length >= 0) { "Length $length can't be negative" }
        checkEncoder(length <= data.size) { "Not enough data ${data.size} to decode $length bytes" }

        val decodedValue = data.sliceArray(0 until length)
            .toHexString(HexFormat.UpperCase)
            .toByteArray(Charsets.US_ASCII)
        return decodedValue to length
    }
}