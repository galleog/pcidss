package ru.whyhappen.pcidss.iso8583.api.j8583

import com.solab.iso8583.IsoMessage
import com.solab.iso8583.IsoType
import com.solab.iso8583.MessageFactory
import java.io.UnsupportedEncodingException
import java.text.ParseException
import java.util.*

/**
 * [MessageFactory] that can parse ISO8583 messages without strict typification.
 */
class LooseMessageFactory<T : IsoMessage> : MessageFactory<T>() {
    @Throws(ParseException::class, UnsupportedEncodingException::class)
    override fun parseMessage(buf: ByteArray, isoHeaderLength: Int, binaryIsoHeader: Boolean): T {
        val minLength =
            isoHeaderLength + (if (isBinaryHeader) 2 else 4) + (if (isUseBinaryBitmap || isBinaryHeader) 8 else 16)
        if (buf.size < minLength) {
            throw ParseException("Insufficient buffer length, needs to be at least $minLength", 0)
        }

        val m: T = if (binaryIsoHeader && isoHeaderLength > 0) {
            val _bih = ByteArray(isoHeaderLength)
            System.arraycopy(buf, 0, _bih, 0, isoHeaderLength)
            createIsoMessageWithBinaryHeader(_bih)
        } else {
            createIsoMessage(
                if (isoHeaderLength > 0) String(buf, 0, isoHeaderLength, charset(characterEncoding))
                else null
            )
        }

        m.characterEncoding = characterEncoding
        val type = when {
            isBinaryHeader ->
                ((buf[isoHeaderLength].toInt() and 0xff) shl 8) or (buf[isoHeaderLength + 1].toInt() and 0xff)

            isForceStringEncoding ->
                String(buf, isoHeaderLength, 4, charset(characterEncoding)).toInt(16)

            else -> {
                (((buf[isoHeaderLength] - 48) shl 12)
                        or ((buf[isoHeaderLength + 1] - 48) shl 8)
                        or ((buf[isoHeaderLength + 2] - 48) shl 4)
                        or (buf[isoHeaderLength + 3] - 48))
            }
        }
        m.type = type

        // parse the bitmap (primary first)
        val bs = BitSet(64)
        var pos = 0
        if (isBinaryHeader || isUseBinaryBitmap) {
            val bitmapStart = isoHeaderLength + (if (isBinaryHeader) 2 else 4)
            for (i in bitmapStart until 8 + bitmapStart) {
                var bit = 128
                for (b in 0..7) {
                    bs[pos++] = (buf[i].toInt() and bit) != 0
                    bit = bit shr 1
                }
            }

            // check for secondary bitmap and parse if necessary
            if (bs[0]) {
                if (buf.size < minLength + 8) {
                    throw ParseException("Insufficient length for secondary bitmap", minLength)
                }

                for (i in 8 + bitmapStart until 16 + bitmapStart) {
                    var bit = 128
                    for (b in 0..7) {
                        bs[pos++] = (buf[i].toInt() and bit) != 0
                        bit = bit shr 1
                    }
                }
                pos = minLength + 8
            } else {
                pos = minLength
            }
        } else {
            // ASCII parsing
            try {
                val bitmapBuffer: ByteArray
                if (isForceStringEncoding) {
                    val _bb = String(buf, isoHeaderLength + 4, 16, charset(characterEncoding)).toByteArray()
                    bitmapBuffer = ByteArray(36 + isoHeaderLength)
                    System.arraycopy(_bb, 0, bitmapBuffer, 4 + isoHeaderLength, 16)
                } else {
                    bitmapBuffer = buf
                }

                for (i in isoHeaderLength + 4 until isoHeaderLength + 20) {
                    when {
                        bitmapBuffer[i] >= '0'.code.toByte() && bitmapBuffer[i] <= '9'.code.toByte() -> {
                            bs[pos++] = ((bitmapBuffer[i] - 48) and 8) > 0
                            bs[pos++] = ((bitmapBuffer[i] - 48) and 4) > 0
                            bs[pos++] = ((bitmapBuffer[i] - 48) and 2) > 0
                            bs[pos++] = ((bitmapBuffer[i] - 48) and 1) > 0
                        }

                        bitmapBuffer[i] >= 'A'.code.toByte() && bitmapBuffer[i] <= 'F'.code.toByte() -> {
                            bs[pos++] = ((bitmapBuffer[i] - 55) and 8) > 0
                            bs[pos++] = ((bitmapBuffer[i] - 55) and 4) > 0
                            bs[pos++] = ((bitmapBuffer[i] - 55) and 2) > 0
                            bs[pos++] = ((bitmapBuffer[i] - 55) and 1) > 0
                        }

                        bitmapBuffer[i] >= 'a'.code.toByte() && bitmapBuffer[i] <= 'f'.code.toByte() -> {
                            bs[pos++] = ((bitmapBuffer[i] - 87) and 8) > 0
                            bs[pos++] = ((bitmapBuffer[i] - 87) and 4) > 0
                            bs[pos++] = ((bitmapBuffer[i] - 87) and 2) > 0
                            bs[pos++] = ((bitmapBuffer[i] - 87) and 1) > 0
                        }
                    }
                }

                // check for secondary bitmap and parse it if necessary
                if (bs[0]) {
                    if (buf.size < minLength + 16) {
                        throw ParseException("Insufficient length for secondary bitmap", minLength)
                    }

                    if (isForceStringEncoding) {
                        val _bb = String(buf, isoHeaderLength + 20, 16, charset(characterEncoding)).toByteArray()
                        System.arraycopy(_bb, 0, bitmapBuffer, 20 + isoHeaderLength, 16)
                    }
                    for (i in isoHeaderLength + 20 until isoHeaderLength + 36) {
                        when {
                            bitmapBuffer[i] >= '0'.code.toByte() && bitmapBuffer[i] <= '9'.code.toByte() -> {
                                bs[pos++] = ((bitmapBuffer[i] - 48) and 8) > 0
                                bs[pos++] = ((bitmapBuffer[i] - 48) and 4) > 0
                                bs[pos++] = ((bitmapBuffer[i] - 48) and 2) > 0
                                bs[pos++] = ((bitmapBuffer[i] - 48) and 1) > 0
                            }

                            bitmapBuffer[i] >= 'A'.code.toByte() && bitmapBuffer[i] <= 'F'.code.toByte() -> {
                                bs[pos++] = ((bitmapBuffer[i] - 55) and 8) > 0
                                bs[pos++] = ((bitmapBuffer[i] - 55) and 4) > 0
                                bs[pos++] = ((bitmapBuffer[i] - 55) and 2) > 0
                                bs[pos++] = ((bitmapBuffer[i] - 55) and 1) > 0
                            }

                            bitmapBuffer[i] >= 'a'.code.toByte() && bitmapBuffer[i] <= 'f'.code.toByte() -> {
                                bs[pos++] = ((bitmapBuffer[i] - 87) and 8) > 0
                                bs[pos++] = ((bitmapBuffer[i] - 87) and 4) > 0
                                bs[pos++] = ((bitmapBuffer[i] - 87) and 2) > 0
                                bs[pos++] = ((bitmapBuffer[i] - 87) and 1) > 0
                            }
                        }
                    }
                    pos = 16 + minLength
                } else {
                    pos = minLength
                }
            } catch (ex: NumberFormatException) {
                val _e = ParseException("Invalid ISO8583 bitmap", pos)
                _e.initCause(ex)
                throw _e
            }
        }

        // parse each field
        val parseGuide = parseMap[type] ?: parseMap[-1]
        val index = parseOrder[type] ?: parseOrder[-1]
        if (parseGuide == null || index == null) {
            val errMsg =
                "ISO8583 MessageFactory has no parsing guide for message type 0x${"%04x".format(type)} [${String(buf)}]"
            log.error(errMsg)
            throw ParseException(errMsg, 0)
        }

        // first we check if the message contains fields not specified in the parsing template
        for (i in 1..<bs.length()) {
            if (bs[i] && !index.contains(i + 1)) {
                log.warn(
                    "ISO8583 MessageFactory cannot parse field {}: unspecified in parsing guide for type 0x{}",
                    i + 1, "%04x".format(type)
                )
                throw ParseException("ISO8583 MessageFactory cannot parse fields", 0)
            }
        }

        // now we parse each field
        if (isBinaryFields) {
            for (i in index) {
                val fpi = parseGuide[i]
                if (bs[i - 1]) {
                    if (ignoreLastMissingField && pos >= buf.size && i == index[index.size - 1]) {
                        log.warn("Field {} is not really in the message even though it's in the bitmap", i)
                        bs.clear(i - 1)
                    } else {
                        var decoder = fpi?.decoder
                        if (decoder == null) {
                            decoder = getCustomField<Any>(i)
                        }
                        val value = fpi?.parseBinary(i, buf, pos, decoder)
                        m.setField(i, value)
                        value?.let {
                            pos += when {
                                it.type == IsoType.NUMERIC || it.type.isDateTimeType
                                        || it.type == IsoType.AMOUNT -> (it.length / 2) + (it.length % 2)

                                it.type == IsoType.LLBCDBIN || it.type == IsoType.LLLBCDBIN
                                        || it.type == IsoType.LLLLBCDBIN ->
                                    it.length / 2 + (if (it.length % 2 == 0) 0 else 1)

                                else -> value.length
                            }

                            when (it.type) {
                                IsoType.LLVAR, IsoType.LLBIN, IsoType.LLBCDBIN -> pos++

                                IsoType.LLLVAR, IsoType.LLLBIN, IsoType.LLLBCDBIN, IsoType.LLLLVAR,
                                IsoType.LLLLBIN, IsoType.LLLLBCDBIN -> pos += 2

                                else -> {} // do nothing
                            }
                        }
                    }
                }
            }
        } else {
            for (i in index) {
                val fpi = parseGuide[i]
                if (bs[i - 1]) {
                    if (ignoreLastMissingField && pos >= buf.size && i == index[index.size - 1]) {
                        log.warn("Field {} is not really in the message even though it's in the bitmap", i)
                        bs.clear(i - 1)
                    } else {
                        var decoder = fpi?.decoder
                        if (decoder == null) {
                            decoder = getCustomField<Any>(i)
                        }
                        val value = fpi?.parse(i, buf, pos, decoder)
                        m.setField(i, value)
                        value?.let {
                            // to get the correct next position, we need to get the number of bytes, not chars
                            pos += it.toString().toByteArray(charset(it.characterEncoding)).size
                            when (it.type) {
                                IsoType.LLVAR, IsoType.LLBIN, IsoType.LLBCDBIN -> pos += 2
                                IsoType.LLLVAR, IsoType.LLLBIN, IsoType.LLLBCDBIN -> pos += 3
                                IsoType.LLLLVAR, IsoType.LLLLBIN, IsoType.LLLLBCDBIN -> pos += 4
                                else -> {} // do nothing
                            }
                        }
                    }
                }
            }
        }

        m.isBinaryHeader = isBinaryHeader
        m.isBinaryFields = isBinaryFields
        m.isBinaryBitmap = isUseBinaryBitmap
        m.isEncodeVariableLengthFieldsInHex = isVariableLengthFieldsInHex
        return m
    }
}
