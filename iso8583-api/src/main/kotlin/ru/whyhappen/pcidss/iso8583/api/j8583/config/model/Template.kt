package ru.whyhappen.pcidss.iso8583.api.j8583.config.model

/**
 * Template for new messages with the fields that the messages should include.
 */
data class Template(
    /**
     * ISO type for messages that should be created with this template.
     */
    val type: String,
    /**
     * ISO type for an existing template this one extends.
     * It can refer to the same template in another configuration file.
     */
    val extends: String?,
    /**
     * Fields new messages of this type contain.
     */
    val fields: List<Field> = emptyList()
)