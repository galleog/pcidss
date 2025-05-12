package ru.whyhappen.pcidss.iso8583

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nfeld.jsonpathkt.kotlinx.resolvePathAsStringOrNull
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
        json.resolvePathAsStringOrNull("$.mti") shouldBe "1644"
        json.resolvePathAsStringOrNull("$.fields.2") shouldBe "value2"
        json.resolvePathAsStringOrNull("$.fields.3") shouldBe "value3"
    }

    @Test
    fun `should deserialize IsoMessageDto`() {
        val json = """{
                    "mti":"0210",
                    "fields":{
                        "2":"value2",
                        "3":"value3"
                    }
                }""".trimIndent()

        with(objectMapper.readValue(json, IsoMessageDto::class.java)) {
            mti shouldBe 0x0210
            fields shouldBe mapOf(
                2 to "value2",
                3 to "value3"
            )
        }
    }
}