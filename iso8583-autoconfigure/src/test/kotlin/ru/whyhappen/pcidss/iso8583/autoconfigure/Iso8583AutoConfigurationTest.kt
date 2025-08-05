package ru.whyhappen.pcidss.iso8583.autoconfigure

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.whyhappen.pcidss.iso8583.DefaultMessageFactory
import ru.whyhappen.pcidss.iso8583.MessageFactory
import ru.whyhappen.pcidss.iso8583.encode.Encoders
import ru.whyhappen.pcidss.iso8583.fields.Bitmap
import ru.whyhappen.pcidss.iso8583.fields.StringField
import ru.whyhappen.pcidss.iso8583.mti.ISO8583Version
import ru.whyhappen.pcidss.iso8583.prefix.Ascii
import ru.whyhappen.pcidss.iso8583.prefix.Hex
import ru.whyhappen.pcidss.iso8583.reactor.server.Iso8583Server
import ru.whyhappen.pcidss.iso8583.spec.IsoMessageSpec
import ru.whyhappen.pcidss.iso8583.spec.MessageSpec
import ru.whyhappen.pcidss.iso8583.spec.Spec
import kotlin.test.Test

/**
 * Tests for [Iso8583AutoConfiguration].
 */
class Iso8583AutoConfigurationTest {
    companion object {
        private val messageSpec = MessageSpec(
            mapOf(
                2 to Bitmap(
                    spec = Spec(
                        length = 16,
                        description = "Bitmap",
                        encoder = Encoders.hexToAscii,
                        prefixer = Hex.fixed
                    )
                )
            )
        )
    }

    @Test
    fun `should create default beans`() {
        val contextRunner = ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(Iso8583AutoConfiguration::class.java))

        contextRunner.run { context ->
            with(context.getBean(MessageFactory::class.java)) {
                shouldBeInstanceOf<DefaultMessageFactory>()
                isoVersion shouldBe ISO8583Version.V1987
                spec shouldBe IsoMessageSpec.spec
            }

            with(context.getBean(Iso8583Server::class.java)) {
                isStarted.shouldBeTrue()
            }
        }
    }

    @Test
    fun `should create customized message factory`() {
        val contextRunner = ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(Iso8583AutoConfiguration::class.java))
            .withUserConfiguration(Config::class.java)
            .withPropertyValues(
                "iso8583.message.specs=classpath:test-spec.json",
                "iso8583.message.isoVersion=V1993"
            )

        contextRunner.run { context ->
            with(context.getBean(MessageFactory::class.java)) {
                shouldBeInstanceOf<DefaultMessageFactory>()
                isoVersion shouldBe ISO8583Version.V1993
                spec shouldBe IsoMessageSpec.spec + messageSpec + MessageSpec(
                    mapOf(
                        127 to StringField(
                            spec = Spec(
                                length = 999,
                                description = "Reserved (Private)",
                                encoder = Encoders.ascii,
                                prefixer = Ascii.LLL
                            )
                        )
                    )
                )
            }
        }
    }

    @Configuration
    class Config {
        @Bean
        fun messageSpec(): MessageSpec = messageSpec
    }
}