package ru.whyhappen.pcidss.bpc

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import ru.whyhappen.pcidss.iso8583.MessageFactory
import ru.whyhappen.pcidss.iso8583.spec.MessageSpec

/**
 * Autoconfiguration for BPC's flavor of ISO8583.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(MessageFactory::class)
@ConditionalOnProperty(prefix = "iso8583", name = ["flavor"], havingValue = "bpc")
class BpcAutoConfiguration {
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    fun messageSpec(): MessageSpec = BpcMessageSpec.spec
}