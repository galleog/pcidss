package ru.whyhappen.pcidss.iso8583.api.reactor.netty.handler

import com.github.kpavlov.jreactive8583.iso.MessageFactory
import com.solab.iso8583.IsoMessage
import com.solab.iso8583.IsoType

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
                response.setValue(44, details, IsoType.LLVAR, 25)
            }
        }

        return response
    }
}