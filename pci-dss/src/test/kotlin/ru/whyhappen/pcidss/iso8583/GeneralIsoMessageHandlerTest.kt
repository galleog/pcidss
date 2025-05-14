package ru.whyhappen.pcidss.iso8583

import com.github.kpavlov.jreactive8583.iso.MessageFactory
import com.nfeld.jsonpathkt.kotlinx.resolvePathAsStringOrNull
import com.solab.iso8583.IsoMessage
import com.solab.iso8583.IsoType
import com.solab.iso8583.IsoValue
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerifyAll
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verifyAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import ru.whyhappen.pcidss.service.TokenService
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Tests for [GeneralIsoMessageHandler].
 */
@ExtendWith(MockKExtension::class)
class GeneralIsoMessageHandlerTest {
    @MockK
    private lateinit var messageFactory: MessageFactory<IsoMessage>
    @MockK
    private lateinit var tokenService: TokenService
    @MockK(relaxed = true)
    private lateinit var customizer: IsoMessageCustomizer

    private lateinit var webClient: WebClient
    private lateinit var handler: GeneralIsoMessageHandler

    companion object {
        private lateinit var mockWebServer: MockWebServer

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

    @BeforeTest
    fun setUp() {
        webClient = WebClient.builder()
            .baseUrl(mockWebServer.url("/").toString())
            .build()
    }

    @Test
    fun `should tokenize fields and update fields set by the external service`() = runTest {
        val mockResponse = """{
                "fields": {
                    "2": "token1",
                    "39": "01"
                }
            }""".trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(mockResponse)
        )

        val inboundMessage = IsoMessage().apply {
            type = 0x200
            setField(2, IsoValue(IsoType.NUMERIC, "650000", 6))
            setField(32, IsoValue(IsoType.LLVAR, "456"))
            setField(35, IsoValue(IsoType.LLVAR, "4591700012340000"))
            setField(60, IsoValue(IsoType.LLLVAR, "B456PRO1+000"))
            setField(61, IsoValue(IsoType.LLLVAR, "1234P"))
        }

        val responseMessage = IsoMessage().apply {
            type = 0x210
            (2..128).asSequence()
                .filter(inboundMessage::hasField)
                .forEach {
                    setField(it, inboundMessage.getAt<Any>(it).clone())
                }
            setField(39, IsoValue(IsoType.NUMERIC, "", 2))
            updateValue(60, "Fixed value 60")
            setField(70, IsoValue(IsoType.ALPHA, "ABC", 3))
        }

        coEvery { tokenService.getToken(inboundMessage.getObjectValue(2)) } returns "token1"
        coEvery { tokenService.getToken(inboundMessage.getObjectValue(35)) } returns "token2"
        every { messageFactory.createResponse(inboundMessage) } returns responseMessage

        handler = GeneralIsoMessageHandler(listOf(2, 35), messageFactory, tokenService, webClient, customizer)
        val result = handler.onMessage(inboundMessage)

        val recordedRequest = mockWebServer.takeRequest()
        recordedRequest.method shouldBe "POST"

        with(Json.parseToJsonElement(recordedRequest.body.readUtf8())) {
            resolvePathAsStringOrNull("$.mti") shouldBe "%04x".format(inboundMessage.type)
            resolvePathAsStringOrNull("$.fields.2") shouldBe "token1"
            resolvePathAsStringOrNull("$.fields.32") shouldBe inboundMessage.getObjectValue(32)
            resolvePathAsStringOrNull("$.fields.35") shouldBe "token2"
            resolvePathAsStringOrNull("$.fields.60") shouldBe inboundMessage.getObjectValue(60)
            resolvePathAsStringOrNull("$.fields.61") shouldBe inboundMessage.getObjectValue(61)
        }

        with(result) {
            type shouldBe 0x210
            getObjectValue<String>(2) shouldBe inboundMessage.getObjectValue(2)
            getObjectValue<String>(32) shouldBe inboundMessage.getObjectValue(32)
            getObjectValue<String>(35) shouldBe inboundMessage.getObjectValue(35)
            getObjectValue<String>(39) shouldBe "01"
            getObjectValue<String>(60) shouldBe responseMessage.getObjectValue(60)
            getObjectValue<String>(61) shouldBe inboundMessage.getObjectValue(61)
            getObjectValue<String>(70) shouldBe responseMessage.getObjectValue(70)
        }

        coVerifyAll {
            tokenService.getToken(inboundMessage.getObjectValue(2))
            tokenService.getToken(inboundMessage.getObjectValue(35))
        }

        verifyAll {
            messageFactory.createResponse(inboundMessage)
            customizer.customize(responseMessage)
        }
    }
}
