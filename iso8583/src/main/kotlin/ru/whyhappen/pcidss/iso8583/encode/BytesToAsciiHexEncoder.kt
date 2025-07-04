package ru.whyhappen.pcidss.iso8583.encode

/**
 * [Encoder] that converts bytes into their ASCII representation.
 * On success, the ASCII representation bytes are returned.
 */
@OptIn(ExperimentalStdlibApi::class)
class BytesToAsciiHexEncoder : Encoder {
    /**
     * Converts bytes into their ASCII representation.
     *
     * @return the ASCII representation bytes ([0x5F, 0x2A] would be "5F2A")
     */
    override fun encode(data: ByteArray): ByteArray {
        val hexStr = data.toHexString(HexFormat.UpperCase)
        return hexStr.toByteArray(Charsets.US_ASCII)
    }

    /**
     * Decodes ASCII hexadecimal string and returns bytes and the number of read hex bytes.
     *
     * @param length the number of hexadecimal digits (two ASCII characters is one hexadecimal digit)
     * @return the actual bytes of the ASCII representation ("AABBCC" would be converted into [0xAA, 0xBB, 0xCC]) and
     * the number of read bytes (twice as much as [length])
     */
    override fun decode(data: ByteArray, length: Int): Pair<ByteArray, Int> {
        checkEncoder(length >= 0) { "Length $length can't be negative" }

        // to read 8 hex digits we have to read 16 ASCII chars (bytes)
        val read = length * 2
        checkEncoder(read <= data.size) { "Not enough data ${data.size} to decode $read bytes" }

        val slice = data.toString(Charsets.US_ASCII)
            .substring(0 until read)
        return slice.hexToByteArray() to read
    }
}