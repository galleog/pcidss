package ru.whyhappen.service

import java.io.IOException
import java.nio.file.Path
import java.security.GeneralSecurityException

/**
 * Manager for the keystore to store secret keys.
 */
interface KeystoreManager {
    /**
     * Path to the keystore.
     */
    val keystorePath: Path

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