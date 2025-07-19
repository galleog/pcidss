package ru.whyhappen.pcidss.iso8583

import ru.whyhappen.pcidss.iso8583.mti.*
import ru.whyhappen.pcidss.iso8583.spec.MessageSpec

/**
 * Default implementation of [MessageFactory].
 */
class DefaultMessageFactory(
    /**
     * ISO version of newly created messages.
     */
    val isoVersion: ISO8583Version = ISO8583Version.V1987,
    /**
     * Default role for new messages.
     */
    val role: MessageOrigin,
    /**
     * Message specification.
     */
    val spec: MessageSpec
) : MessageFactory<IsoMessage> {
    override fun newMessage(mti: Int): IsoMessage = DefaultIsoMessage(spec)
        .apply { this.mti = mti }

    override fun newMessage(
        messageClass: MessageClass,
        messageFunction: MessageFunction,
        messageOrigin: MessageOrigin
    ): IsoMessage = newMessage(Mti.mtiValue(isoVersion, messageClass, messageFunction, messageOrigin))

    override fun newMessage(messageClass: MessageClass, messageFunction: MessageFunction): IsoMessage =
        newMessage(messageClass, messageFunction, role)

    override fun createResponse(requestMessage: IsoMessage): IsoMessage = requestMessage.copyOf()
        .apply { mti = requestMessage.mti + 16 }

    override fun parseMessage(buf: ByteArray): IsoMessage = DefaultIsoMessage(spec).apply { unpack(buf) }
}