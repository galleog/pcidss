package ru.whyhappen.pcidss.iso8583

import com.nfeld.jsonpathkt.kotlinx.resolvePathAsStringOrNull
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerifyAll
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
import ru.whyhappen.pcidss.iso8583.encode.Encoders.ascii
import ru.whyhappen.pcidss.iso8583.encode.Encoders.binary
import ru.whyhappen.pcidss.iso8583.fields.Bitmap
import ru.whyhappen.pcidss.iso8583.fields.StringField
import ru.whyhappen.pcidss.iso8583.mti.ISO8583Version
import ru.whyhappen.pcidss.iso8583.mti.MessageClass
import ru.whyhappen.pcidss.iso8583.mti.MessageFunction
import ru.whyhappen.pcidss.iso8583.mti.MessageOrigin
import ru.whyhappen.pcidss.iso8583.pad.StartPadder
import ru.whyhappen.pcidss.iso8583.prefix.Ascii
import ru.whyhappen.pcidss.iso8583.prefix.Binary
import ru.whyhappen.pcidss.iso8583.spec.MessageSpec
import ru.whyhappen.pcidss.iso8583.spec.Spec
import ru.whyhappen.pcidss.service.TokenService
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Tests for [ExternalIsoMessageHandler].
 */
@ExtendWith(MockKExtension::class)
class ExternalIsoMessageHandlerTest {
    @MockK
    private lateinit var tokenService: TokenService
    @MockK(relaxed = true)
    private lateinit var customizer: IsoMessageCustomizer

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
                12 to StringField(
                    spec = Spec(
                        6,
                        "Local Transaction Time",
                        ascii,
                        Ascii.fixed
                    )
                ),
                34 to StringField(
                    spec = Spec(
                        28,
                        "Extended Primary Account Number",
                        ascii,
                        Ascii.LL
                    )
                ),
                39 to StringField(
                    spec = Spec(
                        2,
                        "Response Code",
                        ascii,
                        Ascii.fixed
                    )
                )
            )
        )
    )

    private lateinit var webClient: WebClient
    private lateinit var handler: ExternalIsoMessageHandler

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
                "2": "token1",
                "4": "456",
                "7": "0630200856",
                "11": "654321",
                "34": "token2",
                "39": "00"
            }""".trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(mockResponse)
        )

        val requestMessage = messageFactory.newMessage(MessageClass.FINANCIAL, MessageFunction.REQUEST)
            .apply {
                setFieldValue(2, "4242424242424242")
                setFieldValue(4, "456")
                setFieldValue(7, "0630200856")
                setFieldValue(11, "123456")
                setFieldValue(34, "2345674242424242424242123456")
            }

        coEvery { tokenService.getToken(requestMessage.getFieldValue(2, ByteArray::class.java)!!) } returns "token1"
        coEvery { tokenService.getToken(requestMessage.getFieldValue(34, ByteArray::class.java)!!) } returns "token2"

        handler = ExternalIsoMessageHandler(listOf(2, 34), messageFactory, tokenService, webClient, listOf(customizer))

        val responseMessage = handler.onMessage(requestMessage)
        with(responseMessage) {
            mti shouldBe 0x210
            fields.keys shouldContainExactly setOf(2, 4, 7, 11, 34, 39)
            getFieldValue(2, String::class.java) shouldBe requestMessage.getFieldValue(2, String::class.java)
            getFieldValue(4, String::class.java) shouldBe "456"
            getFieldValue(7, String::class.java) shouldBe "0630200856"
            getFieldValue(11, String::class.java) shouldBe "654321"
            getFieldValue(34, String::class.java) shouldBe requestMessage.getFieldValue(34, String::class.java)
            getFieldValue(39, String::class.java) shouldBe "00"
        }

        val recordedRequest = mockWebServer.takeRequest()
        recordedRequest.method shouldBe "POST"

        with(Json.parseToJsonElement(recordedRequest.body.readUtf8())) {
            resolvePathAsStringOrNull("$.0") shouldBe "%04x".format(requestMessage.mti)
            resolvePathAsStringOrNull("$.2") shouldBe "token1"
            resolvePathAsStringOrNull("$.4") shouldBe requestMessage.getFieldValue(4, String::class.java)
            resolvePathAsStringOrNull("$.7") shouldBe requestMessage.getFieldValue(7, String::class.java)
            resolvePathAsStringOrNull("$.11") shouldBe requestMessage.getFieldValue(11, String::class.java)
            resolvePathAsStringOrNull("$.34") shouldBe "token2"
        }

        coVerifyAll {
            tokenService.getToken(requestMessage.getFieldValue(2, ByteArray::class.java)!!)
            tokenService.getToken(requestMessage.getFieldValue(34, ByteArray::class.java)!!)
        }

        verifyAll {
            customizer.customize(responseMessage)
        }
    }
}
