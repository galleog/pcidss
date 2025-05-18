package ru.whyhappen.pcidss.iso8583.api.reactor.netty

import com.github.kpavlov.jreactive8583.client.ClientConfiguration
import com.github.kpavlov.jreactive8583.iso.ISO8583Version
import com.github.kpavlov.jreactive8583.iso.J8583MessageFactory
import com.github.kpavlov.jreactive8583.iso.MessageFactory
import com.github.kpavlov.jreactive8583.iso.MessageOrigin
import com.github.kpavlov.jreactive8583.netty.codec.Iso8583Decoder
import com.github.kpavlov.jreactive8583.netty.codec.Iso8583Encoder
import com.github.kpavlov.jreactive8583.server.ServerConfiguration
import com.solab.iso8583.IsoMessage
import com.solab.iso8583.parse.ConfigParser
import io.micrometer.observation.ObservationRegistry
import io.netty.channel.Channel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import org.reactivestreams.Publisher
import org.springframework.test.util.TestSocketUtils
import reactor.netty.*
import reactor.netty.tcp.TcpClient
import ru.whyhappen.pcidss.iso8583.api.j8583.CurrentTimeTraceNumberGenerator
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.handler.IsoMessageHandler
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.pipeline.ISO8583_DECODER
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.pipeline.ISO8583_ENCODER
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.pipeline.LENGTH_FIELD_FRAME_DECODER
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.server.Iso8583Server
import java.net.SocketAddress
import java.nio.charset.StandardCharsets

/**
 * Helper for tests that uses [Iso8583Server] and [TcpClient].
 */
interface TcpTestHelper {
    val port: Int
    val messageFactory: MessageFactory<IsoMessage>

    fun createClient(handler: (NettyInbound, NettyOutbound) -> Publisher<Void>): TcpClient
    fun createServer(
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
    override val messageFactory by lazy { messageFactory() }

    override fun createClient(handler: (NettyInbound, NettyOutbound) -> Publisher<Void>): TcpClient {
        return TcpClient.create()
            .port(port)
            .doOnChannelInit(ClientChannelInitializer(messageFactory))
            .handle { inbound, outbound -> handler(inbound, outbound) }
    }

    override fun createServer(
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

    private fun messageFactory(): MessageFactory<IsoMessage> {
        val messageFactory = ConfigParser.createDefault()
            .apply {
                traceNumberGenerator = CurrentTimeTraceNumberGenerator()
                characterEncoding = StandardCharsets.US_ASCII.name()
                useBinaryMessages = false
                isUseDateTimeApi = true
                assignDate = true
            }
        return J8583MessageFactory(messageFactory, ISO8583Version.V1987, MessageOrigin.ACQUIRER)
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