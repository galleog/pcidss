package ru.whyhappen.pcidss.bpc

import ru.whyhappen.pcidss.iso8583.encode.Encoders.ascii
import ru.whyhappen.pcidss.iso8583.encode.Encoders.binary
import ru.whyhappen.pcidss.iso8583.encode.Encoders.hexToAscii
import ru.whyhappen.pcidss.iso8583.fields.BinaryField
import ru.whyhappen.pcidss.iso8583.fields.Bitmap
import ru.whyhappen.pcidss.iso8583.fields.StringField
import ru.whyhappen.pcidss.iso8583.prefix.Ascii
import ru.whyhappen.pcidss.iso8583.prefix.Binary
import ru.whyhappen.pcidss.iso8583.prefix.Hex
import ru.whyhappen.pcidss.iso8583.spec.MessageSpec
import ru.whyhappen.pcidss.iso8583.spec.Spec

/**
 * [MessageSpec] for BPC.
 */
object BpcMessageSpec {
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
                    length = 24,
                    description = "Primary Account Number (PAN)",
                    encoder = ascii,
                    prefixer = Ascii.LL
                )
            ),
            3 to StringField(
                spec = Spec(
                    length = 6,
                    description = "Processing Code",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            4 to StringField(
                spec = Spec(
                    length = 12,
                    description = "Amount, Transaction",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            5 to StringField(
                spec = Spec(
                    length = 12,
                    description = "Amount, Settlement",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            6 to StringField(
                spec = Spec(
                    length = 12,
                    description = "Amount, Cardholder Billing",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            7 to StringField(
                spec = Spec(
                    length = 10,
                    description = "Transmission Date & Time",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            8 to StringField(
                spec = Spec(
                    length = 8,
                    description = "Amount, Cardholder Billing Fee",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            9 to StringField(
                spec = Spec(
                    length = 8,
                    description = "Conversion Rate, Settlement",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            10 to StringField(
                spec = Spec(
                    length = 8,
                    description = "Conversion Rate, Cardholder Billing",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            11 to StringField(
                spec = Spec(
                    length = 6,
                    description = "Systems Trace Audit Number (STAN)",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            12 to StringField(
                spec = Spec(
                    length = 12,
                    description = "Time, Local Transaction",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            13 to StringField(
                spec = Spec(
                    length = 4,
                    description = "Date, Local Transaction",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            14 to StringField(
                spec = Spec(
                    length = 4,
                    description = "Date, Expiration",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            15 to StringField(
                spec = Spec(
                    length = 6,
                    description = "Date, Settlement",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            16 to StringField(
                spec = Spec(
                    length = 4,
                    description = "Date, Conversion",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            17 to StringField(
                spec = Spec(
                    length = 4,
                    description = "Date, Capture",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            18 to StringField(
                spec = Spec(
                    length = 4,
                    description = "Merchant Type",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            19 to StringField(
                spec = Spec(
                    length = 3,
                    description = "Acquiring Institution Country Code",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            20 to StringField(
                spec = Spec(
                    length = 3,
                    description = "PAN Country Code",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            21 to StringField(
                spec = Spec(
                    length = 3,
                    description = "Forwarding Institution Country Code",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            22 to StringField(
                spec = Spec(
                    length = 12,
                    description = "Point of Service Entry Mode",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            23 to StringField(
                spec = Spec(
                    length = 2,
                    description = "Application PAN Sequence Number",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            24 to StringField(
                spec = Spec(
                    length = 3,
                    description = "Network International Identifier (NII)",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            25 to StringField(
                spec = Spec(
                    length = 2,
                    description = "Point of Service Condition Code",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            26 to StringField(
                spec = Spec(
                    length = 2,
                    description = "POS PIN Capture Code",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            27 to StringField(
                spec = Spec(
                    length = 1,
                    description = "Authorization Identification Response Length",
                    encoder = ascii,
                    prefixer = Ascii.fixed
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
            29 to StringField(
                spec = Spec(
                    length = 9,
                    description = "Amount, Settlement Fee",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            30 to StringField(
                spec = Spec(
                    length = 9,
                    description = "Amount, Transaction Processing Fee",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            31 to StringField(
                spec = Spec(
                    length = 9,
                    description = "Amount, Settlement Processing Fee",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            32 to StringField(
                spec = Spec(
                    length = 11,
                    description = "Acquiring Institution Identification Code",
                    encoder = ascii,
                    prefixer = Ascii.LL
                )
            ),
            33 to StringField(
                spec = Spec(
                    length = 11,
                    description = "Forwarding Institution Identification Code",
                    encoder = ascii,
                    prefixer = Ascii.LL
                )
            ),
            34 to StringField(
                spec = Spec(
                    length = 28,
                    description = "Primary Account Number, Extended",
                    encoder = ascii,
                    prefixer = Ascii.LL
                )
            ),
            35 to BinaryField(
                spec = Spec(
                    length = 37,
                    description = "Track 2 Data",
                    encoder = binary,
                    prefixer = Ascii.LL
                )
            ),
            36 to StringField(
                spec = Spec(
                    length = 104,
                    description = "Track 3 Data",
                    encoder = ascii,
                    prefixer = Ascii.LLL
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
                    length = 3,
                    description = "Response Code",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            40 to StringField(
                spec = Spec(
                    length = 3,
                    description = "Service Restriction Code",
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
                    length = 152,
                    description = "Card Acceptor Name amd Location",
                    encoder = ascii,
                    prefixer = Ascii.LLL
                )
            ),
            44 to StringField(
                spec = Spec(
                    length = 25,
                    description = "Additional Response Data",
                    encoder = ascii,
                    prefixer = Ascii.LL
                )
            ),
            45 to StringField(
                spec = Spec(
                    length = 76,
                    description = "Track 1 Data",
                    encoder = ascii,
                    prefixer = Ascii.LL
                )
            ),
            46 to StringField(
                spec = Spec(
                    length = 999,
                    description = "Additional data (ISO)",
                    encoder = ascii,
                    prefixer = Ascii.LLL
                )
            ),
            47 to StringField(
                spec = Spec(
                    length = 999,
                    description = "Additional data (National)",
                    encoder = ascii,
                    prefixer = Ascii.LLL
                )
            ),
            48 to BinaryField(
                spec = Spec(
                    length = 999,
                    description = "Additional data (Private)",
                    encoder = binary,
                    prefixer = Ascii.LLL
                )
            ),
            49 to StringField(
                spec = Spec(
                    length = 3,
                    description = "Currency Code, Transaction",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            50 to StringField(
                spec = Spec(
                    length = 3,
                    description = "Currency Code, Settlement",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            51 to StringField(
                spec = Spec(
                    length = 3,
                    description = "Currency Code, Cardholder Billing",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            52 to StringField(
                spec = Spec(
                    length = 16,
                    description = "Personal Identification Number (PIN) Data",
                    encoder = ascii,
                    prefixer = Ascii.LL
                )
            ),
            53 to StringField(
                spec = Spec(
                    length = 16,
                    description = "Security Related Control Information",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            54 to StringField(
                spec = Spec(
                    length = 999,
                    description = "Additional Amounts",
                    encoder = ascii,
                    prefixer = Ascii.LLL
                )
            ),
            55 to StringField(
                spec = Spec(
                    length = 999,
                    description = "Reserved (ISO)",
                    encoder = ascii,
                    prefixer = Ascii.LLL
                )
            ),
            56 to StringField(
                spec = Spec(
                    length = 999,
                    description = "Message Reason Code",
                    encoder = ascii,
                    prefixer = Ascii.LLL
                )
            ),
            57 to StringField(
                spec = Spec(
                    length = 999,
                    description = "Reserved (National)",
                    encoder = ascii,
                    prefixer = Ascii.LLL
                )
            ),
            58 to StringField(
                spec = Spec(
                    length = 11,
                    description = "Reserved (National)",
                    encoder = ascii,
                    prefixer = Ascii.LL
                )
            ),
            59 to StringField(
                spec = Spec(
                    length = 255,
                    description = "Reserved (National)",
                    encoder = ascii,
                    prefixer = Ascii.LLL
                )
            ),
            60 to StringField(
                spec = Spec(
                    length = 999,
                    description = "Reserved (National)",
                    encoder = ascii,
                    prefixer = Ascii.LLL
                )
            ),
            61 to StringField(
                spec = Spec(
                    length = 999,
                    description = "Reserved (Private)",
                    encoder = ascii,
                    prefixer = Ascii.LLL
                )
            ),
            62 to StringField(
                spec = Spec(
                    length = 999,
                    description = "Reserved (Private)",
                    encoder = ascii,
                    prefixer = Ascii.LLL
                )
            ),
            63 to StringField(
                spec = Spec(
                    length = 999,
                    description = "Reserved (Private)",
                    encoder = ascii,
                    prefixer = Ascii.LLL
                )
            ),
            64 to StringField(
                spec = Spec(
                    length = 8,
                    description = "Message Authentication Code (MAC)",
                    encoder = hexToAscii,
                    prefixer = Hex.fixed
                )
            ),
            66 to StringField(
                spec = Spec(
                    length = 1,
                    description = "Settlement Code",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            67 to StringField(
                spec = Spec(
                    length = 2,
                    description = "Extended Payment Code",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            68 to StringField(
                spec = Spec(
                    length = 3,
                    description = "Receiving Institution Country Code",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            69 to StringField(
                spec = Spec(
                    length = 3,
                    description = "Settlement Institution Country Code",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            70 to StringField(
                spec = Spec(
                    length = 3,
                    description = "Network Management Information Code",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            71 to StringField(
                spec = Spec(
                    length = 4,
                    description = "Message Number",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            72 to StringField(
                spec = Spec(
                    length = 4,
                    description = "Message Number Last",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            73 to StringField(
                spec = Spec(
                    length = 6,
                    description = "Date, Action",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            74 to StringField(
                spec = Spec(
                    length = 10,
                    description = "Credits, Number",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            75 to StringField(
                spec = Spec(
                    length = 10,
                    description = "Credits, Reversal Number",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            76 to StringField(
                spec = Spec(
                    length = 10,
                    description = "Debits, Number",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            77 to StringField(
                spec = Spec(
                    length = 10,
                    description = "Debits, Reversal Number",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            78 to StringField(
                spec = Spec(
                    length = 10,
                    description = "Transfers, Number",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            79 to StringField(
                spec = Spec(
                    length = 10,
                    description = "Transfers, Reversal Number",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            80 to StringField(
                spec = Spec(
                    length = 10,
                    description = "Inquiries, Number",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            81 to StringField(
                spec = Spec(
                    length = 10,
                    description = "Authorizations, Number",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            82 to StringField(
                spec = Spec(
                    length = 12,
                    description = "Credits, Processing Fee Amount",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            83 to StringField(
                spec = Spec(
                    length = 12,
                    description = "Credits, Transaction Fee Amount",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            84 to StringField(
                spec = Spec(
                    length = 12,
                    description = "Debits, Processing Fee Amount",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            85 to StringField(
                spec = Spec(
                    length = 12,
                    description = "Debits, Transaction Fee Amount",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            86 to StringField(
                spec = Spec(
                    length = 16,
                    description = "Credits, Amount",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            87 to StringField(
                spec = Spec(
                    length = 16,
                    description = "Credits, Reversal Amount",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            88 to StringField(
                spec = Spec(
                    length = 16,
                    description = "Debits, Amount",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            89 to StringField(
                spec = Spec(
                    length = 16,
                    description = "Debits, Reversal Amount",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            90 to StringField(
                spec = Spec(
                    length = 42,
                    description = "Original Data Elements",
                    encoder = ascii,
                    prefixer = Ascii.fixed
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
            92 to StringField(
                spec = Spec(
                    length = 2,
                    description = "File Security Code",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            93 to StringField(
                spec = Spec(
                    length = 5,
                    description = "Response Indicator",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            94 to StringField(
                spec = Spec(
                    length = 7,
                    description = "Service Indicator",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            95 to StringField(
                spec = Spec(
                    length = 42,
                    description = "Replacement Amounts",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            96 to StringField(
                spec = Spec(
                    length = 8,
                    description = "Message Security Code",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            97 to StringField(
                spec = Spec(
                    length = 17,
                    description = "Amount, Net Settlement",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            98 to StringField(
                spec = Spec(
                    length = 25,
                    description = "Payee",
                    encoder = ascii,
                    prefixer = Ascii.fixed
                )
            ),
            99 to StringField(
                spec = Spec(
                    length = 11,
                    description = "Settlement Institution Identification Code",
                    encoder = ascii,
                    prefixer = Ascii.LL
                )
            ),
            100 to StringField(
                spec = Spec(
                    length = 4,
                    description = "Receiving Institution Identification Code",
                    encoder = ascii,
                    prefixer = Ascii.LL
                )
            ),
            101 to StringField(
                spec = Spec(
                    length = 17,
                    description = "File Name",
                    encoder = ascii,
                    prefixer = Ascii.LL
                )
            ),
            102 to StringField(
                spec = Spec(
                    length = 32,
                    description = "Account Identification 1",
                    encoder = ascii,
                    prefixer = Ascii.LL
                )
            ),
            103 to StringField(
                spec = Spec(
                    length = 32,
                    description = "Account Identification 2",
                    encoder = ascii,
                    prefixer = Ascii.LL
                )
            ),
            104 to StringField(
                spec = Spec(
                    length = 100,
                    description = "Transaction Description",
                    encoder = ascii,
                    prefixer = Ascii.LLL
                )
            ),
            105 to StringField(
                spec = Spec(
                    length = 999,
                    description = "Reserved (ISO)",
                    encoder = ascii,
                    prefixer = Ascii.LLL
                )
            ),
            106 to StringField(
                spec = Spec(
                    length = 999,
                    description = "Reserved (ISO)",
                    encoder = ascii,
                    prefixer = Ascii.LLL
                )
            ),
            107 to StringField(
                spec = Spec(
                    length = 999,
                    description = "Reserved (ISO)",
                    encoder = ascii,
                    prefixer = Ascii.LLL
                )
            ),
            108 to StringField(
                spec = Spec(
                    length = 999,
                    description = "Reserved (ISO)",
                    encoder = ascii,
                    prefixer = Ascii.LLL
                )
            ),
            109 to StringField(
                spec = Spec(
                    length = 999,
                    description = "Reserved (ISO)",
                    encoder = ascii,
                    prefixer = Ascii.LLL
                )
            ),
            110 to StringField(
                spec = Spec(
                    length = 999,
                    description = "Reserved (ISO)",
                    encoder = ascii,
                    prefixer = Ascii.LLL
                )
            ),
            111 to StringField(
                spec = Spec(
                    length = 999,
                    description = "Reserved (ISO)",
                    encoder = ascii,
                    prefixer = Ascii.LLL
                )
            ),
            112 to StringField(
                spec = Spec(
                    length = 999,
                    description = "Reserved (National)",
                    encoder = ascii,
                    prefixer = Ascii.LLL
                )
            ),
            113 to StringField(
                spec = Spec(
                    length = 999,
                    description = "Reserved (National)",
                    encoder = ascii,
                    prefixer = Ascii.LLL
                )
            ),
            114 to StringField(
                spec = Spec(
                    length = 999,
                    description = "Reserved (National)",
                    encoder = ascii,
                    prefixer = Ascii.LLL
                )
            ),
            115 to StringField(
                spec = Spec(
                    length = 999,
                    description = "Reserved (National)",
                    encoder = ascii,
                    prefixer = Ascii.LLL
                )
            ),
            116 to StringField(
                spec = Spec(
                    length = 999,
                    description = "Reserved (National)",
                    encoder = ascii,
                    prefixer = Ascii.LLL
                )
            ),
            117 to StringField(
                spec = Spec(
                    length = 999,
                    description = "Reserved (National)",
                    encoder = ascii,
                    prefixer = Ascii.LLL
                )
            ),
            118 to StringField(
                spec = Spec(
                    length = 999,
                    description = "Reserved (National)",
                    encoder = ascii,
                    prefixer = Ascii.LLL
                )
            ),
            119 to StringField(
                spec = Spec(
                    length = 999,
                    description = "Reserved (National)",
                    encoder = ascii,
                    prefixer = Ascii.LLL
                )
            ),
            120 to StringField(
                spec = Spec(
                    length = 999,
                    description = "Reserved (Private)",
                    encoder = ascii,
                    prefixer = Ascii.LLL
                )
            ),
            121 to StringField(
                spec = Spec(
                    length = 999,
                    description = "Reserved (Private)",
                    encoder = ascii,
                    prefixer = Ascii.LLL
                )
            ),
            122 to StringField(
                spec = Spec(
                    length = 999,
                    description = "Reserved (Private)",
                    encoder = ascii,
                    prefixer = Ascii.LLL
                )
            ),
            123 to StringField(
                spec = Spec(
                    length = 999,
                    description = "Reserved (Private)",
                    encoder = ascii,
                    prefixer = Ascii.LLL
                )
            ),
            124 to StringField(
                spec = Spec(
                    length = 999,
                    description = "Reserved (Private)",
                    encoder = ascii,
                    prefixer = Ascii.LLL
                )
            ),
            125 to StringField(
                spec = Spec(
                    length = 16,
                    description = "Reserved (Private)",
                    encoder = ascii,
                    prefixer = Ascii.LL
                )
            ),
            126 to StringField(
                spec = Spec(
                    length = 999,
                    description = "Reserved (Private)",
                    encoder = ascii,
                    prefixer = Ascii.LLL
                )
            ),
            127 to StringField(
                spec = Spec(
                    length = 999999,
                    description = "Reserved (Private)",
                    encoder = ascii,
                    prefixer = Ascii.LLLLLL
                )
            )
        )
    )
}