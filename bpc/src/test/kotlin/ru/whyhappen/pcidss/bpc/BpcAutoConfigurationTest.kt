package ru.whyhappen.pcidss.bpc

import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import ru.whyhappen.pcidss.iso8583.DefaultMessageFactory
import ru.whyhappen.pcidss.iso8583.MessageFactory
import ru.whyhappen.pcidss.iso8583.autoconfigure.Iso8583AutoConfiguration
import ru.whyhappen.pcidss.iso8583.mti.ISO8583Version
import ru.whyhappen.pcidss.iso8583.reactor.server.Iso8583Server
import ru.whyhappen.pcidss.iso8583.spec.IsoMessageSpec
import ru.whyhappen.pcidss.iso8583.spec.MessageSpec
import kotlin.test.Test

/**
 * Tests for [BpcAutoConfiguration].
 */
class BpcAutoConfigurationTest {
    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                Iso8583AutoConfiguration::class.java,
                BpcAutoConfiguration::class.java
            )
        ).withPropertyValues("iso8583.flavor=bpc", "iso8583.message.isoVersion=V1993")

    @Test
    fun `should use specific BPC beans`() {
        contextRunner.run { context ->
            context.getBeansOfType(MessageSpec::class.java) shouldHaveSize 1
            context.getBeansOfType(Iso8583Server::class.java) shouldHaveSize 1

            with(context.getBean(MessageFactory::class.java)) {
                shouldBeInstanceOf<DefaultMessageFactory>()
                isoVersion shouldBe ISO8583Version.V1993
                spec shouldBe IsoMessageSpec.spec + BpcMessageSpec.spec
            }
        }
    }
}