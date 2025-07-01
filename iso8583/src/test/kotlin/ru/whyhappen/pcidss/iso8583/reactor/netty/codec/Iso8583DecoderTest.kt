package ru.whyhappen.pcidss.iso8583.reactor.netty.codec

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verifySequence
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import org.junit.jupiter.api.extension.ExtendWith
import ru.whyhappen.pcidss.iso8583.IsoMessage
import ru.whyhappen.pcidss.iso8583.MessageFactory
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Tests for [Iso8583Decoder].
 */
@ExtendWith(MockKExtension::class)
class Iso8583DecoderTest {
    @MockK
    private lateinit var messageFactory: MessageFactory<IsoMessage>
    @MockK
    private lateinit var byteBuf: ByteBuf
    @MockK
    private lateinit var ctx: ChannelHandlerContext
    @MockK
    private lateinit var isoMessage: IsoMessage

    private lateinit var decoder: Iso8583Decoder

    @BeforeTest
    fun setUp() {
        decoder = Iso8583Decoder(messageFactory)
    }

    @Test
    fun `should apply the decoder`() {
        val bytes = "hello".toByteArray()
        val bytesSlot = slot<ByteArray>()

        every { byteBuf.isReadable } returns true
        every { byteBuf.readableBytes() } returns bytes.size
        every { byteBuf.readBytes(any<ByteArray>()) } answers {
            bytes.copyInto(firstArg<ByteArray>(), 0, 0, bytes.size)
            byteBuf
        }
        every { messageFactory.parseMessage(capture(bytesSlot)) } returns isoMessage

        val out = mutableListOf<Any>()
        decoder.decode(ctx, byteBuf, out)

        bytesSlot.captured shouldBe bytes
        out shouldContainExactly listOf(isoMessage)

        verifySequence {
            byteBuf.isReadable
            byteBuf.readableBytes()
            byteBuf.readBytes(any<ByteArray>())
            messageFactory.parseMessage(any<ByteArray>())
        }
    }

    @Test
    fun `shouldn't decode message if buffer isn't readable`() {
        every { byteBuf.isReadable } returns false

        val out = mutableListOf<Any>()
        decoder.decode(ctx, byteBuf, out)

        out.shouldBeEmpty()

        verifySequence {
            byteBuf.isReadable
        }
    }
}