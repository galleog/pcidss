package ru.whyhappen.pcidss.core.iso8583

import com.solab.iso8583.IsoMessage

/**
 * Customizes [IsoMessage] before sending.
 */
fun interface IsoMessageCustomizer {
    /**
     * Customizes the specified ISO message adding additional fields.
     */
    fun customize(isoMessage: IsoMessage)
}