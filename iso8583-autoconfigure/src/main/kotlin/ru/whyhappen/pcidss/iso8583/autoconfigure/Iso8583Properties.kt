package ru.whyhappen.pcidss.iso8583.autoconfigure

import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import ru.whyhappen.pcidss.iso8583.mti.ISO8583Version
import ru.whyhappen.pcidss.iso8583.mti.MessageOrigin
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.core.io.Resource
import ru.whyhappen.pcidss.iso8583.reactor.*

/**
 * Configuration properties for ISO8583 server and client.
 */
@ConfigurationProperties(prefix = "iso8583")
data class Iso8583Properties(
    /**
     * Properties to establish a connection.
     */
    var connection: ConnectionProperties = ConnectionProperties(),
    /**
     * Properties for ISO8583 messages.
     */
    var message: Iso8583MessageProperties = Iso8583MessageProperties()
)

/**
 * Connection properties.
 */
data class ConnectionProperties(
    /**
     * Connection host.
     */
    var host: String = "127.0.0.1",
    /**
     * Connection port.
     */
    var port: Int = 9876,
    /**
     * Maximum length of the TCP frame.
     */
    val maxFrameLength: Int = DEFAULT_MAX_FRAME_LENGTH,
    /**
     * Length of TCP frame length field.
     *
     * @see LengthFieldBasedFrameDecoder
     */
    val frameLengthFieldLength: Int = DEFAULT_FRAME_LENGTH_FIELD_LENGTH,

    /**
     * Offset of the length field.
     *
     * @see LengthFieldBasedFrameDecoder
     */
    val frameLengthFieldOffset: Int = DEFAULT_FRAME_LENGTH_FIELD_OFFSET,
    /**
     * Compensation value to add to the value of the length field.
     *
     * @see LengthFieldBasedFrameDecoder
     */
    val frameLengthFieldAdjust: Int = DEFAULT_FRAME_LENGTH_FIELD_ADJUST,
    /**
     * If `true` then the length header is to be encoded as a String, as opposed to the default binary.
     */
    val encodeFrameLengthAsString: Boolean = false,
    /**
     * Indicates if management messages should be sent if connection is idle.
     */
    var addIdleEventHandler: Boolean = false,
    /**
     * Timeout between heartbeats in seconds.
     */
    var idleTimeout: Int = DEFAULT_IDLE_TIMEOUT_SECONDS,
    /**
     * Indicates if a reply with an administrative message should be sent in case of message syntax errors.
     */
    var replyOnError: Boolean = false,
    /**
     * Indicates if messages should be logged.
     */
    var addLoggingHandler: Boolean = false,
    /**
     * Indicates if sensitive data specified in [Iso8583MessageProperties.sensitiveDataFields]
     * should be masked in the log.
     */
    var logSensitiveData: Boolean = false,
    /**
     * Indicates if field names should be logged.
     */
    var logFieldDescription: Boolean = false
)

/**
 * Properties to create and parse ISO8583 messages.
 */
data class Iso8583MessageProperties(
    /**
     * ISO8583 version.
     */
    var isoVersion: ISO8583Version = ISO8583Version.V1987,
    /**
     * Role of the communicating party.
     */
    var role: MessageOrigin = MessageOrigin.ACQUIRER,
    /**
     * Default value for the ISO field 39 'Response Code'.
     */
    var defaultResponseCode: String = "00",
    /**
     * Resources to build the message spec. They are applied consecutively.
     */
    var configs: List<Resource> = emptyList(),
    /**
     * List of ISO8583 sensitive field numbers to be masked and encoded.
     */
    var sensitiveDataFields: List<Int> = listOf(
        2,  // PAN
        34, // PAN extended
        35, // track 2
        36, // track 3
        45, // track 1
    )
)