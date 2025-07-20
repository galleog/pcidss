package ru.whyhappen.pcidss.iso8583.reactor

import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.timeout.IdleStateEvent
import ru.whyhappen.pcidss.iso8583.reactor.netty.pipeline.IdleEventHandler
import ru.whyhappen.pcidss.iso8583.reactor.netty.pipeline.IsoMessageLoggingHandler

/**
 * Default read/write idle timeout in seconds (ping interval) = 30 sec.
 *
 * @see ConnectorConfiguration.idleTimeout
 */
const val DEFAULT_IDLE_TIMEOUT_SECONDS = 30

/**
 * Default [ConnectorConfiguration.maxFrameLength] (max message length) = 8192
 *
 * @see ConnectorConfiguration.maxFrameLength
 */
const val DEFAULT_MAX_FRAME_LENGTH = 8192

/**
 * Default [ConnectorConfiguration.frameLengthFieldLength] (length of TCP Frame length) = 2
 *
 * @see ConnectorConfiguration.frameLengthFieldLength
 */
const val DEFAULT_FRAME_LENGTH_FIELD_LENGTH = 2

/**
 * Default [ConnectorConfiguration.frameLengthFieldAdjust] (compensation value to add to the value of the length field) = 0
 *
 * @see ConnectorConfiguration.frameLengthFieldAdjust
 */
const val DEFAULT_FRAME_LENGTH_FIELD_ADJUST = 0

/**
 * Default [ConnectorConfiguration.frameLengthFieldOffset] (the offset of the length field) = 0
 *
 * @see ConnectorConfiguration.frameLengthFieldOffset
 */
const val DEFAULT_FRAME_LENGTH_FIELD_OFFSET = 0

/**
 * Default value for the ISO field 39 'Response Code'.
 */
const val DEFAULT_RESPONSE_CODE = "00"

/**
 * Abstract class representing the configuration for a connector.
 *
 * See [jreactive-iso8583](https://github.com/kpavlov/jreactive-8583).
 *
 * @param b The builder used to create the configuration instance.
 */
@Suppress("UnnecessaryAbstractClass")
abstract class ConnectorConfiguration protected constructor(
    b: Builder<*>,
) {
    val addIdleEventHandler: Boolean

    /**
     * Maximum length of the TCP frame.
     */
    val maxFrameLength: Int

    /**
     * Specifies the timeout in seconds when the channel becomes idle, i.e. [IdleStateEvent] is received.
     */
    val idleTimeout: Int

    val replyOnError: Boolean

    val addLoggingHandler: Boolean

    val logSensitiveData: Boolean

    /**
     * Array of ISO8583 sensitive field numbers to be masked, or `null` to use default fields.
     *
     * @see IsoMessageLoggingHandler
     */
    val sensitiveDataFields: IntArray

    val logFieldDescription: Boolean

    /**
     * Length of TCP frame length field.
     * Default value is `2`.
     *
     * @see LengthFieldBasedFrameDecoder
     */
    val frameLengthFieldLength: Int

    /**
     * Offset of the length field.
     *
     * Default value is `0`.
     *
     * @see LengthFieldBasedFrameDecoder
     */
    val frameLengthFieldOffset: Int

    /**
     * Compensation value to add to the value of the length field.
     * Default value is `0`.
     *
     * @see LengthFieldBasedFrameDecoder
     */
    val frameLengthFieldAdjust: Int

    /**
     * If `true` then the length header is to be encoded as a String, as opposed to the default binary.
     */
    val encodeFrameLengthAsString: Boolean

    /**
     * Default value for the ISO field 39 'Response Code'.
     */
    val responseCode: String

    /**
     * Indicates if [IdleEventHandler] should be added to the Netty pipeline.
     */
    fun shouldAddIdleEventHandler(): Boolean = addIdleEventHandler

    /**
     * Indicates if [IsoMessageLoggingHandler] should be added to the Netty pipeline.
     */
    fun addLoggingHandler(): Boolean = addLoggingHandler

    /**
     * Whether to reply with administrative messages to message syntax errors.
     * Default value is `false`.
     */
    fun replyOnError(): Boolean = replyOnError

    /**
     * Returns `true` if sensitive information like PAN, CVV/CVV2, and Track2 should be printed to log.
     * Default value is `true` (sensitive data is printed).
     */
    fun logSensitiveData(): Boolean = logSensitiveData

    fun logFieldDescription(): Boolean = logFieldDescription

    /**
     * Returns `true` if the length header is to be encoded as a string, as opposed to the default binary.
     * Default value is `false` (frame length header is binary encoded).
     */
    fun encodeFrameLengthAsString(): Boolean = this.encodeFrameLengthAsString

    init {
        this.addIdleEventHandler = b.addIdleEventHandler
        this.addLoggingHandler = b.addLoggingHandler
        this.encodeFrameLengthAsString = b.encodeFrameLengthAsString
        this.frameLengthFieldAdjust = b.frameLengthFieldAdjust
        this.frameLengthFieldLength = b.frameLengthFieldLength
        this.frameLengthFieldOffset = b.frameLengthFieldOffset
        this.idleTimeout = b.idleTimeout
        this.logFieldDescription = b.logFieldDescription
        this.logSensitiveData = b.logSensitiveData
        this.maxFrameLength = b.maxFrameLength
        this.replyOnError = b.replyOnError
        this.sensitiveDataFields = b.sensitiveDataFields
        this.responseCode = b.defaultResponseCode
    }

    @Suppress("UNCHECKED_CAST", "TooManyFunctions")
    open class Builder<B : Builder<B>> {
        internal var addLoggingHandler = false
        internal var addIdleEventHandler = false
        internal var logFieldDescription = true
        internal var logSensitiveData = true
        internal var replyOnError = false
        internal var idleTimeout = DEFAULT_IDLE_TIMEOUT_SECONDS
        internal var maxFrameLength = DEFAULT_MAX_FRAME_LENGTH
        internal var sensitiveDataFields: IntArray = IntArray(0)
        internal var frameLengthFieldLength = DEFAULT_FRAME_LENGTH_FIELD_LENGTH
        internal var frameLengthFieldOffset = DEFAULT_FRAME_LENGTH_FIELD_OFFSET
        internal var frameLengthFieldAdjust = DEFAULT_FRAME_LENGTH_FIELD_ADJUST
        internal var encodeFrameLengthAsString = false
        internal var defaultResponseCode = DEFAULT_RESPONSE_CODE

        /**
         * @param shouldAddIdleEventHandler `true` to add [IdleEventHandler].
         */
        fun addIdleEventHandler(shouldAddIdleEventHandler: Boolean = true): B =
            apply {
                addIdleEventHandler = shouldAddIdleEventHandler
            } as B

        /**
         * @param length the maximum frame length.
         */
        fun maxFrameLength(length: Int): B =
            apply {
                maxFrameLength = length
            } as B

        fun idleTimeout(timeout: Int): B =
            apply {
                idleTimeout = timeout
            } as B

        fun replyOnError(doReply: Boolean = true): B =
            apply {
                replyOnError = doReply
            } as B

        /**
         * @param value `true` if [IsoMessageLoggingHandler] should be added to the Netty pipeline.
         */
        fun addLoggingHandler(value: Boolean = true): B =
            apply {
                addLoggingHandler = value
            } as B

        /**
         * Should log sensitive data (unmasked) or not.
         * **Don't use on production!**
         *
         * @param logSensitiveData `true` to log sensitive data via logger
         */
        fun logSensitiveData(logSensitiveData: Boolean = true): B =
            apply {
                this.logSensitiveData = logSensitiveData
            } as B

        /**
         * @param shouldDescribe `true` to print ISO field descriptions in the log
         */
        fun describeFieldsInLog(shouldDescribe: Boolean = true): B =
            apply {
                logFieldDescription = shouldDescribe
            } as B

        /**
         * @param sensitiveDataFields Array of sensitive fields
         */
        fun sensitiveDataFields(vararg sensitiveDataFields: Int): B =
            apply {
                this.sensitiveDataFields = sensitiveDataFields
            } as B

        fun frameLengthFieldLength(frameLengthFieldLength: Int): B =
            apply {
                this.frameLengthFieldLength = frameLengthFieldLength
            } as B

        fun frameLengthFieldOffset(frameLengthFieldOffset: Int): B =
            apply {
                this.frameLengthFieldOffset = frameLengthFieldOffset
            } as B

        fun frameLengthFieldAdjust(frameLengthFieldAdjust: Int): B =
            apply {
                this.frameLengthFieldAdjust = frameLengthFieldAdjust
            } as B

        fun encodeFrameLengthAsString(encodeFrameLengthAsString: Boolean): B =
            apply {
                this.encodeFrameLengthAsString = encodeFrameLengthAsString
            } as B

        fun responseCode(responseCode: String): B =
            apply {
                this.defaultResponseCode = responseCode
            } as B
    }
}
