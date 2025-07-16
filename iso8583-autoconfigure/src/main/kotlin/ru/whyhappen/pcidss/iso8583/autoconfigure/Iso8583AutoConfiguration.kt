package ru.whyhappen.pcidss.iso8583.autoconfigure

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import ru.whyhappen.pcidss.iso8583.DefaultMessageFactory
import ru.whyhappen.pcidss.iso8583.IsoMessage
import ru.whyhappen.pcidss.iso8583.MessageFactory
import ru.whyhappen.pcidss.iso8583.autoconfigure.server.Iso8583ServerConfiguration
import ru.whyhappen.pcidss.iso8583.spec.IsoMessageSpec
import ru.whyhappen.pcidss.iso8583.spec.MessageSpec

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
        spec: MessageSpec
    ): MessageFactory<IsoMessage> = DefaultMessageFactory(
        properties.message.isoVersion,
        properties.message.role,
        spec
    )

    @Bean
    @ConditionalOnMissingBean(MessageSpec::class)
    fun messageSpec(): MessageSpec = IsoMessageSpec.spec
}