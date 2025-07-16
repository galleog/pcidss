package ru.whyhappen.pcidss.iso8583.encode

import ru.whyhappen.pcidss.iso8583.utils.Bcd
import ru.whyhappen.pcidss.iso8583.utils.fromBcd
import ru.whyhappen.pcidss.iso8583.utils.toBcd

/**
 * [Encoder] that uses BCD (Binary-Coded Decimal) encoding for numeric fields.
 */
@OptIn(ExperimentalStdlibApi::class)
class BcdEncoder : Encoder {
    override fun encode(data: ByteArray): ByteArray {
        return runCatching {
            data.toString(Charsets.US_ASCII)
                .toBcd()
        }.getOrElse { e ->
            throw EncoderException("Failed to encode bytes: ${data.toHexString(HexFormat.UpperCase)}", e)
        }
    }

    override fun decode(data: ByteArray, length: Int): Pair<ByteArray, Int> {
        checkEncoder(length >= 0) { "Length $length can't be negative" }

        // for BCD encoding the length should be even
        val decodedLength = if (length % 2 == 0) length else length + 1

        // how many bytes we will read
        val read = Bcd.encodeLen(decodedLength)
        checkEncoder(read <= data.size) { "Not enough data ${data.size} to decode $read bytes" }

        val bytesToRead = data.sliceArray(0 until read)
        val decimal = runCatching { bytesToRead.fromBcd() }
            .getOrElse { e ->
                throw EncoderException("Failed to decode bytes: ${bytesToRead.toHexString(HexFormat.UpperCase)}", e)
            }

        // because BCD is right aligned, we skip first bytes and read only what we need e.g. 0643 => 643
        val decoded = decimal.substring(decodedLength - length)
            .toByteArray(Charsets.US_ASCII)
        return decoded to read
    }

    override fun toString(): String = "BcdEncoder"
}