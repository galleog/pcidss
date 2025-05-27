package ru.whyhappen.pcidss.iso8583.api.j8583.config

import com.solab.iso8583.IsoMessage
import com.solab.iso8583.IsoType
import com.solab.iso8583.IsoValue
import com.solab.iso8583.MessageFactory
import com.solab.iso8583.codecs.CompositeField
import com.solab.iso8583.parse.*
import com.solab.iso8583.parse.date.Date10ParseInfo
import com.solab.iso8583.parse.date.TimeParseInfo
import com.solab.iso8583.util.HexCodec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainAnyOf
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldMatchEach
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.maps.shouldMatchExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.springframework.test.util.ReflectionTestUtils
import ru.whyhappen.pcidss.iso8583.api.j8583.LooseMessageFactory
import ru.whyhappen.pcidss.iso8583.api.j8583.config.model.*
import java.time.*
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import com.solab.iso8583.parse.temporal.Date10ParseInfo as TemporalDate10ParseInfo
import com.solab.iso8583.parse.temporal.Date4ParseInfo as TemporalDate4ParseInfo
import com.solab.iso8583.parse.temporal.DateExpParseInfo as TemporalDateExpParseInfo
import com.solab.iso8583.parse.temporal.TimeParseInfo as TemporalTimeParseInfo

/**
 * Tests for [AbstractMessageFactoryConfigurer].
 */
class AbstractMessageFactoryConfigurerTest {
    private lateinit var messageFactory: MessageFactory<IsoMessage>

    @BeforeTest
    fun setUp() {
        messageFactory = LooseMessageFactory()
    }

    @Test
    fun `should configure headers`() {
        val config = Iso8583Config(
            headers = listOf(
                Header("0200", null, null, "ISO015000050"),
                Header("400", "200", null, null),
                Header("800", null, false, "ISO015000015"),
                Header("0810", "0800", null, null),
                Header("0280", null, true, "ffffffff")
            )
        )

        TestConfigurer(config).configure(messageFactory)
        with(messageFactory) {
            getIsoHeader(0x200) shouldBe config.headers[0].value
            getIsoHeader(0x400) shouldBe config.headers[0].value
            getIsoHeader(0x800) shouldBe config.headers[2].value
            getIsoHeader(0x0810) shouldBe config.headers[2].value
            getIsoHeader(0x0280).shouldBeNull()
            getBinaryIsoHeader(0x200).shouldBeNull()
            getBinaryIsoHeader(0x400).shouldBeNull()
            getBinaryIsoHeader(0x800).shouldBeNull()
            getBinaryIsoHeader(0x0810).shouldBeNull()
            getBinaryIsoHeader(0x0280) shouldBe HexCodec.hexDecode(config.headers[4].value)
        }
    }

    @Test
    fun `should fail if a header refers to nonexistent one`() {
        val config = Iso8583Config(headers = listOf(Header("0210", "200", null, null)))
        shouldThrow<IllegalArgumentException> {
            TestConfigurer(config).configure(messageFactory)
        }
    }

    @Test
    fun `should fail if header type is invalid`() {
        val configs = listOf(
            Iso8583Config(headers = listOf(Header("x200", null, null, null))),
            Iso8583Config(headers = listOf(Header("0000", null, null, null))),
            Iso8583Config(headers = listOf(Header("0010", null, null, null))),
            Iso8583Config(headers = listOf(Header("5200", null, null, null))),
            Iso8583Config(headers = listOf(Header("0900", null, null, null))),
            Iso8583Config(headers = listOf(Header("0010", null, null, null))),
            Iso8583Config(headers = listOf(Header("2206", null, null, null)))
        )

        for (config in configs) {
            shouldThrow<IllegalArgumentException> {
                TestConfigurer(config).configure(messageFactory)
            }
        }
    }

    @Test
    fun `should configure templates`() {
        val config = Iso8583Config(
            templates = listOf(
                Template(
                    null, null, listOf(
                        Field(39, "NUMERIC", 2, null, "01")
                    )
                ),
                Template(
                    "200", null, listOf(
                        Field(3, "NUMERIC", 6, null, "65000"),
                        Field(32, "LLVAR", null, null, "456"),
                        Field(43, "ALPHA", 5, null, "TEST1"),
                        Field(60, "LLLVAR", null, null, "B456PRO1+000"),
                        Field(102, "LLVAR", null, null, "ABCD")
                    )
                ),
                Template("0210", "untyped"),
                Template(
                    "400", "0200", listOf(
                        Field(90, "ALPHA", 42, null, "BLA"),
                        Field(102, "exclude", null, null, null)
                    )
                )
            )
        )

        TestConfigurer(config).configure(messageFactory)

        messageFactory.getMessageTemplate(-1) shouldMatchFields mapOf(
            39 to IsoValue(IsoType.NUMERIC, "01", 2)
        )
        messageFactory.getMessageTemplate(0x0200) shouldMatchFields mapOf(
            3 to IsoValue(IsoType.NUMERIC, "65000", 6),
            32 to IsoValue(IsoType.LLVAR, "456"),
            43 to IsoValue(IsoType.ALPHA, "TEST1", 5),
            60 to IsoValue(IsoType.LLLVAR, "B456PRO1+000"),
            102 to IsoValue(IsoType.LLVAR, "ABCD")
        )
        messageFactory.getMessageTemplate(0x0210) shouldMatchFields mapOf(
            39 to IsoValue(IsoType.NUMERIC, "01", 2)
        )
        messageFactory.getMessageTemplate(0x0400) shouldMatchFields mapOf(
            3 to IsoValue(IsoType.NUMERIC, "65000", 6),
            32 to IsoValue(IsoType.LLVAR, "456"),
            43 to IsoValue(IsoType.ALPHA, "TEST1", 5),
            60 to IsoValue(IsoType.LLLVAR, "B456PRO1+000"),
            90 to IsoValue(IsoType.ALPHA, "BLA", 42),
        )
    }

    @Test
    fun `should fail if a template refers to nonexistent one`() {
        val config = Iso8583Config(
            templates = listOf(
                Template("0210", "200", listOf())
            )
        )
        shouldThrow<IllegalArgumentException> {
            TestConfigurer(config).configure(messageFactory)
        }
    }

    @Test
    fun `should configure a template with composite fields`() {
        val config = Iso8583Config(
            templates = listOf(
                Template(
                    "100", null, listOf(
                        Field(
                            10, "LLLVAR", null, null, null, listOf(
                                Field(1, "ALPHA", 5, null, "abcde"),
                                Field(2, "LLVAR", null, null, "llvar"),
                                Field(3, "NUMERIC", 5, null, "12345"),
                                Field(4, "ALPHA", 1, null, "X"),
                            )
                        )
                    )
                )
            )
        )

        TestConfigurer(config).configure(messageFactory)

        val template = messageFactory.getMessageTemplate(0x100)
        (2..128).filter(template::hasField) shouldBe listOf(10)
        template.getObjectValue<CompositeField>(10).values shouldContainExactly listOf(
            IsoValue(IsoType.ALPHA, "abcde", 5),
            IsoValue(IsoType.LLVAR, "llvar"),
            IsoValue(IsoType.NUMERIC, "12345", 5),
            IsoValue(IsoType.ALPHA, "X", 1),
        )
    }

    @Test
    fun `should configure a template with nested composite fields`() {
        val config = Iso8583Config(
            templates = listOf(
                Template(
                    "100", null, listOf(
                        Field(
                            10, "LLLVAR", null, null, null, listOf(
                                Field(1, "ALPHA", 5, null, "abcde"),
                                Field(
                                    2, "LLVAR", null, null, null, listOf(
                                        Field(1, "ALPHA", 2, null, "FG"),
                                        Field(2, "ALPHA", 2, null, "hi"),
                                        Field(
                                            3, "LLVAR", null, null, null, listOf(
                                                Field(1, "ALPHA", 3, null, "123"),
                                                Field(2, "ALPHA", 2, null, "45"),
                                            )
                                        )
                                    )
                                ),
                                Field(3, "NUMERIC", 5, null, "67890"),
                                Field(4, "ALPHA", 1, null, "J")
                            )
                        )
                    )
                ),
                Template(
                    "101", "100", listOf(
                        Field(
                            11, "LLLVAR", null, null, null, listOf(
                                Field(1, "ALPHA", 3, null, "klm"),
                                Field(
                                    2, "LLVAR", null, null, null, listOf(
                                        Field(1, "ALPHA", 2, null, "NO"),
                                        Field(
                                            2, "LLVAR", null, null, null, listOf(
                                                Field(1, "ALPHA", 3, null, "098"),
                                                Field(2, "ALPHA", 2, null, "76"),
                                            )
                                        )
                                    )
                                ),
                                Field(3, "NUMERIC", 5, null, "54321")
                            )
                        )
                    )
                )
            )
        )

        TestConfigurer(config).configure(messageFactory)

        val template100 = messageFactory.getMessageTemplate(0x100)
        (2..128).filter(template100::hasField) shouldBe listOf(10)

        val composite10 = template100.getObjectValue<CompositeField>(10)
        val nestedComposite1 = composite10.run {
            values shouldHaveSize 4
            values shouldContainAnyOf listOf(
                IsoValue(IsoType.ALPHA, "abcde", 5),
                IsoValue(IsoType.NUMERIC, "67890", 5),
                IsoValue(IsoType.ALPHA, "J", 1)
            )
            values[1].value
        }

        val nestedComposite2 = nestedComposite1.run {
            shouldBeInstanceOf<CompositeField>()
            values shouldHaveSize 3
            values shouldContainAnyOf listOf(
                IsoValue(IsoType.ALPHA, "FG", 2),
                IsoValue(IsoType.ALPHA, "hi", 2)
            )
            values[2].value
        }

        nestedComposite2 shouldNotBeNull {
            shouldBeInstanceOf<CompositeField>()
            values shouldContainExactly listOf(
                IsoValue(IsoType.ALPHA, "123", 3),
                IsoValue(IsoType.ALPHA, "45", 2)
            )
        }

        val template101 = messageFactory.getMessageTemplate(0x101)
        (2..128).filter(template101::hasField) shouldBe listOf(10, 11)
        template101.getObjectValue<CompositeField>(10).shouldBeInstanceOf<CompositeField>()

        val composite11 = template101.getObjectValue<CompositeField>(11)
        val nestedComposite3 = composite11.run {
            values shouldHaveSize 3
            values shouldContainAnyOf listOf(
                IsoValue(IsoType.ALPHA, "klm", 3),
                IsoValue(IsoType.NUMERIC, "54321", 5)
            )
            values[1].value
        }

        val nestedComposite4 = nestedComposite3.run {
            shouldBeInstanceOf<CompositeField>()
            values shouldHaveSize 2
            values shouldContainAnyOf listOf(
                IsoValue(IsoType.ALPHA, "NO", 2)
            )
            values[1].value
        }

        nestedComposite4 shouldNotBeNull {
            shouldBeInstanceOf<CompositeField>()
            values shouldContainExactly listOf(
                IsoValue(IsoType.ALPHA, "098", 3),
                IsoValue(IsoType.ALPHA, "76", 2)
            )
        }
    }

    @Test
    fun `should configure a template without any value`() {
        val config = Iso8583Config(
            templates = listOf(
                Template(
                    "200", null, listOf(
                        Field(3, "NUMERIC", 6, null, null)
                    )
                )
            )
        )

        TestConfigurer(config).configure(messageFactory)

        messageFactory.getMessageTemplate(0x200) shouldMatchFields mapOf(3 to IsoValue(IsoType.NUMERIC, "", 6))
    }

    @Test
    fun `should configure a template with old dates`() {
        val config = Iso8583Config(
            templates = listOf(
                Template(
                    "300", null, listOf(
                        Field(4, "DATE4", null, null, "0125"),
                        Field(5, "DATE6", null, null, "730125"),
                        Field(6, "DATE10", null, null, "0125213456"),
                        Field(7, "DATE12", null, null, "730125213456"),
                        Field(8, "DATE14", null, null, "19730125213456"),
                        Field(9, "DATE_EXP", null, null, "2506"),
                        Field(10, "TIME", null, null, "213456"),
                        Field(11, "TIME", null, "America/Mexico_City", "213456"),
                        Field(12, "DATE10", null, "EET", "0125213456"),
                    )
                )
            )
        )

        TestConfigurer(config).configure(messageFactory)

        messageFactory.getMessageTemplate(0x300) shouldNotBeNull {
            shouldMatchFields(
                mapOf(
                    4 to IsoValue(IsoType.DATE4, "0125"),
                    5 to IsoValue(IsoType.DATE6, "730125"),
                    6 to IsoValue(IsoType.DATE10, "0125213456"),
                    7 to IsoValue(IsoType.DATE12, "730125213456"),
                    8 to IsoValue(IsoType.DATE14, "19730125213456"),
                    9 to IsoValue(IsoType.DATE_EXP, "2506"),
                    10 to IsoValue(IsoType.TIME, "213456"),
                    11 to IsoValue(IsoType.TIME, "213456"),
                    12 to IsoValue(IsoType.DATE10, "0125213456")
                )
            )
            getAt<Any>(11).timeZone.id shouldBe "America/Mexico_City"
            getAt<Any>(12).timeZone.id shouldBe "EET"
        }
    }

    @Test
    fun `should configure a template with DateTime API`() {
        val config = Iso8583Config(
            templates = listOf(
                Template(
                    "300", null, listOf(
                        Field(4, "DATE4", null, null, "0125"),
                        Field(5, "DATE6", null, null, "730125"),
                        Field(6, "DATE10", null, null, "0125213456"),
                        Field(7, "DATE12", null, null, "730125213456"),
                        Field(8, "DATE14", null, null, "19730125213456"),
                        Field(9, "DATE_EXP", null, null, "2506"),
                        Field(10, "TIME", null, null, "213456"),
                        Field(11, "TIME", null, "America/Mexico_City", "213456"),
                        Field(12, "DATE10", null, "EET", "0125213456"),
                    )
                )
            )
        )

        messageFactory.isUseDateTimeApi = true
        TestConfigurer(config).configure(messageFactory)

        val now = ZonedDateTime.now()
        messageFactory.getMessageTemplate(0x300) shouldNotBeNull {
            hasEveryField(4, 5, 6, 7, 8, 9, 10, 11, 12).shouldBeTrue()

            getObjectValue<Any>(4) shouldBe LocalDate.now()
                .withMonth(1)
                .withDayOfMonth(25)
            getObjectValue<Any>(5) shouldBe LocalDate.of(1973, 1, 25)
            getObjectValue<Any>(6) shouldBe ZonedDateTime.of(
                2025,
                1,
                25,
                21,
                34,
                56,
                0,
                ZoneId.systemDefault()
            ).withYear(now.year)
            getObjectValue<Any>(7) shouldBe ZonedDateTime.of(
                1973,
                1,
                25,
                21,
                34,
                56,
                0,
                ZoneId.systemDefault()
            )
            getObjectValue<Any>(8) shouldBe ZonedDateTime.of(
                1973,
                1,
                25,
                21,
                34,
                56,
                0,
                ZoneId.systemDefault()
            )
            getObjectValue<Any>(9) shouldBe LocalDate.of(2025, 6, 1)
            getObjectValue<Any>(10) shouldBe OffsetTime.of(
                21,
                34,
                56,
                0,
                getZoneOffset(ZoneId.systemDefault())
            )
            getObjectValue<Any>(11) shouldBe OffsetTime.of(
                21,
                34,
                56,
                0,
                getZoneOffset(ZoneId.of("America/Mexico_City"))
            )
            getObjectValue<Any>(12) shouldBe ZonedDateTime.of(
                2025,
                1,
                25,
                21,
                34,
                56,
                0,
                ZoneId.of("EET")
            ).withYear(now.year)
        }
    }

    @Test
    fun `should apply data to parse messages`() {
        val config = Iso8583Config(
            parses = listOf(
                Parse(
                    null, null, listOf(
                        Field(3, "NUMERIC", 6),
                        Field(4, "AMOUNT"),
                        Field(7, "DATE10"),
                        Field(11, "NUMERIC", 6),
                        Field(12, "TIME"),
                        Field(13, "DATE4"),
                        Field(17, "DATE_EXP"),
                        Field(32, "LLVAR"),
                        Field(35, "LLVAR"),
                        Field(37, "NUMERIC", 12)
                    )
                ),
                Parse(
                    "0210", "untyped", listOf(
                        Field(35, "exclude"),
                        Field(37, "exclude"),
                        Field(38, "NUMERIC", 6)
                    )
                ),
                Parse(
                    "800", null, listOf(
                        Field(3, "ALPHA", 6),
                        Field(12, "DATE4"),
                        Field(17, "DATE4")
                    )
                ),
                Parse(
                    "810", "800", listOf(
                        Field(17, "exclude"),
                        Field(39, "ALPHA", 2)
                    )
                )
            )
        )

        messageFactory.isUseDateTimeApi = true
        TestConfigurer(config).configure(messageFactory)
        val parseMap = ReflectionTestUtils.getField(messageFactory, "parseMap")
        parseMap.shouldBeInstanceOf<Map<Int, Map<Int, FieldParseInfo>>>()

        parseMap[-1] shouldNotBeNull {
            shouldMatchExactly(
                3 to {
                    it.shouldBeInstanceOf<NumericParseInfo>()
                    it.type shouldBe IsoType.NUMERIC
                    it.length shouldBe 6
                },
                4 to {
                    it.shouldBeInstanceOf<AmountParseInfo>()
                    it.type shouldBe IsoType.AMOUNT
                    it.length shouldBe 12
                },
                7 to {
                    it.shouldBeInstanceOf<TemporalDate10ParseInfo>()
                    it.type shouldBe IsoType.DATE10
                    it.length shouldBe 10
                },
                11 to {
                    it.shouldBeInstanceOf<NumericParseInfo>()
                    it.type shouldBe IsoType.NUMERIC
                    it.length shouldBe 6
                },
                12 to {
                    it.shouldBeInstanceOf<TemporalTimeParseInfo>()
                    it.type shouldBe IsoType.TIME
                    it.length shouldBe 6
                },
                13 to {
                    it.shouldBeInstanceOf<TemporalDate4ParseInfo>()
                    it.type shouldBe IsoType.DATE4
                    it.length shouldBe 4
                },
                17 to {
                    it.shouldBeInstanceOf<TemporalDateExpParseInfo>()
                    it.type shouldBe IsoType.DATE_EXP
                    it.length shouldBe 4
                },
                32 to {
                    it.shouldBeInstanceOf<LlvarParseInfo>()
                    it.type shouldBe IsoType.LLVAR
                    it.length shouldBe 0
                },
                35 to {
                    it.shouldBeInstanceOf<LlvarParseInfo>()
                    it.type shouldBe IsoType.LLVAR
                    it.length shouldBe 0
                },
                37 to {
                    it.shouldBeInstanceOf<NumericParseInfo>()
                    it.type shouldBe IsoType.NUMERIC
                    it.length shouldBe 12
                }
            )
        }

        parseMap[0x0210] shouldNotBeNull {
            shouldMatchExactly(
                3 to {
                    it.shouldBeInstanceOf<NumericParseInfo>()
                    it.type shouldBe IsoType.NUMERIC
                    it.length shouldBe 6
                },
                4 to {
                    it.shouldBeInstanceOf<AmountParseInfo>()
                    it.type shouldBe IsoType.AMOUNT
                    it.length shouldBe 12
                },
                7 to {
                    it.shouldBeInstanceOf<TemporalDate10ParseInfo>()
                    it.type shouldBe IsoType.DATE10
                    it.length shouldBe 10
                },
                11 to {
                    it.shouldBeInstanceOf<NumericParseInfo>()
                    it.type shouldBe IsoType.NUMERIC
                    it.length shouldBe 6
                },
                12 to {
                    it.shouldBeInstanceOf<TemporalTimeParseInfo>()
                    it.type shouldBe IsoType.TIME
                    it.length shouldBe 6
                },
                13 to {
                    it.shouldBeInstanceOf<TemporalDate4ParseInfo>()
                    it.type shouldBe IsoType.DATE4
                    it.length shouldBe 4
                },
                17 to {
                    it.shouldBeInstanceOf<TemporalDateExpParseInfo>()
                    it.type shouldBe IsoType.DATE_EXP
                    it.length shouldBe 4
                },
                32 to {
                    it.shouldBeInstanceOf<LlvarParseInfo>()
                    it.type shouldBe IsoType.LLVAR
                    it.length shouldBe 0
                },
                38 to {
                    it.shouldBeInstanceOf<NumericParseInfo>()
                    it.type shouldBe IsoType.NUMERIC
                    it.length shouldBe 6
                }
            )
        }

        parseMap[0x800] shouldNotBeNull {
            shouldMatchExactly(
                3 to {
                    it.shouldBeInstanceOf<AlphaParseInfo>()
                    it.type shouldBe IsoType.ALPHA
                    it.length shouldBe 6
                },
                12 to {
                    it.shouldBeInstanceOf<TemporalDate4ParseInfo>()
                    it.type shouldBe IsoType.DATE4
                    it.length shouldBe 4
                },
                17 to {
                    it.shouldBeInstanceOf<TemporalDate4ParseInfo>()
                    it.type shouldBe IsoType.DATE4
                    it.length shouldBe 4
                }
            )
        }

        parseMap[0x810] shouldNotBeNull {
            shouldMatchExactly(
                3 to {
                    it.shouldBeInstanceOf<AlphaParseInfo>()
                    it.type shouldBe IsoType.ALPHA
                    it.length shouldBe 6
                },
                12 to {
                    it.shouldBeInstanceOf<TemporalDate4ParseInfo>()
                    it.type shouldBe IsoType.DATE4
                    it.length shouldBe 4
                },
                39 to {
                    it.shouldBeInstanceOf<AlphaParseInfo>()
                    it.type shouldBe IsoType.ALPHA
                    it.length shouldBe 2
                }
            )
        }
    }

    @Test
    fun `should apply data to parse composite fields`() {
        val config = Iso8583Config(
            parses = listOf(
                Parse(
                    "100", null, listOf(
                        Field(
                            10, "LLLVAR", subFields = listOf(
                                Field(1, "ALPHA", 5),
                                Field(2, "LLVAR"),
                                Field(3, "NUMERIC", 5),
                                Field(4, "ALPHA", 1)
                            )
                        )
                    )
                )
            )
        )

        TestConfigurer(config).configure(messageFactory)
        val parseMap = ReflectionTestUtils.getField(messageFactory, "parseMap")
        parseMap.shouldBeInstanceOf<Map<Int, Map<Int, FieldParseInfo>>>()

        parseMap[0x100] shouldNotBeNull {
            shouldMatchExactly(
                10 to { composite ->
                    composite.shouldBeInstanceOf<LllvarParseInfo>()
                    composite.type shouldBe IsoType.LLLVAR
                    composite.length shouldBe 0
                    composite.decoder shouldNotBeNull {
                        this.shouldBeInstanceOf<CompositeField>()
                        this.parsers.shouldMatchEach(
                            listOf(
                                {
                                    it.shouldBeInstanceOf<AlphaParseInfo>()
                                    it.type shouldBe IsoType.ALPHA
                                    it.length shouldBe 5
                                },
                                {
                                    it.shouldBeInstanceOf<LlvarParseInfo>()
                                    it.type shouldBe IsoType.LLVAR
                                    it.length shouldBe 0
                                },
                                {
                                    it.shouldBeInstanceOf<NumericParseInfo>()
                                    it.type shouldBe IsoType.NUMERIC
                                    it.length shouldBe 5
                                },
                                {
                                    it.shouldBeInstanceOf<AlphaParseInfo>()
                                    it.type shouldBe IsoType.ALPHA
                                    it.length shouldBe 1
                                }
                            ))
                    }
                }
            )
        }
    }

    @Test
    fun `should apply data to parse nested composite fields`() {
        val config = Iso8583Config(
            parses = listOf(
                Parse(
                    "100", null, listOf(
                        Field(
                            10, "LLLVAR", subFields = listOf(
                                Field(1, "ALPHA", 5),
                                Field(
                                    2, "LLVAR", subFields = listOf(
                                        Field(1, "ALPHA", 2),
                                        Field(2, "ALPHA", 2),
                                        Field(
                                            3, "LLVAR", subFields = listOf(
                                                Field(1, "ALPHA", 3),
                                                Field(2, "ALPHA", 2)
                                            )
                                        )
                                    )
                                ),
                                Field(3, "ALPHA", 1)
                            )
                        )
                    )
                )
            )
        )

        TestConfigurer(config).configure(messageFactory)
        val parseMap = ReflectionTestUtils.getField(messageFactory, "parseMap")
        parseMap.shouldBeInstanceOf<Map<Int, Map<Int, FieldParseInfo>>>()

        parseMap[0x100] shouldNotBeNull {
            shouldMatchExactly(
                10 to { composite ->
                    composite.shouldBeInstanceOf<LllvarParseInfo>()
                    composite.type shouldBe IsoType.LLLVAR
                    composite.length shouldBe 0
                    composite.decoder shouldNotBeNull {
                        this.shouldBeInstanceOf<CompositeField>()
                        this.parsers.shouldMatchEach(
                            listOf(
                                {
                                    it.shouldBeInstanceOf<AlphaParseInfo>()
                                    it.type shouldBe IsoType.ALPHA
                                    it.length shouldBe 5
                                },
                                { nested1 ->
                                    nested1.shouldBeInstanceOf<LlvarParseInfo>()
                                    nested1.type shouldBe IsoType.LLVAR
                                    nested1.length shouldBe 0
                                    nested1.decoder shouldNotBeNull {
                                        this.shouldBeInstanceOf<CompositeField>()
                                        this.parsers.shouldMatchEach(
                                            listOf(
                                                {
                                                    it.shouldBeInstanceOf<AlphaParseInfo>()
                                                    it.type shouldBe IsoType.ALPHA
                                                    it.length shouldBe 2
                                                },
                                                {
                                                    it.shouldBeInstanceOf<AlphaParseInfo>()
                                                    it.type shouldBe IsoType.ALPHA
                                                    it.length shouldBe 2
                                                },
                                                { nested2 ->
                                                    nested2.shouldBeInstanceOf<LlvarParseInfo>()
                                                    nested2.type shouldBe IsoType.LLVAR
                                                    nested2.length shouldBe 0
                                                    nested2.decoder shouldNotBeNull {
                                                        this.shouldBeInstanceOf<CompositeField>()
                                                        this.parsers.shouldMatchEach(
                                                            listOf(
                                                                {
                                                                    it.shouldBeInstanceOf<AlphaParseInfo>()
                                                                    it.type shouldBe IsoType.ALPHA
                                                                    it.length shouldBe 3
                                                                },
                                                                {
                                                                    it.shouldBeInstanceOf<AlphaParseInfo>()
                                                                    it.type shouldBe IsoType.ALPHA
                                                                    it.length shouldBe 2
                                                                }
                                                            )
                                                        )
                                                    }

                                                }
                                            )
                                        )
                                    }
                                },
                                {
                                    it.shouldBeInstanceOf<AlphaParseInfo>()
                                    it.type shouldBe IsoType.ALPHA
                                    it.length shouldBe 1
                                }
                            )
                        )
                    }
                }
            )
        }
    }

    @Test
    fun `should apply data to parse messages that extend each other several times`() {
        val config = Iso8583Config(
            parses = listOf(
                Parse(
                    "200", null, listOf(
                        Field(2, "LLVAR"),
                        Field(3, "NUMERIC", 6),
                        Field(4, "NUMERIC", 12),
                        Field(7, "DATE10"),
                        Field(18, "NUMERIC", 4),
                        Field(32, "LLVAR"),
                        Field(37, "NUMERIC", 12),
                        Field(41, "ALPHA", 8)
                    )
                ),
                Parse(
                    "210", "200", listOf(
                        Field(39, "ALPHA", 2),
                        Field(62, "LLLVAR")
                    )
                ),
                Parse(
                    "400", "200", listOf(
                        Field(62, "LLLVAR")
                    )
                ),
                Parse(
                    "410", "400", listOf(
                        Field(39, "ALPHA", 2),
                        Field(61, "LLLVAR")
                    )
                )
            )
        )

        messageFactory.isUseDateTimeApi = true
        TestConfigurer(config).configure(messageFactory)
        val parseMap = ReflectionTestUtils.getField(messageFactory, "parseMap")
        parseMap.shouldBeInstanceOf<Map<Int, Map<Int, FieldParseInfo>>>()

        parseMap[0x200] shouldNotBeNull {
            shouldMatchExactly(
                2 to {
                    it.shouldBeInstanceOf<LlvarParseInfo>()
                    it.type shouldBe IsoType.LLVAR
                    it.length shouldBe 0
                },
                3 to {
                    it.shouldBeInstanceOf<NumericParseInfo>()
                    it.type shouldBe IsoType.NUMERIC
                    it.length shouldBe 6
                },
                4 to {
                    it.shouldBeInstanceOf<NumericParseInfo>()
                    it.type shouldBe IsoType.NUMERIC
                    it.length shouldBe 12
                },
                7 to {
                    it.shouldBeInstanceOf<TemporalDate10ParseInfo>()
                    it.type shouldBe IsoType.DATE10
                    it.length shouldBe 10
                },
                18 to {
                    it.shouldBeInstanceOf<NumericParseInfo>()
                    it.type shouldBe IsoType.NUMERIC
                    it.length shouldBe 4
                },
                32 to {
                    it.shouldBeInstanceOf<LlvarParseInfo>()
                    it.type shouldBe IsoType.LLVAR
                    it.length shouldBe 0
                },
                37 to {
                    it.shouldBeInstanceOf<NumericParseInfo>()
                    it.type shouldBe IsoType.NUMERIC
                    it.length shouldBe 12
                },
                41 to {
                    it.shouldBeInstanceOf<AlphaParseInfo>()
                    it.type shouldBe IsoType.ALPHA
                    it.length shouldBe 8
                }
            )
        }

        parseMap[0x210] shouldNotBeNull {
            shouldMatchExactly(
                2 to {
                    it.shouldBeInstanceOf<LlvarParseInfo>()
                    it.type shouldBe IsoType.LLVAR
                    it.length shouldBe 0
                },
                3 to {
                    it.shouldBeInstanceOf<NumericParseInfo>()
                    it.type shouldBe IsoType.NUMERIC
                    it.length shouldBe 6
                },
                4 to {
                    it.shouldBeInstanceOf<NumericParseInfo>()
                    it.type shouldBe IsoType.NUMERIC
                    it.length shouldBe 12
                },
                7 to {
                    it.shouldBeInstanceOf<TemporalDate10ParseInfo>()
                    it.type shouldBe IsoType.DATE10
                    it.length shouldBe 10
                },
                18 to {
                    it.shouldBeInstanceOf<NumericParseInfo>()
                    it.type shouldBe IsoType.NUMERIC
                    it.length shouldBe 4
                },
                32 to {
                    it.shouldBeInstanceOf<LlvarParseInfo>()
                    it.type shouldBe IsoType.LLVAR
                    it.length shouldBe 0
                },
                37 to {
                    it.shouldBeInstanceOf<NumericParseInfo>()
                    it.type shouldBe IsoType.NUMERIC
                    it.length shouldBe 12
                },
                39 to {
                    it.shouldBeInstanceOf<AlphaParseInfo>()
                    it.type shouldBe IsoType.ALPHA
                    it.length shouldBe 2
                },
                41 to {
                    it.shouldBeInstanceOf<AlphaParseInfo>()
                    it.type shouldBe IsoType.ALPHA
                    it.length shouldBe 8
                },
                62 to {
                    it.shouldBeInstanceOf<LllvarParseInfo>()
                    it.type shouldBe IsoType.LLLVAR
                    it.length shouldBe 0
                }
            )
        }

        parseMap[0x400] shouldNotBeNull {
            shouldMatchExactly(
                2 to {
                    it.shouldBeInstanceOf<LlvarParseInfo>()
                    it.type shouldBe IsoType.LLVAR
                    it.length shouldBe 0
                },
                3 to {
                    it.shouldBeInstanceOf<NumericParseInfo>()
                    it.type shouldBe IsoType.NUMERIC
                    it.length shouldBe 6
                },
                4 to {
                    it.shouldBeInstanceOf<NumericParseInfo>()
                    it.type shouldBe IsoType.NUMERIC
                    it.length shouldBe 12
                },
                7 to {
                    it.shouldBeInstanceOf<TemporalDate10ParseInfo>()
                    it.type shouldBe IsoType.DATE10
                    it.length shouldBe 10
                },
                18 to {
                    it.shouldBeInstanceOf<NumericParseInfo>()
                    it.type shouldBe IsoType.NUMERIC
                    it.length shouldBe 4
                },
                32 to {
                    it.shouldBeInstanceOf<LlvarParseInfo>()
                    it.type shouldBe IsoType.LLVAR
                    it.length shouldBe 0
                },
                37 to {
                    it.shouldBeInstanceOf<NumericParseInfo>()
                    it.type shouldBe IsoType.NUMERIC
                    it.length shouldBe 12
                },
                41 to {
                    it.shouldBeInstanceOf<AlphaParseInfo>()
                    it.type shouldBe IsoType.ALPHA
                    it.length shouldBe 8
                },
                62 to {
                    it.shouldBeInstanceOf<LllvarParseInfo>()
                    it.type shouldBe IsoType.LLLVAR
                    it.length shouldBe 0
                }
            )
        }

        parseMap[0x410] shouldNotBeNull {
            shouldMatchExactly(
                2 to {
                    it.shouldBeInstanceOf<LlvarParseInfo>()
                    it.type shouldBe IsoType.LLVAR
                    it.length shouldBe 0
                },
                3 to {
                    it.shouldBeInstanceOf<NumericParseInfo>()
                    it.type shouldBe IsoType.NUMERIC
                    it.length shouldBe 6
                },
                4 to {
                    it.shouldBeInstanceOf<NumericParseInfo>()
                    it.type shouldBe IsoType.NUMERIC
                    it.length shouldBe 12
                },
                7 to {
                    it.shouldBeInstanceOf<TemporalDate10ParseInfo>()
                    it.type shouldBe IsoType.DATE10
                    it.length shouldBe 10
                },
                18 to {
                    it.shouldBeInstanceOf<NumericParseInfo>()
                    it.type shouldBe IsoType.NUMERIC
                    it.length shouldBe 4
                },
                32 to {
                    it.shouldBeInstanceOf<LlvarParseInfo>()
                    it.type shouldBe IsoType.LLVAR
                    it.length shouldBe 0
                },
                37 to {
                    it.shouldBeInstanceOf<NumericParseInfo>()
                    it.type shouldBe IsoType.NUMERIC
                    it.length shouldBe 12
                },
                39 to {
                    it.shouldBeInstanceOf<AlphaParseInfo>()
                    it.type shouldBe IsoType.ALPHA
                    it.length shouldBe 2
                },
                41 to {
                    it.shouldBeInstanceOf<AlphaParseInfo>()
                    it.type shouldBe IsoType.ALPHA
                    it.length shouldBe 8
                },
                61 to {
                    it.shouldBeInstanceOf<LllvarParseInfo>()
                    it.type shouldBe IsoType.LLLVAR
                    it.length shouldBe 0
                },
                62 to {
                    it.shouldBeInstanceOf<LllvarParseInfo>()
                    it.type shouldBe IsoType.LLLVAR
                    it.length shouldBe 0
                }
            )
        }
    }

    @Test
    fun `should apply data to parse a message that extends another one with the same composite field`() {
        val config = Iso8583Config(
            parses = listOf(
                Parse(
                    "100", null, listOf(
                        Field(4, "AMOUNT")
                    )
                ),
                Parse(
                    "200", "100", listOf(
                        Field(
                            62, "LLLVAR", subFields = listOf(
                                Field(4, "ALPHA", 13)
                            )
                        )
                    )
                )
            )
        )

        TestConfigurer(config).configure(messageFactory)
        val parseMap = ReflectionTestUtils.getField(messageFactory, "parseMap")
        parseMap.shouldBeInstanceOf<Map<Int, Map<Int, FieldParseInfo>>>()

        parseMap[0x100] shouldNotBeNull {
            shouldMatchExactly(
                4 to {
                    it.shouldBeInstanceOf<AmountParseInfo>()
                    it.type shouldBe IsoType.AMOUNT
                    it.length shouldBe 12
                }
            )
        }

        parseMap[0x200] shouldNotBeNull {
            shouldMatchExactly(
                4 to {
                    it.shouldBeInstanceOf<AmountParseInfo>()
                    it.type shouldBe IsoType.AMOUNT
                    it.length shouldBe 12
                },
                62 to { composite ->
                    composite.shouldBeInstanceOf<LllvarParseInfo>()
                    composite.type shouldBe IsoType.LLLVAR
                    composite.length shouldBe 0
                    composite.decoder shouldNotBeNull {
                        this.shouldBeInstanceOf<CompositeField>()
                        this.parsers.shouldMatchEach(
                            listOf(
                                {
                                    it.shouldBeInstanceOf<AlphaParseInfo>()
                                    it.type shouldBe IsoType.ALPHA
                                    it.length shouldBe 13
                                }
                            )
                        )
                    }
                }
            )
        }
    }

    @Test
    fun `should apply data to parse old dates with different timezones`() {
        val config = Iso8583Config(
            parses = listOf(
                Parse(
                    "0110", null, listOf(
                        Field(7, "DATE10", tz = "UTC"),
                        Field(12, "TIME", tz = "UTC-5"),
                        Field(13, "TIME"),
                    )
                )
            )
        )

        TestConfigurer(config).configure(messageFactory)
        val parseMap = ReflectionTestUtils.getField(messageFactory, "parseMap")
        parseMap.shouldBeInstanceOf<Map<Int, Map<Int, FieldParseInfo>>>()

        parseMap[0x0110] shouldNotBeNull {
            shouldMatchExactly(
                7 to {
                    it.shouldBeInstanceOf<Date10ParseInfo>()
                    it.type shouldBe IsoType.DATE10
                    it.length shouldBe 10
                    it.timeZone shouldBe TimeZone.getTimeZone("UTC")
                },
                12 to {
                    it.shouldBeInstanceOf<TimeParseInfo>()
                    it.type shouldBe IsoType.TIME
                    it.length shouldBe 6
                    it.timeZone shouldBe TimeZone.getTimeZone("UTC-5")
                },
                13 to {
                    it.shouldBeInstanceOf<TimeParseInfo>()
                    it.type shouldBe IsoType.TIME
                    it.length shouldBe 6
                    it.timeZone.shouldBeNull()
                }
            )
        }
    }

    @Test
    fun `should apply data to parse temporals with different timezones`() {
        val config = Iso8583Config(
            parses = listOf(
                Parse(
                    "0110", null, listOf(
                        Field(7, "DATE10", tz = "UTC"),
                        Field(12, "TIME", tz = "UTC-5"),
                        Field(13, "TIME"),
                    )
                )
            )
        )

        messageFactory.isUseDateTimeApi = true
        TestConfigurer(config).configure(messageFactory)
        val parseMap = ReflectionTestUtils.getField(messageFactory, "parseMap")
        parseMap.shouldBeInstanceOf<Map<Int, Map<Int, FieldParseInfo>>>()

        parseMap[0x0110] shouldNotBeNull {
            shouldMatchExactly(
                7 to {
                    it.shouldBeInstanceOf<TemporalDate10ParseInfo>()
                    it.type shouldBe IsoType.DATE10
                    it.length shouldBe 10
                    it.zoneId shouldBe ZoneId.of("UTC")
                },
                12 to {
                    it.shouldBeInstanceOf<TemporalTimeParseInfo>()
                    it.type shouldBe IsoType.TIME
                    it.length shouldBe 6
                    it.zoneId shouldBe ZoneId.of("UTC-5")
                },
                13 to {
                    it.shouldBeInstanceOf<TemporalTimeParseInfo>()
                    it.type shouldBe IsoType.TIME
                    it.length shouldBe 6
                    it.zoneId shouldBe ZoneId.systemDefault()
                }
            )
        }
    }

    private infix fun IsoMessage.shouldMatchFields(fields: Map<Int, IsoValue<*>>) {
        val actual = (2..128).asSequence()
            .filter(::hasField)
            .associateWith { getAt<Any>(it) }
        actual shouldContainExactly fields
    }

    private fun getZoneOffset(zoneId: ZoneId): ZoneOffset = zoneId.rules.getOffset(Instant.now())

}

class TestConfigurer(val config: Iso8583Config) : AbstractMessageFactoryConfigurer<IsoMessage>() {
    override fun createIsoMessage(type: Int) = IsoMessage().apply {
        this.type = type
    }

    override fun configure(messageFactory: MessageFactory<IsoMessage>) {
        applyConfigs(messageFactory, listOf(config))
    }
}