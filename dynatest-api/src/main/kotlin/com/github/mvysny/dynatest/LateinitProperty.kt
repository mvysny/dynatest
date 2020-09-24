package com.github.mvysny.dynatest

import java.lang.RuntimeException
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A very simple implementation of [ReadWriteProperty] which implements the semantics of `lateinit`.
 */
internal data class LateinitProperty<V: Any>(val name: String, private var value: V? = null) : ReadWriteProperty<Any?, V> {
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
        this.value = value
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): V {
        return value ?: throw RuntimeException("$this: not initialized")
    }
}

/**
 * Internal.
 */
public class LateinitPropertyProvider<V: Any> {
    public operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ReadWriteProperty<Any?, V> =
            LateinitProperty(prop.name)
}

/**
 * Allows you to write lateinit variables as follows:
 * ```
 * var file: File by late()
 * ```
 * Used in [withTempDir].
 */
public fun <V: Any> late(): LateinitPropertyProvider<V> = LateinitPropertyProvider()
