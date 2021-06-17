package com.fprieto.hms.wearable.presentation.vm


open class Event<out T> internal constructor(private val content: T) {

    @Suppress("MemberVisibilityCanBePrivate")
    var hasBeenHandled = false
        private set

    fun handleContent(): T? {
        if (hasBeenHandled) return null

        hasBeenHandled = true
        return content
    }

    fun peekContent(): T = content
}

fun <T> eventOf(value: T) = Event(value)

inline fun <T> Event<T>.handleOrPass(block: (T) -> Unit) {
    handleContent()?.apply(block)
}
