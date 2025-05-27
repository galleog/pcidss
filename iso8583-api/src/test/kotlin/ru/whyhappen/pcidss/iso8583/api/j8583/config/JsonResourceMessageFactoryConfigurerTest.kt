package ru.whyhappen.pcidss.iso8583.api.j8583.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.solab.iso8583.IsoMessage
import com.solab.iso8583.MessageFactory
import com.solab.iso8583.parse.FieldParseInfo
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.springframework.core.io.ClassPathResource
import org.springframework.test.util.ReflectionTestUtils
import ru.whyhappen.pcidss.iso8583.api.j8583.LooseMessageFactory
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Tests for [JsonResourceMessageFactoryConfigurer].
 */
class JsonResourceMessageFactoryConfigurerTest {
    private lateinit var messageFactory: MessageFactory<IsoMessage>

    @BeforeTest
    fun setUp() {
        messageFactory = LooseMessageFactory<IsoMessage>().apply { isUseDateTimeApi = true }
    }

    @Test
    fun `should configurer a message factory`() {
        val resources = listOf(
            ClassPathResource("config-1.json", this::class.java),
            ClassPathResource("config-2.json", this::class.java)
        )
        createConfigurer(resources).configure(messageFactory)

        val parseMap = ReflectionTestUtils.getField(messageFactory, "parseMap")
        parseMap.shouldBeInstanceOf<Map<Int, Map<Int, FieldParseInfo>>>()

        with(messageFactory) {
            getIsoHeader(0x200) shouldBe "ISO015000050"

            getMessageTemplate(-1) shouldNotBeNull {
                shouldHaveFields(39)
            }
            getMessageTemplate(0x0800) shouldNotBeNull {
                shouldHaveFields(70)
            }

            parseMap.keys shouldBe setOf(-1, 0x0800)
            parseMap[-1] shouldNotBeNull {
                keys shouldBe setOf(3, 4, 7, 11, 12, 13, 15, 17, 32, 35, 37, 41, 43, 48, 49, 60, 61, 100, 102)
            }
            parseMap[0x0800] shouldNotBeNull {
                keys shouldBe setOf(7, 11, 70)
            }
        }
    }

    private fun createConfigurer(resources: List<ClassPathResource>) = JsonResourceMessageFactoryConfigurer(
        jacksonObjectMapper(),
        resources
    ) { type -> IsoMessage().apply { this.type = type } }

    private fun IsoMessage.shouldHaveFields(vararg fields: Int) {
        (2..128).filter(::hasField) shouldBe fields.toList()
    }
}