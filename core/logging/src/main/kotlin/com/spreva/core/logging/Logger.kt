package com.spreva.core.logging

/**
 * Logging abstraction. Never log user text, answers, tokens or personal
 * data (plan section 113).
 */
interface Logger {
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String, throwable: Throwable? = null)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}
