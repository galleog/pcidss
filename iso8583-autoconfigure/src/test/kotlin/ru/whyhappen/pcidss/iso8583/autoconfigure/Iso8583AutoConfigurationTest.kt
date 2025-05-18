package ru.whyhappen.pcidss.iso8583.autoconfigure

import com.github.kpavlov.jreactive8583.iso.MessageFactory
import io.kotest.matchers.maps.shouldHaveSize
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.handler.EchoMessageHandler
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.server.Iso8583Server
import ru.whyhappen.pcidss.iso8583.autoconfigure.server.Iso8583ServerBootstrap
import kotlin.test.Test

/**
 * Tests for [Iso8583AutoConfiguration].
 */
class Iso8583AutoConfigurationTest {
    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(Iso8583AutoConfiguration::class.java))

    @Test
    fun `should create beans`() {
        contextRunner.run { context ->
            context.getBeansOfType(MessageFactory::class.java) shouldHaveSize 1
            context.getBeansOfType(Iso8583Server::class.java) shouldHaveSize 1
            context.getBeansOfType(Iso8583ServerBootstrap::class.java) shouldHaveSize 1
            context.getBeansOfType(EchoMessageHandler::class.java) shouldHaveSize 1
        }
    }
}