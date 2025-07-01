package ru.whyhappen.pcidss.iso8583.mti

/**
 * Position four of the MTI specifies the origin of the message.
 *
 * See [jreactive-iso8583](https://github.com/kpavlov/jreactive-8583).
 */
@Suppress("unused", "MagicNumber")
enum class MessageOrigin(val value: Int) {
    /**
     * xxx0	Acquirer
     */
    ACQUIRER(0x0000),

    /**
     * xxx1	Acquirer repeat
     */
    ACQUIRER_REPEAT(0x0001),

    /**
     * xxx2	Issuer
     */
    ISSUER(0x0002),

    /**
     * xxx3	Issuer repeat
     */
    ISSUER_REPEAT(0x0003),

    /**
     * xxx4	Other
     */
    OTHER(0x0004),

    /**
     * xxx5	Other repeat
     */
    OTHER_REPEAT(0x0005)
}
