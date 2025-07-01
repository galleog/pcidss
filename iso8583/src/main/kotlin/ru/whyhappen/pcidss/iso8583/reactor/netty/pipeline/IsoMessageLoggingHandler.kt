package ru.whyhappen.pcidss.iso8583.reactor.netty.pipeline

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import ru.whyhappen.pcidss.iso8583.IsoMessage
import ru.whyhappen.pcidss.iso8583.fields.IsoField

/**
 * Netty channel handler responsible for logging messages.
 *
 * According to PCI DSS, sensitive cardholder data, like PAN and track data, should not be exposed.
 * When running in secure mode, sensitive cardholder data will be printed masked.
 *
 * See [jreactive-iso8583](https://github.com/kpavlov/jreactive-8583).
 */
@Sharable
class IsoMessageLoggingHandler(
    level: LogLevel,
    private val printSensitiveData: Boolean,
    private val printFieldDescriptions: Boolean,
    private val maskedFields: IntArray = DEFAULT_MASKED_FIELDS,
) : LoggingHandler(level) {
    companion object {
        internal const val MASKED_VALUE = "***"

        private val DEFAULT_MASKED_FIELDS: IntArray =
            intArrayOf(
                2,  // PAN
                34, // PAN extended
                35, // track 2
                36, // track 3
                45, // track 1
            )
    }

    public override fun format(ctx: ChannelHandlerContext, eventName: String, arg: Any): String =
        if (arg is IsoMessage) {
            super.format(ctx, eventName, formatIsoMessage(arg))
        } else {
            super.format(ctx, eventName, arg)
        }

    private fun formatIsoMessage(isoMessage: IsoMessage): String {
        val sb = StringBuilder()
        sb.append("MTI: ").append("%04x".format(isoMessage.mti))
        for ((id, field) in isoMessage.fields) {
            sb.append("\n  ").append(id).append(": [")
            if (printFieldDescriptions) {
                sb.append(field.spec.description).append(": ")
            }
            val formattedValue = getFormattedValue(field, id)
            sb.append(field::class.simpleName)
                .append('(')
                .append(field.spec.length)
                .append(")] = '")
                .append(formattedValue)
                .append('\'')
        }
        return sb.toString()
    }

    private fun getFormattedValue(field: IsoField, id: Int): String =
        if (printSensitiveData || id !in maskedFields) (field.getValue(String::class.java) ?: "null")
        else MASKED_VALUE
}
