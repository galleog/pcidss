package ru.whyhappen.pcidss

import com.redis.testcontainers.RedisContainer
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.netty.channel.Channel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.reactivestreams.Publisher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.util.TestSocketUtils
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.cast
import reactor.netty.*
import reactor.netty.tcp.TcpClient
import ru.whyhappen.pcidss.iso8583.IsoMessage
import ru.whyhappen.pcidss.iso8583.MessageFactory
import ru.whyhappen.pcidss.iso8583.reactor.client.ClientConfiguration
import ru.whyhappen.pcidss.iso8583.reactor.netty.codec.Iso8583Decoder
import ru.whyhappen.pcidss.iso8583.reactor.netty.pipeline.ISO8583_DECODER
import ru.whyhappen.pcidss.iso8583.reactor.netty.pipeline.LENGTH_FIELD_FRAME_DECODER
import ru.whyhappen.pcidss.iso8583.reactor.server.Iso8583Server
import java.net.SocketAddress
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Integration tests for [PcidssPocApplication].
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class PciDssApplicationTest {
    companion object {
        @Container
        private val container = RedisContainer(RedisContainer.DEFAULT_IMAGE_NAME.withTag(RedisContainer.DEFAULT_TAG))

        private val port = TestSocketUtils.findAvailableTcpPort()
        private lateinit var mockWebServer: MockWebServer

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("keystore.path") { System.getProperty("java.io.tmpdir") + "/keystore.bcfks" }
            registry.add("spring.data.redis.url") { "redis://${container.host}:${container.redisPort}" }
            registry.add("iso8583.connection.port") { port }
            registry.add("antifraud.base-url") { mockWebServer.url("/").toString() }
        }

        @JvmStatic
        @BeforeAll
        fun setUpAll() {
            mockWebServer = MockWebServer()
            mockWebServer.start()
        }

        @JvmStatic
        @AfterAll
        fun tearDownAll() {
            mockWebServer.shutdown()
        }
    }

    @Autowired
    private lateinit var server: Iso8583Server
    @Autowired
    private lateinit var messageFactory: MessageFactory<IsoMessage>

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun `should handle a message`() {
        val mockResponse = """{
                "39": "000"
            }""".trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(mockResponse)
        )

        val data = "0223" + // frame length
                "31313030" + // MTI
                "f476440128e1a0000000000010000000" + // bitmap
                "313634303032383978787878787833373232" + // 2 Primary Account Number (PAN)
                "303030303030" + // 3 Processing Code
                "303030303030383030303030" + // 4 Amount, Transaction
                "303030303030303031353634" + // 6 Amount, Cardholder Billing
                "3931393535303030" + // 10 Conversion Rate, Cardholder Billing
                "383632313333" + // 11 Systems Trace Audit Number (STAN)
                "323530353236313534383532" + // 12 Time, Local Transaction
                "32383034" + // 14 Date, Expiration
                "323530353236" + // 15 Date, Settlement
                "35343131" + // 18 Merchant Type
                "353130313032374344303031" + // 22 Point of Service Entry Mode
                "3036343137363635" + // 32 Acquiring Institution Identification Code
                "333734303032383930303333333033373232443238303432323130303030303939393939383232" + // 35 Track 2 Data
                "353134363130353636373534" + // 37 Retrieval Reference Number
                "3331313638343837" + // 41 Card Acceptor Terminal Identification
                "303130303132303030313034313937" + // 42 Card Acceptor Identification Code
                "3033374950205a485553555042454b4f5620412e442e3e3e4e55522d20554c54414e3e3e4b415a3e" + // 43 Card Acceptor Name amd Location
                // 48 Additional data (Private)
                "3239303030323030333737343030343030333030303031323030313230313330303332323130323530313535383531343633383933323331313930333530303634313736363530343030303131303431303031303034353030393030303038353834313037373030324e31303930303031353039313030323030303935303136343330383631323930303538343237303135343030343239313231363630313830303030303030303035333038363231333331383330333030303031303030303030303030303030303030303030303335333136383932343230363462353631323230363861376636356137626163363233626630386630663131613265363363353039616634383738663733346262313562656361643566333861323535303033333938" +
                "333938" + // 49 Currency Code, Transaction
                "383430" + // 51 Currency Code, Cardholder Billing
                "303430303031" // 100 Receiving Institution Identification Code

        val response = AtomicReference<IsoMessage>()
        val connection = createClient(messageFactory) { inbound, outbound ->
            inbound.receiveObject()
                .cast<IsoMessage>()
                .subscribe { msg -> response.set(msg) }

            outbound.sendByteArray(Mono.just(data.hexToByteArray()))
                .neverComplete()
        }.connectNow()

        await atMost 5.seconds untilNotNull { response.get() }

        connection.disposeNow()
        server.stop()

        with(response.get()) {
            mti shouldBe 0x1110
            fields.keys shouldContainExactly setOf(
                2, 3, 4, 6, 10, 11, 12, 14, 15, 18, 22, 32,
                35, 37, 39, 41, 42, 43, 48, 49, 51, 100
            )
            getFieldValue(2, String::class.java) shouldBe "400289xxxxxx3722"
            getFieldValue(3, String::class.java) shouldBe "000000"
            getFieldValue(4, String::class.java) shouldBe "800000"
            getFieldValue(6, String::class.java) shouldBe "1564"
            getFieldValue(10, String::class.java) shouldBe "91955000"
            getFieldValue(11, String::class.java) shouldBe "862133"
            getFieldValue(12, String::class.java) shouldBe "250526154852"
            getFieldValue(14, String::class.java) shouldBe "2804"
            getFieldValue(15, String::class.java) shouldBe "250526"
            getFieldValue(18, String::class.java) shouldBe "5411"
            getFieldValue(22, String::class.java) shouldBe "5101027CD001"
            getFieldValue(32, String::class.java) shouldBe "417665"
            getFieldValue(
                35,
                String::class.java
            ) shouldBe "34303032383930303333333033373232443238303432323130303030303939393939383232"
            getFieldValue(37, String::class.java) shouldBe "514610566754"
            getFieldValue(39, String::class.java) shouldBe "000"
            getFieldValue(41, String::class.java) shouldBe "31168487"
            getFieldValue(42, String::class.java) shouldBe "010012000104197"
            getFieldValue(43, String::class.java) shouldBe "IP ZHUSUPBEKOV A.D.>>NUR- ULTAN>>KAZ>"
            getFieldValue(
                48,
                String::class.java
            ) shouldBe "3030323030333737343030343030333030303031323030313230313330303332323130323530313535383531343633383933323331313930333530303634313736363530343030303131303431303031303034353030393030303038353834313037373030324E31303930303031353039313030323030303935303136343330383631323930303538343237303135343030343239313231363630313830303030303030303035333038363231333331383330333030303031303030303030303030303030303030303030303335333136383932343230363462353631323230363861376636356137626163363233626630386630663131613265363363353039616634383738663733346262313562656361643566333861323535303033333938"
            getFieldValue(49, String::class.java) shouldBe "398"
            getFieldValue(51, String::class.java) shouldBe "840"
            getFieldValue(100, String::class.java) shouldBe "0001"
        }
    }

    private fun createClient(
        messageFactory: MessageFactory<IsoMessage>,
        handler: (NettyInbound, NettyOutbound) -> Publisher<Void>
    ): TcpClient {
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
        }

        private fun createLengthFieldBasedFrameDecoder(configuration: ClientConfiguration) =
            LengthFieldBasedFrameDecoder(
                configuration.maxFrameLength,
                configuration.frameLengthFieldOffset,
                configuration.frameLengthFieldLength,
                configuration.frameLengthFieldAdjust,
                configuration.frameLengthFieldLength
            )
    }
}