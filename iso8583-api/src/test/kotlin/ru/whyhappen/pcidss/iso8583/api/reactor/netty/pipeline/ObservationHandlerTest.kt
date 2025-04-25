package ru.whyhappen.pcidss.iso8583.api.reactor.netty.pipeline

import com.github.kpavlov.jreactive8583.client.ClientConfiguration
import com.github.kpavlov.jreactive8583.iso.*
import com.github.kpavlov.jreactive8583.netty.codec.Iso8583Decoder
import com.github.kpavlov.jreactive8583.netty.codec.Iso8583Encoder
import com.github.kpavlov.jreactive8583.server.ServerConfiguration
import com.solab.iso8583.IsoMessage
import com.solab.iso8583.parse.ConfigParser
import io.kotest.matchers.booleans.shouldBeTrue
import io.micrometer.tracing.test.SampleTestRunner
import io.micrometer.tracing.test.SampleTestRunner.SampleTestRunnerConsumer
import io.micrometer.tracing.test.simple.SpansAssert
import io.netty.channel.Channel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilTrue
import org.reactivestreams.Publisher
import org.springframework.test.util.TestSocketUtils
import reactor.kotlin.core.publisher.cast
import reactor.netty.*
import reactor.netty.tcp.TcpClient
import ru.whyhappen.pcidss.iso8583.api.j8583.CurrentTimeTraceNumberGenerator
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.handler.IsoMessageHandler
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.pipeline.ObservationHandler.Companion.IN_MTI_TAG
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.pipeline.ObservationHandler.Companion.OBSERVATION_NAME
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.pipeline.ObservationHandler.Companion.OUT_MTI_TAG
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.pipeline.ObservationHandler.Companion.REQUEST_PROCESSED_EVENT
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.pipeline.ObservationHandler.Companion.REQUEST_RECEIVED_EVENT
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.server.Iso8583Server
import java.net.SocketAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for [ObservationHandler].
 */
//@ExtendWith(MockKExtension::class)
class ObservationHandlerTest : SampleTestRunner() {
    private val messageFactory by lazy { messageFactory() }
    private val port = TestSocketUtils.findAvailableTcpPort()

    override fun getTracingSetup(): Array<TracingSetup> = arrayOf(TracingSetup.IN_MEMORY_OTEL)

    override fun yourCode(): SampleTestRunnerConsumer = SampleTestRunnerConsumer { bb, _ ->
        val responseReceived = AtomicBoolean(false)
        val tracePropagated = AtomicBoolean(false)

        val server = createServer(object : IsoMessageHandler {
            override fun supports(isoMessage: IsoMessage) = isoMessage.type == 0x0200

            override suspend fun onMessage(inbound: IsoMessage): IsoMessage {
                bb.tracer.currentSpan()?.run { tracePropagated.set(true) }
                return messageFactory.createResponse(inbound)
            }
        })
        server.start()

        val connection = createClient { inbound, outbound ->
            inbound.receiveObject()
                .cast<IsoMessage>()
                .subscribe { msg -> responseReceived.set(true) }

            outbound.sendObject(
                messageFactory.newMessage(
                    MessageClass.FINANCIAL,
                    MessageFunction.REQUEST,
                    MessageOrigin.ACQUIRER
                )
            ).neverComplete()
        }.connectNow()

        await atMost 5.seconds untilTrue responseReceived

        connection.disposeNow()
        server.stop()

        tracePropagated.get().shouldBeTrue()

        SpansAssert.assertThat(bb.finishedSpans)
            .hasNumberOfSpansEqualTo(1)
            .forAllSpansWithNameEqualTo(OBSERVATION_NAME) { span ->
                span.hasTag(IN_MTI_TAG, "0200")
                span.hasTag(OUT_MTI_TAG, "0210")
                span.hasEventWithNameEqualTo(REQUEST_RECEIVED_EVENT.name)
                span.hasEventWithNameEqualTo(REQUEST_PROCESSED_EVENT.name)
            }
    }

    private fun messageFactory(): MessageFactory<IsoMessage> {
        val messageFactory = ConfigParser.createDefault()
            .apply {
                traceNumberGenerator = CurrentTimeTraceNumberGenerator()
                characterEncoding = StandardCharsets.US_ASCII.name()
                useBinaryMessages = false
                assignDate = true
            }
        return J8583MessageFactory(messageFactory, ISO8583Version.V1987, MessageOrigin.ACQUIRER)
    }

    private fun createServer(isoMessageHandler: IsoMessageHandler): Iso8583Server {
        val config = ServerConfiguration.newBuilder()
            .addLoggingHandler(true)
            .build()
        return Iso8583Server(
            port,
            observationRegistry,
            config,
            messageFactory,
            listOf(isoMessageHandler)
        )
    }

    private fun createClient(handler: (NettyInbound, NettyOutbound) -> Publisher<Void>): TcpClient {
        return TcpClient.create()
            .port(port)
            .doOnChannelInit(ClientChannelInitializer(messageFactory))
            .handle { inbound, outbound -> handler(inbound, outbound) }
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