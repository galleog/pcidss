package ru.whyhappen.pcidss.iso8583.api.j8583.config.model

import com.solab.iso8583.IsoType

/**
 * Field that an incoming or outgoing message can contain with its type and length (if needed).
 */
data class Field(
    /**
     * Ordinal number of the field in a message.
     */
    val num: Int,
    /**
     * [IsoType] to which the value must be formatted or 'exclude' if the field should be excluded from a template.
     */
    val type: String,
    /**
     * Length of the field. Only ALPHA and NUMERIC types need to have a length specified.
     * The other types either have a fixed length or have their length specified as part of
     * the field (LLVAR and LLLVAR).
     */
    val length: Int? = null,
    /**
     * Timezone useful for date fields.
     */
    val tz: String? = null,
    /**
     * Default value of the field when creating messages.
     */
    val value: String? = null,
    /**
     * Subfields if the field acts as a container for several ISO values.
     */
    val subFields: List<Field> = emptyList(),
)