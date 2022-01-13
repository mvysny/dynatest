package com.github.mvysny.dynatest

// all of the classes below must not compile: there must be compiler error on every
// line marked with `// compiler error`

/*
class NestedBeforeEach : DynaTest({
    beforeEach {
        beforeEach {} // compiler error
        afterEach {} // compiler error
    }
    beforeEach {
        beforeGroup {} // compiler error
        afterGroup {} // compiler error
    }
})

class NestedBeforeGroup : DynaTest({
    beforeGroup {
        beforeGroup {} // compiler error
        afterGroup {}  // compiler error
    }
    beforeGroup {
        beforeEach {} // compiler error
        afterEach {}  // compiler error
    }
})

class NestedAfterEach : DynaTest({
    afterEach {
        beforeEach {} // compiler error
        afterEach {} // compiler error
    }
    afterEach {
        beforeGroup {} // compiler error
        afterGroup {} // compiler error
    }
})

class NestedAfterGroup : DynaTest({
    afterGroup {
        beforeGroup {} // compiler error
        afterGroup {}  // compiler error
    }
    afterGroup {
        beforeEach {} // compiler error
        afterEach {}  // compiler error
    }
})

class CallingBeforeFromTest : DynaTest({
    test("foo") {
        beforeEach {} // compiler error
        beforeGroup {}  // compiler error
    }
})

class CallingAfterFromTest : DynaTest({
    test("foo") {
        afterEach {} // compiler error
        afterGroup {}  // compiler error
    }
})

class CallingTestFromTest : DynaTest({
    test("foo") {
        test("bar") {} // compiler error
        xtest("bar") {} // compiler error
    }
})

class CallingTestFromXTest : DynaTest({
    xtest("foo") {
        test("bar") {} // compiler error
        xtest("bar") {} // compiler error
    }
})

class CallingTestFromBefore : DynaTest({
    beforeEach {
        test("bar") {} // compiler error
        xtest("bar") {} // compiler error
    }
    beforeGroup {
        test("bar") {} // compiler error
        xtest("bar") {} // compiler error
    }
})

class CallingTestFromAfter : DynaTest({
    afterEach {
        test("bar") {} // compiler error
        xtest("bar") {} // compiler error
    }
    afterGroup {
        test("bar") {} // compiler error
        xtest("bar") {} // compiler error
    }
})

class CallingGroupFromBefore : DynaTest({
    beforeEach {
        group("bar") {} // compiler error
        xgroup("bar") {} // compiler error
    }
    beforeGroup {
        group("bar") {} // compiler error
        xgroup("bar") {} // compiler error
    }
})

class CallingGroupFromAfter : DynaTest({
    afterEach {
        group("bar") {} // compiler error
        xgroup("bar") {} // compiler error
    }
    afterGroup {
        group("bar") {} // compiler error
        xgroup("bar") {} // compiler error
    }
})
*/
