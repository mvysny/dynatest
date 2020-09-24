package com.github.mvysny.dynatest

import java.io.IOException
import java.lang.IllegalArgumentException
import kotlin.test.expect
import kotlin.test.fail

/**
 * Bunch of tests for the engine.
 *
 * If you add tests here, don't forget to fix the `build.gradle.kts` - there is a check that there are in fact 33
 * test methods executed by this test.
 */
class DynaTestEngineTest : DynaTest({
    group("test the 'beforeEach' behavior") {

        test("test that beforeEach runs before every test") {
            runTests {
                var called = false
                test("check that 'beforeEach' ran") {
                    expect(true) { called }
                }
                beforeEach { called = true }
            }
        }

        test("test that 'beforeEach' is also applied to tests nested inside a child group") {
            runTests {
                var called = false
                // an artificial group, only for the purpose of nesting the test that checks whether the 'beforeEach' block ran
                group("artificial group") {
                    test("check that 'beforeEach' ran") {
                        expect(true) { called }
                    }
                }
                beforeEach { called = true }
            }
        }

        test("when beforeEach throws, the test is not called") {
            expectFailures({
                runTests {
                    beforeEach { throw RuntimeException("expected") }
                    test("should not have been called") { fail("should not have been called since beforeEach failed") }
                }
            }) {
                expectStats(0, 1, 0)
                expectFailure<RuntimeException>("should not have been called")
            }
        }

        test("when beforeEach throws, the afterEach is still called") {
            var called = false
            expectFailures({
                runTests {
                    beforeEach { throw RuntimeException("expected") }
                    test("should not have been called") { fail("should not have been called since beforeEach failed") }
                    afterEach { called = true }
                }
            }) {
                expectStats(0, 1, 0)
                expectFailure<RuntimeException>("should not have been called")
            }
            expect(true) { called }
        }
    }

    group("test the 'afterEach' behavior") {
        test("test that 'afterEach' runs after every test") {
            lateinit var outcome: Outcome
            runTests {
                afterEach { outcome = it }
                test("dummy test which triggers 'afterEach'") {}
            }
            expect(true) { outcome.isSuccess }
        }

        test("test that 'afterEach' is also applied to tests nested inside a child group") {
            var called = 0
            runTests {
                afterEach { called++ }

                // an artificial group, only for the purpose of nesting the test that checks whether the 'afterEach' block ran
                group("artificial group") {
                    test("dummy test which triggers 'afterEach'") {}
                }
            }
            expect(1) { called }
        }

        test("when both beforeEach and afterEach throws, the afterEach's exception is added as suppressed") {
            var called = false
            expectFailures({
                runTests {
                    beforeEach { throw RuntimeException("expected") }
                    test("should not have been called") { called = true; fail("should not have been called since beforeEach failed") }
                    afterEach { throw IOException("simulated") }
                }
            }) {
                expectStats(0, 1, 0)
                expect<Class<out Throwable>>(IOException::class.java) { getFailure("should not have been called").suppressed[0].javaClass }
            }
            expect(false) { called }
        }

        test("throwing in `afterEach` will make the test fail") {
            expectFailures({
                runTests {
                    test("dummy") {}
                    afterEach { throw IOException("simulated") }
                }
            }) {
                expectStats(0, 1, 0)
                expectFailure<IOException>("dummy")
            }
        }

        test("throwing in test should invoke all `afterEach`") {
            expectFailures({
                runTests {
                    test("simulated failure") { throw RuntimeException("simulated") }
                    afterEach { throw IOException("simulated") }
                }
            }) {
                expectStats(0, 1, 0)
                expect<Class<out Throwable>>(IOException::class.java) { getFailure("simulated failure").suppressed[0].javaClass }
            }
        }

        test("all `afterEach` should have been invoked even if some of them fail") {
            lateinit var outcome: Outcome
            expectFailures({
                runTests {
                    test("dummy") {}
                    afterEach { throw RuntimeException("simulated") }
                    afterEach { outcome = it }
                }
            }) {
                expectStats(0, 1, 0)
                expectFailure<RuntimeException>("dummy")
            }
            expect(true) { outcome.isFailure }
            expect(RuntimeException::class.java) { outcome.failureCause?.javaClass }
            expect("simulated") { outcome.failureCause?.message }
        }

        test("if `beforeEach` fails, no `afterEach` in subgroup should be called") {
            var called = false
            expectFailures({
                runTests {
                    beforeEach { throw RuntimeException("simulated") }
                    group("nested group") {
                        test("dummy") { called = true; fail("should not have been called") }
                        afterEach { called = true; fail("should not have been called") }
                    }
                }
            }) {
                expectStats(0, 1, 0)
                expectFailure<RuntimeException>("dummy")
                expectList() { getFailure("dummy").suppressed.toList() }
            }
            expect(false) { called }
        }
    }

    group("test the 'beforeGroup' behavior") {
        test("simple before-test") {
            var called = false
            runTests {
                test("check that 'beforeGroup' ran") {
                    expect(true) { called }
                }
                beforeGroup { called = true }
            }
            expect(true) { called }
        }

        test("before-group") {
            var called = false
            runTests {
                group("artificial group") {
                    test("check that 'beforeEach' ran") {
                        expect(true) { called }
                    }
                }
                beforeGroup { called = true }
            }
            expect(true) { called }
        }

        group("test when `beforeGroup` fails") {
            test("`beforeEach`, `test`, `afterEach`, `afterGroup` doesn't get called when `beforeGroup` fails") {
                var called = false
                expectFailures({
                    runTests {
                        beforeGroup { throw RuntimeException("Simulated") }
                        beforeEach { called = true; fail("shouldn't be called") }
                        test("shouldn't be called") { called = true; fail("shouldn't be called") }
                        afterEach { called = true; fail("shouldn't be called") }
                    }
                }) {
                    expectStats(0, 1, 0)
                    expectFailure<RuntimeException>("root")
                    expect(0) { getFailure("root").suppressed.size }
                }
                expect(false) { called }
            }
        }
    }

    group("test the 'afterGroup' behavior") {
        test("simple after-test") {
            var called = 0
            runTests {
                afterGroup { called++ }
                test("dummy test") {}
                test("dummy test2") {}
            }
            expect(1) { called }
        }

        test("`afterGroup` is called only once even when there are nested groups") {
            var called = 0
            runTests {
                afterGroup { called++ }

                group("artificial group") {
                    test("dummy test which triggers 'afterEach'") {}
                }
            }
            expect(1) { called }
        }

        group("exceptions") {
            test("`afterGroup` is called even if `beforeGroup` fails") {
                var called = 0
                expectFailures({
                    runTests {
                        beforeGroup { throw RuntimeException("Simulated") }
                        afterGroup { called++ }
                    }
                }) {
                    expectStats(0, 1, 0)
                }
                expect(1) { called }
            }

            test("Exceptions thrown in `afterGroup` are attached as suppressed to the exception thrown in `beforeGroup`") {
                expectFailures({
                    runTests {
                        beforeGroup { throw RuntimeException("Simulated") }
                        afterGroup { throw IOException("Simulated") }
                    }
                }) {
                    expectStats(0, 1, 0)
                    expectFailure<RuntimeException>("root")
                    expect<Class<out Throwable>>(IOException::class.java) { getFailure("root").suppressed[0].javaClass }
                }
            }

            test("Exceptions thrown from `beforeGroup` do not prevent other groups from running") {
                var called = false
                expectFailures({
                    runTests {
                        group("failing group") {
                            beforeGroup { throw RuntimeException("Simulated") }
                        }
                        group("successful group") {
                            test("test") { called = true }
                        }
                    }
                }) {
                    expectStats(1, 1, 0)
                    expectFailure<RuntimeException>("failing group")
                }
                expect(true) { called }
            }

            test("Failure in `afterGroup` won't prevent `afterGroup` from being called in the parent group") {
                var called = false
                expectFailures({
                    runTests {
                        group("Failing group") {
                            test("dummy") {}
                            afterGroup { throw RuntimeException("Simulated") }
                        }
                        afterGroup { called = true }
                    }
                }) {
                    expectStats(1, 1, 0)
                }
                expect(true) { called }
            }

            test("Failure in a test won't prevent `afterGroup` from being called") {
                var called = false
                expectFailures({
                    runTests {
                        test("failing") { fail("simulated") }
                        afterGroup { called = true }
                    }
                }) {
                    expectStats(0, 1, 0)
                }
                expect(true) { called }
            }

            test("Failure in a `afterEach` won't prevent `afterGroup` from being called") {
                var called = false
                expectFailures({
                    runTests {
                        test("dummy") {}
                        afterEach { throw RuntimeException("Simulated") }
                        afterGroup { called = true }
                    }
                }) {
                    expectStats(0, 1, 0)
                }
                expect(true) { called }
            }
        }
    }

    group("nesting DynaTest inside a test block is forbidden") {
        test("calling test") {
            var called = false
            expectFailures({
                runTests {
                    test("should fail") {
                        this@runTests.test("can't define a test inside a test") { called = true }
                    }
                }
            }) {
                expectStats(0, 1, 0)
                expectFailure<IllegalStateException>("should fail")
            }
            expect(false) { called }
        }

        test("calling beforeGroup") {
            var called = false
            expectFailures({
                runTests {
                    test("should fail") {
                        this@runTests.beforeGroup { called = true }
                    }
                }
            }) {
                expectStats(0, 1, 0)
                expectFailure<IllegalStateException>("should fail")
            }
            expect(false) { called }
        }

        test("calling beforeEach") {
            var called = false
            expectFailures({
                runTests {
                    test("should fail") {
                        this@runTests.beforeEach { called = true }
                    }
                }
            }) {
                expectStats(0, 1, 0)
                expectFailure<IllegalStateException>("should fail")
            }
            expect(false) { called }
        }

        test("calling afterEach") {
            var called = false
            expectFailures({
                runTests {
                    test("should fail") {
                        this@runTests.afterEach { called = true }
                    }
                }
            }) {
                expectStats(0, 1, 0)
                expectFailure<IllegalStateException>("should fail")
            }
            expect(false) { called }
        }

        test("calling afterGroup") {
            var called = false
            expectFailures({
                runTests {
                    test("should fail") {
                        this@runTests.afterGroup { called = true }
                    }
                }
            }) {
                expectStats(0, 1, 0)
                expectFailure<IllegalStateException>("should fail")
            }
            expect(false) { called }
        }
    }

    group("make sure Gradle runs same-named tests") {
        // this is actually checked by the build.gradle.kts
        group("group 1") {
            test("a test") {  "foo".indices.count { "foo".substring(it).startsWith("bar") }  }
        }
        group("group 2") {
            test("a test") {}
        }
    }

    // intellij gets confused and only runs the first test; even if the second one fails, the failure is not reported
    // in the UI.
    group("prohibit same-named tests/groups in the same group") {
        test("test/test") {
            var called = 0
            expectThrows(IllegalArgumentException::class, "test/group with name 'duplicite' is already present: duplicite") {
                runTests {
                    test("duplicite") { called++; fail("shouldn't be called") }
                    test("duplicite") { called++; fail("shouldn't be called") }
                }
            }
            expect(0) { called }
        }
        test("test/group") {
            var called = 0
            expectThrows(IllegalArgumentException::class, "test/group with name 'duplicite' is already present: duplicite") {
                runTests {
                    test("duplicite") { called++; fail("shouldn't be called") }
                    group("duplicite") { called++; fail("shouldn't be called") }
                }
            }
            expect(0) { called }
        }
        test("group/test") {
            var called = 0
            expectThrows(IllegalArgumentException::class, "test/group with name 'duplicite' is already present: duplicite") {
                runTests {
                    group("duplicite") { }
                    test("duplicite") { called++; fail("shouldn't be called") }
                }
            }
            expect(0) { called }
        }
        test("group/group") {
            var called = 0
            expectThrows(IllegalArgumentException::class, "test/group with name 'duplicite' is already present: duplicite") {
                runTests {
                    group("duplicite") { }
                    group("duplicite") { called++; fail("shouldn't be called") }
                }
            }
            expect(0) { called }
        }
    }
})
