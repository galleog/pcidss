package ru.whyhappen.pcidss.core.service.redis

import com.redis.testcontainers.RedisContainer
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.spyk
import io.mockk.verifySequence
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import ru.whyhappen.pcidss.core.service.Encryptor
import ru.whyhappen.pcidss.core.service.KeyRepository
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Tests for [RedisTokenService].
 */
@Testcontainers
@ActiveProfiles("test")
@EnableAutoConfiguration
@ExtendWith(MockKExtension::class)
@SpringBootTest(classes = [RedisTokenServiceTest.TestConfig::class])
class RedisTokenServiceTest {
    @Autowired
    private lateinit var redisTemplate: ReactiveRedisTemplate<ByteArray, String>
    @MockK
    private lateinit var keyRepository: KeyRepository
    @MockK
    private lateinit var encryptor: Encryptor

    private val currentKey = SecretKeySpec("currentKey".toByteArray(), "HmacSHA256")
    private val previousKey = SecretKeySpec("previousKey".toByteArray(), "HmacSHA256")

    @OptIn(ExperimentalEncodingApi::class)
    private val data = Base64.encode(Random.Default.nextBytes(16))
    private val dataAsByteArray = data.toByteArray(Charsets.UTF_8)
    private val token = "token"

    companion object {
        @Container
        private val container = RedisContainer(RedisContainer.DEFAULT_IMAGE_NAME.withTag(RedisContainer.DEFAULT_TAG))

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.redis.url") { "redis://${container.host}:${container.redisPort}" }
        }
    }

    @BeforeTest
    fun setUp(): Unit = runBlocking {
        redisTemplate.connectionFactory.reactiveConnection.serverCommands().flushDb().awaitSingle()

        every { keyRepository.currentKey } returns currentKey
        every { encryptor.encrypt(currentKey, dataAsByteArray) } returns currentKey.encoded + dataAsByteArray
    }

    @Test
    fun `should find token if it exists among current keys`() = runTest {
        redisTemplate.opsForValue().set(currentKey.encoded + dataAsByteArray, token).awaitSingle()

        val tokenService = RedisTokenService(redisTemplate, keyRepository, encryptor)
        tokenService.getToken(data) shouldBe token

        verifySequence {
            keyRepository.currentKey
            encryptor.encrypt(currentKey, dataAsByteArray)
        }

        redisTemplate.hasKey(previousKey.encoded + dataAsByteArray).awaitSingle().shouldBeFalse()
    }

    @Test
    fun `should find token if it exists among previous keys`() = runTest {
        redisTemplate.opsForValue().set(previousKey.encoded + dataAsByteArray, token).awaitSingle()

        every { keyRepository.previousKey } returns previousKey
        every { encryptor.encrypt(previousKey, dataAsByteArray) } returns previousKey.encoded + dataAsByteArray

        val tokenService = RedisTokenService(redisTemplate, keyRepository, encryptor)
        tokenService.getToken(data) shouldBe token

        verifySequence {
            keyRepository.currentKey
            encryptor.encrypt(currentKey, dataAsByteArray)
            keyRepository.previousKey
            encryptor.encrypt(previousKey, dataAsByteArray)
        }

        redisTemplate.opsForValue().get(currentKey.encoded + dataAsByteArray).awaitSingle() shouldBe token
        redisTemplate.hasKey(previousKey.encoded + dataAsByteArray).awaitSingle().shouldBeFalse()
    }

    @Test
    fun `should generate a new token if it doesn't exist among current and previous keys`() = runTest {
        every { keyRepository.previousKey } returns previousKey
        every { encryptor.encrypt(previousKey, dataAsByteArray) } returns previousKey.encoded + dataAsByteArray

        val tokenService = RedisTokenService(redisTemplate, keyRepository, encryptor)
        val result = tokenService.getToken(data)
        result.shouldBeInstanceOf<String>()
        result shouldNotBe token

        verifySequence {
            keyRepository.currentKey
            encryptor.encrypt(currentKey, dataAsByteArray)
            keyRepository.previousKey
            encryptor.encrypt(previousKey, dataAsByteArray)
        }

        redisTemplate.opsForValue().get(currentKey.encoded + dataAsByteArray).awaitSingle() shouldBe result
        redisTemplate.hasKey(previousKey.encoded + dataAsByteArray).awaitSingle().shouldBeFalse()
    }

    @Test
    fun `should generate a new token if it doesn't exist among current keys and the previous key is null`() = runTest {
        every { keyRepository.previousKey } returns null

        val tokenService = RedisTokenService(redisTemplate, keyRepository, encryptor)
        val result = tokenService.getToken(data)
        result.shouldBeInstanceOf<String>()
        result shouldNotBe token

        verifySequence {
            keyRepository.currentKey
            encryptor.encrypt(currentKey, dataAsByteArray)
            keyRepository.previousKey
        }

        redisTemplate.opsForValue().get(currentKey.encoded + dataAsByteArray).awaitSingle() shouldBe result
        redisTemplate.hasKey(previousKey.encoded + dataAsByteArray).awaitSingle().shouldBeFalse()
    }

    @Test
    fun `should reread token if another process has already stored it`() = runTest {
        val spyRedisTemplate = spyk(redisTemplate)
        val operations = spyk(redisTemplate.opsForValue())
        every { spyRedisTemplate.opsForValue() } returns operations

        val tokenService = RedisTokenService(spyRedisTemplate, keyRepository, encryptor)

        every { keyRepository.previousKey } returns null
        every { operations.setIfAbsent(currentKey.encoded + dataAsByteArray, any<String>()) } answers {
            redisTemplate.opsForValue().set(currentKey.encoded + dataAsByteArray, token)
                .then(callOriginal())
        }

        tokenService.getToken(data) shouldBe token

        verifySequence {
            keyRepository.currentKey
            encryptor.encrypt(currentKey, dataAsByteArray)
            operations.get(currentKey.encoded + dataAsByteArray)
            keyRepository.previousKey
            operations.setIfAbsent(currentKey.encoded + dataAsByteArray, any<String>())
            operations.get(currentKey.encoded + dataAsByteArray)
        }
    }

    @Configuration
    class TestConfig {
        @Bean
        fun reactiveRedisTemplate(
            connectionFactory: ReactiveRedisConnectionFactory
        ): ReactiveRedisTemplate<ByteArray, String> {
            val serializationContext =
                RedisSerializationContext.newSerializationContext<ByteArray, String>(RedisSerializer.byteArray())
                    .value(RedisSerializer.string())
                    .build()
            return ReactiveRedisTemplate(connectionFactory, serializationContext)
        }
    }
}
