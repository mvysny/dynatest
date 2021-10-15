package com.github.mvysny.dynatest

import java.io.PrintWriter
import java.io.StringWriter
import kotlin.reflect.KClass
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
 * Handy function to get a stack trace from receiver.
 */
public fun Throwable.getStackTraceAsString(): String {
    val sw = StringWriter()
    val pw = PrintWriter(sw)
    printStackTrace(pw)
    pw.flush()
    return sw.toString()
}
