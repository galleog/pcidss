package ru.whyhappen.pcidss.iso8583.fields

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import ru.whyhappen.pcidss.iso8583.encode.AsciiEncoder
import ru.whyhappen.pcidss.iso8583.encode.BinaryEncoder
import ru.whyhappen.pcidss.iso8583.pad.StartPadder
import ru.whyhappen.pcidss.iso8583.prefix.AsciiFixedPrefixer
import ru.whyhappen.pcidss.iso8583.prefix.BinaryVarPrefixer
import ru.whyhappen.pcidss.iso8583.spec.DefaultPacker
import ru.whyhappen.pcidss.iso8583.spec.HexPacker
import kotlin.test.Test

/**
 * Tests for [IsoFieldDeserializer].
 */
class IsoFieldDeserializerTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `should deserialize correct JSON with padding`() {
        val json = """{
            "type":"String",
            "length": 10,
            "description":"With padding",
            "enc":"ASCII",
            "prefix":"ASCII.fixed",
            "padding":{
                "type":"Start",
                "pad":48
            },
            "packer":"Hex"
        }""".trimIndent()

        val field = objectMapper.readValue(json, IsoField::class.java)

        field.shouldBeInstanceOf<StringField>()
        with(field.spec) {
            length shouldBe 10
            description shouldBe "With padding"
            encoder.shouldBeInstanceOf<AsciiEncoder>()
            prefixer.shouldBeInstanceOf<AsciiFixedPrefixer>()
            padder.shouldBeInstanceOf<StartPadder>()
            padder.padBytes shouldBe byteArrayOf(48)
            packer.shouldBeInstanceOf<HexPacker>()
        }
    }

    @Test
    fun `should deserialize JSON without padding and packer`() {
        val json = """{
            "type":"Binary",
            "length": 99,
            "description":"Without padding",
            "enc":"Binary",
            "prefix":"Binary.LL"
        }""".trimIndent()

        val field = objectMapper.readValue(json, IsoField::class.java)
        field.shouldBeInstanceOf<BinaryField>()
        with(field.spec) {
            length shouldBe 99
            description shouldBe "Without padding"
            encoder.shouldBeInstanceOf<BinaryEncoder>()
            prefixer.shouldBeInstanceOf<BinaryVarPrefixer>()
            prefixer.digits shouldBe 2
            padder.shouldBeNull()
            packer.shouldBeInstanceOf<DefaultPacker>()
        }
    }

    @Test
    fun `should fail to deserialize empty JSON`() {
        val json = "{}"

        shouldThrow<InvalidFormatException> {
            objectMapper.readValue(json, IsoField::class.java)
        }
    }

    @Test
    fun `should fail to deserialize JSON with invalid property 'type'`() {
        val json = """{
            "type":"Invalid",
            "length": 10,
            "description":"Test",
            "enc":"ASCII",
            "prefix":"ASCII.fixed"
        }""".trimIndent()

        shouldThrow<InvalidFormatException> {
            objectMapper.readValue(json, IsoField::class.java)
        }
    }

    @Test
    fun `should fail to deserialize JSON without property 'length'`() {
        val json = """{
            "type":"String",
            "description":"Test",
            "enc":"ASCII",
            "prefix":"ASCII.Fixed"
        }""".trimIndent()

        shouldThrow<InvalidFormatException> {
            objectMapper.readValue(json, IsoField::class.java)
        }
    }

    @Test
    fun `should fail to serialize JSON with invalid property 'description'`() {
        val json = """{
            "type":"String",
            "length": 10,
            "description":4,
            "enc":"ASCII",
            "prefix":"ASCII.Fixed"
        }""".trimIndent()

        shouldThrow<InvalidFormatException> {
            objectMapper.readValue(json, IsoField::class.java)
        }
    }

    @Test
    fun `should fail to serialize JSON with invalid property 'enc'`() {
        val json = """{
            "type":"String",
            "length":10,
            "description":"Test",
            "enc":{},
            "prefix":"ASCII.fixed"
        }""".trimIndent()

        shouldThrow<InvalidFormatException> {
            objectMapper.readValue(json, IsoField::class.java)
        }
    }

    @Test
    fun `should fail to serialize JSON without property 'prefix'`() {
        val json = """{
            "type":"String",
            "length":10,
            "description":"Test",
            "enc":"ASCII"
        }""".trimIndent()

        shouldThrow<InvalidFormatException> {
            objectMapper.readValue(json, IsoField::class.java)
        }
    }

    @Test
    fun `should fail to serialize JSON with invalid property 'padding'`() {
        val json = """{
            "type":"String",
            "length": 10,
            "description":"Test",
            "enc":"ASCII",
            "prefix":"ASCII.fixed",
            "padding":{}
        }""".trimIndent()

        shouldThrow<InvalidFormatException> {
            objectMapper.readValue(json, IsoField::class.java)
        }
    }

    @Test
    fun `should fail to serialize JSON with invalid 'pad'`() {
        val json = """{
            "type":"String",
            "length": 10,
            "description":"Test",
            "enc":"ASCII",
            "prefix":"ASCII.fixed",
            "padding":{
                "type":"End",
                "pad":"0"
            }
        }""".trimIndent()

        shouldThrow<InvalidFormatException> {
            objectMapper.readValue(json, IsoField::class.java)
        }
    }

    @Test
    fun `should fail to serialize JSON with missing 'pad'`() {
        val json = """{
            "type":"String",
            "length": 10,
            "description":"Test",
            "enc":"ASCII",
            "prefix":"ASCII.fixed",
            "padding":{
                "type":"End"
            }
        }""".trimIndent()

        shouldThrow<IllegalStateException> {
            objectMapper.readValue(json, IsoField::class.java)
        }
    }

    @Test
    fun `should fail to serialize JSON with invalid property 'packer'`() {
        val json = """{
            "type":"String",
            "length": 10,
            "description":"Test",
            "enc":"ASCII",
            "prefix":"ASCII.fixed",
            "packer":"Invalid"
        }""".trimIndent()

        shouldThrow<InvalidFormatException> {
            objectMapper.readValue(json, IsoField::class.java)
        }
    }
}