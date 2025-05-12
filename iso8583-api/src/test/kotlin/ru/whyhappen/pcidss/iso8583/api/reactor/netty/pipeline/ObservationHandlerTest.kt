package ru.whyhappen.pcidss.iso8583.api.reactor.netty.pipeline

import com.github.kpavlov.jreactive8583.iso.MessageClass
import com.github.kpavlov.jreactive8583.iso.MessageFunction
import com.github.kpavlov.jreactive8583.iso.MessageOrigin
import com.solab.iso8583.IsoMessage
import io.kotest.matchers.booleans.shouldBeTrue
import io.micrometer.tracing.test.SampleTestRunner
import io.micrometer.tracing.test.SampleTestRunner.SampleTestRunnerConsumer
import io.micrometer.tracing.test.simple.SpansAssert
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilTrue
import reactor.kotlin.core.publisher.cast
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.DefaultTcpTestHelper
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.TcpTestHelper
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.handler.IsoMessageHandler
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.pipeline.ObservationHandler.Companion.IN_MTI_TAG
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.pipeline.ObservationHandler.Companion.OBSERVATION_NAME
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.pipeline.ObservationHandler.Companion.OUT_MTI_TAG
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.pipeline.ObservationHandler.Companion.REQUEST_PROCESSED_EVENT
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.pipeline.ObservationHandler.Companion.REQUEST_RECEIVED_EVENT
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for [ObservationHandler].
 */
class ObservationHandlerTest : SampleTestRunner(), TcpTestHelper by DefaultTcpTestHelper() {
    override fun getTracingSetup(): Array<TracingSetup> = arrayOf(TracingSetup.IN_MEMORY_OTEL)

    override fun yourCode(): SampleTestRunnerConsumer = SampleTestRunnerConsumer { bb, _ ->
        val responseReceived = AtomicBoolean(false)
        val tracePropagated = AtomicBoolean(false)

        val messageHandler = object : IsoMessageHandler {
            override fun supports(isoMessage: IsoMessage) = isoMessage.type == 0x0200

            override suspend fun onMessage(inbound: IsoMessage): IsoMessage {
                bb.tracer.currentSpan()?.run { tracePropagated.set(true) }
                return messageFactory.createResponse(inbound)
            }
        }
        val server = createServer(messageHandler, observationRegistry) {
            addLoggingHandler(true)
        }
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
}