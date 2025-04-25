package ru.whyhappen.pcidss.iso8583.api.reactor.netty.pipeline

import com.github.kpavlov.jreactive8583.iso.MessageClass
import com.github.kpavlov.jreactive8583.iso.MessageFactory
import com.github.kpavlov.jreactive8583.iso.MessageFunction
import com.github.kpavlov.jreactive8583.iso.MessageOrigin
import com.solab.iso8583.IsoMessage
import com.solab.iso8583.IsoType
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verifySequence
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.DecoderException
import org.junit.jupiter.api.extension.ExtendWith
import java.text.ParseException
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Tests for [ParseExceptionHandler].
 */
@Suppress("DEPRECATION")
@OptIn(ExperimentalEncodingApi::class)
@ExtendWith(MockKExtension::class)
class ParseExceptionHandlerTest {
    @MockK
    private lateinit var messageFactory: MessageFactory<IsoMessage>
    @MockK(relaxed = true)
    private lateinit var ctx: ChannelHandlerContext

    private lateinit var exceptionHandler: ParseExceptionHandler
    private val exceptionMessage = Base64.encode(Random.nextBytes(30))
    private val cause = DecoderException(ParseException(exceptionMessage, 0))

    @BeforeTest
    fun setUp() {
        exceptionHandler = ParseExceptionHandler(messageFactory)
    }

    @Test
    fun `should create an administrative message`() {
        every {
            messageFactory.newMessage(
                MessageClass.ADMINISTRATIVE,
                MessageFunction.NOTIFICATION,
                MessageOrigin.OTHER
            )
        } returns IsoMessage().apply {
            type = MESSAGE_TYPE
        }

        val messageSlot = slot<IsoMessage>()
        exceptionHandler.exceptionCaught(ctx, cause)

        verifySequence {
            messageFactory.newMessage(
                MessageClass.ADMINISTRATIVE,
                MessageFunction.NOTIFICATION,
                MessageOrigin.OTHER
            )
            ctx.writeAndFlush(capture(messageSlot))
            ctx.fireExceptionCaught(cause)
        }

        messageSlot.captured.type shouldBe MESSAGE_TYPE

        with(messageSlot.captured.getAt<Any>(24)) {
            type shouldBe IsoType.NUMERIC
            length shouldBe 3
            value shouldBe 650
        }

        with(messageSlot.captured.getAt<Any>(44)) {
            type shouldBe IsoType.LLVAR
            length shouldBe 25
            value shouldBe exceptionMessage.take(22) + "..."
        }
    }

    companion object {
        private const val MESSAGE_TYPE = 0x1644
    }
}