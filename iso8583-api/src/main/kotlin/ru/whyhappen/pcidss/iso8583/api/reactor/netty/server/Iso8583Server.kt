package ru.whyhappen.pcidss.iso8583.api.reactor.netty.server

import com.github.kpavlov.jreactive8583.iso.MessageFactory
import com.github.kpavlov.jreactive8583.server.ServerConfiguration
import com.solab.iso8583.IsoMessage
import io.micrometer.observation.ObservationRegistry
import io.netty.channel.ChannelHandler
import org.slf4j.LoggerFactory
import reactor.netty.DisposableServer
import reactor.netty.tcp.TcpServer
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.handler.CompositeIsoMessageHandler
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.handler.DefaultExceptionHandler
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.handler.ExceptionHandler
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.handler.IsoMessageHandler
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.pipeline.IdleEventChannelHandler
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.pipeline.Iso8583ChannelInitializer
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.pipeline.ParseExceptionChannelHandler

/**
 * Server to handle ISO8583 messages.
 */
class Iso8583Server(
    val port: Int,
    private val observationRegistry: ObservationRegistry,
    private val configuration: ServerConfiguration,
    private val isoMessageFactory: MessageFactory<IsoMessage>,
    messageHandlers: List<IsoMessageHandler>,
    exceptionHandler: ExceptionHandler = DefaultExceptionHandler(isoMessageFactory),
    private val parseExceptionChannelHandler: ChannelHandler = ParseExceptionChannelHandler(isoMessageFactory),
    private val idleEventChannelHandler: ChannelHandler = IdleEventChannelHandler(isoMessageFactory)
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
            kotlin.runCatching {
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
                    parseExceptionChannelHandler,
                    idleEventChannelHandler
                )
            ).handle { inbound, outbound -> compositeIsoMessageHandler.handle(inbound, outbound) }
    }
}
