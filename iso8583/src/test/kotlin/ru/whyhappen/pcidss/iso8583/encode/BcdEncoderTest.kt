package ru.whyhappen.pcidss.iso8583.encode

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.Test

/**
 * Tests for [BcdEncoder].
 */
class BcdEncoderTest {
    private val encoder = BcdEncoder()

    companion object {
        @JvmStatic
        private fun testEncodeData() = Stream.of(
            Arguments.of("0110".toByteArray(), byteArrayOf(0x01, 0x10)),
            Arguments.of("123".toByteArray(), byteArrayOf(0x01, 0x23)),
            Arguments.of("51230".toByteArray(), byteArrayOf(0x05, 0x12, 0x30))
        )

        @JvmStatic
        private fun testDecodeData() = Stream.of(
            Arguments.of(byteArrayOf(0x12, 0x34), 4, "1234".toByteArray(), 2),
            Arguments.of(byteArrayOf(0x01, 0x23), 3, "123".toByteArray(), 2),
            Arguments.of(byteArrayOf(0x12, 0x30), 3, "230".toByteArray(), 2),
            Arguments.of(byteArrayOf(0x21, 0x43, 0x55), 4, "2143".toByteArray(), 2),
            Arguments.of(byteArrayOf(0x21, 0x43, 0xFF.toByte()), 4, "2143".toByteArray(), 2),
        )
    }

    @ParameterizedTest
    @MethodSource("testEncodeData")
    fun `should encode bytes`(data: ByteArray, expected: ByteArray) {
        encoder.encode(data) shouldBe expected
    }

    @ParameterizedTest
    @MethodSource("testDecodeData")
    fun `should decode bytes`(data: ByteArray, length: Int, expectedBytes: ByteArray, expectedRead: Int) {
        val (decoded, read) = encoder.decode(data, length)
        decoded shouldBe expectedBytes
        read shouldBe expectedRead
    }

    @Test
    fun `should fail to encode if there are invalid characters`() {
        shouldThrow<EncoderException> {
            encoder.encode(byteArrayOf(0x41, 0x42, 0x43))
        }
    }

    @Test
    fun `should fail to decode if not enough data`() {
        shouldThrow<EncoderException> {
            encoder.decode(byteArrayOf(0x21, 0x43), 6)
        }
    }

    @Test
    fun `should fail to decode if there are invalid characters`() {
        shouldThrow<EncoderException> {
            encoder.decode(byteArrayOf(0xAB.toByte(), 0xCD.toByte()), 4)
        }
    }
}