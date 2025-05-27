package ru.whyhappen.pcidss.iso8583.api.j8583.config.model

/**
 * ISO header after which the message type and the rest of the message must come.
 */
data class Header(
    /**
     * ISO type for messages the header is applied to.
     */
    val type: String,
    /**
     * ISO type for another header that has the same value. That header can be in another configuration file
     */
    val ref: String? = null,
    /**
     * Indicates if the header is binary.
     */
    val binary: Boolean? = null,
    /**
     * Header's value. Can be omitted if the header refers to another one.
     */
    val value: String?
)