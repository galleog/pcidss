package ru.whyhappen.pcidss.iso8583.autoconfigure.server

import io.micrometer.observation.ObservationRegistry
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import ru.whyhappen.pcidss.iso8583.IsoMessage
import ru.whyhappen.pcidss.iso8583.MessageFactory
import ru.whyhappen.pcidss.iso8583.autoconfigure.Iso8583Properties
import ru.whyhappen.pcidss.iso8583.reactor.netty.handler.DefaultExceptionHandler
import ru.whyhappen.pcidss.iso8583.reactor.netty.handler.EchoMessageHandler
import ru.whyhappen.pcidss.iso8583.reactor.netty.handler.ExceptionHandler
import ru.whyhappen.pcidss.iso8583.reactor.netty.handler.IsoMessageHandler
import ru.whyhappen.pcidss.iso8583.reactor.netty.pipeline.DecoderExceptionHandler
import ru.whyhappen.pcidss.iso8583.reactor.netty.pipeline.IdleEventHandler
import ru.whyhappen.pcidss.iso8583.reactor.server.Iso8583Server
import ru.whyhappen.pcidss.iso8583.reactor.server.ServerConfiguration

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
        decoderExceptionHandler: ObjectProvider<DecoderExceptionHandler>,
        idleEventHandler: ObjectProvider<IdleEventHandler>
    ): Iso8583Server {
        val configuration = ServerConfiguration.newBuilder()
            .maxFrameLength(properties.connection.maxFrameLength)
            .frameLengthFieldLength(properties.connection.frameLengthFieldLength)
            .frameLengthFieldOffset(properties.connection.frameLengthFieldOffset)
            .frameLengthFieldAdjust(properties.connection.frameLengthFieldAdjust)
            .encodeFrameLengthAsString(properties.connection.encodeFrameLengthAsString)
            .addIdleEventHandler(properties.connection.addIdleEventHandler)
            .idleTimeout(properties.connection.idleTimeout)
            .replyOnError(properties.connection.replyOnError)
            .addLoggingHandler(properties.connection.addLoggingHandler)
            .logSensitiveData(properties.connection.logSensitiveData)
            .sensitiveDataFields(*properties.message.sensitiveDataFields.toIntArray())
            .describeFieldsInLog(properties.connection.logFieldDescription)
            .responseCode(properties.message.defaultResponseCode)
            .build()

        return Iso8583Server(
            properties.connection.port,
            configuration,
            messageFactory,
            messageHandlers.orderedStream().toList(),
            observationRegistry.ifAvailable ?: ObservationRegistry.NOOP,
            exceptionHandler.ifAvailable ?: DefaultExceptionHandler(messageFactory, configuration.responseCode),
            decoderExceptionHandler.ifAvailable ?: DecoderExceptionHandler(messageFactory, true),
            idleEventHandler.ifAvailable ?: IdleEventHandler(messageFactory)
        )
    }

    @Bean
    fun iso8583Bootstrap(server: Iso8583Server) = Iso8583ServerBootstrap(server)

    @Bean
    @Order(0)
    @ConditionalOnMissingBean(EchoMessageHandler::class)
    fun echoMessageHandler(properties: Iso8583Properties, messageFactory: MessageFactory<IsoMessage>) =
        EchoMessageHandler(messageFactory, properties.message.defaultResponseCode)
}