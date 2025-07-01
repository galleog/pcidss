package ru.whyhappen.pcidss.iso8583.reactor.netty.handler

import ru.whyhappen.pcidss.iso8583.IsoMessage

/**
 * Handler for [IsoMessage]s that only supports some of their types.
 */
interface IsoMessageHandler {
    /**
     * Indicates if this handler can handle the incoming message.
     *
     * @param isoMessage the ISO message to check
     * @return `true` if the message should be handled; `false` otherwise
     */
    fun supports(isoMessage: IsoMessage): Boolean

    /**
     * Handles the incoming message.
     *
     * @param inbound the incoming ISO message
     * @return the response message or `null` if no response should be sent
     */
    suspend fun onMessage(inbound: IsoMessage): IsoMessage?
}