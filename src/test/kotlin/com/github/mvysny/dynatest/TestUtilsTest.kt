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
    }
})
