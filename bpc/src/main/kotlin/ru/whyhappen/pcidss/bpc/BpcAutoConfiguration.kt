package ru.whyhappen.pcidss.bpc

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.whyhappen.pcidss.iso8583.MessageFactory
import ru.whyhappen.pcidss.iso8583.spec.MessageSpec

/**
 * Autoconfiguration for BPC's flavor of ISO8583.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "iso8583", name = ["flavor"], havingValue = "bpc")
@ConditionalOnBean(MessageFactory::class)
class BpcAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(MessageSpec::class)
    fun messageSpec(): MessageSpec = BpcMessageSpec.spec
}