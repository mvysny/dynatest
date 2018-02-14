package com.github.mvysny.dynatest

import java.io.*
import kotlin.reflect.KClass
import kotlin.test.expect
import kotlin.test.fail

/**
 * Expects that given block fails with an exception of given [clazz] (or its subtype).
 * @throws AssertionError if the block completed successfully or threw some other exception.
 */
fun expectThrows(clazz: KClass<out Throwable>, block: ()->Unit) {
    var completedSuccessfully = false
    try {
        block()
        completedSuccessfully = true
    } catch (t: Throwable) {
        if (!clazz.java.isInstance(t)) throw AssertionError("Expected to fail with $clazz but failed with $t", t)
    }
    if (completedSuccessfully) fail("Expected to fail with $clazz but completed successfully")
}

/**
 * Expects that [actual] list of objects matches [expected] list of objects. Fails otherwise.
 */
fun <T> expectList(vararg expected: T, actual: ()->List<T>) = expect(expected.toList(), actual)

/**
 * Expects that [actual] map matches [expected] map, passed in as a list of pairs. Fails otherwise.
 */
fun <K, V> expectMap(vararg expected: Pair<K, V>, actual: ()->Map<K, V>) = expect(mapOf(*expected), actual)

/**
 * Serializes the object to a byte array
 * @return the byte array containing this object serialized form.
 */
fun Serializable.serializeToBytes(): ByteArray = ByteArrayOutputStream().also { ObjectOutputStream(it).writeObject(this) }.toByteArray()

/**
 * Clones this object by serialization and returns the deserialized clone.
 * @return the clone of this
 */
fun <T : Serializable> T.cloneBySerialization(): T = javaClass.cast(ObjectInputStream(ByteArrayInputStream(serializeToBytes())).readObject())
