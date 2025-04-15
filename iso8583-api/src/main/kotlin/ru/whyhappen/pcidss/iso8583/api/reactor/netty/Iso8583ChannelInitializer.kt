package ru.whyhappen.pcidss.iso8583.api.reactor.netty

import com.github.kpavlov.jreactive8583.ConnectorConfiguration
import com.github.kpavlov.jreactive8583.iso.MessageFactory
import com.github.kpavlov.jreactive8583.netty.codec.Iso8583Decoder
import com.github.kpavlov.jreactive8583.netty.codec.Iso8583Encoder
import com.github.kpavlov.jreactive8583.netty.codec.StringLengthFieldBasedFrameDecoder
import com.github.kpavlov.jreactive8583.netty.pipeline.IsoMessageLoggingHandler
import com.solab.iso8583.IsoMessage
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.logging.LogLevel
import io.netty.handler.timeout.IdleStateHandler
import reactor.netty.ChannelPipelineConfigurer
import reactor.netty.ConnectionObserver
import reactor.netty.NettyPipeline
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.handler.IdleEventHandler
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.handler.ParseExceptionHandler
import java.net.SocketAddress

const val LENGTH_FIELD_FRAME_DECODER = "lengthFieldFrameDecoder"
const val ISO8583_DECODER = "iso8583Decoder"
const val ISO8583_ENCODER = "iso8583Encoder"
const val LOGGING_HANDLER = "loggingHandler"
const val REPLY_ON_ERROR_HANDLER = "replyOnErrorHandler"
const val IDLE_STATE_HANDLER = "idleStateHandler"
const val IDLE_EVENT_HANDLER = "idleEventHandler"

/**
 * Configures channel pipeline for ISO messages.
 *
 * @param T the type of connector configuration providing necessary settings
 */
class Iso8583ChannelInitializer<T : ConnectorConfiguration>(
    /**
     * Connector configuration that provides necessary settings for initializing the channel.
     */
    private val configuration: T,
    /**
     * Factory to create and parse ISO messages
     */
    private val isoMessageFactory: MessageFactory<IsoMessage>,
    /**
     * Handles exceptions thrown when parsing ISO messages.
     */
    private val parseExceptionHandler: ChannelHandler = ParseExceptionHandler(isoMessageFactory),
    /**
     * Sends heartbeats when channel becomes idle.
     */
    private val idleEventHandler: ChannelHandler = IdleEventHandler(isoMessageFactory)
) : ChannelPipelineConfigurer {
    override fun onChannelInit(
        connectionObserver: ConnectionObserver,
        channel: Channel,
        remoteAddress: SocketAddress?
    ) {
        val baseName = NettyPipeline.ReactiveBridge
        with(channel.pipeline()) {
            addBefore(baseName, LENGTH_FIELD_FRAME_DECODER, createLengthFieldBasedFrameDecoder(configuration))
            addBefore(baseName, ISO8583_DECODER, createIso8583Decoder(isoMessageFactory))
            addBefore(baseName, ISO8583_ENCODER, createIso8583Encoder(configuration))
            if (configuration.addLoggingHandler()) {
                addBefore(baseName, LOGGING_HANDLER, createLoggingHandler(configuration))
            }
            if (configuration.replyOnError()) {
                addBefore(baseName, REPLY_ON_ERROR_HANDLER, parseExceptionHandler)
            }
            if (configuration.shouldAddEchoMessageListener()) {
                addBefore(baseName, IDLE_STATE_HANDLER, IdleStateHandler(0, 0, configuration.idleTimeout))
                addAfter(IDLE_STATE_HANDLER, IDLE_EVENT_HANDLER, idleEventHandler)
            }
        }
    }

    private fun createIso8583Encoder(configuration: T): Iso8583Encoder =
        Iso8583Encoder(
            configuration.frameLengthFieldLength,
            configuration.encodeFrameLengthAsString()
        )

    private fun createIso8583Decoder(
        messageFactory: MessageFactory<IsoMessage>
    ): Iso8583Decoder = Iso8583Decoder(messageFactory)

    private fun createLoggingHandler(configuration: T): ChannelHandler =
        IsoMessageLoggingHandler(
            LogLevel.DEBUG,
            configuration.logSensitiveData(),
            configuration.logFieldDescription(),
            configuration.sensitiveDataFields
        )

    private fun createLengthFieldBasedFrameDecoder(configuration: T): ChannelHandler {
        val lengthFieldLength = configuration.frameLengthFieldLength
        return if (configuration.encodeFrameLengthAsString()) {
            StringLengthFieldBasedFrameDecoder(
                configuration.maxFrameLength,
                configuration.frameLengthFieldOffset,
                lengthFieldLength,
                configuration.frameLengthFieldAdjust,
                lengthFieldLength
            )
        } else {
            LengthFieldBasedFrameDecoder(
                configuration.maxFrameLength,
                configuration.frameLengthFieldOffset,
                lengthFieldLength,
                configuration.frameLengthFieldAdjust,
                lengthFieldLength
            )
        }
    }
}