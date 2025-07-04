package ru.whyhappen.pcidss.iso8583.prefix

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.Test

/**
 * Tests for [BcdVarPrefixer].
 */
class BcdVarPrefixerTest {
    companion object {
        @JvmStatic
        private fun testData() = Stream.of(
            Arguments.of(BcdVarPrefixer(1), 1, 5, 3, byteArrayOf(0x03)),
            Arguments.of(BcdVarPrefixer(2), 1, 20, 2, byteArrayOf(0x02)),
            Arguments.of(BcdVarPrefixer(2), 1, 20, 12, byteArrayOf(0x12)),
            Arguments.of(BcdVarPrefixer(3), 2, 340, 2, byteArrayOf(0x00, 0x02)),
            Arguments.of(BcdVarPrefixer(3), 2, 340, 200, byteArrayOf(0x02, 0x00)),
            Arguments.of(BcdVarPrefixer(4), 2, 9999, 1234, byteArrayOf(0x12, 0x34))
        )
    }

    @ParameterizedTest
    @MethodSource("testData")
    fun `should encode length`(prefixer: Prefixer, bytesRead: Int, maxLen: Int, dataLen: Int, data: ByteArray) {
        prefixer.encodeLength(maxLen, dataLen) shouldBe data
    }

    @ParameterizedTest
    @MethodSource("testData")
    fun `should decode length`(prefixer: Prefixer, bytesRead: Int, maxLen: Int, dataLen: Int, data: ByteArray) {
        prefixer.decodeLength(maxLen, data) shouldBe (dataLen to bytesRead)
    }

    @Test
    fun `should fail if dataLen exceeds maxLen`() {
        shouldThrow<PrefixerException> {
            BcdVarPrefixer(2).encodeLength(20, 22)
        }
    }

    @Test
    fun `should fail if number of digits in dataLen exceeds number of digits in prefixer`() {
        shouldThrow<PrefixerException> {
            BcdVarPrefixer(2).encodeLength(999, 123)
        }
    }

    @Test
    fun `should fail if not enough data to read length prefix`() {
        shouldThrow<PrefixerException> {
            BcdVarPrefixer(3).decodeLength(20, byteArrayOf(0x22))
        }
    }
}