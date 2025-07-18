package ru.whyhappen.pcidss.iso8583.prefix

import ru.whyhappen.pcidss.iso8583.utils.Bcd
import ru.whyhappen.pcidss.iso8583.utils.fromBcdToInt
import ru.whyhappen.pcidss.iso8583.utils.toBcd

/**
 * Prefixers that uses BCD (Binary-Coded Decimal) encoding to encode field length
 */
object Bcd {
    val fixed = BcdFixedPrefixer()
    val L = BcdVarPrefixer(1)
    val LL = BcdVarPrefixer(2)
    val LLL = BcdVarPrefixer(3)
    val LLLL = BcdVarPrefixer(4)
    val LLLLL = BcdVarPrefixer(5)
    val LLLLLL = BcdVarPrefixer(6)
}

/**
 * BCD [Prefixer] for fields of fixed length.
 */
class BcdFixedPrefixer : FixedPrefixer()

/**
 * [Prefixer] that uses BCD (Binary-Coded Decimal) encoding to encode field length.
 */
class BcdVarPrefixer(override val digits: Int) : Prefixer {
    override fun encodeLength(maxLen: Int, dataLen: Int): ByteArray {
        checkPrefixer(maxLen >= dataLen) { "Field length $dataLen is greater than maximum $maxLen" }
        checkPrefixer(dataLen.toString().length <= digits) { "Number of digits in length $dataLen exceeds $digits" }

        return "%0${digits}d".format(dataLen)
            .toBcd()
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun decodeLength(maxLen: Int, data: ByteArray): Pair<Int, Int> {
        val length = Bcd.encodedLen(digits)
        checkPrefixer(data.size >= length) { "Not enough data ${data.size} to read $length bytes" }

        val lengthPrefix = data.sliceArray(0 until length)
        val dataLen = runCatching {
            lengthPrefix.fromBcdToInt()
        }.getOrElse { e ->
            throw PrefixerException("Failed to decode bytes: ${lengthPrefix.toHexString(HexFormat.UpperCase)}", e)
        }

        checkPrefixer(dataLen <= maxLen) { "Data length $dataLen is greater than maximum $maxLen" }
        return dataLen to length
    }

    override fun toString(): String = "BcdVarPrefixer($digits)"
}