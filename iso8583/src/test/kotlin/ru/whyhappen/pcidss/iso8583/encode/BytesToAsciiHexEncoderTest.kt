package ru.whyhappen.pcidss.iso8583.encode

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Tests for [BytesToAsciiHexEncoder].
 */
class BytesToAsciiHexEncoderTest {
    private val encoder = BytesToAsciiHexEncoder()

    @Test
    fun `should encode a byte array to ASCII Hex`() {
        val data = byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte())
        encoder.encode(data) shouldBe "AABBCC".toByteArray()
    }

    @Test
    fun `should decode an ASCII Hex string to bytes`() {
        val (decoded, read) = encoder.decode("30BAFC".toByteArray(), 2)
        decoded shouldBe byteArrayOf(0x30, 0xBA.toByte())
        read shouldBe 4
    }
}