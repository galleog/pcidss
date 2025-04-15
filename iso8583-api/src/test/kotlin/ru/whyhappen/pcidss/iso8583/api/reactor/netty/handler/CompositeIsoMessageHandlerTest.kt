package ru.whyhappen.pcidss.iso8583.api.reactor.netty.handler

import com.solab.iso8583.IsoMessage
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.extension.ExtendWith
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.netty.NettyInbound
import reactor.netty.NettyOutbound
import reactor.test.StepVerifier
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Tests for [CompositeIsoMessageHandler].
 */
@ExtendWith(MockKExtension::class)
class CompositeIsoMessageHandlerTest {
    @MockK
    private lateinit var inbound: NettyInbound
    @MockK
    private lateinit var outbound: NettyOutbound
    @MockK
    private lateinit var messageHandler1: IsoMessageHandler
    @MockK
    private lateinit var messageHandler2: IsoMessageHandler
    @MockK
    private lateinit var exceptionHandler: ExceptionHandler

    private val message = IsoMessage().apply {
        type = 0x200
    }
    private val response = IsoMessage()
    private lateinit var handler: CompositeIsoMessageHandler

    @BeforeTest
    fun setUp() {
        every { inbound.receiveObject() } returns Flux.just(message)
        every { outbound.sendObject(any<IsoMessage>()) } returns outbound
        every { outbound.then() } returns Mono.empty()

        handler = CompositeIsoMessageHandler(listOf(messageHandler1, messageHandler2), exceptionHandler)
    }

    @Test
    fun `should handle message with a suitable handler`() {
        every { messageHandler1.supports(message) } returns false
        every { messageHandler2.supports(message) } returns true
        coEvery { messageHandler2.onMessage(message) } returns response

        StepVerifier.create(handler.handle(inbound, outbound))
            .verifyComplete()

        verify { messageHandler1.supports(message) }
        verify { messageHandler2.supports(message) }

        coVerifyCount {
            0 * { messageHandler1.onMessage(any<IsoMessage>()) }
            1 * { messageHandler2.onMessage(message) }
            0 * { exceptionHandler.handleException(message, any<Throwable>()) }
        }
        verify { outbound.sendObject(response) }
    }

    @Test
    fun `should handle exception during message processing`() {
        every { messageHandler1.supports(message) } returns true
        every { messageHandler2.supports(message) } returns false

        val exception = RuntimeException()
        coEvery { messageHandler1.onMessage(message) } throws exception
        coEvery { exceptionHandler.handleException(message, exception) } returns null

        StepVerifier.create(handler.handle(inbound, outbound))
            .verifyComplete()

        coVerifyCount {
            1 * { messageHandler1.onMessage(message) }
            0 * { messageHandler2.onMessage(any<IsoMessage>()) }
            1 * { exceptionHandler.handleException(message, exception) }
        }
        verify(exactly = 0) { outbound.sendObject(any<IsoMessage>()) }
    }

    @Test
    fun `should handle case when no handler found`() {
        every { messageHandler1.supports(message) } returns false
        every { messageHandler2.supports(message) } returns false

        val exceptionSlot = slot<IsoHandlerNotFoundException>()
        coEvery { exceptionHandler.handleException(message, capture(exceptionSlot)) } returns response

        StepVerifier.create(handler.handle(inbound, outbound))
            .verifyComplete()

        coVerifyCount {
            0 * { messageHandler1.onMessage(any<IsoMessage>()) }
            0 * { messageHandler2.onMessage(any<IsoMessage>()) }
            1 * { exceptionHandler.handleException(message, any<IsoHandlerNotFoundException>()) }
        }
        verify { outbound.sendObject(response) }

        exceptionSlot.captured.isoMessage shouldBe message
        exceptionSlot.captured.message shouldBe "Message handler not found for IsoMessage[type=0x0200]"
    }
}