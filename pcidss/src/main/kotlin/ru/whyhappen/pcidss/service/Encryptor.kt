package ru.whyhappen.pcidss.service

import java.security.GeneralSecurityException
import java.security.Key

/**
 * Interface to encrypt data.
 */
interface Encryptor {
    /**
     * Encrypts the specified data using the specified key.
     *
     * @param key the key to use for encryption
     * @param data the [ByteArray] that should be encrypted
     * @return the encrypted data
     */
    @Throws(GeneralSecurityException::class)
    fun encrypt(key: Key, data: ByteArray): ByteArray
}