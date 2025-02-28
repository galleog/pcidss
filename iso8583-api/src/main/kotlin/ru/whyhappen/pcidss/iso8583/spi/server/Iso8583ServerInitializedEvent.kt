package ru.whyhappen.pcidss.iso8583.spi.server

import com.github.kpavlov.jreactive8583.server.Iso8583Server
import com.solab.iso8583.IsoMessage
import org.springframework.context.ApplicationEvent

/**
 * Event to be published after the application context is refreshed and the [Iso8583Server] is ready.
 *
 * @param server [Iso8583Server] that was started.
 */
class Iso8583ServerInitializedEvent<T : IsoMessage>(server: Iso8583Server<T>) : ApplicationEvent(server) {
    val server get() = getSource()

    @Suppress("UNCHECKED_CAST")
    override fun getSource(): Iso8583Server<T> = super.getSource() as Iso8583Server<T>
}