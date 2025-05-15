package ru.whyhappen.pcidss.core.iso8583

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nfeld.jsonpathkt.kotlinx.resolvePathAsStringOrNull
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlin.test.Test

/**
 * Test for [IsoMessageDto].
 */
class IsoMessageDtoTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `should serialize IsoMessageDto`() {
        val isoMessageDto = IsoMessageDto(
            mti = 0x1644,
            fields = mapOf(
                2 to "value2",
                3 to "value3"
            )
        )

        val json = Json.parseToJsonElement(objectMapper.writeValueAsString(isoMessageDto))
        json.resolvePathAsStringOrNull("$.0") shouldBe "1644"
        json.resolvePathAsStringOrNull("$.2") shouldBe "value2"
        json.resolvePathAsStringOrNull("$.3") shouldBe "value3"
    }

    @Test
    fun `should deserialize IsoMessageDto with MTI`() {
        val json = """{
                    "0":"0200",
                    "2":"value2",
                    "3":"value3"
                }""".trimIndent()

        with(objectMapper.readValue(json, IsoMessageDto::class.java)) {
            mti shouldBe 0x0200
            fields shouldBe mapOf(
                2 to "value2",
                3 to "value3"
            )
        }
    }

    @Test
    fun `should deserialize IsoMessageDto without MTI`() {
        val json = """{
                    "2":"value2",
                    "3":"value3"
                }""".trimIndent()

        with(objectMapper.readValue(json, IsoMessageDto::class.java)) {
            mti.shouldBeNull()
            fields shouldBe mapOf(
                2 to "value2",
                3 to "value3"
            )
        }
    }

    @Test
    fun `should ignore incorrect fields`() {
        val json = """{
                    "0":"0200",
                    "num":"value",
                    "2":"value2"
                }""".trimIndent()

        with(objectMapper.readValue(json, IsoMessageDto::class.java)) {
            mti shouldBe 0x0200
            fields shouldBe mapOf(2 to "value2")
        }
    }
}