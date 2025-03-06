package ru.whyhappen.pcidss.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kpavlov.jreactive8583.ConnectorConfigurer
import com.github.kpavlov.jreactive8583.iso.J8583MessageFactory
import com.github.kpavlov.jreactive8583.iso.MessageFactory
import com.github.kpavlov.jreactive8583.server.Iso8583Server
import com.github.kpavlov.jreactive8583.server.ServerConfiguration
import com.solab.iso8583.IsoMessage
import com.solab.iso8583.TraceNumberGenerator
import io.netty.bootstrap.ServerBootstrap
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.whyhappen.pcidss.bc.KeyManager
import ru.whyhappen.pcidss.iso8583.spi.j8583.CurrentTimeTraceNumberGenerator
import ru.whyhappen.pcidss.iso8583.spi.j8583.config.JsonResourceMessageFactoryConfigurer
import ru.whyhappen.pcidss.iso8583.spi.server.Iso8583ServerBootstrap

/**
 * Configuration for [Iso8583Server].
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(Iso8583Properties::class)
class Iso8583Configuration {
    @Bean
    fun iso8583Server(
        properties: Iso8583Properties,
        configurer: ObjectProvider<ConnectorConfigurer<ServerConfiguration, ServerBootstrap>>,
        messageFactory: MessageFactory<IsoMessage>
    ): Iso8583Server<IsoMessage> {
        val configuration = ServerConfiguration.newBuilder()
            .addEchoMessageListener(properties.connection.addEchoMessageListener)
            .idleTimeout(properties.connection.idleTimeout)
            .replyOnError(properties.connection.replyOnError)
            .addLoggingHandler(properties.connection.addLoggingHandler)
            .logSensitiveData(properties.connection.logSensitiveData)
            .sensitiveDataFields(*properties.message.sensitiveDataFields.toIntArray())
            .describeFieldsInLog(properties.connection.logFieldDescription)
            .build()
        val server = Iso8583Server(properties.connection.port, configuration, messageFactory)
        server.configurer = configurer.ifAvailable
        return server
    }

    @Bean
    fun messageFactory(
        properties: Iso8583Properties,
        objectMapper: ObjectMapper,
        traceNumberGenerator: TraceNumberGenerator
    ): MessageFactory<IsoMessage> {
        val messageFactory =
            JsonResourceMessageFactoryConfigurer<IsoMessage>(objectMapper, properties.message.configs)
                .createMessageFactory()

        // set message factory properties
        messageFactory.traceNumberGenerator = traceNumberGenerator
        messageFactory.assignDate = properties.message.assignDate
        messageFactory.isBinaryHeader = properties.message.binaryHeader
        messageFactory.isBinaryFields = properties.message.binaryFields
        messageFactory.etx = properties.message.etx
        messageFactory.ignoreLastMissingField = properties.message.ignoreLastMissingField
        messageFactory.isForceSecondaryBitmap = properties.message.forceSecondaryBitmap
        messageFactory.isUseBinaryBitmap = properties.message.binaryBitmap
        messageFactory.isForceStringEncoding = properties.message.forceStringEncoding
        messageFactory.characterEncoding = properties.message.characterEncoding
        messageFactory.isVariableLengthFieldsInHex = properties.message.variableLengthFieldsInHex
        messageFactory.characterEncoding = properties.message.characterEncoding
        messageFactory.isUseDateTimeApi = true
        return J8583MessageFactory(messageFactory, properties.message.isoVersion, properties.message.role)
    }

    @Bean
    fun iso8583ServerBootstrap(server: Iso8583Server<IsoMessage>) = Iso8583ServerBootstrap(server)

    @Bean
    fun traceNumberGenerator(): TraceNumberGenerator = CurrentTimeTraceNumberGenerator()
}