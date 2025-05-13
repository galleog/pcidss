package ru.whyhappen.service

import java.io.IOException
import java.nio.file.Path
import java.security.GeneralSecurityException
import java.time.LocalDateTime

/**
 * Manager for the keystore to store secret keys.
 */
interface KeystoreManager {
    /**
     * Path to the keystore.
     */
    val keystorePath: Path
    /**
     * Date of the last keystore update.
     */
    val keystoreDate: LocalDateTime

    /**
     * Verifies the keystore or creates it if it doesn't exist.
     */
    @Throws(
        IOException::class,
        GeneralSecurityException::class
    )
    fun init()

    /**
     * Changes the secret key used to encrypt ISO8583 sensitive fields and saves the previous key with another alias.
     */
    @Throws(
        IOException::class,
        GeneralSecurityException::class
    )
    fun updateSecretKey()
}