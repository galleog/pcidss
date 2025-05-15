package ru.whyhappen.pcidss.core.service

import java.io.IOException
import java.nio.file.Path
import java.security.GeneralSecurityException

/**
 * Manager for the keystore to store secret keys.
 */
interface KeystoreManager {
    /**
     * Path to the keystore file.
     */
    val keystoreFile: Path

    /**
     * Verifies the keystore or creates it if it doesn't exist.
     */
    @Throws(
        IOException::class,
        GeneralSecurityException::class
    )
    fun init()

    /**
     * Changes the secret key used to hash ISO8583 secret fields and saves the previous key with another alias.
     */
    @Throws(
        IOException::class,
        GeneralSecurityException::class
    )
    fun updateSecretKey()
}