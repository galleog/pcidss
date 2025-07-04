package ru.whyhappen.pcidss.iso8583

import ru.whyhappen.pcidss.iso8583.fields.IsoField

/**
 * Interface to represent an ISO8583 message.
 */
interface IsoMessage {
    /**
     * MTI (Message Type Identifier).
     * A four-byte code that identifies the type of the message.
     */
    var mti: Int

    /**
     * Fields the message contains.
     */
    val fields: Map<Int, IsoField>

    /**
     * Sets the value of the field identified by its `id`.
     *
     * @param T the type of the field value
     * @param id the number of the field
     * @param value the field value
     *
     * @see [IsoField.setValue]
     */
    fun <T : Any> setFieldValue(id: Int, value: T)
    /**
     * Gets the value of the field identified by its `id`.
     *
     * @param T the type of the result
     * @param id the number of the field
     * @param cls the Java class of the value to get
     *
     * @see [IsoField.getValue]
     */
    fun <T> getFieldValue(id: Int, cls: Class<T>): T?

    /**
     * Marks the fields with the given `ids` as not set and resets its value.
     * This effectively removes the field's value and excludes it from operations like [pack].
     */
    fun unsetFields(vararg ids: Int)

    /**
     * Indicates if the value of the field with the given `id` was set.
     */
    fun hasField(id: Int): Boolean

    /**
     * Serializes the message into its binary representation.
     */
    fun pack(): ByteArray
    /**
     * Deserializes the message from its binary representation.
     *
     * @return the number of read bytes
     */
    fun unpack(bytes: ByteArray): Int

    /**
     * Creates a copy of the message.
     */
    fun copyOf(): IsoMessage
}

/**
 * Exception thrown by [IsoMessage] when performing operations such as packing and unpacking.
 */
class IsoMessageException(message: String, cause: Throwable) : IsoException(message, cause)