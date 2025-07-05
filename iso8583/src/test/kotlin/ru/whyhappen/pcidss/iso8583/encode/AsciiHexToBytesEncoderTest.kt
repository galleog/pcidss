package ru.whyhappen.pcidss.iso8583.encode

import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * Tests for [AsciiHexToBytesEncoder].
 */
class AsciiHexToBytesEncoderTest {
    private val encoder = AsciiHexToBytesEncoder()

    @Test
    fun `should encode an ASCII HEX string to byte array`() {
        val data = byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte())
        val hex = "AABBCC".toByteArray()

        encoder.encode(hex) shouldBe data
    }

    @Test
    fun `should decode bytes to ASCII HEX`() {
        val data = byteArrayOf(0x30, 0xC5.toByte(), 0xF2.toByte())
        val hex = "30C5F2".toByteArray()

        val (decoded, read) = encoder.decode(data, 3)
        decoded shouldBe hex
        read shouldBe 3
    }
}