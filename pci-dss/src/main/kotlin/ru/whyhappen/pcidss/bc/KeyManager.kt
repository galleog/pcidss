package ru.whyhappen.pcidss.bc

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.*
import java.security.cert.CertificateException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Manager for the keystore and secret key the application uses to hash ISO8583 secret fields.
 * Uses Bouncy Castle FIPS keystore to save keys.
 */
class KeyManager(
    /**
     * Path to the keystore to save secret keys.
     */
    keystorePath: String,
    /**
     * Keystore password.
     */
    private val keystorePassword: String
) {
    val path: Path = Path.of(keystorePath)

    /**
     * Verifies if the keystore exists.
     */
    val keystoreExists: Boolean
        get() = Files.exists(path)

    /**
     * Gets the secret key from the keystore.
     *
     * @param keyAlias the alias for the new secret key
     * @param keyPassword the password to protect the key
     */
    @Throws(
        IOException::class,
        KeyStoreException::class,
        CertificateException::class,
        NoSuchAlgorithmException::class,
        UnrecoverableKeyException::class
    )
    fun getSecretKey(keyAlias: String, keyPassword: String): Key {
        require(keystoreExists) { "Keystore $path doesn't exist" }

        val keystore = KeyStore.getInstance("BCFKS", "BCFIPS")
        Files.newInputStream(path, StandardOpenOption.READ).use {
            keystore.load(it, keystorePassword.toCharArray())
        }

        return keystore.getKey(keyAlias, keyPassword.toCharArray())
    }

    /**
     * Changes the secret key used to hash ISO8583 secret fields and saves the previous key with another alias.
     *
     * @param currentKeyAlias the alias for the new secret key
     * @param previousKeyAlias the alias to store the previous secret key
     * @param keyPassword the password to protect the keys
     */
    @Throws(
        IOException::class,
        KeyStoreException::class,
        CertificateException::class,
        NoSuchProviderException::class,
        NoSuchAlgorithmException::class,
        UnrecoverableKeyException::class
    )
    fun updateSecretKey(currentKeyAlias: String, previousKeyAlias: String, keyPassword: String) {
        val keystore = KeyStore.getInstance("BCFKS", "BCFIPS")
        val keystorePassArray = keystorePassword.toCharArray()
        val keyPassArray = keyPassword.toCharArray()

        if (keystoreExists) {
            Files.newInputStream(path, StandardOpenOption.READ).use {
                keystore.load(it, keystorePassArray)
            }

            // store the previous key under the previous key alias
            val previousKey = keystore.getKey(currentKeyAlias, keyPassArray)
            keystore.setKeyEntry(currentKeyAlias, generateHmacKey(), keyPassArray, null)
            keystore.setKeyEntry(previousKeyAlias, previousKey, keyPassArray, null)

            logger.info("Secret key has been updated in keystore $path")
        } else {
            keystore.load(null, null)

            keystore.setKeyEntry(currentKeyAlias, generateHmacKey(), keyPassArray, null)

            logger.info("New keystore $path has been created")
        }

        Files.newOutputStream(
            path,
            StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.CREATE
        ).use {
            keystore.store(it, keystorePassArray)
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(KeyManager::class.java)

        private fun generateHmacKey(): SecretKey =
            KeyGenerator.getInstance("HmacSHA256", "BCFIPS").run {
                init(256, RandomHolder.secureRandom)
                generateKey()
            }
    }
}