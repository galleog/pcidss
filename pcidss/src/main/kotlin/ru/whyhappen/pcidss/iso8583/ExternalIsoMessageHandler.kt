package ru.whyhappen.pcidss.iso8583

import com.github.kpavlov.jreactive8583.iso.MessageFactory
import com.solab.iso8583.IsoMessage
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.handler.IsoMessageHandler
import ru.whyhappen.pcidss.service.TokenService

/**
 * Message handler that delegates message processing to an external service.
 * It tokenizes sensitive data fields of an ISO message and sends the message to that service.
 * Then uses the data it gets from the service to send an ISO response message.
 */
class ExternalIsoMessageHandler(
    /**
     * List of sensitive data fields to tokenize.
     */
    private val sensitiveDataFields: List<Int>,
    /**
     * ISO8583 message factory.
     */
    private val messageFactory: MessageFactory<IsoMessage>,
    /**
     * Service to get tokens for sensitive data fields.
     */
    private val tokenService: TokenService,
    /**
     * Web client for sending an HTTP request to the external service.
     */
    private val webClient: WebClient,
    /**
     * Optional customizer for an ISO response message.
     */
    private val customizer: IsoMessageCustomizer? = null,
) : IsoMessageHandler {
    companion object {
        private val logger = LoggerFactory.getLogger(ExternalIsoMessageHandler::class.java)
    }

    override fun supports(isoMessage: IsoMessage): Boolean = true

    override suspend fun onMessage(inbound: IsoMessage): IsoMessage {
        val respBody = withContext(Dispatchers.IO) {
            // get tokens for all sensitive fields
            val deferredEntries: List<Pair<Int, Deferred<String>>> = sensitiveDataFields
                .asSequence()
                .filter(inbound::hasField)
                .map {
                    val value = inbound.getAt<Any>(it).toString()
                    it to async { tokenService.getToken(value) }
                }.toList()

            val results = deferredEntries.map { it.second }.awaitAll()
            val tokens = deferredEntries.map { it.first }
                .zip(results)
                .toMap()

            // send the incoming message to the external service
            val reqBody = IsoMessageDto(
                inbound.type,
                (2..128).asSequence()
                    .filter(inbound::hasField)
                    .associateWith { tokens[it] ?: inbound.getAt<Any>(it).toString() }
            )
            webClient.post()
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(reqBody)
                .retrieve()
                .awaitBody<IsoMessageDto>()
        }

        return messageFactory.createResponse(inbound)
            .apply {
                // copy fields to the response message
                for ((key, value) in respBody.fields) {
                    if (key !in sensitiveDataFields && hasField(key)) {
                        updateValue(key, value)
                    } else {
                        logger.warn(
                            "Unknown response field {} with value {} for IsoMessage[type=0x{}]",
                            key,
                            value,
                            "%04x".format(type)
                        )
                    }
                }

                customizer?.customize(this)
            }
    }
}