package ru.whyhappen.pcidss.iso8583.fields

import org.springframework.core.convert.TypeDescriptor
import org.springframework.core.convert.converter.Converter
import org.springframework.core.convert.support.GenericConversionService
import ru.whyhappen.pcidss.iso8583.spec.Spec

/**
 * [IsoField] for string values.
 */
class StringField(value: String? = null, spec: Spec) :
    AbstractIsoField<String>(value, spec, StringConversionService.INSTANCE) {
    override val valueType = TypeDescriptor.valueOf(String::class.java)

    override fun copyOf(): StringField = StringField(innerValue, spec)
}

class StringConversionService : GenericConversionService() {
    companion object {
        val INSTANCE = StringConversionService()
    }

    init {
        addConverter(StringToBytesConverter())
        addConverter(BytesToStringConverter())
    }

    class StringToBytesConverter : Converter<String, ByteArray> {
        override fun convert(source: String): ByteArray = source.toByteArray(Charsets.US_ASCII)
    }

    class BytesToStringConverter : Converter<ByteArray, String> {
        override fun convert(source: ByteArray): String = source.toString(Charsets.US_ASCII)
    }
}

