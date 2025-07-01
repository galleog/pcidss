package ru.whyhappen.pcidss.iso8583.mti

/**
 * ISO 8583 version
 *
 * The placements of fields in different versions of the standard vary;
 * for example, the currency elements of the 1987 and 1993 versions of the standard
 * are no longer used in the 2003 version, which holds currency as a subelement
 * of any financial amount element.
 *
 * As of June 2017, however, ISO 8583:2003 has yet to achieve wide acceptance.
 *
 * See [jreactive-iso8583](https://github.com/kpavlov/jreactive-8583).
 *
 * @param value the first digit in Message type indicator
) */
@Suppress("unused", "MagicNumber")
enum class ISO8583Version(val value: Int) {
    /**
     * ISO 8583:1987
     */
    V1987(0x0000),

    /**
     * ISO 8583:1993
     */
    V1993(0x1000),

    /**
     * ISO 8583:2003
     */
    V2003(0x2000),

    /**
     * National use
     */
    NATIONAL(0x8000),

    /**
     * Private use
     */
    PRIVATE(0x9000),
}
