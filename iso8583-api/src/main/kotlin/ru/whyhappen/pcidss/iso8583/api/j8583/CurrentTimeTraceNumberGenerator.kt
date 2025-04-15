package ru.whyhappen.pcidss.iso8583.api.j8583

import com.solab.iso8583.TraceNumberGenerator
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger

/**
 * Implementation of [TraceNumberGenerator] that returns the current time as the next trace number,
 */
class CurrentTimeTraceNumberGenerator : TraceNumberGenerator {
    private val value: AtomicInteger = AtomicInteger(0)

    override fun nextTrace(): Int {
        while (true) {
            val current = value.get()
            val next = LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss")).toInt()
            if (value.compareAndSet(current, next))
                return next
        }
    }

    override fun getLastTrace() = value.get()
}