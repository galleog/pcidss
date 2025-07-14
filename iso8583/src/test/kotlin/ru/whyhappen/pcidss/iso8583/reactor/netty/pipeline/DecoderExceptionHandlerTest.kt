package ru.whyhappen.pcidss.iso8583.reactor.netty.pipeline

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldHaveLength
import io.kotest.matchers.string.shouldHaveMaxLength
import io.micrometer.observation.tck.TestObservationRegistry
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.extension.ExtendWith
import reactor.kotlin.core.publisher.cast
import ru.whyhappen.pcidss.iso8583.DefaultMessageFactory
import ru.whyhappen.pcidss.iso8583.IsoMessage
import ru.whyhappen.pcidss.iso8583.MessageFactory
import ru.whyhappen.pcidss.iso8583.encode.AsciiEncoder
import ru.whyhappen.pcidss.iso8583.encode.EncoderException
import ru.whyhappen.pcidss.iso8583.encode.Encoders.ascii
import ru.whyhappen.pcidss.iso8583.encode.Encoders.binary
import ru.whyhappen.pcidss.iso8583.fields.Bitmap
import ru.whyhappen.pcidss.iso8583.fields.StringField
import ru.whyhappen.pcidss.iso8583.mti.ISO8583Version
import ru.whyhappen.pcidss.iso8583.mti.MessageClass
import ru.whyhappen.pcidss.iso8583.mti.MessageFunction
import ru.whyhappen.pcidss.iso8583.mti.MessageOrigin
import ru.whyhappen.pcidss.iso8583.prefix.Ascii
import ru.whyhappen.pcidss.iso8583.prefix.Binary
import ru.whyhappen.pcidss.iso8583.reactor.DefaultTcpTestHelper
import ru.whyhappen.pcidss.iso8583.reactor.TcpTestHelper
import ru.whyhappen.pcidss.iso8583.reactor.netty.handler.IsoMessageHandler
import ru.whyhappen.pcidss.iso8583.spec.MessageSpec
import ru.whyhappen.pcidss.iso8583.spec.Spec
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for [DecoderExceptionHandler].
 */
@ExtendWith(MockKExtension::class)
class DecoderExceptionHandlerTest : TcpTestHelper by DefaultTcpTestHelper() {
    @MockK
    private lateinit var encoder: AsciiEncoder

    private lateinit var messageFactory: MessageFactory<IsoMessage>
    private val observationRegistry = TestObservationRegistry.create()

    @BeforeTest
    fun setUp() {
        val spec = MessageSpec(
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
                        encoder,
                        Ascii.LL
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
                12 to StringField(
                    spec = Spec(
                        6,
                        "Local Transaction Time",
                        ascii,
                        Ascii.fixed
                    )
                ),
                13 to StringField(
                    spec = Spec(
                        4,
                        "Local Transaction Date",
                        ascii,
                        Ascii.fixed
                    )
                ),
                24 to StringField(
                    spec = Spec(
                        3,
                        "Function Code",
                        ascii,
                        Ascii.fixed
                    )
                ),
                44 to StringField(
                    spec = Spec(
                        99,
                        "Additional Data",
                        ascii,
                        Ascii.LL
                    )
                )
            )
        )

        messageFactory = DefaultMessageFactory(ISO8583Version.V1987, MessageOrigin.ACQUIRER, spec)
    }

    @Test
    fun `should reply with an administrative message on decoding exception`() {
        val adminMessage = AtomicReference<IsoMessage>()

        val messageHandler = object : IsoMessageHandler {
            override fun supports(isoMessage: IsoMessage): Boolean = false
            override suspend fun onMessage(inbound: IsoMessage): IsoMessage? = null
        }

        every { encoder.encode(any<ByteArray>()) } answers { callOriginal() }
        every { encoder.decode(any<ByteArray>(), any<Int>()) } throws EncoderException("test")

        val server = createServer(messageFactory, messageHandler, observationRegistry) {
            addLoggingHandler(true)
            replyOnError(true)
        }
        server.start()

        val connection = createClient(messageFactory) { inbound, outbound ->
            inbound.receiveObject()
                .cast<IsoMessage>()
                .subscribe { msg -> adminMessage.set(msg) }

            outbound.sendObject(
                messageFactory.newMessage(
                    MessageClass.FINANCIAL,
                    MessageFunction.REQUEST
                ).apply { setFieldValue(2, "12345") }
            ).neverComplete()
        }.connectNow()

        await atMost 5.seconds untilNotNull { adminMessage.get() }

        with(adminMessage.get()) {
            mti shouldBe 0x0644
            fields.keys shouldContainExactly setOf(7, 11, 12, 13, 24, 44)
            getFieldValue(7, String::class.java) shouldHaveLength 10
            getFieldValue(11, String::class.java) shouldHaveLength 6
            getFieldValue(12, String::class.java) shouldHaveLength 6
            getFieldValue(13, String::class.java) shouldHaveLength 4
            getFieldValue(24, String::class.java) shouldBe "650"

            val field44Value = getFieldValue(44, String::class.java)
            field44Value.shouldNotBeNull()
            field44Value shouldHaveMaxLength 25
        }

        connection.disposeNow()
        server.stop()
    }
}