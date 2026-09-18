package com.spreva.core.common

import java.time.Instant

/**
 * Time abstraction so scheduling logic is testable with a fake clock
 * (plan section 97: no Instant.now() directly in business logic).
 */
interface AppClock {
    fun now(): Instant
}

/** Production clock backed by the system UTC clock. */
class SystemClock : AppClock {
    override fun now(): Instant = Instant.now()
}
