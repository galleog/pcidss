package ru.whyhappen.pcidss.iso8583.mti

/**
 * Position three of the MTI specifies the action being performed by the message.
 *
 * See [jreactive-iso8583](https://github.com/kpavlov/jreactive-8583).
 */
@Suppress("unused", "MagicNumber")
enum class MessageFunction(val value: Int) {
    /**
     * 'xx0x' Request from acquirer to issuer to carry out an action; issuer may accept or reject
     */
    REQUEST(0x0000),

    /**
     * `xx1x` Request response. Issuer response to a request
     */
    REQUEST_RESPONSE(0x0010),

    /**
     * `xx2x` Advice
     * Advice that an action has taken place; receiver can only accept, not reject
     */
    ADVICE(0x0020),

    /**
     * `xx3x` Advice response
     * Response to an advice
     */
    ADVICE_RESPONSE(0x0030),

    /**
     * `xx4x`	Notification
     * Notification that an event has taken place; receiver can only accept, not reject
     */
    NOTIFICATION(0x0040),

    /**
     * `xx5x`	Notification acknowledgement
     * Response to a notification
     */
    NOTIFICATION_ACK(0x0050),

    /**
     * `xx6x`	Instruction	ISO 8583:2003
     */
    INSTRUCTION(0x0060),

    /**
     * xx7x	Instruction acknowledgement
     */
    INSTRUCTION_ACK(0x0070),

    /**
     * `xx8x`	Reserved for ISO use
     *
     *
     * Some implementations (such as MasterCard) use for positive acknowledgment.[4]
     */
    RESERVED_8(0x0080),

    /**
     * `xx9x`	Some implementations (such as MasterCard) use for negative acknowledgement.
     */
    RESERVED_9(0x0090)
}
