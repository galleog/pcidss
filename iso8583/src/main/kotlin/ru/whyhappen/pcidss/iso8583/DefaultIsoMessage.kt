package ru.whyhappen.pcidss.iso8583

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.whyhappen.pcidss.iso8583.fields.Bitmap
import ru.whyhappen.pcidss.iso8583.fields.IsoField
import ru.whyhappen.pcidss.iso8583.spec.MessageSpec

/**
 * Default implementation of [IsoMessage].
 */
class DefaultIsoMessage internal constructor(spec: MessageSpec) : IsoMessage {
    companion object {
        private const val MTI_FIELD_ID = 0
        private const val BITMAP_FIELD_ID = 1

        private val logger: Logger = LoggerFactory.getLogger(DefaultIsoMessage::class.java)
    }

    override var mti: Int
        get() = mtiField.getValue(String::class.java)?.toInt(16) ?: 0
        set(value) = mtiField.setValue("%04x".format(value))

    override val fields: Map<Int, IsoField>
        get() = allFields.filter { (key, _) -> key in setFields }.toMap()

    private val mtiField: IsoField
    private val bitmap: Bitmap

    private val setFields = mutableSetOf<Int>()
    private val allFields: Map<Int, IsoField>

    init {
        mtiField = createMtiField(spec)
        mti = 0

        bitmap = createBitmap(spec)
        allFields = spec.fields.filter { (id, _) -> id != MTI_FIELD_ID && !bitmap.isBitmapPresenceBit(id) }
            .mapValues { (_, field) ->
                field.copyOf().apply { reset() }
            }
    }

    override fun <T : Any> setFieldValue(id: Int, value: T) {
        check(id in allFields) { "Specification for field $id not found" }

        allFields[id]!!.setValue(value)
        setFields += id
    }

    override fun <T> getFieldValue(id: Int, cls: Class<T>): T? =
        if (id in setFields) allFields[id]?.getValue(cls) else null

    override fun unsetFields(vararg ids: Int) {
        setFields.removeAll(ids.toSet())

        for (id in ids) fields[id]?.reset()
    }

    override fun hasField(id: Int): Boolean = id in setFields

    /**
     * Serializes MTI, bitmap, and all other fields of the message into its binary representation.
     *
     * @see [IsoField.pack]
     */
    override fun pack(): ByteArray {
        bitmap.reset()

        val packableIds = setFields.sorted()
        for (id in packableIds) {
            bitmap.set(id)
        }

        // pack MTI
        var packed = runCatching { mtiField.pack() }.getOrElse { e -> throwException(e) { "Failed to pack MTI" } }
        // pack bitmap
        packed += runCatching { bitmap.pack() }.getOrElse { e -> throwException(e) { "Failed to pack bitmap" } }

        for (id in packableIds) {
            packed += allFields[id]?.run {
                runCatching { pack() }.getOrElse { e -> throwException(e) { "Failed to pack field $id" } }
            } ?: continue
        }

        return packed
    }

    /**
     * Deserializes the message from its binary representation.
     *
     * @see [IsoField.unpack]
     */
    override fun unpack(bytes: ByteArray): Int {
        setFields.clear()
        bitmap.reset()

        // unpack MTI
        var read = runCatching { mtiField.unpack(bytes) }
            .getOrElse { e -> throwException(e) { "Failed to unpack MTI" } }

        // unpack bitmap
        read += runCatching {
            bitmap.unpack(bytes.sliceArray(read until bytes.size))
        }.getOrElse { e -> throwException(e) { "Failed to unpack bitmap" } }

        for (i in 2..bitmap.length) {
            // skip unset bits and bitmap presence bits.
            // For default bitmap of length 64 these are bits 1, 65, 129, 193, etc.
            if (bitmap.isSet(i) && !bitmap.isBitmapPresenceBit(i)) {
                runCatching {
                    check(i in allFields) { "Specification for field $i not found" }

                    read += allFields[i]!!.unpack(bytes.sliceArray(read until bytes.size))
                    setFields += i
                }.getOrElse { e -> throwException(e) { "Failed to unpack field $i" } }
            }
        }

        return read
    }

    override fun copyOf(): IsoMessage {
        val newMessage = DefaultIsoMessage(
            MessageSpec(this.allFields + (MTI_FIELD_ID to this.mtiField) + (BITMAP_FIELD_ID to this.bitmap))
        )

        // copy all fields of the original message to the new message
        val bytes = this.pack()
        newMessage.unpack(bytes)
        return newMessage
    }

    override fun toString(): String {
        return "${this::class.simpleName}(mti = ${"%04x".format(mti)})"
    }

    private fun createMtiField(spec: MessageSpec): IsoField =
        spec.fields.getOrElse(MTI_FIELD_ID) { throw IllegalArgumentException("MTI header not found") }.copyOf()

    private fun createBitmap(spec: MessageSpec): Bitmap {
        val copy = spec.fields
            .getOrElse(BITMAP_FIELD_ID) { throw IllegalArgumentException("Bitmap not found") }
            .copyOf()
        return (copy as Bitmap).apply { reset() }
    }

    private fun throwException(cause: Throwable, lazyMessage: () -> String): Nothing {
        val message = lazyMessage()
        logger.error(message, cause)
        throw IsoMessageException(message, cause)
    }
}