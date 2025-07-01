package ru.whyhappen.pcidss.iso8583.prefix

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.Test

/**
 * Tests for [BinaryVarPrefixer].
 */
class BinaryVarPrefixerTest {
    companion object {
        @JvmStatic
        private fun testEncodeData() = Stream.of(
            Arguments.of(BinaryVarPrefixer(1), 32, 24, byteArrayOf(0x18)),
            Arguments.of(BinaryVarPrefixer(2), 512, 256, byteArrayOf(0x01, 0x00)),
            Arguments.of(
                BinaryVarPrefixer(3),
                19999999,
                11258879,
                byteArrayOf(0xAB.toByte(), 0xCB.toByte(), 0xFF.toByte())
            ),
            Arguments.of(
                BinaryVarPrefixer(6),
                2147483647,
                40976,
                byteArrayOf(0x00, 0x00, 0x00, 0x00, 0xA0.toByte(), 0x10)
            )
        )

        @JvmStatic
        private fun testDecodeData() = Stream.of(
            Arguments.of(BinaryVarPrefixer(1), 32, 24, byteArrayOf(0x18, 0x23, 0x11, 0xAD.toByte())),
            Arguments.of(BinaryVarPrefixer(2), 512, 256, byteArrayOf(0x01, 0x00, 0x45, 0x00)),
            Arguments.of(
                BinaryVarPrefixer(3),
                19999999,
                11258879,
                byteArrayOf(0xAB.toByte(), 0xCB.toByte(), 0xFF.toByte(), 0x00)
            ),
            Arguments.of(
                BinaryVarPrefixer(6),
                2147483647,
                40976,
                byteArrayOf(0x00, 0x00, 0x00, 0x00, 0xA0.toByte(), 0x10, 0xAB.toByte(), 0xCB.toByte(), 0xFF.toByte())
            )
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
        prefixer.decodeLength(maxLen, data) shouldBe (length to prefixer.bytesSize)
    }

    @Test
    fun `should fail to encode length if data length exceeds maximum`() {
        shouldThrow<PrefixerException> {
            BinaryVarPrefixer(1).encodeLength(16, 24)
        }
    }

    @Test
    fun `should fail to decode length if data length exceeds maximal possible length`() {
        shouldThrow<PrefixerException> {
            BinaryVarPrefixer(1).encodeLength(512, 256)
        }
    }

    @Test
    fun `should fail to decode length if not enough data to decode`() {
        shouldThrow<PrefixerException> {
            BinaryVarPrefixer(3).decodeLength(32, byteArrayOf(0x00))
        }
    }

    @Test
    fun `should fail to decode length if data length exceeds maximum`() {
        shouldThrow<PrefixerException> {
            BinaryVarPrefixer(1).decodeLength(8, byteArrayOf(0x18))
        }
    }

    @Test
    fun `should fail to decode length if encoded length exceeds 4 bytes`() {
        shouldThrow<PrefixerException> {
            BinaryVarPrefixer(5).decodeLength(Int.MAX_VALUE, byteArrayOf(0x01, 0x00, 0x00, 0x00, 0x00))
        }
    }
}