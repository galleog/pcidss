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
                    logger.debug(
                        "Handling IsoMessage[type=0x{}] with {}",
                        "%04x".format(message.type),
                        handler.javaClass.name
                    )

                    runCatching {
                        response = handler.onMessage(message)
                    }.getOrElse { e ->
                        logger.error("Error while handling IsoMessage[type=0x{}]", "%04x".format(message.type), e)
                        response = exceptionHandler.handleException(message, e)
                    }
                } else {
                    logger.warn("No suitable handler found for IsoMessage[type=0x{}]", "%04x".format(message.type))
                    response = exceptionHandler.handleException(message, IsoHandlerNotFoundException(message))
                }

                response?.also {
                    logger.info("Sending outgoing IsoMessage[type=0x{}]", "%04x".format(it.type))
                }
            }
        }
    }
}

/**
 * Exception thrown when no [IsoMessageHandler] found for an incoming [IsoMessage].
 */
class IsoHandlerNotFoundException(val isoMessage: IsoMessage) : RuntimeException() {
    override val message: String = "Message handler not found for IsoMessage[type=0x${"%04x".format(isoMessage.type)}]"
}