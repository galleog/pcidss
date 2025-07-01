package ru.whyhappen.pcidss.iso8583.reactor.netty.handler

import ru.whyhappen.pcidss.iso8583.IsoMessage
import ru.whyhappen.pcidss.iso8583.MessageFactory

/**
 * Handles exceptions thrown when processing ISO messages.
 */
open class DefaultExceptionHandler(
    protected val isoMessageFactory: MessageFactory<IsoMessage>,
    protected val includeErrorDetails: Boolean = true
) : ExceptionHandler {
    override suspend fun handleException(inbound: IsoMessage, cause: Throwable): IsoMessage? {
        // TODO
        val response = isoMessageFactory.createResponse(inbound)

        if (includeErrorDetails) {
            cause.message?.run {
                val details = if (length > 25) take(22) + "..." else this
                response.setFieldValue(44, details)
            }
        }

        return response
    }
}