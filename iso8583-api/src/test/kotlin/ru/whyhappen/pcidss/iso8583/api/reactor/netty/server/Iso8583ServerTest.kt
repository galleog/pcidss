package ru.whyhappen.pcidss.iso8583.api.reactor.netty.server

import com.github.kpavlov.jreactive8583.iso.MessageClass
import com.github.kpavlov.jreactive8583.iso.MessageFunction
import com.github.kpavlov.jreactive8583.iso.MessageOrigin
import com.solab.iso8583.IsoMessage
import com.solab.iso8583.IsoType
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micrometer.observation.tck.TestObservationRegistry
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import reactor.kotlin.core.publisher.cast
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.DefaultTcpTestHelper
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.TcpTestHelper
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.handler.IsoMessageHandler
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for [Iso8583Server].
 */
class Iso8583ServerTest : TcpTestHelper by DefaultTcpTestHelper() {
    private val observationRegistry = TestObservationRegistry.create()

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
        val server = createServer(messageHandler, observationRegistry) { addLoggingHandler(true) }
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
        val server = createServer(messageHandler, observationRegistry) {
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
}
