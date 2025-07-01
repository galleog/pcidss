package ru.whyhappen.pcidss.iso8583.encode

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

/**
 * Tests for [AsciiEncoder].
 */
class AsciiEncoderTest {
    private val encoder = AsciiEncoder()
    private val validBytes = "hello".toByteArray()
    private val invalidBytes = "hello, 世界!".toByteArray()

    @Test
    fun `should encode ASCII bytes`() {
        encoder.encode(validBytes) shouldBe validBytes
    }

    @Test
    fun `shouldn't encode non-ASCII bytes`() {
        val exception = shouldThrow<EncoderException> { encoder.encode(invalidBytes) }
        exception.message shouldContain "Invalid ASCII character"
    }

    @Test
    fun `should decode ASCII bytes`() {
        val (decoded, read) = encoder.decode(validBytes, 3)
        decoded shouldBe "hel".toByteArray()
        read shouldBe 3
    }

    @Test
    fun `shouldn't decode non-ASCII bytes`() {
        val exception = shouldThrow<EncoderException> { encoder.decode(invalidBytes, invalidBytes.size) }
        exception.message shouldContain "Invalid ASCII character"
    }
}