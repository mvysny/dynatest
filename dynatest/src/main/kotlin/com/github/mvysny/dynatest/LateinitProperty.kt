package com.github.mvysny.dynatest

import java.lang.RuntimeException
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A very simple implementation of [ReadWriteProperty] which implements the semantics of `lateinit`.
 *
 * Allows you to create a reusable `withXYZ()` function. See README.md for more details.
 *
 * See [withTempDir] and the DynaTest documentation on how to use this class in your projects.
 */
public data class LateinitProperty<V: Any>(val name: String, private var value: V? = null) : ReadWriteProperty<Any?, V> {
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
        this.value = value
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): V {
        return value ?: throw RuntimeException("$this: not initialized")
    }
}

/**
 * Internal, used by [late].
 */
public class LateinitPropertyProvider<V: Any> {
    public operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ReadWriteProperty<Any?, V> =
            LateinitProperty(prop.name)
}

/**
 * Allows you to write lateinit variables as follows:
 * ```
 * var file: File by late()
 * beforeEach { file = File.createTempFile("foo", "bar") }
 * afterEach { file.delete() }
 * test("something") {
 *   file.expectExists()
 * }
 * ```
 *
 * However, to create a reusable `withXYZ()` function, see [LateinitProperty] directly.
 */
public fun <V: Any> late(): LateinitPropertyProvider<V> = LateinitPropertyProvider()
