package ru.whyhappen.pcidss.iso8583.reactor.server

import io.micrometer.observation.ObservationRegistry
import io.netty.channel.ChannelHandler
import org.slf4j.LoggerFactory
import reactor.netty.DisposableServer
import reactor.netty.tcp.TcpServer
import ru.whyhappen.pcidss.iso8583.IsoMessage
import ru.whyhappen.pcidss.iso8583.MessageFactory
import ru.whyhappen.pcidss.iso8583.reactor.netty.handler.CompositeIsoMessageHandler
import ru.whyhappen.pcidss.iso8583.reactor.netty.handler.DefaultExceptionHandler
import ru.whyhappen.pcidss.iso8583.reactor.netty.handler.ExceptionHandler
import ru.whyhappen.pcidss.iso8583.reactor.netty.handler.IsoMessageHandler
import ru.whyhappen.pcidss.iso8583.reactor.netty.pipeline.DecoderExceptionHandler
import ru.whyhappen.pcidss.iso8583.reactor.netty.pipeline.IdleEventHandler
import ru.whyhappen.pcidss.iso8583.reactor.netty.pipeline.Iso8583ChannelInitializer

/**
 * Server to handle ISO8583 messages.
 */
class Iso8583Server(
    val port: Int,
    private val configuration: ServerConfiguration,
    private val isoMessageFactory: MessageFactory<IsoMessage>,
    messageHandlers: List<IsoMessageHandler>,
    private val observationRegistry: ObservationRegistry,
    exceptionHandler: ExceptionHandler = DefaultExceptionHandler(isoMessageFactory, configuration.responseCode),
    private val decoderExceptionHandler: ChannelHandler = DecoderExceptionHandler(isoMessageFactory),
    private val idleEventHandler: ChannelHandler = IdleEventHandler(isoMessageFactory)
) {
    private val tcpServer: TcpServer by lazy { createTcpServer() }
    private val compositeIsoMessageHandler = CompositeIsoMessageHandler(messageHandlers, exceptionHandler)
    private var disposableServer: DisposableServer? = null

    val isStarted: Boolean
        get() = disposableServer != null

    companion object {
        private val logger = LoggerFactory.getLogger(Iso8583Server::class.java)
    }

    /**
     * Starts the ISO8583 server. Calling this method on an already started server has no effect.
     */
    fun start() {
        if (disposableServer == null) {
            runCatching {
                disposableServer = tcpServer.bindNow()
                tcpServer
            }.onSuccess { server ->
                logger.info("Netty started on port {}", port)
            }.onFailure { e ->
                logger.error("Unable to start Netty", e)
                throw e
            }
        }
    }

    /**
     * Stops the ISO8583 server. Calling this method on an already stopped server has no effect.
     */
    fun stop() {
        disposableServer?.run {
            disposeNow()
            disposableServer = null
        }
    }

    private fun createTcpServer(): TcpServer {
        return TcpServer.create()
            .port(port)
            .doOnChannelInit(
                Iso8583ChannelInitializer(
                    observationRegistry,
                    configuration,
                    isoMessageFactory,
                    decoderExceptionHandler,
                    idleEventHandler
                )
            ).handle { inbound, outbound -> compositeIsoMessageHandler.handle(inbound, outbound) }
    }
}
