package ru.whyhappen.pcidss.iso8583.prefix

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.Test

/**
 * Tests for [AsciiVarPrefixer].
 */
class AsciiVarPrefixerTest {
    companion object {
        @JvmStatic
        private fun testEncodeData() = Stream.of(
            Arguments.of(AsciiVarPrefixer(1), 5, 3, byteArrayOf(0x33)),
            Arguments.of(AsciiVarPrefixer(2), 20, 2, byteArrayOf(0x30, 0x32)),
            Arguments.of(AsciiVarPrefixer(2), 20, 12, byteArrayOf(0x31, 0x32)),
            Arguments.of(AsciiVarPrefixer(3), 340, 2, byteArrayOf(0x30, 0x30, 0x32)),
            Arguments.of(AsciiVarPrefixer(3), 340, 200, byteArrayOf(0x32, 0x30, 0x30)),
            Arguments.of(AsciiVarPrefixer(4), 9999, 1234, byteArrayOf(0x31, 0x32, 0x33, 0x34))
        )

        @JvmStatic
        private fun testDecodeData() = Stream.of(
            Arguments.of(AsciiVarPrefixer(1), 5, 3, byteArrayOf(0x33, 0x33, 0x34)),
            Arguments.of(AsciiVarPrefixer(2), 20, 2, byteArrayOf(0x30, 0x32, 0x33, 0x34)),
            Arguments.of(AsciiVarPrefixer(2), 20, 12, byteArrayOf(0x31, 0x32, 0x33, 0x34, 0x35)),
            Arguments.of(AsciiVarPrefixer(3), 340, 2, byteArrayOf(0x30, 0x30, 0x32, 0x33, 0x34, 0x35)),
            Arguments.of(AsciiVarPrefixer(3), 340, 200, byteArrayOf(0x32, 0x30, 0x30)),
            Arguments.of(AsciiVarPrefixer(4), 9999, 1234, byteArrayOf(0x31, 0x32, 0x33, 0x34))
        )
    }

    @ParameterizedTest
    @MethodSource("testEncodeData")
    fun `should encode length`(prefixer: Prefixer, maxLen: Int, length: Int, data: ByteArray) {
        prefixer.encodeLength(maxLen, length) shouldBe data
    }

    @ParameterizedTest
    @MethodSource("testDecodeData")
    fun `should decode length`(prefixer: Prefixer, maxLen: Int, length: Int, data: ByteArray) {
        prefixer.decodeLength(maxLen, data) shouldBe (length to prefixer.digits)
    }

    @Test
    fun `should fail to encode length if the number of digits is too large`() {
        shouldThrow<PrefixerException> {
            AsciiVarPrefixer(1).encodeLength(999, 123)
        }
    }

    @Test
    fun `should fail to encode length if the field length is too large`() {
        shouldThrow<PrefixerException> {
            AsciiVarPrefixer(2).encodeLength(20, 22)
        }
    }

    @Test
    fun `should fail to decode length if the data length is too small`() {
        shouldThrow<PrefixerException> {
            AsciiVarPrefixer(3).decodeLength(20, byteArrayOf(0x32, 0x32))
        }
    }
}