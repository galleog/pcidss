package ru.whyhappen.pcidss.service

import java.security.Key

/**
 * Interface to get secret keys for encryption.
 */
interface KeyRepository {
    /**
     * Key used to encrypt data now.
     */
    val currentKey: Key

    /**
     * Key used to encrypt data beforehand.
     */
    val previousKey: Key?
}