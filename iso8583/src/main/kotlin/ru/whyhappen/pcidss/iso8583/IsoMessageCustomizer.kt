package ru.whyhappen.pcidss.iso8583

/**
 * Customizes [IsoMessage] before sending.
 */
fun interface IsoMessageCustomizer {
    /**
     * Customizes the specified ISO message adding additional fields.
     *
     * @return `true` if the next customizer should be applied to the message; `false` otherwise
     */
    fun customize(isoMessage: IsoMessage): Boolean
}