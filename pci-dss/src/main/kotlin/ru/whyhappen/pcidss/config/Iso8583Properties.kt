package ru.whyhappen.pcidss.config

import com.github.kpavlov.jreactive8583.iso.ISO8583Version
import com.github.kpavlov.jreactive8583.iso.MessageOrigin
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.core.io.Resource
import java.nio.charset.StandardCharsets

/**
 * Configuration properties for ISO8583 server and client.
 */
@ConfigurationProperties(prefix = "iso8583")
data class Iso8583Properties(
    /**
     * Properties to establish a connection.
     */
    var connection: ConnectionProperties = ConnectionProperties(),
    /**
     * Properties for ISO8583 messages.
     */
    var message: Iso9583MessageProperties = Iso9583MessageProperties(),
    /**
     * Keystore properties.
     */
    var keystore: KeystoreProperties = KeystoreProperties()
)

/**
 * Connection properties.
 */
data class ConnectionProperties(
    /**
     * Connection host.
     */
    var host: String = "127.0.0.1",
    /**
     * Connection port.
     */
    var port: Int = 9876,
    /**
     * Indicates if management messages should be sent and received.
     */
    var addEchoMessageListener: Boolean = false,
    /**
     * Timeout between heartbeats in seconds.
     */
    var idleTimeout: Int = 30,
    /**
     * Indicates if a reply with an administrative message should be sent in case of message syntax errors.
     */
    var replyOnError: Boolean = false,
    /**
     * Indicates if messages should be logged.
     */
    var addLoggingHandler: Boolean = false,
    /**
     * Indicates if sensitive data specified in [sensitiveDataFields] should be masked in the log.
     */
    var logSensitiveData: Boolean = false,
    /**
     * List of ISO8583 sensitive field numbers to be masked.
     */
    var sensitiveDataFields: List<Int> = listOf(
        34, // PAN extended
        35, // track 2
        36, // track 3
        45, // track 1
    ),
    /**
     * Indicates if field names should be logged.
     */
    var logFieldDescription: Boolean = false
)

/**
 * Properties to create and parse ISO8583 messages.
 */
data class Iso9583MessageProperties(
    /**
     * ISO8583 version.
     */
    var isoVersion: ISO8583Version = ISO8583Version.V1987,
    /**
     * Role of the communicating party.
     */
    var role: MessageOrigin = MessageOrigin.ACQUIRER,
    /**
     * Indicates if the current date should be set on new messages (field 7).
     */
    var assignDate: Boolean = true,
    /**
     * Indicates if the header should be written/parsed as binary.
     */
    var binaryHeader: Boolean = false,
    /**
     * Indicates if the fields should be written/parsed as binary.
     */
    var binaryFields: Boolean = false,
    /**
     * ETX character, which is sent at the end of the message as a terminator.
     * Default is -1, which means no terminator is sent.
     */
    var etx: Int = -1,
    /**
     * Flag to specify if missing fields should be ignored as long as they're at the end of the message.
     */
    var ignoreLastMissingField: Boolean = false,
    /**
     * Flag to pass to new messages to include a secondary bitmap even if it's not needed.
     */
    var forceSecondaryBitmap: Boolean = false,
    /**
     * Flag to create messages that encode their bitmaps in binary format even when they're encoded as text.
     * Has no effect on binary messages.
     */
    var binaryBitmap: Boolean = false,
    /**
     * Indicates whether length headers for variable-length fields in text mode should be decoded
     * using proper string conversion with the character encoding.
     * Default is `false`, which means to use the old behavior of decoding as ASCII.
     */
    var forceStringEncoding: Boolean = false,
    /**
     * Indicates whether length headers for variable-length fields in binary mode should be decoded as
     * a hexadecimal values. Default is `false`, which means decoding the length as BCD.
     */
    var variableLengthFieldsInHex: Boolean = false,
    /**
     * Character encoding used for parsing ALPHA, LLVAR and LLLVAR fields.
     */
    var characterEncoding: String = StandardCharsets.US_ASCII.name(),
    /**
     * Resources to configure the message factory. They are applied consecutively.
     */
    var configs: List<Resource> = emptyList()
)

/**
 * Properties for the keystore that keeps the secret key to hash ISO8583 secret fields.
 */
data class KeystoreProperties(
    /**
     * Path to the keystore.
     */
    var keystorePath: String = "keystore.bcfks",
    /**
     * Keystore password.
     */
    var keystorePassword: String = "secret",
    /**
     * Password for the secret key.
     */
    var keyPassword: String = "secret",
    /**
     * Alias for the current secret key.
     */
    var currentKeyAlias: String = "current",
    /**
     * Alias for the previous secret key.
     */
    var previousKeyAlias: String = "previous",
)