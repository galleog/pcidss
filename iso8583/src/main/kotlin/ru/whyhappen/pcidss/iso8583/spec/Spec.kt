package ru.whyhappen.pcidss.iso8583.spec

import ru.whyhappen.pcidss.iso8583.encode.Encoder
import ru.whyhappen.pcidss.iso8583.fields.BinaryField
import ru.whyhappen.pcidss.iso8583.fields.NumericField
import ru.whyhappen.pcidss.iso8583.fields.StringField
import ru.whyhappen.pcidss.iso8583.pad.Padder
import ru.whyhappen.pcidss.iso8583.prefix.Prefixer

/**
 * Specification of an ISO8583 field.
 */
data class Spec(
    /**
     * Defines the maximum length of the field value (bytes, characters, digits or hexadecimal digits),
     * for both fixed and variable lengths. You should use appropriate field types corresponding to the
     * length of the field you're defining, e.g. [NumericField], [StringField], [BinaryField] etc.
     * For hex fields, the length is defined in terms of bytes, while the value of the field is hex string.
     */
    val length: Int,
    /**
     * Description of what data the field holds.
     */
    val description: String,
    /**
     * [Encoder] used to marshal and unmarshal the field.
     * Only applicable to primitive field types e.g. numerics, strings, binary etc.
     */
    val encoder: Encoder,
    /**
     * [Prefixer] of the field used to encode and decode the length of the field.
     */
    val prefixer: Prefixer,
    /**
     * [Padder] sets the padding direction and type of the field.
     */
    val padder: Padder? = null
)