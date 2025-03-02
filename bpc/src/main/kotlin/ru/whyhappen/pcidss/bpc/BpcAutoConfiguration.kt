package ru.whyhappen.pcidss.bpc

import com.github.kpavlov.jreactive8583.iso.MessageClass
import com.github.kpavlov.jreactive8583.iso.MessageFactory
import com.github.kpavlov.jreactive8583.iso.MessageFunction
import com.solab.iso8583.IsoMessage
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.whyhappen.pcidss.iso8583.spi.netty.ServerConnectorConfigurer

/**
 * Autoconfiguration for BPC's flavor of ISO8583.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "iso8583", name = ["flavor"], havingValue = "bpc")
@ConditionalOnBean(MessageFactory::class)
class BpcAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun connectorConfigurer(messageFactory: MessageFactory<IsoMessage>): ServerConnectorConfigurer<IsoMessage> {
        // I'm not sure it's needed because as far as I understand only clients should send logon messages
        return ServerConnectorConfigurer({
            messageFactory.newMessage(
                MessageClass.NETWORK_MANAGEMENT,
                MessageFunction.REQUEST
            ).apply {
                updateValue(24, 801)
            }
        })
    }
}