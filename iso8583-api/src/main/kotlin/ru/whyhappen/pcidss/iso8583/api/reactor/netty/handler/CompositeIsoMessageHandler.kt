package ru.whyhappen.pcidss.iso8583.api.reactor.netty.handler

import com.solab.iso8583.IsoMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.cast
import reactor.netty.NettyInbound
import reactor.netty.NettyOutbound

/**
 * Handles incoming ISO messages using suitable [IsoMessageHandler]s.
 */
class CompositeIsoMessageHandler(
    /**
     * [IsoMessageHandler]s that actually handle ISO messages.
     */
    private val messageHandlers: List<IsoMessageHandler>,
    /**
     * Handles possible exceptions thrown when processing messages.
     */
    private val exceptionHandler: ExceptionHandler
) {
    /**
     * Handles ISO messages.
     */
    fun handle(inbound: NettyInbound, outbound: NettyOutbound): Flux<Void> {
        return inbound.receiveObject()
            .cast<IsoMessage>()
            .flatMap { isoMessage ->
                processMessage(isoMessage).flatMap { response ->
                    outbound.sendObject(response).then()
                }
            }
    }

    private fun processMessage(message: IsoMessage): Mono<IsoMessage> {
        return Mono.defer {
            mono(Dispatchers.Unconfined) {
                val handler = messageHandlers.firstOrNull { handler -> handler.supports(message) }
                if (handler != null) {
                    logger.debug("Handling IsoMessage[type=0x{}] with {}", "%04X".format(message.type), handler)

                    runCatching {
                        handler.onMessage(message)
                    }.getOrElse { e ->
                        exceptionHandler.handleException(message, e)
                    }
                } else {
                    exceptionHandler.handleException(message, IsoHandlerNotFoundException(message))
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CompositeIsoMessageHandler::class.java)
    }
}

/**
 * Exception thrown when no [IsoMessageHandler] found for an incoming [IsoMessage].
 */
class IsoHandlerNotFoundException(val isoMessage: IsoMessage) : RuntimeException() {
    override val message: String = "Message handler not found for IsoMessage[type=0x${"%04X".format(isoMessage.type)}]"
}