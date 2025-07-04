package ru.whyhappen.pcidss.iso8583.spec

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verifySequence
import org.junit.jupiter.api.extension.ExtendWith
import ru.whyhappen.pcidss.iso8583.encode.Encoder
import ru.whyhappen.pcidss.iso8583.pad.Padder
import ru.whyhappen.pcidss.iso8583.prefix.Prefixer
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Tests for [DefaultPacker].
 */
@ExtendWith(MockKExtension::class)
class DefaultPackerTest {
    @MockK
    private lateinit var encoder: Encoder
    @MockK
    private lateinit var prefixer: Prefixer
    @MockK
    private lateinit var padder: Padder

    private lateinit var packer: Packer
    private val length = 5

    @BeforeTest
    fun setUp() {
        packer = DefaultPacker(length, encoder, prefixer, padder)
    }

    @Test
    fun `should pack bytes`() {
        val bytes = byteArrayOf(0x12, 0xAA.toByte(), 0xBA.toByte())
        val hex = "12AABA".toByteArray()
        val lengthPrefix = "3".toByteArray()

        every { padder.pad(any<ByteArray>(), length) } returns bytes
        every { encoder.encode(any<ByteArray>()) } returns hex
        every { prefixer.encodeLength(any<Int>(), any<Int>()) } returns lengthPrefix

        packer.pack(bytes) shouldBe lengthPrefix + hex

        verifySequence {
            padder.pad(bytes, length)
            encoder.encode(bytes)
            prefixer.encodeLength(length, bytes.size)
        }
    }

    @Test
    fun `should unpack bytes`() {
        val bytes = byteArrayOf(0x12, 0xAA.toByte(), 0xBA.toByte())
        val hex = "12AABA".toByteArray()
        val lengthPrefix = "3".toByteArray()

        every { prefixer.decodeLength(any<Int>(), any<ByteArray>()) } returns (3 to 1)
        every { prefixer.digits } returns 1
        every { encoder.decode(any<ByteArray>(), any<Int>()) } returns (bytes to 6)
        every { padder.unpad(any<ByteArray>()) } returns bytes

        val (value, read) = packer.unpack(lengthPrefix + hex)
        value shouldBe bytes
        read shouldBe 7

        verifySequence {
            prefixer.decodeLength(length, lengthPrefix + hex)
            prefixer.digits
            encoder.decode(hex, 3)
            padder.unpad(bytes)
        }
    }
}