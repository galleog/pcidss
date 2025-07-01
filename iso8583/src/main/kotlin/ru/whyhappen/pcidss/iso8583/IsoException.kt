package ru.whyhappen.pcidss.iso8583

/**
 * Base class for exception thrown when working with ISO8583 messages.
 */
open class IsoException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)