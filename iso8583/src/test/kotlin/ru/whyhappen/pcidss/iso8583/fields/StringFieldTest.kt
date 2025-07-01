package ru.whyhappen.pcidss.iso8583.fields

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import ru.whyhappen.pcidss.iso8583.encode.AsciiEncoder
import ru.whyhappen.pcidss.iso8583.pad.EndPadder
import ru.whyhappen.pcidss.iso8583.prefix.AsciiFixedPrefixer
import ru.whyhappen.pcidss.iso8583.spec.Spec
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Tests for [StringField].
 */
class StringFieldTest {
    private val encoder = AsciiEncoder()
    private val prefixer = AsciiFixedPrefixer()
    private val padder = EndPadder('0')
    private lateinit var field: IsoField

    @BeforeTest
    fun setUp() {
        field = StringField(spec = Spec(5, "Test", encoder, prefixer, padder))
    }

    @Test
    fun `should set and get field values`() {
        val value = "12345"
        val bytes = value.toByteArray(Charsets.US_ASCII)

        with(field) {
            setValue(value)
            getValue(String::class.java) shouldBe value
            getValue(ByteArray::class.java) shouldBe bytes
            this.bytes = bytes

            setValue(bytes)
            getValue(String::class.java) shouldBe value
            getValue(ByteArray::class.java) shouldBe bytes
            this.bytes = bytes
        }
    }

    @Test
    fun `should get field value if no values were assigned`() {
        with(field) {
            getValue(String::class.java).shouldBeNull()
            getValue(ByteArray::class.java).shouldBeNull()
            bytes shouldBe byteArrayOf()
        }
    }

    @Test
    fun `should pack field value`() {
        val value = "abcde"
        val bytes = value.toByteArray(Charsets.US_ASCII)

        with(field) {
            setValue(value)
            pack() shouldBe bytes
        }
    }

    @Test
    fun `should unpack field value`() {
        val value = "ABCDE"
        val bytes = value.toByteArray(Charsets.US_ASCII)

        with(field) {
            unpack(bytes) shouldBe value.length
            getValue(String::class.java) shouldBe value
        }
    }

    @Test
    fun `should create a copy of the field`() {
        val value = "54321"
        field.setValue(value)

        with(field.copyOf()) {
            getValue(String::class.java) shouldBe value
            spec shouldBe field.spec
        }
    }

    @Test
    fun `should pad value before packing`() {
        val value = "abc"
        val bytes = "abc00".toByteArray(Charsets.US_ASCII)

        field.setValue(value)
        field.pack() shouldBe bytes
    }

    @Test
    fun `should unpad value after unpacking`() {
        val value = "ABC00"
        val bytes = value.toByteArray(Charsets.US_ASCII)

        with(field) {
            unpack(bytes) shouldBe value.length
            getValue(String::class.java) shouldBe "ABC"
        }
    }
}