package ru.wayhappen.pcidss.way4

import ru.whyhappen.pcidss.iso8583.encode.Encoders.ascii
import ru.whyhappen.pcidss.iso8583.encode.Encoders.bcd
import ru.whyhappen.pcidss.iso8583.encode.Encoders.binary
import ru.whyhappen.pcidss.iso8583.fields.BinaryField
import ru.whyhappen.pcidss.iso8583.fields.Bitmap
import ru.whyhappen.pcidss.iso8583.fields.StringField
import ru.whyhappen.pcidss.iso8583.pad.StartPadder
import ru.whyhappen.pcidss.iso8583.prefix.Ascii
import ru.whyhappen.pcidss.iso8583.prefix.Bcd
import ru.whyhappen.pcidss.iso8583.prefix.Binary
import ru.whyhappen.pcidss.iso8583.spec.MessageSpec
import ru.whyhappen.pcidss.iso8583.spec.Spec

/**
 * [MessageSpec] for Way4.
 */
object Way4MessageSpec {
    val spec = MessageSpec(
        mapOf(
            0 to StringField(
                spec = Spec(
                    length = 4,
                    description = "Message Type Identifier (MTI)",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            1 to Bitmap(
                Spec(
                    length = 8,
                    description = "Bitmap",
                    encoder = binary,
                    prefixer = Binary.fixed
                )
            ),
            2 to StringField(
                spec = Spec(
                    length = 19,
                    description = "Primary Account Number (PAN)",
                    encoder = bcd,
                    prefixer = Bcd.LL
                )
            ),
            3 to StringField(
                spec = Spec(
                    length = 6,
                    description = "Processing Code",
                    encoder = bcd,
                    prefixer = Bcd.fixed
                )
            ),
            4 to StringField(
                spec = Spec(
                    length = 12,
                    description = "Amount, Transaction",
                    encoder = bcd,
                    prefixer = Bcd.fixed,
                    padder = StartPadder('0')
                )
            ),
            6 to StringField(
                spec = Spec(
                    length = 12,
                    description = "Amount, Cardholder Billing",
                    encoder = bcd,
                    prefixer = Bcd.fixed,
                    padder = StartPadder('0')
                )
            ),
            7 to StringField(
                spec = Spec(
                    length = 10,
                    description = "Transmission Date & Time",
                    encoder = bcd,
                    prefixer = Bcd.fixed
                )
            ),
            10 to StringField(
                spec = Spec(
                    length = 8,
                    description = "Conversion Rate, Cardholder Billing",
                    encoder = bcd,
                    prefixer = Bcd.fixed
                )
            ),
            11 to StringField(
                spec = Spec(
                    length = 6,
                    description = "Systems Trace Audit Number (STAN)",
                    encoder = bcd,
                    prefixer = Bcd.fixed
                )
            ),
            12 to StringField(
                spec = Spec(
                    length = 6,
                    description = "Time, Local Transaction",
                    encoder = bcd,
                    prefixer = Bcd.fixed
                )
            ),
            13 to StringField(
                spec = Spec(
                    length = 4,
                    description = "Date, Local Transaction",
                    encoder = bcd,
                    prefixer = Bcd.fixed
                )
            ),
            14 to StringField(
                spec = Spec(
                    length = 4,
                    description = "Date, Expiration",
                    encoder = bcd,
                    prefixer = Bcd.fixed
                )
            ),
            16 to StringField(
                spec = Spec(
                    length = 4,
                    description = "Date, Conversion",
                    encoder = bcd,
                    prefixer = Bcd.fixed
                )
            ),
            18 to StringField(
                spec = Spec(
                    length = 4,
                    description = "Merchant Type",
                    encoder = bcd,
                    prefixer = Bcd.fixed
                )
            ),
            19 to StringField(
                spec = Spec(
                    length = 3,
                    description = "Acquiring Institution Country Code",
                    encoder = bcd,
                    prefixer = Bcd.fixed
                )
            ),
            20 to StringField(
                spec = Spec(
                    length = 3,
                    description = "PAN Country Code",
                    encoder = bcd,
                    prefixer = Bcd.fixed
                )
            ),
            22 to StringField(
                spec = Spec(
                    length = 3,
                    description = "Point of Service Entry Mode",
                    encoder = bcd,
                    prefixer = Bcd.fixed
                )
            ),
            23 to StringField(
                spec = Spec(
                    length = 3,
                    description = "Card Sequence Number",
                    encoder = bcd,
                    prefixer = Bcd.fixed
                )
            ),
            25 to StringField(
                spec = Spec(
                    length = 2,
                    description = "Point of Service Condition Code",
                    encoder = bcd,
                    prefixer = Bcd.fixed
                )
            ),
            26 to StringField(
                spec = Spec(
                    length = 2,
                    description = "POS PIN Capture Code",
                    encoder = bcd,
                    prefixer = Bcd.fixed
                )
            ),
            28 to StringField(
                spec = Spec(
                    length = 9,
                    description = "Amount, Transaction Fee",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            32 to StringField(
                spec = Spec(
                    length = 11,
                    description = "Acquiring Institution Identification Code",
                    encoder = bcd,
                    prefixer = Bcd.LL
                )
            ),
            33 to StringField(
                spec = Spec(
                    length = 11,
                    description = "Forwarding Institution Identification Code",
                    encoder = bcd,
                    prefixer = Bcd.LL
                )
            ),
            35 to BinaryField(
                spec = Spec(
                    length = 37,
                    description = "Track 2 Data",
                    encoder = bcd,
                    prefixer = Bcd.LL
                )
            ),
            37 to StringField(
                spec = Spec(
                    length = 12,
                    description = "Retrieval Reference Number",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            38 to StringField(
                spec = Spec(
                    length = 6,
                    description = "Authorization Identification Response",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            39 to StringField(
                spec = Spec(
                    length = 2,
                    description = "Response Code",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            41 to StringField(
                spec = Spec(
                    length = 8,
                    description = "Card Acceptor Terminal Identification",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            42 to StringField(
                spec = Spec(
                    length = 15,
                    description = "Card Acceptor Identification Code",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            43 to StringField(
                spec = Spec(
                    length = 40,
                    description = "Card Acceptor Name amd Location",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            44 to StringField(
                spec = Spec(
                    length = 25,
                    description = "Additional Response Data",
                    encoder = ascii,
                    prefixer = Bcd.LL
                )
            ),
            45 to StringField(
                spec = Spec(
                    length = 76,
                    description = "Track 1 Data",
                    encoder = ascii,
                    prefixer = Bcd.LL
                )
            ),
            46 to StringField(
                spec = Spec(
                    length = 999,
                    description = "Additional data (ISO)",
                    encoder = ascii,
                    prefixer = Bcd.LLL
                )
            ),
            47 to StringField(
                spec = Spec(
                    length = 999,
                    description = "Additional data (National)",
                    encoder = ascii,
                    prefixer = Bcd.LLL
                )
            ),
            48 to StringField(
                spec = Spec(
                    length = 999,
                    description = "Additional data (Private)",
                    encoder = ascii,
                    prefixer = Bcd.LLL
                )
            ),
            49 to StringField(
                spec = Spec(
                    length = 3,
                    description = "Currency Code, Transaction",
                    encoder = bcd,
                    prefixer = Bcd.fixed
                )
            ),
            51 to StringField(
                spec = Spec(
                    length = 3,
                    description = "Currency Code, Cardholder Billing",
                    encoder = bcd,
                    prefixer = Bcd.fixed
                )
            ),
            52 to BinaryField(
                spec = Spec(
                    length = 8,
                    description = "Personal Identification Number (PIN) Data",
                    encoder = binary,
                    prefixer = Binary.fixed
                )
            ),
            53 to StringField(
                spec = Spec(
                    length = 16,
                    description = "Security Related Control Information",
                    encoder = bcd,
                    prefixer = Bcd.fixed
                )
            ),
            54 to StringField(
                spec = Spec(
                    length = 120,
                    description = "Additional Amounts",
                    encoder = ascii,
                    prefixer = Bcd.LLL
                )
            ),
            55 to BinaryField(
                spec = Spec(
                    length = 255,
                    description = "Smart Card Specific Data",
                    encoder = binary,
                    prefixer = Bcd.LLL
                )
            ),
            70 to StringField(
                spec = Spec(
                    length = 3,
                    description = "Network Management Information Code",
                    encoder = bcd,
                    prefixer = Bcd.fixed
                )
            ),
            90 to StringField(
                spec = Spec(
                    length = 42,
                    description = "Original Data Elements",
                    encoder = bcd,
                    prefixer = Bcd.fixed
                )
            ),
            91 to StringField(
                spec = Spec(
                    length = 1,
                    description = "File Update Code",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            95 to StringField(
                spec = Spec(
                    length = 42,
                    description = "Replacement Amounts",
                    encoder = bcd,
                    prefixer = Bcd.fixed
                )
            ),
            100 to StringField(
                spec = Spec(
                    length = 11,
                    description = "Receiving Institution Identification Code",
                    encoder = bcd,
                    prefixer = Bcd.LL
                )
            ),
            101 to StringField(
                spec = Spec(
                    length = 17,
                    description = "File Name",
                    encoder = ascii,
                    prefixer = Bcd.LL
                )
            ),
            102 to StringField(
                spec = Spec(
                    length = 99,
                    description = "Account Identification 1",
                    encoder = ascii,
                    prefixer = Bcd.LL
                )
            ),
            103 to StringField(
                spec = Spec(
                    length = 99,
                    description = "Account Identification 2",
                    encoder = ascii,
                    prefixer = Bcd.LL
                )
            ),
            104 to StringField(
                spec = Spec(
                    length = 99,
                    description = "Transaction Description",
                    encoder = ascii,
                    prefixer = Bcd.LL
                )
            ),
            112 to BinaryField(
                spec = Spec(
                    length = 999,
                    description = "Reserved (National)",
                    encoder = binary,
                    prefixer = Bcd.LLL
                )
            ),
            122 to StringField(
                spec = Spec(
                    length = 999,
                    description = "Reserved (Private)",
                    encoder = ascii,
                    prefixer = Bcd.LLL
                )
            )
        )
    )
}