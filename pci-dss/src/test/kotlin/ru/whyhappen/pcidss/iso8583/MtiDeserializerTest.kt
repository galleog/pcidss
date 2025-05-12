package ru.whyhappen.pcidss.iso8583

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * Tests for [MtiDeserializer].
 */
class MtiDeserializerTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `should deserialize a hex string to an integer`() {
        val json = """{
                "value":"0210"
            }""".trimIndent()

        objectMapper.readValue(json, TestClass::class.java).value shouldBe 0x0210
    }

    @Test
    fun `should deserialize null values`() {
        val json1 = "{}"
        objectMapper.readValue(json1, TestClass::class.java).value.shouldBeNull()

        val json2 = """{
                "value":null
            }""".trimIndent()
        objectMapper.readValue(json2, TestClass::class.java).value.shouldBeNull()
    }

    private data class TestClass(
        @JsonDeserialize(using = MtiDeserializer::class)
        val value: Int? = null
    )
}

