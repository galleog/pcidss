package ru.whyhappen.pcidss.iso8583

import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import ru.whyhappen.pcidss.iso8583.encode.AsciiEncoder
import ru.whyhappen.pcidss.iso8583.encode.BytesToAsciiHexEncoder
import ru.whyhappen.pcidss.iso8583.fields.Bitmap
import ru.whyhappen.pcidss.iso8583.fields.StringField
import ru.whyhappen.pcidss.iso8583.mti.ISO8583Version
import ru.whyhappen.pcidss.iso8583.mti.MessageClass
import ru.whyhappen.pcidss.iso8583.mti.MessageFunction
import ru.whyhappen.pcidss.iso8583.mti.MessageOrigin
import ru.whyhappen.pcidss.iso8583.pad.StartPadder
import ru.whyhappen.pcidss.iso8583.prefix.AsciiFixedPrefixer
import ru.whyhappen.pcidss.iso8583.prefix.AsciiVarPrefixer
import ru.whyhappen.pcidss.iso8583.prefix.HexFixedPrefixer
import ru.whyhappen.pcidss.iso8583.spec.MessageSpec
import ru.whyhappen.pcidss.iso8583.spec.Spec
import kotlin.test.Test

/**
 * Tests for [DefaultMessageFactory].
 */
class DefaultMessageFactoryTest {
    private val spec = MessageSpec(
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
                    BytesToAsciiHexEncoder(),
                    HexFixedPrefixer()
                )
            ),
            2 to StringField(
                spec = Spec(
                    19,
                    "Primary Account Number",
                    AsciiEncoder(),
                    AsciiVarPrefixer(2)
                )
            ),
            3 to StringField(
                spec = Spec(
                    6,
                    "Processing Code",
                    AsciiEncoder(),
                    AsciiFixedPrefixer()
                )
            ),
            4 to StringField(
                spec = Spec(
                    12,
                    "Transaction Amount",
                    AsciiEncoder(),
                    AsciiFixedPrefixer(),
                    StartPadder('0')
                )
            )
        )
    )
    private val messageFactory = DefaultMessageFactory(ISO8583Version.V1987, MessageOrigin.ACQUIRER, spec)

    @Test
    fun `should create a new message with all fields unset`() {
        messageFactory.newMessage(
            messageClass = MessageClass.FINANCIAL,
            messageFunction = MessageFunction.REQUEST
        ).fields.shouldBeEmpty()
    }

    @Test
    fun `should create a response`() {
        val isoMessage = DefaultIsoMessage(spec)
            .apply {
                mti = 0x200
                setFieldValue(2, "4242424242424242")
                setFieldValue(3, "123456")
                setFieldValue(4, "100")
            }

        with(messageFactory.createResponse(isoMessage)) {
            mti shouldBe 0x210
            fields.keys shouldBe setOf(2, 3, 4)
            getFieldValue(2, String::class.java) shouldBe "4242424242424242"
            getFieldValue(3, String::class.java) shouldBe "123456"
            getFieldValue(4, String::class.java) shouldBe "100"
        }
    }

    @Test
    fun `should parse message`() {
        val bytes = "01007000000000000000164242424242424242123456000000000100".toByteArray()

        with(messageFactory.parseMessage(bytes)) {
            mti shouldBe 0x0100
            fields.keys shouldBe setOf(2, 3, 4)
            getFieldValue(2, String::class.java) shouldBe "4242424242424242"
            getFieldValue(3, String::class.java) shouldBe "123456"
            getFieldValue(4, String::class.java) shouldBe "100"
        }
    }
}