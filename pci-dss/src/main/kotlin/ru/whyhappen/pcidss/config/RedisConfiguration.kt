package ru.whyhappen.pcidss.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.RedisSerializer

/**
 * [Redis](https://redis.io/open-source/) configuration.
 */
@Configuration(proxyBeanMethods = false)
class RedisConfiguration {
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