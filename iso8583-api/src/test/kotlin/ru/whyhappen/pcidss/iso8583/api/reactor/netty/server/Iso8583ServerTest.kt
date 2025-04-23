package ru.whyhappen.pcidss.iso8583.api.reactor.netty.server

import com.github.kpavlov.jreactive8583.client.ClientConfiguration
import com.github.kpavlov.jreactive8583.iso.*
import com.github.kpavlov.jreactive8583.server.ServerConfiguration
import com.solab.iso8583.IsoMessage
import com.solab.iso8583.IsoType
import com.solab.iso8583.parse.ConfigParser
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micrometer.observation.tck.TestObservationRegistry
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import org.reactivestreams.Publisher
import org.springframework.test.util.TestSocketUtils
import reactor.kotlin.core.publisher.cast
import reactor.netty.NettyInbound
import reactor.netty.NettyOutbound
import reactor.netty.tcp.TcpClient
import ru.whyhappen.pcidss.iso8583.api.j8583.CurrentTimeTraceNumberGenerator
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.handler.IsoMessageHandler
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.pipeline.Iso8583ChannelInitializer
import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for [Iso8583Server].
 */
class Iso8583ServerTest {
    private val observationRegistry = TestObservationRegistry.create()
    private val messageFactory by lazy { messageFactory() }
    private val port = TestSocketUtils.findAvailableTcpPort()

    @Test
    fun `should receive an ISO message and send a response`() {
        val request = AtomicReference<IsoMessage>()
        val response = AtomicReference<IsoMessage>()

        val messageHandler = object : IsoMessageHandler {
            override fun supports(isoMessage: IsoMessage): Boolean = isoMessage.type == 0x0200

            override suspend fun onMessage(inbound: IsoMessage): IsoMessage {
                request.set(inbound)

                return messageFactory.createResponse(inbound).apply {
                    setField(39, IsoType.ALPHA.value("00", 2))
                }
            }
        }
        val server = createServer(messageHandler) { addLoggingHandler(true) }
        server.start()

        val connection = createClient { inbound, outbound ->
            inbound.receiveObject()
                .cast<IsoMessage>()
                .subscribe { msg -> response.set(msg) }

            outbound.sendObject(
                messageFactory.newMessage(
                    MessageClass.FINANCIAL,
                    MessageFunction.REQUEST,
                    MessageOrigin.ACQUIRER
                )
            ).neverComplete()
        }.connectNow()

        await atMost 5.seconds untilNotNull { response.get() }

        connection.disposeNow()
        server.stop()

        request.get().type shouldBe 0x0200
        with(response.get()) {
            type shouldBe 0x0210
            getObjectValue<String>(11) shouldBe request.get().getObjectValue(11)
            getObjectValue<String>(39) shouldBe "00"
        }
    }

    @Test
    fun `should send heartbeat messages on timeout`() {
        val echoMessage = AtomicReference<IsoMessage>()

        val messageHandler = object : IsoMessageHandler {
            override fun supports(isoMessage: IsoMessage): Boolean = false
            override suspend fun onMessage(inbound: IsoMessage): IsoMessage? = null
        }
        val server = createServer(messageHandler) {
            addLoggingHandler(true)
            addEchoMessageListener(true)
            idleTimeout(1)
        }
        server.start()

        val connection = createClient { inbound, outbound ->
            inbound.receiveObject()
                .cast<IsoMessage>()
                .subscribe { msg -> echoMessage.set(msg) }

            outbound.neverComplete()
        }.connectNow()

        await atMost 7.seconds untilNotNull { echoMessage.get() }

        connection.disposeNow()
        server.stop()

        with(echoMessage.get()) {
            type shouldBe 0x0800
            getObjectValue<String>(11).shouldNotBeNull()
        }
    }

    @Test
    fun `should reply with an administrative message on ParseException`() {
        val adminMessage = AtomicReference<IsoMessage>()

        val messageHandler = object : IsoMessageHandler {
            override fun supports(isoMessage: IsoMessage): Boolean = false
            override suspend fun onMessage(inbound: IsoMessage): IsoMessage? = null
        }
        val server = createServer(messageHandler) {
            addLoggingHandler(true)
            replyOnError(true)
        }
        server.start()

        val connection = createClient { inbound, outbound ->
            inbound.receiveObject()
                .cast<IsoMessage>()
                .subscribe { msg -> adminMessage.set(msg) }

            outbound.sendObject(
                messageFactory.newMessage(
                    MessageClass.FINANCIAL,
                    MessageFunction.RESERVED_8,
                    MessageOrigin.ACQUIRER
                )
            ).neverComplete()
        }.connectNow()

        await atMost 5.seconds untilNotNull { adminMessage.get() }

        with(adminMessage.get()) {
            type shouldBe 0x0644
            getObjectValue<ZonedDateTime>(7).shouldNotBeNull()
            getObjectValue<String>(11).shouldNotBeNull()
            getObjectValue<String>(24) shouldBe "650"
            getObjectValue<String>(44).shouldNotBeNull()
        }

        connection.disposeNow()
        server.stop()
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

    private fun createServer(
        isoMessageHandler: IsoMessageHandler,
        configureServer: ServerConfiguration.Builder.() -> Unit = {}
    ): Iso8583Server {
        val config = ServerConfiguration.newBuilder()
            .apply(configureServer)
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
        val config = ClientConfiguration.newBuilder()
            .addLoggingHandler(true)
            .build()
        return TcpClient.create()
            .port(port)
            .doOnChannelInit(Iso8583ChannelInitializer(observationRegistry, config, messageFactory))
            .handle { inbound, outbound -> handler(inbound, outbound) }
    }
}
