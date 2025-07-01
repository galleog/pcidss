package ru.whyhappen.pcidss.iso8583.fields

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import ru.whyhappen.pcidss.iso8583.encode.AsciiEncoder
import ru.whyhappen.pcidss.iso8583.pad.StartPadder
import ru.whyhappen.pcidss.iso8583.prefix.AsciiFixedPrefixer
import ru.whyhappen.pcidss.iso8583.spec.Spec
import java.math.BigInteger
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Tests for [NumericField].
 */
class NumericFieldTest {
    private val encoder = AsciiEncoder()
    private val prefixer = AsciiFixedPrefixer()
    private val padder = StartPadder('0')

    private lateinit var field: NumericField

    @BeforeTest
    fun setUp() {
        field = NumericField(spec = Spec(10, "Test", encoder, prefixer, padder))
    }

    @Test
    fun `should set and get field values`() {
        val value = 110.toBigInteger()
        val str = value.toString()
        val bytes = str.toByteArray(Charsets.US_ASCII)

        with(field) {
            setValue(value)
            getValue(String::class.java) shouldBe str
            getValue(ByteArray::class.java) shouldBe bytes
            getValue(Int::class.java) shouldBe value.toInt()
            getValue(Long::class.java) shouldBe value.toLong()
            getValue(BigInteger::class.java) shouldBe value
            this.bytes shouldBe bytes

            setValue(str)
            getValue(String::class.java) shouldBe str
            getValue(ByteArray::class.java) shouldBe bytes
            getValue(Int::class.java) shouldBe value.toInt()
            getValue(Long::class.java) shouldBe value.toLong()
            getValue(BigInteger::class.java) shouldBe value
            this.bytes shouldBe bytes

            setValue(bytes)
            getValue(String::class.java) shouldBe str
            getValue(ByteArray::class.java) shouldBe bytes
            getValue(Int::class.java) shouldBe value.toInt()
            getValue(Long::class.java) shouldBe value.toLong()
            getValue(BigInteger::class.java) shouldBe value
            this.bytes shouldBe bytes

            setValue(value.toInt())
            getValue(String::class.java) shouldBe str
            getValue(ByteArray::class.java) shouldBe bytes
            getValue(Int::class.java) shouldBe value.toInt()
            getValue(Long::class.java) shouldBe value.toLong()
            getValue(BigInteger::class.java) shouldBe value
            this.bytes shouldBe bytes

            setValue(value.toLong())
            getValue(String::class.java) shouldBe str
            getValue(ByteArray::class.java) shouldBe bytes
            getValue(Int::class.java) shouldBe value.toInt()
            getValue(Long::class.java) shouldBe value.toLong()
            getValue(BigInteger::class.java) shouldBe value
            this.bytes shouldBe bytes
        }
    }

    @Test
    fun `should get field value if no values were assigned`() {
        with(field) {
            getValue(String::class.java).shouldBeNull()
            getValue(ByteArray::class.java).shouldBeNull()
            getValue(BigInteger::class.java).shouldBeNull()
            getValue(Int::class.javaObjectType).shouldBeNull()
            getValue(Long::class.javaObjectType).shouldBeNull()
            bytes shouldBe byteArrayOf()
        }
    }

    @Test
    fun `should pack field value`() {
        val value = 123456.toBigInteger()

        with(field) {
            setValue(value)
            pack() shouldBe "0000123456".toByteArray(Charsets.US_ASCII)
        }
    }

    @Test
    fun `should unpack field value`() {
        val bytes = "0000012345".toByteArray(Charsets.US_ASCII)

        with(field) {
            unpack(bytes) shouldBe bytes.size
            getValue(BigInteger::class.java) shouldBe 12345.toBigInteger()
        }
    }

    @Test
    fun `should unpack field value where all symbols were padded out`() {
        val bytes = "0000000000".toByteArray(Charsets.US_ASCII)

        with(field) {
            unpack(bytes) shouldBe bytes.size
            getValue(BigInteger::class.java) shouldBe 0.toBigInteger()
        }
    }

    @Test
    fun `should create a copy of the field`() {
        val value = 123.toBigInteger()
        field.setValue(value)

        with(field.copyOf()) {
            getValue(BigInteger::class.java) shouldBe value
            spec shouldBe field.spec
        }
    }
}