package ru.whyhappen.pcidss.iso8583

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nfeld.jsonpathkt.kotlinx.resolvePathAsStringOrNull
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlin.test.Test

/**
 * Tests for [MtiSerializer].
 */
class MtiSerializerTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `should serialize an integer value to a hex string`() {
        val testCases = mapOf(
            0x0100 to "0100",
            0x0110 to "0110",
            0x0400 to "0400",
            0x0410 to "0410",
            0x0800 to "0800",
            0x0810 to "0810",
            0 to "0000",
            0xFFFF to "ffff"
        )

        for ((value, expected) in testCases) {
            with(
                Json.parseToJsonElement(
                    objectMapper.writeValueAsString(TestClass(value))
                )
            ) {
                resolvePathAsStringOrNull("$.value") shouldBe expected
            }
        }
    }

    @Test
    fun `should serialize null values`() {
        with(
            Json.parseToJsonElement(
                objectMapper.writeValueAsString(TestClass())
            )
        ) {
            resolvePathAsStringOrNull("$.value").shouldBeNull()
        }
    }

    private data class TestClass(
        @JsonSerialize(using = MtiSerializer::class)
        val value: Int? = null
    )
}
