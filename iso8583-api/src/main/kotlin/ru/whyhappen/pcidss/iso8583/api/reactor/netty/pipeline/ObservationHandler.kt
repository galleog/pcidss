package ru.whyhappen.pcidss.iso8583.api.reactor.netty.pipeline

import com.solab.iso8583.IsoMessage
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.netty.util.AttributeKey

/**
 * Enables observability for processing of [IsoMessage]s.
 */
class ObservationHandler(private val observationRegistry: ObservationRegistry) : ChannelDuplexHandler() {
    companion object {
        internal const val OBSERVATION_NAME = "iso8583.request"
        internal const val IN_MTI_TAG = "iso8583.in.mti"
        internal const val OUT_MTI_TAG = "iso8583.out.mti"

        internal val REQUEST_RECEIVED_EVENT = Observation.Event.of("request.received")
        internal val REQUEST_PROCESSED_EVENT = Observation.Event.of("request.processed")
        internal val OBSERVATION_ATTR_KEY = AttributeKey.valueOf<Observation>("observation")
        internal val OBSERVATION_SCOPE_ATTR_KEY = AttributeKey.valueOf<Observation.Scope>("observationScope")
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        val observation = Observation.createNotStarted(OBSERVATION_NAME, observationRegistry)

        if (msg is IsoMessage) {
            observation.lowCardinalityKeyValue(IN_MTI_TAG, "%04X".format(msg.type))
        }

        // Start the observation and store it in the channel attributes
        val startedObservation = observation.start()
            .event(REQUEST_RECEIVED_EVENT)
        ctx.channel().attr(OBSERVATION_ATTR_KEY).set(startedObservation)

        val scope = startedObservation.openScope()
        ctx.channel().attr(OBSERVATION_SCOPE_ATTR_KEY).set(scope)

        runCatching {
            ctx.fireChannelRead(msg)
        }.onFailure { e ->
            startedObservation.error(e)
            throw e
        }
    }

    override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
        val observation = ctx.channel().attr(OBSERVATION_ATTR_KEY).get()

        if (msg is IsoMessage && observation != null) {
            observation.lowCardinalityKeyValue(OUT_MTI_TAG, "%04X".format(msg.type))
        }

        try {
            ctx.write(msg, promise)
        } finally {
            observation?.event(REQUEST_PROCESSED_EVENT)
            closeObservation(ctx)
        }
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        closeObservation(ctx)
        ctx.fireChannelInactive()
    }

    private fun closeObservation(ctx: ChannelHandlerContext) {
        ctx.channel().attr(OBSERVATION_SCOPE_ATTR_KEY).getAndSet(null)?.close()
        ctx.channel().attr(OBSERVATION_ATTR_KEY).getAndSet(null)?.stop()
    }
}
