package ru.whyhappen.pcidss.iso8583.utils

/**
 * BCD (Binary-Coded Decimal) encoder and decoder for byte arrays.
 * Each decimal digit (0-9) is represented by 4 bits.
 * Two decimal digits are packed into one byte.
 */
object Bcd {
    /**
     * Encodes a decimal string to BCD byte array.
     *
     * @param decimal the string containing only digits 0-9
     * @return the BCD encoded byte array
     * @throws IllegalArgumentException if input contains non-digit characters
     */
    fun encode(decimal: String): ByteArray {
        // validate input - only digits allowed
        require(decimal.all { it.isDigit() }) { "Input must contain only digits 0-9" }

        // pad with leading zero if odd number of digits
        val paddedDecimal = if (decimal.length % 2 == 1) "0$decimal" else decimal

        val result = ByteArray(encodeLen(paddedDecimal.length))

        for (i in paddedDecimal.indices step 2) {
            val highNibble = paddedDecimal[i].digitToInt()
            val lowNibble = paddedDecimal[i + 1].digitToInt()

            // pack two decimal digits into one byte
            result[i / 2] = ((highNibble shl 4) or lowNibble).toByte()
        }

        return result
    }

    /**
     * Decodes BCD byte array to decimal string.
     *
     * @param bcdData the array containing BCD encoded data
     * @param trimLeadingZeros whether to remove leading zeros from result
     * @throws IllegalArgumentException if BCD data contains invalid nibbles (> 9)
     */
    fun decode(bcdData: ByteArray, trimLeadingZeros: Boolean = true): String {
        val result = StringBuilder()

        for (byte in bcdData) {
            val unsignedByte = byte.toInt() and 0xFF
            val highNibble = (unsignedByte shr 4) and 0x0F
            val lowNibble = unsignedByte and 0x0F

            // validate BCD - each nibble must be 0-9
            require(highNibble <= 9 && lowNibble <= 9) { "Invalid BCD data: nibble value > 9" }

            result.append(highNibble)
                .append(lowNibble)
        }

        val resultString = result.toString()

        return if (trimLeadingZeros) {
            resultString.trimStart('0').ifEmpty { "0" }
        } else {
            resultString
        }
    }

    /**
     * Returns amount of space needed to store bytes after encoding data of the specified length.
     */
    fun encodeLen(length: Int): Int = (length + 1) / 2
}

// extension functions for convenience
fun String.toBcd(): ByteArray = Bcd.encode(this)
fun Int.toBcd(): ByteArray = Bcd.encode(this.toString())
fun ByteArray.fromBcd(): String = Bcd.decode(this, false)
fun ByteArray.fromBcdToInt(): Int {
    val decimalString = Bcd.decode(this)
    return decimalString.toInt()
}