package com.spreva.core.common

import java.util.UUID

/**
 * ID generation abstraction for events and cards so tests can be
 * deterministic (plan section 98).
 */
interface IdGenerator {
    fun newId(): String
}

/** Production generator using random UUIDs. */
class UuidGenerator : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}
