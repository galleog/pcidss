package ru.whyhappen.pcidss.iso8583.reactor.netty.handler

import ru.whyhappen.pcidss.iso8583.IsoMessage

/**
 * Handles exceptions thrown when processing ISO messages.
 */
fun interface ExceptionHandler {
    /**
     * Creates an outgoing ISO message when an exception occurs.
     *
     * @param message the incoming ISO message
     * @param cause the thrown exception
     * @return the response message or `null` if no response should be sent
     */
    suspend fun handleException(message: IsoMessage, cause: Throwable): IsoMessage?
}