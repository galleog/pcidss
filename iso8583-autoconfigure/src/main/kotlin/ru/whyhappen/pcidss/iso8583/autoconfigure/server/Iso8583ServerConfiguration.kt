package ru.whyhappen.pcidss.iso8583.autoconfigure.server

import com.github.kpavlov.jreactive8583.iso.MessageFactory
import com.github.kpavlov.jreactive8583.server.ServerConfiguration
import com.solab.iso8583.IsoMessage
import io.micrometer.observation.ObservationRegistry
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.handler.DefaultExceptionHandler
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.handler.EchoMessageHandler
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.handler.ExceptionHandler
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.handler.IsoMessageHandler
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.pipeline.IdleEventHandler
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.pipeline.ParseExceptionHandler
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.server.Iso8583Server
import ru.whyhappen.pcidss.iso8583.autoconfigure.Iso8583Properties

/**
 * Configuration for [Iso8583Server].
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Iso8583Server::class)
class Iso8583ServerConfiguration {
    @Bean
    @ConditionalOnMissingBean(Iso8583Server::class)
    fun iso8583Server(
        properties: Iso8583Properties,
        messageFactory: MessageFactory<IsoMessage>,
        messageHandlers: ObjectProvider<IsoMessageHandler>,
        observationRegistry: ObjectProvider<ObservationRegistry>,
        exceptionHandler: ObjectProvider<ExceptionHandler>,
        parseExceptionHandler: ObjectProvider<ParseExceptionHandler>,
        idleEventHandler: ObjectProvider<IdleEventHandler>
    ): Iso8583Server {
        val configuration = ServerConfiguration.newBuilder()
            .addEchoMessageListener(properties.connection.addEchoMessageListener)
            .idleTimeout(properties.connection.idleTimeout)
            .replyOnError(properties.connection.replyOnError)
            .addLoggingHandler(properties.connection.addLoggingHandler)
            .logSensitiveData(properties.connection.logSensitiveData)
            .sensitiveDataFields(*properties.message.sensitiveDataFields.toIntArray())
            .describeFieldsInLog(properties.connection.logFieldDescription)
            .build()

        return Iso8583Server(
            properties.connection.port,
            configuration,
            messageFactory,
            messageHandlers.orderedStream().toList(),
            observationRegistry.ifAvailable ?: ObservationRegistry.NOOP,
            exceptionHandler.ifAvailable ?: DefaultExceptionHandler(messageFactory, true),
            parseExceptionHandler.ifAvailable ?: ParseExceptionHandler(messageFactory, true),
            idleEventHandler.ifAvailable ?: IdleEventHandler(messageFactory)
        )
    }

    @Bean
    fun iso8583Bootstrap(server: Iso8583Server) = Iso8583ServerBootstrap(server)

    @Bean
    @Order(0)
    @ConditionalOnMissingBean(EchoMessageHandler::class)
    fun echoMessageHandler(messageFactory: MessageFactory<IsoMessage>) = EchoMessageHandler(messageFactory)
}