package ru.whyhappen.pcidss.iso8583.utils

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Tests for [Bcd].
 */
class BcdTest {
    companion object {
        @JvmStatic
        private fun testEncodeString() = Stream.of(
            Arguments.of("123", byteArrayOf(0x01, 0x23)),
            Arguments.of("00456", byteArrayOf(0x00, 0x04, 0x56)),
            Arguments.of("2143", byteArrayOf(0x21, 0x43)),
            Arguments.of("0", byteArrayOf(0x00))
        )

        @JvmStatic
        private fun testEncodeInt() = Stream.of(
            Arguments.of(1234, byteArrayOf(0x12, 0x34)),
            Arguments.of(1, byteArrayOf(0x01)),
            Arguments.of(0, byteArrayOf(0x00)),
            Arguments.of(298, byteArrayOf(0x02, 0x98.toByte()))
        )

        @JvmStatic
        private fun testDecodeToString() = Stream.of(
            Arguments.of(byteArrayOf(0x33), "33"),
            Arguments.of(byteArrayOf(0x00, 0x00), "0000"),
            Arguments.of(byteArrayOf(0x00, 0x47), "0047"),
            Arguments.of(byteArrayOf(0x50, 0x00), "5000"),
            Arguments.of(byteArrayOf(0x79, 0x02), "7902"),
        )

        @JvmStatic
        private fun testDecodeToInt() = Stream.of(
            Arguments.of(byteArrayOf(0x12, 0x34), 1234),
            Arguments.of(byteArrayOf(0x00, 0x00), 0),
            Arguments.of(byteArrayOf(0x00, 0x01), 1),
            Arguments.of(byteArrayOf(0x79, 0x05), 7905)
        )
    }

    @ParameterizedTest
    @MethodSource("testEncodeString")
    fun `should encode decimal string to BCD byte array`(decimal: String, encoded: ByteArray) {
        decimal.toBcd() shouldBe encoded
    }

    @ParameterizedTest
    @MethodSource("testEncodeInt")
    fun `should encode int to BCD byte array`(num: Int, encoded: ByteArray) {
        num.toBcd() shouldBe encoded
    }

    @Test
    fun `should fail if string contains not only digits`() {
        shouldThrow<IllegalArgumentException> {
            "1b3".toBcd()
        }
    }

    @ParameterizedTest
    @MethodSource("testDecodeToString")
    fun `should decode BCD byte array to decimal string`(encoded: ByteArray, decimal: String) {
        encoded.fromBcd() shouldBe decimal
    }

    @ParameterizedTest
    @MethodSource("testDecodeToInt")
    fun `should decode BCD byte array to int`(encoded: ByteArray, num: Int) {
        encoded.fromBcdToInt() shouldBe num
    }

    @Test
    fun `should fail if bytes contains not only digits`() {
        shouldThrow<IllegalArgumentException> {
            byteArrayOf(0x1A).fromBcd()
        }
    }
}