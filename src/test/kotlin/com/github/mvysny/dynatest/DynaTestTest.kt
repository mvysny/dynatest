package com.github.mvysny.dynatest

import kotlin.test.expect

class DynaTestTest : DynaTest({
    group("test the 'beforeEach' behavior") {
        group("test that beforeEach runs before every test") {
            var called = false
            test("check that 'beforeEach' ran") {
                expect(true) { called }
            }
            beforeEach { called = true }
        }

        group("test that 'beforeEach' is also applied to tests nested inside a child group") {
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

    group("test the 'afterEach' behavior") {
        group("test that 'afterEach' runs after every test") {
            var called = false
            afterEach { called = true }

            test("dummy test which triggers 'afterEach'") {}

            test("check that 'afterEach' ran") {
                expect(true) { called }
            }
        }

        group("test that 'afterEach' is also applied to tests nested inside a child group") {
            var called = 0
            afterEach { called++ }

            // an artificial group, only for the purpose of nesting the test that checks whether the 'afterEach' block ran
            group("artificial group") {
                test("dummy test which triggers 'afterEach'") {}
            }

            test("check that 'afterEach' ran") {
                expect(1) { called }
            }
        }
    }

    group("test the 'beforeAll' behavior") {
        group("simple before-test") {
            var called = false
            test("check that 'beforeAll' ran") {
                expect(true) { called }
            }
            beforeAll { called = true }
        }

        group("before-group") {
            var called = false
            group("artificial group") {
                test("check that 'beforeEach' ran") {
                    expect(true) { called }
                }
            }
            beforeAll { called = true }
        }
    }

    group("test the 'afterAll' behavior") {
        group("simple after-test") {
            var called = 0
            group("dummy") {
                afterAll { called++ }
                test("dummy test") {}
                test("dummy test2") {}
            }

            test("check that 'afterAll' ran") {
                expect(1) { called }
            }
        }

        group("after-group") {
            var called = 0
            group("dummy") {
                afterAll { called++ }

                group("artificial group") {
                    test("dummy test which triggers 'afterEach'") {}
                }
            }

            test("check that 'afterAll' ran") {
                expect(1) { called }
            }
        }
    }
})
