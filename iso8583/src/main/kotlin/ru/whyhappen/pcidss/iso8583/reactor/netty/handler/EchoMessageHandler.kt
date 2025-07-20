package ru.whyhappen.pcidss.iso8583.reactor.netty.handler

import ru.whyhappen.pcidss.iso8583.IsoMessage
import ru.whyhappen.pcidss.iso8583.MessageFactory
import ru.whyhappen.pcidss.iso8583.mti.MessageClass

/**
 * Sends responses to incoming management messages.
 *
 * See [jreactive-iso8583](https://github.com/kpavlov/jreactive-8583).
 */
open class EchoMessageHandler(
    protected val isoMessageFactory: MessageFactory<IsoMessage>,
    protected val responseCode: String
) : IsoMessageHandler {
    override fun supports(isoMessage: IsoMessage): Boolean =
        isoMessage.mti and MessageClass.NETWORK_MANAGEMENT.value != 0

    override suspend fun onMessage(inbound: IsoMessage): IsoMessage? =
        isoMessageFactory.createResponse(inbound)
            .apply {
                setFieldValue(39, responseCode) // response code
            }
}
