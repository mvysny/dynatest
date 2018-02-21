package com.github.mvysny.dynatest

import java.io.IOException
import kotlin.test.expect
import kotlin.test.fail

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
            var called = false
            runTests {
                afterEach { called = true }
                test("dummy test which triggers 'afterEach'") {}
            }
            expect(true) { called }
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
            var called = false
            expectFailures({
                runTests {
                    test("dummy") {}
                    afterEach { throw RuntimeException("simulated") }
                    afterEach { called = true }
                }
            }) {
                expectStats(0, 1, 0)
                expectFailure<RuntimeException>("dummy")
            }
            expect(true) { called }
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

    group("test the 'beforeAll' behavior") {
        test("simple before-test") {
            var called = false
            runTests {
                test("check that 'beforeAll' ran") {
                    expect(true) { called }
                }
                beforeAll { called = true }
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
                beforeAll { called = true }
            }
            expect(true) { called }
        }

        group("test when `beforeAll` fails") {
            test("`beforeEach`, `test`, `afterEach`, `afterAll` doesn't get called when `beforeAll` fails") {
                var called = false
                expectFailures({
                    runTests {
                        beforeAll { throw RuntimeException("Simulated") }
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

    group("test the 'afterAll' behavior") {
        test("simple after-test") {
            var called = 0
            runTests {
                afterAll { called++ }
                test("dummy test") {}
                test("dummy test2") {}
            }
            expect(1) { called }
        }

        test("`afterAll` is called only once even when there are nested groups") {
            var called = 0
            runTests {
                afterAll { called++ }

                group("artificial group") {
                    test("dummy test which triggers 'afterEach'") {}
                }
            }
            expect(1) { called }
        }

        group("exceptions") {
            test("`afterAll` is called even if `beforeAll` fails") {
                var called = 0
                expectFailures({
                    runTests {
                        beforeAll { throw RuntimeException("Simulated") }
                        afterAll { called++ }
                    }
                }) {
                    expectStats(0, 1, 0)
                }
                expect(1) { called }
            }

            test("Exceptions thrown in `afterAll` are attached as suppressed to the exception thrown in `beforeAll`") {
                expectFailures({
                    runTests {
                        beforeAll { throw RuntimeException("Simulated") }
                        afterAll { throw IOException("Simulated") }
                    }
                }) {
                    expectStats(0, 1, 0)
                    expectFailure<RuntimeException>("root")
                    expect<Class<out Throwable>>(IOException::class.java) { getFailure("root").suppressed[0].javaClass }
                }
            }

            test("Exceptions thrown from `beforeAll` do not prevent other groups from running") {
                var called = false
                expectFailures({
                    runTests {
                        group("failing group") {
                            beforeAll { throw RuntimeException("Simulated") }
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

            test("Failure in `afterAll` won't prevent `afterAll` from being called in the parent group") {
                var called = false
                expectFailures({
                    runTests {
                        group("Failing group") {
                            test("dummy") {}
                            afterAll { throw RuntimeException("Simulated") }
                        }
                        afterAll { called = true }
                    }
                }) {
                    expectStats(1, 1, 0)
                }
                expect(true) { called }
            }

            test("Failure in a test won't prevent `afterAll` from being called") {
                var called = false
                expectFailures({
                    runTests {
                        test("failing") { fail("simulated") }
                        afterAll { called = true }
                    }
                }) {
                    expectStats(0, 1, 0)
                }
                expect(true) { called }
            }

            test("Failure in a `afterEach` won't prevent `afterAll` from being called") {
                var called = false
                expectFailures({
                    runTests {
                        test("dummy") {}
                        afterEach { throw RuntimeException("Simulated") }
                        afterAll { called = true }
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

        test("calling beforeAll") {
            var called = false
            expectFailures({
                runTests {
                    test("should fail") {
                        this@runTests.beforeAll { called = true }
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

        test("calling afterAll") {
            var called = false
            expectFailures({
                runTests {
                    test("should fail") {
                        this@runTests.afterAll { called = true }
                    }
                }
            }) {
                expectStats(0, 1, 0)
                expectFailure<IllegalStateException>("should fail")
            }
            expect(false) { called }
        }
    }
})
