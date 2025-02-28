package ru.whyhappen.pcidss.iso8583.spi.netty

import com.github.kpavlov.jreactive8583.server.ServerConfiguration
import com.solab.iso8583.IsoMessage
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelPipeline
import com.github.kpavlov.jreactive8583.ConnectorConfigurer as JReactive8583ConnectorConfigurer

const val IDLE_EVENT_HANDLER_NAME = "idleEventHandler"
const val CONNECT_EVENT_HANDLER_NAME = "connectEventHandler"

/**
 * Configurer that sends an ISO message when a client connects to the server.
 */
class ConnectorConfigurer<T : IsoMessage>(
    private val messageCreator: () -> T
) : JReactive8583ConnectorConfigurer<ServerConfiguration, ServerBootstrap> {
    override fun configurePipeline(pipeline: ChannelPipeline, configuration: ServerConfiguration) {
        val connectHandler = object : ChannelInboundHandlerAdapter() {
            override fun channelActive(ctx: ChannelHandlerContext) {
                super.channelActive(ctx)

                val msg = messageCreator()
                ctx.writeAndFlush(msg)
            }
        }

        if (pipeline.get(IDLE_EVENT_HANDLER_NAME) != null) {
            pipeline.addBefore(IDLE_EVENT_HANDLER_NAME, CONNECT_EVENT_HANDLER_NAME, connectHandler)
        } else {
            pipeline.addLast(CONNECT_EVENT_HANDLER_NAME, connectHandler)
        }
    }
}