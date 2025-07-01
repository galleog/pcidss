package ru.whyhappen.pcidss.iso

import ru.whyhappen.pcidss.iso8583.MessageFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration

/**
 * Autoconfiguration for standard ISO8583.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "iso8583", name = ["flavor"], havingValue = "iso")
@ConditionalOnBean(MessageFactory::class)
class IsoAutoConfiguration