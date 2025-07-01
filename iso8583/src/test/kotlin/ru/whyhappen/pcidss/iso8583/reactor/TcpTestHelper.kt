package ru.whyhappen.pcidss.iso8583.reactor

import io.micrometer.observation.ObservationRegistry
import io.netty.channel.Channel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import org.reactivestreams.Publisher
import org.springframework.test.util.TestSocketUtils
import reactor.netty.*
import reactor.netty.tcp.TcpClient
import ru.whyhappen.pcidss.iso8583.IsoMessage
import ru.whyhappen.pcidss.iso8583.MessageFactory
import ru.whyhappen.pcidss.iso8583.reactor.client.ClientConfiguration
import ru.whyhappen.pcidss.iso8583.reactor.netty.codec.Iso8583Decoder
import ru.whyhappen.pcidss.iso8583.reactor.netty.codec.Iso8583Encoder
import ru.whyhappen.pcidss.iso8583.reactor.netty.handler.IsoMessageHandler
import ru.whyhappen.pcidss.iso8583.reactor.netty.pipeline.ISO8583_DECODER
import ru.whyhappen.pcidss.iso8583.reactor.netty.pipeline.ISO8583_ENCODER
import ru.whyhappen.pcidss.iso8583.reactor.netty.pipeline.LENGTH_FIELD_FRAME_DECODER
import ru.whyhappen.pcidss.iso8583.reactor.server.Iso8583Server
import ru.whyhappen.pcidss.iso8583.reactor.server.ServerConfiguration
import java.net.SocketAddress

/**
 * Helper for tests that uses [Iso8583Server] and [TcpClient].
 */
interface TcpTestHelper {
    val port: Int

    fun createClient(
        messageFactory: MessageFactory<IsoMessage>,
        handler: (NettyInbound, NettyOutbound) -> Publisher<Void>
    ): TcpClient

    fun createServer(
        messageFactory: MessageFactory<IsoMessage>,
        isoMessageHandler: IsoMessageHandler,
        observationRegistry: ObservationRegistry,
        configureServer: ServerConfiguration.Builder.() -> Unit = {}
    ): Iso8583Server
}

/**
 * [TcpTestHelper] implementation.
 */
class DefaultTcpTestHelper : TcpTestHelper {
    override val port = TestSocketUtils.findAvailableTcpPort()

    override fun createClient(
        messageFactory: MessageFactory<IsoMessage>,
        handler: (NettyInbound, NettyOutbound) -> Publisher<Void>
    ): TcpClient {
        return TcpClient.create()
            .port(port)
            .doOnChannelInit(ClientChannelInitializer(messageFactory))
            .handle { inbound, outbound -> handler(inbound, outbound) }
    }

    override fun createServer(
        messageFactory: MessageFactory<IsoMessage>,
        isoMessageHandler: IsoMessageHandler,
        observationRegistry: ObservationRegistry,
        configureServer: ServerConfiguration.Builder.() -> Unit
    ): Iso8583Server {
        val config = ServerConfiguration.newBuilder()
            .apply(configureServer)
            .build()
        return Iso8583Server(
            port,
            config,
            messageFactory,
            listOf(isoMessageHandler),
            observationRegistry
        )
    }

    class ClientChannelInitializer(
        private val isoMessageFactory: MessageFactory<IsoMessage>
    ) : ChannelPipelineConfigurer {
        override fun onChannelInit(
            connectionObserver: ConnectionObserver,
            channel: Channel,
            remoteAddress: SocketAddress?
        ) {
            val configuration = ClientConfiguration.newBuilder().build()
            val baseName = NettyPipeline.ReactiveBridge
            channel.pipeline()
                .addBefore(baseName, LENGTH_FIELD_FRAME_DECODER, createLengthFieldBasedFrameDecoder(configuration))
                .addBefore(baseName, ISO8583_DECODER, Iso8583Decoder(isoMessageFactory))
                .addBefore(baseName, ISO8583_ENCODER, createIso8583Encoder(configuration))
        }

        private fun createLengthFieldBasedFrameDecoder(configuration: ClientConfiguration) =
            LengthFieldBasedFrameDecoder(
                configuration.maxFrameLength,
                configuration.frameLengthFieldOffset,
                configuration.frameLengthFieldLength,
                configuration.frameLengthFieldAdjust,
                configuration.frameLengthFieldLength
            )

        private fun createIso8583Encoder(configuration: ClientConfiguration) =
            Iso8583Encoder(
                configuration.frameLengthFieldLength,
                configuration.encodeFrameLengthAsString()
            )
    }
}