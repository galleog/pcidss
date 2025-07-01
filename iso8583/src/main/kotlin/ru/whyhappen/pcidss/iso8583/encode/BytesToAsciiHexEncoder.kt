package ru.whyhappen.pcidss.iso8583.encode

import ru.whyhappen.pcidss.iso8583.fields.IsoField

/**
 * [Encoder] that converts bytes into their ASCII representation.
 * On success, the ASCII representation bytes are returned.
 *
 * Don't use this encoder with string, numeric or binary fields as packing and
 * unpacking in these fields uses length of value/bytes, so only [IsoField.pack] will be
 * able to write the value correctly.
 */
@OptIn(ExperimentalStdlibApi::class)
class BytesToAsciiHexEncoder : Encoder {
    /**
     * Converts bytes into their ASCII representation.
     * On success, the ASCII representation bytes are returned, e.g. [0x5F, 0x2A] would be "5F2A".
     */
    override fun encode(data: ByteArray): ByteArray {
        val hexStr = data.toHexString(HexFormat.UpperCase)
        return hexStr.toByteArray()
    }

    /**
     * Decodes ASCII hexadecimal string and returns bytes and the number of read hex bytes.
     * Length is the number of hexadecimal digits (two ASCII characters is one hexadecimal digit);
     * e.g. "AABBCC" would be converted into [0xAA, 0xBB, 0xCC].
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