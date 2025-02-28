package ru.whyhappen.pcidss.iso8583.spi.j8583.config.model

import com.solab.iso8583.MessageFactory

/**
 * Model class to configure [MessageFactory].
 */
data class Iso8583Config(
    /**
     * Additional headers for new ISO8583 messages.
     */
    val headers: List<Header> = emptyList(),
    /**
     * Templates for new messages.
     */
    val templates: List<Template> = emptyList(),
    /**
     * Parsing templates for incoming messages.
     */
    val parses: List<Parse> = emptyList()
)