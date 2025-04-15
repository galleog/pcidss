package ru.whyhappen.pcidss.iso8583.api.reactor.netty.handler

import com.github.kpavlov.jreactive8583.iso.MessageFactory
import com.solab.iso8583.IsoMessage

/**
 * Sends responses to incoming management messages.
 */
open class EchoMessageHandler(
    protected val isoMessageFactory: MessageFactory<IsoMessage>
) : IsoMessageHandler {
    override fun supports(isoMessage: IsoMessage): Boolean = isoMessage.type and 0x0800 != 0

    override suspend fun onMessage(inbound: IsoMessage): IsoMessage? = isoMessageFactory.createResponse(inbound)
}
