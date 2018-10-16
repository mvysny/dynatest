package com.github.mvysny.dynatest

class _UniqueIdCheckupClass : DynaTest({
    test("root test") {}
    group("root group") {
        test("nested") {}
        group("nested group") {
            test("nested nested") {}
        }
        test("nested2") {}
    }
    test("root test 2") {}
})