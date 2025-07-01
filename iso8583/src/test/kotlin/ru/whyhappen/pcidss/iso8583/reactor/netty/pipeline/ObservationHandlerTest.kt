package ru.whyhappen.pcidss.iso8583.reactor.netty.pipeline

import io.kotest.matchers.booleans.shouldBeTrue
import io.micrometer.tracing.test.SampleTestRunner
import io.micrometer.tracing.test.simple.SpansAssert
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilTrue
import reactor.kotlin.core.publisher.cast
import ru.whyhappen.pcidss.iso8583.DefaultMessageFactory
import ru.whyhappen.pcidss.iso8583.IsoMessage
import ru.whyhappen.pcidss.iso8583.encode.AsciiEncoder
import ru.whyhappen.pcidss.iso8583.encode.BinaryEncoder
import ru.whyhappen.pcidss.iso8583.fields.Bitmap
import ru.whyhappen.pcidss.iso8583.fields.StringField
import ru.whyhappen.pcidss.iso8583.mti.ISO8583Version
import ru.whyhappen.pcidss.iso8583.mti.MessageClass
import ru.whyhappen.pcidss.iso8583.mti.MessageFunction
import ru.whyhappen.pcidss.iso8583.mti.MessageOrigin
import ru.whyhappen.pcidss.iso8583.prefix.AsciiFixedPrefixer
import ru.whyhappen.pcidss.iso8583.prefix.BinaryFixedPrefixer
import ru.whyhappen.pcidss.iso8583.reactor.DefaultTcpTestHelper
import ru.whyhappen.pcidss.iso8583.reactor.TcpTestHelper
import ru.whyhappen.pcidss.iso8583.reactor.netty.handler.IsoMessageHandler
import ru.whyhappen.pcidss.iso8583.reactor.netty.pipeline.ObservationHandler.Companion.IN_MTI_TAG
import ru.whyhappen.pcidss.iso8583.reactor.netty.pipeline.ObservationHandler.Companion.OBSERVATION_NAME
import ru.whyhappen.pcidss.iso8583.reactor.netty.pipeline.ObservationHandler.Companion.OUT_MTI_TAG
import ru.whyhappen.pcidss.iso8583.reactor.netty.pipeline.ObservationHandler.Companion.REQUEST_PROCESSED_EVENT
import ru.whyhappen.pcidss.iso8583.reactor.netty.pipeline.ObservationHandler.Companion.REQUEST_RECEIVED_EVENT
import ru.whyhappen.pcidss.iso8583.spec.MessageSpec
import ru.whyhappen.pcidss.iso8583.spec.Spec
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for [ObservationHandler].
 */
class ObservationHandlerTest : SampleTestRunner(), TcpTestHelper by DefaultTcpTestHelper() {
    private val messageFactory = DefaultMessageFactory(
        ISO8583Version.V1987,
        MessageOrigin.ACQUIRER,
        MessageSpec(
            mapOf(
                0 to StringField(
                    spec = Spec(
                        4,
                        "Message Type Indicator",
                        AsciiEncoder(),
                        AsciiFixedPrefixer()
                    )
                ),
                1 to Bitmap(
                    Spec(
                        8,
                        "Bitmap",
                        BinaryEncoder(),
                        BinaryFixedPrefixer()
                    )
                )
            )
        )
    )

    override fun getTracingSetup(): Array<TracingSetup> = arrayOf(TracingSetup.IN_MEMORY_OTEL)

    override fun yourCode(): SampleTestRunnerConsumer = SampleTestRunnerConsumer { bb, _ ->
        val responseReceived = AtomicBoolean(false)
        val tracePropagated = AtomicBoolean(false)

        val messageHandler = object : IsoMessageHandler {
            override fun supports(isoMessage: IsoMessage) = isoMessage.mti == 0x0200

            override suspend fun onMessage(inbound: IsoMessage): IsoMessage {
                bb.tracer.currentSpan()?.run { tracePropagated.set(true) }
                return messageFactory.createResponse(inbound)
            }
        }
        val server = createServer(messageFactory, messageHandler, observationRegistry) {
            addLoggingHandler(true)
        }
        server.start()

        val connection = createClient(messageFactory) { inbound, outbound ->
            inbound.receiveObject()
                .cast<IsoMessage>()
                .subscribe { msg -> responseReceived.set(true) }

            outbound.sendObject(
                messageFactory.newMessage(
                    MessageClass.FINANCIAL,
                    MessageFunction.REQUEST
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
}