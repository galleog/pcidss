package ru.whyhappen.pcidss.config

import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.redis.spring.ReactiveRedisLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Configuration for [ShedLock](https://github.com/lukas-krecan/ShedLock).
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
class ShedLockConfiguration {
    @Bean
    fun lockProvider(connectionFactory: ReactiveRedisConnectionFactory): LockProvider =
        ReactiveRedisLockProvider.Builder(connectionFactory)
            .build()
}