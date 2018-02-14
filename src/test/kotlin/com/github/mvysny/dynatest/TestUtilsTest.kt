package com.github.mvysny.dynatest

import java.io.IOException

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
})
