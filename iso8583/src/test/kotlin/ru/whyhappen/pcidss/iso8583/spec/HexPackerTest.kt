package ru.whyhappen.pcidss.iso8583.spec

import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import ru.whyhappen.pcidss.iso8583.encode.Encoders.binary
import ru.whyhappen.pcidss.iso8583.prefix.Ascii
import ru.whyhappen.pcidss.iso8583.prefix.Bcd
import ru.whyhappen.pcidss.iso8583.prefix.Binary
import java.util.stream.Stream


/**
 * Tests for [HexPacker].
 */
class HexPackerTest {
    companion object {
        @JvmStatic
        private fun testPackData() = Stream.of(
            Arguments.of(
                HexPacker(4, binary, Ascii.L),
                byteArrayOf(0x23, 0xDF.toByte(), 0xCA.toByte()),
                byteArrayOf(0x36, 0x23, 0xDF.toByte(), 0xCA.toByte())
            ),
            Arguments.of(
                HexPacker(3, binary, Bcd.L),
                byteArrayOf(0xF3.toByte(), 0x1D, 0xBE.toByte()),
                byteArrayOf(0x06, 0xF3.toByte(), 0x1D, 0xBE.toByte())
            ),
            Arguments.of(
                HexPacker(1, binary, Binary.fixed),
                byteArrayOf(0x45),
                byteArrayOf(0x45)
            )
        )

        @JvmStatic
        private fun testUnpackData() = Stream.of(
            Arguments.of(
                HexPacker(4, binary, Ascii.L),
                byteArrayOf(0x36, 0x23, 0xDF.toByte(), 0xCA.toByte(), 0x27),
                byteArrayOf(0x23, 0xDF.toByte(), 0xCA.toByte()),
                4
            ),
            Arguments.of(
                HexPacker(3, binary, Bcd.L),
                byteArrayOf(0x06, 0xF3.toByte(), 0x1D, 0xBE.toByte(), 0x00),
                byteArrayOf(0xF3.toByte(), 0x1D, 0xBE.toByte()),
                4
            ),
            Arguments.of(
                HexPacker(1, binary, Binary.fixed),
                byteArrayOf(0x45),
                byteArrayOf(0x45),
                1
            )
        )
    }

    @ParameterizedTest
    @MethodSource("testPackData")
    fun `should pack data`(packer: Packer, data: ByteArray, expected: ByteArray) {
        packer.pack(data) shouldBe expected
    }

    @ParameterizedTest
    @MethodSource("testUnpackData")
    fun `should unpack data`(packer: Packer, data: ByteArray, expectedBytes: ByteArray, expectedRead: Int) {
        val (bytes, read) = packer.unpack(data)
        bytes shouldBe expectedBytes
        read shouldBe expectedRead
    }
}