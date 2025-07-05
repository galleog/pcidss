package ru.whyhappen.pcidss.iso8583.prefix

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test

/**
 * Tests for [AsciiHexPrefixerDecorator].
 */
@ExtendWith(MockKExtension::class)
class AsciiHexPrefixerDecoratorTest {
    @RelaxedMockK
    private lateinit var prefixer: Prefixer
    @InjectMockKs
    private lateinit var decorator: AsciiHexPrefixerDecorator

    @Test
    fun `should encode length`() {
        val lengthPrefix = byteArrayOf(0x08)

        every { prefixer.encodeLength(any<Int>(), any<Int>()) } returns lengthPrefix

        decorator.encodeLength(5, 4) shouldBe lengthPrefix

        verify { prefixer.encodeLength(10, 8) }
    }

    @Test
    fun `should decode length`() {
        val bytes = byteArrayOf(0x06, 0x10, 0xAA.toByte(), 0xFC.toByte())

        every { prefixer.decodeLength(any<Int>(), any<ByteArray>()) } returns (6 to 3)

        decorator.decodeLength(3, bytes) shouldBe (3 to 3)

        verify { prefixer.decodeLength(6, bytes) }
    }
}