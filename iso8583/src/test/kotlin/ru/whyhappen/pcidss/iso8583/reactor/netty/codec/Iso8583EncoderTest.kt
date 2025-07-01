package ru.whyhappen.pcidss.iso8583.reactor.netty.codec

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import org.junit.jupiter.api.extension.ExtendWith
import ru.whyhappen.pcidss.iso8583.IsoMessage
import kotlin.test.Test

/**
 * Tests for [Iso8583Encoder].
 */
@ExtendWith(MockKExtension::class)
class Iso8583EncoderTest {
    @MockK
    private lateinit var byteBuf: ByteBuf
    @MockK
    private lateinit var ctx: ChannelHandlerContext
    @MockK
    private lateinit var isoMessage: IsoMessage

    @Test
    fun `should encode message when header length is 0`() {
        val bytes = "hello".toByteArray()
        val encoder = Iso8583Encoder(0, false)
        val bytesSlot = slot<ByteArray>()

        every { isoMessage.pack() } returns bytes
        every { byteBuf.writeBytes(capture(bytesSlot)) } returns byteBuf

        encoder.encode(ctx, isoMessage, byteBuf)

        bytesSlot.captured shouldBe bytes

        verifySequence {
            isoMessage.pack()
            byteBuf.writeBytes(any<ByteArray>())
        }
    }

    @Test
    fun `should encode message when header length is encoded as string`() {
        val bytes = "hello".toByteArray()
        val encoder = Iso8583Encoder(4, true)
        val captureList = mutableListOf<ByteArray>()

        every { isoMessage.pack() } returns bytes
        every { byteBuf.writeBytes(capture(captureList)) } returns byteBuf

        encoder.encode(ctx, isoMessage, byteBuf)

        captureList shouldHaveSize 2
        captureList[0] shouldBe "0005".toByteArray()
        captureList[1] shouldBe bytes

        verifyCount {
            1 * { isoMessage.pack() }
            2 * { byteBuf.writeBytes(any<ByteArray>()) }
        }
    }

    @Test
    fun `should encode message when header length is binary encoded`() {
        val bytes = "hello".toByteArray()
        val encoder = Iso8583Encoder(2, false)
        val captureList = mutableListOf<ByteArray>()

        every { isoMessage.pack() } returns bytes
        every { byteBuf.writeBytes(capture(captureList)) } returns byteBuf

        encoder.encode(ctx, isoMessage, byteBuf)

        captureList shouldHaveSize 2
        captureList[0] shouldBe byteArrayOf(0, 0x05)
        captureList[1] shouldBe bytes

        verifyCount {
            1 * { isoMessage.pack() }
            2 * { byteBuf.writeBytes(any<ByteArray>()) }
        }
    }
}