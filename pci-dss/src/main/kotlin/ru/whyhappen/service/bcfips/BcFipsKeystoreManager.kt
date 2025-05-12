package ru.whyhappen.service.bcfips

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.whyhappen.service.KeyRepository
import ru.whyhappen.service.KeystoreManager
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.*
import java.security.GeneralSecurityException
import java.security.Key
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * [KeystoreManager] that uses [Bouncy Castle FIPS](https://www.bouncycastle.org/download/bouncy-castle-java-fips/)
 * keystore to store secret keys.
 */
class BcFipsKeystoreManager(
    /**
     * Path to the keystore to save secret keys.
     */
    keystorePath: String,
    /**
     * Keystore password.
     */
    private val keystorePassword: String,
    /**
     * Key password.
     */
    private val keyPassword: String,
    /**
     * Current key alias.
     */
    private val currentKeyAlias: String,
    /**
     * Previous key alias.
     */
    private val previousKeyAlias: String
) : KeystoreManager, KeyRepository {
    override val keystorePath: Path = Path.of(keystorePath)
    override lateinit var currentKey: Key
        private set
    override var previousKey: Key? = null
        private set

    private val keystoreExists: Boolean
        get() = Files.exists(keystorePath)

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(BcFipsKeystoreManager::class.java)

        private fun generateHmacKey(): SecretKey =
            KeyGenerator.getInstance("HmacSHA256", "BCFIPS").run {
                init(256, RandomHolder.secureRandom)
                generateKey()
            }
    }

    /**
     * Reads keys from the keystore or creates it if it doesn't exist.
     */
    @Throws(
        IOException::class,
        GeneralSecurityException::class
    )
    override fun init() {
        val keystore = KeyStore.getInstance("BCFKS", "BCFIPS")
        val keystorePassArray = keystorePassword.toCharArray()
        val keyPassArray = keyPassword.toCharArray()

        if (keystoreExists) {
            // read keys from the keystore
            Files.newInputStream(keystorePath, READ).use {
                keystore.load(it, keystorePassArray)
            }

            currentKey = keystore.getKey(currentKeyAlias, keyPassArray)
            previousKey = keystore.getKey(previousKeyAlias, keyPassArray)

            logger.info("Keystore $keystorePath has been read")
        } else {
            // create a new keystore
            keystore.load(null, null)

            currentKey = generateHmacKey()
            keystore.setKeyEntry(currentKeyAlias, currentKey, keyPassArray, null)

            logger.info("New keystore $keystorePath has been created")

            Files.newOutputStream(keystorePath, WRITE, CREATE_NEW).use {
                keystore.store(it, keystorePassArray)
            }
        }
    }

    /**
     * Changes the secret key used to hash ISO8583 secret fields and saves the previous key with another alias.
     */
    @Throws(
        IOException::class,
        GeneralSecurityException::class
    )
    override fun updateSecretKey() {
        require(keystoreExists) { "Keystore $keystorePath doesn't exist" }

        val keystore = KeyStore.getInstance("BCFKS", "BCFIPS")
        val keystorePassArray = keystorePassword.toCharArray()
        val keyPassArray = keyPassword.toCharArray()

        keystore.load(null, null)

        // generate a new key and store the previous one under the previous key alias
        previousKey = currentKey
        currentKey = generateHmacKey()
        keystore.setKeyEntry(currentKeyAlias, currentKey, keyPassArray, null)
        keystore.setKeyEntry(previousKeyAlias, previousKey, keyPassArray, null)

        logger.info("Secret key has been updated in keystore $keystorePath")

        Files.newOutputStream(keystorePath, WRITE, TRUNCATE_EXISTING).use {
            keystore.store(it, keystorePassArray)
        }
    }
}