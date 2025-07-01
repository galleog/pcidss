package ru.whyhappen.pcidss.service.redis

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.core.publisher.Mono
import ru.whyhappen.pcidss.service.Encryptor
import ru.whyhappen.pcidss.service.KeyRepository
import ru.whyhappen.pcidss.service.TokenService
import ru.whyhappen.pcidss.service.bcfips.Tokenizer

/**
 * Implementation of [TokenService] that stores tokens in [Redis](https://redis.io/community-edition/).
 */
class RedisTokenService(
    redisTemplate: ReactiveRedisTemplate<ByteArray, String>,
    private val keyRepository: KeyRepository,
    private val encryptor: Encryptor
) : TokenService {
    private val operations = redisTemplate.opsForValue()

    companion object {
        private val logger = LoggerFactory.getLogger(RedisTokenService::class.java)
    }

    override suspend fun getToken(value: ByteArray): String = coroutineScope {
        val key = encryptor.encrypt(keyRepository.currentKey, value)
        operations.get(key)
            .switchIfEmpty(Mono.defer { tryGetPrevious(key, value) })
            .awaitSingle()
            .also { logger.debug("Token '{}' found", it) }
    }

    private fun tryGetPrevious(key: ByteArray, data: ByteArray): Mono<String> {
        logger.debug("Token not found among current keys, try previous ones")

        val monoToken = keyRepository.previousKey?.run {
            operations.getAndDelete(encryptor.encrypt(this, data))
                .switchIfEmpty(Mono.defer { generateToken() })
        } ?: generateToken()

        return monoToken.zipWhen { token ->
            // TODO: set expiration time
            operations.setIfAbsent(key, token)
        }.flatMap { tuple ->
            if (tuple.t2) Mono.just(tuple.t1)
            else {
                logger.debug("Token has been written by another thread, reread it")
                operations.get(key)
            }
        }
    }

    private fun generateToken(): Mono<String> = Mono.just(
        Tokenizer.generateToken().also {
            logger.debug("Token not found among previous keys, new token '{}' generated", it)
        }
    )
}