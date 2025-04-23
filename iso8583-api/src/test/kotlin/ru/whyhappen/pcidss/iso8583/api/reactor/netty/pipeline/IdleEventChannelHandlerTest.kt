package ru.whyhappen.pcidss.iso8583.api.reactor.netty.pipeline

import com.github.kpavlov.jreactive8583.iso.MessageClass
import com.github.kpavlov.jreactive8583.iso.MessageFactory
import com.github.kpavlov.jreactive8583.iso.MessageFunction
import com.solab.iso8583.IsoMessage
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifySequence
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.timeout.IdleStateEvent
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Tests for [IdleEventChannelHandler].
 */
@ExtendWith(MockKExtension::class)
class IdleEventChannelHandlerTest {
    @MockK(relaxed = true)
    private lateinit var ctx: ChannelHandlerContext
    @MockK
    private lateinit var messageFactory: MessageFactory<IsoMessage>

    private lateinit var handler: IdleEventChannelHandler

    @BeforeTest
    fun setUp() {
        handler = IdleEventChannelHandler(messageFactory)

        every {
            messageFactory.newMessage(
                MessageClass.NETWORK_MANAGEMENT,
                MessageFunction.REQUEST
            )
        } returns IsoMessage().apply {
            type = MESSAGE_TYPE
        }
    }

    @Test
    fun `should send an idle message on IdleState#READER_IDLE`() {
        testSendIdleMessage(IdleStateEvent.READER_IDLE_STATE_EVENT)
    }

    @Test
    fun `should send an idle message on IdleState#ALL_IDLE`() {
        testSendIdleMessage(IdleStateEvent.ALL_IDLE_STATE_EVENT)
    }

    @Test
    fun `should send no idle message on IdleState#WRITEER_IDLE`() {
        handler.userEventTriggered(ctx, IdleStateEvent.WRITER_IDLE_STATE_EVENT)

        verify(exactly = 0) { ctx.writeAndFlush(any()) }
    }

    private fun testSendIdleMessage(idleStateEvent: IdleStateEvent) {
        every {
            messageFactory.newMessage(
                MessageClass.NETWORK_MANAGEMENT,
                MessageFunction.REQUEST
            )
        } returns IsoMessage().apply {
            type = MESSAGE_TYPE
        }

        val messageSlot = slot<IsoMessage>()
        handler.userEventTriggered(ctx, idleStateEvent)

        verifySequence {
            messageFactory.newMessage(MessageClass.NETWORK_MANAGEMENT, MessageFunction.REQUEST)
            ctx.writeAndFlush(capture(messageSlot))
        }

        messageSlot.captured.type shouldBe MESSAGE_TYPE
    }

    companion object {
        private const val MESSAGE_TYPE = 0x1800
    }
}