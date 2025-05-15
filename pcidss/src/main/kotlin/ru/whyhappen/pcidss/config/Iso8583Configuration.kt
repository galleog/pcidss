package ru.whyhappen.pcidss.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kpavlov.jreactive8583.iso.J8583MessageFactory
import com.github.kpavlov.jreactive8583.iso.MessageFactory
import com.solab.iso8583.IsoMessage
import com.solab.iso8583.TraceNumberGenerator
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.web.reactive.function.client.WebClient
import ru.whyhappen.pcidss.core.iso8583.ExternalIsoMessageHandler
import ru.whyhappen.pcidss.core.service.Encryptor
import ru.whyhappen.pcidss.core.service.KeyRepository
import ru.whyhappen.pcidss.core.service.TokenService
import ru.whyhappen.pcidss.core.service.bcfips.BcFipsEncryptor
import ru.whyhappen.pcidss.core.service.redis.RedisTokenService
import ru.whyhappen.pcidss.iso8583.api.j8583.CurrentTimeTraceNumberGenerator
import ru.whyhappen.pcidss.iso8583.api.j8583.config.JsonResourceMessageFactoryConfigurer
import ru.whyhappen.pcidss.core.iso8583.IsoMessageCustomizer
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.handler.IsoMessageHandler

/**
* Common configuration for ISO8583 server and client.
*/
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(Iso8583Properties::class)
class Iso8583Configuration {
    @Bean
    fun messageFactory(
        properties: Iso8583Properties,
        objectMapper: ObjectMapper,
        traceNumberGenerator: ObjectProvider<TraceNumberGenerator>
    ): MessageFactory<IsoMessage> {
        val messageFactory = JsonResourceMessageFactoryConfigurer<IsoMessage>(objectMapper, properties.message.configs)
            .createMessageFactory()
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
            }
        return J8583MessageFactory(messageFactory, properties.message.isoVersion, properties.message.role)
    }

    @Bean
    fun encryptor(): Encryptor = BcFipsEncryptor()

    @Bean
    fun tokenService(
        redisTemplate: ReactiveRedisTemplate<ByteArray, String>,
        keyRepository: KeyRepository,
        encryptor: Encryptor
    ): TokenService = RedisTokenService(redisTemplate, keyRepository, encryptor)

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    fun generalIsoMessageHandler(
        properties: Iso8583Properties,
        messageFactory: MessageFactory<IsoMessage>,
        tokenService: TokenService,
        webClient: WebClient,
        customizer: ObjectProvider<IsoMessageCustomizer>
    ): IsoMessageHandler {
        return ExternalIsoMessageHandler(
            properties.message.sensitiveDataFields,
            messageFactory,
            tokenService,
            webClient,
            customizer.ifAvailable
        )
    }
}