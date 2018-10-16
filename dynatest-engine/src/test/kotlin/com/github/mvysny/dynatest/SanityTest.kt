package com.github.mvysny.dynatest

import com.github.mvysny.dynatest.engine.DynaNodeGroupImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.test.expect

/**
 * If one of these tests fail, there is a really glaring bug in the DynaTest framework. Other test suites will then produce completely
 * incorrect results and should be ignored.
 */
class SanityTest {
    @Test
    fun testComputeTestSource() {
        val e = DynaNodeGroupImpl.computeTestSource()!!
        expect(SanityTest::class.java.name) { e.className }
        expect("testComputeTestSource") { e.methodName }
    }

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
