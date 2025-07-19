package ru.wayhappen.pcidss.way4

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
import kotlin.test.Test

/**
 * Tests for [Way4AutoConfiguration].
 */
class Way4AutoConfigurationTest {
    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                Iso8583AutoConfiguration::class.java,
                Way4AutoConfiguration::class.java
            )
        ).withPropertyValues("iso8583.flavor=way4")

    @Test
    fun `should use customized Way4 message factory`() {
        contextRunner.run { context ->
            context.getBeansOfType(Iso8583Server::class.java) shouldHaveSize 1

            with(context.getBean(MessageFactory::class.java)) {
                shouldBeInstanceOf<DefaultMessageFactory>()
                isoVersion shouldBe ISO8583Version.V1987
                spec shouldBe IsoMessageSpec.spec + Way4MessageSpec.spec
            }
        }
    }
}