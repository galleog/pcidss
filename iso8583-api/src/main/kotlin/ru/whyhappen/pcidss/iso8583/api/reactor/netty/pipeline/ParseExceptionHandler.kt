package ru.whyhappen.pcidss.iso8583.api.reactor.netty.pipeline

import com.github.kpavlov.jreactive8583.iso.MessageClass
import com.github.kpavlov.jreactive8583.iso.MessageFactory
import com.github.kpavlov.jreactive8583.iso.MessageFunction
import com.github.kpavlov.jreactive8583.iso.MessageOrigin
import com.solab.iso8583.IsoMessage
import com.solab.iso8583.IsoType
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.DecoderException
import java.text.ParseException

/**
 * Handles [ParseException]s and responds with an administrative message.
 * See [StackOverflow: How to answer an invalid ISO8583 message](http://stackoverflow.com/questions/28275677/how-to-answer-an-invalid-iso8583-message).
 *
 * @see com.github.kpavlov.jreactive8583.netty.pipeline.ParseExceptionHandler
 */
open class ParseExceptionHandler(
    protected val isoMessageFactory: MessageFactory<IsoMessage>,
    protected val includeErrorDetails: Boolean = true
) : ChannelInboundHandlerAdapter() {
    @Deprecated("Deprecated in Java")
    @Throws(Exception::class)
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        if (cause is DecoderException) {
            val originalCause = cause.cause
            if (originalCause is ParseException) {
                val message = createErrorResponseMessage(originalCause)
                ctx.writeAndFlush(message)
            }
        }
        ctx.fireExceptionCaught(cause)
    }

    /**
     * Creates messages to be sent when [ParseException] is thrown.
     */
    protected open fun createErrorResponseMessage(cause: ParseException): IsoMessage {
        val message = isoMessageFactory.newMessage(
            MessageClass.ADMINISTRATIVE,
            MessageFunction.NOTIFICATION,
            MessageOrigin.OTHER
        )

        // 650 (Unable to parse the message)
        message.setValue(24, 650, IsoType.NUMERIC, 3)

        if (includeErrorDetails) {
            cause.message?.run {
                val details = if (length > 25) take(22) + "..." else this
                message.setValue(44, details, IsoType.LLVAR, 25)
            }
        }

        return message
    }
}