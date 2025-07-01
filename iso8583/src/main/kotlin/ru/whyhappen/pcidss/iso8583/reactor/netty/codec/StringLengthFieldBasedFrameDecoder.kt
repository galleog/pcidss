package ru.whyhappen.pcidss.iso8583.reactor.netty.codec

import io.netty.buffer.ByteBuf
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.util.CharsetUtil
import java.nio.ByteOrder

/**
 * Netty's [LengthFieldBasedFrameDecoder] assumes the frame length header is a binary-encoded integer.
 * This class overrides its frame length decoding to implement string-encoding instead.
 * Uses [CharsetUtil.US_ASCII] for decoding.
 *
 * See [jreactive-iso8583](https://github.com/kpavlov/jreactive-8583).
 *
 * @param maxFrameLength the maximum length of the frame. If the length of the frame is greater than
 *    this value, `TooLongFrameException` will be thrown.
 * @param lengthFieldOffset the offset of the length field
 * @param lengthFieldLength the length of the length field
 * @param lengthAdjustment the compensation value to add to the value of the length field
 * @param initialBytesToStrip the number of first bytes to strip out from the decoded frame
 * @see LengthFieldBasedFrameDecoder
 */
class StringLengthFieldBasedFrameDecoder(
    maxFrameLength: Int,
    lengthFieldOffset: Int,
    lengthFieldLength: Int,
    lengthAdjustment: Int,
    initialBytesToStrip: Int
) : LengthFieldBasedFrameDecoder(
    maxFrameLength,
    lengthFieldOffset,
    lengthFieldLength,
    lengthAdjustment,
    initialBytesToStrip
) {
    public override fun getUnadjustedFrameLength(buf: ByteBuf, offset: Int, length: Int, order: ByteOrder): Long {
        val b = buf.order(order)
        val lengthBytes = ByteArray(length)
        b.getBytes(offset, lengthBytes)
        val s = lengthBytes.toString(CharsetUtil.US_ASCII)
        return s.toLong()
    }
}
