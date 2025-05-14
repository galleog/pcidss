package ru.whyhappen.pcidss.iso8583.api.reactor.netty.handler

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