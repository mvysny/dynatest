package com.github.mvysny.dynatest

import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class TestUtilsTest : DynaTest({
    group("tests for expectThrows()") {
        test("throwing expected exception succeeds") {
            expectThrows(RuntimeException::class) { throw RuntimeException("Expected") }
        }

        test("fails if block completes successfully") {
            try {
                expectThrows(RuntimeException::class) {}
                throw RuntimeException("Should have failed")
            } catch (e: AssertionError) { /*okay*/ }
        }

        test("fails if block throws something else") {
            expectThrows(AssertionError::class) {
                // this should fail with AssertionError since some other exception has been thrown
                expectThrows(RuntimeException::class) {
                    throw IOException("simulated")
                }
            }
        }

        group("AssertionError not handled specially") {
            test("throwing expected exception succeeds") {
                expectThrows(AssertionError::class) { throw AssertionError("Expected") }
            }

            test("fails if block completes successfully") {
                try {
                    expectThrows(AssertionError::class) {}
                    throw RuntimeException("Should have failed")
                } catch (e: AssertionError) { /*okay*/ }
            }

            test("fails if block throws something else") {
                try {
                    // this should fail with AssertionError since some other exception has been thrown
                    expectThrows(AssertionError::class) {
                        throw IOException("simulated")
                    }
                    throw RuntimeException("Should have failed")
                } catch (e: AssertionError) { /*okay*/ }
            }
        }
    }

    group("tests for expectList") {
        test("empty list") {
            expectList() { listOf<Int>() }
            expectList() { mutableListOf<Int>() }
            expectList() { LinkedList<Int>() }
            expectList() { CopyOnWriteArrayList<Int>() }
        }
        test("trivial list") {
            expectList(25) { listOf(25) }
        }
        test("simple list of strings") {
            expectList("a", "b", "c") { listOf("a", "b", "c") }
        }
        test("comparison failure") {
            expectThrows(AssertionError::class) {
                expectList() { listOf("a", "b", "c") }
            }
            expectThrows(AssertionError::class) {
                expectList(1, 2, 3) { listOf("a", "b", "c") }
            }
            expectThrows(AssertionError::class) {
                expectList(1, 2, 3) { listOf() }
            }
        }
    }

    group("tests for expectMap") {
        test("empty map") {
            expectMap() { mapOf<Int, Int>() }
            expectMap() { mutableMapOf<Int, Int>() }
            expectMap() { LinkedHashMap<String, Boolean>() }
            expectMap() { ConcurrentHashMap<String, Boolean>() }
        }
        test("trivial map") {
            expectMap(25 to "a") { mapOf(25 to "a") }
        }
        test("simple map of strings") {
            expectMap("a" to 1, "b" to 2, "c" to 3) { mutableMapOf("a" to 1, "b" to 2, "c" to 3) }
        }
        test("comparison failure") {
            expectThrows(AssertionError::class) {
                expectMap() { mapOf("a" to 1, "b" to 2, "c" to 3) }
            }
            expectThrows(AssertionError::class) {
                expectMap("a" to 1, "b" to 2, "c" to 3) { mapOf<Any, Any>(1 to "a", 2 to "b", 3 to "c") }
            }
            expectThrows(AssertionError::class) {
                expectMap("a" to 1, "b" to 2, "c" to 3) { mapOf<Any, Any>() }
            }
        }
    }
})
