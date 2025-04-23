package ru.whyhappen.pcidss.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kpavlov.jreactive8583.iso.J8583MessageFactory
import com.github.kpavlov.jreactive8583.iso.MessageFactory
import com.github.kpavlov.jreactive8583.server.ServerConfiguration
import com.solab.iso8583.IsoMessage
import com.solab.iso8583.TraceNumberGenerator
import io.micrometer.observation.ObservationRegistry
import io.micrometer.tracing.Tracer
import io.micrometer.tracing.handler.DefaultTracingObservationHandler
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.whyhappen.pcidss.iso8583.api.j8583.CurrentTimeTraceNumberGenerator
import ru.whyhappen.pcidss.iso8583.api.j8583.config.JsonResourceMessageFactoryConfigurer
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.handler.*
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.pipeline.IdleEventChannelHandler
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.pipeline.ParseExceptionChannelHandler
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.server.Iso8583Server
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.server.Iso8583ServerBootstrap

/**
 * Configuration for [Iso8583Server].
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(Iso8583Properties::class)
class Iso8583ServerConfiguration {
    @Bean
    fun iso8583Server(
        properties: Iso8583Properties,
        tracer: Tracer,
        messageFactory: MessageFactory<IsoMessage>,
        messageHandlers: ObjectProvider<IsoMessageHandler>,
        exceptionHandler: ObjectProvider<ExceptionHandler>,
        parseExceptionHandler: ObjectProvider<ParseExceptionChannelHandler>,
        idleEventHandler: ObjectProvider<IdleEventChannelHandler>
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
        val observationRegistry = ObservationRegistry.create().apply {
            observationConfig().observationHandler(DefaultTracingObservationHandler(tracer))
        }

        return Iso8583Server(
            properties.connection.port,
            observationRegistry,
            configuration,
            messageFactory,
            messageHandlers.orderedStream().toList(),
            exceptionHandler.ifAvailable ?: DefaultExceptionHandler(messageFactory, true),
            parseExceptionHandler.ifAvailable ?: ParseExceptionChannelHandler(messageFactory, true),
            idleEventHandler.ifAvailable ?: IdleEventChannelHandler(messageFactory)
        )
    }

    @Bean
    fun messageFactory(
        properties: Iso8583Properties,
        objectMapper: ObjectMapper,
        traceNumberGenerator: TraceNumberGenerator
    ): MessageFactory<IsoMessage> {
        val messageFactory = JsonResourceMessageFactoryConfigurer<IsoMessage>(objectMapper, properties.message.configs)
            .createMessageFactory()
            .apply {
                // set message factory properties
                this.traceNumberGenerator = traceNumberGenerator
                assignDate = properties.message.assignDate
                isBinaryHeader = properties.message.binaryHeader
                isBinaryFields = properties.message.binaryFields
                etx = properties.message.etx
                ignoreLastMissingField = properties.message.ignoreLastMissingField
                isForceSecondaryBitmap = properties.message.forceSecondaryBitmap
                isUseBinaryBitmap = properties.message.binaryBitmap
                isForceStringEncoding = properties.message.forceStringEncoding
                characterEncoding = properties.message.characterEncoding
                isVariableLengthFieldsInHex = properties.message.variableLengthFieldsInHex
                characterEncoding = properties.message.characterEncoding
                isUseDateTimeApi = true
            }
        return J8583MessageFactory(messageFactory, properties.message.isoVersion, properties.message.role)
    }

    @Bean
    fun iso9593Bootstrap(server: Iso8583Server) = Iso8583ServerBootstrap(server)

    @Bean
    fun traceNumberGenerator(): TraceNumberGenerator = CurrentTimeTraceNumberGenerator()
}