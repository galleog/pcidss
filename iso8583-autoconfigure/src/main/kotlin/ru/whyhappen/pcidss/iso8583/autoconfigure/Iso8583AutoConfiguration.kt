package ru.whyhappen.pcidss.iso8583.autoconfigure

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kpavlov.jreactive8583.iso.J8583MessageFactory
import com.github.kpavlov.jreactive8583.iso.MessageFactory
import com.solab.iso8583.IsoMessage
import com.solab.iso8583.TraceNumberGenerator
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import ru.whyhappen.pcidss.iso8583.api.j8583.CurrentTimeTraceNumberGenerator
import ru.whyhappen.pcidss.iso8583.api.j8583.LooseMessageFactory
import ru.whyhappen.pcidss.iso8583.api.j8583.config.JsonResourceMessageFactoryConfigurer
import ru.whyhappen.pcidss.iso8583.autoconfigure.server.Iso8583ServerConfiguration

/**
 * Autoconfiguration for ISO8583 server and client.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(MessageFactory::class)
@Import(Iso8583ServerConfiguration::class)
@EnableConfigurationProperties(Iso8583Properties::class)
class Iso8583AutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(MessageFactory::class)
    fun messageFactory(
        properties: Iso8583Properties,
        objectMapper: ObjectProvider<ObjectMapper>,
        traceNumberGenerator: ObjectProvider<TraceNumberGenerator>
    ): MessageFactory<IsoMessage> {
        val configurer = object : JsonResourceMessageFactoryConfigurer<IsoMessage>(
            objectMapper.ifAvailable ?: jacksonObjectMapper(),
            properties.message.configs
        ) {
            override fun createIsoMessage(type: Int) = IsoMessage().apply { this.type = type }
        }

        val messageFactory = LooseMessageFactory<IsoMessage>()
            .apply {
                // set message factory properties
                this.traceNumberGenerator = traceNumberGenerator.ifAvailable ?: CurrentTimeTraceNumberGenerator()
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

                configurer.configure(this)
            }

        return J8583MessageFactory(messageFactory, properties.message.isoVersion, properties.message.role)
    }
}