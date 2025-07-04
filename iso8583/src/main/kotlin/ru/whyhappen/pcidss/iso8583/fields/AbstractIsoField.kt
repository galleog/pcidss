package ru.whyhappen.pcidss.iso8583.fields

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.TypeDescriptor
import ru.whyhappen.pcidss.iso8583.spec.Spec

/**
 * Base class for all implementations of [IsoField].
 *
 * @param V the type of the field value.
 */
abstract class AbstractIsoField<V : Any>(
    /**
     * Field value.
     */
    protected var innerValue: V?,
    /**
     * Field specification.
     */
    final override val spec: Spec,
    /**
     * Converter to/from different types that [setValue] and [getValue] should support.
     * It should at least implement conversion to/from [String] and [ByteArray].
     */
    private val converter: ConversionService
) : IsoField {
    protected val logger: Logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Type of the field value.
     */
    protected abstract val valueType: TypeDescriptor

    override var bytes: ByteArray
        get() = getValue(ByteArray::class.java) ?: byteArrayOf()
        set(value) = setValue(value)

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> setValue(value: T) {
        require(converter.canConvert(TypeDescriptor.forObject(value), valueType)) {
            "Can't convert value from ${value::class.java.name} to ${valueType.name}"
        }
        innerValue = converter.convert(value, valueType) as V
    }


    @Suppress("UNCHECKED_CAST")
    override fun <T> getValue(cls: Class<T>): T? {
        val typeDescriptor = TypeDescriptor.valueOf(cls)
        require(converter.canConvert(valueType, typeDescriptor)) {
            "Can't convert value from ${valueType.name} to ${cls.name}"
        }
        return converter.convert(innerValue, valueType, typeDescriptor) as T?
    }

    override fun pack(): ByteArray {
        logger.debug("Packing field using {}", spec)
        return spec.packer.pack(bytes)
    }

    override fun unpack(bytes: ByteArray): Int {
        logger.debug("Unpacking field using {}", spec)

        val (value, read) = spec.packer.unpack(bytes)
        setValue(value)
        return read
    }

    override fun reset() {
        innerValue = null
    }

    override fun toString(): String {
        return "${this::class.simpleName}(spec = $spec)"
    }
}