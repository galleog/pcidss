package ru.wayhappen.pcidss.way4

import ru.whyhappen.pcidss.iso8583.MessageFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration

/**
 * Autoconfiguration for Way4's flavor of ISO8583.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "iso8583", name = ["flavor"], havingValue = "way4")
@ConditionalOnBean(MessageFactory::class)
class Way4AutoConfiguration