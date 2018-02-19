package com.github.mvysny.dynatest

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.test.expect

class SanityTest {
    @Test
    fun simple() {
        var ran = false
        runTests {
            test("simple") {
                ran = true
            }
        }
        expect(true) { ran }
    }

    @Test
    fun simpleInGroups() {
        var ran = false
        runTests {
            group("group1") {
                group("group2") {
                    test("simple") {
                        ran = true
                    }
                }
            }
        }
        expect(true) { ran }
    }

    @Test
    fun simpleAfter() {
        var ran = false
        runTests {
            group("group1") {
                group("group2") {
                    test("simple") {}
                    afterEach { ran = true }
                }
            }
        }
        expect(true) { ran }
    }

    @Test
    fun testProperlyRethrowsException() {
        Assertions.assertThrows(TestFailedException::class.java) {
            runTests {
                test("always fail") {
                    throw RuntimeException("Simulated failure")
                }
            }
        }
    }
}
