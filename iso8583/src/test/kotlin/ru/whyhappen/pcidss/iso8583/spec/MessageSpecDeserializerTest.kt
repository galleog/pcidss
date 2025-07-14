package ru.whyhappen.pcidss.iso8583.spec

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.maps.shouldMatchExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.springframework.core.io.ClassPathResource
import ru.whyhappen.pcidss.iso8583.encode.AsciiEncoder
import ru.whyhappen.pcidss.iso8583.encode.BytesToAsciiHexEncoder
import ru.whyhappen.pcidss.iso8583.fields.Bitmap
import ru.whyhappen.pcidss.iso8583.fields.StringField
import ru.whyhappen.pcidss.iso8583.prefix.AsciiFixedPrefixer
import ru.whyhappen.pcidss.iso8583.prefix.AsciiVarPrefixer
import ru.whyhappen.pcidss.iso8583.prefix.HexFixedPrefixer
import kotlin.test.Test

/**
 * Tests for [MessageSpecDeserializer].
 */
class MessageSpecDeserializerTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `should deserialize fields`() {
        val json = """{
            "0": {
                "type": "String",
                "length": 4,
                "description": "Message Type Indicator",
                "enc": "ASCII",
                "prefix": "ASCII.fixed"
            },
            "1": {
                "type": "Bitmap",
                "length": 8,
                "description": "Bitmap",
                "enc": "HexToASCII",
                "prefix": "Hex.fixed"
            },
            "2": {
                "type": "String",
                "length": 19,
                "description": "Primary Account Number",
                "enc": "ASCII",
                "prefix": "ASCII.LL"
            }
        }""".trimIndent()

        val spec = objectMapper.readValue(json, MessageSpec::class.java)
        with(spec.fields) {
            shouldMatchExactly(
                0 to {
                    it.shouldBeInstanceOf<StringField>()
                    with(it.spec) {
                        length shouldBe 4
                        description shouldBe "Message Type Indicator"
                        encoder.shouldBeInstanceOf<AsciiEncoder>()
                        prefixer.shouldBeInstanceOf<AsciiFixedPrefixer>()
                        padder.shouldBeNull()
                    }
                },
                1 to {
                    it.shouldBeInstanceOf<Bitmap>()
                    with(it.spec) {
                        length shouldBe 8
                        description shouldBe "Bitmap"
                        encoder.shouldBeInstanceOf<BytesToAsciiHexEncoder>()
                        prefixer.shouldBeInstanceOf<HexFixedPrefixer>()
                        padder.shouldBeNull()
                    }
                },
                2 to {
                    it.shouldBeInstanceOf<StringField>()
                    with(it.spec) {
                        length shouldBe 19
                        description shouldBe "Primary Account Number"
                        encoder.shouldBeInstanceOf<AsciiEncoder>()
                        prefixer.shouldBeInstanceOf<AsciiVarPrefixer>()
                        prefixer.digits shouldBe 2
                        padder.shouldBeNull()
                    }
                }
            )
        }
    }

    @Test
    fun `should deserialize JSON with specification ISO 8583 v1987`() {
        val resource = ClassPathResource("spec87ascii.json")
        resource.inputStream.use {
            val spec = objectMapper.readValue(it, MessageSpec::class.java)
            spec.fields shouldHaveSize 66
        }
    }

    @Test
    fun `should ignore incorrect fields`() {
        val json = """{
            "0": {
                "type": "String",
                "length": 4,
                "description": "Message Type Indicator",
                "enc": "ASCII",
                "prefix": "ASCII.fixed"
            },
            "1": {
                "type": "Bitmap",
                "length": 8,
                "description": "Bitmap",
                "enc": "HexToASCII",
                "prefix": "Hex.fixed"
            },
            "invalid": {
            }
        }""".trimIndent()

        val spec = objectMapper.readValue(json, MessageSpec::class.java)
        spec.fields shouldHaveSize 2
    }
}