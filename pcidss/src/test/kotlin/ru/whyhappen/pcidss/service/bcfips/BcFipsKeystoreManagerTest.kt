package ru.whyhappen.pcidss.service.bcfips

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider
import org.junit.jupiter.api.BeforeAll
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.*
import java.security.Key
import java.security.KeyStore
import java.security.Security
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Tests for [BcFipsKeystoreManager].
 */
class BcFipsKeystoreManagerTest {
    private val keystorePassword = "password"
    private val keyPassword = "keyPassword"
    private val currentKeyAlias = "currentKey"
    private val previousKeyAlias = "previousKey"
    private lateinit var keystoreFile: Path

    companion object {
        @JvmStatic
        @BeforeAll
        fun setUpAll() {
            // add support for Bouncy Castle FIPS
            Security.addProvider(BouncyCastleFipsProvider())
        }
    }

    @BeforeTest
    fun setUp() {
        keystoreFile = Files.createTempFile("keystore-", ".bcfks")
    }

    @AfterTest
    fun tearDown() {
        keystoreFile.toFile().deleteOnExit()
    }

    @Test
    fun `should create a new keystore if it doesn't exist`() {
        val keystorePath = keystoreFile.toString()
        Files.deleteIfExists(keystoreFile)

        val keystoreManager = keystoreManager(keystorePath)
        keystoreManager.init()

        Files.exists(keystoreFile).shouldBeTrue()
        keystoreManager.currentKey.shouldBeInstanceOf<Key>()
        keystoreManager.previousKey.shouldBeNull()
    }

    @Test
    fun `should read keys from an existing keystore`() {
        val currentKey = getSecretKey()
        val previousKey = getSecretKey()
        createKeystoreWithKeys(currentKey, previousKey)

        val keystoreManager = keystoreManager(keystoreFile.toString())
        keystoreManager.init()

        keystoreManager.currentKey shouldBe currentKey
        keystoreManager.previousKey shouldBe previousKey
    }

    @Test
    fun `should update the current key`() {
        val currentKey = getSecretKey()
        val previousKey = getSecretKey()
        createKeystoreWithKeys(currentKey, previousKey)

        val keystoreManager = keystoreManager(keystoreFile.toString())
        keystoreManager.init()
        keystoreManager.updateSecretKey()

        keystoreManager.currentKey.shouldNotBeNull()
        keystoreManager.currentKey shouldNotBe currentKey
        keystoreManager.previousKey shouldBe currentKey

        val oldKeystoreFile = keystoreFile.resolveSibling("${keystoreFile.fileName}.old")
        Files.exists(oldKeystoreFile).shouldBeTrue()

        oldKeystoreFile.toFile().deleteOnExit()
    }

    private fun keystoreManager(keystorePath: String) = BcFipsKeystoreManager(
        keystorePath,
        keystorePassword,
        keyPassword,
        currentKeyAlias,
        previousKeyAlias
    )

    private fun createKeystoreWithKeys(currentKey: SecretKey, previousKey: SecretKey? = null) {
        // create a keystore and add the predefined keys
        val keystore = KeyStore.getInstance("BCFKS", "BCFIPS")
        keystore.load(null, null)

        val keyPassArray = keyPassword.toCharArray()
        keystore.setKeyEntry(currentKeyAlias, currentKey, keyPassArray, null)
        previousKey?.let {
            keystore.setKeyEntry(previousKeyAlias, it, keyPassArray, null)
        }

        Files.newOutputStream(keystoreFile, WRITE, CREATE, TRUNCATE_EXISTING).use {
            keystore.store(it, keystorePassword.toCharArray())
        }

        require(Files.exists(keystoreFile)) { "Keystore file does not exist after creation" }
        require(Files.size(keystoreFile) > 0) { "Keystore file is empty after creation" }
    }

    private fun getSecretKey(): SecretKey = SecretKeySpec(Random.nextBytes(16), "HmacSHA256")
}
