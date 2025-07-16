package ru.whyhappen.pcidss.iso8583.reactor.netty.pipeline

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.DecoderException
import ru.whyhappen.pcidss.iso8583.IsoMessage
import ru.whyhappen.pcidss.iso8583.IsoMessageException
import ru.whyhappen.pcidss.iso8583.MessageFactory
import ru.whyhappen.pcidss.iso8583.fields.DateFormats
import ru.whyhappen.pcidss.iso8583.mti.MessageClass
import ru.whyhappen.pcidss.iso8583.mti.MessageFunction
import ru.whyhappen.pcidss.iso8583.mti.MessageOrigin
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Handles [IsoMessageException]s and responds with an administrative message.
 * See [StackOverflow: How to answer an invalid ISO8583 message](http://stackoverflow.com/questions/28275677/how-to-answer-an-invalid-iso8583-message).
 *
 * See [jreactive-iso8583](https://github.com/kpavlov/jreactive-8583).
 */
open class DecoderExceptionHandler(
    protected val isoMessageFactory: MessageFactory<IsoMessage>,
    protected val includeErrorDetails: Boolean = true
) : ChannelInboundHandlerAdapter() {
    @Deprecated("Deprecated in Java")
    @Throws(Exception::class)
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        if (cause is DecoderException) {
            val originalCause = cause.cause
            if (originalCause is IsoMessageException) {
                val message = createErrorResponseMessage(originalCause)
                ctx.writeAndFlush(message)
            }
        }
        ctx.fireExceptionCaught(cause)
    }

    /**
     * Creates messages to be sent when an exception is thrown.
     */
    protected open fun createErrorResponseMessage(cause: Throwable): IsoMessage =
        isoMessageFactory.newMessage(
            MessageClass.ADMINISTRATIVE,
            MessageFunction.NOTIFICATION,
            MessageOrigin.OTHER
        ).apply {
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            setFieldValue(7, now.format(DateFormats.DATE10))
            setFieldValue(11, now.format(DateFormats.TIME))

            // 650 (Unable to parse the message)
            setFieldValue(24, "650")

            if (includeErrorDetails) {
                cause.message?.run {
                    val details = if (length > 25) take(22) + "..." else this
                    setFieldValue(44, details)
                }
            }
        }
}