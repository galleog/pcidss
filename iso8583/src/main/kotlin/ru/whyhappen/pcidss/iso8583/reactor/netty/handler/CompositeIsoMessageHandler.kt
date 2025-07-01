package ru.whyhappen.pcidss.iso8583.reactor.netty.handler

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.cast
import reactor.netty.NettyInbound
import reactor.netty.NettyOutbound
import ru.whyhappen.pcidss.iso8583.IsoMessage

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
    companion object {
        private val logger = LoggerFactory.getLogger(CompositeIsoMessageHandler::class.java)
    }

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
            var response: IsoMessage? = null
            mono(Dispatchers.Unconfined) {
                val handler = messageHandlers.firstOrNull { handler -> handler.supports(message) }
                if (handler != null) {
                    logger.debug("Handling {} with {}", message, handler::class.simpleName)

                    runCatching {
                        response = handler.onMessage(message)
                    }.getOrElse { e ->
                        logger.error("Error while handling {}", message, e)
                        response = exceptionHandler.handleException(message, e)
                    }
                } else {
                    logger.warn("No suitable handler found for {}", message)
                    response = exceptionHandler.handleException(message, IsoHandlerNotFoundException(message))
                }

                response?.also {
                    logger.info("Sending outgoing {}", it)
                }
            }
        }
    }
}

/**
 * Exception thrown when no [IsoMessageHandler] found for an incoming [IsoMessage].
 */
class IsoHandlerNotFoundException(val isoMessage: IsoMessage) : RuntimeException() {
    override val message: String = "Message handler not found for $isoMessage"
}