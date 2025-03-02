package ru.whyhappen.pcidss.iso8583.spi.j8583.config

import com.solab.iso8583.IsoMessage
import com.solab.iso8583.MessageFactory
import ru.whyhappen.pcidss.iso8583.spi.j8583.config.model.Iso8583Config

/**
 * Abstract base implementation of [MessageFactoryConfigurer].
 */
abstract class AbstractMessageFactoryConfigurer<T : IsoMessage> : MessageFactoryConfigurer<T> {
    override fun createMessageFactory(): MessageFactory<T> = MessageFactory<T>().also { configure(it) }

    /**
     * Applies [Iso8583Config]s to a message factory.
     */
    protected open fun applyConfigs(messageFactory: MessageFactory<T>, configs: List<Iso8583Config>) {
        for (config in configs) {
            applyConfig(messageFactory, config)
        }
    }

    private fun applyConfig(messageFactory: MessageFactory<T>, config: Iso8583Config) {
//        TODO("Not yet implemented")
    }
}