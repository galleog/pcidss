package ru.whyhappen.pcidss.iso8583.autoconfigure.server

import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.ApplicationEventPublisherAware
import org.springframework.context.SmartLifecycle
import ru.whyhappen.pcidss.iso8583.reactor.server.Iso8583Server

/**
 * Bootstrap an [Iso8583Server] and start it with the application context.
 */
class Iso8583ServerBootstrap(private val server: Iso8583Server) : ApplicationEventPublisherAware, SmartLifecycle {
    private lateinit var eventPublisher: ApplicationEventPublisher

    override fun setApplicationEventPublisher(applicationEventPublisher: ApplicationEventPublisher) {
        this.eventPublisher = applicationEventPublisher
    }

    override fun start() {
        this.server.start()
        eventPublisher.publishEvent(Iso8583ServerInitializedEvent(this.server))
    }

    override fun stop() {
        this.server.stop()
    }

    override fun isRunning(): Boolean = this.server.isStarted
}