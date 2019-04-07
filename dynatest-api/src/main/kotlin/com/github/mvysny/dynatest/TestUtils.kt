package com.github.mvysny.dynatest

import java.io.*
import kotlin.reflect.KClass
import kotlin.test.expect
import kotlin.test.fail

/**
 * Expects that given block fails with an exception of given [clazz] (or its subtype).
 * @param message optional substring which the exception message must contain.
 * @throws AssertionError if the block completed successfully or threw some other exception.
 * @return the exception thrown, so that you can assert on it.
 */
fun <T: Throwable> expectThrows(clazz: KClass<out T>, message: String = "", block: ()->Unit): T {
    val ex: T? = try {
        block()
        null
    } catch (t: Throwable) {
        if (!clazz.java.isInstance(t)) {
            throw AssertionError("Expected to fail with ${clazz.javaObjectType.name} but failed with $t", t)
        }
        if (!(t.message ?: "").contains(message)) {
            throw AssertionError("${clazz.javaObjectType.name} message: Expected '$message' but was '${t.message}'", t)
        }
        clazz.java.cast(t)
    }
    return ex ?: fail("Expected to fail with ${clazz.javaObjectType.name} but completed successfully")
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
fun Serializable?.serializeToBytes(): ByteArray = ByteArrayOutputStream().also { ObjectOutputStream(it).writeObject(this) }.toByteArray()

inline fun <reified T: Serializable> ByteArray.deserialize(): T? = T::class.java.cast(ObjectInputStream(inputStream()).readObject())

/**
 * Clones this object by serialization and returns the deserialized clone.
 * @return the clone of this
 */
fun <T : Serializable> T.cloneBySerialization(): T = javaClass.cast(serializeToBytes().deserialize())

/**
 * Handy function to get a stack trace from receiver.
 */
fun Throwable.getStackTraceAsString(): String {
    val sw = StringWriter()
    val pw = PrintWriter(sw)
    printStackTrace(pw)
    pw.flush()
    return sw.toString()
}
