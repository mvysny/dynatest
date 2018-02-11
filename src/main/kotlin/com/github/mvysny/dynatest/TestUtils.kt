package com.github.mvysny.dynatest

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
