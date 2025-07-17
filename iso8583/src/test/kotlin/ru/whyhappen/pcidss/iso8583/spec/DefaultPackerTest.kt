package ru.whyhappen.pcidss.iso8583.spec

import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import ru.whyhappen.pcidss.iso8583.encode.Encoders.ascii
import ru.whyhappen.pcidss.iso8583.encode.Encoders.bcd
import ru.whyhappen.pcidss.iso8583.encode.Encoders.binary
import ru.whyhappen.pcidss.iso8583.pad.StartPadder
import ru.whyhappen.pcidss.iso8583.prefix.Ascii
import ru.whyhappen.pcidss.iso8583.prefix.Bcd
import ru.whyhappen.pcidss.iso8583.prefix.Binary
import java.util.stream.Stream

/**
 * Tests for [DefaultPacker].
 */
class DefaultPackerTest {
    companion object {
        @JvmStatic
        private fun testPackData() = Stream.of(
            Arguments.of(
                DefaultPacker(4, ascii, Ascii.L),
                "123".toByteArray(),
                "3123".toByteArray()
            ),
            Arguments.of(
                DefaultPacker(4, ascii, Ascii.fixed, StartPadder('0')),
                "456".toByteArray(),
                "0456".toByteArray()
            ),
            Arguments.of(
                DefaultPacker(4, binary, Binary.L),
                byteArrayOf(0x23, 0x72),
                byteArrayOf(0x02, 0x23, 0x72)
            ),
            Arguments.of(
                DefaultPacker(3, bcd, Bcd.L),
                "123".toByteArray(),
                byteArrayOf(0x03, 0x01, 0x23)
            ),
            Arguments.of(
                DefaultPacker(4, bcd, Bcd.L),
                "1234".toByteArray(),
                byteArrayOf(0x04, 0x12, 0x34)
            )
        )

        @JvmStatic
        private fun testUnpackData() = Stream.of(
            Arguments.of(
                DefaultPacker(4, ascii, Ascii.L),
                "31235".toByteArray(),
                "123".toByteArray(),
                4
            ),
            Arguments.of(
                DefaultPacker(4, ascii, Ascii.fixed, StartPadder('0')),
                "0456".toByteArray(),
                "456".toByteArray(),
                4
            ),
            Arguments.of(
                DefaultPacker(4, binary, Binary.L),
                byteArrayOf(0x02, 0x23, 0x72, 0xFF.toByte()),
                byteArrayOf(0x23, 0x72),
                3
            ),
            Arguments.of(
                DefaultPacker(3, bcd, Bcd.L),
                byteArrayOf(0x03, 0x01, 0x23),
                "123".toByteArray(),
                3
            ),
            Arguments.of(
                DefaultPacker(4, bcd, Bcd.L),
                byteArrayOf(0x04, 0x12, 0x34),
                "1234".toByteArray(),
                3
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
    fun `should unpack bytes`(packer: Packer, data: ByteArray, expectedBytes: ByteArray, expectedRead: Int) {
        val (bytes, read) = packer.unpack(data)
        bytes shouldBe expectedBytes
        read shouldBe expectedRead
    }
}