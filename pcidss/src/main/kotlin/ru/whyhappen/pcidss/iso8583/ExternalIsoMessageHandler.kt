package ru.whyhappen.pcidss.iso8583

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import ru.whyhappen.pcidss.iso8583.reactor.netty.handler.IsoMessageHandler
import ru.whyhappen.pcidss.service.TokenService

/**
 * Message handler that delegates message processing to an external service.
 * It tokenizes sensitive data fields of an ISO message and sends the message to that service.
 * It uses the data it gets from the service to send an ISO response message.
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
     * Default response code.
     */
    private val responseCode: String,
    /**
     * Optional customizers for an ISO response message.
     */
    private val customizers: List<IsoMessageCustomizer> = emptyList()
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
                    val value = inbound.getFieldValue(it, ByteArray::class.java)!!
                    it to async { tokenService.getToken(value) }
                }.toList()

            val results = deferredEntries.map { it.second }.awaitAll()
            val tokens = deferredEntries.map { it.first }
                .zip(results)
                .toMap()

            // send the incoming message to the external service
            val reqBody = IsoMessageDto(
                inbound.mti,
                inbound.fields.mapValues { (id, field) -> tokens[id] ?: field.getValue(String::class.java)!! }
            )
            webClient.post()
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(reqBody)
                .retrieve()
                .awaitBody<IsoMessageDto>()
        }

        val response = messageFactory.createResponse(inbound)

        // copy fields to the response message
        for ((key, value) in respBody.fields) {
            if (key !in sensitiveDataFields) {
                runCatching {
                    response.setFieldValue(key, value)
                }.onFailure {
                    logger.warn("Unknown response field {} with value {} for {}", key, value, response)
                }
            }
        }
        if (!response.hasField(39)) {
            response.setFieldValue(39, responseCode)
        }

        for (customizer in customizers) {
            if (!customizer.customize(response)) break
        }

        return response
    }
}