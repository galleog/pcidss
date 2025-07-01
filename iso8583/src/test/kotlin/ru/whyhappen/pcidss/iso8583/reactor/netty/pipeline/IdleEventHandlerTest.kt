package ru.whyhappen.pcidss.iso8583.reactor.netty.pipeline

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldMatchEach
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldHaveLength
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyCount
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.timeout.IdleStateEvent
import org.junit.jupiter.api.extension.ExtendWith
import ru.whyhappen.pcidss.iso8583.IsoMessage
import ru.whyhappen.pcidss.iso8583.MessageFactory
import ru.whyhappen.pcidss.iso8583.mti.MessageClass
import ru.whyhappen.pcidss.iso8583.mti.MessageFunction
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Tests for [IdleEventHandler].
 */
@ExtendWith(MockKExtension::class)
class IdleEventHandlerTest {
    companion object {
        private const val MESSAGE_TYPE = 0x1800
    }

    @RelaxedMockK
    private lateinit var ctx: ChannelHandlerContext
    @MockK
    private lateinit var messageFactory: MessageFactory<IsoMessage>
    @RelaxedMockK
    private lateinit var isoMessage: IsoMessage

    private lateinit var handler: IdleEventHandler

    @BeforeTest
    fun setUp() {
        handler = IdleEventHandler(messageFactory)

        every {
            messageFactory.newMessage(
                MessageClass.NETWORK_MANAGEMENT,
                MessageFunction.REQUEST
            )
        } returns isoMessage
        every { isoMessage.mti } returns MESSAGE_TYPE
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
        val idLists = mutableListOf<Int>()
        val valueList = mutableListOf<String>()
        val messageSlot = slot<IsoMessage>()

        handler.userEventTriggered(ctx, idleStateEvent)

        verifyCount {
            1 * { messageFactory.newMessage(MessageClass.NETWORK_MANAGEMENT, MessageFunction.REQUEST) }
            5 * { isoMessage.setFieldValue(capture(idLists), capture(valueList)) }
            1 * { ctx.writeAndFlush(capture(messageSlot)) }
        }

        messageSlot.captured.mti shouldBe MESSAGE_TYPE
        idLists shouldContainExactly listOf(7, 11, 12, 13, 70)
        valueList shouldHaveSize 5
        valueList.shouldMatchEach(
            { it shouldHaveLength 10 },
            { it shouldHaveLength 6 },
            { it shouldHaveLength 6 },
            { it shouldHaveLength 4 },
            { it shouldBe "301" }
        )
    }
}