package ru.whyhappen.pcidss.iso8583.api.reactor.netty.pipeline

import com.github.kpavlov.jreactive8583.iso.MessageClass
import com.github.kpavlov.jreactive8583.iso.MessageFunction
import com.github.kpavlov.jreactive8583.iso.MessageOrigin
import com.solab.iso8583.IsoMessage
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldHaveMaxLength
import io.micrometer.observation.tck.TestObservationRegistry
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import reactor.kotlin.core.publisher.cast
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.DefaultHelperIT
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.HelperIT
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.handler.IsoMessageHandler
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for [ParseExceptionHandler].
 */
class ParseExceptionHandlerTest : HelperIT by DefaultHelperIT() {
    private val observationRegistry = TestObservationRegistry.create()

    @Test
    fun `should reply with an administrative message on ParseException`() {
        val adminMessage = AtomicReference<IsoMessage>()

        val messageHandler = object : IsoMessageHandler {
            override fun supports(isoMessage: IsoMessage): Boolean = false
            override suspend fun onMessage(inbound: IsoMessage): IsoMessage? = null
        }
        val server = createServer(messageHandler, observationRegistry) {
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

            val field44Value = getObjectValue<String>(44)
            field44Value.shouldNotBeNull()
            field44Value shouldHaveMaxLength 25
        }

        connection.disposeNow()
        server.stop()
    }
}