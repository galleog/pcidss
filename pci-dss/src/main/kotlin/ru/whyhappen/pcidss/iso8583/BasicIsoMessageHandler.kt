package ru.whyhappen.pcidss.iso8583

import com.github.kpavlov.jreactive8583.iso.MessageFactory
import com.solab.iso8583.IsoMessage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import ru.whyhappen.pcidss.iso8583.api.reactor.netty.handler.IsoMessageHandler
import ru.whyhappen.service.TokenService

/**
 * Basic message handler for all types of ISO8583 messages.
 */
class BasicIsoMessageHandler(
    private val sensitiveDataFields: List<Int>,
    private val messageFactory: MessageFactory<IsoMessage>,
    private val tokenService: TokenService,
    private val webClient: WebClient,
    private val customizer: IsoMessageCustomizer? = null,
) : IsoMessageHandler {
    companion object {
        private val logger = LoggerFactory.getLogger(BasicIsoMessageHandler::class.java)
    }

    override fun supports(isoMessage: IsoMessage): Boolean = true

    override suspend fun onMessage(inbound: IsoMessage): IsoMessage = coroutineScope {
        // get tokens for all sensitive fields
        val tokens: Map<Int, String> = sensitiveDataFields.asSequence()
            .filter(inbound::hasField)
            .associateWith {
                val value = inbound.getAt<Any>(it).toString()
                tokenService.getToken(value)
            }

        // send the incoming message to the external service
        val reqBody = IsoMessageDto(
            inbound.type,
            (2..128).asSequence()
                .filter(inbound::hasField)
                .associateWith { tokens[it] ?: inbound.getAt<Any>(it).toString() }
        )
        val respBody = webClient.post()
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(reqBody)
            .retrieve()
            .bodyToMono(IsoMessageDto::class.java)
            .awaitSingle()

        messageFactory.createResponse(inbound)
            .apply {
                // copy fields to the response message
                for ((key, value) in respBody.fields) {
                    if (hasField(key)) {
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