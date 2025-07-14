package ru.whyhappen.pcidss.iso8583.fields

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import ru.whyhappen.pcidss.iso8583.encode.BinaryEncoder
import ru.whyhappen.pcidss.iso8583.encode.BytesToAsciiHexEncoder
import ru.whyhappen.pcidss.iso8583.encode.EncoderException
import ru.whyhappen.pcidss.iso8583.encode.Encoders
import ru.whyhappen.pcidss.iso8583.prefix.Binary
import ru.whyhappen.pcidss.iso8583.prefix.HexFixedPrefixer
import ru.whyhappen.pcidss.iso8583.spec.Spec
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Tests for [Bitmap].
 */
class BitmapTest {
    @Test
    fun `should set bits correctly`() {
        with(createBitmap(2)) {
            set(2)
            isSet(2).shouldBeTrue()
            bytes shouldBe byteArrayOf(0b01000000.toByte(), 0)

            set(16)
            isSet(16).shouldBeTrue()
            bytes shouldBe byteArrayOf(0b01000000.toByte(), 0b00000001.toByte())

            set(20)
            isSet(20).shouldBeTrue()
            bytes shouldBe byteArrayOf(0b11000000.toByte(), 0b00000001.toByte(), 0b00010000.toByte(), 0)

            set(64)
            isSet(64).shouldBeTrue()
            bytes shouldBe byteArrayOf(
                0b11000000.toByte(),
                0b00000001.toByte(),
                0b10010000.toByte(),
                0,
                0b10000000.toByte(),
                0,
                0,
                0b00000001.toByte()
            )
        }
    }

    @Test
    fun `should check bitmap presence bits`() {
        with(createBitmap(8)) {
            isBitmapPresenceBit(1).shouldBeTrue()
            isBitmapPresenceBit(65).shouldBeTrue()
            isBitmapPresenceBit(129).shouldBeTrue()
            isBitmapPresenceBit(193).shouldBeTrue()

            isBitmapPresenceBit(2).shouldBeFalse()
            isBitmapPresenceBit(66).shouldBeFalse()
            isBitmapPresenceBit(130).shouldBeFalse()
            isBitmapPresenceBit(194).shouldBeFalse()
        }
    }

    private fun createBitmap(length: Int): Bitmap {
        val spec = Spec(length, "Bitmap", BinaryEncoder(), Binary.fixed)
        return Bitmap(spec)
    }

    @Nested
    inner class HexBitmapTest {
        private val encoder = BytesToAsciiHexEncoder()
        private val prefixer = HexFixedPrefixer()
        private lateinit var bitmap: Bitmap

        @BeforeTest
        fun setup() {
            val spec = Spec(8, "Bitmap", encoder, prefixer)
            bitmap = Bitmap(spec)
        }

        @Test
        fun `should pack hex bitmap`() {
            with(bitmap) {
                set(20)
                pack() shouldHaveSize 16

                set(70)
                pack() shouldHaveSize 32

                set(150)
                pack() shouldHaveSize 48
            }
        }

        @Test
        fun `should unpack hex bitmap`() {
            with(bitmap) {
                unpack("004000000000000000000000000000000000000000000000".toByteArray()) shouldBe 16
                isSet(10).shouldBeTrue()

                unpack("804000000000000004000000000000000000000000000000".toByteArray()) shouldBe 32
                isSet(10).shouldBeTrue()
                isSet(70).shouldBeTrue()

                unpack("804000000000000080000000000000000010000000000000".toByteArray()) shouldBe 48
                isSet(10).shouldBeTrue()
                isSet(140).shouldBeTrue()


            }
        }

        @Test
        fun `should fail to unpack bitmap because there's not enough data to unpack`() {
            shouldThrow<EncoderException> {
                bitmap.unpack("4000".toByteArray())
            }
        }

        @Test
        fun `should fail to unpack bitmap when bit for secondary bitmap is set but not enough data to read`() {
            shouldThrow<EncoderException> {
                bitmap.unpack("c0001000000000008000000000000100".toByteArray())
            }
        }
    }

    @Nested
    inner class BinaryBitmapTest {
        private lateinit var bitmap: Bitmap

        @BeforeTest
        fun setup() {
            val spec = Spec(8, "Bitmap", Encoders.binary, Binary.fixed)
            bitmap = Bitmap(spec)
        }

        @Test
        fun `should pack binary bitmap`() {
            with(bitmap) {
                set(20)
                pack() shouldHaveSize 8

                set(70)
                pack() shouldHaveSize 16

                set(150)
                pack() shouldHaveSize 24
            }
        }

        @Test
        fun `should unpack binary bitmap`() {
            with(bitmap) {
                unpack(byteArrayOf(0, 0x40, 0, 0, 0, 0, 0, 0)) shouldBe 8
                isSet(10).shouldBeTrue()

                unpack(byteArrayOf(0x80.toByte(), 0x40, 0, 0, 0, 0, 0, 0, 0x04, 0, 0, 0, 0, 0, 0, 0)) shouldBe 16
                isSet(10).shouldBeTrue()
                isSet(70).shouldBeTrue()

                unpack(
                    byteArrayOf(
                        0x80.toByte(), 0x40, 0, 0, 0, 0, 0, 0,
                        0x80.toByte(), 0, 0, 0, 0, 0, 0, 0,
                        0, 0x10, 0, 0, 0, 0, 0, 0
                    )
                ) shouldBe 24
                isSet(10).shouldBeTrue()
                isSet(140).shouldBeTrue()
            }
        }
    }
}