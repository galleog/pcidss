package ru.whyhappen.pcidss.iso8583.fields

import org.springframework.core.convert.TypeDescriptor
import org.springframework.core.convert.converter.Converter
import org.springframework.core.convert.support.GenericConversionService
import ru.whyhappen.pcidss.iso8583.spec.Spec

/**
 * [IsoField] for binary values.
 */
class BinaryField(value: ByteArray? = null, spec: Spec) :
    AbstractIsoField<ByteArray>(value, spec, BinaryConversionService.INSTANCE) {
    override val valueType = TypeDescriptor.valueOf(ByteArray::class.java)

    override fun copyOf(): IsoField = BinaryField(innerValue?.copyOf(), spec)
}

class BinaryConversionService : GenericConversionService() {
    companion object {
        val INSTANCE = BinaryConversionService()
    }

    init {
        addConverter(BytesToAsciiHexConverter())
        addConverter(AsciiHexToBytesConverter())
    }

    @OptIn(ExperimentalStdlibApi::class)
    class BytesToAsciiHexConverter : Converter<ByteArray, String> {
        override fun convert(source: ByteArray): String = source.toHexString(HexFormat.UpperCase)
    }

    @OptIn(ExperimentalStdlibApi::class)
    class AsciiHexToBytesConverter : Converter<String, ByteArray> {
        override fun convert(source: String): ByteArray = source.hexToByteArray()
    }
}