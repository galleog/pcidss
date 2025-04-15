package ru.whyhappen.pcidss.iso8583.api.reactor.netty.server

import org.springframework.context.ApplicationEvent

/**
 * Event to be published after the application context is refreshed and the [Iso8583Server] is started.
 */
class Iso8583ServerInitializedEvent(server: Iso8583Server) : ApplicationEvent(server) {
    /**
     * [Iso8583Server] that was started.
     */
    val server get() = getSource()

    override fun getSource(): Iso8583Server = super.getSource() as Iso8583Server
}