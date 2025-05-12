package ru.whyhappen.pcidss.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Properties for the keystore that keeps the secret key to hash ISO8583 secret fields.
 */
@ConfigurationProperties(prefix = "keystore")
data class KeystoreProperties(
    /**
     * Path to the keystore.
     */
    var path: String = "keystore.bcfks",
    /**
     * Keystore password.
     */
    var password: String = "secret",
    /**
     * Password for the secret key.
     */
    var keyPassword: String = "secret",
    /**
     * Alias for the current secret key.
     */
    var currentKeyAlias: String = "currentKey",
    /**
     * Alias for the previous secret key.
     */
    var previousKeyAlias: String = "previousKey",
)