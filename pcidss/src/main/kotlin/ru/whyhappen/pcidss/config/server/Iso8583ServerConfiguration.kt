package ru.whyhappen.pcidss.config.server

import com.github.kpavlov.jreactive8583.iso.MessageFactory
import com.github.kpavlov.jreactive8583.server.ServerConfiguration
import com.solab.iso8583.IsoMessage
import io.micrometer.observation.ObservationRegistry
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.whyhappen.pcidss.config.Iso8583Properties
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.handler.DefaultExceptionHandler
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.handler.ExceptionHandler
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.handler.IsoMessageHandler
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.pipeline.IdleEventHandler
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.pipeline.ParseExceptionHandler
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.server.Iso8583Server

/**
 * Configuration for [Iso8583Server].
 */
@Configuration(proxyBeanMethods = false)
class Iso8583ServerConfiguration {
    @Bean
    fun iso8583Server(
        properties: Iso8583Properties,
        observationRegistry: ObservationRegistry,
        messageFactory: MessageFactory<IsoMessage>,
        messageHandlers: ObjectProvider<IsoMessageHandler>,
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
            observationRegistry,
            configuration,
            messageFactory,
            messageHandlers.orderedStream().toList(),
            exceptionHandler.ifAvailable ?: DefaultExceptionHandler(messageFactory, true),
            parseExceptionHandler.ifAvailable ?: ParseExceptionHandler(messageFactory, true),
            idleEventHandler.ifAvailable ?: IdleEventHandler(messageFactory)
        )
    }

    @Bean
    fun iso9593Bootstrap(server: Iso8583Server) = Iso8583ServerBootstrap(server)
}