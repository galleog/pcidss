package ru.whyhappen.pcidss.iso8583.api.j8583.config

import com.solab.iso8583.IsoMessage
import com.solab.iso8583.IsoType
import com.solab.iso8583.IsoValue
import com.solab.iso8583.MessageFactory
import com.solab.iso8583.codecs.CompositeField
import com.solab.iso8583.parse.FieldParseInfo
import com.solab.iso8583.parse.date.DateTimeParseInfo
import com.solab.iso8583.parse.temporal.TemporalParseInfo
import com.solab.iso8583.util.HexCodec
import org.slf4j.LoggerFactory
import ru.whyhappen.pcidss.iso8583.api.j8583.config.model.*
import java.time.ZoneId
import java.util.*
import java.util.regex.Pattern

/**
 * Abstract base implementation of [MessageFactoryConfigurer].
 */
abstract class AbstractMessageFactoryConfigurer<T : IsoMessage>(
    private val messageCreator: (Int) -> T,
) : MessageFactoryConfigurer<T> {
    private val parseData = mutableMapOf<Int, Map<Int, FieldParseInfo>>()

    companion object {
        private const val UNTYPED = "untyped"
        private const val EXCLUDE = "exclude"

        private val logger = LoggerFactory.getLogger(AbstractMessageFactoryConfigurer::class.java)
        private val templateParsers = mutableMapOf<IsoType, TemporalParseInfo<*>>()

        private fun getTemplateParser(isoType: IsoType): TemporalParseInfo<*> {
            return templateParsers.computeIfAbsent(isoType) { type -> TemporalParseInfo.createParserForType(type) }
        }

        /**
         * Parses a message type expressed as a hex string and returns the integer number.
         * For example, "0200" or "200" return the number 512 (0x200)
         */
        private fun parseType(type: String): Int {
            require(Pattern.matches("""^[01289]?[1-8][0-9][0-5]$""", type)) {
                "ISO8583 message type must be a valid MTI"
            }
            return type.toInt(16)
        }
    }

    /**
     * Applies [Iso8583Config]s to a message factory.
     */
    protected open fun applyConfigs(messageFactory: MessageFactory<T>, configs: List<Iso8583Config>) {
        for (config in configs) {
            applyConfig(messageFactory, config)
        }
    }

    private fun applyConfig(messageFactory: MessageFactory<T>, config: Iso8583Config) {
        applyHeaders(messageFactory, config.headers)
        applyTemplates(messageFactory, config.templates)
        applyParses(messageFactory, config.parses)
    }

    private fun applyHeaders(messageFactory: MessageFactory<T>, headers: List<Header>) {
        val refs = mutableListOf<Triple<Int, Header, Int>>()

        for (header in headers) {
            val type = parseType(header.type)

            if (header.ref.isNullOrEmpty()) {
                val binary = header.binary ?: false
                logger.trace(
                    "Adding {}ISO8583 header for type {}: {}",
                    if (binary) "binary " else "",
                    header.type,
                    header.value
                )

                if (binary) {
                    messageFactory.setBinaryIsoHeader(type, HexCodec.hexDecode(header.value))
                } else {
                    messageFactory.setIsoHeader(type, header.value)
                }
            } else {
                // header refers to another one
                refs += Triple(type, header, parseType(header.ref))
            }
        }

        // process references to other headers
        for ((type, header, ref) in refs) {
            val value = messageFactory.getIsoHeader(ref) ?: throw IllegalArgumentException(
                "Header ${header.type} refers to nonexistent header ${header.ref}"
            )
            logger.debug("Adding ISO8583 header for type {}: {} (copied from 0x{})", header.type, value, header.ref)
            messageFactory.setIsoHeader(type, value)
        }
    }

    private fun applyTemplates(messageFactory: MessageFactory<T>, templates: List<Template>) {
        val extensions = mutableListOf<Triple<Int, Template, Int>>()

        for (template in templates) {
            val type = if (template.type != null) parseType(template.type) else -1

            when {
                template.extends.isNullOrEmpty() -> {
                    val message = messageCreator(type).apply {
                        characterEncoding = messageFactory.characterEncoding

                        for (field in template.fields) {
                            val value = getIsoValue(messageFactory, field)
                            logger.debug("Template {}. Adding field {}: {}", template.type, field.num, value)
                            setField(field.num, value)
                        }
                    }
                    messageFactory.addMessageTemplate(message)
                }

                // template extends one untyped one
                template.extends == UNTYPED -> extensions += Triple(type, template, -1)

                // template extends another one
                else -> extensions += Triple(type, template, parseType(template.extends))
            }
        }

        // process extensions of other templates
        for ((type, template, ref) in extensions) {
            val extension = messageFactory.getMessageTemplate(ref) ?: throw IllegalArgumentException(
                "Template ${template.type} extends nonexistent template ${template.extends}"
            )

            val message = messageCreator(type).apply {
                characterEncoding = messageFactory.characterEncoding
            }

            val extendedFields = (2..128).asSequence()
                .filter(extension::hasField)
                .associateWith { extension.getField<Any>(it).clone() }
                .toMap()
            val excludedFields = template.fields
                .asSequence()
                .filter { it.type == EXCLUDE }
                .map { it.num }
                .toSet()
            val templateFields = template.fields
                .filter { it.type != EXCLUDE }
                .associate { it.num to getIsoValue(messageFactory, it) }

            for ((key, value) in (extendedFields - excludedFields) + templateFields) {
                logger.debug("Template {}. Adding field {}: {}", template.type, key, value)
                message.setField(key, value)
            }
            messageFactory.addMessageTemplate(message)
        }
    }

    private fun getIsoValue(messageFactory: MessageFactory<T>, field: Field): IsoValue<*> {
        val isoType = IsoType.valueOf(field.type)
        require(!isoType.needsLength() || field.length != null) { "Field length must be set" }

        val isoValue = if (field.subFields.isEmpty()) {
            // support for custom fields omitted
            when {
                isoType.isDateTimeType && messageFactory.isUseDateTimeApi -> {
                    val parser = getTemplateParser(isoType).apply {
                        zoneId = if (field.tz == null) ZoneId.systemDefault() else ZoneId.of(field.tz)
                    }

                    runCatching {
                        parser.parse<Any>(1, field.value.orEmpty().toByteArray(), 0, null)
                    }.getOrElse { e ->
                        logger.error(
                            "Cannot parse template value {} for field of type {}, using String",
                            field.value.orEmpty(),
                            isoType,
                            e
                        )
                        IsoValue(isoType, field.value.orEmpty())
                    }
                }

                isoType.needsLength() -> IsoValue(isoType, field.value.orEmpty(), field.length!!)
                else -> IsoValue(isoType, field.value.orEmpty())
            }
        } else {
            // composite field
            val composite = CompositeField()
            for (subField in field.subFields) {
                composite.addValue(getIsoValue(messageFactory, subField))
            }

            if (isoType.needsLength()) IsoValue(isoType, composite, field.length!!, composite)
            else IsoValue(isoType, composite, composite)
        }

        field.tz?.let {
            isoValue.timeZone = TimeZone.getTimeZone(it)
        }

        return isoValue.apply {
            characterEncoding = messageFactory.characterEncoding
        }
    }

    private fun applyParses(messageFactory: MessageFactory<T>, parses: List<Parse>) {
        val extensions = mutableListOf<Triple<Int, Parse, Int>>()

        for (parse in parses) {
            val type = if (parse.type != null) parseType(parse.type) else -1

            when {
                parse.extends.isNullOrEmpty() -> {
                    val parseMap = parse.fields.associate { field ->
                        logger.debug("Parse {}. Adding field {} of type {}", parse.type, field.num, field.type)
                        field.num to getParserInfo(messageFactory, field)
                    }
                    messageFactory.setParseMap(type, parseMap)
                    parseData += type to parseMap
                }

                // parse element extends untyped one
                parse.extends == UNTYPED -> extensions += Triple(type, parse, -1)

                // parse element extends another one
                else -> extensions += Triple(type, parse, parseType(parse.extends))
            }
        }

        for ((type, parse, ref) in extensions) {
            val extendedParseMap = parseData[ref] ?: throw IllegalArgumentException(
                "Parse element ${parse.type} extends nonexistent template ${parse.extends}"
            )
            val excludedFields = parse.fields
                .asSequence()
                .filter { it.type == EXCLUDE }
                .map { it.num }
                .toSet()
            val parseMap = parse.fields
                .filter { it.type != EXCLUDE }
                .associate { it.num to getParserInfo(messageFactory, it) }
            val resultMap = (extendedParseMap - excludedFields) + parseMap

            if (logger.isDebugEnabled) {
                for ((key, value) in resultMap) {
                    logger.debug("Parse {}. Adding field {} of type {}", parse.type, key, value.type)
                }
            }

            messageFactory.setParseMap(type, resultMap)
            parseData += type to resultMap
        }
    }

    private fun getParserInfo(messageFactory: MessageFactory<T>, field: Field): FieldParseInfo {
        val type = IsoType.valueOf(field.type)
        val fpi = FieldParseInfo.getInstance(
            type,
            field.length ?: 0,
            messageFactory.characterEncoding,
            messageFactory.isUseDateTimeApi
        )

        if (field.subFields.isNotEmpty()) {
            val composite = CompositeField().apply {
                for (subField in field.subFields) {
                    addParser(getParserInfo(messageFactory, subField))
                }
            }
            fpi.decoder = composite
        }

        field.tz?.let {
            when (fpi) {
                is DateTimeParseInfo -> fpi.timeZone = TimeZone.getTimeZone(it)
                is TemporalParseInfo<*> -> fpi.zoneId = ZoneId.of(it)
                else -> {
                    // do nothing
                }
            }
        }

        return fpi
    }
}