package ru.whyhappen.pcidss.iso8583

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import ru.whyhappen.pcidss.iso8583.encode.AsciiEncoder
import ru.whyhappen.pcidss.iso8583.encode.BytesToAsciiHexEncoder
import ru.whyhappen.pcidss.iso8583.fields.Bitmap
import ru.whyhappen.pcidss.iso8583.fields.StringField
import ru.whyhappen.pcidss.iso8583.pad.StartPadder
import ru.whyhappen.pcidss.iso8583.prefix.AsciiFixedPrefixer
import ru.whyhappen.pcidss.iso8583.prefix.AsciiVarPrefixer
import ru.whyhappen.pcidss.iso8583.prefix.HexFixedPrefixer
import ru.whyhappen.pcidss.iso8583.spec.MessageSpec
import ru.whyhappen.pcidss.iso8583.spec.Spec
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Tests for [DefaultIsoMessage].
 */
class DefaultIsoMessageTest {
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
            ),
            // this field will be ignored as bit 65 is a bitmap presence indicator
            65 to StringField(spec = Spec(1, "Settlement Code", AsciiEncoder(), AsciiFixedPrefixer())),
            // this is a field of the third bitmap
            130 to StringField(spec = Spec(1, "Additional Data", AsciiEncoder(), AsciiFixedPrefixer()))
        )
    )

    private lateinit var msg: IsoMessage

    @BeforeTest
    fun setUp() {
        msg = DefaultIsoMessage(spec)
    }

    @Test
    fun `should get and set MTI`() {
        with(msg) {
            mti shouldBe 0
            mti = 0x200
            mti shouldBe 0x0200
        }
    }

    @Test
    fun `should set and unset values correctly`() {
        with(msg) {
            hasField(2).shouldBeFalse()
            setFieldValue(2, "4242424242424242")
            hasField(2).shouldBeTrue()
            getFieldValue(2, String::class.java) shouldBe "4242424242424242"

            hasField(3).shouldBeFalse()
            setFieldValue(3, "123456")
            hasField(3).shouldBeTrue()
            getFieldValue(3, String::class.java) shouldBe "123456"

            hasField(4).shouldBeFalse()
            setFieldValue(4, "100")
            hasField(4).shouldBeTrue()
            getFieldValue(4, String::class.java) shouldBe "100"

            unsetFields(2, 3)
            hasField(2).shouldBeFalse()
            getFieldValue(2, String::class.java).shouldBeNull()
            hasField(3).shouldBeFalse()
            getFieldValue(3, String::class.java).shouldBeNull()
            hasField(4).shouldBeTrue()
        }
    }

    @Test
    fun `shouldn't set values for fields that match the bitmap presence indicator`() {
        shouldThrow<IllegalStateException> {
            msg.setFieldValue(65, "0")
        }
    }

    @Test
    fun `should pack message`() {
        with(msg) {
            mti = 0x0100
            setFieldValue(2, "38383838383838")
            setFieldValue(3, "123123")
            setFieldValue(4, "100")

            pack() shouldBe "010070000000000000001438383838383838123123000000000100".toByteArray()

            unsetFields(2, 3, 4)
            setFieldValue(130, "1")
            pack() shouldBe "01008000000000000000800000000000000040000000000000001".toByteArray()
        }
    }

    @Test
    fun `should unpack message`() {
        with(msg) {
            unpack("02007000000000000000164545454545454545654321000000000111".toByteArray())

            mti shouldBe 0x200
            fields.keys shouldContainExactly setOf(2, 3, 4)
            getFieldValue(2, String::class.java) shouldBe "4545454545454545"
            getFieldValue(3, String::class.java) shouldBe "654321"
            getFieldValue(4, String::class.java) shouldBe "111"

            unpack("01008000000000000000800000000000000040000000000000003".toByteArray())

            mti shouldBe 0x100
            fields.keys shouldContainExactly setOf(130)
            getFieldValue(130, String::class.java) shouldBe "3"
        }
    }

    @Test
    fun `should fail to unpack message with wrong MTI`() {
        shouldThrow<IsoMessageException> {
            msg.unpack("027000000000000000164545454545454545654321000000000111".toByteArray())
        }
    }

    @Test
    fun `should fail to unpack message with wrong bitmap`() {
        shouldThrow<IsoMessageException> {
            // one byte removed from the bitmap that makes the length check fail
            msg.unpack("020070000000000000164545454545454545654321000000000111".toByteArray())
        }
    }

    @Test
    fun `should fail to unpack message with wrong field`() {
        shouldThrow<IsoMessageException> {
            // one symbol removed from 3-rd field that makes the length check fail
            msg.unpack("0200700000000000000016454545454545454565432000000000111".toByteArray())
        }
    }

    @Test
    fun `should copy message`() {
        val copy = with(msg) {
            mti = 0x100
            setFieldValue(2, "4242424242")
            setFieldValue(3, "123456")
            setFieldValue(4, "100")
            setFieldValue(130, "2")

            copyOf()
        }

        with(copy) {
            fields.keys shouldContainExactly setOf(2, 3, 4, 130)
            getFieldValue(2, String::class.java) shouldBe "4242424242"
            getFieldValue(3, String::class.java) shouldBe "123456"
            getFieldValue(4, String::class.java) shouldBe "100"
            getFieldValue(130, String::class.java) shouldBe "2"
        }
    }
}