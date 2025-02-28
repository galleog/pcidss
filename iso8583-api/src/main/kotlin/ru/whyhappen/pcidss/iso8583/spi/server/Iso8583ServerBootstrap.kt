package ru.whyhappen.pcidss.iso8583.spi.server

import com.github.kpavlov.jreactive8583.server.Iso8583Server
import com.solab.iso8583.IsoMessage
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.ApplicationEventPublisherAware
import org.springframework.context.SmartLifecycle

/**
 * Bootstrap an [Iso8583Server] and start it with the application context.
 */
class Iso8583ServerBootstrap<T : IsoMessage>(
    private val server: Iso8583Server<T>
) : ApplicationEventPublisherAware, SmartLifecycle {
    private lateinit var eventPublisher: ApplicationEventPublisher

    override fun setApplicationEventPublisher(applicationEventPublisher: ApplicationEventPublisher) {
        this.eventPublisher = applicationEventPublisher
    }

    override fun start() {
        this.server.init()
        this.server.start()
        this.eventPublisher.publishEvent(Iso8583ServerInitializedEvent(this.server))
    }

    override fun stop() {
        this.server.stop()
    }

    override fun isRunning(): Boolean = this.server.isStarted
}