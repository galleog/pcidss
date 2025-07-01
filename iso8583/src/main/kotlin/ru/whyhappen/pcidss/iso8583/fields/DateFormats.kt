package ru.whyhappen.pcidss.iso8583.fields

import java.time.format.DateTimeFormatter

/**
 * Data formats used in ISO messages.
 */
object DateFormats {
    val DATE10 = DateTimeFormatter.ofPattern("MMddHHmmss")
    val DATE4 = DateTimeFormatter.ofPattern("MMdd")
    val DATE6 = DateTimeFormatter.ofPattern("yyMMdd")
    val DATE4_EXP = DateTimeFormatter.ofPattern("yyMM")
    val TIME = DateTimeFormatter.ofPattern("HHmmss")
    val DATE12 = DateTimeFormatter.ofPattern("yyMMddHHmmss")
    val DATE14 = DateTimeFormatter.ofPattern("YYYYMMddHHmmss")
}