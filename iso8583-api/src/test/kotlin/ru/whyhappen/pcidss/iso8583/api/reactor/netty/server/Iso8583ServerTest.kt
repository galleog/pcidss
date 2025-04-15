package ru.whyhappen.pcidss.iso8583.api.reactor.netty.server

import com.github.kpavlov.jreactive8583.client.ClientConfiguration
import com.github.kpavlov.jreactive8583.iso.*
import com.github.kpavlov.jreactive8583.server.ServerConfiguration
import com.solab.iso8583.IsoMessage
import com.solab.iso8583.IsoType
import com.solab.iso8583.parse.ConfigParser
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import org.springframework.test.util.TestSocketUtils
import reactor.kotlin.core.publisher.cast
import reactor.netty.Connection
import reactor.netty.tcp.TcpClient
import ru.whyhappen.pcidss.iso8583.api.j8583.CurrentTimeTraceNumberGenerator
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.Iso8583ChannelInitializer
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.handler.IsoMessageHandler
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for [Iso8583Server].
 */
class Iso8583ServerTest {
    private val messageFactory by lazy { messageFactory() }
    private val port = TestSocketUtils.findAvailableTcpPort()

    @Test
    fun `should receive an ISO message and send a response`() {
        val request = AtomicReference<IsoMessage>()
        val response = AtomicReference<IsoMessage>()

        val server = createServer(object : IsoMessageHandler {
            override fun supports(isoMessage: IsoMessage): Boolean = isoMessage.type == 0x0200

            override suspend fun onMessage(inbound: IsoMessage): IsoMessage {
                request.set(inbound)

                return messageFactory.createResponse(inbound).apply {
                    setField(39, IsoType.ALPHA.value("00", 2))
                }
            }
        })
        server.start()

        val connection: Connection = createClient { msg -> response.set(msg) }
            .connectNow()

        await atMost 5.seconds untilNotNull { response.get() }

        connection.disposeNow()
        server.stop()

        request.get().type shouldBe 0x0200
        response.get().type shouldBe 0x0210
        request.get().getObjectValue<Int>(7) shouldBeEqual response.get().getObjectValue(7)
        response.get().getObjectValue<String>(39) shouldBeEqual "00"
    }

    private fun messageFactory(): MessageFactory<IsoMessage> {
        val packagePath = this.javaClass.packageName.replace(".", "/")
        val messageFactory = ConfigParser.createFromClasspathConfig("$packagePath/j8583.xml")
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
            config,
            messageFactory,
            listOf(isoMessageHandler)
        )
    }

    private fun createClient(handler: (IsoMessage) -> Unit): TcpClient {
        val config = ClientConfiguration.newBuilder()
            .addLoggingHandler(true)
            .build()
        return TcpClient.create()
            .port(port)
            .doOnChannelInit(Iso8583ChannelInitializer(config, messageFactory))
            .handle { inbound, outbound ->
                inbound.receiveObject()
                    .cast<IsoMessage>()
                    .subscribe(handler)

                outbound.sendObject(createFinancialRequest())
                    .neverComplete()
            }
    }

    private fun createFinancialRequest(): IsoMessage = messageFactory.newMessage(
        MessageClass.FINANCIAL,
        MessageFunction.REQUEST,
        MessageOrigin.ACQUIRER
    )
}