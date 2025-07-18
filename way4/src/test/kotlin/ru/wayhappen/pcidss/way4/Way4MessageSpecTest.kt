package ru.wayhappen.pcidss.way4

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvFileSource
import ru.whyhappen.pcidss.iso8583.DefaultMessageFactory
import ru.whyhappen.pcidss.iso8583.encode.Encoders.binary
import ru.whyhappen.pcidss.iso8583.fields.BinaryField
import ru.whyhappen.pcidss.iso8583.fields.StringField
import ru.whyhappen.pcidss.iso8583.mti.ISO8583Version
import ru.whyhappen.pcidss.iso8583.mti.MessageOrigin
import ru.whyhappen.pcidss.iso8583.prefix.Bcd
import ru.whyhappen.pcidss.iso8583.spec.HexPacker
import ru.whyhappen.pcidss.iso8583.spec.MessageSpec
import ru.whyhappen.pcidss.iso8583.spec.Spec

/**
 * Tests for [Way4MessageSpec].
 */
@OptIn(ExperimentalStdlibApi::class)
class Way4MessageSpecTest {
    private val spec = Way4MessageSpec.spec +
            MessageSpec(
                mapOf(
                    2 to BinaryField(
                        spec = Spec(
                            length = 40,
                            description = "Primary Account Number (PAN)",
                            encoder = binary,
                            prefixer = Bcd.LL,
                            packer = HexPacker(40, binary, Bcd.LL)
                        )
                    )
                )
            )

    private val messageFactory = DefaultMessageFactory(
        ISO8583Version.V1987,
        MessageOrigin.ACQUIRER,
        spec
    )

    @ParameterizedTest
    @CsvFileSource(resources = ["/way4-messages.csv"], numLinesToSkip = 1)
    fun `should parse Way4 messages`(message: String, json: String) {
        val bytes = message.substring(4)
            .hexToByteArray()
        val values = jacksonObjectMapper().readValue(json, HashMap::class.java)
        val ids = values.keys
            .filter { it != "0" }
            .map { it.toString().toInt() }
            .toSet()

        with(messageFactory.parseMessage(bytes)) {
            mti shouldBe values["0"].toString().toInt(16)
            fields.keys shouldContainExactly ids

            for (id in ids) {
                val field = spec.fields[id]
                val value = values[id.toString()].toString()
                val expected = when {
                    field is BinaryField -> value.uppercase()
                    field is StringField && field.spec.padder != null -> value.trimStart('0')
                    else -> value
                }

                getFieldValue(id, String::class.java) shouldBe expected
            }
        }
    }
}