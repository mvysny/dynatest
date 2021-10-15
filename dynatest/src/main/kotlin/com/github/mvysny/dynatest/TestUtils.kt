package com.github.mvysny.dynatest

import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KClass
import kotlin.test.expect
import kotlin.test.fail

/**
 * Expects that given block fails with an exception of given [clazz] (or its subtype).
 * @param message optional substring which the exception message must contain.
 * @throws AssertionError if the block completed successfully or threw some other exception.
 * @return the exception thrown, so that you can assert on it.
 */
public fun <T: Throwable> expectThrows(clazz: KClass<out T>, message: String = "", block: ()->Unit): T {
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
public fun <T> expectList(vararg expected: T, actual: ()->List<T>) {
    expect(expected.toList(), actual)
}

/**
 * Expects that [actual] map matches [expected] map, passed in as a list of pairs. Fails otherwise.
 */
public fun <K, V> expectMap(vararg expected: Pair<K, V>, actual: ()->Map<K, V>) {
    expect(mapOf(*expected), actual)
}

/**
 * Serializes the object to a byte array
 * @return the byte array containing this object serialized form.
 */
public fun Serializable?.serializeToBytes(): ByteArray = ByteArrayOutputStream().also { ObjectOutputStream(it).writeObject(this) }.toByteArray()

public inline fun <reified T: Serializable> ByteArray.deserialize(): T? = T::class.java.cast(ObjectInputStream(inputStream()).readObject())

/**
 * Clones this object by serialization and returns the deserialized clone.
 * @return the clone of this
 */
public fun <T : Serializable> T.cloneBySerialization(): T = javaClass.cast(serializeToBytes().deserialize())

/**
 * Handy function to get a stack trace from receiver.
 */
public fun Throwable.getStackTraceAsString(): String {
    val sw = StringWriter()
    val pw = PrintWriter(sw)
    printStackTrace(pw)
    pw.flush()
    return sw.toString()
}

/**
 * Similar to [File.deleteRecursively] but throws informative [IOException] instead of
 * just returning false on error. uses Java 8 [Files.deleteIfExists] to delete files and folders.
 */
public fun Path.deleteRecursively() {
    toFile().walkBottomUp().forEach { Files.deleteIfExists(it.toPath()) }
}

/**
 * Returns the major JVM version of the current JRE, e.g. 6 for Java 1.6, 8 for Java 8, 11 for Java 11 etc.
 */
public val jvmVersion: Int get() = System.getProperty("java.version").parseJvmVersion()

private fun String.parseJvmVersion(): Int {
    // taken from https://stackoverflow.com/questions/2591083/getting-java-version-at-runtime
    val version: String = removePrefix("1.").takeWhile { it.isDigit() }
    return version.toInt()
}
