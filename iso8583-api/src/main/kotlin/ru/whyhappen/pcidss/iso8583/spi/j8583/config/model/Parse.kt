package ru.whyhappen.pcidss.iso8583.spi.j8583.config.model

/**
 * Defines a parsing template for a message type. It includes all the fields an incoming message can contain.
 */
data class Parse(
    /**
     * ISO type of incoming messages the parsing rules are applied to.
     * It can be omitted if the rules are for all message types.
     */
    val type: String?,
    /**
     * ISO type for an existing parsing template this one extends.
     * It can refer to the same template in another configuration file or
     * be 'untyped' if this template extends a one without any type.
     */
    val extends: String?,
    /**
     * Fields incoming messages of this type contain.
     */
    val fields: List<Field> = emptyList()
)
