package ru.whyhappen.pcidss.iso8583.mti

/**
 * MTI (Message Type Identifier) is a four-byte code that identifies the type of message.
 *
 * See [jreactive-iso8583](https://github.com/kpavlov/jreactive-8583).
 */
object Mti {
    @JvmStatic
    fun mtiValue(
        iso8583Version: ISO8583Version,
        messageClass: MessageClass,
        messageFunction: MessageFunction,
        messageOrigin: MessageOrigin
    ): Int = iso8583Version.value +
            messageClass.value +
            messageFunction.value +
            messageOrigin.value

}
