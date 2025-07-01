package ru.whyhappen.pcidss.iso8583.pad

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import kotlin.test.BeforeTest
import kotlin.test.Test

class StartPadderTest {
    private lateinit var padder: Padder

    @Nested
    inner class SingleBytePadderTest {
        @BeforeTest
        fun setUp() {
            padder = StartPadder('0')
        }

        @Test
        fun `should pad with single-byte character`() {
            padder.pad(byteArrayOf(1, 2, 3), 5) shouldBe byteArrayOf(0x30, 0x30, 1, 2, 3)
        }

        @Test
        fun `should not pad when original length is sufficient`() {
            val original = byteArrayOf(1, 2, 3)
            padder.pad(original, 3) shouldBe original
        }

        @Test
        fun `should unpad single-byte character correctly`() {
            val padded = byteArrayOf(0x30, 0x30, 0x30, 1, 2, 3)
            padder.unpad(padded) shouldBe byteArrayOf(1, 2, 3)
        }

        @Test
        fun `should handle empty array for padding`() {
            padder.pad(byteArrayOf(), 3) shouldBe byteArrayOf(0x30, 0x30, 0x30)
        }

        @Test
        fun `should handle empty array for unpadding`() {
            val empty = byteArrayOf()
            padder.unpad(empty) shouldBe empty
        }

        @Test
        fun `should uppad to an empty array`() {
            padder.unpad(byteArrayOf(0x30, 0x30)) shouldBe byteArrayOf()
        }
    }

    @Nested
    inner class MultibytePadderTest {
        @BeforeTest
        fun setUp() {
            padder = StartPadder('世')
        }

        @Test
        fun `should pad with multibyte character`() {
            val original = byteArrayOf(1, 2, 3)
            val padBytes = '世'.toString().toByteArray(Charsets.UTF_8)

            val padded = padder.pad(original, 9)

            padded.size shouldBe 9
            padded[0] shouldBe padBytes[0]
            padded[1] shouldBe padBytes[1]
            padded[2] shouldBe padBytes[2]
            padded[3] shouldBe padBytes[0]
            padded[4] shouldBe padBytes[1]
            padded[5] shouldBe padBytes[2]
            padded[6] shouldBe 1
            padded[7] shouldBe 2
            padded[8] shouldBe 3
        }

        @Test
        fun `should unpad with multibyte character`() {
            val content = byteArrayOf(1, 2, 3)
            val padded = padder.pad(content, 9)
            padded.size shouldBe 9

            padder.unpad(padded) shouldBe content
        }
    }
}
