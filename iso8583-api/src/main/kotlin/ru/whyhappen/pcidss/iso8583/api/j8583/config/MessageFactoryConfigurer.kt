package ru.whyhappen.pcidss.iso8583.api.j8583.config

import com.solab.iso8583.IsoMessage
import com.solab.iso8583.MessageFactory

/**
 * Configurer for a [MessageFactory].
 */
interface MessageFactoryConfigurer<T : IsoMessage> {
    /**
     * Creates and configures a new message factory.
     */
    fun createMessageFactory(): MessageFactory<T>

    /**
     * Configures an existing message factory.
     */
    fun configure(messageFactory: MessageFactory<T>)
}