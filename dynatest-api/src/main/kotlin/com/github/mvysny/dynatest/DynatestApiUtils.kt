package com.github.mvysny.dynatest

import java.io.PrintWriter
import java.io.StringWriter
import kotlin.reflect.KClass
import kotlin.test.assertFailsWith

/**
 * Expects that given block fails with an exception of given [clazz] (or its subtype).
 *
 * Note that this is different from [assertFailsWith] since this function
 * also asserts on [Throwable.message].
 * @param expectMessage optional substring which the [Throwable.message] must contain.
 * @throws AssertionError if the block completed successfully or threw some other exception.
 * @return the exception thrown, so that you can assert on it.
 */
public fun <T: Throwable> expectThrows(clazz: KClass<out T>, expectMessage: String = "", block: ()->Unit): T {
    // tests for this function are present in the dynatest-engine project
    val ex = assertFailsWith(clazz, block)
    if (!(ex.message ?: "").contains(expectMessage)) {
        throw AssertionError("${clazz.javaObjectType.name} message: Expected '$expectMessage' but was '${ex.message}'", ex)
    }
    return ex
}

/**
 * Expects that given block fails with an exception of given [clazz] (or its subtype).
 *
 * Note that this is different from [assertFailsWith] since this function
 * also asserts on [Throwable.message].
 * @param expectMessage optional substring which the [Throwable.message] must contain.
 * @throws AssertionError if the block completed successfully or threw some other exception.
 * @return the exception thrown, so that you can assert on it.
 */
public inline fun <reified T: Throwable> expectThrows(expectMessage: String = "", noinline block: ()->Unit): T =
    expectThrows(T::class, expectMessage, block)

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
