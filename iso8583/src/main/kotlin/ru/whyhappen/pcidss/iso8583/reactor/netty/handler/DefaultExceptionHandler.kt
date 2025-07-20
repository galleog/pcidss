package ru.whyhappen.pcidss.iso8583.reactor.netty.handler

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.whyhappen.pcidss.iso8583.IsoMessage
import ru.whyhappen.pcidss.iso8583.MessageFactory

/**
 * Exception handler that returns successful response.
 */
open class DefaultExceptionHandler(
    protected val isoMessageFactory: MessageFactory<IsoMessage>,
    protected val responseCode: String
) : ExceptionHandler {
    protected val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun handleException(message: IsoMessage, cause: Throwable): IsoMessage? {
        logger.error("Handling {} failed. Return successful response", message, cause)

        return isoMessageFactory.createResponse(message)
            .apply {
                setFieldValue(39, responseCode) // successful response code
            }
    }
}