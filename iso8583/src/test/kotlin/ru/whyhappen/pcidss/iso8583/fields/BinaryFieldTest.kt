package ru.whyhappen.pcidss.iso8583.fields

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import ru.whyhappen.pcidss.iso8583.encode.Encoders
import ru.whyhappen.pcidss.iso8583.prefix.Binary
import ru.whyhappen.pcidss.iso8583.prefix.PrefixerException
import ru.whyhappen.pcidss.iso8583.spec.Spec
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Tests for [BinaryField]
 */
class BinaryFieldTest {
    private lateinit var field: IsoField

    @BeforeTest
    fun setUp() {
        field = BinaryField(spec = Spec(5, "Test", Encoders.binary, Binary.fixed))
    }

    @Test
    fun `should set and get field values`() {
        val value = byteArrayOf(0x01, 0xA0.toByte(), 0xBC.toByte(), 0x00, 0xF2.toByte())
        val strValue = "01A0BC00F2"

        with(field) {
            setValue(value)
            getValue(ByteArray::class.java) shouldBe value
            getValue(String::class.java) shouldBe strValue
            bytes = value

            setValue(strValue)
            getValue(ByteArray::class.java) shouldBe value
            getValue(String::class.java) shouldBe strValue
            bytes = value
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
        val value = byteArrayOf(0xF0.toByte(), 0xA3.toByte(), 0xCA.toByte(), 0x53, 0x10)

        with(field) {
            bytes = value
            pack() shouldBe value
        }
    }

    @Test
    fun `should unpack field value`() {
        val value = byteArrayOf(0xA0.toByte(), 0xC3.toByte(), 0xFA.toByte(), 0xA3.toByte(), 0x54)

        with(field) {
            unpack(value) shouldBe value.size
            bytes shouldBe value
        }
    }

    @Test
    fun `should create a copy of the field`() {
        val value = byteArrayOf(0xA0.toByte(), 0xC3.toByte(), 0xF9.toByte(), 0xBF.toByte(), 0x23)
        field.bytes = value

        with(field.copyOf()) {
            bytes shouldBe value
            spec shouldBe field.spec
        }
    }

    @Test
    fun `should fail to pack if the value is of incorrect size`() {
        field.setValue(byteArrayOf(0xF0.toByte(), 0xA3.toByte(), 0xCA.toByte()))
        shouldThrow<PrefixerException> {
            field.pack()
        }
    }
}