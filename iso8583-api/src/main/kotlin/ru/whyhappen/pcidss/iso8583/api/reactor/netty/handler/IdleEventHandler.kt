package ru.whyhappen.pcidss.iso8583.api.reactor.netty.handler

import com.github.kpavlov.jreactive8583.iso.MessageClass
import com.github.kpavlov.jreactive8583.iso.MessageFactory
import com.github.kpavlov.jreactive8583.iso.MessageFunction
import com.solab.iso8583.IsoMessage
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent

/**
 * Sends heartbeats (administrative messages) when channel becomes idle, i.e. `IdleStateEvent` is received.
 *
 * @see com.github.kpavlov.jreactive8583.netty.pipeline.IdleEventHandler
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
        )
}