@file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

package com.github.mvysny.dynatest

// doesn't run any tests - this only serves to test whether the API compiles and looks good.
@Suppress("UNUSED_PARAMETER")
abstract class DynaTest(block: DynaNodeGroup.()->Unit)

// this class lists all allowed cases of usages of the API and must always compile
class ApiTestClass : DynaTest({
    beforeEach { }
    beforeGroup { }
    afterEach { outcome: Outcome ->
    }
    afterEach { }
    afterGroup { outcome: Outcome ->
    }
    afterGroup {  }

    group("group") {
        beforeEach { }
        beforeGroup { }
        afterEach { }
        afterGroup {  }
        group("nested group") {
            test("a test") {}
            xtest("commented out test") {}
        }
        beforeEach { }
        beforeGroup { }
        afterEach { }
        afterGroup {  }
        xgroup("commented out group") {
            group("nested group") {
                test("test") {}
                xtest("test") {}
            }
            test("test") {}
            xtest("test") {}
        }
        beforeEach { }
        beforeGroup { }
        afterEach { }
        afterGroup {  }
    }
    xgroup("commented out group") {
        lateinit var something: String
        beforeEach { something = "foo" } // we should be able to have a lateinit variable and modify it

        group("nested group") {
            test("a test") { println(something) }
            xtest("commented out test") {}
        }
        xgroup("commented out group") {
            group("nested group") {
                test("test") {}
                xtest("test") {}
            }
            test("test") {}
            xtest("test") {}
        }
    }
})
