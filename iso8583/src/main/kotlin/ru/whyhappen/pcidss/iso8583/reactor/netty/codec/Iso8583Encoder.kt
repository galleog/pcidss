package ru.whyhappen.pcidss.iso8583.reactor.netty.codec

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import io.netty.util.CharsetUtil
import ru.whyhappen.pcidss.iso8583.IsoMessage

/**
 * [MessageToByteEncoder] for encoding [IsoMessage]s to [ByteBuf]s.
 *
 * See [jreactive-iso8583](https://github.com/kpavlov/jreactive-8583).
 *
 * @param headerLength the length of the length header in bytes
 * @param encodeLengthHeaderAsString whether to encode the length header as a string
 *
 */
@Sharable
class Iso8583Encoder(
    private val headerLength: Int,
    private val encodeLengthHeaderAsString: Boolean
) : MessageToByteEncoder<IsoMessage>() {
    public override fun encode(ctx: ChannelHandlerContext, isoMessage: IsoMessage, out: ByteBuf) {
        val bytes = isoMessage.pack()
        when {
            headerLength == 0 -> {
                out.writeBytes(bytes)
            }

            encodeLengthHeaderAsString -> {
                val lengthHeader = "%0${headerLength}d".format(bytes.size)
                out.writeBytes(lengthHeader.toByteArray(CharsetUtil.US_ASCII))
                out.writeBytes(bytes)
            }

            else -> {
                val lengthHeader = intToBytes(bytes.size, headerLength)
                out.writeBytes(lengthHeader)
                out.writeBytes(bytes)
            }
        }
    }

    private fun intToBytes(n: Int, length: Int): ByteArray {
        val result = ByteArray(length)

        // fill the array in big-endian format (the most significant byte first)
        for (i in 0 until length) {
            result[length - 1 - i] = ((n shr (i * 8)) and 0xFF).toByte()
        }

        return result
    }
}
