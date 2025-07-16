package ru.whyhappen.pcidss.iso8583.reactor.server

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldHaveLength
import io.micrometer.observation.tck.TestObservationRegistry
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import reactor.kotlin.core.publisher.cast
import ru.whyhappen.pcidss.iso8583.DefaultMessageFactory
import ru.whyhappen.pcidss.iso8583.IsoMessage
import ru.whyhappen.pcidss.iso8583.encode.Encoders.ascii
import ru.whyhappen.pcidss.iso8583.encode.Encoders.binary
import ru.whyhappen.pcidss.iso8583.fields.Bitmap
import ru.whyhappen.pcidss.iso8583.fields.DateFormats
import ru.whyhappen.pcidss.iso8583.fields.StringField
import ru.whyhappen.pcidss.iso8583.mti.ISO8583Version
import ru.whyhappen.pcidss.iso8583.mti.MessageClass
import ru.whyhappen.pcidss.iso8583.mti.MessageFunction
import ru.whyhappen.pcidss.iso8583.mti.MessageOrigin
import ru.whyhappen.pcidss.iso8583.pad.StartPadder
import ru.whyhappen.pcidss.iso8583.prefix.Ascii
import ru.whyhappen.pcidss.iso8583.prefix.Binary
import ru.whyhappen.pcidss.iso8583.reactor.DefaultTcpTestHelper
import ru.whyhappen.pcidss.iso8583.reactor.TcpTestHelper
import ru.whyhappen.pcidss.iso8583.reactor.netty.handler.IsoMessageHandler
import ru.whyhappen.pcidss.iso8583.spec.MessageSpec
import ru.whyhappen.pcidss.iso8583.spec.Spec
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for [Iso8583Server].
 */
class Iso8583ServerTest : TcpTestHelper by DefaultTcpTestHelper() {
    private val observationRegistry = TestObservationRegistry.create()
    private val messageFactory = DefaultMessageFactory(
        ISO8583Version.V1987,
        MessageOrigin.ACQUIRER,
        MessageSpec(
            mapOf(
                0 to StringField(
                    spec = Spec(
                        4,
                        "Message Type Indicator",
                        ascii,
                        Ascii.fixed
                    )
                ),
                1 to Bitmap(
                    Spec(
                        8,
                        "Bitmap",
                        binary,
                        Binary.fixed
                    )
                ),
                2 to StringField(
                    spec = Spec(
                        19,
                        "Primary Account Number",
                        ascii,
                        Ascii.LL
                    )
                ),
                4 to StringField(
                    spec = Spec(
                        12,
                        "Transaction Amount",
                        ascii,
                        Ascii.fixed,
                        StartPadder('0')
                    )
                ),
                7 to StringField(
                    spec = Spec(
                        10,
                        "Transmission Date & Time",
                        ascii,
                        Ascii.fixed
                    )
                ),
                11 to StringField(
                    spec = Spec(
                        6,
                        "Systems Trace Audit Number (STAN)",
                        ascii,
                        Ascii.fixed
                    )
                ),
                39 to StringField(
                    spec = Spec(
                        2,
                        "Response Code",
                        ascii,
                        Ascii.fixed
                    )
                ),
                70 to StringField(
                    spec = Spec(
                        3,
                        "Network management information code",
                        ascii,
                        Ascii.fixed
                    )
                )
            )
        )
    )

    @Test
    fun `should receive an ISO message and send a response`() {
        val request = AtomicReference<IsoMessage>()
        val response = AtomicReference<IsoMessage>()

        val messageHandler = object : IsoMessageHandler {
            override fun supports(isoMessage: IsoMessage): Boolean = isoMessage.mti == 0x0200

            override suspend fun onMessage(inbound: IsoMessage): IsoMessage {
                request.set(inbound)

                return messageFactory.createResponse(inbound).apply {
                    setFieldValue(39, "00")
                }
            }
        }
        val server = createServer(messageFactory, messageHandler, observationRegistry) { addLoggingHandler(true) }
        server.start()

        val connection = createClient(messageFactory) { inbound, outbound ->
            inbound.receiveObject()
                .cast<IsoMessage>()
                .subscribe { msg -> response.set(msg) }

            outbound.sendObject(createFinancialMessage())
                .neverComplete()
        }.connectNow()

        await atMost 5.seconds untilNotNull { response.get() }

        connection.disposeNow()
        server.stop()

        request.get().mti shouldBe 0x0200
        with(response.get()) {
            mti shouldBe 0x0210
            fields.keys shouldBe request.get().fields.keys + 39
            getFieldValue(2, String::class.java) shouldBe "012345678912345"
            getFieldValue(4, String::class.java) shouldBe "100"
            getFieldValue(7, String::class.java) shouldBe request.get().getFieldValue(7, String::class.java)
            getFieldValue(11, String::class.java) shouldBe request.get().getFieldValue(11, String::class.java)
            getFieldValue(39, String::class.java) shouldBe "00"
        }
    }

    @Test
    fun `should send heartbeat messages on timeout`() {
        val echoMessage = AtomicReference<IsoMessage>()

        val messageHandler = object : IsoMessageHandler {
            override fun supports(isoMessage: IsoMessage): Boolean = false
            override suspend fun onMessage(inbound: IsoMessage): IsoMessage? = null
        }
        val server = createServer(messageFactory, messageHandler, observationRegistry) {
            addLoggingHandler(true)
            addIdleEventHandler(true)
            idleTimeout(1)
        }
        server.start()

        val connection = createClient(messageFactory) { inbound, outbound ->
            inbound.receiveObject()
                .cast<IsoMessage>()
                .subscribe { msg -> echoMessage.set(msg) }

            outbound.neverComplete()
        }.connectNow()

        await atMost 7.seconds untilNotNull { echoMessage.get() }

        connection.disposeNow()
        server.stop()

        with(echoMessage.get()) {
            mti shouldBe 0x0800
            fields.keys shouldContainExactly setOf(7, 11, 70)
            getFieldValue(7, String::class.java) shouldHaveLength 10
            getFieldValue(11, String::class.java) shouldHaveLength 6
            getFieldValue(70, String::class.java) shouldBe "301"
        }
    }

    private fun createFinancialMessage(): IsoMessage =
        messageFactory.newMessage(
            MessageClass.FINANCIAL,
            MessageFunction.REQUEST
        ).apply {
            setFieldValue(2, "012345678912345")
            setFieldValue(4, "100")

            val now = LocalDateTime.now()
            setFieldValue(7, now.format(DateFormats.DATE10))
            setFieldValue(11, now.format(DateFormats.TIME))
        }
}
