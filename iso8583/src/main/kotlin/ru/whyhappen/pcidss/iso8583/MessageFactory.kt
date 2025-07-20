package ru.whyhappen.pcidss.iso8583

import ru.whyhappen.pcidss.iso8583.mti.MessageClass
import ru.whyhappen.pcidss.iso8583.mti.MessageFunction
import ru.whyhappen.pcidss.iso8583.mti.MessageOrigin

/**
 * Factory for creating and parsing ISO8583 messages.
 *
 * See [jreactive-iso8583](https://github.com/kpavlov/jreactive-8583).
 *
 */
interface MessageFactory<T> {
    /**
     * Creates a new message with the specified MTI.
     */
    fun newMessage(mti: Int): T

    /**
     * Creates a new message with the default message version, the given message class, function, and role
     * that specify its MTI.
     */
    fun newMessage(
        messageClass: MessageClass,
        messageFunction: MessageFunction,
        messageOrigin: MessageOrigin,
    ): T

    /**
     * Creates a new message with a default message origin (i.e. role).
     */
    fun newMessage(messageClass: MessageClass, messageFunction: MessageFunction): T

    /**
     * Creates a response to the given incoming message.
     */
    fun createResponse(requestMessage: T): T

    /**
     * Parses a message from its binary representation.
     */
    fun parseMessage(buf: ByteArray): T
}
