package com.github.mvysny.dynatest

import kotlin.test.expect

class DynaTestTest : DynaTest({
    group("test the 'beforeEach' behavior") {
        group("simple before-test") {
            var called = false
            test("check that 'beforeEach' ran") {
                expect(true) { called }
            }
            beforeEach { called = true }
        }

        group("before-group") {
            var called = false
            group("artificial group") {
                test("check that 'beforeEach' ran") {
                    expect(true) { called }
                }
            }
            beforeEach { called = true }
        }
    }

    group("test the 'afterEach' behavior") {
        group("simple after-test") {
            var called = false
            afterEach { called = true }

            test("dummy test which triggers 'afterEach'") {}

            test("check that 'afterEach' ran") {
                expect(true) { called }
            }
        }

        group("after-group") {
            var called = false
            afterEach { called = true }

            group("artificial group") {
                test("dummy test which triggers 'afterEach'") {}
            }

            test("check that 'afterEach' ran") {
                expect(true) { called }
            }
        }
    }
})
