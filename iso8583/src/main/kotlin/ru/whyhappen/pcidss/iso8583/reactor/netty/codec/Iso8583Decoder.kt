package ru.whyhappen.pcidss.iso8583.reactor.netty.codec

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import ru.whyhappen.pcidss.iso8583.IsoMessage
import ru.whyhappen.pcidss.iso8583.MessageFactory

/**
 * A decoder for ISO8583 messages.
 *
 * This class extends [ByteToMessageDecoder] and is responsible for decoding ISO8583 messages
 * from a Netty [ByteBuf] to [IsoMessage] instances.
 *
 * See [jreactive-iso8583](https://github.com/kpavlov/jreactive-8583).
 */
class Iso8583Decoder(
    /**
     * Factory for creating and parsing `IsoMessage` instances
     */
    private val messageFactory: MessageFactory<IsoMessage>
) : ByteToMessageDecoder() {
    /**
     * Decodes an ISO8583 message from [ByteBuf].
     */
    @Throws(Exception::class)
    public override fun decode(ctx: ChannelHandlerContext, byteBuf: ByteBuf, out: MutableList<Any>) {
        if (!byteBuf.isReadable) return

        val bytes = ByteArray(byteBuf.readableBytes())
        byteBuf.readBytes(bytes)
        val isoMessage = messageFactory.parseMessage(bytes)
        out.add(isoMessage)
    }
}
