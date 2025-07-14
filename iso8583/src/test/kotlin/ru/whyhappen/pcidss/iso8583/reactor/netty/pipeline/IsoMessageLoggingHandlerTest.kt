package ru.whyhappen.pcidss.iso8583.reactor.netty.pipeline

import io.kotest.matchers.string.shouldContainInOrder
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.local.LocalChannel
import io.netty.handler.logging.LogLevel
import org.junit.jupiter.api.extension.ExtendWith
import ru.whyhappen.pcidss.iso8583.DefaultIsoMessage
import ru.whyhappen.pcidss.iso8583.IsoMessage
import ru.whyhappen.pcidss.iso8583.encode.AsciiEncoder
import ru.whyhappen.pcidss.iso8583.encode.Encoders
import ru.whyhappen.pcidss.iso8583.fields.Bitmap
import ru.whyhappen.pcidss.iso8583.fields.StringField
import ru.whyhappen.pcidss.iso8583.pad.StartPadder
import ru.whyhappen.pcidss.iso8583.prefix.AsciiFixedPrefixer
import ru.whyhappen.pcidss.iso8583.prefix.AsciiVarPrefixer
import ru.whyhappen.pcidss.iso8583.prefix.Binary
import ru.whyhappen.pcidss.iso8583.reactor.netty.pipeline.IsoMessageLoggingHandler.Companion.MASKED_VALUE
import ru.whyhappen.pcidss.iso8583.spec.MessageSpec
import ru.whyhappen.pcidss.iso8583.spec.Spec
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Tests for [IsoMessageLoggingHandler].
 */
@ExtendWith(MockKExtension::class)
class IsoMessageLoggingHandlerTest {
    companion object {
        private const val EVENT_NAME = "event"
    }

    @RelaxedMockK
    private lateinit var ctx: ChannelHandlerContext

    private lateinit var isoMessage: IsoMessage

    private val pan = "424242424242424242"
    private val transactionAmount = "100"
    private val stan = "234567"
    private val extendedPan = "1234564242424242424242421223456"
    private val maskFields: IntArray = intArrayOf(2, 34)

    private val fields = mapOf(
        0 to StringField(
            spec = Spec(
                4,
                "Message Type Indicator",
                AsciiEncoder(),
                AsciiFixedPrefixer()
            )
        ),
        1 to Bitmap(
            Spec(
                8,
                "Bitmap",
                Encoders.binary,
                Binary.fixed
            )
        ),
        2 to StringField(
            spec = Spec(
                19,
                "Primary Account Number",
                AsciiEncoder(),
                AsciiVarPrefixer(2)
            )
        ),
        4 to StringField(
            spec = Spec(
                12,
                "Transaction Amount",
                AsciiEncoder(),
                AsciiFixedPrefixer(),
                StartPadder('0')
            )
        ),
        11 to StringField(
            spec = Spec(
                6,
                "Systems Trace Audit Number (STAN)",
                AsciiEncoder(),
                AsciiFixedPrefixer()
            )
        ),
        34 to StringField(
            spec = Spec(
                28,
                "Extended Primary Account Number",
                AsciiEncoder(),
                AsciiVarPrefixer(2)
            )
        )
    )

    @BeforeTest
    fun setUp() {
        isoMessage = DefaultIsoMessage(MessageSpec(fields))
            .apply {
                mti = 0x200
                setFieldValue(2, pan)
                setFieldValue(4, transactionAmount)
                setFieldValue(11, stan)
                setFieldValue(34, extendedPan)
            }

        every { ctx.channel() } returns LocalChannel()
    }

    @Test
    fun `should log IsoMessage with full info`() {
        val loggingHandler = IsoMessageLoggingHandler(
            level = LogLevel.DEBUG,
            printSensitiveData = true,
            printFieldDescriptions = true,
            maskedFields = maskFields
        )

        loggingHandler.format(ctx, EVENT_NAME, isoMessage).shouldContainInOrder(
            "MTI: 0200",
            "2: [${fields[2]!!.spec.description}: StringField(${fields[2]!!.spec.length})] = '$pan'",
            "4: [${fields[4]!!.spec.description}: StringField(${fields[4]!!.spec.length})] = '$transactionAmount'",
            "11: [${fields[11]!!.spec.description}: StringField(${fields[11]!!.spec.length})] = '$stan'",
            "34: [${fields[34]!!.spec.description}: StringField(${fields[34]!!.spec.length})] = '$extendedPan'"
        )
    }

    @Test
    fun `should log IsoMessage without field descriptions`() {
        val loggingHandler = IsoMessageLoggingHandler(
            level = LogLevel.DEBUG,
            printSensitiveData = true,
            printFieldDescriptions = false,
            maskedFields = maskFields
        )

        loggingHandler.format(ctx, EVENT_NAME, isoMessage).shouldContainInOrder(
            "MTI: 0200",
            "2: [StringField(${fields[2]!!.spec.length})] = '$pan'",
            "4: [StringField(${fields[4]!!.spec.length})] = '$transactionAmount'",
            "11: [StringField(${fields[11]!!.spec.length})] = '$stan'",
            "34: [StringField(${fields[34]!!.spec.length})] = '$extendedPan'"
        )
    }

    @Test
    fun `should mask sensitive fields`() {
        val loggingHandler = IsoMessageLoggingHandler(
            level = LogLevel.DEBUG,
            printSensitiveData = false,
            printFieldDescriptions = true,
            maskedFields = maskFields
        )

        loggingHandler.format(ctx, EVENT_NAME, isoMessage).shouldContainInOrder(
            "MTI: 0200",
            "2: [${fields[2]!!.spec.description}: StringField(${fields[2]!!.spec.length})] = '$MASKED_VALUE'",
            "4: [${fields[4]!!.spec.description}: StringField(${fields[4]!!.spec.length})] = '$transactionAmount'",
            "11: [${fields[11]!!.spec.description}: StringField(${fields[11]!!.spec.length})] = '$stan'",
            "34: [${fields[34]!!.spec.description}: StringField(${fields[34]!!.spec.length})] = '$MASKED_VALUE'"
        )
    }
}