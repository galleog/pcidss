package ru.whyhappen.pcidss.iso8583.api.reactor.netty.pipeline

import com.github.kpavlov.jreactive8583.iso.MessageFactory
import com.github.kpavlov.jreactive8583.netty.codec.Iso8583Decoder
import com.github.kpavlov.jreactive8583.netty.codec.Iso8583Encoder
import com.github.kpavlov.jreactive8583.netty.pipeline.IsoMessageLoggingHandler
import com.github.kpavlov.jreactive8583.server.ServerConfiguration
import com.solab.iso8583.IsoMessage
import io.micrometer.observation.tck.TestObservationRegistry
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verifySequence
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelPipeline
import io.netty.handler.timeout.IdleStateHandler
import org.junit.jupiter.api.extension.ExtendWith
import reactor.netty.ConnectionObserver
import reactor.netty.NettyPipeline
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Tests for [Iso8583ChannelInitializer].
 */
@ExtendWith(MockKExtension::class)
class Iso8583ChannelInitializerTest {
    @MockK
    private lateinit var channel: Channel
    @MockK(relaxed = true)
    private lateinit var pipeline: ChannelPipeline
    @MockK
    private lateinit var parseExceptionChannelHandler: ChannelHandler
    @MockK
    private lateinit var idleEventChannelHandler: ChannelHandler
    @MockK
    private lateinit var messageFactory: MessageFactory<IsoMessage>
    @MockK
    private lateinit var connectionObserver: ConnectionObserver

    private lateinit var configBuilder: ServerConfiguration.Builder
    private val observationRegistry = TestObservationRegistry.create()

    @BeforeTest
    fun setUp() {
        configBuilder = ServerConfiguration.newBuilder()

        every { channel.pipeline() } returns pipeline
    }

    @Test
    fun `should initialize necessary handlers`() {
        createChannelInitializer().onChannelInit(connectionObserver, channel, null)

        verifySequence {
            channel.pipeline()
            pipeline.addBefore(NettyPipeline.ReactiveBridge, LENGTH_FIELD_FRAME_DECODER, any<ChannelHandler>())
            pipeline.addBefore(NettyPipeline.ReactiveBridge, ISO8583_DECODER, any<Iso8583Decoder>())
            pipeline.addBefore(NettyPipeline.ReactiveBridge, ISO8583_ENCODER, any<Iso8583Encoder>())
            pipeline.addBefore(NettyPipeline.ReactiveBridge, OBSERVATION_HANDLER, any<ChannelHandler>())
        }
    }

    @Test
    fun `should add a logging handler`() {
        configBuilder.addLoggingHandler(true)
        createChannelInitializer().onChannelInit(connectionObserver, channel, null)

        verifySequence {
            channel.pipeline()
            pipeline.addBefore(NettyPipeline.ReactiveBridge, LENGTH_FIELD_FRAME_DECODER, any<ChannelHandler>())
            pipeline.addBefore(NettyPipeline.ReactiveBridge, ISO8583_DECODER, any<Iso8583Decoder>())
            pipeline.addBefore(NettyPipeline.ReactiveBridge, ISO8583_ENCODER, any<Iso8583Encoder>())
            pipeline.addBefore(NettyPipeline.ReactiveBridge, OBSERVATION_HANDLER, any<ChannelHandler>())
            pipeline.addBefore(NettyPipeline.ReactiveBridge, LOGGING_HANDLER, any<IsoMessageLoggingHandler>())
        }
    }

    @Test
    fun `should add an exception handler`() {
        configBuilder.replyOnError(true)
        createChannelInitializer().onChannelInit(connectionObserver, channel, null)

        verifySequence {
            channel.pipeline()
            pipeline.addBefore(NettyPipeline.ReactiveBridge, LENGTH_FIELD_FRAME_DECODER, any<ChannelHandler>())
            pipeline.addBefore(NettyPipeline.ReactiveBridge, ISO8583_DECODER, any<Iso8583Decoder>())
            pipeline.addBefore(NettyPipeline.ReactiveBridge, ISO8583_ENCODER, any<Iso8583Encoder>())
            pipeline.addBefore(NettyPipeline.ReactiveBridge, OBSERVATION_HANDLER, any<ChannelHandler>())
            pipeline.addBefore(NettyPipeline.ReactiveBridge, REPLY_ON_ERROR_HANDLER, parseExceptionChannelHandler)
        }
    }

    @Test
    fun `should add an idle handler`() {
        configBuilder.addEchoMessageListener(true)
        createChannelInitializer().onChannelInit(connectionObserver, channel, null)

        verifySequence {
            channel.pipeline()
            pipeline.addBefore(NettyPipeline.ReactiveBridge, LENGTH_FIELD_FRAME_DECODER, any<ChannelHandler>())
            pipeline.addBefore(NettyPipeline.ReactiveBridge, ISO8583_DECODER, any<Iso8583Decoder>())
            pipeline.addBefore(NettyPipeline.ReactiveBridge, ISO8583_ENCODER, any<Iso8583Encoder>())
            pipeline.addBefore(NettyPipeline.ReactiveBridge, OBSERVATION_HANDLER, any<ChannelHandler>())
            pipeline.addBefore(NettyPipeline.ReactiveBridge, IDLE_STATE_HANDLER, any<IdleStateHandler>())
            pipeline.addAfter(IDLE_STATE_HANDLER, IDLE_EVENT_HANDLER, idleEventChannelHandler)
        }
    }

    private fun createChannelInitializer() = Iso8583ChannelInitializer(
        observationRegistry,
        configBuilder.build(),
        messageFactory,
        parseExceptionChannelHandler,
        idleEventChannelHandler
    )
}
