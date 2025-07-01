package ru.whyhappen.pcidss.iso8583.reactor.netty.codec

import io.kotest.matchers.shouldBe
import io.netty.buffer.Unpooled
import java.nio.ByteOrder
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Tests for [StringLengthFieldBasedFrameDecoder].
 */
class StringLengthFieldBasedFrameDecoderTest {
    private val maxFrameLength = 8192
    private val frameLengthHeaderLength = 4
    private val frameLengthFieldOffset = 0
    private val frameLengthFieldAdjust = 0

    private lateinit var decoder: StringLengthFieldBasedFrameDecoder

    @BeforeTest
    fun setUp() {
        decoder = StringLengthFieldBasedFrameDecoder(
            maxFrameLength,
            frameLengthFieldOffset,
            frameLengthHeaderLength,
            frameLengthFieldAdjust,
            frameLengthHeaderLength
        )
    }

    @Test
    fun `should calculate unadjusted frame length`() {
        val content = "MESSAGE"
        val header = "0007"
        val buf = Unpooled.buffer()
        buf.writeBytes((header + content).toByteArray())

        val length = decoder.getUnadjustedFrameLength(
            buf,
            frameLengthFieldOffset,
            frameLengthHeaderLength,
            ByteOrder.BIG_ENDIAN
        )

        length shouldBe content.length.toLong()
    }
}