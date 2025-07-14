package ru.whyhappen.pcidss.bpc

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvFileSource
import ru.whyhappen.pcidss.iso8583.DefaultMessageFactory
import ru.whyhappen.pcidss.iso8583.fields.BinaryField
import ru.whyhappen.pcidss.iso8583.fields.StringField
import ru.whyhappen.pcidss.iso8583.mti.ISO8583Version
import ru.whyhappen.pcidss.iso8583.mti.MessageOrigin

/**
 * Tests for [BpcMessageSpec].
 */
@OptIn(ExperimentalStdlibApi::class)
class BpcMessageSpecTest {
    private val messageFactory = DefaultMessageFactory(
        ISO8583Version.V1993,
        MessageOrigin.ACQUIRER,
        BpcMessageSpec.spec
    )

    @ParameterizedTest
    @CsvFileSource(resources = ["/bpc-messages.csv"], numLinesToSkip = 1)
    fun `should parse BPC messages`(message: String, json: String) {
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
                val field = BpcMessageSpec.spec.fields[id]
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