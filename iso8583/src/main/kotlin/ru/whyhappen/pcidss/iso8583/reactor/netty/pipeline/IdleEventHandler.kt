package ru.whyhappen.pcidss.iso8583.reactor.netty.pipeline

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent
import ru.whyhappen.pcidss.iso8583.IsoMessage
import ru.whyhappen.pcidss.iso8583.MessageFactory
import ru.whyhappen.pcidss.iso8583.fields.DateFormats
import ru.whyhappen.pcidss.iso8583.mti.MessageClass
import ru.whyhappen.pcidss.iso8583.mti.MessageFunction
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Sends heartbeats (administrative messages) when the channel becomes idle, i.e. `IdleStateEvent` is received.
 *
 * See [jreactive-iso8583](https://github.com/kpavlov/jreactive-8583).
 */
open class IdleEventHandler(
    protected val isoMessageFactory: MessageFactory<IsoMessage>
) : ChannelInboundHandlerAdapter() {
    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
        if (evt is IdleStateEvent &&
            (evt.state() == IdleState.READER_IDLE || evt.state() == IdleState.ALL_IDLE)
        ) {
            val heartbeatMessage = createHeartbeatMessage()
            ctx.writeAndFlush(heartbeatMessage)
        }
    }

    /**
     * Creates heartbeat messages.
     */
    protected open fun createHeartbeatMessage(): IsoMessage =
        isoMessageFactory.newMessage(
            MessageClass.NETWORK_MANAGEMENT,
            MessageFunction.REQUEST
        ).apply {
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            setFieldValue(7, now.format(DateFormats.DATE10))
            setFieldValue(11, now.format(DateFormats.TIME))
            setFieldValue(70, "301") // echo test
        }
}