package ru.whyhappen.pcidss.iso8583.fields

import org.slf4j.LoggerFactory
import ru.whyhappen.pcidss.iso8583.spec.Spec
import kotlin.experimental.and
import kotlin.experimental.or

/**
 * Bitmap of an ISO8583 message that indicates which fields are present.
 */
class Bitmap(override val spec: Spec) : IsoField {
    companion object {
        private const val FIRST_BIT_ON = 0b10000000.toByte()
        private val logger = LoggerFactory.getLogger(Bitmap::class.java)
    }

    override var bytes = ByteArray(spec.length)

    /**
     * Length of the bitmap in bits.
     */
    val length: Int
        get() = bytes.size * 8

    override fun <T : Any> setValue(value: T) {
        require(value is ByteArray) { "Can't convert value from ${value::class.java.name} to ${ByteArray::class.java.name}" }
        bytes = value
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getValue(cls: Class<T>): T {
        require(cls.isInstance(bytes)) { "Can't convert value from ${ByteArray::class.java.name} to ${cls.name}" }
        return bytes as T
    }

    override fun pack(): ByteArray {
        logger.debug("Packing bitmap\n{}\n using {}", this, spec)
        return spec.encoder.encode(bytes)
    }

    /**
     * Sets the bitmap data. It returns the number of bytes read from the byte array.
     * Usually it's 8 for binary, 16 for hex - for a single bitmap.
     * It will read all bitmaps until the first bit of the read bitmap is not set.
     */
    override fun unpack(bytes: ByteArray): Int {
        logger.debug("Unpacking bitmap using {}", spec)

        val (minLen, _) = spec.prefixer.decodeLength(spec.length, bytes)

        this.bytes = byteArrayOf()
        var read = 0

        // read until we have no more bitmaps
        while (true) {
            val (decoded, readDecoded) = spec.encoder.decode(bytes.copyOfRange(read, bytes.size), minLen)
            this.bytes += decoded
            read += readDecoded

            // if the first bit of the decoded bitmap isn't set, exit loop
            if (this.bytes[this.bytes.size - spec.length] and FIRST_BIT_ON == 0.toByte()) break
        }

        logger.debug("Bitmap unpacked:\n{}", this)
        return read
    }

    override fun copyOf(): Bitmap {
        val result = Bitmap(spec)
        result.bytes = bytes.copyOf()
        return result
    }

    override fun reset() {
        bytes = ByteArray(spec.length)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Bitmap

        if (spec != other.spec) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = spec.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }

    override fun toString(): String = bytes.joinToString(" ") {
        Integer.toBinaryString(it.toInt() and 0xFF)
            .padStart(8, '0')
    }

    /**
     * Sets the `n`th bit of the bitmap to 1 that means that the ISO8583 message has *n*th field.
     */
    fun set(n: Int) {
        if (n <= 0) return

        // do we have to expand bitmap?
        if (n > bytes.size * 8) {
            // calculate how many bitmaps we need to store n-th bit
            val bitmapIndex = (n - 1) / (spec.length * 8)
            val newBitmapCount = bitmapIndex + 1

            // set the first bit of the last bitmap in current data to 1 to show the presence of the next bitmap
            bytes[bytes.size - spec.length] = bytes[bytes.size - spec.length] or FIRST_BIT_ON

            // add new empty bitmaps and for every new bitmap except the
            // last one, set bit that shows the presence of the next bitmap
            for (i in newBitmapCount - bytes.size / spec.length downTo 1) {
                val newBitmap = ByteArray(spec.length)

                // set the first bit of the new bitmap to 1, but only if it isn't the last bitmap
                if (i > 1) {
                    newBitmap[0] = newBitmap[0] or FIRST_BIT_ON
                }

                bytes += newBitmap
            }
        }

        val bitIndex = n - 1 // 0-based
        val byteIndex = bitIndex / 8
        val bitInByte = bitIndex % 8
        val offset = 7 - bitInByte // reverse order for MSB=bit 1 convention (0..7)

        bytes[byteIndex] = bytes[byteIndex] or (1 shl offset).toByte()
    }

    /**
     * Indicates if the ISO8583 message has the `n`th field.
     */
    fun isSet(n: Int): Boolean {
        if (n <= 0 || n > bytes.size * 8) return false

        val bitIndex = n - 1
        val byteIndex = bitIndex / 8
        val bitInByte = bitIndex % 8
        val offset = 7 - bitInByte // reverse order for MSB=bit 1 convention (0..7)

        return bytes[byteIndex] and (1 shl offset).toByte() != 0.toByte()
    }

    /**
     * Checks if the bit at position `n` in the bitmap is an indicator of an additional bitmap.
     */
    fun isBitmapPresenceBit(n: Int): Boolean =
        if (n <= 0) false else n % (spec.length * 8) == 1
}