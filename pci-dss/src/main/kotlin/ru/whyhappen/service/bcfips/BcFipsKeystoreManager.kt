package ru.whyhappen.service.bcfips

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import ru.whyhappen.service.KeyRepository
import ru.whyhappen.service.KeystoreManager
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
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
open class BcFipsKeystoreManager(
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
    override val keystoreFile: Path = Path.of(keystorePath)
    override lateinit var currentKey: Key
        protected set
    override var previousKey: Key? = null
        protected set

    private val keystoreExists: Boolean
        get() = Files.exists(keystoreFile)

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(BcFipsKeystoreManager::class.java)

        private fun generateHmacKey(): SecretKey =
            KeyGenerator.getInstance("HmacSHA256", "BCFIPS").run {
                init(256, RandomHolder.secureRandom)
                generateKey()
            }
    }

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
            Files.newInputStream(keystoreFile, READ).use {
                keystore.load(it, keystorePassArray)
            }

            currentKey = keystore.getKey(currentKeyAlias, keyPassArray)
            previousKey = keystore.getKey(previousKeyAlias, keyPassArray)

            logger.info("Keystore {} has been read", keystoreFile)
        } else {
            // create a new keystore
            keystore.load(null, null)

            currentKey = generateHmacKey()
            keystore.setKeyEntry(currentKeyAlias, currentKey, keyPassArray, null)
            keystore.getCreationDate(currentKeyAlias)

            logger.info("New keystore {} has been created", keystoreFile)

            Files.newOutputStream(keystoreFile, WRITE, CREATE_NEW).use {
                keystore.store(it, keystorePassArray)
            }
        }
    }

    /**
     * Changes the secret key used to hash ISO8583 secret fields and saves the previous key with another alias.
     */
    @Scheduled(cron = "\${keystore.update-cron:@yearly}")
    @SchedulerLock(name = "updateSecretKey", lockAtMostFor = "PT1H", lockAtLeastFor = "PT1H")
    @Throws(
        IOException::class,
        GeneralSecurityException::class
    )
    fun updateSecretKey() {
        require(keystoreExists) { "Keystore $keystoreFile doesn't exist" }

        val keystore = KeyStore.getInstance("BCFKS", "BCFIPS")
        val keystorePassArray = keystorePassword.toCharArray()
        val keyPassArray = keyPassword.toCharArray()

        keystore.load(null, null)

        // generate a new key and store the previous one under the previous key alias
        val newKey = generateHmacKey()
        keystore.setKeyEntry(currentKeyAlias, newKey, keyPassArray, null)
        keystore.setKeyEntry(previousKeyAlias, currentKey, keyPassArray, null)

        // save old keystore
        val oldKeystoreFile = keystoreFile.resolveSibling("${keystoreFile.fileName}.old")
        Files.move(keystoreFile, oldKeystoreFile, ATOMIC_MOVE, REPLACE_EXISTING)

        runCatching {
            Files.newOutputStream(keystoreFile, WRITE, CREATE_NEW).use {
                keystore.store(it, keystorePassArray)
            }
        }.onSuccess {
            previousKey = currentKey
            currentKey = newKey

            logger.info("Secret key has been updated in keystore {}", keystoreFile)
        }.onFailure { e ->
            logger.error("Failed to update secret key in keystore {}", keystoreFile, e)

            // restore keystore
            Files.move(oldKeystoreFile, keystoreFile, ATOMIC_MOVE)
        }
    }
}