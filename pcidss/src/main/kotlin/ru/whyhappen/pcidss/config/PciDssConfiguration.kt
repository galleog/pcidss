package ru.whyhappen.pcidss.config

import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.web.reactive.function.client.WebClient
import ru.whyhappen.pcidss.iso8583.ExternalIsoMessageHandler
import ru.whyhappen.pcidss.iso8583.IsoMessage
import ru.whyhappen.pcidss.iso8583.IsoMessageCustomizer
import ru.whyhappen.pcidss.iso8583.MessageFactory
import ru.whyhappen.pcidss.iso8583.autoconfigure.Iso8583Properties
import ru.whyhappen.pcidss.iso8583.reactor.netty.handler.IsoMessageHandler
import ru.whyhappen.pcidss.service.Encryptor
import ru.whyhappen.pcidss.service.KeyRepository
import ru.whyhappen.pcidss.service.TokenService
import ru.whyhappen.pcidss.service.bcfips.BcFipsEncryptor
import ru.whyhappen.pcidss.service.redis.RedisTokenService

/**
 * Configuration for the PCI DSS module.
 */
@Configuration(proxyBeanMethods = false)
class PciDssConfiguration {
    @Bean
    fun encryptor(): Encryptor = BcFipsEncryptor()

    @Bean
    fun tokenService(
        redisTemplate: ReactiveRedisTemplate<ByteArray, String>,
        keyRepository: KeyRepository,
        encryptor: Encryptor
    ): TokenService = RedisTokenService(redisTemplate, keyRepository, encryptor)

    @Bean
    @ConditionalOnBean(WebClient::class)
    @Order(Ordered.LOWEST_PRECEDENCE)
    fun generalIsoMessageHandler(
        properties: Iso8583Properties,
        messageFactory: MessageFactory<IsoMessage>,
        tokenService: TokenService,
        webClient: WebClient,
        customizers: ObjectProvider<IsoMessageCustomizer>
    ): IsoMessageHandler {
        return ExternalIsoMessageHandler(
            properties.message.sensitiveDataFields,
            messageFactory,
            tokenService,
            webClient,
            customizers.orderedStream().toList()
        )
    }
}