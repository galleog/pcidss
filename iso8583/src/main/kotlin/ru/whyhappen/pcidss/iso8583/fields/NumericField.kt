package ru.whyhappen.pcidss.iso8583.fields

import org.springframework.core.convert.TypeDescriptor
import org.springframework.core.convert.converter.Converter
import org.springframework.core.convert.support.GenericConversionService
import ru.whyhappen.pcidss.iso8583.spec.Spec
import java.math.BigInteger

/**
 * [IsoField] for numeric values.
 */
class NumericField(value: BigInteger? = null, spec: Spec) :
    AbstractIsoField<BigInteger>(value, spec, BigIntegerConversionService.INSTANCE) {
    override val valueType: TypeDescriptor = TypeDescriptor.valueOf(BigInteger::class.java)

    override fun copyOf(): NumericField = NumericField(innerValue, spec)

    override fun reset() {
        innerValue = null
    }
}

class BigIntegerConversionService : GenericConversionService() {
    companion object {
        val INSTANCE = BigIntegerConversionService()
    }

    init {
        addConverter(BigIntegerToStringConverter())
        addConverter(StringToBigIntegerConverter())
        addConverter(BigIntegerToBytesConverter())
        addConverter(BytesToBigIntegerConverter())
        addConverter(BigIntegerToIntConverter())
        addConverter(IntToBigIntegerConverter())
        addConverter(BigIntegerToLongConverter())
        addConverter(LongToBigIntegerConverter())
    }

    class BigIntegerToStringConverter : Converter<BigInteger, String> {
        override fun convert(source: BigInteger): String = source.toString()
    }

    class StringToBigIntegerConverter : Converter<String, BigInteger> {
        override fun convert(source: String): BigInteger = source.toBigInteger()
    }

    class BigIntegerToBytesConverter : Converter<BigInteger, ByteArray> {
        override fun convert(source: BigInteger): ByteArray = source.toString().toByteArray(Charsets.US_ASCII)
    }

    class BytesToBigIntegerConverter : Converter<ByteArray, BigInteger> {
        override fun convert(source: ByteArray): BigInteger {
            return if (source.isEmpty()) {
                // string representation of an empty byte array would become "" which returns an error.
                // However, for example, "0000" (value 0 left-padded with '0') should have 0 as output,
                // not an error. So if the byte array is empty, convert it to 0 instead of parsing the bytes
                BigInteger.ZERO
            } else {
                source.toString(Charsets.US_ASCII).toBigInteger()
            }
        }
    }

    class BigIntegerToIntConverter : Converter<BigInteger, Int?> {
        override fun convert(source: BigInteger): Int = source.intValueExact()
    }

    class IntToBigIntegerConverter : Converter<Int, BigInteger> {
        override fun convert(source: Int): BigInteger = source.toBigInteger()
    }

    class BigIntegerToLongConverter: Converter<BigInteger, Long?> {
        override fun convert(source: BigInteger): Long = source.longValueExact()
    }

    class LongToBigIntegerConverter: Converter<Long, BigInteger> {
        override fun convert(source: Long): BigInteger = source.toBigInteger()
    }
}